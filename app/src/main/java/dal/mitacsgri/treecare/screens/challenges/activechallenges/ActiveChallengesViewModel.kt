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
        val userChallenge = getUserChallenge(challenge)
        val userChallengeJson = userChallenge.toJson<UserChallenge>()

        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
            mapOf("currentChallenges.${challenge.name}" to userChallengeJson))
            .addOnSuccessListener {
                updateUserSharedPrefsData(userChallenge, userChallengeJson)
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

    private fun updateUserSharedPrefsData(userChallenge: UserChallenge, userChallengeJson: String) {
        val user = sharedPrefsRepository.user
        user.currentChallenges[userChallenge.name] = userChallengeJson
        sharedPrefsRepository.user = user
    }

    private fun getUserChallenge(challenge: Challenge) =
        UserChallenge(
            name = challenge.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis,
            type = challenge.type
        )
}