package dal.mitacsgri.treecare.screens.tournaments


import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateTeamDataWorker
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateTournamentSteps
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserChallengeDataWorker
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserTournamentDataWorker
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import kotlin.collections.ArrayList

class TournamentsViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository,
    private val stepCountRepository: StepCountRepository
): ViewModel() {

    companion object Types {
        const val ACTIVE_TOURNAMENTS = 0
        const val TOURNAMENTS_BY_YOU = 1
    }

    val activeTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val currentTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val teamsHolder = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    val existingTeams = MutableLiveData<ArrayList<String>>().default(arrayListOf())

    val MessageStatus = MutableLiveData<String>()
    var messageDisplayed = false  //for current tournament frag
    var messageDisplayed2 = false //for active tournament frag

    val totalSteps = MutableLiveData<Int>().default(0)
    var c = 0 // for counting teams in forloop
    var currentStepCount = 0

    //The error status message must contain 'error' in string because it is used to check whether to
    //disable or enable join button
    val statusMessage = MutableLiveData<String>()

    fun getAllActiveTournaments() {
        firestoreRepository.getAllActiveTournaments()
            .addOnSuccessListener {
                activeTournamentsList.value?.clear()
                activeTournamentsList.value = it.toObjects<Tournament>().filter { it.exist }.toArrayList()
                activeTournamentsList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active tournaments", "Fetch failed: $it")
            }
    }


    fun getCurrentTournamentsForUser() {
// directly fetching current tournaments from db.

        val uid = sharedPrefsRepository.user.uid
        sharedPrefsRepository.team = Team()

        //updating steps for user in the db
        stepCountRepository.getTodayStepCountData {
            firestoreRepository.updateUserData(sharedPrefsRepository.user.uid, mapOf("dailySteps" to it))
            currentStepCount = it
        }


//        Log.d("Test", "SharedPref" + sharedPrefsRepository.user.currentTournaments)
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {

                val user = it.toObject<User>()
                val tournaments = user?.currentTournaments
                Log.d("Test", "TeamDB "+ user?.currentTeams.toString())
                val team = user?.currentTeams.toString().removeSurrounding("[","]")
                if(team.isNotEmpty()) {
                    Log.d("Test", "Team is not empty")
                    firestoreRepository.getTeam(team)
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            sharedPrefsRepository.team = teamDB!!
                        }
                }
                else Log.d("Test","Team is empty")

                for (tournament in tournaments!!) {
                    Log.d("Test", "tournament" + tournament.key)
                    firestoreRepository.getTournament(tournament.key)
                        .addOnSuccessListener {

                            val tourney = it.toObject<Tournament>() ?: Tournament(exist = false)
//                            synchronized(currentTournamentsList.value!!) {
                            if (tourney.exist && tourney.active) {
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
                                    userTeam.currentTournaments[tourney.name] = getTeamTournament(
                                        tourney,
                                        sharedPrefsRepository.getDailyStepCount(),
                                        userTeam.name
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
                        }
                        .addOnFailureListener {
                            Log.d("Tournament not Found", it.toString())
                        }
                }

//                val updateUserTournamentDataRequest = OneTimeWorkRequestBuilder<UpdateUserChallengeDataWorker>().build()
//
//                WorkManager.getInstance().enqueue(updateUserTournamentDataRequest).result.addListener(
//                    Runnable {
//                        Log.d("Tournament data", "updated by work manager")
//                    }, MoreExecutors.directExecutor())
            }
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
        Log.d("Test", "sharedPref" + userTournament?.teamName)
        if (userTournament?.teamName?.contains(user.captainedTeams.toString().removeSurrounding("[","]"))!!) {

            Log.d("Test", "Teams " + userTournament.teamName)

            firestoreRepository.getTeam(userTournament.teamName.removeSurrounding("[","]"))  //removing prefix and suffix to make db call
                .addOnSuccessListener {

                    val team = it.toObject<Team>()
                    val members = team?.members
                    Log.d("Test","member"+ members)
                    for (member in members!!) {
                        firestoreRepository.deleteTournamentFromUserDB(member, tournament.name)
                            .addOnSuccessListener {
                                firestoreRepository.deleteTournamentFromTeamDB(team.name, tournament.name)
                                    .addOnSuccessListener {

                                        firestoreRepository.updateTournamentData(tournament.name, mapOf("teams" to FieldValue.arrayRemove(team.name)))
                                            .addOnSuccessListener {
                                                messageDisplayed = false
                                                statusMessage.value = "Your team is no longer a part of the tournament ${tournament.name}"

//                                                var index = currentTournamentsList.value?.indexOf(tournament)
//                                                Log.d("Test","Tname "+ team.name)
//                                                currentTournamentsList.value?.get(index!!)?.teams?.remove(team.name)
//                                                currentTournamentsList.notifyObserver()

                                                removeTournamentFromCurrentTournamentsLists(tournament)

//                                                index = activeTournamentsList.value?.indexOf(tournament)
//                                                activeTournamentsList.value?.get(index!!)?.teams?.remove(team.name)
//                                                activeTournamentsList.notifyObserver()
                                            }

                                            .addOnFailureListener {
                                                Log.d("Test", it.toString())
                                                firestoreRepository.updateTournamentData(tournament.name, mapOf("teams" to FieldValue.arrayUnion(team.name)))
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
        }

        else {
            messageDisplayed = false
            statusMessage.value = "Only the team captain can remove the team from the tournament"
            Log.d("Test", "Only the captain can leave a tournament.")
        }

    }

//    fun deleteTournament(tournament: Tournament) {
//        firestoreRepository.setTournamentAsNonExist(tournament.name)
//            .addOnSuccessListener {
//                activeTournamentsList.value?.remove(tournament)
//                activeTournamentsList.notifyObserver()
//
//                currentTournamentsList.value?.remove(tournament)
//                currentTournamentsList.notifyObserver()
//
//                tournamentsByYouList.value?.remove(tournament)
//                tournamentsByYouList.notifyObserver()
//            }
//            .addOnFailureListener {
//                Log.e("Deletion failed", it.toString())
//            }
//    }

    fun startUnityActivityForTournament(tournament: Tournament, action: () -> Unit) {
        sharedPrefsRepository.apply {

            val userTournament = user.currentTournaments[tournament.name]!!
            gameMode = TOURNAMENT_MODE
            tournamentType = userTournament.type
            tournamentGoal = userTournament.goal
            tournamentLeafCount = userTournament.leafCount
            tournamentFruitCount = userTournament.fruitCount
            tournamentStreak = userTournament.tournamentGoalStreak
            tournamentName = userTournament.name
            isTournamentActive = userTournament.endDate.toDateTime().millis > DateTime().millis
            tournamentTotalStepsCount = if (tournament.active) getDailyStepCount() else userTournament.totalSteps
            action()
        }
    }

    fun getTournamentDurationText(tournament: Tournament): SpannedString {
        val finishDate = tournament.finishTimestamp.toDateTime().millis
        val finishDateString = tournament.finishTimestamp.toDateTime().getStringRepresentation()

        val tournamentEnded = finishDate < DateTime().millis

        return buildSpannedString {
            bold {
                append(if (tournamentEnded) "Ended: " else "Ends: ")
            }
            append(finishDateString)
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

//    private fun updateUserSharedPrefsData(userTournament: UserTournament) {
//        val user = sharedPrefsRepository.user
//        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
//        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
//        user.currentTournaments[userTournament.name] = userTournament
//        sharedPrefsRepository.user = user
//    }


//    private fun removeTournamentFromCurrentTournamentsLists(tournament: Tournament) {
//        currentTournamentsList.value?.remove(tournament)
//        currentTournamentsList.notifyObserver()
//
//        sharedPrefsRepository.user = sharedPrefsRepository.user.let {
//            it.currentTournaments.remove(tournament.name)
//            it
//        }
//    }

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

    fun enrollTeams(tournamentName: String) {
        //TO check if a team has already been enrolled in the tournament

        //Sorry about the mess in this function but it was inevitable during this time. Welcome to callback hell! A possible alternative is using coroutines.
        teamsHolder.value?.add(sharedPrefsRepository.user.captainedTeams.toString().removeSurrounding("[", "]").trimEnd())

        Log.d("Test", "TeamsValue " + teamsHolder.value.toString().removeSurrounding("[", "]").trimEnd())

        if (sharedPrefsRepository.user.captainedTeams.isNotEmpty()) {
            for (team in teamsHolder.value!!) {

                Log.d("Test", "insideEnrollTeams" + team)
                firestoreRepository.getTournament(tournamentName)
                    .addOnSuccessListener {
                        val tournament = it.toObject<Tournament>()

                        //getTeamTotalSteps(team)

                        val teamTournament = tournament?.let { it1 -> getTeamTournament(it1, 0, team) }  // sending 0 for steps and setting it again at line 358 after TODO steps

                        if (existingTeams.value?.contains(team)!!) {
                            Log.d("Test", existingTeams.value.toString())
                            messageDisplayed2 = false
                            MessageStatus.value = "Team has already been enrolled"
                        } else if (tournament?.teams?.count() == tournament?.teamLimit) {
                            messageDisplayed2 = false
                            MessageStatus.value = "Tournament is full"
                        } else {
                            firestoreRepository.updateTournamentData(tournamentName, mapOf("teams" to FieldValue.arrayUnion(team)))
                                .addOnSuccessListener {
                                    Log.d("Test", "TeamName " + team)

                                    mapOf("currentTournaments.${tournament?.name}" to teamTournament).let { it1 ->
                                        firestoreRepository.updateTeamTournamentData(team, it1)}
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
                                                                    Log.d("StepTest", "dailySteps ${user.uid} " + steps.toString())
                                                                    totalSteps.value = totalSteps.value?.plus(steps)
                                                                    totalSteps.notifyObserver()
                                                                    if(c == size){
                                                                        //TODO: steps
                                                                        val updateTotalSteps =  tournament?.let { it1 -> getTeamTournament(it1, totalSteps.value?.toInt()!!, team) }
                                                                        updateTeamSharedPrefsData(updateTotalSteps!!)
                                                                        mapOf("currentTournaments.${tournament.name}" to updateTotalSteps).let { it1 ->
                                                                            firestoreRepository.updateTeamTournamentData(team, it1)}
                                                                            .addOnSuccessListener {
                                                                                addTournament(team, tournamentName, updateTotalSteps)
                                                                            }
                                                                            .addOnFailureListener {
                                                                                mapOf("currentTournaments.${tournament.name}" to updateTotalSteps).let { it1 ->
                                                                                    firestoreRepository.deleteTournamentFromTeamDB(team, tournamentName)}
                                                                            }
                                                                    }
                                                                    Log.d("StepTest", "totalsteps " + totalSteps.value?.toInt().toString())
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
                                            MessageStatus.value = "Unable to Enroll. Please Try Again later"
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
                        val userTournament = tournament?.let { it1 -> getUserTournament(it1, team) }

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
                val updateUserChallengeDataRequest =
                    OneTimeWorkRequestBuilder<UpdateUserTournamentDataWorker>().build()
                WorkManager.getInstance().enqueue(updateUserChallengeDataRequest).result.addListener(
                    Runnable {
                        Log.d("Challenge data", "updated by work manager")
                    }, MoreExecutors.directExecutor())
            }
    }

    private fun getUserTournament(tournament: Tournament, team: String) =
        UserTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = 0,
            joinDate = DateTime().millis,
            goal = tournament.dailyGoal,
            startDate = tournament.startTimestamp,
            endDate = tournament.finishTimestamp,
            teamName = team.removeSurrounding("[","]")
        )

    private fun getTeamTournament(tournament: Tournament, steps: Int, team: String) =
        TeamTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = steps,
            joinDate = DateTime().millis,
            goal = tournament.dailyGoal,
            startDate = tournament.startTimestamp,
            endDate = tournament.finishTimestamp,
            teamName = team
        )


    //TODO: to call this method everytime a user visits the tournament fragment
    //TODO: so that the prefs are updated for unity before opening the tree

    private fun updateUserSharedPrefsData(userTournament: UserTournament) {
        val user = sharedPrefsRepository.user
        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 3000
        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        user.currentTournaments[userTournament.name] = userTournament
        sharedPrefsRepository.user = user
    }

    fun updateTeamSharedPrefsData(teamTournament: TeamTournament){
        val team = sharedPrefsRepository.team
        teamTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 3000
        teamTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        team.currentTournaments[teamTournament.name] = teamTournament
        sharedPrefsRepository.team = team
    }

    fun display() {

//        var tour = sharedPrefsRepository.team
//        val tourey = sharedPrefsRepository.user

//        tourey.currentTournaments.forEach { (_, tourneys) ->
//            if(tourneys.name == "Workling"){
//                tourneys.isActive = false
//            }
//        }

//        tour.currentTournaments.forEach { (_, tourney) ->
//            if(tourney.name == "NewTourney"){
//                tourney.dailyStepsMap.remove("1593313200000")
//            }
////             tourney.dailyStepsMap = tourney.dailyStepsMap.toSortedMap()
////             Log.d("Test", " Sorted ${tourney.name}" +tourney.dailyStepsMap.toSortedMap())
//        }
//        sharedPrefsRepository.storeLastDayStepCount(654)
//
//        sharedPrefsRepository.team = tour
//        sharedPrefsRepository.user = tourey

//        Log.d("Test","Time $Timestamp(Date(1592535600000).toString()")

        Log.d("Test", "Team " + sharedPrefsRepository.team)
        Log.d("Test", "currentTourney " + sharedPrefsRepository.user.currentTournaments)
        Log.d("Test", "LAst "+ sharedPrefsRepository.getLastDayStepCount())
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
}

