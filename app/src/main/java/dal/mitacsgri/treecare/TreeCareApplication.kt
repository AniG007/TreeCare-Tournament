package dal.mitacsgri.treecare

import android.app.Application
import androidx.work.*
import com.evernote.android.job.JobApi
import com.evernote.android.job.JobConfig
import com.evernote.android.job.JobManager
import dal.mitacsgri.treecare.backgroundtasks.jobcreator.MainJobCreator
import dal.mitacsgri.treecare.backgroundtasks.jobs.DailyGoalNotificationJob
import dal.mitacsgri.treecare.backgroundtasks.jobs.TrophiesUpdateJob
import dal.mitacsgri.treecare.backgroundtasks.workers.*
import dal.mitacsgri.treecare.di.appModule
import dal.mitacsgri.treecare.di.firestoreRepositoryModule
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.di.stepCountRepositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit.*

class TreeCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TreeCareApplication)
            modules(
                listOf(
                    appModule, sharedPreferencesRepositoryModule, stepCountRepositoryModule,
                    firestoreRepositoryModule
                )
            )
        }

        val mConstraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val updateUserChallengeDataRequest: WorkRequest =
            PeriodicWorkRequestBuilder<UpdateUserChallengeDataWorker>(15, MINUTES)
                .setConstraints(mConstraints)
                .setInitialDelay(5, MINUTES)
                .build()

//        val updateUserChallengeDataRequest: WorkRequest =
//            OneTimeWorkRequestBuilder<UpdateUserChallengeDataWorker>()
//                .setConstraints(mConstraints)
//                .setInitialDelay(5, MINUTES)
//                .build()

        val updateDailyStepCountDataRequest: WorkRequest =
            PeriodicWorkRequestBuilder<UpdateDailyStepCount>(15, MINUTES)
                .setConstraints(mConstraints)
                .setInitialDelay(5, MINUTES)
                .build()

//        val updateDailyStepCountDataRequest: WorkRequest =
//            OneTimeWorkRequestBuilder<UpdateDailyStepCount>()
//                .setConstraints(mConstraints)
//                .setInitialDelay(5,MINUTES)
//                .build()

        val updateTeamDataRequest: WorkRequest =
            PeriodicWorkRequestBuilder<UpdateTeamDataWorker>(15, MINUTES)
                .setConstraints(mConstraints)
                .setInitialDelay(5, MINUTES)
                .build()

//        val updateTeamDataRequest: WorkRequest =
//            OneTimeWorkRequestBuilder<UpdateTeamDataWorker>()
//                .setConstraints(mConstraints)
//                .setInitialDelay(5, MINUTES)
//                .build()
//
//        val DailyGoalNotificationRequest: WorkRequest =
//            OneTimeWorkRequestBuilder<DailyNotificationWorker>()
//                .setConstraints(mConstraints)
//                .setInitialDelay(5,MINUTES)
//                .build()
//
//        val TrophiesUpdateRequest: WorkRequest =
//            OneTimeWorkRequestBuilder<TrophiesUpdateWorker>()
//                .setConstraints(mConstraints)
//                .setInitialDelay(5,MINUTES)
//                .build()


        val dailyGoalNotificationRequest: WorkRequest =
            PeriodicWorkRequestBuilder<DailyNotificationWorker>(3, HOURS)
                .setConstraints(mConstraints)
                .setInitialDelay(5, MINUTES)
                .build()


        val trophiesUpdateRequest: WorkRequest =
            PeriodicWorkRequestBuilder<TrophiesUpdateWorker>(8, HOURS)
                .setConstraints(mConstraints)
                .setInitialDelay(5, MINUTES)
                .build()

        WorkManager.getInstance(this).enqueue(
            listOf(
                updateDailyStepCountDataRequest,
                updateTeamDataRequest,
                updateUserChallengeDataRequest,
                dailyGoalNotificationRequest,
                trophiesUpdateRequest
            )
        )
        //JobConfig.setApiEnabled(JobApi.WORK_MANAGER, false)
        //JobManager.create(this).addJobCreator(MainJobCreator())
        //TrophiesUpdateJob.scheduleJob()

        val tag = "DailyGoalNotificationJob"

//        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(6),
//            HOURS.toMillis(6) + MINUTES.toMillis(15), tag + 1)
//        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(13),
//            HOURS.toMillis(13) + MINUTES.toMillis(15), tag + 2)
//        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(18),
//            HOURS.toMillis(18) + MINUTES.toMillis(15), tag + 3)
//        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(21),
//            HOURS.toMillis(21) + MINUTES.toMillis(15), tag + 4)
//
//        DailyGoalNotificationJob.scheduleJob(HOURS.toMillis(20) + MINUTES.toMillis(36),
//            HOURS.toMillis(20) + MINUTES.toMillis(50) , tag +5)
        //DailyGoalNotificationJob.scheduleJob()
    }
}