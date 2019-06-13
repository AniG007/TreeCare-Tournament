package dal.mitacsgri.treecare.backgroundtasks.jobs

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class TreeUpdateJobCreator(private val context: Context): JobCreator {

    override fun create(tag: String): Job? =
        when(tag) {
            CheckGoalAndUpdateTreeJob.TAG ->
                CheckGoalAndUpdateTreeJob(context)
            else ->
                null
        }
}