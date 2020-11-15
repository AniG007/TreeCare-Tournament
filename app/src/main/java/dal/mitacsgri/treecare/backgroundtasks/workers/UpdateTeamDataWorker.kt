package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import calculateLeafCountFromStepCountForTeam
import calculateDailyGoalsAchievedFromStepCountForTeam
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.toDateTime
import dal.mitacsgri.treecare.model.Team
import dal.mitacsgri.treecare.model.TeamTournament
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days
import org.joda.time.format.DateTimeFormat
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UpdateTeamDataWorker (appContext: Context, workerParams: WorkerParameters)
    : ListenableWorker(appContext, workerParams), KoinComponent {

    private val stepCountRepository: StepCountRepository by inject()
    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()
    //private val today = DateTime().withTimeAtStartOfDay()
    private val today = DateTime()
    private val halifaxTimeZone = DateTimeZone.forID("America/Halifax")
    //private val dateOutputFormat = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss").withZone(halifaxTimeZone)

    var cr = 0 //for counting number of calc call for the active tourneys

    override fun startWork(): ListenableFuture<Result> {

        Log.d("WorkerT", "Starting Team Worker")
        val future = SettableFuture.create<Result>()
        Log.d("Datey", "Today: "+ today.toString())
        //dateOutputFormat.setTimeZone(TimeZone.getTimeZone("America/Halifax"))  //https://stackoverflow.com/questions/37390080/convert-local-time-to-utc-and-vice-versa
        //val mNight = Date(dateOutputFormat.format(today)).time   //Converting time to Halifax time so that all phones update according to halifax midnight time.
        val mNight = today.withZone(halifaxTimeZone).withTimeAtStartOfDay().millis
        Log.d("Datey", "After conv"+ mNight.toString())

//        if(sharedPrefsRepository.getLastDayStepCount() == 0){
//            firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
//                .addOnSuccessListener {
//                    val user = it.toObject<User>()
//                    sharedPrefsRepository.storeLastDayStepCount(user?.dailySteps!!)
//                }
//        }


        val team = sharedPrefsRepository.team
        var county = 0

        team.currentTournaments.forEach { (_, tourney) ->
            if(tourney.isActive && tourney.endDate.toDateTime().millis > DateTime().millis && tourney.startDate.toDateTime().millis <= DateTime().millis)
                county++
        }

        if (!team.currentTournaments.isNullOrEmpty()) {
            team.currentTournaments.forEach { (_, tourney) ->
                val startTimeMillis = tourney.startDate.toDateTime().millis
                val endTimeMillis = tourney.endDate.toDateTime().millis
                if (tourney.isActive && endTimeMillis > DateTime().millis && startTimeMillis <= DateTime().millis) {
                    stepCountRepository.getTodayStepCountData {
                        Log.d("WorkerT", "TourneyName: " + tourney.name)
                        calc(team, it, future, county, tourney, mNight)
                    }
                }
            }
        }
//            if(c == team.currentTournaments.size) {
//                Log.d("WorkerT", "count "+ c+ "size "+ team.currentTournaments.size)
//                updateUserTeamDataInFirestore(future)
//                c=0
//            }

//            val updateTeamDataRequest = OneTimeWorkRequestBuilder<UpdateTeamDataWorker>()
//                .setConstraints(mConstraints)
//                .setInitialDelay(5,TimeUnit.MINUTES)
//                .build() // calling this again so as to mimic periodic work request
//            WorkManager.getInstance(applicationContext).enqueue(updateTeamDataRequest)

//            val serviceIntent = Intent(applicationContext, ForegroundService::class.java)
//            serviceIntent.putExtra("inputExtra", "input")
//            ContextCompat.startForegroundService(applicationContext, serviceIntent)

       // WorkManager.getInstance(applicationContext).enqueue(updateTeamDataRequest)
        return future
    }

//    private fun calc(team: Team, currentStepCount: Int, future: SettableFuture<Result>) {
//        //Sorting the step map according to the dates
//        Log.d("WorkerT", "Inside Calc")
//
//        team.currentTournaments.forEach { (_, teamTourney) ->
//            teamTourney.dailyStepsMap = teamTourney.dailyStepsMap.toSortedMap()
//        }
//
//        team.currentTournaments.forEach { (_, teamTourney)  ->
//            if (teamTourney.isActive && teamTourney.endDate.toDateTime().millis > DateTime().millis) {
//                if (teamTourney.dailyStepsMap.isNotEmpty()) {
//                    Log.d("WorkerT", "pref not empty")
////                    Log.d("WorkerT", teamTourney.dailyStepsMap.keys.last() + " " + tourney.dailyStepsMap.keys.elementAt(index))
//
//                    if (teamTourney.dailyStepsMap.keys.last() != DateTime().withTimeAtStartOfDay().millis.toString()) {
//                        Log.d("WorkerT", "pref needs to be updated")
//                        //when tournament exists and user starts a new day // user opens the app first day for a day
//                        //condition check for new day. Since only one time stamp (date in millis) is used during the update of values
//                        //DB fetch is needed
//                        firestoreRepository.getTeam(team.name)
//                            .addOnSuccessListener {
//
//                                val teamDB = it.toObject<Team>()
//
//                                if (teamDB?.currentTournaments!![teamTourney.name]?.dailyStepsMap?.isNotEmpty()!!) {
//                                    Log.d("WorkerT", "DB not empty for tournament ${teamTourney.name}")
//                                    //check for today's date stamp in db
//                                    // set DB value to pref and update back to DB
//                                    Log.d("WorkerT", "Last Dates: pref"+ teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap?.keys?.last().toString() +"Date "+ DateTime().withTimeAtStartOfDay().millis.toString())
//                                    if(teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap?.keys?.last().toString() == DateTime().withTimeAtStartOfDay().millis.toString()) {
//                                        Log.d("WorkerT", "DB up to Date for${teamTourney.name}")
//                                        val oldStep = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
//                                        //todo: user needs to update steps for starting a new day
//                                        val diff = currentStepCount - sharedPrefsRepository.getLastDayStepCount()
//                                        Log.d("WorkerT", "Diff1 "+ diff)
//                                        val updatedSteps = oldStep!! + diff
//
//                                        if (diff > 0) {
//                                            teamTourney.dailyStepsMap =
//                                                teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//
//                                            teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                                updatedSteps
//
//                                            updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                            //updateUserTeamDataInFirestore(future)
//                                            sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                        }
//                                    }
//                                    else{
//                                        Log.d("WorkerT", "DB not up to Date")
//                                        teamTourney.dailyStepsMap = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                        teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                            currentStepCount
//
//                                        updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                        //updateUserTeamDataInFirestore(future)
//                                        sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                    }
//                                }
//
//                                else {
//                                    Log.d("WorkerT", "DB step map empty")
//                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                        currentStepCount
//
//                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                    //updateUserTeamDataInFirestore(future)
//                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                }
//                            }
//                    }
//                    else {
//                        Log.d("WorkerT", "LastDayStepCount " + sharedPrefsRepository.getLastDayStepCount())
//                        Log.d("WorkerT", "pref not up to date")
//                        //TODO: check if last date is in db or not
//                        //val oldStep = tourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()]
//                        firestoreRepository.getTeam(team.name)
//                            .addOnSuccessListener {
//                                val teamDB = it.toObject<Team>()
//                                val tournaments = teamDB?.currentTournaments
//                                if(tournaments!![teamTourney.name]?.dailyStepsMap?.keys?.last().toString() != DateTime().withTimeAtStartOfDay().millis.toString()){
//                                    Log.d("WorkerT", "DB is not upto date")
//                                    teamTourney.dailyStepsMap = teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount
//
//                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                    //updateUserTeamDataInFirestore(future)
//                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                }
//                                else {
//                                    Log.d("WorkerT", "DB is upto date")
//                                    val oldStep =
//                                        tournaments[teamTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()]
//                                    val diff =
//                                        currentStepCount - sharedPrefsRepository.getLastDayStepCount()
//                                    Log.d("WorkerT", "Diff2 " + diff)
//                                    if (diff > 0) {
//                                        val updatedSteps = oldStep!! + diff
//                                        teamTourney.dailyStepsMap =
//                                            teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                        teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = updatedSteps
//
//                                        updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                        //updateUserTeamDataInFirestore(future)
//                                        sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                    }
//                                }
//                            }
//                    }
//                }
//
//                else {
//                    Log.d("WorkerT", "prefs is empty")
//                    // When tournament exists but doesn't have steps in the teams collection
//                    // take care of updating team and tour prefs when user has joined a tournament/team (when user is not a captain)
//                    firestoreRepository.getTeam(team.name)
//                        // checking if the tournament has steps updated by other users. If not, then we update it, if present,
//                        // the last step count is fetched and then it is incremented with the users steps and is updated back in the db
//                        .addOnSuccessListener {
//                            val teamDB = it.toObject<Team>()
//                            val tournaments = teamDB?.currentTournaments
//                            if (tournaments!![teamTourney.name]?.dailyStepsMap?.isEmpty()!!) {
//                                Log.d("WorkerT", "DB and pref have empty step Map")
//
//                                teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount
//
//                                updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                //updateUserTeamDataInFirestore(future)
//                                sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                            }
//
//                            else{
//                                Log.d("WorkerT", "DB is upto date and pref is empty")
//                                val oldStep = tournaments[teamTourney.name]?.dailyStepsMap?.values?.last()
//                                val diff = currentStepCount - sharedPrefsRepository.getLastDayStepCount()
//                                Log.d("WorkerT", "Diff3 "+ diff)
//                                val updatedSteps = oldStep!! + diff
//                                if (diff > 0) {
//                                    teamTourney.dailyStepsMap =
//                                        teamDB.currentTournaments[teamTourney.name]?.dailyStepsMap!!
//                                    teamTourney.dailyStepsMap[DateTime().withTimeAtStartOfDay().millis.toString()] =
//                                        updatedSteps
//                                    updateAndStoreTeamDataInSharedPrefs(teamTourney, team)
//                                    //updateUserTeamDataInFirestore(future)
//                                    sharedPrefsRepository.storeLastDayStepCount(currentStepCount)
//                                }
//                            }
//                        }
//                }
//            }
//        }
//    }

    private fun calc(team: Team, currentStepCount: Int, future:SettableFuture<Result>,county: Int, tourney: TeamTournament, midNight: Long) {
        cr++
        Log.d("WorkerT", "cr: " + cr)
        val userTourneys = sharedPrefsRepository.user
        //Sorting the step map according to the dates
        Log.d("WorkerT", "Inside Calc")
        Log.d("WorkerT", "County: "+ county)

        team.currentTournaments.forEach { (_, teamTourney) ->
            teamTourney.dailyStepsMap = teamTourney.dailyStepsMap.toSortedMap()
        }

        userTourneys.currentTournaments.forEach { (_, userTourney) ->
            userTourney.dailyStepsMap = userTourney.dailyStepsMap.toSortedMap()
        }

        firestoreRepository.getUserData(sharedPrefsRepository.user.uid)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                val userTournaments = user?.currentTournaments

                // userTournaments?.forEach { (_, userTourney) ->
                if (userTournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!! && userTournaments[tourney.name]?.isActive!!) {
                    Log.d("WorkerT", "Stepmap is empty for ${tourney.name}")

//                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
//                        currentStepCount

                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                        currentStepCount
                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                        Timestamp.now()
                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                        currentStepCount
                    //sharedPrefsRepository.user = userTourneys
                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)

//                        userTourneys[userTourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] =
//                            currentStepCount
//
//                        userTourneys[userTourney.name]?.lastUpdateTime = Timestamp.now()

//                        sharedPrefsRepository.user = userTourneys

                    Log.d("WorkerT", sharedPrefsRepository.user.currentTournaments[tourney.name].toString())

                    firestoreRepository.updateUserData(
                        user.uid,
                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                    )
                        .addOnSuccessListener {
                            Log.d("WorkerT", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                            Log.d("WorkerT", "stepmap for ${tourney.name} was uploaded successfully")

                            firestoreRepository.getTeam(team.name)
                                .addOnSuccessListener {
                                    val teamDB = it.toObject<Team>()
                                    val teamTourney = teamDB?.currentTournaments
                                    if (teamTourney!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                        Log.d(
                                            "WorkerT",
                                            "Team tourney:${tourney.name} Daily Step Map is empty"
                                        )
                                        team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                            currentStepCount

                                        updateAndStoreTeamDataInSharedPrefs(
                                            team.currentTournaments[tourney.name]!!,
                                            team,
                                            future,
                                            cr,
                                            county
                                        )
                                        //updateUserTeamDataInFirestore(future)
//                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                currentStepCount
//                                            )
                                    } else {
                                        Log.d("WorkerT", "Daily Step Map is not empty")
                                        if (teamTourney[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                .toString() != midNight.toString()
                                        ) {
                                            Log.d(
                                                "WorkerT",
                                                "Team tourney: ${tourney.name} is not up to date"
                                            )
                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                currentStepCount

                                            updateAndStoreTeamDataInSharedPrefs(
                                                team.currentTournaments[tourney.name]!!,
                                                team,
                                                future,
                                                cr,
                                                county
                                            )
                                            //updateUserTeamDataInFirestore(future)
//                                                sharedPrefsRepository.storeLastDayStepCount(
//                                                    currentStepCount
//                                                )
                                        } else {
                                            Log.d(
                                                "WorkerT",
                                                "Team tourney: ${tourney.name} is up to date"
                                            )
                                            val oldStep =
                                                teamTourney[tourney.name]?.dailyStepsMap?.values?.last()
                                            val updatedSteps = oldStep!! + currentStepCount
                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] = updatedSteps


                                            team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                updatedSteps
                                            updateAndStoreTeamDataInSharedPrefs(
                                                team.currentTournaments[tourney.name]!!,
                                                team,
                                                future,
                                                cr,
                                                county
                                            )
                                            //updateUserTeamDataInFirestore(future)
//                                                sharedPrefsRepository.storeLastDayStepCount(
//                                                    currentStepCount
//                                                )
                                        }
                                    }
                                }
                        }
                        .addOnFailureListener {
                            Log.d("WorkerT", "stepmap for ${tourney.name} failed to upload")
                        }

                } else if (tourney.isActive) {
                    Log.d("WorkerT", "User Tourney: ${tourney.name} stepmap is not empty")
                    firestoreRepository.getTeam(team.name)
                        .addOnSuccessListener {
                            val teamDB = it.toObject<Team>()
                            val tournaments = teamDB?.currentTournaments
//                        team.currentTournaments.forEach { (_, tourney) ->
                            if (tourney.isActive) {
                                Log.d("WorkerT", "userStepMap for Tourney ${tourney.name}"+ userTournaments[tourney.name]?.dailyStepsMap?.keys+ "Last "+ userTournaments[tourney.name]?.dailyStepsMap?.keys?.last())
                                if (userTournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                        .toString() == midNight.toString()
                                ) {

                                   // Log.d("WorkerT", "userStepMap for Tourney ${tourney.name}"+ tournaments!![tourney.name]?.dailyStepsMap?.keys+ "Last "+ tournaments[tourney.name]?.dailyStepsMap?.keys?.last())
                                    //TODO:replace users daily steps
                                    Log.d("WorkerT", "UserTourney is up to date")

                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                        currentStepCount

                                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                                        Timestamp.now()
                                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                                        currentStepCount
                                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)
                                    //sharedPrefsRepository.user = userTourneys


                                    firestoreRepository.updateUserData(
                                        user.uid,
                                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                                    )
                                        .addOnSuccessListener {
                                            Log.d("WorkerT", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                                            Log.d(
                                                "WorkerT",
                                                "stepmap for ${tourney.name} was uploaded successfully"
                                            )


//                                            firestoreRepository.getTeam(team.name)
//                                                .addOnSuccessListener {
//                                                    val teamDB = it.toObject<Team>()
//                                                    val tournaments = teamDB?.currentTournaments
                                            if (tournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is empty"
                                                )
                                                tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                    currentStepCount

                                                updateAndStoreTeamDataInSharedPrefs(
                                                    tournaments[tourney.name]!!,
                                                    team,
                                                    future,
                                                    cr,
                                                    county
                                                )
                                                //updateUserTeamDataInFirestore(future)
//                                                        sharedPrefsRepository.storeLastDayStepCount(
//                                                            currentStepCount
//                                                        )
                                            } else {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is not empty"
                                                )
                                                if (tournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                        .toString() != midNight.toString()
                                                ) {
                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is not up to date"
                                                    )
                                                    tourney.dailyStepsMap =
                                                        teamDB.currentTournaments[tourney.name]?.dailyStepsMap!!
                                                    tourney.dailyStepsMap[midNight.toString()] =
                                                        currentStepCount

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        tourney,
                                                        team,
                                                        future,
                                                        cr,
                                                        county
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                } else {
                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is up to date"
                                                    )
                                                    val oldStep =
                                                        tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()]

                                                    Log.d("WorkerT", "OldStep for tourney: ${tourney.name} " + oldStep)
                                                    Log.d("WorkerT", "Last Day step count "+ sharedPrefsRepository.getLastDayStepCount())
                                                    val diff =
                                                        currentStepCount - sharedPrefsRepository.getLastDayStepCount()
                                                    Log.d("WorkerT", "Diff3 for tourney: ${tourney.name} " + diff)
                                                    val updatedSteps = oldStep!! + diff

                                                    if (diff > 0) {
                                                        tourney.dailyStepsMap[midNight.toString()] =
                                                            updatedSteps
                                                        updateAndStoreTeamDataInSharedPrefs(
                                                            tourney,
                                                            team,
                                                            future,
                                                            cr,
                                                            county
                                                        )
                                                        //updateUserTeamDataInFirestore(future)
//                                                                sharedPrefsRepository.storeLastDayStepCount(
//                                                                    currentStepCount
//                                                                )
                                                    }
//                                                            else if(diff == 0) future.set(
//                                                                ListenableWorker.Result.success())
                                                }
                                            }
                                            //}
                                        }
                                } else {
                                    Log.d("WorkerT", "UserTourney is not up to date")

//                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap =
//                                        userTourney.dailyStepsMap
                                    userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                        currentStepCount

                                    userTourneys.currentTournaments[tourney.name]?.lastUpdateTime =
                                        Timestamp.now()
                                    userTourneys.currentTournaments[tourney.name]?.totalSteps =
                                        currentStepCount

                                    updateAndStoreUserDataInSharedPrefs(userTourneys.currentTournaments[tourney.name]!!)

                                    //sharedPrefsRepository.user = userTourneys


                                    firestoreRepository.updateUserData(
                                        user.uid,
                                        mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments)
                                    )
                                        .addOnSuccessListener {
                                            Log.d("WorkerT", "user stepMap before Upload for tourney: ${tourney.name}" + userTourneys.currentTournaments)
                                            Log.d(
                                                "WorkerT",
                                                "stepmap for ${tourney.name} was uploaded successfully"
                                            )

//                                            firestoreRepository.getTeam(team.name)
//                                                .addOnSuccessListener {
//                                                    val teamDB = it.toObject<Team>()
//                                                    val tournaments = teamDB?.currentTournaments

//                                            userTourneys.currentTournaments[tourney.name]?.dailyStepsMap = userTourney.dailyStepsMap
//                                            userTourneys.currentTournaments[tourney.name]?.dailyStepsMap!![DateTime().withTimeAtStartOfDay().millis.toString()] = currentStepCount
                                            //TODO: Update user Tourney
                                            if (tournaments!![tourney.name]?.dailyStepsMap?.isEmpty()!!) {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is empty"
                                                )
                                                tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] =
                                                    currentStepCount

                                                updateAndStoreTeamDataInSharedPrefs(
                                                    tournaments[tourney.name]!!,
                                                    team,
                                                    future,
                                                    cr,
                                                    county
                                                )
                                                //updateUserTeamDataInFirestore(future)
//                                                        sharedPrefsRepository.storeLastDayStepCount(
//                                                            currentStepCount
//                                                        )
                                            } else {
                                                Log.d(
                                                    "WorkerT",
                                                    "Team tourney step map is not empty"
                                                )
                                                if (tournaments[tourney.name]?.dailyStepsMap?.keys?.sorted()?.last()
                                                        .toString() != midNight.toString()
                                                ) {
                                                    //Data is present in team tourney
                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is not up to date"
                                                    )

                                                    tourney.dailyStepsMap =
                                                        teamDB.currentTournaments[tourney.name]?.dailyStepsMap!!
                                                    tourney.dailyStepsMap[midNight.toString()] =
                                                        currentStepCount

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        tourney,
                                                        team,
                                                        future,
                                                        cr,
                                                        county
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                } else {
                                                    //Data is not present in TeamTourney

                                                    Log.d(
                                                        "WorkerT",
                                                        "Team tourney is up to date"
                                                    )
                                                    val oldStep =
                                                        tournaments[tourney.name]?.dailyStepsMap!![midNight.toString()]
                                                    val updatedSteps = oldStep!! + currentStepCount
                                                    team.currentTournaments[tourney.name]?.dailyStepsMap!![midNight.toString()] = updatedSteps

                                                    updateAndStoreTeamDataInSharedPrefs(
                                                        team.currentTournaments[tourney.name]!!,
                                                        team,
                                                        future,
                                                        cr,
                                                        county
                                                    )
                                                    //updateUserTeamDataInFirestore(future)
//                                                            sharedPrefsRepository.storeLastDayStepCount(
//                                                                currentStepCount
//                                                            )
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
                // }
            }
    }

    private fun updateAndStoreTeamDataInSharedPrefs(teamTourney: TeamTournament, team: Team, future:SettableFuture<Result>, cr:Int, county: Int) {
        teamTourney.leafCount = getTotalLeafCountForTeam(teamTourney)
        teamTourney.fruitCount = getTotalFruitCountForTeam(teamTourney)
        teamTourney.tournamentGoalStreak = getTeamGoalStreakForUser(teamTourney, team)
        teamTourney.lastUpdateTime = Timestamp.now()
        teamTourney.dailyGoalsAchieved = calculateDailyGoalsAchieved(teamTourney)
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
        updateUserTeamDataInFirestore(future, cr, county)
    }

    private fun updateUserTeamDataInFirestore(future: SettableFuture<Result>, cr1:Int, county1: Int) {
        if (cr1 == county1) {
            Log.d("WorkerT", "updateUserTeamDataInFirestore")
            Log.d("WorkerT", "Pref " + sharedPrefsRepository.team)
            Log.d("WorkerT", "PrefValue " + sharedPrefsRepository.team.currentTournaments)
            Log.d("WorkerT", "TeamName " + sharedPrefsRepository.team.name)
            firestoreRepository.updateTeamData(
                sharedPrefsRepository.team.name,
                mapOf("currentTournaments" to sharedPrefsRepository.team.currentTournaments))
                .addOnSuccessListener {
                    Log.d("WorkerT", "Team User data upload success")
                    Log.d("WorkerT", "cr: " + cr1 + "county: " + county1)

                    stepCountRepository.getTodayStepCountData {
                        sharedPrefsRepository.storeLastDayStepCount(it)
                    }
                    future.set(Result.success())
                }
                .addOnFailureListener {
                    Log.e("WorkerT", "Team User data upload failed")
                    future.set(Result.failure())
                }
        }
    }

//    private fun updateUserTournamentDataInFirestore() {
//        Log.d("WorkerTournament", "updateUserTournamentDataInFirestore")
//        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid,
//            mapOf("currentTournaments" to sharedPrefsRepository.user.currentTournaments))
//            .addOnSuccessListener {
//                Log.d("WorkerTournament", "TUser data upload success")
//            }
//            .addOnFailureListener {
//                Log.e("WorkerTournament", "TUser data upload failed")
//            }
//    }

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
            Log.d("WorkerT", "currentDay "+ currentDay)
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

    private fun updateAndStoreUserDataInSharedPrefs(userTourney: UserTournament){

        synchronized(sharedPrefsRepository.user){
            val user = sharedPrefsRepository.user
            user.currentTournaments[userTourney.name] = userTourney
            sharedPrefsRepository.user = user
        }
        // updateUserTourneyDataInFirestore()
    }

    fun calculateDailyGoalsAchieved(teamTournament: TeamTournament): Int{

        Log.d("WorkerT","getTotalLeafCountForTeam")
        val stepsMap = teamTournament.dailyStepsMap
        val goal = teamTournament.goal
        var dailyGoalsAchieved = 0

        val keys = stepsMap.keys.sortedBy {
            it.toLong()
        }

        for (i in 0 until keys.size-1) {
            //for (i in 0 until keys.size) {
            Log.d("WorkerT","DailyGoalStepMap "+ stepsMap[keys[i]])
            dailyGoalsAchieved += calculateDailyGoalsAchievedFromStepCountForTeam(stepsMap[keys[i]]!!, goal)
        }

        dailyGoalsAchieved += if(stepsMap[keys[keys.size-1]]!! > goal) 1 else 0

        return dailyGoalsAchieved
    }
}