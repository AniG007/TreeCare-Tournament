package dal.mitacsgri.treecare.screens.login

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataType
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.toast
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
                "Login cancelled".toast(this)
            }
        } else {
            "Invalid code".toast(this)
        }
    }

    private fun startGoogleFitApiConfiguration() {
        mClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)
            .addApi(Fitness.RECORDING_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(connectionCallbacksImpl)
            .addOnConnectionFailedListener {connectionFailed}
            .build()

        mClient.connect()

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

    private val connectionFailed = GoogleApiClient.OnConnectionFailedListener {
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
//            val dataSourceRequest = DataSourcesRequest.Builder()
//                .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
//                .setDataSourceTypes(DataSource.TYPE_RAW)
//                .build()
//
//            Fitness.SensorsApi.findDataSources(mClient, dataSourceRequest)
//                .setResultCallback {
//                    for (dataSource in it.dataSources) {
//                        if (DataType.TYPE_STEP_COUNT_CUMULATIVE == dataSource.dataType) {
//                            registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_CUMULATIVE)
//                        }
//                    }
//                }
        }

        override fun onConnectionSuspended(p0: Int) {}
    }

//    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {
//
//        val request = SensorRequest.Builder()
//            .setDataSource(dataSource)
//            .setDataType(dataType)
//            .setSamplingRate(1, TimeUnit.SECONDS)
//            .build()
//
//        Fitness.SensorsApi.add(mClient, request) {
//            for (field in it.dataType.fields) {
//                val value = it.getValue(field)
//                Log.i("datapoint", "Detected DataPoint field: " + field.name)
//                Log.i("datapoint", "Detected DataPoint value: $value")
//                val value1 = value.asInt()
//
//                if (field.name.equals("steps", true)) {
//                    runOnUiThread {
//                        run {
//                            tv.text = "Value $value1"
//                        }
//                    }
//                }
//
//            }
//        }.setResultCallback { status ->
//                if (status.isSuccess) {
//                    Log.e("GoogleFit", "SensorApi successfully added")
//                }
//            }
//    }
}
