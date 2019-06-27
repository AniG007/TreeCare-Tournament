package dal.mitacsgri.treecare.di

import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.screens.challenges.activechallenges.ActiveChallengesViewModel
import dal.mitacsgri.treecare.screens.challenges.currentchallenges.CurrentChallengesViewModel
import dal.mitacsgri.treecare.screens.createchallenge.CreateChallengeViewModel
import dal.mitacsgri.treecare.screens.gamesettings.SettingsViewModel
import dal.mitacsgri.treecare.screens.splash.SplashScreenViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Created by Devansh on 19-06-2019
 */

val sharedPreferencesRepositoryModule = module {
    single { SharedPreferencesRepository(get()) }
}

val stepCountRepositoryModule = module {
    single { StepCountRepository(get()) }
}

val firestoreRepositoryModule = module {
    single { FirestoreRepository() }
}

val appModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { SplashScreenViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { CurrentChallengesViewModel(get(), get()) }
    viewModel { ActiveChallengesViewModel(get(), get()) }
    viewModel { CreateChallengeViewModel(get(), get()) }
}