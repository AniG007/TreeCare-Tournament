package dal.mitacsgri.treecare.screens.challenges

import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_AGGREGATE_BASED
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class ChallengesViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository
    ): ViewModel() {

    fun calculateLeavesForChallenge(challenge: Challenge) {
        when(challenge.type) {
            CHALLENGE_TYPE_AGGREGATE_BASED -> {

            }
        }
    }
}