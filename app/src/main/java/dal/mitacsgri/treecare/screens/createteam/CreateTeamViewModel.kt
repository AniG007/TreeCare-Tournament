package dal.mitacsgri.treecare.screens.createteam

import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

/**
 * Created by Devansh on 16-07-2019
 */

class CreateTeamViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
) : ViewModel() {

    var isNameValid = false

    val messageLiveData = MutableLiveData<String>()
    val isFullDataValid = MutableLiveData<Boolean>().default(false)

    fun checkAllInputFieldsValid(): Boolean {
        isFullDataValid.value = isNameValid
        return isFullDataValid.value ?: false
    }

    fun createTeam(name: Editable?, description: Editable?, action: () -> Unit) {
        if (checkAllInputFieldsValid()) {
            firestoreRepository.getTeam(name.toString()).addOnSuccessListener {
                if (it.exists()) {
                    messageLiveData.value = "Team name already in use"
                    return@addOnSuccessListener
                } else {
                    firestoreRepository.storeTeam(
                        Team(name = name.toString(),
                            description = description.toString(),
                            captain = sharedPrefsRepository.user.uid,
                            captainName = sharedPrefsRepository.user.name
                            )) {
                        messageLiveData.value = if (it) "Team created successfully"
                                                else "Team creation failed"
                        action()
                    }
                }
            }
        }
    }
}