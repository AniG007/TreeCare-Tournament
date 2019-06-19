package dal.mitacsgri.treecare

import android.app.Application
import com.evernote.android.job.JobManager
import com.facebook.stetho.Stetho
import dal.mitacsgri.treecare.backgroundtasks.jobs.TreeUpdateJobCreator
import dal.mitacsgri.treecare.di.appModule
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.di.stepCountRepositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TreeCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
        JobManager.create(this).addJobCreator(TreeUpdateJobCreator(this))

        startKoin {
            androidContext(this@TreeCareApplication)
            modules(listOf(appModule, sharedPreferencesRepositoryModule, stepCountRepositoryModule))
        }
    }
}