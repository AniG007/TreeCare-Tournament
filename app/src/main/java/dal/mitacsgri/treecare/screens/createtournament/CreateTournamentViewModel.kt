package dal.mitacsgri.treecare.screens.createtournament

import android.text.Editable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import java.util.*

/**
 * Created by Anirudh on 23-04-2020
 */

class CreateTournamentViewModel (
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    var isNameValid = false
    var isGoalValid = false
    var isStartDateValid = false
    var isEndDateValid = false
    var isTeamSizeValid = false

    val messageLiveData = MutableLiveData<String>()
    val isFullDataValid = MutableLiveData<Boolean>()

    private lateinit var startdate: Calendar
    private lateinit var endDate: Calendar

    fun getCurrentDateDestructured(): Triple<Int, Int, Int> {
       // startdate = Calendar.getInstance()
        endDate = Calendar.getInstance()
        return Triple(endDate.get(Calendar.DAY_OF_MONTH), endDate.get(Calendar.MONTH), endDate.get(Calendar.YEAR))
    }

//    fun getStartDateText(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
//        storeStartDate(dayOfMonth, monthOfYear, year)
//        return "$dayOfMonth / ${monthOfYear+1} / $year"
//    }

    fun getDateText(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
        storeEndDate(dayOfMonth, monthOfYear, year)
        return "$dayOfMonth / ${monthOfYear+1} / $year"
    }

    //fun getRegexToMatchStepsGoal() = Regex("([5-9][0-9]*(000)+)|([1-9]+0*0000)")
    fun getRegexToMatchStepsGoal() = Regex("([1-9]+0*000)") //For checking if goalsteps > = 10,000 and not more than 90,000

    fun getRegexToMatchTeamSize() = Regex("[2-9]|10")

    fun isTeamLimitValid(size: Int):Boolean{ return size<=10 }

    fun areAllInputFieldsValid(): Boolean {
        isFullDataValid.value = isNameValid and isGoalValid and isEndDateValid and isTeamSizeValid
        return isFullDataValid.value ?: false
    }



    fun createTournament (name: Editable?, description: Editable?,type: Int,goal: Editable?, teamLimit:Editable?, action: (Boolean) -> Unit) {
        if (areAllInputFieldsValid()) {
            firestoreRepository.getTournament(name.toString())
                .addOnSuccessListener {
                    if (it.exists()) {
                    messageLiveData.value = "Tournament already exists"
                    action(it.exists())
                    return@addOnSuccessListener
                    }


//                else if(Timestamp.now() == Timestamp(startdate.timeInMillis / 1000, 0)){
//                    //Setting the tournament as active if the tourney starts on the same date as the creation day
//                    // But currently, FireBase rules doesn't have the capability to check for dates and update accordingly
//                    // so we are setting the tournament to active in both cases and start date has been removed as of now
//                    firestoreRepository.storeTournament(
//                        Tournament(
//                            name = name.toString(),
//                            description = description.toString(),
//                            //type = type,
//                            dailyGoal = goal.toString().toInt(),
//                            startTimestamp = Timestamp(startdate.timeInMillis / 1000, 0),
//                            finishTimestamp = Timestamp(endDate.timeInMillis / 1000, 0),
//                            creationTimestamp = Timestamp.now(),
//                            creatorName = sharedPrefsRepository.user.name
//                                    + " (${sharedPrefsRepository.user.email.split("@")[0]})",
//                            creatorUId = sharedPrefsRepository.user.uid,
//                            //isActive = true,
//                            active = true,
//                            exist = true
//                        )
//                    ) {
//                        action(it)
//                        messageLiveData.value = if (it) "Tournament created successfully"
//                        else "Tournament creation failed"
//                    }
//                }

                else {
                    firestoreRepository.storeTournament(
                        Tournament(
                            name = name.toString(),
                            description = description.toString(),
                            //type = type,
                            dailyGoal = goal.toString().toInt(),
                            //startTimestamp = Timestamp(startdate.timeInMillis / 1000, 0),
                            finishTimestamp = Timestamp(endDate.timeInMillis / 1000, 0),
                            creationTimestamp = Timestamp.now(),
                            creatorName = sharedPrefsRepository.user.name
                                    + " (${sharedPrefsRepository.user.email.split("@")[0]})",
                            creatorUId = sharedPrefsRepository.user.uid,
                            //isActive = false,
                            active = true,
                            exist = true,
                            teamLimit = teamLimit.toString().toInt()
                            //active = true
                        )
                    ) {
                        action(it)
                        messageLiveData.value = if (it) "Tournament created successfully"
                        else "Tournament creation failed"
                    }
                }
            }
                .addOnFailureListener {
                    messageLiveData.value = "Failure"
                }
        }
        else{
            Log.d("Test","Tournament creation failed")
        }
    }

//    private fun storeStartDate(dayOfMonth: Int, monthOfYear: Int, year: Int) {
//        startdate = Calendar.getInstance()
//        startdate.apply {
//            set(Calendar.DAY_OF_MONTH, dayOfMonth)
//            set(Calendar.MONTH, monthOfYear)
//            set(Calendar.YEAR, year)
//            set(Calendar.HOUR_OF_DAY, 23)
//            set(Calendar.MINUTE, 59)
//            set(Calendar.SECOND, 59)
//        }
//    }

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
