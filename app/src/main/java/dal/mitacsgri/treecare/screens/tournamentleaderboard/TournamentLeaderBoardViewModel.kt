package dal.mitacsgri.treecare.screens.tournamentleaderboard

import android.text.SpannedString
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.extensions.xnor
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.TeamTournament
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime
import java.lang.reflect.Array.set

class TournamentLeaderBoardViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
):ViewModel() {

    private lateinit var tournament: Tournament
    private var teamTournament: TeamTournament? = null

    private val mTeamList = MutableLiveData<ArrayList<TeamTournament>>()

    var isDialogDisplayed: Boolean

    get() = tournament.active.xnor(teamTournament?.isActive ?: tournament.active)
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

    fun isCurrentTeam(teamTournament: TeamTournament) = teamTournament.name == sharedPrefsRepository.team.name

    fun getTeamList(tournamentName: String): LiveData<ArrayList<TeamTournament>> {

        val teamList = MutableLiveData<ArrayList<TeamTournament>>().default(arrayListOf())

        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                tournament = it.toObject<Tournament>()!!

                teamTournament = sharedPrefsRepository.team.currentTournaments[tournamentName]

                val teams = tournament.teams
                val teamsCount = teams.size
                val limit = teamsCount

                for (i in 0 until limit){
                    firestoreRepository.getTeam(teams[i])
                        .addOnSuccessListener {
                            val teamFromDB = it.toObject<Team>()
                            val teamTourney = teamFromDB?.currentTournaments!![tournamentName]!!
                            teamList.value?.add(teamTourney)

                            if(teamList.value?.size == limit){
                                teamList.value!!.sortByDescending {
                                    it.totalSteps
                                }
                                teamList.notifyObserver()
                            }
                        }
                }

            }
        mTeamList.value = teamList.value
        return teamList
    }

    fun getTournamentName(): String = tournament.name

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
}
