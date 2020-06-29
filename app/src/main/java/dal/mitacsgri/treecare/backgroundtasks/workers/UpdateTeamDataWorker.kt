package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import calculateLeafCountFromStepCountForTeam
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.toDateTime
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.TeamTournament
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.Days
import org.koin.core.KoinComponent
import org.koin.core.inject

class UpdateTeamDataWorker (appContext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appContext, workerParams), KoinComponent {

    private val stepCountRepository: StepCountRepository by inject()
    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()
    private val today = DateTime().withTimeAtStartOfDay().millis.toString()

    override fun startWork(): ListenableFuture<Result> {
        Log.d("WorkerT", "Starting Team Worker")
        val future = SettableFuture.create<Result>()

//        if(sharedPrefsRepository.getLastDayStepCount() == 0){
//            firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
//                .addOnSuccessListener {
//                    val user = it.toObject<User>()
//                    sharedPrefsRepository.storeLastDayStepCount(user?.dailySteps!!)
//                }
//        }

        val team = sharedPrefsRepository.team
        var c =0
        if (!team.currentTournaments.isNullOrEmpty()) {
            team.currentTournaments.forEach { (_, tourney) ->
                c++
                val endTimeMillis = tourney.endDate.toDateTime().millis
                if (tourney.isActive && endTimeMillis > DateTime().millis) {
                    stepCountRepository.getTodayStepCountData {
                        calc(team, it, future)
                    }
                }
            }
            if(c == team.currentTournaments.size) {
                Log.d("WorkerT", "count "+ c+ "size "+ team.currentTournaments.size)
                updateUserTeamDataInFirestore(future)
            }
        }
        return future
    }

    private fun calc(team: Team, currentStepCount: Int, future: SettableFuture<Result>) {
        //Sorting the step map according to the dates
        Log.d("WorkerT", "Inside Calc")
        team.currentTournaments.forEach { (_, teamTourney) ->
            teamTourney.dailyStepsMap = teamTourney.dailyStepsMap.toSortedMap()
        }

        team.currentTournaments.forEach { (_, teamTourney) ->
            if (teamTourney.isActive && teamTourney.endDate.toDateTime().millis > DateTime().millis) {
                if (teamTourney.dailyStepsMap.isNotEmpty()) {
                    Log.d("WorkerT", "pref not empty")
//                    Log.d("WorkerT", teamTourney.dailyStepsMap.keys.last() + " " + tourney.dailyStepsMap.keys.elementAt(index))

                    if (teamTourney.dailyStepsMap.keys.last() != DateTime().withTimeAtStartOfDay().millis.toString()) {
                        Log.d("WorkerT", "pref up to date")
                        //when tournament exists and user starts a new day // user opens the app first day for a day
                        //condition check for new day. Since only one time stamp (date in millis) is used during the update of values
                        //DB fetch is needed
                        firestoreRepository.getTeam(team.name)
                            .addOnSuccessListener {

                                val teamDB = it.toObject<Team>()

                                if (teamDB?.currentTournaments!![teamTourney.name]?.dailyStepsMap?.isNotEmpty()!!) {
                                    Log.d("WorkerT", "DB not empty for tournament ${teamTourney.name}")
                                    //check for today's date stamp in db
                                    // set DB value to pref and update back to DB
                                    Log.d("WorkerT", "Last Dates: pref"+ teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap?.values?.last().toString() +"Date "+ DateTime().withTimeAtStartOfDay().millis.toString())
                                    if(teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap?.values?.last().toString() == DateTime().withTimeAtStartOfDay().millis.toString()) {
                                        Log.d("WorkerT", "DB up to Date for${teamTourney.name}")
                                        val oldStep = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
                                        val diff = currentStepCount - sharedPrefsRepository.getLastDayStepCount()
                                        Log.d("WorkerT", "Diff1 "+ diff)
                                        val updatedSteps = oldStep!! + diff

                                        if (diff > 0) {
                                            teamTourney.dailyStepsMap =
                                                teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!

                                            teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                                updatedSteps

                                            updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                            //updateUserTeamDataInFirestore(future)
                                            sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                                        }
                                    }
                                    else{
                                        Log.d("WorkerT", "DB not up to Date")
                                        teamTourney.dailyStepsMap = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
                                        teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                            currentStepCount

                                        updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                        //updateUserTeamDataInFirestore(future)
                                        sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                                    }
                                }

                                else {
                                    Log.d("WorkerT", "DB step map empty")
                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                        currentStepCount

                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                    //updateUserTeamDataInFirestore(future)
                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                                }
                            }
                    }
                    else {
                        Log.d("WorkerT", "LastDayStepCount " + sharedPrefsRepository.getLastDayStepCount())
                        Log.d("WorkerT", "pref not up to date")
                        //TODO: check if last date is in db or not
                        //val oldStep = tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()]
                        firestoreRepository.getTeam(team.name)
                            .addOnSuccessListener {
                                val teamDB = it.toObject<Team>()
                                val tournaments = teamDB?.currentTournaments
                                if(tournaments!![teamTourney.name]?.dailyStepsMap?.keys?.last().toString() != DateTime().withTimeAtStartOfDay().millis.toString()){
                                    Log.d("WorkerT", "DB is not upto date")
                                    teamTourney.dailyStepsMap = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount

                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                    //updateUserTeamDataInFirestore(future)
                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                                }
                                else {
                                    Log.d("WorkerT", "DB is upto date")
                                    val oldStep =
                                        tournaments[teamTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
                                    val diff =
                                        currentStepCount - sharedPrefsRepository.getLastDayStepCount()
                                    Log.d("WorkerT", "Diff2 " + diff)
                                    if (diff > 0) {
                                        val updatedSteps = oldStep!! + diff
                                        teamTourney.dailyStepsMap =
                                            teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
                                        teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = updatedSteps

                                        updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                        //updateUserTeamDataInFirestore(future)
                                        sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                                    }
                                }
                            }
                    }
                }

                else {
                    Log.d("WorkerT", "prefs is empty")
                    // When tournament exists but doesn't have steps in the teams collection
                    // take care of updating team and tour prefs when user has joined a tournament/team (when user is not a captain)
                    firestoreRepository.getTeam(team.name)
                        // checking if the tournament has steps updated by other users. If not, then we update it, if present,
                        // the last step count is fetched and then it is incremented with the users steps and is updated back in the db
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            val tournaments = teamDB?.currentTournaments
                            if (tournaments!![teamTourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                Log.d("WorkerT", "DB and pref have empty step Map")

                                teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount

                                updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                //updateUserTeamDataInFirestore(future)
                                sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                            }

                            else{
                                Log.d("WorkerT", "DB is upto date and pref is empty")
                                val oldStep = tournaments[teamTourney.name]?.dailyStepsMap?.values?.last()
                                val diff = currentStepCount - sharedPrefsRepository.getLastDayStepCount()
                                Log.d("WorkerT", "Diff3 "+ diff)
                                val updatedSteps = oldStep!! + diff
                                if (diff > 0) {
                                    teamTourney.dailyStepsMap =
                                        teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
                                        updatedSteps
                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
                                    //updateUserTeamDataInFirestore(future)
                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun updateAndStoreTeamDataInSharedPrefs(teamTourney: TeamTournament, team: Team) {
        teamTourney.leafCount = getTotalLeafCountForTeam(teamTourney)
        teamTourney.fruitCount = getTotalFruitCountForTeam(teamTourney)
        teamTourney.tournamentGoalStreak = getTeamGoalStreakForUser(teamTourney, team)
        teamTourney.lastUpdateTime = Timestamp.now()
        Log.d("WorkerT", "updateAndStoreTeamDataInSharedPrefs")
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

    private fun updateUserTeamDataInFirestore(future: SettableFuture<Result>) {
        Log.d("WorkerT", "updateUserTeamDataInFirestore")
        Log.d("WorkerT","Pref "+sharedPrefsRepository.team)
        Log.d("WorkerT","PrefValue "+ sharedPrefsRepository.team.currentTournaments)
        firestoreRepository.updateTeamData(sharedPrefsRepository.team.name,
            mapOf("currentTournaments" to sharedPrefsRepository.team.currentTournaments))
            .addOnSuccessListener {
                Log.d("WorkerT", "Team User data upload success")
                future.set(Result.success())
            }
            .addOnFailureListener {
                Log.e("WorkerT", "Team User data upload failed")
                future.set(Result.failure())
            }
    }

    private fun getTotalLeafCountForTeam(teamTournament: TeamTournament): Int {
        Log.d("WorkerT","getTotalLeafCountForTeam")
        val stepsMap = teamTournament.dailyStepsMap
        val goal = teamTournament.goal
        var leafCount = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
            //for (i in 0 until keys.size) {
            Log.d("WorkerT","StepMap "+ stepsMap[keys[i]])
            leafCount += calculateLeafCountFromStepCountForTeam(stepsMap[keys[i]]!!, goal)
        }
        leafCount += stepsMap[keys[keys.size-1]]!! / 3000


        return leafCount
    }

    private fun getTotalFruitCountForTeam(tournament: TeamTournament): Int {
        Log.d("WorkerT","getTotalFruitCountForTeam")
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
        Log.d("WorkerT","calculateTeamFruitCountForWeek")
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
        Log.d("WorkerT","getTeamGoalStreakForUser")
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