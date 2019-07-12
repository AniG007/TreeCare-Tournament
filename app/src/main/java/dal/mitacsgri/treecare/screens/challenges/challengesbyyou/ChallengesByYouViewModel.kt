package dal.mitacsgri.treecare.screens.challenges.challengesbyyou

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserChallengeDataWorker
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.UserChallenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

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

    fun getChallengeTypeText(challenge: Challenge) =
        buildSpannedString {
            bold {
                append("Type: ")
            }
            append(if (challenge.type == 0) "Daily Goal Based" else "Aggregate based")
        }

    fun getGoalText(challenge: Challenge) =
        buildSpannedString {
            bold {
                append(if(challenge.type == 0) "Daily Steps Goal: " else "Total steps goal: ")
            }
            append(challenge.goal.toString())
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

        //Update data as soon as user joins a challenge
        val updateUserChallengeDataRequest =
            OneTimeWorkRequestBuilder<UpdateUserChallengeDataWorker>().build()
        WorkManager.getInstance().enqueue(updateUserChallengeDataRequest).result.addListener(
            Runnable {
                Log.d("Challenge data", "updated by work manager")
            }, MoreExecutors.directExecutor()
        )
    }

    private fun updateUserSharedPrefsData(userChallenge: UserChallenge, userChallengeJson: String) {
        val user = sharedPrefsRepository.user
        user.currentChallenges[userChallenge.name] = userChallenge
        sharedPrefsRepository.user = user
    }

    private fun getUserChallenge(challenge: Challenge) =
        UserChallenge(
            name = challenge.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis,
            type = challenge.type,
            goal = challenge.goal
        )
}