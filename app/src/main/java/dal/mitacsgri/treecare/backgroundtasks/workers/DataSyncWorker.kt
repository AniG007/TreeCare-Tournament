package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.concurrent.TimeUnit

class DataSyncWorker(appContext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appContext, workerParams), KoinComponent {

    private val sharedPrefRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()

    override fun startWork(): ListenableFuture<Result> {

        val future = SettableFuture.create<Result>()
        WorkManager.getInstance().cancelUniqueWork("teamWorker")
        WorkManager.getInstance().cancelUniqueWork("challengeWorker")
        if(sharedPrefRepository.team.currentTournaments.isEmpty()){

            firestoreRepository.getUserData(sharedPrefRepository.user.uid).addOnSuccessListener {
                val user = it.toObject<User>()
                val userTeam = user?.currentTeams.toString().removeSurrounding("[", "]")
                if (userTeam.isNotEmpty()) {
                    firestoreRepository.getTeam(userTeam).addOnSuccessListener {
                        val team = it.toObject<Team>()
                        val teamTournaments = team?.currentTournaments
                        for (tournament in teamTournaments!!) {
                            synchronized(sharedPrefRepository) {
                                sharedPrefRepository.user = user!!
                                sharedPrefRepository.team = team
                            }
                        }
                    }
                }
            }
        }

        else{
            firestoreRepository.getUserData(sharedPrefRepository.user.uid).addOnSuccessListener {
                val user = it.toObject<User>()
                val userTournaments = user?.currentTournaments
                val prefTournaments = sharedPrefRepository.user.currentTournaments
                val userTeam = user?.currentTeams.toString().removeSurrounding("[", "]")
                if (userTeam.isNotEmpty()) {
                    if (userTournaments?.size != prefTournaments.size) {

                        firestoreRepository.getTeam(userTeam).addOnSuccessListener {
                            val team = it.toObject<Team>()
                            val teamTournaments = team?.currentTournaments
                            synchronized(sharedPrefRepository.team) {
                                val userData = sharedPrefRepository.user
                                userData.currentTournaments = userTournaments!!
                                sharedPrefRepository.user = userData

                                val teamData = sharedPrefRepository.team
                                teamData.currentTournaments = teamTournaments!!
                                sharedPrefRepository.team = teamData
                            }
                        }
                    }
                }
            }
        }
        startWorkers()
        return future
    }

    fun startWorkers(){
        val mConstraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val updateTeamDataRequest =
            PeriodicWorkRequestBuilder<UpdateTeamDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(mConstraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

        val updateUserChallengeDataRequest =
            PeriodicWorkRequestBuilder<UpdateUserChallengeDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(mConstraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance().enqueueUniquePeriodicWork(
            "teamWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateTeamDataRequest,
        )

        WorkManager.getInstance().enqueueUniquePeriodicWork(
            "challengeWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateUserChallengeDataRequest,
        )

    }
}