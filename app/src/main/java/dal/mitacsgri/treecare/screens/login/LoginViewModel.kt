package dal.mitacsgri.treecare.screens.login

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
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
import dal.mitacsgri.treecare.provider.SharedPreferencesRepository
import dal.mitacsgri.treecare.provider.StepCountRepository
import java.util.concurrent.CyclicBarrier

class LoginViewModel(
    private val sharedPrefRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository
    ) : ViewModel() {

    private lateinit var mClient: GoogleApiClient
    private var authInProgress = false

    private var RC_SIGN_IN = 1000

    //Use it to wait till all the step count data has been obtained
    val cyclicBarrier = CyclicBarrier(2) {
        hasStepsData.value = true
    }

    val hasStepsData = MutableLiveData<Boolean>().default(false)
    val loginStatus = MutableLiveData<Boolean>().default(false)

    fun performLogin(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (requestCode == RC_SIGN_IN) {
            authInProgress = false
            if (resultCode == Activity.RESULT_OK) {

                val user = FirebaseAuth.getInstance().currentUser

                mClient = GoogleApiClient.Builder(activity)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.HISTORY_API)
                    .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
                    .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .setAccountName(user?.email)
                    .addConnectionCallbacks(connectionCallbacksImpl)
                    .addOnConnectionFailedListener {
                        Log.d("Connection failed: ", it.toString())
                    }
                    .build()
                //if (!mClient.isConnecting && !mClient.isConnected) {
                    mClient.connect()
                    Log.d("User: ", user.toString())
                    Log.d("Login done", "$requestCode")
                //}
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED")
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth")
        }
    }

    fun startGoogleFitApiConfiguration(activity: Activity) {
        mClient = GoogleApiClient.Builder(activity)
            .addApi(Fitness.RECORDING_API)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(connectionCallbacksImpl)
            .addOnConnectionFailedListener {
                if (!authInProgress) {
                    try {
                        authInProgress = true
                        it.startResolutionForResult(activity, RC_SIGN_IN)
                    } catch (e: IntentSender.SendIntentException) {

                    }
                } else {
                }
                Log.e("Login failed: ", "$it")
            }
            .build()
        mClient.connect()
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

//    private val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
//        if (!authInProgress) {
//            try {
//                authInProgress = true
//                it.startResolutionForResult(, SIGN_IN_CODE)
//            } catch (e: IntentSender.SendIntentException) {
//
//            }
//        } else {
//        }
//        Log.e("Login failed: ", "$it")
//    }

    private val connectionCallbacksImpl = object: GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {

            stepCountRepository.apply {
                getTodayStepCountData(mClient) {
                    sharedPrefRepository.storeDailyStepCount(it)
                    //cyclicBarrier.await()
                }

                getLastDayStepCountData(mClient) {
                    sharedPrefRepository.storeLastDayStepCount(it)
                    //cyclicBarrier.await()
                }
            }

            subscribeToRecordSteps {
                setStateAsLoginDone()
            }

        }

        override fun onConnectionSuspended(p0: Int) {}
    }

    fun startLoginAndConfiguration(activity: Activity) {

//        mClient = GoogleApiClient.Builder(activity)
//            .addApi(Fitness.RECORDING_API)
//            .addApi(Fitness.HISTORY_API)
//            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
//            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
//            .addConnectionCallbacks(connectionCallbacksImpl)
//            .addOnConnectionFailedListener {
//                Log.d("Connection failed: ", it.toString())
//            }
//            .build()

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

    fun setStateAsLoginDone() {
        loginStatus.value = true
        sharedPrefRepository.isLoginDone = true
    }

}