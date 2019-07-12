package dal.mitacsgri.treecare.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.consts.CHALLENGER_MODE
import dal.mitacsgri.treecare.consts.STARTER_MODE
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import dal.mitacsgri.treecare.screens.dialog.logindataloading.LoginDataLoadingDialog
import org.joda.time.DateTime
import org.joda.time.Days
import java.util.*

class MainViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository,
    private val firestoreRepository: FirestoreRepository
    ) : ViewModel() {

    private lateinit var mClient: GoogleApiClient
    private lateinit var mAccount: GoogleSignInAccount
    private val loadingDialog = LoginDataLoadingDialog()
    private lateinit var mActivity: Activity

    private var RC_SIGN_IN = 1000
    private val RC_GOOGLE_FIT_PERMISSIONS = 4

    //This variable is accessed synchronously. The moment its value reaches 2, we move to new fragment
    //Value 2 means both the steps counts have been obtained
    val stepCountDataFetchedCounter = MutableLiveData<Int>().default(0)
    val userFirstName =  MutableLiveData<String>()

    var firstLoginTime: Long
        set(value) {
            sharedPrefsRepository.firstLoginTime = value
        }
        get() = sharedPrefsRepository.firstLoginTime

    var lastLoginTime: Long
        set(value) {
            sharedPrefsRepository.lastLoginTime = value
        }
        get() = sharedPrefsRepository.lastLoginTime

    var lastLogoutTime: Long
        set(value) {
        sharedPrefsRepository.lastLogoutTime = value
        }
        get() = sharedPrefsRepository.lastLogoutTime

    fun hasInstructionsDisplayed(mode: Int) =
            when(mode) {
                STARTER_MODE -> sharedPrefsRepository.starterModeInstructionsDisplayed
                CHALLENGER_MODE -> sharedPrefsRepository.challengerModeInstructionsDisplayed
                TOURNAMENT_MODE -> sharedPrefsRepository.tournamentModeInstructionsDisplayed
                else -> true
            }

    fun setInstructionsDisplayed(mode: Int, value: Boolean) {
        when(mode) {
            STARTER_MODE -> sharedPrefsRepository.starterModeInstructionsDisplayed = value
            CHALLENGER_MODE -> sharedPrefsRepository.challengerModeInstructionsDisplayed = value
            TOURNAMENT_MODE -> sharedPrefsRepository.tournamentModeInstructionsDisplayed = value
        }
    }

    fun setGameMode(mode: Int) {
        sharedPrefsRepository.gameMode = mode
    }

    fun startLoginAndConfiguration(activity: FragmentActivity) {

        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("628888141862-lmblquvs5s3gl9rmshvag3sin348kaam.apps.googleusercontent.com"/*Web application type client ID*/)
            .requestEmail()
            .build()

        val mGoogleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = mGoogleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun onSignInResult(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                RC_SIGN_IN -> {

                    GoogleSignIn.getSignedInAccountFromIntent(data)
                        .addOnSuccessListener {
                            mAccount = GoogleSignIn.getLastSignedInAccount(activity)!!
                            mActivity = activity
                            //accessFitApi(task)

                            val fitnessOptions = FitnessOptions.builder()
                                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_WRITE)
                                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                                .build()

                            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(mActivity), fitnessOptions)) {
                                GoogleSignIn.requestPermissions(
                                    mActivity, // your activity
                                    RC_GOOGLE_FIT_PERMISSIONS,
                                    GoogleSignIn.getLastSignedInAccount(mActivity),
                                    fitnessOptions)
                            } else {
                                Log.d("FitAPI", "permissions exist")
                                accessFitApi()
                            }

                        }

        /*//                val user = FirebaseAuth.getInstance().currentUser
        //
        //                userFirstName.value = user?.displayName?.split(" ")?.get(0)
        //
        //                //Store user dal.mitacsgri.treecare.data if user does not exist
        //                user?.let {
        //
        //                    checkIfUserExists(user.uid, {
        //                        firstLoginTime = it.firstLoginTime
        //                        sharedPrefsRepository.isFirstRun = false
        //                        performFitnessApiConfiguration(activity, user.email)
        //                        expandDailyGoalMapIfNeeded(it)
        //                    }) {
        //                        sharedPrefsRepository.isFirstRun = true
        //                        performFitnessApiConfiguration(activity, user.email)
        //                        return@checkIfUserExists User(
        //                            uid = user.uid,
        //                            isFirstRun = true,
        //                            name = user.displayName!!,
        //                            firstLoginTime = DateTime().millis,
        //                            email = user.email!!,
        //                            photoUrl = user.photoUrl.toString())
        //                    }
        //
        //                    Log.d("User: ", userFirstName.toString())
        //                }
                         */

                    lastLoginTime = Date().time

                }
                RC_GOOGLE_FIT_PERMISSIONS -> {
                    Log.d("FitAPI", "permissions do not")
                    accessFitApi()
                }
            }
        } else {
            Log.e("TreeCare", "RESULT_CANCELED")
        }
    }

    private fun accessFitApi() {
        subscribeToRecordSteps {
            //stepCountRepository.getTodayStepCountData {  }
            stepCountRepository.getStepCountDataOverARange(
                GoogleApiClient.Builder(mActivity)
                    .addApi(Fitness.HISTORY_API).build(),
                DateTime().minusDays(7).withTimeAtStartOfDay().millis,
                DateTime().millis
            ) {

            }

            stepCountRepository.getTodayStepCountData {

            }
        }
    }

    private fun subscribeToRecordSteps(action: () -> Unit) {

        val TAG = "RecordingAPI"

        Fitness.getRecordingClient(mActivity, mAccount).subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addOnSuccessListener {
                Log.d(TAG, "success")
                action()
            }
            .addOnFailureListener {
                Log.d(TAG, "failure: $it")
            }

        Fitness.getRecordingClient(mActivity, mAccount).subscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.d(TAG, "success")
                action()
            }
            .addOnFailureListener {
                Log.d(TAG, "failure: $it")
            }
    }

    private val connectionCallbacksImpl = object: GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {

            stepCountRepository.apply {
                if (sharedPrefsRepository.isFirstRun) {
                    getTodayStepCountData {
                        sharedPrefsRepository.storeDailyStepCount(it)
                        Log.d("DailyStepCount", it.toString())
                        sharedPrefsRepository.currentLeafCount = it / 1000
                        increaseStepCountDataFetchedCounter()
                        increaseStepCountDataFetchedCounter()
                    }
                    sharedPrefsRepository.lastLeafCount = 0
                } else {
                    //Get aggregate step count up to the last day + current day step count
                    getStepCountDataOverARange(mClient,
                        DateTime(sharedPrefsRepository.firstLoginTime).withTimeAtStartOfDay().millis,
                        DateTime().withTimeAtStartOfDay().millis) {

                        var totalLeafCountTillLastDay = 0
                        it.forEach { (date, stepCount) ->
                            val goal = sharedPrefsRepository.user.dailyGoalMap[date.toString()]
                            totalLeafCountTillLastDay +=
                                calculateLeafCountFromStepCount(stepCount, goal!!)
                        }
                        sharedPrefsRepository.lastLeafCount = totalLeafCountTillLastDay
                        increaseStepCountDataFetchedCounter()

                        var currentLeafCount = totalLeafCountTillLastDay
                        //Add today's leaf count to leafCountTillLastDay
                        //Call needs to be made here because it uses dal.mitacsgri.treecare.data from previous call
                        getTodayStepCountData {
                            currentLeafCount += it / 1000
                            sharedPrefsRepository.currentLeafCount = currentLeafCount
                            sharedPrefsRepository.storeDailyStepCount(it)
                            Log.d("DailyStepCount", it.toString())
                            increaseStepCountDataFetchedCounter()
                        }
                    }
                }
            }
        }

        override fun onConnectionSuspended(p0: Int) {
            Log.d("Suspended", p0.toString())
        }
    }

    private fun increaseStepCountDataFetchedCounter() {
        synchronized(stepCountDataFetchedCounter) {
            stepCountDataFetchedCounter.value = stepCountDataFetchedCounter.value?.plus(1)
            Log.d("Counter value", stepCountDataFetchedCounter.value.toString())
        }
    }

    private fun calculateLeafCountFromStepCount(stepCount: Int, dailyGoal: Int): Int {
        var leafCount = stepCount / 1000
        if (stepCount < dailyGoal) {
            leafCount -= Math.ceil((dailyGoal - stepCount) / 1000.0).toInt()
            if (leafCount < 0) leafCount = 0
        }
        return leafCount
    }

    private inline fun checkIfUserExists(uid: String,
                                  crossinline userExistsAction: (User) -> Unit,
                                  crossinline userDoesNotExistAction: () -> User) {
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {
                if (it.exists()) {
                    val user = it.toObject<User>()!!
                    userExistsAction(user)
                    sharedPrefsRepository.user = user
                    storeDailyGoalInPrefs()
                }
                else {
                    val user = userDoesNotExistAction()
                    firestoreRepository.storeUser(user)
                    Log.e("USER", it.toString())
                    sharedPrefsRepository.user = user
                }
                storeDailyGoalInPrefs()
            }
            .addOnFailureListener {
                Log.e("USER", it.toString())
            }
    }

    private fun expandDailyGoalMapIfNeeded(user: User) {
        val dailyGoalMap = user.dailyGoalMap
        var keysList = mutableListOf<Long>()
        dailyGoalMap.keys.forEach {
            keysList.add(it.toLong())
        }
        keysList = keysList.sorted().toMutableList()

        val lastTime = keysList[keysList.size-1]
        val days = Days.daysBetween(DateTime(lastTime), DateTime()).days

        val oldGoal = dailyGoalMap[lastTime.toString()]

        for (i in 1..days) {
            val key = DateTime(lastTime).plusDays(i).withTimeAtStartOfDay().millis.toString()
            user.dailyGoalMap[key] = oldGoal!!
        }

        sharedPrefsRepository.user = user
    }

    //Update the daily goal stored in SharedPrefs to display in Unity
    //DailyGoalChecked is set to true only by Unity
    private inline fun storeDailyGoalInPrefs() {
        val dailyGoalMap = sharedPrefsRepository.user.dailyGoalMap
        if (sharedPrefsRepository.isDailyGoalChecked == 0) {
            //In reality, the queried entry will never be null as the User has atleast 2 entries by default,
            //i.e., the current day and the next day
            sharedPrefsRepository.storeDailyStepsGoal(
                dailyGoalMap[DateTime().withTimeAtStartOfDay().millis.toString()] ?: 5000)
        }
    }
}