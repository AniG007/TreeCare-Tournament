package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.screens.gamesettings.SettingsActivity
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import java.util.concurrent.TimeUnit


/**
 * Created by Devansh on 24-06-2019
 */
class TreeCareUnityActivity : UnityPlayerActivity() {

    private val TAG: String = "SensorAPI"
    val listener = OnDataPointListener { dataPoint ->
            Log.d("Datapoint", dataPoint.toString())
            dataPoint.dataType.fields.forEach { field ->
                    ("Sensor value: " + dataPoint.getValue(field)).toast(this@TreeCareUnityActivity)
                Log.d("Sensor value: ", dataPoint.getValue(field).toString())

        }
    }
    //Called from Unity
    fun Launch() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        findFitnessDataSources()
        startListeningSensorData(this)

//        doAsync {
//            for (i in 0 until 10) {
//                uiThread {
//                    "Hello".toast(this@TreeCareUnityActivity)
//                }
//                Thread.sleep(5000)
//            }
//        }
    }

    private fun startListeningSensorData(context: Context) {

        Fitness.getSensorsClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .add(
                SensorRequest.Builder()
                    .setDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .setSamplingRate(10, TimeUnit.SECONDS)
                    .build(), listener)
            .addOnSuccessListener {
                Log.d("Sensor", "registered")
            }
            .addOnFailureListener {
                Log.d("Sensor", "registration failed")
            }
    }

    private fun findFitnessDataSources() {
 // [START find_data_sources]
    // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
        Fitness.getSensorsClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
            .findDataSources(
                DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build())
                .addOnSuccessListener { dataSources ->
                    for (dataSource in dataSources) {
                        Log.i(TAG, "Data source found: $dataSource")
                        Log.i(TAG, "Data Source type: " + dataSource.dataType.name)

                        // Let's register a listener to receive Activity data!
                            Log.i(TAG, "Data source for LOCATION_SAMPLE found!  Registering.")
                            startListeningSensorData(this)                        }
                }
                .addOnFailureListener { e -> Log.e(TAG, "failed", e) }
        // [END find_data_sources]
  }
}