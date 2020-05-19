package dal.mitacsgri.treecare.screens.teams.allteams

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
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

    fun isUserCaptain(captainUid: String) = captainUid == sharedPrefsRepository.user.uid

    fun sendJoinRequest(teamName: String, action: (status: Boolean) -> Unit) {
        val uid = sharedPrefsRepository.user.uid

        firestoreRepository.updateTeamData(teamName,
            mapOf("joinRequests" to FieldValue.arrayUnion(uid)))
            .addOnSuccessListener {
                Log.d("Join request", "sent")
                firestoreRepository.updateUserData(uid,
                    mapOf("teamJoinRequests" to FieldValue.arrayUnion(teamName)))
                    .addOnSuccessListener {
                        action(true)
                    }
                    .addOnFailureListener {
                        action(false)
                        firestoreRepository.updateTeamData(teamName,
                            mapOf("joinRequests" to FieldValue.arrayRemove(uid)))
                    }
            }
            .addOnFailureListener {
                Log.d("Join request", "failed")
                action(false)
            }
    }

    fun cancelJoinRequest(teamName: String, action: (status: Boolean) -> Unit) {
        val uid = sharedPrefsRepository.user.uid

        firestoreRepository.updateTeamData(teamName,
            mapOf("joinRequests" to FieldValue.arrayRemove(uid)))
            .addOnSuccessListener {
                Log.d("Join request", "cancelled")
                firestoreRepository.updateUserData(uid,
                    mapOf("teamJoinRequests" to FieldValue.arrayRemove(teamName)))
                    .addOnSuccessListener {
                        action(true)
                    }
                    .addOnFailureListener {
                        action(false)
                        firestoreRepository.updateTeamData(teamName,
                            mapOf("joinRequests" to FieldValue.arrayUnion(uid)))
                    }
            }
            .addOnFailureListener {
                Log.d("Join request cancel", "failed")
                action(false)
            }
    }

    fun isJoinRequestSent(team: Team) = team.joinRequests.contains(sharedPrefsRepository.user.uid)
}