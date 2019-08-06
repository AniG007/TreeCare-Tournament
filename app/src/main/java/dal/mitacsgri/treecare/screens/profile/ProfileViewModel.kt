package dal.mitacsgri.treecare.screens.profile

import android.content.Context
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.UserChallengeTrophies
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

    fun getCurrentWeekDayForStreak() = sharedPrefsRepository.currentDayOfWeek

    fun getGoalCompletionStreakData(): Array<Boolean> {
        val goalCompletionString = sharedPrefsRepository.dailyGoalStreakString ?: ""
        val boolArray = arrayOf(false, false, false, false, false, false, false)

        for (i in 0 until goalCompletionString.length) {
            boolArray[i] = goalCompletionString[i] == '1'
        }

        return boolArray
    }

    fun getDailyGoalStreakText() = buildSpannedString {
        append("Daily goal achieved ")
        bold {
            append("for ${getDailyGoalCompletionStreakCount()} " +
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
                val userTrophies = it.toObject<UserChallengeTrophies>()
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

    fun logout(context: Context) {
        sharedPrefsRepository.clearSharedPrefs()
        Fitness.getConfigClient(context, GoogleSignIn.getLastSignedInAccount(context)!!).disableFit()
        //TODO: Maybe cancellation of jobs is needed
    }
}
