package dal.mitacsgri.treecare.screens.createchallenge

import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import java.util.*

/**
 * Created by Devansh on 27-06-2019
 */

class CreateChallengeViewModel(
    sharedPrefsRepository: SharedPreferencesRepository,
    firestoreRepository: FirestoreRepository
    ): ViewModel() {

    fun getCurrentDateDestructured(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return Triple(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    fun getGoalInputHint(checkedId: Int) =
            when(checkedId) {
                R.id.optionDailyGoalBased -> "Daily steps goal"
                R.id.optionAggregateBased -> "Total steps"
                else -> ""
    }

    fun getDateText(year: Int, monthOfYear: Int, dayOfMonth: Int) = "$dayOfMonth / ${monthOfYear+1} / $year"
}