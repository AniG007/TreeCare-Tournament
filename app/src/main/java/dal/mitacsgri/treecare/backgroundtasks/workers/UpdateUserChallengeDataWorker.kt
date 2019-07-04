package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_AGGREGATE_BASED
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.toJson
import dal.mitacsgri.treecare.model.UserChallenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.koin.core.KoinComponent
import org.koin.core.inject

class UpdateUserChallengeDataWorker(appContext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appContext, workerParams), KoinComponent {

    private val stepCountRepository: StepCountRepository by inject()
    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()

    private lateinit var mClient: GoogleApiClient

    override fun startWork(): ListenableFuture<Result> {
        val future = SettableFuture.create<Result>()

        val user = sharedPrefsRepository.user

        user.currentChallenges.forEach { (_, challengeJson) ->
            val challenge = Gson().fromJson(challengeJson, UserChallenge::class.java)
            setupFitApiToGetData(applicationContext)

            if (challenge.type == CHALLENGE_TYPE_DAILY_GOAL_BASED) {
                stepCountRepository.getTodayStepCountData(mClient) {
                    challenge.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
                    storeUserChallengeDataInSharedPrefs(challenge)
                }
            } else if (challenge.type == CHALLENGE_TYPE_AGGREGATE_BASED) {
                stepCountRepository.getAggregateStepCountDataOverARange(mClient,
                    DateTime(challenge.joinDate).withTimeAtStartOfDay().millis,
                    DateTime().millis) {
                        challenge.totalSteps =  it
                        storeUserChallengeDataInSharedPrefs(challenge)
                }
            }
        }
        updateUserChallengeDataInFirestore(future)

        return future
    }

    private fun setupFitApiToGetData(context: Context) {
        val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
            Log.e("Connection failed", it.toString())
        }

        mClient = GoogleApiClient.Builder(context)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .setAccountName(FirebaseAuth.getInstance().currentUser?.email)
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                }
                override fun onConnectionSuspended(p0: Int) {}
            })
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()
    }



    private fun storeUserChallengeDataInSharedPrefs(challenge: UserChallenge) {
        synchronized(sharedPrefsRepository.user) {
            val user = sharedPrefsRepository.user
            user.currentChallenges[challenge.name] = challenge.toJson<UserChallenge>()
            sharedPrefsRepository.user = user
        }
    }

    private fun updateUserChallengeDataInFirestore(future: SettableFuture<Result>) {
        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
            mapOf("currentChallenges" to sharedPrefsRepository.user.currentChallenges))
            .addOnSuccessListener {
                Log.d("Worker", "User data upload success")
                future.set(Result.success())
            }
            .addOnFailureListener {
                Log.e("Worker", "User data upload failed")
                future.set(Result.failure())
            }
    }
}
