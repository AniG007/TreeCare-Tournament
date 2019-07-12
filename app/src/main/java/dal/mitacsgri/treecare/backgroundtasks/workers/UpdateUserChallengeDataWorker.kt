package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_AGGREGATE_BASED
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.model.User
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

    override fun startWork(): ListenableFuture<Result> {
        val future = SettableFuture.create<Result>()

        val user = sharedPrefsRepository.user

        user.currentChallenges.forEach { (_, challenge) ->

            challenge.leafCount = calculateLeavesForChallenge(challenge)
            challenge.challengeGoalStreak = getChallengeGoalStreakForUser(challenge, user)

            if (challenge.type == CHALLENGE_TYPE_DAILY_GOAL_BASED) {
                stepCountRepository.getTodayStepCountData {
                    challenge.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
                    storeUserChallengeDataInSharedPrefs(challenge)
                }
            } else if (challenge.type == CHALLENGE_TYPE_AGGREGATE_BASED) {
                stepCountRepository.getAggregateStepCountDataOverARange(
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

    private fun calculateLeavesForChallenge(challenge: UserChallenge) =
        when(challenge.type) {
            CHALLENGE_TYPE_AGGREGATE_BASED -> {
                challenge.totalSteps/1000
            }

            CHALLENGE_TYPE_DAILY_GOAL_BASED -> {
                var leafCount = challenge.leafCount
                val goal = challenge.goal
                challenge.dailyStepsMap.forEach {(_, steps) ->
                    leafCount += (steps - (if (steps < goal) goal else 0))/1000
                }
                leafCount
            }

            else -> 0
        }

    private fun getChallengeGoalStreakForUser(challenge: UserChallenge, user: User): Int {
        val userChallengeData = user.currentChallenges[challenge.name]!!
        var streakCount = 0

        userChallengeData.dailyStepsMap.forEach { (date, stepCount) ->
            //This check prevents resetting streak count if goal is yet to be met today
            if (date.toInt() < DateTime().withTimeAtStartOfDay().millis) {
                if (stepCount >= challenge.goal) streakCount++
                else streakCount = 0
            }
        }
        return streakCount
    }

    private fun storeUserChallengeDataInSharedPrefs(challenge: UserChallenge) {
        synchronized(sharedPrefsRepository.user) {
            val user = sharedPrefsRepository.user
            user.currentChallenges[challenge.name] = challenge
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
