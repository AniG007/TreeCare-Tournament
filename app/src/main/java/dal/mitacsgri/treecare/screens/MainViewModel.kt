package dal.mitacsgri.treecare.screens

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.provider.SharedPreferencesRepository

/**
 * Created by Devansh on 19-06-2019
 */

class MainViewModel(private val sharedPrefsRepository: SharedPreferencesRepository) : ViewModel() {

    val haveInstructionsDisplayed = MutableLiveData<Boolean>().default(false)

    fun setInstructionsDisplayedStatus(value: Boolean) {
        sharedPrefsRepository.hasInstructionsDisplayed = value
        haveInstructionsDisplayed.value = value
    }
}