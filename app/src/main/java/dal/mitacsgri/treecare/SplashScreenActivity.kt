package dal.mitacsgri.treecare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = SharedPreferencesProvider(this)
        sharedPref.storeDailyStepCount(8000)

        startNextActivity(StepsCounterActivity::class.java, 2000)
    }

    private fun startNextActivity(activity : Class<*>, delay : Long) {
        Handler().postDelayed( {
            startActivity(Intent(this@SplashScreenActivity, activity))
        }, delay)
    }
}