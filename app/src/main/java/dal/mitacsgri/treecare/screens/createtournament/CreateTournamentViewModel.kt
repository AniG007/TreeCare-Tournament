package dal.mitacsgri.treecare.screens.createtournament

import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime
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
    val buttonVis = MutableLiveData<Boolean>().default(false)

    private lateinit var startDate: Calendar
    private lateinit var endDate: Calendar
    val currentDate: Calendar = Calendar.getInstance()

    fun getCurrentDateDestructured(): Triple<Int, Int, Int> {
        endDate = Calendar.getInstance()
        return Triple(endDate.get(Calendar.DAY_OF_MONTH), endDate.get(Calendar.MONTH), endDate.get(Calendar.YEAR))
    }

    fun getCurrentDateDestructured2(): Triple<Int, Int, Int> {
        startDate = Calendar.getInstance()
        return Triple(startDate.get(Calendar.DAY_OF_MONTH), startDate.get(Calendar.MONTH), startDate.get(Calendar.YEAR))
    }




    fun getStartDateText(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
        storeStartDate(dayOfMonth, monthOfYear, year)
        return "$dayOfMonth / ${monthOfYear+1} / $year"
    }

    fun getEndDateText(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
        storeEndDate(dayOfMonth, monthOfYear, year)
        return "$dayOfMonth / ${monthOfYear+1} / $year"
    }

    //fun getRegexToMatchStepsGoal() = Regex("([5-9][0-9]*(000)+)|([1-9]+0*0000)")  ([1-9]0000)|([1-9][1-9]000)
    fun getRegexToMatchStepsGoal() = Regex("([1-9]0000)|([1-9][1-9]000)") //For checking if goalsteps > = 10,000 and not more than 90,000

    fun getRegexToMatchTeamSize() = Regex("[2-9]|10")

    //fun isTeamLimitValid(size: Int):Boolean{ return size<=10 }

    fun areAllInputFieldsValid(): Boolean {
        isFullDataValid.value = isNameValid and isGoalValid and isEndDateValid and isStartDateValid and isTeamSizeValid
        if (isFullDataValid.value == true){
            buttonVis.value = true
            return true
        }
        else {
            buttonVis.value = false
            return false
        }
    }

    fun createTournament (name: Editable?, description: Editable?,type: Int,goal: Editable?, teamLimit:Editable?, teams:Array<String>?, action: (Boolean) -> Unit) {
        //Log.d("FDin",isFullDataValid.value.toString())
        Log.d("startDate", Timestamp(startDate.timeInMillis / 1000, 0).toString())
        Log.d("currentDate", Timestamp.now().toString())
            firestoreRepository.getTournament(name.toString())
                .addOnSuccessListener {
                    if (it.exists()) {
                        messageLiveData.value = "Tournament already exists"
                        action(it.exists())
                        return@addOnSuccessListener
                    }

                    else if((currentDate.get(Calendar.YEAR).equals(startDate.get(Calendar.YEAR))) &&
                        (currentDate.get(Calendar.DAY_OF_MONTH).equals(startDate.get(Calendar.DAY_OF_MONTH))) &&
                        (currentDate.get(Calendar.MONTH).equals(startDate.get(Calendar.MONTH)))){
                    //Setting the tournament as active if the tourney starts on the same date as the creation day
                    firestoreRepository.storeTournament(
                        Tournament(
                            name = name.toString(),
                            description = description.toString(),
                            //type = type,
                            dailyGoal = goal.toString().toInt(),
                            startTimestamp = Timestamp(startDate.timeInMillis / 1000, 0),
                            finishTimestamp = Timestamp(endDate.timeInMillis / 1000, 0),
                            creationTimestamp = Timestamp.now(),
                            creatorName = sharedPrefsRepository.user.name
                                    + " (${sharedPrefsRepository.user.email.split("@")[0]})",
                            creatorUId = sharedPrefsRepository.user.uid,
                            //isActive = true,
                            active = true,
                            exist = true
                        )
                    ) {
                        action(it)
                        messageLiveData.value = if (it) "Tournament created successfully"
                        else "Tournament creation failed"

                        if(it) {
                            if (!teams.isNullOrEmpty()) {
                                for (team in teams) {
                                    firestoreRepository.updateTournamentData(name.toString(), mapOf("teams" to FieldValue.arrayUnion(team)))
                                        .addOnFailureListener {
                                            messageLiveData.value = "Failed to created tournament. Please try again later"
                                            firestoreRepository.updateTournamentData(name.toString(), mapOf("teams" to FieldValue.arrayRemove(team)))
                                            Log.d("TourneyCreation Failure", it.toString())
                                        }
                                    firestoreRepository.updateTeamData(team, mapOf("currentTournaments" to FieldValue.arrayUnion(name.toString())))
                                        .addOnSuccessListener {
                                            firestoreRepository.getTournament(name.toString())
                                                .addOnSuccessListener {
                                                    val tournament = it.toObject<Tournament>()
                                                    firestoreRepository.getTeam(team)
                                                        .addOnSuccessListener {
                                                            val teamData = it.toObject<Team>()
                                                            val members = teamData?.members
                                                            val userTournament =
                                                                tournament?.let { it1 -> getUserTournament(it1, team) }

                                                            for (uid in members!!) {
                                                                //val uid = sharedPrefsRepository.user.uid
                                                                //Log.d("Test","UID ${uid}")
                                                                userTournament?.let { it1 ->
                                                                    updateUserSharedPrefsData(it1) }
                                                                Log.d("Test", "tourneyName2 ${tournament?.name}")
                                                                mapOf("currentTournaments.${tournament?.name}" to userTournament).let { it1 -> firestoreRepository.updateUserTournamentData(uid, it1) }
                                                                    .addOnSuccessListener {

//                                                                      userTournament?.leafCount =
//                                                                      sharedPrefsRepository.getDailyStepCount() / 1000
                                                                        messageLiveData.value = "Tournament has been created and teams were added successfully"
                                                                        //TODO: These 3 lines which are below have
                                                                        // to be executed everytime when a user navigates to tournament fragment
                                                                        val user = sharedPrefsRepository.user
                                                                        user.currentTournaments[tournament!!.name] = userTournament!!
                                                                        sharedPrefsRepository.user = user

                                                                        Log.d("Test", "Being added to user")
                                                                    }
                                                                    .addOnFailureListener {
                                                                        Log.d("Test", "Unable to add user")
                                                                    }
                                                            }
                                                        }
                                                }
                                                .addOnFailureListener {
                                                    firestoreRepository.updateTeamData(team, mapOf("currentTournaments" to FieldValue.arrayRemove(name.toString())))
                                                }
                                        }
                                }
                            }
                        }
                        else{
                            messageLiveData.value = "Tournament Creation Failed"
                        }
                    }
                    }

                    else {
                        firestoreRepository.storeTournament(
                            Tournament(
                                name = name.toString(),
                                description = description.toString(),
                                //type = type,
                                dailyGoal = goal.toString().toInt(),
                                startTimestamp = Timestamp(startDate.timeInMillis / 1000, 0),
                                finishTimestamp = Timestamp(endDate.timeInMillis / 1000, 0),
                                creationTimestamp = Timestamp.now(),
                                creatorName = sharedPrefsRepository.user.name
                                        + " (${sharedPrefsRepository.user.email.split("@")[0]})",
                                creatorUId = sharedPrefsRepository.user.uid,
                                //isActive = false,
                                active = false,
                                exist = true,
                                teamLimit = teamLimit.toString().toInt()
                                //active = true
                            )
                        ) {
                            action(it)
                            //messageLiveData.value = if (it) "Tournament has been created successfully"
                            //else "Tournament creation failed"
                            if(it) {
                                if (!teams.isNullOrEmpty()) {
                                    for (team in teams) {
                                        firestoreRepository.updateTournamentData(name.toString(), mapOf("teams" to FieldValue.arrayUnion(team)))
                                            .addOnFailureListener {
                                                messageLiveData.value = "Failed to created tournament. Please try again later"
                                                firestoreRepository.updateTournamentData(name.toString(), mapOf("teams" to FieldValue.arrayRemove(team)))
                                                Log.d("TourneyCreation Failure", it.toString())
                                            }
                                        firestoreRepository.updateTeamData(team, mapOf("currentTournaments" to FieldValue.arrayUnion(name.toString())))
                                            .addOnSuccessListener {
                                                firestoreRepository.getTournament(name.toString())
                                                    .addOnSuccessListener {
                                                        val tournament = it.toObject<Tournament>()
                                                        firestoreRepository.getTeam(team)
                                                            .addOnSuccessListener {
                                                                val teamData = it.toObject<Team>()
                                                                val members = teamData?.members
                                                                val userTournament =
                                                                    tournament?.let { it1 -> getUserTournament(it1, team) }

                                                                for (uid in members!!) {
                                                                    //val uid = sharedPrefsRepository.user.uid
                                                                    //Log.d("Test","UID ${uid}")
                                                                    userTournament?.let { it1 ->
                                                                        updateUserSharedPrefsData(it1) }
                                                                    Log.d("Test", "tourneyName2 ${tournament?.name}")
                                                                    mapOf("currentTournaments.${tournament?.name}" to userTournament).let { it1 -> firestoreRepository.updateUserTournamentData(uid, it1) }
                                                                        .addOnSuccessListener {

//                                                                      userTournament?.leafCount =
//                                                                      sharedPrefsRepository.getDailyStepCount() / 1000
                                                                            messageLiveData.value = "Tournament has been created and teams were added successfully"
                                                                            //TODO: These 3 lines which are below have
                                                                            // to be executed everytime when a user navigates to tournament fragment
                                                                            val user = sharedPrefsRepository.user
                                                                            user.currentTournaments[tournament!!.name] = userTournament!!
                                                                            sharedPrefsRepository.user = user

                                                                            Log.d("Test", "Being added to user")
                                                                        }
                                                                        .addOnFailureListener {
                                                                            Log.d("Test", "Unable to add user")
                                                                        }
                                                                }
                                                            }
                                                    }
                                            .addOnFailureListener {
                                                firestoreRepository.updateTeamData(team, mapOf("currentTournaments" to FieldValue.arrayRemove(name.toString())))
                                            }
                                        }
                                    }
                                }
                            }
                            else{
                             messageLiveData.value = "Tournament Creation Failed"
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    messageLiveData.value = "Failure"
                }
    }

    private fun storeStartDate(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        startDate = Calendar.getInstance()
        startDate.apply {
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.MONTH, monthOfYear)
            set(Calendar.YEAR, year)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
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

    private fun getUserTournament(tournament: Tournament, team:String) =
        UserTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis,
            goal = tournament.goal,
            endDate = tournament.finishTimestamp,
            teamName = team
        )

    private fun updateUserSharedPrefsData(userTournament: UserTournament){
        val user = sharedPrefsRepository.user
        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        user.currentTournaments[userTournament.name] = userTournament
        sharedPrefsRepository.user = user
    }
}
