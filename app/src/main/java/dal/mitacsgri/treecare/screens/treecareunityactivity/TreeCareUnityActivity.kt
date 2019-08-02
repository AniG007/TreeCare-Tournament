package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.core.content.ContextCompat
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.screens.gamesettings.SettingsActivity
import dal.mitacsgri.treecare.services.StepDetectorService
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import java.util.*


/**
 * Created by Devansh on 24-06-2019
 */
class TreeCareUnityActivity : UnityPlayerActivity() {

    private val TAG: String = "SensorAPI"
    private var volume = 0f

    private lateinit var mediaPlayer: MediaPlayer

    //Called from Unity
    fun Launch() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService()
        mediaPlayer = MediaPlayer.create(this, R.raw.tree_background_sound)
        mediaPlayer.isLooping = true
        startFadeIn()
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer.start()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        mediaPlayer.stop()
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

    private fun startFadeIn() {
        val FADE_DURATION = 6000
        val FADE_INTERVAL = 100L
        val MAX_VOLUME = 1
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL
        val deltaVolume = MAX_VOLUME / numberOfSteps.toFloat()

        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                fadeInStep(deltaVolume)
                if (volume >= 1f) {
                    timer.cancel()
                    timer.purge()
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL)
    }

    private fun fadeInStep(deltaVolume: Float) {
        mediaPlayer.setVolume(volume, volume)
        volume += deltaVolume
    }
}