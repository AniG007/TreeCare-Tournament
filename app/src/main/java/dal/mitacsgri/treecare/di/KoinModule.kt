package dal.mitacsgri.treecare.di

import dal.mitacsgri.treecare.provider.SharedPreferencesRepository
import dal.mitacsgri.treecare.provider.StepCountRepository
import dal.mitacsgri.treecare.screens.MainViewModel
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

val appModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { SplashScreenViewModel(get(), get()) }
}