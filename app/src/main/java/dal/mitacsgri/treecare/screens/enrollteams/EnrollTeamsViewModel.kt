package dal.mitacsgri.treecare.screens.enrollteams

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

class EnrollTeamsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
)

    :ViewModel() {

    val teamsList = MutableLiveData<ArrayList<Team>>().default(arrayListOf())
    val teamsHolder = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    val existingTeams = MutableLiveData<ArrayList<String>>().default(arrayListOf())

    val MessageStatus = MutableLiveData<String>()

    fun getTeams(): MutableLiveData<ArrayList<Team>> {

        firestoreRepository.getAllCaptainedTeams(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                val docs = it.toObjects<Team>().filter {it.exist}
                //getting the teams owned by the player
                /*for (i in docs){
                    Log.d("Test","In Loop "+i.data?.get("name"))
                    teamsList.value?.add(i.data?.get("name").toString())
                }*/
                //Log.d("Test","QSnap " + docs.toString())
                //val team = it.toObjects<Team>()
                for (team in docs) {
                   // Log.d("Test", "QSnap " + team.toString())

                    //Doing this inorder to remove the team which have already enrolled in the tournament
                    if(existingTeams.value?.contains(team.name)!!){
                        //Log.d("Test","Team exists")
                        //Log.d("Test",existingTeams.value.toString()+" "+team.name)
                        teamsList.value?.remove(team)
                    }
                    else {
                        //Log.d("Test","Team does not exists")
                        //Log.d("Test",existingTeams.value.toString()+" "+team.name)
                        teamsList.value?.add(team)
                    }
                }
                teamsList.notifyObserver()
            }
        return teamsList
    }

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

    fun enrollTeams(tournamentName: String){
        //TO check if a team has already been enrolled in the tournament
        for (team in teamsHolder.value!!) {
            Log.d("Test", "insideEnrollTeams" + team)
            if (existingTeams.value?.contains(team)!!) {
                Log.d("Test", existingTeams.value.toString())
                MessageStatus.value = "Team has already been enrolled"
            } else {
                firestoreRepository.updateTournamentData(tournamentName, mapOf("teams" to FieldValue.arrayUnion(team))
                )

                    .addOnSuccessListener {
                        firestoreRepository.updateTeamData(
                            team,
                            mapOf("currentTournaments" to FieldValue.arrayUnion(tournamentName))
                        )
                            .addOnSuccessListener {
                                MessageStatus.value = "Enrolled Successfully"
                            }
                    }
                    .addOnFailureListener {
                        MessageStatus.value = "Unable to Enroll. Please Try Again later"
                    }
            }


            //Adding tournament to currentTournaments in Users Collection
            firestoreRepository.getTournament(tournamentName)
                .addOnSuccessListener {
                    val tournament = it.toObject<Tournament>()
                    firestoreRepository.getTeam(team)
                        .addOnSuccessListener {
                            val teamData= it.toObject<Team>()
                            val members = teamData?.members
                            val userTournament = tournament?.let { it1 -> getUserTournament(it1,team) }

                            Log.d("Test", "tourneyName2 ${tournament?.name}")

                            for(uid in members!!) {
                                //val uid = sharedPrefsRepository.user.uid
                                //Log.d("Test","UID ${uid}")
                                userTournament?.let { it1 -> updateUserSharedPrefsData(it1) }
                                Log.d("Test", "tourneyName2 ${tournament?.name}")
                                mapOf("currentTournaments.${tournament?.name}" to userTournament)?.let { it1 ->
                                    firestoreRepository.updateUserTournamentData(uid, it1)
                                }
                                    .addOnSuccessListener {
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
    }

    fun addTeamToList (teamName:String) {

        teamsHolder.value?.add(teamName)
        teamsHolder.notifyObserver()
        //Log.d("Test","Adding"+tourneyList.value.toString())

    }

    fun removeTeamFromList (teamName:String){

        teamsHolder.value?.remove(teamName)
        teamsHolder.notifyObserver()
        //Log.d("Test","Adding"+tourneyList.value.toString())
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
