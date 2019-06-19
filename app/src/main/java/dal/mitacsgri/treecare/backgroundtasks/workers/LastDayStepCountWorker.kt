package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import dal.mitacsgri.treecare.provider.SharedPreferencesRepository
import dal.mitacsgri.treecare.provider.StepCountProvider

class LastDayStepCountWorker(private val appContext: Context, private val workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val stepCountProvider = StepCountProvider(appContext)
        val sharedPrefProvider = SharedPreferencesRepository(appContext)
        var mClient: GoogleApiClient? = null

        mClient = GoogleApiClient.Builder(appContext)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    stepCountProvider.apply {
                        getLastDayStepCountData(mClient!!) {
                            sharedPrefProvider.storeLastDayStepCount(it)
                            Log.d("LastDayStepCount", it.toString())
                        }
                    }
                }

                override fun onConnectionSuspended(p0: Int) {}
            })
            .addOnConnectionFailedListener({})
            .build()
        mClient.connect()

        //TODO: This line here won't help actually to sequentially execute jobs. Move return somewhere else to receive callback
        return Result.success()
    }
}