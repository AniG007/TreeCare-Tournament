package dal.mitacsgri.treecare.screens.tournaments


import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

class TournamentsViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    companion object Types {
        const val ACTIVE_TOURNAMENTS = 0
        const val TOURNAMENTS_BY_YOU = 1
    }

    val activeTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val currentTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val tournamentsByYouList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())

    val teamsList = MutableLiveData<ArrayList<Team>>().default(arrayListOf())
    val teamsHolder = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    val existingTeams = MutableLiveData<ArrayList<String>>().default(arrayListOf())

    val MessageStatus = MutableLiveData<String>()
    var messageDisplayed = false  //for current tournament frag

    var messageDisplayed2 = false //for active tournament frag

    //The error status message must contain 'error' in string because it is used to check whether to
    //disable or enable join button
    val statusMessage = MutableLiveData<String>()

    fun getAllActiveTournaments() {
        firestoreRepository.getAllActiveTournaments()
            .addOnSuccessListener {
                activeTournamentsList.value = it.toObjects<Tournament>().filter { it.active }.toArrayList()
                activeTournamentsList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active tournaments", "Fetch failed: $it")
            }
    }


    fun getCurrentTournamentsForUser() {
// directly fetching current tournaments from db.
        val uid = sharedPrefsRepository.user.uid

        Log.d("Test", "SharedPref" + sharedPrefsRepository.user.currentTournaments)
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {

                val user = it.toObject<User>()
                val tournaments = user?.currentTournaments

                for (tournament in tournaments!!) {
                    Log.d("Test", "tournament" + tournament.key)
                    firestoreRepository.getTournament(tournament.key)
                        .addOnSuccessListener {

                            val tourney = it.toObject<Tournament>() ?: Tournament(exist = false)
                            synchronized(currentTournamentsList.value!!) {
                                //Log.d("Test","inside synchronised")
                                //Log.d("Test","Tourney ${tourney}")
                                if (tourney.exist) {
                                    Log.d("Test", "tournament exists")
                                    currentTournamentsList.value?.sortAndAddToList(tourney)
                                    currentTournamentsList.notifyObserver()
                                    Log.d(
                                        "Test",
                                        "currentTourney List" + currentTournamentsList.value
                                    )
                                }
                            }
                            //Log.d("Test","tourneyname ${tourneyName}")
                        }
                        .addOnFailureListener {
                            Log.d("Tournament not Found", it.toString())
                        }

                }
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
        Log.d("Test","SharedPref"+sharedPrefsRepository.user.currentTournaments)
        Log.d("Test","userTourney"+userTournament)
        if(user.captainedTeams.contains(userTournament?.teamName)){

            Log.d("Test","Teams "+userTournament?.teamName)

            firestoreRepository.getTeam(userTournament?.teamName!!)
                .addOnSuccessListener {
                    val team = it.toObject<Team>()
                    val members = team?.members
                    for(member in members!!){

                          firestoreRepository.deleteTournamentFromUserDB(member,tournament.name)
                            .addOnSuccessListener {
                                firestoreRepository.updateTeamData(
                                    team.name,
                                    mapOf("currentTournaments" to FieldValue.arrayRemove(tournament.name))
                                )
                                    .addOnSuccessListener {
                                        firestoreRepository.updateTournamentData(tournament.name, mapOf("teams" to FieldValue.arrayRemove(team.name)))
                                            .addOnSuccessListener {
                                                //Log.d("Test", "Removed from tournament successfully")
                                                currentTournamentsList.value?.remove(tournament)
                                                currentTournamentsList.notifyObserver()
                                                statusMessage.value = "Your team is no long part of the tournament ${tournament.name}"
                                                sharedPrefsRepository.user.currentTournaments.remove(tournament.name)
                                                val index = activeTournamentsList.value?.indexOf(tournament)
                                                //activeTournamentsList.value?.get(index!!)?.teams?.remove(team.name)
                                                //activeTournamentsList.notifyObserver()
                                            }
                                            .addOnFailureListener {
                                                Log.d("Test", it.toString())
                                            }
                                        //       Log.d("Test", "Removed from team successfully")
                                    }
                                // Log.d("Test", "Removed from User successfully")
                            }
                    }
                }
        }

        else{
                statusMessage.value = "Only the team captain can remove the team from the tournament"
                Log.d("Test","Only the captain can leave a tournament.")
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
            append(tournament.goal.toString())
        }

    fun getTeamsCountText(tournament: Tournament) = tournament.teams.size.toString()

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

        for(i in 0 until size) {
            if (this[i].finishTimestamp.toDateTime().millis < finishTimestampMillis) {
                add(i, tournament)
                return
            }
        }
        this.add(tournament)
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    fun getExistingTeams (tournamentName: String){
        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                if(it.exists()) {
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
        teamsHolder.value?.add(sharedPrefsRepository.user.captainedTeams.toString().removeSurrounding("[","]").trimEnd())
        Log.d("Test","TeamsValue "+teamsHolder.value.toString().removeSurrounding("[","]").trimEnd())
        if(sharedPrefsRepository.user.captainedTeams.isNotEmpty()) {
            for (team in teamsHolder.value!!) {

                Log.d("Test", "insideEnrollTeams" + team)
                firestoreRepository.getTournament(tournamentName)
                    .addOnSuccessListener {
                        val tournament = it.toObject<Tournament>()
                        if (existingTeams.value?.contains(team)!!) {
                            Log.d("Test", existingTeams.value.toString())
                            MessageStatus.value = "Team has already been enrolled"
                        } else if (tournament?.teams?.count() == tournament?.teamLimit) {
                            MessageStatus.value = "Tournament is full"
                        } else {
                            //Log.d("Test", "Inside else")
                            firestoreRepository.updateTournamentData(tournamentName, mapOf("teams" to FieldValue.arrayUnion(team)))
                                .addOnSuccessListener {
                                    Log.d("Test", "TeamName " + team)
                                    firestoreRepository.updateTeamData(team, mapOf("currentTournaments" to FieldValue.arrayUnion(tournamentName)))
                                        .addOnSuccessListener {
                                            Log.d("Test", "core")
                                            addTournament(team, tournamentName)
                                        }
                                        .addOnFailureListener {
                                            Log.d("Test", "core fail " + it.toString())
                                        }
                                }
                                .addOnFailureListener {
                                    Log.d("Test", "outside core fail " + it.toString())
                                    MessageStatus.value = "Unable to Enroll. Please Try Again later"
                                }
                        }
                    }
            }
        }
        else{
            MessageStatus.value = "Only team captains can enroll their teams in tournaments"
        }
    }

    fun addTournament(team: String, tournamentName : String) {

        Log.d("Test","Inside Add tourney")
        //Adding tournament to currentTournaments in Users Collection
        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                val tournament = it.toObject<Tournament>()
                firestoreRepository.getTeam(team)
                    .addOnSuccessListener {
                        val teamData = it.toObject<Team>()
                        val members = teamData?.members
                        val userTournament =
                            tournament?.let { it1 -> getUserTournament(it1, team) }

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
                                    MessageStatus.value = "Enrolled Successfully"
                                    currentTournamentsList.value?.add(tournament!!)
                                    currentTournamentsList.notifyObserver()
                                    getCurrentTournamentsForUser()
                                    activeTournamentsList.notifyObserver()
                                    getAllActiveTournaments()
//                                        userTournament?.leafCount =
//                                            sharedPrefsRepository.getDailyStepCount() / 1000
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
    }



    /*fun addTeamToList (teamName:String) {

        teamsHolder.value?.add(teamName)
        teamsHolder.notifyObserver()
        //Log.d("Test","Adding"+tourneyList.value.toString())

    }

    fun removeTeamFromList (teamName:String){

        teamsHolder.value?.remove(teamName)
        teamsHolder.notifyObserver()
        //Log.d("Test","Adding"+tourneyList.value.toString())
    }*/

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

    //TODO: to call this method everytime a user visits the tournament fragment
    //TODO: so that the prefs are updated for unity before opening the tree
    private fun updateUserSharedPrefsData(userTournament: UserTournament){
        val user = sharedPrefsRepository.user
        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        user.currentTournaments[userTournament.name] = userTournament
        sharedPrefsRepository.user = user
    }
}