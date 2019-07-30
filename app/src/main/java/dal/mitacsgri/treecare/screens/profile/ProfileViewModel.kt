package dal.mitacsgri.treecare.screens.profile

import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class ProfileViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository
) : ViewModel() {

    fun getUserPhotoUrl() = sharedPrefsRepository.user.photoUrl

}
