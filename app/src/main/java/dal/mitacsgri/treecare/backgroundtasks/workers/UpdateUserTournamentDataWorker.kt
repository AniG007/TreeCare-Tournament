package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import calculateLeafCountFromStepCount
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.Timestamp
import dal.mitacsgri.treecare.consts.TOURNAMENT_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.toDateTime
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.Days
import org.koin.core.KoinComponent
import org.koin.core.inject

class UpdateUserTournamentDataWorker(appContext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appContext, workerParams), KoinComponent {

    private val stepCountRepository: StepCountRepository by inject()
    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()

    override fun startWork(): ListenableFuture<Result> {
        val future = SettableFuture.create<Result>()
        val user = sharedPrefsRepository.user

        user.currentTournaments.forEach { (_, tourney) ->

            val endTimeMillis = tourney.endDate.toDateTime().millis
            //Two condition checks are applied because the 'isActive' variable is set only after
            //the dialog has been displayed. The second condition check prevents update of Tournament step count
            //in the database even when the dialog has not been displayed
            if (tourney.isActive && endTimeMillis > DateTime().millis) {
                //if (tournament.type == TOURNAMENT_TYPE_DAILY_GOAL_BASED) {
                stepCountRepository.getTodayStepCountData {
                    tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
                    updateAndStoreUserTournamentDataInSharedPrefs(tourney, user)
                    //}
                }
            }
        }
        updateUserTournamentDataInFirestore(future)

        return future
    }

    private fun getTournamentGoalStreakForUser(tournament: UserTournament, user: User): Int {
        val userTournamentData = user.currentTournaments[tournament.name]!!
        var streakCount = 0

        userTournamentData.dailyStepsMap.forEach { (date, stepCount) ->
            //This check prevents resetting streak count if goal is yet to be met today
            if (date.toLong() < DateTime().withTimeAtStartOfDay().millis) {
                if (stepCount >= tournament.goal) streakCount++
                else streakCount = 0
            }
        }
        return streakCount
    }

    private fun updateAndStoreUserTournamentDataInSharedPrefs(tournament: UserTournament, user: User) {
        tournament.leafCount = getTotalLeafCountForTournament(tournament)
        tournament.fruitCount = getTotalFruitCountForTournament(tournament)
        tournament.tournamentGoalStreak = getTournamentGoalStreakForUser(tournament, user)
        tournament.lastUpdateTime = Timestamp.now()

        var totalSteps = 0
        tournament.dailyStepsMap.forEach { (time, steps) ->
            totalSteps += steps
        }
        tournament.totalSteps = totalSteps

        synchronized(sharedPrefsRepository.user) {
            val user = sharedPrefsRepository.user
            user.currentTournaments[tournament.name] = tournament
            sharedPrefsRepository.user = user
        }
    }

    private fun updateUserTournamentDataInFirestore(future: SettableFuture<Result>) {

        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
            mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments))
            .addOnSuccessListener {
                Log.d("Worker", "User data upload success")
                future.set(Result.success())
            }
            .addOnFailureListener {
                Log.e("Worker", "User data upload failed")
                future.set(Result.failure())
            }
    }

    private fun getTotalLeafCountForTournament(tournament: UserTournament): Int {
        val stepsMap = tournament.dailyStepsMap
        val goal = tournament.goal
        var leafCount = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
            leafCount += calculateLeafCountFromStepCount(stepsMap[keys[i]]!!, goal)
        }
        leafCount += stepsMap[keys[keys.size-1]]!! / 1000

        return leafCount
    }

    private fun getTotalFruitCountForTournament(tournament: UserTournament): Int {
        val joinDate = DateTime(tournament.joinDate)
        val currentDate = DateTime()
        val days = Days.daysBetween(joinDate, currentDate).days
        val weeks = Math.ceil(days/7.0).toInt()

        var fruitCount = 0

        var weekStartDate = joinDate
        var newWeekDate = weekStartDate.plusWeeks(1)
        var mapPartition: Map<String, Int>
        for (i in 0 until weeks) {
            mapPartition = tournament.dailyStepsMap.filter {
                val keyAsLong = it.key.toLong()
                keyAsLong >= weekStartDate.millis && keyAsLong < newWeekDate.millis
            }
            fruitCount += calculateFruitCountForWeek(tournament, mapPartition)
            weekStartDate = newWeekDate
            newWeekDate = weekStartDate.plusWeeks(1)
        }

        return fruitCount
    }

    /**
     * @param tournament UserTournament
     * @param stepCountMap Step count map for a week
     * @return +1, 0 or -1 as fruit count
     */
    private fun calculateFruitCountForWeek(tournament: UserTournament, stepCountMap: Map<String, Int>): Int {
        var currentDay = 0
        val goalAchievedStreak = arrayOf(false, false, false, false, false, false, false)
        val fullStreak = arrayOf(true, true, true, true, true, true, true)

        if (stepCountMap.size < 7) return 0

        stepCountMap.forEach { (_, stepCount) ->
            goalAchievedStreak[currentDay] =
                stepCount >= tournament.goal
            currentDay++
        }

        return if (goalAchievedStreak.contentEquals(fullStreak)) 1 else -1
    }
}
