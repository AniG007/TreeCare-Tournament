package dal.mitacsgri.treecare

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserChallengeDataWorker
import dal.mitacsgri.treecare.di.appModule
import dal.mitacsgri.treecare.di.firestoreRepositoryModule
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.di.stepCountRepositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class TreeCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TreeCareApplication)
            modules(listOf(appModule, sharedPreferencesRepositoryModule, stepCountRepositoryModule,
                firestoreRepositoryModule))
        }

        val updateUserChallengeDataRequest =
            PeriodicWorkRequestBuilder<UpdateUserChallengeDataWorker>(15, TimeUnit.MINUTES).build()

        WorkManager.getInstance(this).enqueue(updateUserChallengeDataRequest)
    }
}