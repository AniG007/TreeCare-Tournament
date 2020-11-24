package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import calculateLeafCountFromStepCount
import calculateLeafCountFromStepCountForTeam
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.consts.CHALLENGE_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.toDateTime
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.Days
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.concurrent.TimeUnit

class UpdateUserChallengeDataWorker(appContext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appContext, workerParams), KoinComponent {

    private val stepCountRepository: StepCountRepository by inject()
    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()
    private val mConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    var cr = 0

    override fun startWork(): ListenableFuture<Result> {
        Log.d("Worker","Starting Worker")
        val future = SettableFuture.create<Result>()

        if(sharedPrefsRepository.user.name.isEmpty()){
            sharedPrefsRepository.user = User()
        }

        val user = sharedPrefsRepository.user
        var county = 0

//        sharedPrefsRepository.team = Team()
//
//        firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
//            .addOnSuccessListener {
//                val dbTeam = it.toObject<Team>()
//                sharedPrefsRepository.team = dbTeam!!
//            }
//        Log.d("Worker","Team "+ sharedPrefsRepository.team)
//        val team = sharedPrefsRepository.team

        user.currentChallenges.forEach { (_, challenge) ->
            if(challenge.isActive) {
                county++
            }
        }

        if(!user.currentChallenges.isNullOrEmpty()) {
            user.currentChallenges.forEach { (_, challenge) ->

                val endTimeMillis = challenge.endDate.toDateTime().millis
                //Two condition checks are applied because the 'isActive' variable is set only after
                //the dialog has been displayed. The second condition check prevents update of challenge step count
                //in the database even when the dialog has not been displayed
                if (challenge.isActive && endTimeMillis > DateTime().millis) {
                    if (challenge.type == CHALLENGE_TYPE_DAILY_GOAL_BASED) {
                        stepCountRepository.getTodayStepCountData {
                            challenge.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
                            updateAndStoreUserChallengeDataInSharedPrefs(challenge, user, county, future)

                        }
                    }
                }
            }
//            updateUserChallengeDataInFirestore()
        }
//        if(!user.currentTournaments.isNullOrEmpty() && !team.currentTournaments.isNullOrEmpty()) {
//            user.currentTournaments.forEach { (_, tourney) ->
//
//                val startTimeMillis = tourney.startDate.toDateTime().millis
//                val endTimeMillis = tourney.endDate.toDateTime().millis
//                //Two condition checks are applied because the 'isActive' variable is set only after
//                //the dialog has been displayed. The second condition check prevents update of Tournament step count
//                //in the database even when the dialog has not been displayed
//                if (tourney.isActive && endTimeMillis > DateTime().millis) {
//                    //if (tourney.isActive) {
//                    Log.d("Worker", "TourneyName "+ tourney.name)
//                    stepCountRepository.getTodayStepCountData {
//                        tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = it
//                        val index = tourney.dailyStepsMap.keys.indexOf(DateTime().withTimeAtStartOfDay().millis.toString())
//                        Log.d("Worker","Index "+ index.toString())
//                        updateAndStoreUserTournamentDataInSharedPrefs(tourney, user)
//                        calc(team, it, tourney, index)
//                    }
//                }
//            }
//            updateUserTournamentDataInFirestore(future)
//            //updateUserTeamDataInFirestore(future)
//        }
        else{
            Log.d("Worker","Current Challenges is empty")
        }

        val updateUserChallengeDataRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateUserChallengeDataWorker>()
                .setConstraints(mConstraints)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()
        //WorkManager.getInstance(applicationContext).enqueue(updateUserChallengeDataRequest)


        return future
    }

    private fun updateAndStoreUserChallengeDataInSharedPrefs(challenge: UserChallenge, user: User, county: Int, future:SettableFuture<Result>) {
        cr++
        challenge.leafCount = getTotalLeafCountForChallenge(challenge)
        challenge.fruitCount = getTotalFruitCountForChallenge(challenge)
        challenge.challengeGoalStreak = getChallengeGoalStreakForUser(challenge, user)
        challenge.lastUpdateTime = Timestamp.now()

        var totalSteps = 0
        challenge.dailyStepsMap.forEach { (time, steps) ->
            totalSteps += steps
        }
        challenge.totalSteps = totalSteps

        synchronized(sharedPrefsRepository.user) {
                val user = sharedPrefsRepository.user
                user.currentChallenges[challenge.name] = challenge
                sharedPrefsRepository.user = user
        }
        updateUserChallengeDataInFirestore(cr, county, future)
    }

    private fun getChallengeGoalStreakForUser(challenge: UserChallenge, user: User): Int {
        val userChallengeData = user.currentChallenges[challenge.name]!!
        var streakCount = 0

        userChallengeData.dailyStepsMap.forEach { (date, stepCount) ->
            //This check prevents resetting streak count if goal is yet to be met today
            if (date.toLong() < DateTime().withTimeAtStartOfDay().millis) {
                if (stepCount >= challenge.goal) streakCount++
                else streakCount = 0
            }
        }
        return streakCount
    }

    private fun updateUserChallengeDataInFirestore(cr:Int, county: Int, future: SettableFuture<Result>) {
        if(cr == county) {
            firestoreRepository.updateUserData(
                sharedPrefsRepository.user.uid,
                mapOf("currentChallenges" to sharedPrefsRepository.user.currentChallenges)
            )
                .addOnSuccessListener {
                    Log.d("Worker", "User data upload success")
                    future.set(Result.success())
                }
                .addOnFailureListener {
                    Log.e("Worker", "User data upload failed")
                    future.set(Result.failure())
                }
        }
    }

    private fun getTotalLeafCountForChallenge(challenge: UserChallenge): Int {
        val stepsMap = challenge.dailyStepsMap
        val goal = challenge.goal
        var leafCount = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
            leafCount += calculateLeafCountFromStepCount(stepsMap[keys[i]]!!, goal)
        }
        leafCount += stepsMap[keys[keys.size-1]]!! / 1000

        return leafCount
    }

    private fun getTotalFruitCountForChallenge(challenge: UserChallenge): Int {
        val joinDate = DateTime(challenge.joinDate)
        val currentDate = DateTime()
        val days = Days.daysBetween(joinDate, currentDate).days
        val weeks = Math.ceil(days/7.0).toInt()

        //Log.d("Worker", "weeks "+ weeks)
        var fruitCount = 0

        var weekStartDate = joinDate
        var newWeekDate = weekStartDate.plusWeeks(1)
        var mapPartition: Map<String, Int>
        for (i in 0 until weeks) {
            mapPartition = challenge.dailyStepsMap.filter {
                val keyAsLong = it.key.toLong()
                keyAsLong >= weekStartDate.millis && keyAsLong < newWeekDate.millis
            }
            fruitCount += calculateFruitCountForWeek(challenge, mapPartition)
            weekStartDate = newWeekDate
            newWeekDate = weekStartDate.plusWeeks(1)
        }

        Log.d("fruits", fruitCount.toString())
        return fruitCount
    }

    /**
     * @param challenge UserChallenge
     * @param stepCountMap Step count map for a week
     * @return +1, 0 or -1 as fruit count
     */
    private fun calculateFruitCountForWeek(challenge: UserChallenge, stepCountMap: Map<String, Int>): Int {
        var currentDay = 0
        val goalAchievedStreak = arrayOf(false, false, false, false, false, false, false)
        val fullStreak = arrayOf(true, true, true, true, true, true, true)

        if (stepCountMap.size < 7) return 0

        stepCountMap.forEach { (_, stepCount) ->
            goalAchievedStreak[currentDay] =
                stepCount >= challenge.goal
            currentDay++
        }

        return if (goalAchievedStreak.contentEquals(fullStreak)) 1 else -1
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////Tournament Functions////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private fun updateAndStoreUserTournamentDataInSharedPrefs(tournament: UserTournament, user: User) {
        //tournament.leafCount = getTotalLeafCountForTeam(tournament)
        // tournament.fruitCount = getTotalFruitCountForTournament(tournament)
        // tournament.tournamentGoalStreak = getTournamentGoalStreakForUser(tournament, user)
        // tournament.lastUpdateTime = Timestamp.now()
        Log.d("Worker", "updateAndStoreUserTournamentDataInSharedPrefs")
        tournament.lastUpdateTime = Timestamp.now()
        var totalSteps = 0
        tournament.dailyStepsMap.forEach { (time, steps) ->
            totalSteps += steps
        }
        tournament.totalSteps = totalSteps

        synchronized(sharedPrefsRepository.user) {
            val user = sharedPrefsRepository.user
            user.currentTournaments[tournament.name] = tournament
            sharedPrefsRepository.user = user
        }
        Log.d("Worker","UserPref "+ sharedPrefsRepository.user.currentTournaments)
    }

//    private fun updateUserTournamentDataInFirestore(future: SettableFuture<Result>) {
//        Log.d("Worker", "updateUserTournamentDataInFirestore")
//        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
//            mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments))
//            .addOnSuccessListener {
//                updateUserTeamDataInFirestore(future)
//                Log.d("Worker", "TUser data upload success")
//                //future.set(Result.success())
//            }
//            .addOnFailureListener {
//                Log.e("Worker", "TUser data upload failed")
//                //future.set(Result.failure())
//            }
//    }

//    private fun updateUserTeamDataInFirestore(future: SettableFuture<Result>) {
//        Log.d("Worker", "updateUserTeamDataInFirestore")
//        Log.d("Worker","Pref "+sharedPrefsRepository.team)
//        Log.d("Worker","PrefValue "+ sharedPrefsRepository.team.currentTournaments)
//        firestoreRepository.updateTeamData(sharedPrefsRepository.team.name,
//            mapOf("currentTournaments" to sharedPrefsRepository.team.currentTournaments))
//            .addOnSuccessListener {
//                Log.d("Worker", "Team User data upload success")
//                future.set(Result.success())
//            }
//            .addOnFailureListener {
//                Log.e("Worker", "Team User data upload failed")
//                future.set(Result.failure())
//            }
//    }

    private fun updateAndStoreTeamDataInSharedPrefs(teamTourney: TeamTournament, team: Team) {
        teamTourney.leafCount = getTotalLeafCountForTeam(teamTourney)
        teamTourney.fruitCount = getTotalFruitCountForTeam(teamTourney)
        teamTourney.tournamentGoalStreak = getTeamGoalStreakForUser(teamTourney, team)
        teamTourney.lastUpdateTime = Timestamp.now()
        Log.d("Worker", "updateAndStoreTeamDataInSharedPrefs")
        var totalSteps = 0
        teamTourney.dailyStepsMap.forEach { (time, steps) ->
            totalSteps += steps
        }
        teamTourney.totalSteps = totalSteps

        synchronized(sharedPrefsRepository.team) {
            val team = sharedPrefsRepository.team
            team.currentTournaments[teamTourney.name] = teamTourney
            sharedPrefsRepository.team = team
        }
    }

    private fun getTotalLeafCountForTeam(teamTournament: TeamTournament): Int {
        Log.d("Worker","getTotalLeafCountForTeam")
        val stepsMap = teamTournament.dailyStepsMap
        val goal = teamTournament.goal
        var leafCount = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
        //for (i in 0 until keys.size) {
            Log.d("Worker","StepMap "+ stepsMap[keys[i]])
            leafCount += calculateLeafCountFromStepCountForTeam(stepsMap[keys[i]]!!, goal)
        }
        leafCount += stepsMap[keys[keys.size-1]]!! / 3000


        return leafCount
    }

    private fun getTotalFruitCountForTeam(tournament: TeamTournament): Int {
        Log.d("Worker","getTotalFruitCountForTeam")
        val joinDate = DateTime(tournament.joinDate)
        val currentDate = DateTime()
        val days = Days.daysBetween(joinDate, currentDate).days
        val weeks = Math.ceil(days/7.0).toInt()

        var fruitCount = 0

        var weekStartDate = joinDate
        var newWeekDate = weekStartDate.plusWeeks(1)
        var mapPartition: Map<String, Int>
        for (i in 0 until weeks) {
            mapPartition = tournament.dailyStepsMap.filter {
                val keyAsLong = it.key.toLong()
                keyAsLong >= weekStartDate.millis && keyAsLong < newWeekDate.millis
            }
            fruitCount += calculateTeamFruitCountForWeek(tournament, mapPartition)
            weekStartDate = newWeekDate
            newWeekDate = weekStartDate.plusWeeks(1)
        }

        return fruitCount
    }

    private fun calculateTeamFruitCountForWeek(tournament: TeamTournament, stepCountMap: Map<String, Int>): Int {
        Log.d("Worker","calculateTeamFruitCountForWeek")
        var currentDay = 0
        val goalAchievedStreak = arrayOf(false, false, false, false, false, false, false)
        val fullStreak = arrayOf(true, true, true, true, true, true, true)

        if (stepCountMap.size < 7) return 0

        stepCountMap.forEach { (_, stepCount) ->
            goalAchievedStreak[currentDay] =
                stepCount >= tournament.goal
            currentDay++
        }

        return if (goalAchievedStreak.contentEquals(fullStreak)) 1 else -1
    }

    private fun getTeamGoalStreakForUser(tournament: TeamTournament, team: Team): Int {
        Log.d("Worker","getTeamGoalStreakForUser")
        val teamTournamentData = team.currentTournaments[tournament.name]!!
        var streakCount = 0

        teamTournamentData.dailyStepsMap.forEach { (date, stepCount) ->
            //This check prevents resetting streak count if goal is yet to be met today
            if (date.toLong() < DateTime().withTimeAtStartOfDay().millis) {
                if (stepCount >= tournament.goal) streakCount++
                else streakCount = 0
            }
        }
        return streakCount
    }
}
