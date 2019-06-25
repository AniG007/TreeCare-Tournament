package dal.mitacsgri.treecare.screens.currentchallenges

import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

/**
 * Created by Devansh on 25-06-2019
 */

class CurrentChallengesViewModel(
    sharedPrefsRepository: SharedPreferencesRepository,
    firestoreRepository: FirestoreRepository
    ): ViewModel()