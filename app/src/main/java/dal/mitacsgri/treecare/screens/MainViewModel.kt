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
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import java.util.*

class MainViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository
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
                userFirstName.value = user?.displayName?.let {
                    it.split(" ")[0]
                }
                performFitnessApiConfiguration(activity, user?.email)
                Log.d("User: ", userFirstName.toString())

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

            stepCountRepository.apply {

                if (sharedPrefsRepository.isFirstRun) {
                    getTodayStepCountData(mClient) {
                        sharedPrefsRepository.storeDailyStepCount(it)
                        Log.d("DailyStepCount", it.toString())
                        sharedPrefsRepository.currentLeafCount =
                            calculateLeafCountFromStepCount(it, 5000)
                        increaseStepCountDataFetchedCounter()
                        increaseStepCountDataFetchedCounter()
                    }
                    sharedPrefsRepository.isFirstRun = false
                    sharedPrefsRepository.lastLeafCount = 0
                } else {
                    getTodayStepCountData(mClient) {
                        sharedPrefsRepository.storeDailyStepCount(it)
                        Log.d("DailyStepCount", it.toString())
                        increaseStepCountDataFetchedCounter()
                    }

                    //Get aggregate step count up to the last day
                    getStepCountDataOverARange(mClient,
                        sharedPrefsRepository.lastLoginTime,
                        DateTime().withTimeAtStartOfDay().millis) {

                        sharedPrefsRepository.lastLeafCount =
                            calculateLeafCountFromStepCount(it, 5000)
                    }

                    //Get aggregate leaf count up to today
                    getStepCountDataOverARange(mClient,
                        sharedPrefsRepository.lastLoginTime,
                        DateTime().plusDays(1).withTimeAtStartOfDay().millis) {

                        val leafCount = calculateLeafCountFromStepCount(it, 5000)
                        sharedPrefsRepository.currentLeafCount = leafCount
                        Log.d("Current leaf count", leafCount.toString())
                    }
                }
            }

            subscribeToRecordSteps {
                sharedPrefsRepository.isLoginDone = true
            }
        }

        override fun onConnectionSuspended(p0: Int) {}
    }

    private inline fun increaseStepCountDataFetchedCounter() {
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
}