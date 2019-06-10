package dal.mitacsgri.treecare.provider

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.concurrent.TimeUnit

class DailyStepCountProvider(private val context: Context,
                             private val mClient: GoogleApiClient) {

    private val TAG = "DailyStepCount"

    fun stepCountObtained(funToExecute: (stepCount: Long) -> Unit) {

        context.doAsync {
            var total: Long = 0

            val result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA)
            val totalResult = result.await(1, TimeUnit.SECONDS)
            if (totalResult.status.isSuccess) {
                val totalSet = totalResult.total
                total = (if (totalSet!!.isEmpty)
                    0
                else
                    totalSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()).toLong()
            } else {
                Log.w(TAG, "There was a problem getting the step count.")
            }

            Log.i(TAG, "Total steps: $total")
            uiThread {
                funToExecute(total)
            }
        }
    }
}