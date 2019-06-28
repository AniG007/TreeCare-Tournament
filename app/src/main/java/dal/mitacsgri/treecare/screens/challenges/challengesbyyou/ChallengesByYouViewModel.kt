package dal.mitacsgri.treecare.screens.challenges.challengesbyyou

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

/**
 * Created by Devansh on 28-06-2019
 */
class ChallengesByYouViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    var challengesList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())
    val statusMessage = MutableLiveData<String>()
    var messageDisplayed = true

    fun getAllCreatedChallengesChallenges(userId: String) {
        firestoreRepository.getAllChallengesCreatedByUser(userId)
            .addOnSuccessListener {
                challengesList.value = it.toObjects<Challenge>().filter { it.exist }.toArrayList()
                challengesList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active challenges", "Fetch failed: $it")
            }
    }

    fun getChallengeDurationText(challenge: Challenge): SpannedString {
        val finishDateString =challenge.finishTimestamp.toDateTime().getStringRepresentation()

        return buildSpannedString {
            bold {
                append("Ends: ")
            }
            append(finishDateString)
        }
    }

    fun getParticipantsCountString(challenge: Challenge) = challenge.players.size.toString()

    fun getCurrentUserId() = sharedPrefsRepository.user.uid

    fun deleteChallenge(challenge: Challenge) {
        firestoreRepository.setChallengeAsNonExist(challenge.name)
            .addOnSuccessListener {
                val challengeToRemoveIndex = challengesList.value?.indexOf(challenge)
                if (challengeToRemoveIndex != -1) {
                    challengesList.value?.removeAt(challengeToRemoveIndex!!)
                    challengesList.notifyObserver()
                }
            }
            .addOnFailureListener {
                Log.e("Deletion failed", it.toString())
            }
    }

    fun joinChallenge(challenge: Challenge) {
        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
            mapOf("currentChallenges" to FieldValue.arrayUnion(challenge.name)))
            .addOnSuccessListener {
                sharedPrefsRepository.user.currentChallenges.add(challenge.name)
                messageDisplayed = false
                statusMessage.value = "You are now a part of ${challenge.name}"
            }
            .addOnFailureListener {
                messageDisplayed = false
                statusMessage.value = "Error joining challenge"
                Log.e("Error joining challenge", it.toString())
            }

        firestoreRepository.updateChallengeData(challenge.name,
            mapOf("players" to FieldValue.arrayUnion(sharedPrefsRepository.user.uid)))
    }

}