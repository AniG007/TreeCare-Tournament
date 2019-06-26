package dal.mitacsgri.treecare

import android.app.Application
import dal.mitacsgri.treecare.di.appModule
import dal.mitacsgri.treecare.di.firestoreRepositoryModule
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.di.stepCountRepositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TreeCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TreeCareApplication)
            modules(listOf(appModule, sharedPreferencesRepositoryModule, stepCountRepositoryModule,
                firestoreRepositoryModule))
        }
    }
}