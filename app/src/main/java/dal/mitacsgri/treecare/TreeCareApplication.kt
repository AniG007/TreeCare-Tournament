package dal.mitacsgri.treecare

import android.app.Application
import androidx.work.*
import com.evernote.android.job.JobApi
import com.evernote.android.job.JobConfig
import com.evernote.android.job.JobManager
import dal.mitacsgri.treecare.backgroundtasks.jobcreator.MainJobCreator
import dal.mitacsgri.treecare.backgroundtasks.jobs.DailyGoalNotificationJob
import dal.mitacsgri.treecare.backgroundtasks.jobs.TrophiesUpdateJob
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateTeamDataWorker
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateTournamentSteps
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserChallengeDataWorker
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserTournamentDataWorker
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

        /*val updateUserTournamentDataRequest =
            PeriodicWorkRequestBuilder<UpdateUserTournamentDataWorker>(15 , MINUTES).build()*/

        val mConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val updateUserChallengeDataRequest: WorkRequest =
            PeriodicWorkRequestBuilder<UpdateUserChallengeDataWorker>(15, MINUTES).setConstraints(mConstraints).build()

        val updateUserTournamentDataRequest: WorkRequest =
            PeriodicWorkRequestBuilder<UpdateUserTournamentDataWorker>(15, MINUTES).setConstraints(mConstraints).build()

        val updateTeamDataRequest: WorkRequest =
            PeriodicWorkRequestBuilder<UpdateTeamDataWorker>(15, MINUTES).setConstraints(mConstraints).build()

        //WorkManager.getInstance(this).enqueue(updateTeamDataRequest)
        WorkManager.getInstance(this).enqueue(listOf( updateUserChallengeDataRequest, updateUserTournamentDataRequest, updateTeamDataRequest ))

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