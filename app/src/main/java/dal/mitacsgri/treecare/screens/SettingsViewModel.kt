package dal.mitacsgri.treecare.screens

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

/**
 * Created by Devansh on 24-06-2019
 */

class SettingsViewModel(
    sharedPrefRepository: SharedPreferencesRepository,
    firestoreRepository: FirestoreRepository
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
}