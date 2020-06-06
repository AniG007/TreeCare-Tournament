package dal.mitacsgri.treecare.screens.teaminfo

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.StepCountRepository

class TeamInfoViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository
)
    :ViewModel()  {

    val membersList = MutableLiveData<ArrayList<TeamInfo>>().default(arrayListOf())
    val status :MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val tempList = MutableLiveData<ArrayList<TeamInfo>>().default(arrayListOf())

    fun getTeamMembers (teamName : String): MutableLiveData<ArrayList<TeamInfo>> {
        firestoreRepository.getTeam(teamName)
            .addOnSuccessListener {
                val team = it.toObject<Team>()
                Log.d("Test","members"+team?.members)
                for(m in team?.members!!) {
                    Log.d("Test","m "+m)
                    firestoreRepository.getUserData(m.toString())
                        .addOnSuccessListener {
                            val user = it.toObject<User>()
                            membersList.value?.sortAndAddToList(TeamInfo(
                                user?.uid.toString(),
                                teamName,
                                team.captain,
                                user?.name.toString(),
//                                getDailyStepCount(),
                                user?.dailySteps!!,
                                user?.photoUrl.toString(),
                                sharedPrefsRepository.getDailyStepCount()/1000
                            ))
                            Log.d("Test","memsInTeam "+membersList.value.toString())
                            membersList.notifyObserver()
                        }
                }
            }
        return membersList
    }
    fun isCurrentUser (tInfo: TeamInfo) = tInfo.uId == sharedPrefsRepository.user.uid

    fun isUserCaptain(captainUid: String) = captainUid == sharedPrefsRepository.user.uid

    fun getDailyLeafCount(): String {

        Log.d ("Test","Leaf "+(sharedPrefsRepository.getDailyStepCount() /1000).toString())
        return (sharedPrefsRepository.getDailyStepCount() /1000).toString()

    }

//    fun getDailyStepCount(): MutableMap<String, Int> {
////        Log.d("Test","Steps "+ (sharedPrefsRepository.getDailyStepCount()).toString())
////        return (sharedPrefsRepository.getDailyStepCount()).toString()
//        stepCountRepository.getTodayStepCountData {
//            TeamInfo.dailyStepMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
//            updateAndStoreUserChallengeDataInSharedPrefs(Team, user)
//        }
//
//    }


//    private fun updateAndStoreUserChallengeDataInSharedPrefs(teamInfo: TeamInfo, user: User) {
//        challenge.leafCount = getTotalLeafCountForChallenge(challenge)
//        challenge.fruitCount = getTotalFruitCountForChallenge(challenge)
//        challenge.challengeGoalStreak = getChallengeGoalStreakForUser(challenge, user)
//        challenge.lastUpdateTime = Timestamp.now()
//
//        var totalSteps = 0
//        challenge.dailyStepsMap.forEach { (time, steps) ->
//            totalSteps += steps
//        }
//        challenge.totalSteps = totalSteps
//
//        synchronized(sharedPrefsRepository.user) {
//            val user = sharedPrefsRepository.user
//            user.currentChallenges[challenge.name] = challenge
//            sharedPrefsRepository.user = user
//        }
//    }

    fun removePlayer(team : TeamInfo){
        firestoreRepository.updateTeamData(team.teamName, mapOf("members" to FieldValue.arrayRemove(team.uId)))
            .addOnSuccessListener {
                firestoreRepository.updateUserData(team.uId, mapOf("currentTeams" to FieldValue.arrayRemove(team.teamName)))
                    .addOnSuccessListener {
                        membersList.value?.remove(team)
                        membersList.notifyObserver()
                    }
            }
    }

    private fun ArrayList<TeamInfo>.sortAndAddToList(teamInfo: TeamInfo) {

        if(teamInfo.captainId.equals(teamInfo.uId)) {
            add(0,teamInfo)
            return
        }
        else{
            add(teamInfo)
            return
        }
    }
}