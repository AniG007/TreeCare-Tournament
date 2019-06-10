package dal.mitacsgri.treecare.screens

import android.content.IntentSender
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider
import dal.mitacsgri.treecare.provider.StepCountProvider
import dal.mitacsgri.treecare.screens.instructions.InstructionsActivity
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import kotlinx.android.synthetic.main.activity_menu_selection.*

class ModeSelectionActivity : AppCompatActivity() {

    private lateinit var sharedPrefProvider: SharedPreferencesProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_selection)

        sharedPrefProvider = SharedPreferencesProvider(this)
        setupFitApiToGetData()

        changeBackgroundSolidAndStrokeColor(starterModeButton, "FF0189F1", "FF0000FF")
        changeBackgroundSolidAndStrokeColor(challengerModeButton, "FFFF6F00", "FFBF360C")
        changeBackgroundSolidAndStrokeColor(tournamentModeButton, "FF9C27B0", "1A237E")

        starterModeButton.setOnClickListener {
            startInstructionOrUnityActivity()
        }
    }

    private fun changeBackgroundSolidAndStrokeColor(
        button: Button, solidColor: String, strokeColor: String) {

        val background = button.background as GradientDrawable
        background.setColor(Color.parseColor("#$solidColor"))
        background.setStroke(
            resources.getDimension(com.intuit.sdp.R.dimen._4sdp).toInt(),
            Color.parseColor("#$strokeColor"))

    }

    private fun startInstructionOrUnityActivity() {
        if (sharedPrefProvider.hasInstructionsDisplayed)
            startNextActivity(UnityPlayerActivity::class.java)
        else
            startNextActivity(InstructionsActivity::class.java)
    }

    private fun setupFitApiToGetData() {

        var authInProgress = false
        val SIGN_IN_CODE = 1000
        var mClient: GoogleApiClient? = null
        val stepCountProvider = StepCountProvider(this)

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
