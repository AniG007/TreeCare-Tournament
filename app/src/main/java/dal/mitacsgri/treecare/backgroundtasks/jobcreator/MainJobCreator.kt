package dal.mitacsgri.treecare.backgroundtasks.jobcreator

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import dal.mitacsgri.treecare.backgroundtasks.jobs.DailyGoalNotificationJob
import dal.mitacsgri.treecare.backgroundtasks.jobs.TrophiesUpdateJob

class MainJobCreator: JobCreator {

    override fun create(tag: String): Job? =
        when(tag) {
            TrophiesUpdateJob.TAG -> {
                TrophiesUpdateJob()
            }
            DailyGoalNotificationJob.TAG -> {
                DailyGoalNotificationJob()
            }
            else -> null
        }
}