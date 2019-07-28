package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Intent
import androidx.core.content.ContextCompat
import dal.mitacsgri.treecare.screens.gamesettings.SettingsActivity
import dal.mitacsgri.treecare.services.StepDetectorService
import dal.mitacsgri.treecare.unity.UnityPlayerActivity

/**
 * Created by Devansh on 24-06-2019
 */
class TreeCareUnityActivity : UnityPlayerActivity() {

    private val TAG: String = "SensorAPI"

    //Called from Unity
    fun Launch() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        startService()
    }

    override fun onStop() {
        super.onStop()
        stopService()
    }

    private fun startService() {
        val serviceIntent = Intent(this, StepDetectorService::class.java)
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android")
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, StepDetectorService::class.java)
        stopService(serviceIntent)
    }
}