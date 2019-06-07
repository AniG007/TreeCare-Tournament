package dal.mitacsgri.treecare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import kotlinx.android.synthetic.main.activity_fitness_api.*
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class FitnessAPIActivity : AppCompatActivity(),
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    OnDataPointListener {

    private var mGoogleApiClient: GoogleApiClient? = null

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 4
        const val LOG_TAG = "fitnessapilog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fitness_api)

        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions)
        } else {
            accessGoogleFit()
        }

        // Create a Google Fit Client instance with default user account.
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)  // Required for SensorsApi calls
            .addApi(Fitness.HISTORY_API)
            .useDefaultAccount()
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        mGoogleApiClient?.connect()
        //displayStepDataForToday()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (resultCode == Activity.RESULT_OK) {
//            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
//                accessGoogleFit()
//                displayStepDataForToday()
//            }
//        }
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            mGoogleApiClient?.connect()
        }
    }

    override fun onConnected(connectionHint: Bundle?) {
        // Connected to Google Fit Client.
        Fitness.SensorsApi.add(
            mGoogleApiClient,
            SensorRequest.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build(),
            this
        )
        displayStepDataForToday()
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        // Error while connecting. Try to resolve using the pending intent returned.
        if (result.errorCode == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {

                result.startResolutionForResult(this, 1001)
        }    }

    override fun onDataPoint(p0: DataPoint?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun accessGoogleFit() {
        val cal = Calendar.getInstance()
        cal.time = Date()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val startTime = cal.timeInMillis - TimeUnit.DAYS.toMillis(1)

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()

        var steps = 0

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
            .readData(readRequest)
            .addOnSuccessListener {
                Log.d(LOG_TAG, "${it.buckets.size}")
                it.buckets.forEach {
                    it.dataSets.forEach {
                        it.dataPoints.forEach {
                            steps += it.getValue(Field.FIELD_STEPS).asInt()
                            Log.d("steps: ", it.getValue(Field.FIELD_STEPS).asInt().toString())
                        }
                    }
                }
                tvStepsCount.text = steps.toString()
            }
            .addOnFailureListener { e -> Log.e(LOG_TAG, "onFailure()", e) }
            .addOnCompleteListener { Log.d(LOG_TAG, "onComplete()") }

    }

    private fun displayStepDataForToday() {

        thread {
            val result = Fitness.HistoryApi.readDailyTotal(mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback {
                    showDataSet(it.total!!)
                }

        }
    }

    private fun showDataSet(dataSet: DataSet) {
        val dateFormat = DateFormat.getDateInstance()
        val timeFormat = DateFormat.getTimeInstance()
        for (dp in dataSet.dataPoints) {
            Log.e("History", "Data point:")
            Log.e("History", "\tType: " + dp.dataType.name)
            Log.e(
                "History",
                "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(
                    dp.getStartTime(TimeUnit.MILLISECONDS)
                )
            )
            Log.e(
                "History",
                "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(
                    dp.getStartTime(TimeUnit.MILLISECONDS)
                )
            )
            for (field in dp.dataType.fields) {
                Log.e(
                    "History", "\tField: " + field.name +
                            " Value: " + dp.getValue(field)
                )
            }
        }
    }
}
