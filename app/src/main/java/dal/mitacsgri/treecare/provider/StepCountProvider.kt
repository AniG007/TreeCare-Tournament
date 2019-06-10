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

class StepCountProvider(private val context: Context) {

    private val TAG = "DailyStepCount"

    fun getTodayStepCountData(mClient: GoogleApiClient, onDataObtained: (stepCount: Long) -> Unit) {

        context.doAsync {
            var total: Long = 0

            val result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA)
            val totalResult = result.await(1, TimeUnit.SECONDS)
            if (totalResult.status.isSuccess) {
                val totalSet = totalResult.total
                total = (if (totalSet!!.isEmpty) 0
                else totalSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()).toLong()
            } else {
                Log.w(TAG, "There was a problem getting the step count.")
            }

            Log.i(TAG, "Total steps: $total")
            uiThread {
                onDataObtained(total)
            }
        }
    }

    fun getLastDayStepCountData(mClient: GoogleApiClient, onDataObtained: (stepCount: Long) -> Unit) {
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
                            lastDayStepCount += dataPoint.getValue(dataPoint.dataType.fields[0]).asInt()
                        }
                    }
                }
            }
        }
    }
}