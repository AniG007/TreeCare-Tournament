package dal.mitacsgri.treecare.screens.gamesettings

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.data.User
import org.joda.time.DateTime
import org.joda.time.Days

/**
 * Created by Devansh on 24-06-2019
 */

class SettingsViewModel(
    private val sharedPrefRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    val dailyGoal = MutableLiveData<Int>().default(5000)

    fun increaseDailyGoal() {
        dailyGoal.value = dailyGoal.value?.plus(1000)
    }

    fun decreaseDailyGoal() {
        val v = dailyGoal.value as Int
        if (v > 5000) {
            dailyGoal.value = dailyGoal.value?.minus(1000)
            Log.d("DailyGoal", dailyGoal.value?.toString())
        }
    }

    fun storeUpdatedStepGoal(updatedStepGoal: Int) {
        val user = sharedPrefRepository.user
        //Actually, this will be the date from which the new goal will be applicable
        val lastGoalChangeTime = DateTime().plusDays(1).withTimeAtStartOfDay().millis
        //Set the goal as updated daily goal for the next day
        user.dailyGoalMap[lastGoalChangeTime.toString()] = updatedStepGoal
        user.lastGoalChangeTime = lastGoalChangeTime

        completeUserDailyGoalMap(user, updatedStepGoal)

        sharedPrefRepository.user = user
        sharedPrefRepository.storeDailyStepsGoal(updatedStepGoal)
        firestoreRepository.storeUser(user)
    }

    //This function will add missing daily step goal dal.mitacsgri.treecare.data if user does not opens app for many days
    private fun completeUserDailyGoalMap(user: User, updatedStepGoal: Int) {
        val dailyGoalMap = user.dailyGoalMap

        var keysList = mutableListOf<Long>()
        dailyGoalMap.keys.forEach {
            keysList.add(it.toLong())
        }
        keysList = keysList.sorted().toMutableList()

        //This needs to be done only for the last element, because the map is done every time,
        //So every time only last element needs to be checked
        val lastTime = keysList[keysList.size-1]
        val days = Days.daysBetween(DateTime(lastTime), DateTime(user.lastGoalChangeTime)).days

        val oldGoal = dailyGoalMap[lastTime.toString()]

        for (i in 1..days) {
            val key = DateTime(lastTime).plusDays(i).withTimeAtStartOfDay().millis.toString()
            dailyGoalMap[key] = oldGoal!!
        }

        dailyGoalMap[user.lastGoalChangeTime.toString()] = updatedStepGoal
    }
}