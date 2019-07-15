package dal.mitacsgri.treecare.screens.teams.yourcaptainedteams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class YourCaptainedTeamsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
): ViewModel() {

    fun getCaptainedTeams(): LiveData<List<Team>> {
        val teamsLiveData = MutableLiveData<List<Team>>()

        firestoreRepository.getAllCaptainedTeams(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                teamsLiveData.value = it.toObjects()
                teamsLiveData.notifyObserver()
            }
            .addOnFailureListener {

            }

        return teamsLiveData
    }

}