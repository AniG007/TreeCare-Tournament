package dal.mitacsgri.treecare.screens.profile

import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.UserChallenegeTrophies
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class ProfileViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
) : ViewModel() {

    val trophiesCountData = MutableLiveData<Triple<String, String, String>>()
        .default(Triple("0", "0", "0"))

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

    fun getTrophiesCount() {
        firestoreRepository.getTrophiesData(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                val userTrophies = it.toObject<UserChallenegeTrophies>()
                userTrophies?.let {
                    trophiesCountData.value = Triple(
                        userTrophies.gold.size.toString(),
                        userTrophies.silver.size.toString(),
                        userTrophies.bronze.size.toString())
                }
            }
            .addOnFailureListener {
                Log.d("Profile", "Failed to obtain trophies data")
            }
    }
}
