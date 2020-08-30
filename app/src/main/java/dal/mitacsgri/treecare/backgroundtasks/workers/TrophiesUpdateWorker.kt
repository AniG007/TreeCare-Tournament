package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.backgroundtasks.jobs.TrophiesUpdateJob
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.UserChallengeTrophies
import dal.mitacsgri.treecare.model.UserTournamentTrophies
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.concurrent.TimeUnit.*

class TrophiesUpdateWorker(appContext: Context, workerParams: WorkerParameters)
    :ListenableWorker(appContext, workerParams), KoinComponent {

    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()

    private var challengesCounter = ObservableInt()
    private var tournamentsCounter = ObservableInt1()
    private val mConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    val TAG = "TrophiesUpdateWorker"

    override fun startWork(): ListenableFuture<Result> {
        val future = SettableFuture.create<Result>()
        val currentChallenges = sharedPrefsRepository.user.currentChallenges
        val userTrophies = UserChallengeTrophies()
        Log.d(TrophiesUpdateJob.TAG, "Running Job")
        val currentTournaments = sharedPrefsRepository.team.currentTournaments
        val tournamentTrophies = UserTournamentTrophies()

        currentChallenges.forEach { (name, userChallenge) ->
            firestoreRepository.getChallenge(name).addOnSuccessListener {
                val challenge = it.toObject<Challenge>()
                Log.d(TAG, "Running Trophy Job for challenges")
                if (challenge != null && !challenge.active) {
                    Log.d(TAG, "Some challenge is not active")
                    when (challenge.players.indexOf(sharedPrefsRepository.user.uid)) {
                        0 -> userTrophies.gold.add(name)
                        1 -> userTrophies.silver.add(name)
                        2 -> userTrophies.bronze.add(name)
                    }

                    challengesCounter.setValue(challengesCounter.getValue() + 1) {
                        Log.d(TAG, "C counter " + it + "C size " + currentTournaments.size)
                        if (it == currentChallenges.size) {
                            firestoreRepository.storeTrophiesData(
                                sharedPrefsRepository.user.uid,
                                userTrophies
                            )
                                .addOnSuccessListener {
                                    Log.d(TrophiesUpdateJob.TAG, "Success")
                                }
                                .addOnFailureListener {
                                    Log.d(TrophiesUpdateJob.TAG, it.toString())
                                }
                        }
                    }
                }
            }
        }

        currentTournaments.forEach { (name, userTournament) ->
            firestoreRepository.getTournament(name).addOnSuccessListener {
                val tournament = it.toObject<Tournament>()
                Log.d(TrophiesUpdateJob.TAG, "Running Job for tournaments")
                if (tournament != null && !tournament.active) {
                    Log.d(TAG, "Some tournament is not active")
                    Log.d(TAG, "index of your team " + tournament.teams.indexOf(sharedPrefsRepository.team.name).toString())
                    Log.d(TAG, "Name of your tournament " + tournament.name + "Teams in it " + tournament.teams)
                    when (tournament.teams.indexOf(sharedPrefsRepository.team.name)) {
                        0 -> tournamentTrophies.gold.add(name)
                        1 -> tournamentTrophies.silver.add(name)
                        2 -> tournamentTrophies.bronze.add(name)
                    }
                }

                tournamentsCounter.setValue(tournamentsCounter.getValue() + 1) {
                    Log.d(TrophiesUpdateJob.TAG, "counter " + it + "Size " + currentTournaments.size)
                    if (it == currentTournaments.size) {
                        Log.d(TrophiesUpdateJob.TAG, "Size equals tournament counter")
                        firestoreRepository.storeTeamTrophiesData(
                            sharedPrefsRepository.team.name,
                            tournamentTrophies
                        )
                            .addOnSuccessListener {
                                Log.d(TrophiesUpdateJob.TAG, "TSuccess")
                                future.set(Result.success())
                            }
                            .addOnFailureListener {
                                Log.d(TrophiesUpdateJob.TAG, "TFailure")
                                future.set(Result.failure())
                            }
                    }
                }
            }
        }

        val TrophiesUpdateRequest: WorkRequest =
            OneTimeWorkRequestBuilder<TrophiesUpdateWorker>()
                .setConstraints(mConstraints)
                .setInitialDelay(8, HOURS)
                .build()
        //WorkManager.getInstance(applicationContext).enqueue(TrophiesUpdateRequest)

        return future
    }

    private class ObservableInt(private var value: Int = 0) {

        fun getValue() = value
        fun setValue(value: Int, action: (Int) -> (Unit)) {
            this.value = value
            action(value)
        }
    }

    private class ObservableInt1(private var value: Int = 0) {

        fun getValue() = value
        fun setValue(value: Int, action: (Int) -> (Unit)) {
            this.value = value
            action(value)
        }
    }
}