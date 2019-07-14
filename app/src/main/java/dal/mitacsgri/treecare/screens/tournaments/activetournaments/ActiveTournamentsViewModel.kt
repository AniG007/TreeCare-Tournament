package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.getStringRepresentation
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.extensions.toDateTime
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class ActiveTournamentsViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
): ViewModel() {

    val tournaments = MutableLiveData<List<Tournament>>().default(arrayListOf())

    fun getActiveTournaments() {
        firestoreRepository.getAllActiveTournaments()
            .addOnSuccessListener {
                tournaments.value = it.toObjects()
                tournaments.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("ActiveTournaments", "Failed to obtain data")
            }
    }

    fun getGoalText(tournament: Tournament) = buildSpannedString {
        bold {
            append("Daily Steps Goal: ")
        }
        append(tournament.dailyGoal.toString())
    }

    fun getTournamentDurationText(tournament: Tournament): SpannedString {
        val finishDateString = tournament.finishTimestamp.toDateTime().getStringRepresentation()

        return buildSpannedString {
            bold {
                append("Ends: ")
            }
            append(finishDateString)
        }
    }

    fun getTeamsCountText(tournament: Tournament) = tournament.teams.size.toString()


}