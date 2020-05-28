package dal.mitacsgri.treecare.screens.teams.yourteams

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.extensions.toArrayList
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import kotlin.system.exitProcess

class YourTeamsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository,
    val status: MutableLiveData<Boolean> = MutableLiveData<Boolean>().default(false)
): ViewModel() {

    val teamsLiveData = MutableLiveData<ArrayList<Team>>().default(arrayListOf())
    fun getAllMyTeams(): MutableLiveData<ArrayList<Team>> {

        //Log.d("Test","name "+sharedPrefsRepository.team.name)
        //Log.d("Test","name"+sharedPrefsRepository.user.name)

        val userId = sharedPrefsRepository.user.uid
        firestoreRepository.getAllTeamsForUserAsMember(userId)
            .addOnSuccessListener {
                teamsLiveData.value = it.toObjects<Team>().filter { it.exist }.toArrayList()
                teamsLiveData.notifyObserver()
            }
            .addOnFailureListener {
            }

        return teamsLiveData
    }

    fun deleteTeam(team : Team){

        val teamMembers = team.members
        val tournaments = team.currentTournaments
        var count = 0
        for (member in teamMembers){
            firestoreRepository.updateUserData(member,mapOf("currentTeams" to FieldValue.arrayRemove(team.name)))
                .addOnSuccessListener {
                    for(tournament in tournaments){
                        firestoreRepository.updateTournamentData(tournament, mapOf("teams" to FieldValue.arrayRemove(team.name)))
                        firestoreRepository.deleteTournamentFromUserDB(member, tournament)
                    }
                }
            count++
            if(count.equals(teamMembers.size)){
                firestoreRepository.deleteTeam(team.name)
                    .addOnSuccessListener {
                        firestoreRepository.updateUserData(team.captain,mapOf("captainedTeams" to FieldValue.arrayRemove(team.name)))
                            .addOnSuccessListener {
                                Log.d("Test", "Deletion of Team is successful")
                                teamsLiveData.value?.remove(team)
                                teamsLiveData.notifyObserver()
                            }
                    }
            }
        }





        firestoreRepository.deleteTeam(team.name)
            .addOnSuccessListener {
                teamsLiveData.value?.remove(team)
                teamsLiveData.notifyObserver()
            }

    }

    fun isUserCaptain(captainUid: String) = captainUid == sharedPrefsRepository.user.uid

    fun exitTeam(team : Team){
        firestoreRepository.updateTeamData(team.name, mapOf("members" to FieldValue.arrayRemove(sharedPrefsRepository.user.uid)))
            .addOnSuccessListener {
                firestoreRepository.updateUserData(sharedPrefsRepository.user.uid, mapOf("currentTeams" to FieldValue.arrayRemove(team.name)))
                    .addOnSuccessListener {
                        teamsLiveData.value?.remove(team)
                        teamsLiveData.notifyObserver()
                    }
            }
    }

    /*fun delBtnVis(): MutableLiveData<Boolean> {
        val userId = sharedPrefsRepository.user.uid
        firestoreRepository.getUserData(userId)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                for(t in user?.captainedTeams!!)
                    firestoreRepository.getTeam(t)
                        .addOnSuccessListener {
                            Log.d("Test", "tname" + t)
                            val team = it.toObject<Team>()
                            Log.d("Test", "CapId" + team?.captain)
                            val Cid = team?.captain
                            if (Cid.equals(userId)) {
                                status.value = true
                                status.notifyObserver()
                            }
                        }
                Log.d("Test", "status" + status.value)
            }

        return status
    }*/

}