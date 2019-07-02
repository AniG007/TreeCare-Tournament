package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.koin.core.KoinComponent
import org.koin.core.inject

class UploadUserChallengeDataWorker(appContext: Context, workerParams: WorkerParameters)
    :Worker(appContext, workerParams), KoinComponent {

    private val firestoreRepository: FirestoreRepository by inject()
    private val sharedPrefsRepository: SharedPreferencesRepository by inject()

    override fun doWork(): Result {
        val user = sharedPrefsRepository.user

        firestoreRepository.updateUserData(user.uid,
            mapOf("currentChallenges" to user.currentChallenges))
            .addOnSuccessListener {
                Log.d("Worker", "User data upload success")
            }
            .addOnFailureListener {
                Log.e("Worker", "User data upload failed")
            }

        return Result.success()
    }
}