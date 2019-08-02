package dal.mitacsgri.treecare.backgroundtasks.jobs

import android.util.Log
import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobRequest
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.UserChallengeTrophies
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.concurrent.TimeUnit

class TrophiesUpdateJob: DailyJob(), KoinComponent {

    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()

    private var challengesCounter = ObservableInt()

    companion object {

        const val TAG = "TrophiesUpdateJob"

        fun scheduleJob() {
            DailyJob.schedule(JobRequest.Builder(TAG),
                TimeUnit.HOURS.toMillis(5), TimeUnit.HOURS.toMillis(6))
        }
    }

    override fun onRunDailyJob(p0: Params): DailyJobResult {
        val currentChallenges = sharedPrefsRepository.user.currentChallenges
        val userTrophies = UserChallengeTrophies()

        currentChallenges.forEach { (name, userChallenge) ->
            firestoreRepository.getChallenge(name).addOnSuccessListener {
                val challenge = it.toObject<Challenge>()
                if (challenge != null && !challenge.active) {
                    when(challenge.players.indexOf(sharedPrefsRepository.user.uid)) {
                        0 -> userTrophies.gold.add(name)
                        1 -> userTrophies.silver.add(name)
                        2 -> userTrophies.bronze.add(name)
                    }

                    challengesCounter.setValue(challengesCounter.getValue() + 1) {
                        if (it == currentChallenges.size) {
                            firestoreRepository.storeTrophiesData(sharedPrefsRepository.user.uid, userTrophies)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Success")
                                }
                                .addOnFailureListener {
                                    Log.d(TAG, it.toString())
                                }
                        }
                    }
                }
            }
        }

        return DailyJobResult.SUCCESS
    }

    private class ObservableInt(private var value: Int = 0) {

        fun getValue() = value

        fun setValue(value: Int, action: (Int) -> (Unit)) {
            this.value = value
            action(value)
        }
    }
}