package dal.mitacsgri.treecare.backgroundtasks.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import dal.mitacsgri.treecare.provider.SharedPreferencesRepository

class UpdateLeafCountWorker(private val appContext: Context, private val workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val sharedPrefsProvider = SharedPreferencesRepository(appContext)

        //if (sharedPrefsProvider.get)
        sharedPrefsProvider.apply {
            val stepsTaken = getDailyStepCount()
            val lastDayStepsTaken = getLastDayStepCount()
            val dailyGoal = getDailyStepsGoal()

            val leavesToAdd = stepsTaken / 1000
            Log.v("Steps taken: ", leavesToAdd.toString())
            var leavesToRemove: Int

            leavesToRemove = Math.ceil((dailyGoal - lastDayStepsTaken) / 1000.0).toInt()
            if (leavesToRemove < 0) leavesToRemove = 0

            dailyGoalChecked(1)

            //Here, LEAVES_GAINED_TODAY will contain the last updated leaves value from the previous day
            //val netLeavesToAdd = leavesToAdd - leavesToRemove + PlayerPrefs.GetInt(LEAVES_GAINED_TODAY)

            //This is OK to do here, because this method will run only once each day
            //This should be done here, so that oldLeavesCount variable in UpdateLeavesCount() gets updated info
            //storeLeafCountBeforeToday(PlayerPrefs.GetInt(LEAF_COUNT_BEFORE_TODAY, 0) + netLeavesToAdd)

            //Log.v("Net leaves: ", netLeavesToAdd)
        }
        return Result.success()
    }
}