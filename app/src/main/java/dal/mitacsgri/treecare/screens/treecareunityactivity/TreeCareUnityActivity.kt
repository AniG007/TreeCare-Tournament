package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.CHALLENGER_MODE
import dal.mitacsgri.treecare.consts.STARTER_MODE
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.screens.gamesettings.SettingsActivity
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportActivity
import dal.mitacsgri.treecare.services.StepDetectorService
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

/**
 * Created by Devansh on 24-06-2019
 */
class TreeCareUnityActivity : UnityPlayerActivity(), KoinComponent {

    private val sharedPrefsRepository: SharedPreferencesRepository by inject()

    private val TAG: String = "SensorAPI"
    private var volume = 0f
    private var isSoundFadingIn = true

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager

    //Called from Unity
    fun OpenSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun OpenHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getHelpTitle())
            .setMessage(getHelpText())
            .setNegativeButton("Close") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    fun OpenProgressReport() {
        startActivity(Intent(this, ProgressReportActivity::class.java))
    }

    fun OpenLeaderboard() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val res = audioManager.requestAudioFocus(audioFocusChangedListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN)

        if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer = MediaPlayer.create(this, R.raw.tree_background_sound)
            mediaPlayer.isLooping = true
            startFadeIn()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isSoundFadingIn) {
            val volume = sharedPrefsRepository.volume
            mediaPlayer.setVolume(volume, volume)
        }
        mediaPlayer.start()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
        isSoundFadingIn = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        mediaPlayer.stop()
        audioManager.abandonAudioFocus(audioFocusChangedListener)
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
        val MAX_VOLUME = sharedPrefsRepository.volume
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL
        val deltaVolume = MAX_VOLUME / numberOfSteps.toFloat()

        isSoundFadingIn = true

        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                fadeInStep(deltaVolume)
                if (volume >= MAX_VOLUME) {
                    timer.cancel()
                    timer.purge()
                    isSoundFadingIn = false
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL)
    }

    private fun fadeInStep(deltaVolume: Float) {
        mediaPlayer.setVolume(volume, volume)
        volume += deltaVolume
    }

    private fun getHelpText() =
        when(sharedPrefsRepository.gameMode) {
            STARTER_MODE -> getString(R.string.starter_mode_instructions)
            CHALLENGER_MODE -> getString(R.string.challenger_mode_instructions)
            else -> ""
        }

    private fun getHelpTitle() =
        buildSpannedString {
            bold {
                append("Help: ")
                append(when (sharedPrefsRepository.gameMode) {
                    STARTER_MODE -> getString(R.string.starter_mode)
                    CHALLENGER_MODE -> getString(R.string.challenger_mode)
                    else -> ""
                })
            }
        }

    private val audioFocusChangedListener = AudioManager.OnAudioFocusChangeListener {
        when(it) {
            AudioManager.AUDIOFOCUS_LOSS -> mediaPlayer.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> mediaPlayer.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val volume = sharedPrefsRepository.volume
                if (volume < 0.2) {
                    mediaPlayer.setVolume(volume, volume)
                } else
                    mediaPlayer.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer.start()
        }
    }
}