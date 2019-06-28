package dal.mitacsgri.treecare.screens.challenges.activechallenges

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.getStringRepresentation
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.extensions.toDateTime
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

/**
 * Created by Devansh on 25-06-2019
 */
class ActiveChallengesViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel()  {

    var challengesList = MutableLiveData<List<Challenge>>().default(listOf())

    fun getAllActiveChallenges() {
        firestoreRepository.getAllActiveChallenges()
            .addOnSuccessListener {
                challengesList.value = it.toObjects<Challenge>().filter {it.exist}
                challengesList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active challenges", "Fetch failed: $it")
            }
    }

    fun getChallengeDurationText(challenge: Challenge): SpannedString {
        //val createdDateString = challenge.creationTimestamp.toDateTime().getStringRepresentation()
        val finishDateString =challenge.finishTimestamp.toDateTime().getStringRepresentation()

        return buildSpannedString {
            bold {
                append("Ends: ")
            }
            append(finishDateString)
        }
    }

    fun getParticipantsCountString(challenge: Challenge) = challenge.players.size.toString()

}