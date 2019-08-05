package dal.mitacsgri.treecare.screens.gamesettings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.CHALLENGER_MODE
import dal.mitacsgri.treecare.extensions.disable
import dal.mitacsgri.treecare.extensions.enable
import dal.mitacsgri.treecare.extensions.getTextAsInt
import dal.mitacsgri.treecare.extensions.toast
import kotlinx.android.synthetic.main.activity_settings.*
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity : AppCompatActivity() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsViewModel.settingsChanged.observe(this, Observer {
            if (it) buttonSave.enable()
            else buttonSave.disable()
        })

        buttonSave.setOnClickListener {
            saveSettingsAction()
        }

        if (settingsViewModel.getGameMode() == CHALLENGER_MODE) {
            dailyGoalTV.visibility = View.GONE
            seekBarGoal.visibility = View.GONE
            dailyGoalChangeTV.visibility = View.GONE
            dailyGoalChangeInfoTV.visibility = View.GONE
            iconGoal.visibility = View.GONE
        } else {
            val currentDailyGoal = settingsViewModel.getCurrentDailyStepsGoal()
            dailyGoalTV.text = currentDailyGoal.toString()
            seekBarGoal.progress = currentDailyGoal / 1000
        }

        val currentVolume = settingsViewModel.getCurrentVolume()
        volumeTV.text = currentVolume.toString()
        seekBarVolume.progress = currentVolume

        seekBarGoal.numericTransformer = object : DiscreteSeekBar.NumericTransformer() {
            override fun transform(value: Int): Int = value*1000
        }

        seekBarGoal.setOnProgressChangeListener(object: DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(
                seekBar: DiscreteSeekBar?,
                value: Int,
                fromUser: Boolean
            ) {
                dailyGoalTV.text = (seekBar!!.progress*1000).toString()
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {}

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {
                settingsViewModel.hasSettingsChanged = true
            }
        })

        seekBarVolume.setOnProgressChangeListener(object: DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(
                seekBar: DiscreteSeekBar?,
                value: Int,
                fromUser: Boolean
            ) {
                volumeTV.text = seekBar!!.progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {}

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {
                settingsViewModel.hasSettingsChanged = true
            }
        })
    }

    override fun onBackPressed() {
        if (!settingsViewModel.hasSettingsChanged)
            super.onBackPressed()
        else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.unsaved_changes_warning)
                .setCancelable(false)
                .setPositiveButton("Keep") { _: DialogInterface, _: Int ->
                    saveSettingsAction()
                }
                .setNegativeButton("Discard") { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    super.onBackPressed()
                }
                .show()
        }
    }

    private fun saveSettingsAction() {
        settingsViewModel.saveSettings(volumeTV.getTextAsInt(), dailyGoalTV.getTextAsInt()) {
            "Settings updated".toast(this)
            super.onBackPressed()
        }
    }
}
