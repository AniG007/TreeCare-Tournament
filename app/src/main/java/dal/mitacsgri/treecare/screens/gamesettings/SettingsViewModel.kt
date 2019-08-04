package dal.mitacsgri.treecare.screens.gamesettings

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
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
    val settingsChanged = MutableLiveData<Boolean>().default(false)

    var hasSettingsChanged
        get() = settingsChanged.value ?: true
        set(value) {
            settingsChanged.value = value
        }

    fun getCurrentVolume() = (sharedPrefRepository.volume*10).toInt()

    fun getCurrentDailyStepsGoal() = sharedPrefRepository.user
        .dailyGoalMap[DateTime().plusDays(1).millis.toString()] ?: 5000

    fun saveSettings(volume: Int, updatedStepGoal: Int, action: () -> Unit) {
        val oldVolume = sharedPrefRepository.volume
        val newVolume = volume / 10f

        sharedPrefRepository.volume = newVolume
        storeUpdatedStepGoalInPrefs(updatedStepGoal)

        //Doing this to prevent database update when changing only volume
        if (oldVolume != newVolume) {
            firestoreRepository.storeUser(sharedPrefRepository.user)
                .addOnSuccessListener {
                    action()
                }
                .addOnFailureListener {
                    Log.d("Goal update in DB", "failed: $it")
                }
        } else
            action()
    }

    private fun storeUpdatedStepGoalInPrefs(updatedStepGoal: Int) {
        val user = sharedPrefRepository.user
        //Actually, this will be the date from which the new goal will be applicable
        val lastGoalChangeTime = DateTime().plusDays(1).withTimeAtStartOfDay().millis
        //Set the goal as updated daily goal for the next day
        user.dailyGoalMap[lastGoalChangeTime.toString()] = updatedStepGoal
        user.lastGoalChangeTime = lastGoalChangeTime

        completeUserDailyGoalMap(user, updatedStepGoal)

        sharedPrefRepository.user = user
        sharedPrefRepository.storeDailyStepsGoal(updatedStepGoal)
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

        //The days for which no change in daily goal was made by the user are filled by the last daily goal
        for (i in 1..days) {
            val key = DateTime(lastTime).plusDays(i).withTimeAtStartOfDay().millis.toString()
            dailyGoalMap[key] = oldGoal!!
        }

        dailyGoalMap[user.lastGoalChangeTime.toString()] = updatedStepGoal
    }
}
