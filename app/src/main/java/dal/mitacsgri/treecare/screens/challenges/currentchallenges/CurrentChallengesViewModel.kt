package dal.mitacsgri.treecare.screens.challenges.currentchallenges

import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
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

class CurrentChallengesViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    var challengesList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())

    fun getCurrentChallengesForUser() {
        val challengeReferences = sharedPrefsRepository.user.currentChallenges

        for (i in 0 until challengeReferences.size) {
            firestoreRepository.getChallenge(challengeReferences[i]).addOnSuccessListener {
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
}