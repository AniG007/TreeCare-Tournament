package dal.mitacsgri.treecare

import android.content.IntentSender
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider
import dal.mitacsgri.treecare.provider.StepCountProvider
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

        if (sharedPrefProvider.isLoginDone) setupFitApiToGetData()
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

    private fun setupFitApiToGetData() {

        var authInProgress = false
        val SIGN_IN_CODE = 1000
        var mClient: GoogleApiClient? = null
        val stepCountProvider = StepCountProvider(this)
        var dailyStepCountObtained = false
        var lastDayStepCountObtained = false

        val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
            if (!authInProgress) {
                try {
                    authInProgress = true
                    it.startResolutionForResult(this, SIGN_IN_CODE)
                } catch (e: IntentSender.SendIntentException) {

                }
            } else {
                "Logging you in".toast(this)
            }
        }

        mClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    stepCountProvider.apply {
                        getTodayStepCountData(mClient!!) {
                            sharedPrefProvider.storeDailyStepCount(it.toInt())
                            //startNextActivity(UnityPlayerActivity::class.java)
                        }

                        getLastDayStepCountData(mClient!!) {
                            sharedPrefProvider.storeLastDayStepCount(it.toInt())
                        }

                    }
                }

                override fun onConnectionSuspended(p0: Int) {}
            })
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()
    }


}