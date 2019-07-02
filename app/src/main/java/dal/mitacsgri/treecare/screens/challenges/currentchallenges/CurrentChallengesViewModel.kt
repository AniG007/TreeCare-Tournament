package dal.mitacsgri.treecare.screens.challenges.currentchallenges

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import com.google.gson.Gson
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.UserChallenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

/**
 * Created by Devansh on 25-06-2019
 */

class CurrentChallengesViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    var challengesList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())
    val statusMessage = MutableLiveData<String>()

    fun getCurrentChallengesForUser() {
        val challengeReferences = sharedPrefsRepository.user.currentChallenges

        challengeReferences.forEach { (_, userChallengeString) ->
            val challengeJson = Gson().fromJson(userChallengeString, UserChallenge::class.java)
            //Getting challenges from the Challenges DB after getting reference
            // from the challenges list obtained from the user
            firestoreRepository.getChallenge(challengeJson.name)
                .addOnSuccessListener {
                val challenge = it.toObject<Challenge>() ?: Challenge(exist = false)
                synchronized(challengesList.value!!) {
                    if (challenge.exist) {
                        challengesList.value?.add(challenge)
                        challengesList.notifyObserver()
                    }
                }
            }
            .addOnFailureListener {
                    Log.d("Challenge not found", it.toString())
            }
        }
    }

    fun leaveChallenge(challenge: Challenge) {
        val userId = sharedPrefsRepository.user.uid
        var counter = 0

        firestoreRepository.deleteUserFromChallengeDB(challenge, userId)
            .addOnSuccessListener {
                synchronized(counter) {
                    counter++
                    if (counter == 2) {
                        updateUserChallengesList(challenge)
                    }
                }
                Log.d("Challenge deleted", "from DB")
            }
            .addOnFailureListener {
                Log.e("Challenge delete failed", it.toString())
            }

        val userChallenge = getUserChallenge(challenge).let {
            it.isCurrentChallenge = false
            it
        }

        //TODO: Maybe later on we can think of only disabling the challenge instead of actually deleting from the database
        firestoreRepository.deleteChallengeFromUserDB(userId, userChallenge, userChallenge.toJson<UserChallenge>())
            .addOnSuccessListener {
                synchronized(counter) {
                    counter++
                    if (counter == 2) {
                        updateUserChallengesList(challenge)
                    }
                }
                statusMessage.value = "Success"
            }
            .addOnFailureListener {
                statusMessage.value = "Failed"
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

    fun getPlayersCountText(challenge: Challenge) =
            buildSpannedString {
                append(challenge.players.size.toString())
            }

    private fun updateUserChallengesList(challenge: Challenge) {
        challengesList.value?.remove(challenge)
        challengesList.notifyObserver()

        sharedPrefsRepository.user = sharedPrefsRepository.user.let {
            it.currentChallenges.remove(challenge.name)
            it
        }
    }

    private fun getUserChallenge(challenge: Challenge) =
        UserChallenge(
            name = challenge.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis
        )
}