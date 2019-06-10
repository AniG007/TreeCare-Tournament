package dal.mitacsgri.treecare

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.provider.DailyStepCountProvider
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider
import dal.mitacsgri.treecare.screens.login.LoginActivity
import dal.mitacsgri.treecare.unity.UnityPlayerActivity

class SplashScreenActivity : AppCompatActivity() {

    private val SPLASH_SCREEN_DELAY = 5000L
    private lateinit var sharedPrefProvider: SharedPreferencesProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        sharedPrefProvider = SharedPreferencesProvider(this)
        sharedPrefProvider.apply {
            storeDailyStepsGoal(5000)

            if (isLoginDone) setupAndStartUnityActivity()
            else    startNextActivity(LoginActivity::class.java, SPLASH_SCREEN_DELAY)
        }
    }

    private fun startNextActivity(activity : Class<*>, delay : Long) {
        Handler().postDelayed( {
            startActivity(Intent(this@SplashScreenActivity, activity))
        }, delay)
    }


    private fun setupAndStartUnityActivity() {

        var authInProgress = false
        val SIGN_IN_CODE = 1000
        var mClient: GoogleApiClient? = null

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
            .addApi(Fitness.RECORDING_API)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {

                    DailyStepCountProvider(this@SplashScreenActivity,  mClient!!)
                        .stepCountObtained {
                            sharedPrefProvider.storeDailyStepCount(it.toInt())
                            startNextActivity(UnityPlayerActivity::class.java, SPLASH_SCREEN_DELAY)
                        }
                }

                override fun onConnectionSuspended(p0: Int) {}
            })
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()
    }
}