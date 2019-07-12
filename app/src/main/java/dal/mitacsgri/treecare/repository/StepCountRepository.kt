package dal.mitacsgri.treecare.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.TimeUnit

class StepCountRepository(private val context: Context) {

    fun getTodayStepCountData(onDataObtained: (stepCount: Int) -> Unit) {

        var total = 0
        doAsync {

            val response = Fitness.getHistoryClient(context,
                GoogleSignIn.getLastSignedInAccount(context)!!)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)

            response.addOnSuccessListener {
                total = it.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                Log.d("DailyStepCount", total.toString())
                onDataObtained(total)
            }.addOnFailureListener {
                Log.e("DailyStepCount", "error: $it")
                onDataObtained(total)
            }
        }
    }

    fun getLastDayStepCountData(mClient: GoogleApiClient, onDataObtained: (stepCount: Int) -> Unit) {
        val cal = Calendar.getInstance()
        val now = Date()
        cal.apply {
            time = now
            set(Calendar.MILLISECOND, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.HOUR, 0)
        }

        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        context.doAsync {
            val result = Fitness.HistoryApi.readData(mClient, readRequest).await(30, TimeUnit.SECONDS)
            var lastDayStepCount = 0

            if (result.status.isSuccess) {
                for (bucket in result.buckets) {
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            //Log.d("Datapoint", dataPoint.toString())
                            lastDayStepCount += dataPoint.getValue(dataPoint.dataType.fields[0]).asInt()
                        }
                    }

                    uiThread {
                        onDataObtained(lastDayStepCount)
                    }
                }
            }
        }
    }

    fun getStepCountDataOverARange(startTime: Long, endTime: Long, onDataObtained: (stepCount: Map<Long, Int>) -> Unit) {

        //This statement prevents running the following code when the app is being used for the first day
        //Otherwise the app will crash
        if (endTime <= startTime) {
            onDataObtained(mutableMapOf())
            return
        }

        val stepCountMap = mutableMapOf<Long, Int>()

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener {
                for (bucket in it.buckets) {
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            Log.d("Datapoint", dataPoint.toString())
                            val stepCount = dataPoint.getValue(dataPoint.dataType.fields[0]).asInt()
                            val dpStartTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                            stepCountMap[DateTime(dpStartTime).withTimeAtStartOfDay().millis]= stepCount
                        }
                    }
                }

                onDataObtained(stepCountMap)
                Log.d("Step count map", stepCountMap.toString())
            }
    }

    fun getAggregateStepCountDataOverARange(startTime: Long, endTime: Long, onDataObtained: (stepCount: Int) -> Unit) {
        var aggregateStepCount = 0

        //This statement prevents running the following code when the app is being used for the first day
        //Otherwise the app will crash
        if (endTime <= startTime) {
            onDataObtained(aggregateStepCount)
            return
        }

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.HOURS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readData(readRequest)
            .addOnSuccessListener {
                for (bucket in it.buckets) {
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            Log.d("Datapoint", dataPoint.toString())
                            aggregateStepCount += dataPoint.getValue(dataPoint.dataType.fields[0]).asInt()
                        }
                    }
                }

                onDataObtained(aggregateStepCount)
                Log.d("Aggregate step count", aggregateStepCount.toString())
            }
    }
}