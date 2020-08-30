package dal.mitacsgri.treecare.screens.tournamentleaderboard2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.TournamentLB2
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository

class TournamentLeaderBoard2ViewModel(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    val teamsLiveData = MutableLiveData<ArrayList<TournamentLB2>>().default(arrayListOf())

    fun getTournamentTeams(tournamentName: String): MutableLiveData<ArrayList<TournamentLB2>> {

        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                val tournament = it.toObject<Tournament>()
                val teams = tournament?.teams
                for (team in teams!!) {
                    firestoreRepository.getTeam(team)
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            val captainId = teamDB?.captain
                            firestoreRepository.getUserData(captainId!!)
                                .addOnSuccessListener {
                                    val user = it.toObject<User>()
                                    val photoUrl = user?.photoUrl
                                    val capId = user?.uid
                                    val capName = user?.name
                                    val tName = user?.captainedTeams!![0]
                                    teamsLiveData.value?.add(TournamentLB2(capId!!,capName!!, photoUrl!!,tName))
                                    teamsLiveData.notifyObserver()
                                }
                        }
                }
            }
        return teamsLiveData
    }
}