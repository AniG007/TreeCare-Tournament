package dal.mitacsgri.treecare.screens.tournaments

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.*
import calculateDailyGoalsAchievedFromStepCountForTeam
import calculateLeafCountFromStepCountForTeam
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateTeamDataWorker
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class TournamentsViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository,
    private val stepCountRepository: StepCountRepository
): ViewModel() {

//    companion object Types {
//        const val ACTIVE_TOURNAMENTS = 0
//        const val TOURNAMENTS_BY_YOU = 1
//    }

    val activeTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val currentTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val myTournamentsList: MutableLiveData<ArrayList<Tournament>> =
        MutableLiveData<ArrayList<Tournament>>().default(
            arrayListOf()
        )

    var teamsHolder = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    var existingTeams = MutableLiveData<ArrayList<String>>().default(arrayListOf())

    val MessageStatus = MutableLiveData<String>()
    var messageDisplayed = false  //for current tournament frag
    var messageDisplayed2 = false //for active tournament frag

    val totalSteps = MutableLiveData<Int>().default(0)
    var c = 0 // for counting teams in forloop when team joins a tournament
    var cr =
        0 // to check for count before uploading team tourney data into firestore, part of the worker for updating team data. It's been added here for testing purposes.
    var currentStepCount = 0

    //The error status message must contain 'error' in string because it is used to check whether to
    //disable or enable join button
    val statusMessage = MutableLiveData<String>()

    fun getAllActiveTournaments(): MutableLiveData<ArrayList<Tournament>> {
        firestoreRepository.getAllActiveTournaments()
            .addOnSuccessListener {
                activeTournamentsList.value?.clear()
                activeTournamentsList.value =
                    it.toObjects<Tournament>().filter { it.exist && it.active }.toArrayList()
                activeTournamentsList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active tournaments", "Fetch failed: $it")
            }
        return activeTournamentsList
    }

    fun getUserTournaments(): MutableLiveData<ArrayList<Tournament>> {
        firestoreRepository.getAllTournamentsCreatedByUser(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                //myTournamentsList.value?.clear()
                myTournamentsList.value =
                    it.toObjects<Tournament>().filter { it.exist }.toArrayList()
                myTournamentsList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("User tournaments", "Fetch failed: $it")
            }
        return myTournamentsList
    }
    
    
    fun getCurrentTournamentsForUser(): MutableLiveData<ArrayList<Tournament>> {

        WorkManager.getInstance().cancelUniqueWork("teamWorker")
// directly fetching current tournaments from db.

        val uid = sharedPrefsRepository.user.uid
        sharedPrefsRepository.team = Team()

        val c = sharedPrefsRepository.user
        c.currentTournaments.clear()
        sharedPrefsRepository.user = c

        Log.d("Teamy", sharedPrefsRepository.user.currentTournaments.toString())

        //updating steps for user in the db
        stepCountRepository.getTodayStepCountData {
            firestoreRepository.updateUserData(
                sharedPrefsRepository.user.uid,
                mapOf("dailySteps" to it)
            )
            currentStepCount = it
        }


//        Log.d("Test", "SharedPref" + sharedPrefsRepository.user.currentTournaments)
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {

                val user = it.toObject<User>()
                val tournaments = user?.currentTournaments
                Log.d("Test", "TeamDB " + user?.currentTeams.toString())
                val team = user?.currentTeams.toString().removeSurrounding("[", "]")
                if (team.isNotEmpty()) {
                    Log.d("Test", "Team is not empty")
                    firestoreRepository.getTeam(team)
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            sharedPrefsRepository.team = teamDB!!
                        }
                } else Log.d("Test", "Team is empty")

                if (tournaments?.isNotEmpty()!!) {
                    val tourneys = tournaments
                    val userPref = sharedPrefsRepository.user
                    userPref.currentTournaments = tourneys
                    sharedPrefsRepository.user = userPref
                } else Log.d("Test", "User's currentTournaments is empty")

                for (tournament in tournaments) {
                    Log.d("Test", "tournament" + tournament.key)
                    firestoreRepository.getTournament(tournament.key)
                        .addOnSuccessListener {

                            val tourney = it.toObject<Tournament>() ?: Tournament(exist = false)
//                            synchronized(currentTournamentsList.value!!) {
                            if (tourney.exist && tourney.active) {

                                updateLastDayStepCountIfNeeded() //if the user uninstalls and installs the app, we do not want to sync all the steps again. hence we do this check

                                Log.d("Test", "tournament exists")
                                //currentTournamentsList.value?.sortAndAddToList(tourney)
                                //currentTournamentsList.notifyObserver()

                                //Performing this sync again since firebase is asynchronous and may execute even before they sync that we do with the code above
                                if (sharedPrefsRepository.user.currentTournaments.isNullOrEmpty()) {
                                    val userPref = sharedPrefsRepository.user

                                    userPref.currentTournaments[tourney.name] =
                                        tournaments[tourney.name]!!
                                    sharedPrefsRepository.user = userPref

//                                        userPref.currentTournaments[tourney.name] =
//                                        getUserTournament(tourney, user.currentTeams.toString())

                                    for (prefTourney in sharedPrefsRepository.user.currentTournaments.keys) {
                                        // for removing tournaments from user and team prefs if the user is not a part of the tournament
                                        if (tournaments.containsKey(prefTourney))
                                            continue
                                        else {
                                            userPref.currentTournaments.remove(prefTourney)
                                            sharedPrefsRepository.user = userPref
                                        }
                                    }
                                }

                                if (sharedPrefsRepository.team.currentTournaments.isEmpty()) {
                                    val userTeam = sharedPrefsRepository.team
                                    userTeam.currentTournaments[tourney.name] =
                                        if (tourney.startTimestamp.toDateTime()
                                                .withTimeAtStartOfDay().millis == DateTime().withTimeAtStartOfDay().millis
                                        )
                                            getTeamTournament(
                                                tourney,
                                                sharedPrefsRepository.getDailyStepCount(),
                                                userTeam.name,
                                                true
                                            )
                                        else
                                            getTeamTournament(
                                                tourney,
                                                sharedPrefsRepository.getDailyStepCount(),
                                                userTeam.name,
                                                false
                                            )
                                    sharedPrefsRepository.team = userTeam
                                    //sharedPrefsRepository.storeLastDayStepCount(currentStepCount)

                                    for (prefTourney in sharedPrefsRepository.team.currentTournaments.keys) {
                                        if (tournaments.containsKey(prefTourney)) {
                                            continue
                                        } else {
                                            //if the captain quits the tournament, prefs are cleared when user visits the tournament page.
                                            userTeam.currentTournaments.remove(prefTourney)
                                            sharedPrefsRepository.team = userTeam
                                        }
                                    }
                                }
                                //fetch current tourney for users and check with shared prefs
                                Log.d("Test", "currentTourney List" + currentTournamentsList.value)
                            }

                            updateUserStepsForTournaments(tournament)
                            //}
                            synchronized(currentTournamentsList.value!!) {
                                currentTournamentsList.value?.sortAndAddToList(tourney)
                                currentTournamentsList.notifyObserver()
                            }
                        }
                        .addOnFailureListener {
                            Log.d("Tournament not Found", it.toString())
                        }
                }
            }
        return currentTournamentsList
    }

//    fun getAllCreatedTournamentsTournaments(userId: String) {
//        firestoreRepository.getAllTournamentsCreatedByUser(userId)
//            .addOnSuccessListener {
//                tournamentsByYouList.value = it.toObjects<Tournament>().filter { it.exist }.toArrayList()
//                tournamentsByYouList.notifyObserver()
//            }
//            .addOnFailureListener {
//                Log.e("Active tournaments", "Fetch failed: $it")
//            }
//    }

    fun leaveTournament(tournament: Tournament) {

        val user = sharedPrefsRepository.user
        val userTournament = sharedPrefsRepository.user.currentTournaments.get(tournament.name)

        Log.d("Test", "DB" + user.captainedTeams)
        Log.d("Test", "sharedPrefUser " + user.currentTournaments)
        Log.d("Test", "sharedPref" + userTournament?.teamName)
        //if (userTournament?.teamName?.contains(user.captainedTeams.toString().removeSurrounding("[","]"))!!) {
        if (user.captainedTeams.isNotEmpty()) {
            Log.d("Test", "Teams " + userTournament?.teamName)

            firestoreRepository.getTeam(
                userTournament?.teamName?.removeSurrounding(
                    "[",
                    "]"
                )!!
            )  //removing prefix and suffix to make db call
                .addOnSuccessListener {

                    val team = it.toObject<Team>()
                    val members = team?.members
                    Log.d("Test", "member" + members)
                    for (member in members!!) {
                        firestoreRepository.deleteTournamentFromUserDB(member, tournament.name)
                            .addOnSuccessListener {
                                firestoreRepository.deleteTournamentFromTeamDB(
                                    team.name,
                                    tournament.name
                                )
                                    .addOnSuccessListener {

                                        firestoreRepository.updateTournamentData(
                                            tournament.name,
                                            mapOf("teams" to FieldValue.arrayRemove(team.name))
                                        )
                                            .addOnSuccessListener {
                                                messageDisplayed = false
                                                statusMessage.value =
                                                    "Your team is no longer a part of the tournament ${tournament.name}"

//                                                var index = currentTournamentsList.value?.indexOf(tournament)
//                                                Log.d("Test","Tname "+ team.name)
//                                                currentTournamentsList.value?.get(index!!)?.teams?.remove(team.name)
//                                                currentTournamentsList.notifyObserver()

                                                removeTournamentFromCurrentTournamentsLists(
                                                    tournament
                                                )

//                                                index = activeTournamentsList.value?.indexOf(tournament)
//                                                activeTournamentsList.value?.get(index!!)?.teams?.remove(team.name)
//                                                activeTournamentsList.notifyObserver()
                                            }

                                            .addOnFailureListener {
                                                Log.d("Test", it.toString())
                                                firestoreRepository.updateTournamentData(
                                                    tournament.name,
                                                    mapOf("teams" to FieldValue.arrayUnion(team.name))
                                                )
                                            }
                                    }
                                    .addOnFailureListener {
                                        Log.d("Exception", it.toString())
                                    }
                            }
                            .addOnFailureListener {
                                Log.d("Exception", it.toString())
                            }
                    }
                }
        } else {
            messageDisplayed = false
            statusMessage.value = "Only the team captain can remove the team from the tournament"
            Log.d("Test", "Only the captain can leave a tournament.")
        }

    }

    fun startUnityActivityForTournament(tournament: Tournament, action: () -> Unit) {

        sharedPrefsRepository.apply {

            val teamTournament = team.currentTournaments[tournament.name]!!
            //storeTournamentTotalStepCount(teamTournament.dailyStepsMap.values.last())
            //Log.d("Unity", teamTournament.dailyStepsMap.values.last().toString())
            gameMode = TOURNAMENT_MODE
            tournamentType = teamTournament.type
            tournamentGoal = teamTournament.goal
            tournamentLeafCount = teamTournament.leafCount
            tournamentFruitCount = teamTournament.fruitCount
            tournamentStreak = teamTournament.tournamentGoalStreak
            tournamentName = teamTournament.name
            isTournamentActive = teamTournament.endDate.toDateTime().millis > DateTime().millis
            tournamentTotalStepsCount =
                teamTournament.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()]!!
            //tournamentTotalStepsCount = sharedPrefsRepository.getDailyStepCount()
            //tournamentTotalStepsCount = getDailyStepCount()
            Log.d("Unity", tournamentTotalStepsCount.toString())
            action()
        }
    }

    fun getTournamentDurationText(tournament: Tournament): SpannedString {
        val finishDate = tournament.finishTimestamp.toDateTime().millis
        //val finishDateString = tournament.finishTimestamp.toDateTime().getMapFormattedDate()
        val finishDateString =
            tournament.finishTimestamp.toDateTime().getStringRepresentation().split(",")
        val finishDateFormat1 =
            SimpleDateFormat("HH:mm") //reference for conversion: https://beginnersbook.com/2017/10/java-display-time-in-12-hour-format-with-ampm/?unapproved=224896&moderation-hash=b24a6ccf99e5d9013cc45de55849ebb4#comment-224896
        val finishDateParsed = finishDateFormat1.parse(finishDateString.get(1).trim())
        val finishDateFormat2 = SimpleDateFormat("hh:mm aa")
        val finishDateTime = finishDateFormat2.format(finishDateParsed)
        val finishDateText = finishDateString.get(0) + ", " + finishDateTime
        val tournamentEnded = finishDate < DateTime().millis

        return buildSpannedString {
            bold {
                append(if (tournamentEnded) "Ended: " else "Ends: ")
            }
            append(finishDateText)
        }
    }

    fun getTournamentStartDate(tournament: Tournament): SpannedString {
        val startDate = tournament.startTimestamp.toDateTime().millis
        //val startDateString = tournament.startTimestamp.toDateTime().getMapFormattedDate()
        val startDateString =
            tournament.startTimestamp.toDateTime().getStringRepresentation().split(",")
        val startDateFormat1 = SimpleDateFormat("HH:mm")
        val startDateParsed = startDateFormat1.parse(startDateString.get(1).trim())
        val startDateFormat2 = SimpleDateFormat("hh:mm aa")
        val startDateTime = startDateFormat2.format(startDateParsed)
        val startDateText = startDateString.get(0) + ", " + startDateTime
        val tournamentEnded = startDate > DateTime().millis

        return buildSpannedString {
            bold {
                append(if (tournamentEnded) "Starts: " else "Started: ")
            }
            append(startDateText)
        }
    }

    fun getGoalText(tournament: Tournament) =
        buildSpannedString {
            bold {
                append("Minimum Daily Goal: ")
                /*append(if(tournament.type == TOURNAMENT_TYPE_DAILY_GOAL_BASED) "Minimum Daily Goal: "
                else "Total steps goal: ")*/
            }
            append(tournament.dailyGoal.toString())
        }

    fun getTeamsCountText(tournament: Tournament) = tournament.teams.count()

//    fun getCurrentUserId() = sharedPrefsRepository.user.uid

    fun getJoinTournamentDialogTitleText(tournament: Tournament) =
        buildSpannedString {
            append("Join tournament ")
            bold {
                append("'${tournament.name}'")
            }
        }

    fun getJoinTournamentMessageText() = "Do you want to enroll your team in this tournament?"

    fun storeTournamentLeaderboardPosition(position: Int) {
        sharedPrefsRepository.tournamentLeaderboardPosition = position
    }

    private fun ArrayList<Tournament>.sortAndAddToList(tournament: Tournament) {
        val finishTimestampMillis = tournament.finishTimestamp.toDateTime().millis
        if (size == 0) {
            add(tournament)
            return
        }

        for (i in 0 until size) {
            if (this[i].finishTimestamp.toDateTime().millis < finishTimestampMillis) {
                add(i, tournament)
                return
            }
        }
        this.add(tournament)
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    fun getExistingTeams(tournamentName: String) {
        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                if (it.exists()) {
                    val ct = it.toObject<Tournament>()
                    val teams = ct?.teams
                    for (team in teams!!) {
                        existingTeams.value?.add(team)
                    }
                    Log.d("Test", "existingTeams" + existingTeams.value)
                    existingTeams.notifyObserver()
                }
            }
    }

    fun enrollTeams(tournamentName: String, item: Tournament) {
        //TO check if a team has already been enrolled in the tournament

        //Sorry about the mess in this function but it was inevitable during this time. Welcome to callback hell! A possible alternative is using coroutines.

        teamsHolder.value?.add(
            sharedPrefsRepository.user.captainedTeams.toString().removeSurrounding("[", "]")
                .trimEnd()
        )

        Log.d(
            "Test",
            "TeamsValue " + teamsHolder.value.toString().removeSurrounding("[", "]").trimEnd()
        )

        if (sharedPrefsRepository.user.captainedTeams.isNotEmpty()) {
            for (team in teamsHolder.value!!) {

                //   Log.d("Test", "insideEnrollTeams" + team)
                firestoreRepository.getTournament(tournamentName)
                    .addOnSuccessListener {
                        val tournament = it.toObject<Tournament>()

                        //getTeamTotalSteps(team)

                        val teamTournament = tournament?.let { it1 ->
                            if (item.startTimestamp.toDateTime()
                                    .withTimeAtStartOfDay().millis == DateTime().withTimeAtStartOfDay().millis
                            )
                                getTeamTournament(it1, 0, team, true)
                            else
                                getTeamTournament(it1, 0, team, false)
                        }  // input sent as 0 for steps and setting it again at line 358 after #TO_DO steps
                        //  This was done so as to add users' steps if a team joined a tournament in the middle

                        if (existingTeams.value?.contains(team)!!) {
                            //         Log.d("Test", existingTeams.value.toString())
                            messageDisplayed2 = false
                            MessageStatus.value = "Team has already been enrolled"
                        } else if (existingTeams.value?.contains(team)!!) {
                            //    Log.d("Test", existingTeams.value.toString())
                            messageDisplayed2 = false
                            MessageStatus.value = "Team has already been enrolled"
                        } else if (tournament?.teams?.count() == tournament?.teamLimit) {
                            messageDisplayed2 = false
                            MessageStatus.value = "Tournament is full"
                        } else if (item.startTimestamp.toDateTime()
                                .withTimeAtStartOfDay().millis < DateTime().withTimeAtStartOfDay().millis
                        ) {
                            messageDisplayed2 = false
                            MessageStatus.value = "The Tournament has already begun!"
                        } else {
                            firestoreRepository.updateTournamentData(
                                tournamentName,
                                mapOf("teams" to FieldValue.arrayUnion(team))
                            )
                                .addOnSuccessListener {
                                    Log.d("Test", "TeamName " + team)

                                    mapOf("currentTournaments.${tournament?.name}" to teamTournament).let { it1 ->
                                        firestoreRepository.updateTeamTournamentData(team, it1)
                                    }
                                        //adding total steps (aggregate of user steps) to the team
                                        .addOnSuccessListener {
                                            Log.d("Test", "Core")
                                            firestoreRepository.getTeam(team)
                                                .addOnSuccessListener {
                                                    val teamData = it.toObject<Team>()
                                                    val members = teamData?.members
                                                    val size = members?.count()
                                                    for (member in members!!) {
                                                        c++
                                                        firestoreRepository.getUserData(member)
                                                            .addOnSuccessListener {
                                                                val user = it.toObject<User>()
                                                                val steps = user?.dailySteps!!
                                                                Log.d(
                                                                    "StepTest",
                                                                    "dailySteps ${user.uid} " + steps.toString()
                                                                )
                                                                totalSteps.value =
                                                                    totalSteps.value?.plus(steps)
                                                                totalSteps.notifyObserver()
                                                                if (c == size) {
                                                                    //TODO: steps
                                                                    val updateTotalSteps =
                                                                        tournament?.let { it1 ->
//                                                                            getTeamTournament(    //This was for adding all the users steps while joining the tournament
//                                                                                it1,              // But this is not needed since teams won't be able to join after tournament start date
//                                                                                totalSteps.value?.toInt()!!,
//                                                                                team
//                                                                            )
//                                                                            if(item.startTimestamp.toDateTime().withTimeAtStartOfDay().millis == DateTime().withTimeAtStartOfDay().millis)
                                                                            getTeamTournament(
                                                                                it1,
                                                                                0,
                                                                                team,
                                                                                true
                                                                            )
                                                                            //else
                                                                            // getTeamTournament(it1, 0, team, false)
                                                                        }
                                                                    updateTeamSharedPrefsData(
                                                                        updateTotalSteps!!
                                                                    )
                                                                    mapOf("currentTournaments.${tournament.name}" to updateTotalSteps).let { it1 ->
                                                                        firestoreRepository.updateTeamTournamentData(
                                                                            team,
                                                                            it1
                                                                        )
                                                                    }
                                                                        .addOnSuccessListener {
                                                                            addTournament(
                                                                                team,
                                                                                tournamentName,
                                                                                updateTotalSteps
                                                                            )

                                                                        }
                                                                        .addOnFailureListener {
                                                                            mapOf("currentTournaments.${tournament.name}" to updateTotalSteps).let { it1 ->
                                                                                firestoreRepository.deleteTournamentFromTeamDB(
                                                                                    team,
                                                                                    tournamentName
                                                                                )
                                                                            }
                                                                        }
                                                                    c = 0
                                                                }
                                                                Log.d(
                                                                    "StepTest",
                                                                    "totalsteps " + totalSteps.value?.toInt()
                                                                        .toString()
                                                                )
                                                            }
                                                    }
                                                }
                                        }

                                        .addOnFailureListener {
                                            Log.d("Test", "core fail" + it.toString())
                                        }
                                        .addOnFailureListener {
                                            Log.d("Test", "outside core fail " + it.toString())
                                            messageDisplayed2 = false
                                            MessageStatus.value =
                                                "Unable to Enroll. Please Try Again later"
                                        }
                                }
                        }
                    }
            }
        } else {
            messageDisplayed2 = false
            MessageStatus.value = "Only team captains can enroll their teams in tournaments"
        }
    }

    fun addTournament(team: String, tournamentName: String, teamTournament: TeamTournament) {

        Log.d("Test", "Inside Add tourney")
        //Adding tournament to currentTournaments in Users Collection
        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                val tournament = it.toObject<Tournament>()
                firestoreRepository.getTeam(team)
                    .addOnSuccessListener {
                        val teamData = it.toObject<Team>()
                        val members = teamData?.members
                        val userTournament = tournament?.let { it1 ->
                            //if(teamTournament.startDate.toDateTime().withTimeAtStartOfDay().millis == DateTime().withTimeAtStartOfDay().millis)
                            getUserTournament(it1, team, true)
                            //else
                            //getUserTournament(it1, team, false)
                        }

                        Log.d("Test", "tourneyName2 ${tournament?.name}")

                        for (uid in members!!) {
                            //val uid = sharedPrefsRepository.user.uid
                            //Log.d("Test","UID ${uid}")
                            userTournament?.let { it1 -> updateUserSharedPrefsData(it1) }
                            Log.d("Test", "tourneyName2 ${tournament?.name}")
                            mapOf("currentTournaments.${tournament?.name}" to userTournament).let { it1 ->
                                firestoreRepository.updateUserTournamentData(uid, it1)
                            }
                                .addOnSuccessListener {

                                    messageDisplayed2 = false
                                    MessageStatus.value = "Enrolled Successfully"

//                                    var index = activeTournamentsList.value?.indexOf(tournament)!!
//                                    activeTournamentsList.value?.get(index)?.teams?.add(team)
//                                    activeTournamentsList.notifyObserver()
                                    activeTournamentsList.value?.add(tournament!!)
                                    activeTournamentsList.notifyObserver()
                                    getAllActiveTournaments()
                                    synchronized(currentTournamentsList) {
                                        currentTournamentsList.value?.sortAndAddToList(tournament!!)
                                        currentTournamentsList.notifyObserver()
                                    }

//                                    val tmz = teamsHolder
//                                    tmz.value?.clear()
//                                    teamsHolder = tmz

                                    teamsHolder = MutableLiveData<ArrayList<String>>().default(
                                        arrayListOf()
                                    )
                                    existingTeams = MutableLiveData<ArrayList<String>>().default(
                                        arrayListOf()
                                    )

                                    val user = sharedPrefsRepository.user
                                    userTournament?.endDate!!
                                    user.currentTournaments[tournament.name] = userTournament
                                    sharedPrefsRepository.user = user

                                    val teamPref = sharedPrefsRepository.team
                                    teamPref.currentTournaments[tournament.name] = teamTournament
                                    sharedPrefsRepository.team = teamPref
                                    Log.d("Test", "Being added to user")
                                }
                                .addOnFailureListener {
                                    Log.d("Test", "Unable to add user")
                                }
                        }
                    }
            }
    }

    private fun getUserTournament(tournament: Tournament, team: String, bool: Boolean) =
        UserTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = 0,
            joinDate = DateTime().millis,
            goal = tournament.dailyGoal,
            startDate = tournament.startTimestamp,
            endDate = tournament.finishTimestamp,
            teamName = team.removeSurrounding("[", "]"),
            isActive = bool
        )

    private fun getTeamTournament(tournament: Tournament, steps: Int, team: String, bool: Boolean) =
        TeamTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = 0,
            joinDate = DateTime().millis,
            goal = tournament.dailyGoal,
            startDate = tournament.startTimestamp,
            endDate = tournament.finishTimestamp,
            teamName = team,
            isActive = bool
        )


    /** to call this method everytime a user visits the tournament fragment
    so that the prefs are updated for unity before opening the tree */

    private fun updateUserSharedPrefsData(userTournament: UserTournament) {
        val user = sharedPrefsRepository.user
//        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 3000
//        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        userTournament.leafCount = 0
        userTournament.totalSteps = 0
        user.currentTournaments[userTournament.name] = userTournament
        sharedPrefsRepository.user = user
    }

    fun updateTeamSharedPrefsData(teamTournament: TeamTournament) {
        val team = sharedPrefsRepository.team
//        teamTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 3000
//        teamTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        teamTournament.leafCount = 0
        teamTournament.totalSteps = 0
        team.currentTournaments[teamTournament.name] = teamTournament
        sharedPrefsRepository.team = team
    }

    fun display() {

//        var tour = sharedPrefsRepository.team
//        val tourey = sharedPrefsRepository.user
//
//        tourey.currentTournaments.forEach { (_, tourneys) ->
//            if(tourneys.isActive){
//                tourneys.dailyStepsMap["1595386800000"] = 0
//                tourneys.dailyStepsMap["1595473200000"] = 150
//            }
//        }

//        tour.currentTournaments.forEach { (_, tourney) ->
//            if(tourney.isActive){
//                tourney.dailyStepsMap.remove("1595473200000")
//            }
//             tourney.dailyStepsMap = tourney.dailyStepsMap.toSortedMap()
//             Log.d("Test", " Sorted ${tourney.name}" +tourney.dailyStepsMap.toSortedMap())
//        }
//        sharedPrefsRepository.storeLastDayStepCount(0)
//
//        sharedPrefsRepository.team = tour
//        sharedPrefsRepository.user = tourey

//        Log.d("Test","Time $Timestamp(Date(1592535600000).toString()")

        Log.d("Test", "Team " + sharedPrefsRepository.team)
        Log.d("Test", "currentTourney " + sharedPrefsRepository.user.currentTournaments)
        Log.d("Test", "LAst " + sharedPrefsRepository.getLastDayStepCount())
        Log.d(
            "Test",
            "DailyCount" + sharedPrefsRepository.user.stepMap + "Leaf " + sharedPrefsRepository.user.leafMap
        )
    }

    private fun removeTournamentFromCurrentTournamentsLists(tournament: Tournament) {

        currentTournamentsList.value?.remove(tournament)
        currentTournamentsList.notifyObserver()

        sharedPrefsRepository.user = sharedPrefsRepository.user.let {
            it.currentTournaments.remove(tournament.name)
            it
        }
        sharedPrefsRepository.team = sharedPrefsRepository.team.let {
            it.currentTournaments.remove(tournament.name)
            it
        }
    }


    private fun updateLastDayStepCountIfNeeded() {
        if (sharedPrefsRepository.getLastDayStepCount() == 0) {
            firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
                .addOnSuccessListener {
                    val user = it.toObject<User>()
                    val currentTournaments = user?.currentTournaments
                    currentTournaments?.forEach { (_, tourney) ->
                        if (tourney.isActive && tourney.dailyStepsMap.isNotEmpty()) {
                            Log.d("LastCount", "tourney is active and map is not empty")
                            sharedPrefsRepository.storeLastDayStepCount(tourney.dailyStepsMap.toSortedMap().values.last())
                        }
                    }
                }
        }
    }

    fun display2(tour: Tournament) {
        /*sharedPrefsRepository.team.currentTournaments.forEach { (_, tourney) ->
            if (tourney.isActive) {
                Log.d("Test", tourney.toString())
            }
        }
        sharedPrefsRepository.user.currentTournaments.forEach { (_, tourney) ->
            if (tourney.isActive)
                Log.d("Test", tourney.toString())
        }
        Log.d("Test", "LAst: "+ sharedPrefsRepository.getLastDayStepCount())*/
        firestoreRepository.getAllActiveChallenges().addOnSuccessListener {
            val user = it.toObjects<Challenge>()
            Log.d("Test", user.toString())
        }
    }

    fun deleteTournament(tournament: Tournament) {
        setTournamentInactive(tournament)
        /*removeTournamentFromTeamsAndUsers(tournament)
        removeTournamentFromUserCreatedList(tournament)*/
        /** Update the mytourney list after executing all these functions8 */
    }

    fun setTournamentInactive(tournament: Tournament) {
        firestoreRepository.updateTournamentData(tournament.name, mapOf("active" to false))
            .addOnSuccessListener {
                firestoreRepository.updateTournamentData(tournament.name, mapOf("exist" to false))
                    .addOnSuccessListener {
                        removeTournamentFromTeamsAndUsers(tournament)
                        removeTournamentFromUserCreatedList(tournament)
                    }
            }

    }

    fun removeTournamentFromTeamsAndUsers(tournament: Tournament) {

        val teams = tournament.teams
        for (team in teams) {
            firestoreRepository.getTeam(team).addOnSuccessListener {
                val teamData = it.toObject<Team>()
                val teamMembers = teamData?.members
                firestoreRepository.deleteTournamentFromTeamDB(teamData?.name!!, tournament.name)
                    .addOnSuccessListener {
                        for (member in teamMembers!!) {
                            firestoreRepository.deleteTournamentFromUserDB(member, tournament.name)
                        }
                    }
                myTournamentsList.value?.remove(tournament)
                myTournamentsList.notifyObserver()
            }

        }
    }

    fun removeTournamentFromUserCreatedList(tournament: Tournament) {
        firestoreRepository.updateUserData(
            sharedPrefsRepository.user.uid, mapOf(
                "tournamentsCreated" to FieldValue.arrayRemove(
                    tournament.name
                )
            )
        )
    }

    fun hasTeamJoined(tournamentName: String): Boolean =
        sharedPrefsRepository.user.currentTournaments.keys.contains(tournamentName)


    fun updateUserStepsForTournaments(tournament: MutableMap.MutableEntry<String, UserTournament>) {

        val today = DateTime()
        val halifaxTimeZone = DateTimeZone.forID("America/Halifax")
        val mNight = today.withZone(halifaxTimeZone).withTimeAtStartOfDay().millis

        val team = sharedPrefsRepository.team
        val user = sharedPrefsRepository.user

        if(team.currentTournaments.isNotEmpty()) {
            stepCountRepository.getTodayStepCountData {
//            user.currentTournaments[tournament.key]?.dailyStepsMap!![mNight.toString()] = it
//            team.currentTournaments[tournament.key]?.dailyStepsMap!![mNight.toString()] = it
                //Log.d("tourneyUpload", "TourneyName: " + tourney.name)
                calc(team, it, team.currentTournaments[tournament.key]!!, mNight)
            }
        }

        startWorkerForTournaments()
    }

    private fun calc(team: Team, currentStepCount: Int, tourney: TeamTournament, midNight: Long) {

        cr++
        val userTourneys = sharedPrefsRepository.user
        //Sorting the step map according to the dates
        Log.d("tourneyUpload", "Inside Calc")

        team.currentTournaments.forEach { (_, teamTourney) ->
            teamTourney.dailyStepsMap = teamTourney.dailyStepsMap.toSortedMap()
        }

        userTourneys.currentTournaments.forEach { (_, userTourney) ->
            userTourney.dailyStepsMap = userTourney.dailyStepsMap.toSortedMap()
        }

        firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                val userTournaments = user?.currentTournaments

                if (userTournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!! && userTournaments[tourney.name]?.isActive!!) {
                    Log.d("tourneyUpload", "Stepmap is empty for ${tourney.name}")


                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                        currentStepCount
                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                        Timestamp.now()
                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                        currentStepCount

                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)

                    Log.d("tourneyUpload", sharedPrefsRepository.user.currentTournaments[tourney.name].toString())

                    firestoreRepository.updateUserData(
                        user.uid,
                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                    )
                        .addOnSuccessListener {
                            Log.d("tourneyUpload", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                            Log.d("tourneyUpload", "stepmap for ${tourney.name} was uploaded successfully")

                            firestoreRepository.getTeam(team.name)
                                .addOnSuccessListener {
                                    val teamDB = it.toObject<Team>()
                                    val teamTourney = teamDB?.currentTournaments
                                    if (teamTourney!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                        Log.d(
                                            "tourneyUpload",
                                            "Team tourney:${tourney.name} Daily Step Map is empty"
                                        )
                                        team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                            currentStepCount

                                        updateAndStoreTeamDataInSharedPrefs(
                                            team.currentTournaments[tourney.name]!!,
                                            team
                                        )
                                    } else {
                                        Log.d("tourneyUpload", "Daily Step Map is not empty")
                                        if (teamTourney[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                .toString() != midNight.toString()
                                        ) {
                                            Log.d(
                                                "tourneyUpload",
                                                "Team tourney: ${tourney.name} is not up to date"
                                            )
                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                currentStepCount

                                            updateAndStoreTeamDataInSharedPrefs(
                                                team.currentTournaments[tourney.name]!!,
                                                team
                                            )
                                        } else {
                                            Log.d(
                                                "tourneyUpload",
                                                "Team tourney: ${tourney.name} is up to date"
                                            )
                                            val oldStep =
                                                teamTourney[tourney.name]?.dailyStepsMap?.values?.last()
                                            val updatedSteps = oldStep!! + currentStepCount
                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] = updatedSteps


                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                updatedSteps
                                            updateAndStoreTeamDataInSharedPrefs(
                                                team.currentTournaments[tourney.name]!!,
                                                team
                                            )
                                        }
                                    }
                                }
                        }
                        .addOnFailureListener {
                            Log.d("tourneyUpload", "stepmap for ${tourney.name} failed to upload")
                        }

                } else if (tourney.isActive) {
                    Log.d("tourneyUpload", "User Tourney: ${tourney.name} stepmap is not empty")
                    firestoreRepository.getTeam(team.name)
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            val tournaments = teamDB?.currentTournaments
//                        team.currentTournaments.forEach { (_, tourney) ->
                            if (tourney.isActive) {
                                Log.d("tourneyUpload", "userStepMap for Tourney ${tourney.name}"+ userTournaments[tourney.name]?.dailyStepsMap?.keys+ "Last "+ userTournaments[tourney.name]?.dailyStepsMap?.keys?.last())
                                if (userTournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                        .toString() == midNight.toString()
                                ) {

                                    //TODO:replace users daily steps
                                    Log.d("tourneyUpload", "UserTourney is up to date")

                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                        currentStepCount

                                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                                        Timestamp.now()
                                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                                        currentStepCount
                                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)


                                    firestoreRepository.updateUserData(
                                        user.uid,
                                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                                    )
                                        .addOnSuccessListener {
                                            Log.d("tourneyUpload", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                                            Log.d(
                                                "tourneyUpload",
                                                "stepmap for ${tourney.name} was uploaded successfully"
                                            )
                                            if (tournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                                Log.d(
                                                    "tourneyUpload",
                                                    "Team tourney step map is empty"
                                                )
                                                tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                    currentStepCount

                                                updateAndStoreTeamDataInSharedPrefs(
                                                    tournaments[tourney.name]!!,
                                                    team
                                                )
                                            } else {
                                                Log.d(
                                                    "tourneyUpload",
                                                    "Team tourney step map is not empty"
                                                )
                                                if (tournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                        .toString() != midNight.toString()
                                                ) {
                                                    Log.d(
                                                        "tourneyUpload",
                                                        "Team tourney is not up to date"
                                                    )
                                                    tourney.dailyStepsMap =
                                                        teamDB.currentTournaments[tourney.name]?.dailyStepsMap!!
                                                    tourney.dailyStepsMap[midNight.toString()] =
                                                        currentStepCount

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        tourney,
                                                        team
                                                    )
                                                } else {
                                                    Log.d(
                                                        "tourneyUpload",
                                                        "Team tourney is up to date"
                                                    )
                                                    val oldStep =
                                                        tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()]

                                                    Log.d("tourneyUpload", "OldStep for tourney: ${tourney.name} " + oldStep)
                                                    Log.d("tourneyUpload", "Last Day step count "+ sharedPrefsRepository.getLastDayStepCount())
                                                    val diff =
                                                        currentStepCount - sharedPrefsRepository.getLastDayStepCount()
                                                    Log.d("tourneyUpload", "Diff3 for tourney: ${tourney.name} " + diff)
                                                    val updatedSteps = oldStep!! + diff

                                                    if (diff > 0) {
                                                        tourney.dailyStepsMap[midNight.toString()] =
                                                            updatedSteps
                                                        updateAndStoreTeamDataInSharedPrefs(
                                                            tourney,
                                                            team
                                                        )
                                                    }
                                                }
                                            }

                                        }
                                } else {
                                    Log.d("tourneyUpload", "UserTourney is not up to date")

                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                        currentStepCount

                                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                                        Timestamp.now()
                                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                                        currentStepCount

                                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)


                                    firestoreRepository.updateUserData(
                                        user.uid,
                                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                                    )
                                        .addOnSuccessListener {
                                            Log.d("tourneyUpload", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                                            Log.d(
                                                "tourneyUpload",
                                                "stepmap for ${tourney.name} was uploaded successfully"
                                            )

                                            //TODO: Update user Tourney
                                            if (tournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                                Log.d(
                                                    "tourneyUpload",
                                                    "Team tourney step map is empty"
                                                )
                                                tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                    currentStepCount

                                                updateAndStoreTeamDataInSharedPrefs(
                                                    tournaments[tourney.name]!!,
                                                    team
                                                )
                                                //updateUserTeamDataInFirestore(future)
//                                                        sharedPrefsRepository.storeLastDayStepCount(
//                                                            currentStepCount
//                                                        )
                                            } else {
                                                Log.d(
                                                    "tourneyUpload",
                                                    "Team tourney step map is not empty"
                                                )
                                                if (tournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                        .toString() != midNight.toString()
                                                ) {
                                                    //Data is present in team tourney
                                                    Log.d(
                                                        "tourneyUpload",
                                                        "Team tourney is not up to date"
                                                    )

                                                    tourney.dailyStepsMap =
                                                        teamDB.currentTournaments[tourney.name]?.dailyStepsMap!!
                                                    tourney.dailyStepsMap[midNight.toString()] =
                                                        currentStepCount

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        tourney,
                                                        team
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                } else {
                                                    //Data is not present in TeamTourney

                                                    Log.d(
                                                        "tourneyUpload",
                                                        "Team tourney is up to date"
                                                    )
                                                    val oldStep =
                                                        tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()]
                                                    val updatedSteps = oldStep!! + currentStepCount
                                                    team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] = updatedSteps

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        team.currentTournaments[tourney.name]!!,
                                                        team
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
                // }
            }

    }

    private fun updateAndStoreTeamDataInSharedPrefs(teamTourney: TeamTournament, team: Team) {
        teamTourney.leafCount = getTotalLeafCountForTeam(teamTourney)
        teamTourney.fruitCount = getTotalFruitCountForTeam(teamTourney)
        teamTourney.tournamentGoalStreak = getTeamGoalStreakForUser(teamTourney, team)
        teamTourney.lastUpdateTime = Timestamp.now()
        teamTourney.dailyGoalsAchieved = calculateDailyGoalsAchieved(teamTourney)
        Log.d("tourneyUpload", "updateAndStoreTeamDataInSharedPrefs")
        var totalSteps = 0
        teamTourney.dailyStepsMap.forEach { (time, steps) ->
            totalSteps += steps
        }
        teamTourney.totalSteps = totalSteps

        synchronized(sharedPrefsRepository.team) {
            val team = sharedPrefsRepository.team
            team.currentTournaments[teamTourney.name] = teamTourney
            sharedPrefsRepository.team = team
        }
        updateUserTeamDataInFirestore()
    }

    private fun updateUserTeamDataInFirestore() {

            Log.d("tourneyUpload", "updateUserTeamDataInFirestore")
            Log.d("tourneyUpload", "Pref " + sharedPrefsRepository.team)
            Log.d("tourneyUpload", "PrefValue " + sharedPrefsRepository.team.currentTournaments)
            Log.d("tourneyUpload", "TeamName " + sharedPrefsRepository.team.name)
            firestoreRepository.updateTeamData(
                sharedPrefsRepository.team.name,
                mapOf("currentTournaments" to sharedPrefsRepository.team.currentTournaments))
                .addOnSuccessListener {
                    Log.d("tourneyUpload", "Team User data upload success")

                    stepCountRepository.getTodayStepCountData {
                        sharedPrefsRepository.storeLastDayStepCount(it)
                    }
                }
                .addOnFailureListener {
                    Log.e("tourneyUpload", "Team User data upload failed")
                }
    }

    private fun updateAndStoreUserDataInSharedPrefs(userTourney: UserTournament){

        synchronized(sharedPrefsRepository.user){
            val user = sharedPrefsRepository.user
            user.currentTournaments[userTourney.name] = userTourney
            sharedPrefsRepository.user = user
        }
        // updateUserTourneyDataInFirestore()
    }

    private fun getTotalLeafCountForTeam(teamTournament: TeamTournament): Int {
        Log.d("WorkerT","getTotalLeafCountForTeam")
        val stepsMap = teamTournament.dailyStepsMap
        val goal = teamTournament.goal
        var leafCount = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
            //for (i in 0 until keys.size) {
            Log.d("WorkerT","StepMap "+ stepsMap[keys[i]])
            leafCount += calculateLeafCountFromStepCountForTeam(stepsMap[keys[i]]!!, goal)
        }
        leafCount += stepsMap[keys[keys.size-1]]!! / 3000


        return leafCount
    }

    private fun getTotalFruitCountForTeam(tournament: TeamTournament): Int {
        Log.d("WorkerT","getTotalFruitCountForTeam")
        val joinDate = DateTime(tournament.joinDate)
        val currentDate = DateTime()
        val days = Days.daysBetween(joinDate, currentDate).days
        val weeks = Math.ceil(days/7.0).toInt()

        var fruitCount = 0

        var weekStartDate = joinDate
        var newWeekDate = weekStartDate.plusWeeks(1)
        var mapPartition: Map<String, Int>
        for (i in 0 until weeks) {
            mapPartition = tournament.dailyStepsMap.filter {
                val keyAsLong = it.key.toLong()
                keyAsLong >= weekStartDate.millis && keyAsLong < newWeekDate.millis
            }
            fruitCount += calculateTeamFruitCountForWeek(tournament, mapPartition)
            weekStartDate = newWeekDate
            newWeekDate = weekStartDate.plusWeeks(1)
        }

        return fruitCount
    }

    private fun calculateTeamFruitCountForWeek(tournament: TeamTournament, stepCountMap: Map<String, Int>): Int {
        Log.d("WorkerT","calculateTeamFruitCountForWeek")
        var currentDay = 0
        val goalAchievedStreak = arrayOf(false, false, false, false, false, false, false)
        val fullStreak = arrayOf(true, true, true, true, true, true, true)

        if (stepCountMap.size < 7) return 0

        stepCountMap.forEach { (_, stepCount) ->
            Log.d("WorkerT", "currentDay "+ currentDay)
            goalAchievedStreak[currentDay] =
                stepCount >= tournament.goal
            currentDay++
        }

        return if (goalAchievedStreak.contentEquals(fullStreak)) 1 else -1
    }

    private fun getTeamGoalStreakForUser(tournament: TeamTournament, team: Team): Int {
        Log.d("WorkerT","getTeamGoalStreakForUser")
        val teamTournamentData = team.currentTournaments[tournament.name]!!
        var streakCount = 0

        teamTournamentData.dailyStepsMap.forEach { (date, stepCount) ->
            //This check prevents resetting streak count if goal is yet to be met today
            if (date.toLong() < DateTime().withTimeAtStartOfDay().millis) {
                if (stepCount >= tournament.goal) streakCount++
                else streakCount = 0
            }
        }
        return streakCount
    }

    fun calculateDailyGoalsAchieved(teamTournament: TeamTournament): Int{

        Log.d("WorkerT","getTotalLeafCountForTeam")
        val stepsMap = teamTournament.dailyStepsMap
        val goal = teamTournament.goal
        var dailyGoalsAchieved = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
            //for (i in 0 until keys.size) {
            Log.d("WorkerT","DailyGoalStepMap "+ stepsMap[keys[i]])
            dailyGoalsAchieved += calculateDailyGoalsAchievedFromStepCountForTeam(stepsMap[keys[i]]!!, goal)
        }

        dailyGoalsAchieved += if(stepsMap[keys[keys.size-1]]!! > goal) 1 else 0

        return dailyGoalsAchieved
    }

    fun startWorkerForTournaments(){

        val mConstraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val updateTeamDataRequest =
            PeriodicWorkRequestBuilder<UpdateTeamDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(mConstraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance().enqueueUniquePeriodicWork(
            "teamWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateTeamDataRequest,
        )
    }
}

