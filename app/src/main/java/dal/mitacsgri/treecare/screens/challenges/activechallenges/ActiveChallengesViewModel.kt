package dal.mitacsgri.treecare.screens.challenges.activechallenges

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.UserChallenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

/**
 * Created by Devansh on 25-06-2019
 */
class ActiveChallengesViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel()  {

    var challengesList = MutableLiveData<List<Challenge>>().default(listOf())
    val statusMessage = MutableLiveData<String>()
    var messageDisplayed = false

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
        val finishDateString = challenge.finishTimestamp.toDateTime().getStringRepresentation()

        return buildSpannedString {
            bold {
                append("Ends: ")
            }
            append(finishDateString)
        }
    }

    fun getParticipantsCountString(challenge: Challenge) = challenge.players.size.toString()

    fun joinChallenge(challenge: Challenge) {
        val challengeJson = UserChallenge(
            name = challenge.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis
        ).toJson<UserChallenge>()

        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
            mapOf("currentChallenges" to FieldValue.arrayUnion(challengeJson)))
            .addOnSuccessListener {
                updateUserSharedPrefsData(challengeJson)
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

    private fun updateUserSharedPrefsData(challengeJson: String) {
        val user = sharedPrefsRepository.user
        user.currentChallenges.add(challengeJson)
        sharedPrefsRepository.user = user
    }

}