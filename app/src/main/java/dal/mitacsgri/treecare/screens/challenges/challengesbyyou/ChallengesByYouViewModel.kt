package dal.mitacsgri.treecare.screens.challenges.challengesbyyou

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

/**
 * Created by Devansh on 28-06-2019
 */
class ChallengesByYouViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    var challengesList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())
    val statusMessage = MutableLiveData<String>()
    var messageDisplayed = true


}