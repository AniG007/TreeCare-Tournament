package dal.mitacsgri.treecare.provider

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*
import java.util.concurrent.TimeUnit

class StepCountRepository(private val context: Context) {

    private val TAG = "DailyStepCount"

    fun getTodayStepCountData(mClient: GoogleApiClient, onDataObtained: (stepCount: Int) -> Unit) {

        context.doAsync {
            var total = 0

            val result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA)
            val totalResult = result.await(1, TimeUnit.SECONDS)
            if (totalResult.status.isSuccess) {
                val totalSet = totalResult.total
                total = (if (totalSet!!.isEmpty) 0
                else totalSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt())
            } else {
                Log.w(TAG, "There was a problem getting the step count.")
            }

            uiThread {
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
                Log.d("Buckets: ", result.buckets.toString())
                for (bucket in result.buckets) {
                    Log.d("Datasets: ", bucket.dataSets.toString())
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            Log.d("Datapoint", dataPoint.toString())
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
}