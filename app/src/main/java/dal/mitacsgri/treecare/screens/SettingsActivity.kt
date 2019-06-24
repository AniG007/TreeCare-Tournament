package dal.mitacsgri.treecare.screens

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.activity_settings.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity : AppCompatActivity() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsViewModel.dailyGoal.observe(this, Observer {
            buttonDecrease.apply {
                if (it > 5000) {
                    setImageResource(R.drawable.arrow_active)
                    isEnabled = true
                } else {
                    setImageResource(R.drawable.arrow_inactive)
                    isEnabled = false
                }
            }
            stepsCountText.text = it.toString()
        })

        buttonDecrease.setOnClickListener {
            settingsViewModel.decreaseDailyGoal()
        }

        buttonIncrease.setOnClickListener {
            settingsViewModel.increaseDailyGoal()
        }

        changeBackgroundSolidAndStrokeColor(buttonSaveDailyGoal, "FFCFCFCF", "FF828282")
        buttonSaveDailyGoal.setOnClickListener {
            settingsViewModel.storeUpdatedStepGoal(stepsCountText.text.toString().toInt())
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
}
