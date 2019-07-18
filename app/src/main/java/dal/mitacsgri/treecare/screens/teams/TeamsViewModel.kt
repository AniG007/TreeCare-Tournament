package dal.mitacsgri.treecare.screens.teams

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class TeamsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
): ViewModel() {

    fun hasTeamRequests(): MutableLiveData<Boolean> {
        val status = MutableLiveData<Boolean>().default(false)

        firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                user?.let {
                    sharedPrefsRepository.user = user
                    status.value = user.teamJoinRequests.isNotEmpty()
                }
            }

        return status
    }

}