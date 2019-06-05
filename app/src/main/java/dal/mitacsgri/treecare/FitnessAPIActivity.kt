package dal.mitacsgri.treecare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class FitnessAPIActivity : AppCompatActivity() {

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 4
        const val LOG_TAG = "fitnessapilog"
    }

    lateinit var client: GoogleApiClient

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
//            accessGoogleFit()
            displayStepDataForToday()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                accessGoogleFit()
                displayStepDataForToday()
            }
        }
    }

    private fun accessGoogleFit() {
        val cal = Calendar.getInstance()
        cal.time = Date()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val startTime = cal.timeInMillis - 10000000000

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
            .readData(readRequest)
            .addOnSuccessListener { Log.d(LOG_TAG, "${it.buckets}") }
            .addOnFailureListener { e -> Log.e(LOG_TAG, "onFailure()", e) }
            .addOnCompleteListener { Log.d(LOG_TAG, "onComplete()") }
    }

    private fun displayStepDataForToday() {

        client = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .build()

        val result = Fitness.HistoryApi.readDailyTotal(client, DataType.TYPE_STEP_COUNT_DELTA)
            .await(1, TimeUnit.MINUTES)
        showDataSet(result.total!!)
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
