package dal.mitacsgri.treecare.screens.tournaments

import android.text.Spanned
import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.*
import androidx.work.ListenableWorker
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.Days
import kotlin.collections.ArrayList

class TournamentsViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository,
    private val stepCountRepository: StepCountRepository
): ViewModel(){

//    companion object Types {
//        const val ACTIVE_TOURNAMENTS = 0
//        const val TOURNAMENTS_BY_YOU = 1
//    }

    val activeTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val currentTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    var teamsHolder = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    var existingTeams = MutableLiveData<ArrayList<String>>().default(arrayListOf())

    val MessageStatus = MutableLiveData<String>()
    var messageDisplayed = false  //for current tournament frag
    var messageDisplayed2 = false //for active tournament frag

    val totalSteps = MutableLiveData<Int>().default(0)
    var c = 0 // for counting teams in forloop when team joins a tournament
    var cr = 0 // to check for count before uploading team tourney data into firestore, part of the worker for updating team data. It's been added here for testing purposes.
    var currentStepCount = 0

    //The error status message must contain 'error' in string because it is used to check whether to
    //disable or enable join button
    val statusMessage = MutableLiveData<String>()

    fun getAllActiveTournaments(): MutableLiveData<ArrayList<Tournament>> {
        firestoreRepository.getAllActiveTournaments()
            .addOnSuccessListener {
                activeTournamentsList.value?.clear()
                activeTournamentsList.value =
                    it.toObjects<Tournament>().filter { it.exist }.toArrayList()
                activeTournamentsList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active tournaments", "Fetch failed: $it")
            }
        return activeTournamentsList
    }


    fun updateTeamDB(){
        Log.d("Test", "Inside UpdateDB")

        val team = sharedPrefsRepository.team
        var county = 0
        team.currentTournaments.forEach { (_, tourney) ->
            if (tourney.isActive && tourney.endDate.toDateTime().millis > DateTime().millis && tourney.startDate.toDateTime().millis <= DateTime().millis)
                county++
        }

        if (!team.currentTournaments.isNullOrEmpty()) {
            team.currentTournaments.forEach { (_, tourney) ->
                val startTimeMillis = tourney.startDate.toDateTime().millis
                val endTimeMillis = tourney.endDate.toDateTime().millis
                if (tourney.isActive && endTimeMillis > DateTime().millis && startTimeMillis <= DateTime().millis) {
                    stepCountRepository.getTodayStepCountData {
                        Log.d("WorkerT", "TourneyName: " + tourney.name)
                        calc(team, it, county, tourney)
                    }
                }
            }
        }
        else{
            Log.d("Test", "Current TOurnaments is empty")
        }
    }



    fun getCurrentTournamentsForUser(): MutableLiveData<ArrayList<Tournament>> {
// directly fetching current tournaments from db.

        val uid = sharedPrefsRepository.user.uid
        sharedPrefsRepository.team = Team()

        var c = sharedPrefsRepository.user
        c.currentTournaments.clear()
        sharedPrefsRepository.user = c
        Log.d("Teamy", sharedPrefsRepository.user.currentTournaments.toString())
//        for(tours in curr){
//            var tour = tours
//        }


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

                if(tournaments?.isNotEmpty()!!){
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

                                updateLastDayStepCountIfNeeded()

                                Log.d("Test", "tournament exists")
                                //currentTournamentsList.value?.sortAndAddToList(tourney)
                                //currentTournamentsList.notifyObserver()
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
                                        if(tourney.startTimestamp.toDateTime().withTimeAtStartOfDay().millis == DateTime().withTimeAtStartOfDay().millis)
                                            getTeamTournament(tourney, sharedPrefsRepository.getDailyStepCount(), userTeam.name, true)
                                        else
                                            getTeamTournament(tourney, sharedPrefsRepository.getDailyStepCount(), userTeam.name, false)
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
                            //}
                            synchronized(currentTournamentsList.value!!) {
                                currentTournamentsList.value?.sortAndAddToList(tourney)
                                currentTournamentsList.notifyObserver()
                            }

//                            val updateUserTournamentDataRequest =
//                                OneTimeWorkRequestBuilder<UpdateUserTournamentDataWorker>().build()
//                            WorkManager.getInstance()
//                                .enqueue(updateUserTournamentDataRequest).result.addListener(
//                                Runnable {
//                                    Log.d("User Tournament data", "updated by work manager")
//                                }, MoreExecutors.directExecutor()
//                            )
//                            if (sharedPrefsRepository.getLastDayStepCount() > 0){
//                            val updateTeamDataRequest =
//                                OneTimeWorkRequestBuilder<UpdateTeamDataWorker>().build()
//                            WorkManager.getInstance()
//                                .enqueue(updateTeamDataRequest).result.addListener(
//                                Runnable {
//                                    Log.d("Team data", "updated by work manager")
//                                }, MoreExecutors.directExecutor()
//                            )
                            //}

                            //updateTeamDB() //Was added to mimic UpdateTeamDataWorker
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
            tournamentTotalStepsCount = teamTournament.totalSteps
            //tournamentTotalStepsCount = getDailyStepCount()
            Log.d("Unity", tournamentTotalStepsCount.toString())
            action()
        }
    }

    fun getTournamentDurationText(tournament: Tournament): SpannedString {
        val finishDate = tournament.finishTimestamp.toDateTime().millis
        val finishDateString = tournament.finishTimestamp.toDateTime().getMapFormattedDate()

        val tournamentEnded = finishDate < DateTime().millis

        return buildSpannedString {
            bold {
                append(if (tournamentEnded) "Ended: " else "Ends: ")
            }
            append(finishDateString)
        }
    }

    fun getTournamentStartDate(tournament: Tournament): SpannedString {
        val startDate = tournament.startTimestamp.toDateTime().millis
        val startDateString = tournament.startTimestamp.toDateTime().getMapFormattedDate()
        val tournamentEnded = startDate > DateTime().millis

        return buildSpannedString {
            bold {
                append(if (tournamentEnded) "Starts: " else "Started: ")
            }
            append(startDateString)
        }
    }

    /*fun hasTeamJoinedTournament(tournament: Tournament): Boolean {
        //return sharedPrefsRepos itory.team.currentTournaments.contains(tournament.name) != null
        return sharedPrefsRepository.user.currentTournaments[tournament.name] != null
    }*/

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

                Log.d("Test", "insideEnrollTeams" + team)
                firestoreRepository.getTournament(tournamentName)
                    .addOnSuccessListener {
                        val tournament = it.toObject<Tournament>()

                        //getTeamTotalSteps(team)

                        val teamTournament = tournament?.let { it1 ->
                            if(item.startTimestamp.toDateTime().withTimeAtStartOfDay().millis == DateTime().withTimeAtStartOfDay().millis)
                                getTeamTournament(it1, 0, team, true)
                            else
                                getTeamTournament(it1, 0, team, false)
                        }  // sending 0 for steps and setting it again at line 358 after TODO steps

                        if (existingTeams.value?.contains(team)!!) {
                            Log.d("Test", existingTeams.value.toString())
                            messageDisplayed2 = false
                            MessageStatus.value = "Team has already been enrolled"
                        }
                        else if (existingTeams.value?.contains(team)!!) {
                        Log.d("Test", existingTeams.value.toString())
                        messageDisplayed2 = false
                        MessageStatus.value = "Team has already been enrolled"
                        }
                        else if (tournament?.teams?.count() == tournament?.teamLimit) {
                            messageDisplayed2 = false
                            MessageStatus.value = "Tournament is full"
                        }
                        else if(item.startTimestamp.toDateTime().withTimeAtStartOfDay().millis < DateTime().withTimeAtStartOfDay().millis){
                            messageDisplayed2 = false
                            MessageStatus.value = "The Tournament has already begun!"
                        }
                        else {
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
                                                                                getTeamTournament(it1, 0, team, true)
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
                                                                    c=0
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

                                    teamsHolder = MutableLiveData<ArrayList<String>>().default(arrayListOf())
                                    existingTeams = MutableLiveData<ArrayList<String>>().default(arrayListOf())

//                                    val pmz = existingTeams
//                                    pmz.value?.clear()
//                                    existingTeams = pmz

//                                    currentTournamentsList.value?.add(tournament!!)
//                                    currentTournamentsList.notifyObserver()
//                                    getCurrentTournamentsForUser()
                                    //activeTournamentsList.notifyObserver()
                                    //getAllActiveTournaments()
//                                        userTournament?.leafCount =
//                                            sharedPrefsRepository.getDailyStepCount() / 1000

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
//                val updateUserChallengeDataRequest =
//                    OneTimeWorkRequestBuilder<UpdateUserTournamentDataWorker>().build()
//                WorkManager.getInstance().enqueue(updateUserChallengeDataRequest).result.addListener(
//                    Runnable {
//                        Log.d("Challenge data", "updated by work manager")
//                    }, MoreExecutors.directExecutor())
            }
    }

    private fun getUserTournament(tournament: Tournament, team: String, bool:Boolean) =
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


    //TODO: to call this method everytime a user visits the tournament fragment
    //TODO: so that the prefs are updated for unity before opening the tree

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

//        val user = sharedPrefsRepository.user
//
//        if(!user.currentTournaments.isNullOrEmpty()) {
//            user.currentTournaments.forEach { (_, tourney) ->
//                c++
//                val endTimeMillis = tourney.endDate.toDateTime().millis
//                //Two condition checks are applied because the 'isActive' variable is set only after
//                //the dialog has been displayed. The second condition check prevents update of Tournament step count
//                //in the database even when the dialog has not been displayed
//                if (tourney.isActive && endTimeMillis > DateTime().millis) {
//                    //if (tourney.isActive) {
//                    Log.d("WorkerTournament", "TourneyName "+ tourney.name)
//                    stepCountRepository.getTodayStepCountData {
//                        tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
//                        updateAndStoreUserTournamentDataInSharedPrefs(tourney)
//                    }
//                }
//            }
//            if(c== user.currentTournaments.size) {
//                Log.d("WorkerTournament", "count "+ c+ "size "+ user.currentTournaments.size)
//                //updateUserTournamentDataInFirestore()
//            }
//        }
//
//        else {
//            Log.d("WorkerTournament","CurrentTournament is empty")
//        }

//        val team = sharedPrefsRepository.team
//        var county = 0
//
//        team.currentTournaments.forEach { (_, tourney) ->
//            if (tourney.isActive && tourney.endDate.toDateTime().millis > DateTime().millis && tourney.startDate.toDateTime().millis <= DateTime().millis)
//                county++
//        }
//
//        if (!team.currentTournaments.isNullOrEmpty()) {
//            team.currentTournaments.forEach { (_, tourney) ->
//                val startTimeMillis = tourney.startDate.toDateTime().millis
//                val endTimeMillis = tourney.endDate.toDateTime().millis
//                if (tourney.isActive && endTimeMillis > DateTime().millis && startTimeMillis <= DateTime().millis) {
//                    stepCountRepository.getTodayStepCountData {
//                        Log.d("WorkerT", "TourneyName: " + tourney.name)
//                        calc(team, it, county, tourney)
//                    }
//                }
//            }
//        }
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


//    private fun calc(team: Team, currentStepCount: Int) {
//        //Sorting the step map according to the dates
//        Log.d("WorkerT", "Inside Calc")
//
//        team.currentTournaments.forEach { (_, teamTourney) ->
//            teamTourney.dailyStepsMap = teamTourney.dailyStepsMap.toSortedMap()
//        }
//
//        team.currentTournaments.forEach { (_, teamTourney)  ->
//            if (teamTourney.isActive && teamTourney.endDate.toDateTime().millis > DateTime().millis) {
//                if (teamTourney.dailyStepsMap.isNotEmpty()) {
//                    Log.d("WorkerT", "pref not empty")
////                    Log.d("WorkerT", teamTourney.dailyStepsMap.keys.last() + " " + tourney.dailyStepsMap.keys.elementAt(index))
//
//                    if (teamTourney.dailyStepsMap.keys.last() != DateTime().withTimeAtStartOfDay().millis.toString()) {
//                        Log.d("WorkerT", "pref needs to be updated")
//                        //when tournament exists and user starts a new day // user opens the app first day for a day
//                        //condition check for new day. Since only one time stamp (date in millis) is used during the update of values
//                        //DB fetch is needed
//                        firestoreRepository.getTeam(team.name)
//                            .addOnSuccessListener {
//
//                                val teamDB = it.toObject<Team>()
//
//                                if (teamDB?.currentTournaments!![teamTourney.name]?.dailyStepsMap?.isNotEmpty()!!) {
//                                    Log.d("WorkerT", "DB not empty for tournament ${teamTourney.name}")
//                                    //check for today's date stamp in db
//                                    // set DB value to pref and update back to DB
//                                    Log.d("WorkerT", "Last Dates: pref"+ teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap?.keys?.last().toString() +"Date "+ DateTime().withTimeAtStartOfDay().millis.toString())
//                                    if(teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap?.keys?.last().toString() == DateTime().withTimeAtStartOfDay().millis.toString()) {
//                                        Log.d("WorkerT", "DB up to Date for${teamTourney.name}")
//                                        val oldStep = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
//                                        //todo: user needs to update steps for starting a new day
//                                        val diff = currentStepCount - sharedPrefsRepository.getLastDayStepCount()
//                                        Log.d("WorkerT", "Diff1 "+ diff)
//                                        val updatedSteps = oldStep!! + diff
//
//                                        if (diff > 0) {
//                                            teamTourney.dailyStepsMap =
//                                                teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//
//                                            teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                                updatedSteps
//
//                                            updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                            updateUserTeamDataInFirestore()
//                                            sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                        }
//                                    }
//                                    else{
//                                        Log.d("WorkerT", "DB not up to Date")
//                                        teamTourney.dailyStepsMap = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                        teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                            currentStepCount
//
//                                        updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                        updateUserTeamDataInFirestore()
//                                        sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                    }
//                                }
//
//                                else {
//                                    Log.d("WorkerT", "DB step map empty")
//                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                        currentStepCount
//
//                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                    updateUserTeamDataInFirestore()
//                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                }
//                            }
//                    }
//                    else {
//                        Log.d("WorkerT", "LastDayStepCount " + sharedPrefsRepository.getLastDayStepCount())
//                        Log.d("WorkerT", "pref not up to date")
//                        //TODO: check if last date is in db or not
//                        //val oldStep = tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()]
//                        firestoreRepository.getTeam(team.name)
//                            .addOnSuccessListener {
//                                val teamDB = it.toObject<Team>()
//                                val tournaments = teamDB?.currentTournaments
//                                if(tournaments!![teamTourney.name]?.dailyStepsMap?.keys?.last().toString() != DateTime().withTimeAtStartOfDay().millis.toString()){
//                                    Log.d("WorkerT", "DB is not upto date")
//                                    teamTourney.dailyStepsMap = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount
//
//                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                    updateUserTeamDataInFirestore()
//                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                }
//                                else {
//                                    Log.d("WorkerT", "DB is upto date")
//                                    val oldStep =
//                                        tournaments[teamTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
//                                    val diff =
//                                        currentStepCount - sharedPrefsRepository.getLastDayStepCount()
//                                    Log.d("WorkerT", "Diff2 " + diff)
//                                    if (diff > 0) {
//                                        val updatedSteps = oldStep!! + diff
//                                        teamTourney.dailyStepsMap =
//                                            teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                        teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = updatedSteps
//
//                                        updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                        updateUserTeamDataInFirestore()
//                                        sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                    }
//                                }
//                            }
//                    }
//                }
//
//                else {
//                    Log.d("WorkerT", "prefs is empty")
//                    // When tournament exists but doesn't have steps in the teams collection
//                    // take care of updating team and tour prefs when user has joined a tournament/team (when user is not a captain)
//                    firestoreRepository.getTeam(team.name)
//                        // checking if the tournament has steps updated by other users. If not, then we update it, if present,
//                        // the last step count is fetched and then it is incremented with the users steps and is updated back in the db
//                        .addOnSuccessListener {
//                            val teamDB = it.toObject<Team>()
//                            val tournaments = teamDB?.currentTournaments
//                            if (tournaments!![teamTourney.name]?.dailyStepsMap?.isEmpty()!!) {
//                                Log.d("WorkerT", "DB and pref have empty step Map")
//
//                                teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount
//
//                                updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                updateUserTeamDataInFirestore()
//                                sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                            }
//
//                            else{
//                                Log.d("WorkerT", "DB is upto date and pref is empty")
//                                val oldStep = tournaments[teamTourney.name]?.dailyStepsMap?.values?.last()
//                                //val diff = currentStepCount - sharedPrefsRepository.getLastDayStepCount()
//                                //Log.d("WorkerT", "Diff3 "+ diff)
//                                val updatedSteps = oldStep!! + currentStepCount
//                                //if (diff > 0) {
//                                    teamTourney.dailyStepsMap =
//                                        teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                        updatedSteps
//                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                    updateUserTeamDataInFirestore()
//                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                //}
//                            }
//                        }
//                }
//            }
//        }
//    }

    private fun calc(team: Team, currentStepCount: Int,county: Int, tourney: TeamTournament) {
        Log.d("Test", "Inside calc")
        cr++
        Log.d("WorkerT", "cr: " + cr)
        val userTourneys = sharedPrefsRepository.user
        //Sorting the step map according to the dates
        Log.d("WorkerT", "Inside Calc")
        Log.d("WorkerT", "County: "+ county)

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

                // userTournaments?.forEach { (_, userTourney) ->
                //TODO: End date check is also needed to avoid updating expired tournaments

                if (userTournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!! && userTournaments[tourney.name]?.isActive!!) {
                    Log.d("WorkerT", "Stepmap is empty for ${tourney.name}")

                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                        currentStepCount
                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                        Timestamp.now()
                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                        currentStepCount
                    //sharedPrefsRepository.user = userTourneys
                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)

//                        userTourneys[userTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
//                            currentStepCount
//
//                        userTourneys[userTourney.name]?.lastUpdateTime = Timestamp.now()

//                        sharedPrefsRepository.user = userTourneys

                    Log.d("WorkerT", sharedPrefsRepository.user.currentTournaments[tourney.name].toString())

                    firestoreRepository.updateUserData(
                        user.uid,
                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                    )
                        .addOnSuccessListener {
                            Log.d("WorkerT", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                            Log.d("WorkerT", "stepmap for ${tourney.name} was uploaded successfully")

                            firestoreRepository.getTeam(team.name)
                                .addOnSuccessListener {
                                    val teamDB = it.toObject<Team>()
                                    val teamTourney = teamDB?.currentTournaments
                                    if (teamTourney!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                        Log.d(
                                            "WorkerT",
                                            "Team tourney:${tourney.name} Daily Step Map is empty"
                                        )
                                        team.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                            currentStepCount

                                        updateAndStoreTeamDataInSharedPrefs(
                                            team.currentTournaments[tourney.name]!!,
                                            team,
                                            cr,
                                            county
                                        )
                                        //updateUserTeamDataInFirestore(future)
//                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                currentStepCount
//                                            )
                                    } else {
                                        Log.d("WorkerT", "Daily Step Map is not empty")
                                        if (teamTourney[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                .toString() != DateTime().withTimeAtStartOfDay().millis.toString()
                                        ) {
                                            Log.d(
                                                "WorkerT",
                                                "Team tourney: ${tourney.name} is not up to date"
                                            )
                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                currentStepCount

                                            updateAndStoreTeamDataInSharedPrefs(
                                                team.currentTournaments[tourney.name]!!,
                                                team,
                                                cr,
                                                county
                                            )
                                            //updateUserTeamDataInFirestore(future)
//                                                sharedPrefsRepository.storeLastDayStepCount(
//                                                    currentStepCount
//                                                )
                                        } else {
                                            Log.d(
                                                "WorkerT",
                                                "Team tourney: ${tourney.name} is up to date"
                                            )
                                            val oldStep =
                                                teamTourney[tourney.name]?.dailyStepsMap?.values?.last()
                                            val updatedSteps = oldStep!! + currentStepCount
                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] = updatedSteps


                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                updatedSteps
                                            updateAndStoreTeamDataInSharedPrefs(
                                                team.currentTournaments[tourney.name]!!,
                                                team,
                                                cr,
                                                county
                                            )
                                            //updateUserTeamDataInFirestore(future)
//                                                sharedPrefsRepository.storeLastDayStepCount(
//                                                    currentStepCount
//                                                )
                                        }
                                    }
                                }
                        }
                        .addOnFailureListener {
                            Log.d("WorkerT", "stepmap for ${tourney.name} failed to upload")
                        }

                } else if (tourney.isActive) {
                    Log.d("WorkerT", "User Tourney: ${tourney.name} stepmap is not empty")
                    firestoreRepository.getTeam(team.name)
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            val tournaments = teamDB?.currentTournaments
//                        team.currentTournaments.forEach { (_, tourney) ->
                            if (tourney.isActive) {
                                Log.d("WorkerT", "userStepMap for Tourney ${tourney.name}"+ userTournaments[tourney.name]?.dailyStepsMap?.keys+ "Last "+ userTournaments[tourney.name]?.dailyStepsMap?.keys?.last())
                                if (userTournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                        .toString() == DateTime().withTimeAtStartOfDay().millis.toString()
                                ) {

                                    //Log.d("WorkerT", "userStepMap for Tourney ${tourney.name}"+ tournaments!![tourney.name]?.dailyStepsMap?.keys+ "Last "+ tournaments[tourney.name]?.dailyStepsMap?.keys?.last())
                                    //TODO:replace users daily steps
                                    Log.d("WorkerT", "UserTourney is up to date")

                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                        currentStepCount

                                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                                        Timestamp.now()
                                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                                        currentStepCount
                                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)
                                    //sharedPrefsRepository.user = userTourneys


                                    firestoreRepository.updateUserData(
                                        user.uid,
                                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                                    )
                                        .addOnSuccessListener {
                                            Log.d("WorkerT", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                                            Log.d(
                                                "WorkerT",
                                                "stepmap for ${tourney.name} was uploaded successfully"
                                            )


//                                            firestoreRepository.getTeam(team.name)
//                                                .addOnSuccessListener {
//                                                    val teamDB = it.toObject<Team>()
//                                                    val tournaments = teamDB?.currentTournaments
                                            if (tournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is empty"
                                                )
                                                tournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                    currentStepCount

                                                updateAndStoreTeamDataInSharedPrefs(
                                                    tournaments[tourney.name]!!,
                                                    team,
                                                    cr,
                                                    county
                                                )
                                                //updateUserTeamDataInFirestore(future)
//                                                        sharedPrefsRepository.storeLastDayStepCount(
//                                                            currentStepCount
//                                                        )
                                            } else {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is not empty"
                                                )
                                                if (tournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                        .toString() != DateTime().withTimeAtStartOfDay().millis.toString()
                                                ) {
                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is not up to date"
                                                    )
                                                    tourney.dailyStepsMap =
                                                        teamDB.currentTournaments[tourney.name]?.dailyStepsMap!!
                                                    tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                        currentStepCount

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        tourney,
                                                        team,
                                                        cr,
                                                        county
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                } else {
                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is up to date"
                                                    )
                                                    val oldStep =
                                                        tournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]

                                                    Log.d("WorkerT", "OldStep for tourney: ${tourney.name} " + oldStep)
                                                    Log.d("WorkerT", "Last Day step count "+ sharedPrefsRepository.getLastDayStepCount())
                                                    val diff =
                                                        currentStepCount - sharedPrefsRepository.getLastDayStepCount()
                                                    Log.d("WorkerT", "Diff3 for tourney: ${tourney.name} " + diff)
                                                    val updatedSteps = oldStep!! + diff

                                                    if (diff > 0) {
                                                        tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                            updatedSteps
                                                        updateAndStoreTeamDataInSharedPrefs(
                                                            tourney,
                                                            team,
                                                            cr,
                                                            county
                                                        )
                                                        //updateUserTeamDataInFirestore(future)
//                                                                sharedPrefsRepository.storeLastDayStepCount(
//                                                                    currentStepCount
//                                                                )
                                                    }
//                                                            else if(diff == 0) future.set(
//                                                                ListenableWorker.Result.success())
                                                }
                                            }
                                            //}
                                        }
                                } else {
                                    Log.d("WorkerT", "UserTourney is not up to date")

//                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap =
//                                        userTourney.dailyStepsMap
                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                        currentStepCount

                                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                                        Timestamp.now()
                                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                                        currentStepCount

                                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)

                                    //sharedPrefsRepository.user = userTourneys


                                    firestoreRepository.updateUserData(
                                        user.uid,
                                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                                    )
                                        .addOnSuccessListener {
                                            Log.d("WorkerT", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                                            Log.d(
                                                "WorkerT",
                                                "stepmap for ${tourney.name} was uploaded successfully"
                                            )

//                                            firestoreRepository.getTeam(team.name)
//                                                .addOnSuccessListener {
//                                                    val teamDB = it.toObject<Team>()
//                                                    val tournaments = teamDB?.currentTournaments

//                                            userTourneys.currentTournaments[tourney.name]?.dailyStepsMap = userTourney.dailyStepsMap
//                                            userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount
                                            //TODO: Update user Tourney
                                            if (tournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is empty"
                                                )
                                                tournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                    currentStepCount

                                                updateAndStoreTeamDataInSharedPrefs(
                                                    tournaments[tourney.name]!!,
                                                    team,
                                                    cr,
                                                    county
                                                )
                                                //updateUserTeamDataInFirestore(future)
//                                                        sharedPrefsRepository.storeLastDayStepCount(
//                                                            currentStepCount
//                                                        )
                                            } else {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is not empty"
                                                )
                                                if (tournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                        .toString() != DateTime().withTimeAtStartOfDay().millis.toString()
                                                ) {
                                                    //Data is present in team tourney
                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is not up to date"
                                                    )

                                                    tourney.dailyStepsMap =
                                                        teamDB.currentTournaments[tourney.name]?.dailyStepsMap!!
                                                    tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                        currentStepCount

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        tourney,
                                                        team,
                                                        cr,
                                                        county
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                } else {
                                                    //Data is not present in TeamTourney

                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is up to date"
                                                    )
                                                    val oldStep =
                                                        tournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
                                                    val updatedSteps = oldStep!! + currentStepCount
                                                    team.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] = updatedSteps

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        team.currentTournaments[tourney.name]!!,
                                                        team,
                                                        cr,
                                                        county
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

    private fun updateAndStoreTeamDataInSharedPrefs(teamTourney: TeamTournament, team: Team, cr:Int, county: Int) {
        teamTourney.leafCount = getTotalLeafCountForTeam(teamTourney)
        teamTourney.fruitCount = getTotalFruitCountForTeam(teamTourney)
        teamTourney.tournamentGoalStreak = getTeamGoalStreakForUser(teamTourney, team)
        teamTourney.lastUpdateTime = Timestamp.now()
        Log.d("WorkerT", "updateAndStoreTeamDataInSharedPrefs")
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
        updateUserTeamDataInFirestore(cr, county)
    }

    private fun updateUserTeamDataInFirestore(cr1:Int, county1: Int) {
        if (cr1 == county1) {
            Log.d("WorkerT", "updateUserTeamDataInFirestore")
            Log.d("WorkerT", "Pref " + sharedPrefsRepository.team)
            Log.d("WorkerT", "PrefValue " + sharedPrefsRepository.team.currentTournaments)
            Log.d("WorkerT", "TeamName " + sharedPrefsRepository.team.name)
            firestoreRepository.updateTeamData(
                sharedPrefsRepository.team.name,
                mapOf("currentTournaments" to sharedPrefsRepository.team.currentTournaments))
                .addOnSuccessListener {
                    Log.d("WorkerT", "Team User data upload success")
                    Log.d("WorkerT", "cr: " + cr1 + "county: " + county1)

                    stepCountRepository.getTodayStepCountData {
                        sharedPrefsRepository.storeLastDayStepCount(it)
                    }
                }
                .addOnFailureListener {
                    Log.e("WorkerT", "Team User data upload failed")
                }
        }
    }

    private fun updateAndStoreUserDataInSharedPrefs(userTourney: UserTournament){

        synchronized(sharedPrefsRepository.user){
            val user = sharedPrefsRepository.user
            user.currentTournaments[userTourney.name] = userTourney
            sharedPrefsRepository.user = user
        }
    }

//    private fun updateUserTeamDataInFirestore(cr1:Int, county1: Int) {
//        Log.d("WorkerT", "updateUserTeamDataInFirestore")
//        Log.d("WorkerT", "Pref " + sharedPrefsRepository.team)
//        Log.d("WorkerT", "PrefValue " + sharedPrefsRepository.team.currentTournaments)
//
//        Log.d("WorkerT", "TeamName " + sharedPrefsRepository.team.name)
//        firestoreRepository.updateTeamData(
//            sharedPrefsRepository.team.name,
//            mapOf("currentTournaments" to sharedPrefsRepository.team.currentTournaments)
//        )
//            .addOnSuccessListener {
//                Log.d("WorkerT", "Team User data upload success")
//                Log.d("WorkerT", "cr: "+ cr1 + "county: " + county1)
//                if(cr1 == county1){
//                    stepCountRepository.getTodayStepCountData {
//                        sharedPrefsRepository.storeLastDayStepCount(it)
//                    }
//                }
//
//            }
//            .addOnFailureListener {
//                Log.e("WorkerT", "Team User data upload failed")
//            }
//    }

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
        sharedPrefsRepository.team.currentTournaments.forEach { (_, tourney) ->
            if (tourney.isActive) {
                Log.d("Test", tourney.toString())
            }
        }
        sharedPrefsRepository.user.currentTournaments.forEach { (_, tourney) ->
            if (tourney.isActive)
                Log.d("Test", tourney.toString())
        }
        Log.d("Test", "LAst: "+ sharedPrefsRepository.getLastDayStepCount())
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

    private fun calculateTeamFruitCountForWeek(tournament: TeamTournament, stepCountMap: Map<String, Int>): Int {
        Log.d("WorkerT","calculateTeamFruitCountForWeek")
        var currentDay = 0
        val goalAchievedStreak = arrayOf(false, false, false, false, false, false, false)
        val fullStreak = arrayOf(true, true, true, true, true, true, true)

        if (stepCountMap.size < 7) return 0

        stepCountMap.forEach { (_, stepCount) ->
            goalAchievedStreak[currentDay] =
                stepCount >= tournament.goal
            currentDay++
        }
        return if (goalAchievedStreak.contentEquals(fullStreak)) 1 else -1
    }

    fun calculateLeafCountFromStepCountForTeam(stepCount: Int, dailyGoal: Int): Int {
        var leafCount = stepCount / 3000
        if (stepCount < dailyGoal) {
            leafCount -= Math.ceil((dailyGoal - stepCount) / 3000.0).toInt()
            if (leafCount < 0) leafCount = 0
        }
        return leafCount
    }
}

