package dal.mitacsgri.treecare.screens.tournaments

import androidx.lifecycle.ViewModel
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class TournamentsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository
): ViewModel()