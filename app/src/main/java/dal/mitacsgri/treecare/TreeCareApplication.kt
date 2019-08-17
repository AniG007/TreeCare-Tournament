package dal.mitacsgri.treecare

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.evernote.android.job.JobApi
import com.evernote.android.job.JobConfig
import com.evernote.android.job.JobManager
import dal.mitacsgri.treecare.backgroundtasks.jobcreator.MainJobCreator
import dal.mitacsgri.treecare.backgroundtasks.jobs.DailyGoalNotificationJob
import dal.mitacsgri.treecare.backgroundtasks.jobs.TrophiesUpdateJob
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserChallengeDataWorker
import dal.mitacsgri.treecare.di.appModule
import dal.mitacsgri.treecare.di.firestoreRepositoryModule
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.di.stepCountRepositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class TreeCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TreeCareApplication)
            modules(listOf(appModule, sharedPreferencesRepositoryModule, stepCountRepositoryModule,
                firestoreRepositoryModule))
        }

        val updateUserChallengeDataRequest =
            PeriodicWorkRequestBuilder<UpdateUserChallengeDataWorker>(15, MINUTES).build()

        WorkManager.getInstance(this).enqueue(updateUserChallengeDataRequest)

        JobConfig.setApiEnabled(JobApi.WORK_MANAGER, false)
        JobManager.create(this).addJobCreator(MainJobCreator())
        TrophiesUpdateJob.scheduleJob()

        val tag ="DailyGoalNotificationJob"

        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(6),
            HOURS.toMillis(6) + MINUTES.toMillis(15), tag + 1)
        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(13),
            HOURS.toMillis(13) + MINUTES.toMillis(15), tag + 2)
        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(18),
            HOURS.toMillis(18) + MINUTES.toMillis(15), tag + 3)
        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(21),
            HOURS.toMillis(21) + MINUTES.toMillis(15), tag + 4)
    }
}