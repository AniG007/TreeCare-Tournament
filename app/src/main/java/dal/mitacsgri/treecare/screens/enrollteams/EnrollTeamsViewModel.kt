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
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

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
                val docs = it.toObjects<Team>()
                //getting the teams owned by the player
                /*for (i in docs){
                    Log.d("Test","In Loop "+i.data?.get("name"))
                    teamsList.value?.add(i.data?.get("name").toString())
                }*/
                //Log.d("Test","QSnap " + docs.toString())
                //val team = it.toObjects<Team>()
                for (team in docs) {
                   // Log.d("Test", "QSnap " + team.toString())
                    teamsList.value?.add(team)
                }
                teamsList.notifyObserver()
            }
        return teamsList
    }

    fun getExistingTeams (tournamentName: String){
        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                val ct = it.toObject<Tournament>()
                val teams = ct?.teams
                for(team in teams!!){
                    existingTeams.value?.add(team)
                }
                Log.d("Test","existingTeams"+existingTeams.value)
                existingTeams.notifyObserver()
            }
    }

    fun enrollTeams(tournamentName: String){
        for (team in teamsHolder.value!!) {
            Log.d("Test", "insideEnrollTeams"+ team)
            if(existingTeams.value?.contains(team)!!){
                Log.d("Test",existingTeams.value.toString())
                MessageStatus.value = "Team has already been enrolled"
            }
            else {
                firestoreRepository.updateTournamentData(tournamentName, mapOf("teams" to FieldValue.arrayUnion(team)))

                    .addOnSuccessListener {
                        MessageStatus.value = "Enrolled Successfully"
                    }
                    .addOnFailureListener {
                        MessageStatus.value = "Unable to Enroll. Please Try Again later"
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
}
