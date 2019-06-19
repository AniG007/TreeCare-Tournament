package dal.mitacsgri.treecare.di

import dal.mitacsgri.treecare.provider.SharedPreferencesRepository
import dal.mitacsgri.treecare.provider.StepCountRepository
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.screens.login.LoginViewModel
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
    viewModel { LoginViewModel(get(), get()) }
    viewModel { MainViewModel(get()) }
}