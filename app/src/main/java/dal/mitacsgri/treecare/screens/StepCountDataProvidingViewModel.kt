package dal.mitacsgri.treecare.screens

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.Days
import java.util.*

/**
 * Created by Devansh on 20-06-2019
 */

class StepCountDataProvidingViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository)
    : ViewModel() {

    val isLoginDone = sharedPrefsRepository.isLoginDone

    //This variable is accessed synchronously. The moment its value reaches 2, we move to new fragment
    //Value 2 means both the steps counts have been obtained
    val stepCountDataFetchedCounter = MutableLiveData<Int>().default(0)

    fun storeDailyStepsGoal(goal: Int) {
        sharedPrefsRepository.storeDailyStepsGoal(goal)
    }

    fun resetDailyGoalCheckedFlag() {
        //Will execute only once in each day, when the app is opened for thr first time in the day
        if (sharedPrefsRepository.lastOpenedDayPlus1 < Date().time) {
            sharedPrefsRepository.isDailyGoalChecked = 0
            sharedPrefsRepository.isGoalCompletionSteakChecked = false

            val timeToStore = DateTime().plusDays(1).withTimeAtStartOfDay().millis

            Log.v("Current time: ", Date().time.toString())
            Log.v("Time to store: ", timeToStore.toString())

            sharedPrefsRepository.apply {
                lastOpenedDayPlus1 = timeToStore
                lastLeafCount = currentLeafCount
            }

        }
    }

    fun accessStepCountDataUsingApi() {

        stepCountRepository.apply {

            //Get aggregate step count up to the last day
            getStepCountDataOverARange(
                DateTime(sharedPrefsRepository.user.firstLoginTime).withTimeAtStartOfDay().millis,
                DateTime().withTimeAtStartOfDay().millis
            ) {

                if (!sharedPrefsRepository.isGoalCompletionSteakChecked) {
                    calculateFruitsOnTree(it)
                    calculateDailyGoalStreak(sharedPrefsRepository.currentDayOfWeek, it)
                }

                increaseStepCountDataFetchedCounter()

                val dailyGoalMap = sharedPrefsRepository.user.dailyGoalMap

                //Updated the daily goal stored in SharedPrefs to display in Unity
                if (sharedPrefsRepository.isDailyGoalChecked == 0) {
                    sharedPrefsRepository.storeDailyStepsGoal(
                        dailyGoalMap[DateTime().withTimeAtStartOfDay().millis.toString()] ?: 5000)
                }

                expandDailyGoalMapIfNeeded(sharedPrefsRepository.user)

                var totalLeafCountTillLastDay = 0
                it.forEach { (date, stepCount) ->
                    val goal = sharedPrefsRepository.user.dailyGoalMap[date.toString()]
                    totalLeafCountTillLastDay +=
                        calculateLeafCountFromStepCount(stepCount, goal!!)
                    Log.d("Date: $date", "StepCount: $stepCount")
                }

                Log.d("Last day leaf count", totalLeafCountTillLastDay.toString())

                var currentLeafCount = totalLeafCountTillLastDay
                //Add today's leaf count to leafCountTillLastDay
                //Call needs to be made here because it uses dal.mitacsgri.treecare.data from previous call
                getTodayStepCountData {
                    currentLeafCount += it/1000
                    sharedPrefsRepository.currentLeafCount = currentLeafCount

                    sharedPrefsRepository.storeDailyStepCount(it)
                    Log.d("DailyStepCount", it.toString())
                    increaseStepCountDataFetchedCounter()
                }
            }
        }
    }

    private inline fun increaseStepCountDataFetchedCounter() {
        synchronized(stepCountDataFetchedCounter) {
            stepCountDataFetchedCounter.value = stepCountDataFetchedCounter.value?.plus(1)
            Log.d("Counter value", stepCountDataFetchedCounter.value.toString())
        }
    }

    private fun calculateLeafCountFromStepCount(stepCount: Int, dailyGoal: Int): Int {
        var leafCount = stepCount / 1000
        if (stepCount < dailyGoal) {
            leafCount -= Math.ceil((dailyGoal - stepCount) / 1000.0).toInt()
            if (leafCount < 0) leafCount = 0
        }
        return leafCount
    }

    private fun calculateFruitsOnTree(stepCountMap: Map<Long, Int>) {

        var currentDay = sharedPrefsRepository.currentDayOfWeek
        val goalAchievedStreak = arrayOf(false, false, false, false, false, false, false)
        val fullStreak = arrayOf(true, true, true, true, true, true, true)

        stepCountMap.forEach { (_, stepCount) ->
            goalAchievedStreak[currentDay] =
                stepCount >= sharedPrefsRepository.getDailyStepsGoal()

            currentDay = (currentDay + 1) % 7

            if (currentDay == 0) {
                sharedPrefsRepository.lastFruitCount = sharedPrefsRepository.currentFruitCount
                if (goalAchievedStreak.contentEquals(fullStreak))
                    sharedPrefsRepository.currentFruitCount += 1
                else {
                    sharedPrefsRepository.currentFruitCount -= 1
                }
            }
        }

        sharedPrefsRepository.currentDayOfWeek = currentDay
    }

    private fun expandDailyGoalMapIfNeeded(user: User) {
        val dailyGoalMap = user.dailyGoalMap
        var keysList = mutableListOf<Long>()
        dailyGoalMap.keys.forEach {
            keysList.add(it.toLong())
        }
        keysList = keysList.sorted().toMutableList()

        val lastTime = keysList[keysList.size-1]
        val days = Days.daysBetween(DateTime(lastTime), DateTime()).days

        val oldGoal = dailyGoalMap[lastTime.toString()]

        for (i in 1..days) {
            val key = DateTime(lastTime).plusDays(i).withTimeAtStartOfDay().millis.toString()
            user.dailyGoalMap[key] = oldGoal!!
        }

        sharedPrefsRepository.user = user
    }

    private fun calculateDailyGoalStreak(currentDay: Int, stepCountMap: Map<Long, Int>) {
        val mapSize = stepCountMap.size
        val mapKeys = stepCountMap.keys.sorted()
        var streakCount = 0
        val dailyGoal = sharedPrefsRepository.getDailyStepsGoal()
        val goalStreakStringBuilder = StringBuilder(sharedPrefsRepository.dailyGoalStreakString)

        for (i in 0..6) {
            goalStreakStringBuilder[i] = '0'
        }

        for (i in (mapSize - (currentDay)) until mapSize) {
            if (stepCountMap[mapKeys[i]] ?: error("No data for key ${mapKeys[i]}") >= dailyGoal) {
                streakCount++
            } else {
                streakCount = 0
                break
            }
        }

        val startIndex = mapSize - currentDay
        for (i in (mapSize - (currentDay)) until mapSize) {
            if (stepCountMap[mapKeys[i]] ?: error("No data for key ${mapKeys[i]}") >= dailyGoal) {
                goalStreakStringBuilder[i - startIndex] = '1'
            }
        }

        sharedPrefsRepository.dailyGoalStreak = streakCount
        sharedPrefsRepository.dailyGoalStreakString = goalStreakStringBuilder.toString()
        sharedPrefsRepository.isGoalCompletionSteakChecked = true
    }
}