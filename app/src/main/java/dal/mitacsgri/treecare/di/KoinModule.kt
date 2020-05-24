package dal.mitacsgri.treecare.di

import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import dal.mitacsgri.treecare.screens.MainViewModel
import dal.mitacsgri.treecare.screens.StepCountDataProvidingViewModel
import dal.mitacsgri.treecare.screens.challenges.ChallengesViewModel
import dal.mitacsgri.treecare.screens.createchallenge.CreateChallengeViewModel
import dal.mitacsgri.treecare.screens.createteam.CreateTeamViewModel
import dal.mitacsgri.treecare.screens.createtournament.CreateTournamentViewModel
import dal.mitacsgri.treecare.screens.dialog.challengecomplete.ChallengeCompleteDialogViewModel
import dal.mitacsgri.treecare.screens.enrollteams.EnrollTeamsViewModel
import dal.mitacsgri.treecare.screens.gamesettings.SettingsViewModel
import dal.mitacsgri.treecare.screens.instructions.InstructionsViewModel
import dal.mitacsgri.treecare.screens.invitesrequests.InvitesRequestViewModel
import dal.mitacsgri.treecare.screens.jointeam.JoinTeamViewModel
import dal.mitacsgri.treecare.screens.leaderboard.LeaderboardItemViewModel
import dal.mitacsgri.treecare.screens.profile.ProfileViewModel
import dal.mitacsgri.treecare.screens.progressreport.progressreportdata.ProgressReportDataViewModel
import dal.mitacsgri.treecare.screens.teaminfo.TeamInfoViewModel
import dal.mitacsgri.treecare.screens.teams.TeamsViewModel
import dal.mitacsgri.treecare.screens.teams.allteams.AllTeamsViewModel
import dal.mitacsgri.treecare.screens.teams.yourteams.YourTeamsViewModel
import dal.mitacsgri.treecare.screens.tournaments.TournamentsViewModel
import dal.mitacsgri.treecare.screens.tournaments.activetournaments.ActiveTournamentsViewModel
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
    viewModel { StepCountDataProvidingViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { CreateChallengeViewModel(get(), get()) }
    viewModel { CreateTournamentViewModel(get(), get()) }
    viewModel { LeaderboardItemViewModel(get(), get()) }
    viewModel { ChallengeCompleteDialogViewModel() }
    viewModel { ActiveTournamentsViewModel(get(), get()) }
    viewModel { YourTeamsViewModel(get(), get()) }
    viewModel { AllTeamsViewModel(get(), get()) }
    viewModel { CreateTeamViewModel(get(), get()) }
    viewModel { TeamsViewModel(get(), get()) }
    viewModel { TournamentsViewModel(get(), get()) }
    viewModel { ChallengesViewModel(get(), get())}
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { ProgressReportDataViewModel(get(), get()) }
    viewModel { InstructionsViewModel() }
    viewModel {InvitesRequestViewModel(get(),get())}
    viewModel { JoinTeamViewModel(get()) }
    viewModel { TeamInfoViewModel(get(),get(),get())}
    viewModel { EnrollTeamsViewModel(get(),get())}
}