package dal.mitacsgri.treecare.screens.teams.allteams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.getCardItemDescriptorText
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class AllTeamsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
): ViewModel() {

    fun getAllTeams(): LiveData<List<Team>> {
        val teamsLiveData = MutableLiveData<List<Team>>()

        firestoreRepository.getAllTeams()
            .addOnSuccessListener {
                teamsLiveData.value = it.toObjects()
                teamsLiveData.notifyObserver()
            }
            .addOnFailureListener {
            }

        return teamsLiveData
    }

    fun getMembersCountText(team: Team) =
        getCardItemDescriptorText("Members", team.members.size.toString())

    fun getCaptainNameText(team: Team) =
        getCardItemDescriptorText("Captain", team.captainName)

}