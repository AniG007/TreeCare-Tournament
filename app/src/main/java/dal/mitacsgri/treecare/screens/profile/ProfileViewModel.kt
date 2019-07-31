package dal.mitacsgri.treecare.screens.profile

import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class ProfileViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository
) : ViewModel() {

    fun getUserPhotoUrl() = sharedPrefsRepository.user.photoUrl

    fun getUserFullName() = sharedPrefsRepository.user.name

    fun getDailyGoalCompletionStreakCount() = sharedPrefsRepository.dailyGoalStreak

    fun getDailyGoalStreakText() = buildSpannedString {
        append("Daily goal achieved ")
        bold {
            append("consecutively for ${getDailyGoalCompletionStreakCount()} " +
                    if (getDailyGoalCompletionStreakCount() > 1) "days" else "day")
        }
        append(" in ")
        bold {
            append("last ${sharedPrefsRepository.currentDayOfWeek + 1} " +
                    if ((sharedPrefsRepository.currentDayOfWeek + 1) > 1) "days" else "day")
        }
    }
}
