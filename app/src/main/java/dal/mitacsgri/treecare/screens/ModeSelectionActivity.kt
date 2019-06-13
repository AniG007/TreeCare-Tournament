package dal.mitacsgri.treecare.screens

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider
import dal.mitacsgri.treecare.screens.instructions.InstructionsActivity
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import kotlinx.android.synthetic.main.activity_menu_selection.*

class ModeSelectionActivity : AppCompatActivity() {

    private lateinit var sharedPrefProvider: SharedPreferencesProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_selection)

        sharedPrefProvider = SharedPreferencesProvider(this)

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
}