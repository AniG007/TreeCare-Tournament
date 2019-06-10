package dal.mitacsgri.treecare.screens.login

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataType
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.provider.DailyStepCountProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mClient: GoogleApiClient
    private var authInProgress = false

    val SIGN_IN_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray)

        signInButton.setOnClickListener {
            startGoogleFitApiConfiguration()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    private fun startGoogleFitApiConfiguration() {
        mClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)
            .addApi(Fitness.RECORDING_API)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(connectionCallbacksImpl)
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()

        GoogleSignIn.getLastSignedInAccount(this)?.toast(this)

    }

    private fun subscribeToRecordSteps() {
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    if (status.statusCode == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                        Log.i("recording", "Existing subscription for activity detected.")
                    } else {
                        Log.i("recording", "Successfully subscribed!")
                    }
                } else {
                    Log.w("recording", "There was a problem subscribing.")
                }
            }
    }

    private val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
        if (!authInProgress) {
            try {
                authInProgress = true
                it.startResolutionForResult(this, SIGN_IN_CODE)
            } catch (e: IntentSender.SendIntentException) {

            }
        } else {
            "Logging you in".toast(this)
        }
    }

    private val connectionCallbacksImpl = object: GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(p0: Bundle?) {

            Log.d("Bundle", p0.toString())
            subscribeToRecordSteps()
            DailyStepCountProvider(this@LoginActivity, mClient)
                .updateUiWithStepCount {
                    tvStepCount.text = it.toString()
                }
        }

        override fun onConnectionSuspended(p0: Int) {}
    }
}