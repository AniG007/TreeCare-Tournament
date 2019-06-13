package dal.mitacsgri.treecare.backgroundtasks.jobs

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobRequest
import dal.mitacsgri.treecare.backgroundtasks.workers.DailyStepCountWorker
import dal.mitacsgri.treecare.backgroundtasks.workers.LastDayStepCountWorker
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateLeafCountWorker
import java.util.concurrent.TimeUnit

class CheckGoalAndUpdateTreeJob(context: Context): DailyJob() {

    companion object {
        const val TAG = "job_check_goal_and_update_tree_tag"

        fun schedule() {
            DailyJob.schedule(JobRequest.Builder(TAG),
                TimeUnit.HOURS.toMillis(20), TimeUnit.HOURS.toMillis(21))
        }
    }



    override fun onRunDailyJob(p0: Params): DailyJobResult {

        WorkManager.getInstance()
            .beginWith(makeDailyStepCountRequest())
            .then(makeLastDayStepCountRequest())
            .then(makeUpdateLeafCountRequest())
            .enqueue()

        return DailyJobResult.SUCCESS
    }

    private fun makeDailyStepCountRequest() =
        OneTimeWorkRequest.Builder(DailyStepCountWorker::class.java)
                .build()

    private fun makeLastDayStepCountRequest() =
        OneTimeWorkRequest.Builder(LastDayStepCountWorker::class.java)
                .build()

    private fun makeUpdateLeafCountRequest() =
        OneTimeWorkRequest.Builder(UpdateLeafCountWorker::class.java)
                .build()
}