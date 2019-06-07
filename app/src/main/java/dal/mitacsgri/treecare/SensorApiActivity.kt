package dal.mitacsgri.treecare

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataSource.TYPE_RAW
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_CUMULATIVE
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.gms.fitness.result.DataSourcesResult
import kotlinx.android.synthetic.main.activity_sensor_api.*
import java.util.concurrent.TimeUnit

class SensorApiActivity : AppCompatActivity(),
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    OnDataPointListener {

    private lateinit var mClient: GoogleApiClient
    private var authInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_api)

        mClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)
            .addApi(Fitness.RECORDING_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks (this)
            .addOnConnectionFailedListener (this)
            .build()

        mClient.connect()

        subscribe()
        invokeSensorsAPI()
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

    private fun invokeSensorsAPI() {
//        Fitness.SensorsApi.add(
//            mClient,
//            SensorRequest.Builder()
//                .setDataType(TYPE_STEP_COUNT_DELTA)
//                .setSamplingRate(1, TimeUnit.SECONDS)
//                .build(),
//            this
//        )
//            .setResultCallback {
//                    if (it.isSuccess) {
//                        Log.i("Sensor API", "Sensor Listener registered!")
//                    } else {
//                        Log.i("Sensor API", "Sensor Listener not registered.")
//                    }
//            }
    }

    override fun onConnected(p0: Bundle?) {
        val dataSourceRequest = DataSourcesRequest.Builder()
            .setDataTypes(TYPE_STEP_COUNT_CUMULATIVE)
            .setDataSourceTypes(TYPE_RAW)
            .build()

        val dataSourcesResultCallback =
            ResultCallback<DataSourcesResult> { dataSourcesResult ->
                for (dataSource in dataSourcesResult.dataSources) {
                    if (TYPE_STEP_COUNT_CUMULATIVE == dataSource.dataType) {
                        registerFitnessDataListener(dataSource, TYPE_STEP_COUNT_CUMULATIVE)
                    }
                }
            }

        Fitness.SensorsApi.findDataSources(mClient, dataSourceRequest)
            .setResultCallback {
                for (dataSource in it.dataSources) {
                if (TYPE_STEP_COUNT_CUMULATIVE == dataSource.dataType) {
                    registerFitnessDataListener(dataSource, TYPE_STEP_COUNT_CUMULATIVE)
                }
            }
            }
    }

    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {

        val request = SensorRequest.Builder()
            .setDataSource(dataSource)
            .setDataType(dataType)
            .setSamplingRate(1, TimeUnit.SECONDS)
            .build()

        Fitness.SensorsApi.add(mClient, request, this)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    Log.e("GoogleFit", "SensorApi successfully added")
                }
            }
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        if (!authInProgress) {
            try {
                authInProgress = true
                p0.startResolutionForResult(this, 1000)
            } catch (e: IntentSender.SendIntentException) {

            }

        } else {
            Log.e("GoogleFit", "authInProgress")
        }
    }

    override fun onDataPoint(dataPoint : DataPoint) {
        for (field in dataPoint.dataType.fields) {
            val value = dataPoint.getValue(field)
            Log.i("datapoint", "Detected DataPoint field: " + field.name)
            Log.i("datapoint", "Detected DataPoint value: $value")
            val value1 = value.asInt()

            if (field.name.equals("steps", true)) {
                runOnUiThread {
                    run {
                        tv.text = "Value $value1"
                    }
                }
            }

        }

    }

    fun subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
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
}
