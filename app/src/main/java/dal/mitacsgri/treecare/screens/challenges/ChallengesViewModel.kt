package dal.mitacsgri.treecare.screens.challenges

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
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserChallengeDataWorker
import dal.mitacsgri.treecare.consts.CHALLENGER_MODE
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.UserChallenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

class ChallengesViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
    ): ViewModel() {

    companion object Types {
        const val ACTIVE_CHALLENGES = 0
        const val CHALLENGES_BY_YOU = 1
    }

    val activeChallengesList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())
    val currentChallengesList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())
    val challengesByYouList = MutableLiveData<ArrayList<Challenge>>().default(arrayListOf())

    //The error status message must contain 'error' in string because it is used to check whether to
    //disable or enable join button
    val statusMessage = MutableLiveData<String>()
    var messageDisplayed = true

    fun getAllActiveChallenges() {
        firestoreRepository.getAllActiveChallenges()
            .addOnSuccessListener {
                activeChallengesList.value = it.toObjects<Challenge>().filter { it.exist && it.active }.toArrayList()
                activeChallengesList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active challenges", "Fetch failed: $it")
            }
    }

    fun getCurrentChallengesForUser() {
        val challengeReferences = sharedPrefsRepository.user.currentChallenges

        challengeReferences.forEach { (_, userChallenge) ->
            //Getting challenges from the Challenges DB after getting reference
            // from the challenges list obtained from the user
            firestoreRepository.getChallenge(userChallenge.name)
                .addOnSuccessListener {
                    val challenge = it.toObject<Challenge>() ?: Challenge(exist = false)
                    synchronized(currentChallengesList.value!!) {
                        if (challenge.exist) {
                            currentChallengesList.value?.sortAndAddToList(challenge)
                            currentChallengesList.notifyObserver()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.d("Challenge not found", it.toString())
                }
        }
    }

    fun getAllCreatedChallengesChallenges(userId: String) {
        firestoreRepository.getAllChallengesCreatedByUser(userId)
            .addOnSuccessListener {
                challengesByYouList.value = it.toObjects<Challenge>().filter { it.exist }.toArrayList()
                challengesByYouList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active challenges", "Fetch failed: $it")
            }
    }

    fun joinChallenge(challenge: Challenge, successAction: () -> Unit) {
        val userChallenge = getUserChallenge(challenge)
        val uid = sharedPrefsRepository.user.uid

        updateUserSharedPrefsData(userChallenge)

        firestoreRepository.updateUserData(uid,
            mapOf("currentChallenges.${challenge.name}" to userChallenge))
            .addOnSuccessListener {
                //updateUserSharedPrefsData(userChallenge)
                messageDisplayed = false
                statusMessage.value = "You are now a part of ${challenge.name}"
                var index = activeChallengesList.value?.indexOf(challenge)!!
                activeChallengesList.value?.get(index)?.players?.add(uid)
                activeChallengesList.notifyObserver()

                index = challengesByYouList.value?.indexOf(challenge)!!
                if (index != -1) challengesByYouList.value?.get(index)?.players?.add(uid)
                challengesByYouList.notifyObserver()

                //Do this to display the leaf count as soon as the user joins the challenge
                if (challenge.type == CHALLENGE_TYPE_DAILY_GOAL_BASED) {
                    userChallenge.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
                }
                val user = sharedPrefsRepository.user
                user.currentChallenges[challenge.name] = userChallenge
                sharedPrefsRepository.user = user

                successAction()
            }
            .addOnFailureListener {
                messageDisplayed = false
                statusMessage.value = "Error joining challenge"
                Log.e("Error joining challenge", it.toString())
            }

        firestoreRepository.updateChallengeData(challenge.name,
            mapOf("players" to FieldValue.arrayUnion(sharedPrefsRepository.user.uid)))

        currentChallengesList.value?.add(challenge)
        currentChallengesList.notifyObserver()

        //Update data as soon as user joins a challenge
        val updateUserChallengeDataRequest =
            OneTimeWorkRequestBuilder<UpdateUserChallengeDataWorker>().build()
        WorkManager.getInstance().enqueue(updateUserChallengeDataRequest).result.addListener(
            Runnable {
                Log.d("Challenge data", "updated by work manager")
            }, MoreExecutors.directExecutor())
    }

    fun leaveChallenge(challenge: Challenge) {
        val userId = sharedPrefsRepository.user.uid
        var counter = 0

        firestoreRepository.deleteUserFromChallengeDB(challenge, userId)
            .addOnSuccessListener {
                synchronized(counter) {
                    counter++
                    if (counter == 2) {
                        removeChallengeFromCurrentChallengesLists(challenge)
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
                        removeChallengeFromCurrentChallengesLists(challenge)
                    }
                }
                statusMessage.value = "Success"
            }
            .addOnFailureListener {
                statusMessage.value = "Failed"
            }

        var index = activeChallengesList.value?.indexOf(challenge)!!
        activeChallengesList.value?.get(index)?.players?.remove(sharedPrefsRepository.user.uid)
        activeChallengesList.notifyObserver()

        index = currentChallengesList.value?.indexOf(challenge)!!
        currentChallengesList.value?.get(index)?.players?.remove(sharedPrefsRepository.user.uid)
        currentChallengesList.notifyObserver()
    }

    fun deleteChallenge(challenge: Challenge) {
        firestoreRepository.setChallengeAsNonExist(challenge.name)
            .addOnSuccessListener {
                activeChallengesList.value?.remove(challenge)
                activeChallengesList.notifyObserver()

                currentChallengesList.value?.remove(challenge)
                currentChallengesList.notifyObserver()

                challengesByYouList.value?.remove(challenge)
                challengesByYouList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Deletion failed", it.toString())
            }
    }

    fun startUnityActivityForChallenge(challenge: Challenge, action: () -> Unit) {
        sharedPrefsRepository.apply {

            val userChallenge = user.currentChallenges[challenge.name]!!
            gameMode = CHALLENGER_MODE
            challengeType = userChallenge.type
            challengeGoal = userChallenge.goal
            challengeLeafCount = userChallenge.leafCount
            challengeFruitCount = userChallenge.fruitCount
            challengeStreak = userChallenge.challengeGoalStreak
            challengeName = userChallenge.name
            isChallengeActive = userChallenge.endDate.toDateTime().millis > DateTime().millis
            challengeTotalStepsCount = if (challenge.active) getDailyStepCount() else userChallenge.totalSteps

            action()
        }
    }

    fun getChallengeDurationText(challenge: Challenge): SpannedString {
        val finishDate = challenge.finishTimestamp.toDateTime().millis
        val finishDateString = challenge.finishTimestamp.toDateTime().getStringRepresentation()

        val challengeEnded = finishDate < DateTime().millis

        return buildSpannedString {
            bold {
                append(if (challengeEnded) "Ended: " else "Ends: ")
            }
            append(finishDateString)
        }
    }

    fun hasUserJoinedChallenge(challenge: Challenge): Boolean {
        return sharedPrefsRepository.user.currentChallenges[challenge.name] != null
    }

    fun getChallengeTypeText(challenge: Challenge) =
        buildSpannedString {
            bold {
                append("Type: ")
            }
            append(if (challenge.type == CHALLENGE_TYPE_DAILY_GOAL_BASED) "Daily Goal Based"
                    else "Aggregate based")
        }

    fun getGoalText(challenge: Challenge) =
        buildSpannedString {
            bold {
                append(if(challenge.type == CHALLENGE_TYPE_DAILY_GOAL_BASED) "Minimum Daily Goal: "
                        else "Total steps goal: ")
            }
            append(challenge.goal.toString())
        }

    fun getPlayersCountText(challenge: Challenge) = challenge.players.size.toString()

    fun getCurrentUserId() = sharedPrefsRepository.user.uid

    fun getJoinChallengeDialogTitleText(challenge: Challenge) =
        buildSpannedString {
            append("Join challenge ")
            bold {
                append("'${challenge.name}'")
            }
        }

    fun getJoinChallengeMessageText() = "Are you ready to join now?"

    fun storeChallengeLeaderboardPosition(position: Int) {
        sharedPrefsRepository.challengeLeaderboardPosition = position
    }


    private fun updateUserSharedPrefsData(userChallenge: UserChallenge) {
        val user = sharedPrefsRepository.user
        userChallenge.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
        userChallenge.totalSteps = sharedPrefsRepository.getDailyStepCount()
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
            goal = challenge.goal,
            endDate = challenge.finishTimestamp
        )

    private fun removeChallengeFromCurrentChallengesLists(challenge: Challenge) {
        currentChallengesList.value?.remove(challenge)
        currentChallengesList.notifyObserver()

        sharedPrefsRepository.user = sharedPrefsRepository.user.let {
            it.currentChallenges.remove(challenge.name)
            it
        }
    }

    private fun ArrayList<Challenge>.sortAndAddToList(challenge: Challenge) {
        val finishTimestampMillis = challenge.finishTimestamp.toDateTime().millis
        if (size == 0) {
            add(challenge)
            return
        }

        for(i in 0 until size) {
            if (this[i].finishTimestamp.toDateTime().millis < finishTimestampMillis) {
                add(i, challenge)
                return
            }
        }
        this.add(challenge)
    }
}