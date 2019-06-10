package dal.mitacsgri.treecare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider
import dal.mitacsgri.treecare.screens.ModeSelectionActivity
import dal.mitacsgri.treecare.screens.login.LoginActivity
import java.util.*


class SplashScreenActivity : AppCompatActivity() {

    private val SPLASH_SCREEN_DELAY = 5000L
    private lateinit var sharedPrefProvider: SharedPreferencesProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        sharedPrefProvider = SharedPreferencesProvider(this)
        sharedPrefProvider.apply {
            storeDailyStepsGoal(5000)

            with(sharedPref.edit()) {
                putInt(getString(R.string.is_first_run), 1)
                apply()
            }

            if (isLoginDone) startNextActivity(ModeSelectionActivity::class.java, SPLASH_SCREEN_DELAY)
            else startNextActivity(LoginActivity::class.java, SPLASH_SCREEN_DELAY)
        }
        resetDailyGoalCheckedFlag()
    }

    private fun resetDailyGoalCheckedFlag() {

        //Will execute only once in each day, when the app is opened for thr first time in the day
        if (sharedPrefProvider.lastOpenedDayPlus1 < Date().time) {
            sharedPrefProvider.dailyGoalChecked(0)

            val cal = Calendar.getInstance()
            val now = Date()
            cal.apply {
                time = now
                set(Calendar.MILLISECOND, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR, 0)
            }

            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH)

            cal.add(Calendar.DAY_OF_YEAR, 1)

            //Doing this to prevent rounding off at the end of the year
            if (day == 31 && month == 12) cal.add(Calendar.YEAR, 1)

            sharedPrefProvider.lastOpenedDayPlus1 = cal.timeInMillis
        }
    }


}