package dal.mitacsgri.treecare.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import data.User
import org.joda.time.DateTime
import java.util.*

class MainViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository,
    private val firestoreRepository: FirestoreRepository
    ) : ViewModel() {

    private lateinit var mClient: GoogleApiClient
    private var authInProgress = false
    private var RC_SIGN_IN = 1000

    //This variable is accessed synchronously. The moment its value reaches 2, we move to new fragment
    //Value 2 means both the steps counts have been obtained
    val stepCountDataFetchedCounter = MutableLiveData<Int>().default(0)
    val userFirstName =  MutableLiveData<String>()

    var hasInstructionsDisplayed
        set(value) {
            sharedPrefsRepository.hasInstructionsDisplayed = value
        }
        get() = sharedPrefsRepository.hasInstructionsDisplayed

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

    fun startLoginAndConfiguration(activity: Activity) {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build())

        // Create and launch sign-in intent
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    fun performLogin(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (requestCode == RC_SIGN_IN) {
            authInProgress = false
            if (resultCode == Activity.RESULT_OK) {

                val user = FirebaseAuth.getInstance().currentUser

                //Store user data if user does not exist
                user?.let {
                    userFirstName.value = user.displayName?.let {
                        it.split(" ")[0]
                    }

                    checkIfUserExists(user.uid, {
                        firstLoginTime = it.firstLoginTime
                        sharedPrefsRepository.isFirstRun = false
                        performFitnessApiConfiguration(activity, user.email)
                    }) {
                        sharedPrefsRepository.isFirstRun = true
                        performFitnessApiConfiguration(activity, user.email)
                        return@checkIfUserExists User(
                            uid = user.uid,
                            isFirstRun = false,
                            name = user.displayName!!,
                            firstLoginTime = DateTime().millis,
                            email = user.email!!)
                    }

                    Log.d("User: ", userFirstName.toString())
                }

                lastLoginTime = Date().time

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED")
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth")
        }
    }


    private fun performFitnessApiConfiguration(activity: Activity, accountName: String?) {
        mClient = GoogleApiClient.Builder(activity)
            .addApi(Fitness.RECORDING_API)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .setAccountName(accountName)
            .addConnectionCallbacks(connectionCallbacksImpl)
            .addOnConnectionFailedListener {
                Log.d("Connection failed: ", it.toString())
            }.build()

        if (!mClient.isConnecting && !mClient.isConnected) {
            mClient.connect()
        }
    }

    private fun subscribeToRecordSteps(setLoginProcessDone : () -> Unit) {
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    if (status.statusCode == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                        Log.i("recording", "Existing subscription for activity detected.")
                    } else {
                        Log.i("recording", "Successfully subscribed!")
                    }
                    setLoginProcessDone()
                } else {
                    Log.w("recording", "There was a problem subscribing.")
                }
            }
    }

    private val connectionCallbacksImpl = object: GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {

            subscribeToRecordSteps {
                sharedPrefsRepository.isLoginDone = true
            }

            stepCountRepository.apply {
                if (sharedPrefsRepository.isFirstRun) {
                    getTodayStepCountData(mClient) {
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
                        it.forEach { (_, stepCount) ->
                            totalLeafCountTillLastDay +=
                                calculateLeafCountFromStepCount(stepCount,
                                    sharedPrefsRepository.getDailyStepsGoal())
                        }
                        sharedPrefsRepository.lastLeafCount = totalLeafCountTillLastDay
                        increaseStepCountDataFetchedCounter()

                        var currentLeafCount = totalLeafCountTillLastDay
                        //Add today's leaf count to leafCountTillLastDay
                        //Call needs to be made here because it uses data from previous call
                        getTodayStepCountData(mClient) {
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

        override fun onConnectionSuspended(p0: Int) {}
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
                    val user = it.toObject<User>() as User
                    userExistsAction(user)
                    sharedPrefsRepository.user = user
                }
                else {
                    val user = userDoesNotExistAction()
                    firestoreRepository.storeUser(user)
                    Log.e("USER", it.toString())
                    sharedPrefsRepository.user = user
                }
            }
            .addOnFailureListener {
                Log.e("USER", it.toString())
            }
    }

    private fun makeUserFromFirebaseUser(fUser: FirebaseUser)
            = User(uid = fUser.uid, isFirstRun = false)
}