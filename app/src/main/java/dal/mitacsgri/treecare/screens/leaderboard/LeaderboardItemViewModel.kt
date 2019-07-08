package dal.mitacsgri.treecare.screens.leaderboard

import android.text.SpannedString
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.toObject
import com.google.gson.Gson
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_AGGREGATE_BASED
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.Challenger
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.model.UserChallenge
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository

class LeaderboardItemViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository
    ) : ViewModel() {

    private lateinit var challenge: Challenge

    fun isCurrentUser(challenger: Challenger) = challenger.uid == sharedPrefsRepository.user.uid

    fun getAchievementText(challenger: Challenger): SpannedString =
            when(challenge.type) {
                CHALLENGE_TYPE_DAILY_GOAL_BASED ->
                    buildSpannedString {
                        bold {
                            append("Daily Goal Streak: ")
                        }
                        append(challenger.challengeGoalStreak.toString())
                    }

                CHALLENGE_TYPE_AGGREGATE_BASED -> {
                    buildSpannedString {
                        bold {
                            append("Total Steps: ")
                        }
                        append(challenger.totalSteps.toString())
                    }
                }

                else -> buildSpannedString {  }
            }

    fun getChallengersList(challengeName: String): LiveData<ArrayList<Challenger>> {
        val challengersList = MutableLiveData<ArrayList<Challenger>>().default(arrayListOf())

        firestoreRepository.getChallenge(challengeName)
            .addOnSuccessListener {
                challenge = it.toObject<Challenge>()!!
                val challengers = challenge.players
                val challengersCount = challenge.players.size
                val limit = if (challengersCount > 10) 10 else challengersCount

                for (i in 0 until limit) {
                    firestoreRepository.getUserData(challengers[i])
                        .addOnSuccessListener {
                            val user = it.toObject<User>()
                            val challenger = user?.let{ makeChallengerFromUser(user, challenge) }
                            challengersList.value?.add(challenger!!)

                            if (challengersList.value?.size == limit) {
                                challengersList.value?.sortChallengersList(challenge.type)
                                challengersList.notifyObserver()
                            }
                        }
                }
            }

        return challengersList
    }

    fun getCurrentChallengerPosition(challengers: ArrayList<Challenger>): Int {
        val currentUserUid = sharedPrefsRepository.user.uid
        for (i in 0 until challengers.size) {
            if (challengers[i].uid == currentUserUid)
                return i+1
        }
        return -1
    }

    private fun makeChallengerFromUser(user: User, challenge: Challenge): Challenger {
        val userChallengeData = Gson().fromJson(
            user.currentChallenges[challenge.name], UserChallenge::class.java)

        return Challenger(
            name = user.name,
            uid = user.uid,
            photoUrl = user.photoUrl,
            challengeGoalStreak = userChallengeData.challengeGoalStreak,
            totalSteps = userChallengeData.totalSteps)
    }

    private fun ArrayList<Challenger>.sortChallengersList(challengeType: Int) {
        sortByDescending {
            when(challengeType) {
                CHALLENGE_TYPE_DAILY_GOAL_BASED -> it.challengeGoalStreak
                CHALLENGE_TYPE_AGGREGATE_BASED -> it.totalSteps
                else -> it.totalSteps
            }
        }
    }
}