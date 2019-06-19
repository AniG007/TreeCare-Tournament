package dal.mitacsgri.treecare.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataType
import dal.mitacsgri.treecare.provider.SharedPreferencesRepository
import dal.mitacsgri.treecare.provider.StepCountRepository

class LoginViewModel(
    sharedPrefRepository: SharedPreferencesRepository,
    stepCountRepository: StepCountRepository
    ) : ViewModel() {

    private lateinit var mClient: GoogleApiClient

    var authInProgress = false
    //Creating MutableLiveData with a default value
    val loginStatus = MutableLiveData<Boolean>().apply { value = false }

    fun performLogin(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1000) {
            authInProgress = false
            if (resultCode == Activity.RESULT_OK) {
                if (!mClient.isConnecting && !mClient.isConnected) {
                    mClient.connect()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED")
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth")
        }
    }

    fun startGoogleFitApiConfiguration(context: Context) {
        mClient = GoogleApiClient.Builder(context)
            .addApi(Fitness.RECORDING_API)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(connectionCallbacksImpl)
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()
    }

    private fun subscribeToRecordSteps(setLoginProcessDone : () -> Unit) {
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    if (status.statusCode == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                        Log.i("recording", "Existing subscription for activity detected.")
                        setLoginProcessDone()
                    } else {
                        Log.i("recording", "Successfully subscribed!")
                    }
                } else {
                    Log.w("recording", "There was a problem subscribing.")
                }
            }
    }

    private val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
        Log.e("Login failed: ", it.errorMessage)
    }

    private val connectionCallbacksImpl = object: GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {

            subscribeToRecordSteps {
                loginStatus.value = true
                sharedPrefRepository.isLoginDone = loginStatus.value ?: true
            }

            stepCountRepository.apply {
                getTodayStepCountData(mClient) {
                    sharedPrefRepository.storeDailyStepCount(it)
                    sharedPrefRepository.isLoginDone = true
                    sharedPrefRepository.isLoginDone = loginStatus.value ?: true
                    //startNextActivity(ModeSelectionActivity::class.java)
                }

                getLastDayStepCountData(mClient) {
                    sharedPrefRepository.storeLastDayStepCount(it)
                }
            }

        }

        override fun onConnectionSuspended(p0: Int) {}
    }

}