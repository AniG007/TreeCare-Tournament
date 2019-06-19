package dal.mitacsgri.treecare

import android.app.Application
import android.content.Context
import com.evernote.android.job.JobManager
import com.facebook.stetho.Stetho
import dal.mitacsgri.treecare.backgroundtasks.jobs.TreeUpdateJobCreator

class TreeCareApplication : Application() {

    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        JobManager.create(this).addJobCreator(TreeUpdateJobCreator(this))
    }
}