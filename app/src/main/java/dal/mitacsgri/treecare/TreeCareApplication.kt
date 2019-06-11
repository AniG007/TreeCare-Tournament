package dal.mitacsgri.treecare

import android.app.Application
import com.facebook.stetho.Stetho

class TreeCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this);
    }
}