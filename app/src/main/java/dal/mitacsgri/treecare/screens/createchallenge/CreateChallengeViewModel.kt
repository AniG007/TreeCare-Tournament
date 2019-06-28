package dal.mitacsgri.treecare.screens.createchallenge

import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import java.util.*

/**
 * Created by Devansh on 27-06-2019
 */

class CreateChallengeViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    var isNameValid = false
    var isGoalValid = false
    var isEndDateValid = false

    val isGoalOptionSelected = MutableLiveData<Boolean>()
    val messageLiveData = MutableLiveData<String>()
    val isFullDataValid = MutableLiveData<Boolean>()

    private lateinit var endDate: Calendar

    fun getCurrentDateDestructured(): Triple<Int, Int, Int> {
        endDate = Calendar.getInstance()
        return Triple(endDate.get(Calendar.DAY_OF_MONTH), endDate.get(Calendar.MONTH), endDate.get(Calendar.YEAR))
    }

    fun getGoalInputHint(checkedId: Int) =
            when(checkedId) {
                R.id.optionDailyGoalBased -> "Daily steps goal"
                R.id.optionAggregateBased -> "Total steps"
                else -> ""
    }

    fun getGoalType(checkedId: Int) =
            when(checkedId) {
                R.id.optionDailyGoalBased -> 0
                R.id.optionAggregateBased -> 1
                else -> 2
            }

    fun getDateText(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
        storeEndDate(dayOfMonth, monthOfYear, year)
        return "$dayOfMonth / ${monthOfYear+1} / $year"
    }

    fun getRegexToMatchStepsGoal() = Regex("([5-9][0-9]*(000)+)|([1-9]+0*0000)")

    fun areAllInputFieldsValid(): Boolean {
        if(isGoalOptionSelected.value == null)
            isGoalOptionSelected.value = false
        isFullDataValid.value = isNameValid and (isGoalOptionSelected.value ?: false) and isGoalValid and isEndDateValid
        return isFullDataValid.value ?: false
    }

    fun createChallenge(name: Editable?, description: Editable?, type: Int, goal: Editable?) {
        if (areAllInputFieldsValid()) {
            firestoreRepository.getChallenge(name.toString()).addOnSuccessListener {
                if (it.exists()) {
                    messageLiveData.value = "Challenge already exists"
                    return@addOnSuccessListener
                } else {
                    firestoreRepository.storeChallenge(
                        Challenge(name = name.toString(),
                            description = description.toString(),
                            type = type,
                            goal = goal.toString().toInt(),
                            finishTimestamp = Timestamp(endDate.timeInMillis/1000, 0),
                            creationTimestamp = Timestamp.now(),
                            creatorName = sharedPrefsRepository.user.name,
                            creatorUId = sharedPrefsRepository.user.uid,
                            isExist = true,
                            isActive = true)) {
                        messageLiveData.value = if (it) "Challenge created successfully"
                                                else "Challenge creation failed"
                    }
                }
            }
            .addOnFailureListener {
                messageLiveData.value = "Failure"
            }
        }
    }

    private fun storeEndDate(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        endDate = Calendar.getInstance()
        endDate.apply {
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.MONTH, monthOfYear)
            set(Calendar.YEAR, year)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
    }
}