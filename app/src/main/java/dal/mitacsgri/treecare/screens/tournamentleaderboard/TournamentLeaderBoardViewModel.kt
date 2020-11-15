package dal.mitacsgri.treecare.screens.tournamentleaderboard

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.extensions.xnor
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class TournamentLeaderBoardViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
):ViewModel() {

    private lateinit var tournament: Tournament
    private var teamTournament: TeamTournament? = null

    private val mTeamList = MutableLiveData<ArrayList<TeamTournament>>()

    var isDialogDisplayed: Boolean

    get() =  (tournament.active.xnor(teamTournament?.isActive ?: tournament.active))
    set(value){
     teamTournament?.let {
         it.isActive = value
         val team = sharedPrefsRepository.team
         team.currentTournaments[it.name] = it
         sharedPrefsRepository.team = team

         val user = sharedPrefsRepository.user
         user.currentTournaments[it.name] = getUserTournament(it, value)
         sharedPrefsRepository.user = user

         firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
         mapOf("currentTournaments" to user.currentTournaments))

         firestoreRepository.updateTeamData(sharedPrefsRepository.team.name,
         mapOf("currentTournaments" to team.currentTournaments))
     }
    }

    fun isCurrentTeam(teamTournament: TeamTournament) = teamTournament.teamName == sharedPrefsRepository.team.name


    fun getTeamList(tournamentName: String): LiveData<ArrayList<TeamTournament>> {

        val teamList = MutableLiveData<ArrayList<TeamTournament>>().default(arrayListOf())

        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                tournament = it.toObject<Tournament>()!!

                sharedPrefsRepository.tournamentName = tournamentName
                teamTournament = sharedPrefsRepository.team.currentTournaments[tournamentName]

                val teams = tournament.teams
                val teamsCount = teams.size
                val limit = teamsCount

                for (i in 0 until limit) {
                    firestoreRepository.getTeam(teams[i])
                        .addOnSuccessListener {
                            val teamFromDB = it.toObject<Team>()
                                val teamTourney = teamFromDB?.currentTournaments!![tournamentName]!!
                            teamList.value?.add(teamTourney)

                            if (teamList.value?.size == limit) {
//                                teamList.value!!.sortByDescending {
//                                    //it.totalSteps
//                                    it.dailyGoalsAchieved
//                                }
                                //Sorting according to daily goals first, then if any of the goals are equal, then the team's totalSteps are taken into account
                                teamList.value!!.sortWith(compareBy({it.leafCount}, {it.totalSteps}))
                                teamList.value!!.reverse()

                                sortTeamForTournament(tournamentName, teamList.value!!)
                                teamList.notifyObserver()
                            }
                        }
                }

            }
        mTeamList.value = teamList.value
        return teamList
    }

    fun isCurrentUser(challenger: Challenger) = challenger.uid == sharedPrefsRepository.user.uid

    fun getTournamentNameForLeaderBoard(tournamentName: String) = buildSpannedString {
        run {
        append(tournamentName)
    }
    append("\nLeaderboard")
}

    fun getTournamentName(): String = sharedPrefsRepository.tournamentName!!

    fun getTotalStepsText(teamTournament: TeamTournament): SpannedString =
        buildSpannedString {
            bold{
               append("Total steps: ")
            }
            append(teamTournament.totalSteps.toString())
        }

    fun getTeamPosition(): Int{
        val userTeam = sharedPrefsRepository.team.name
        val teams = mTeamList.value?: arrayListOf()
        for(i in 0 until teams.size){
            if(teams[i].teamName == userTeam)
                return i+1
        }
        return 0
    }

    fun getLeafCountText(teamTournament: TeamTournament): String = teamTournament.leafCount.toString()

    private fun getUserTournament(tournament: TeamTournament, value: Boolean) =
        UserTournament(
            name = tournament.name,
            dailyStepsMap = tournament.dailyStepsMap,
            totalSteps = tournament.totalSteps,
            joinDate = tournament.joinDate,
            isActive = value,
            goal = tournament.goal,
            endDate = tournament.endDate,
            teamName = tournament.teamName
        )

    fun sortTeamForTournament(tournamentName: String, teamList: ArrayList<TeamTournament>){
        val teams = ArrayList<String>()
        for (team in teamList){
            teams.add(team.teamName)
        }
        //Log.d("Test", "Sorted Teams "+ teams)
        firestoreRepository.updateTournamentData(tournamentName, mapOf("teams" to teams))
            .addOnSuccessListener {
                Log.d("Test", "Teams have been updated in the DB Successfully")
            }
            .addOnFailureListener{
                Log.d("Test", "Failed to sort and upload teams in DB: "+ it)
            }
    }
}
