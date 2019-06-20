package dal.mitacsgri.treecare.screens.instructions

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.startNextActivity
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import kotlinx.android.synthetic.main.activity_instructions.*

class InstructionsActivity : AppCompatActivity() {

    private lateinit var sharedPrefProvider: SharedPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        sharedPrefProvider = SharedPreferencesRepository(this)
        sharedPrefProvider.hasInstructionsDisplayed = true

        changeBackgroundSolidAndStrokeColor(scrollView, "ccffffff", "ff646464")
        changeBackgroundSolidAndStrokeColor(continueButton, "FF0189F1", "FF0000FF")

        continueButton.setOnClickListener {
            startNextActivity(UnityPlayerActivity::class.java)
        }
    }

    private fun changeBackgroundSolidAndStrokeColor(
        view: View, solidColor: String, strokeColor: String) {

        val background = view.background as GradientDrawable
        background.setColor(Color.parseColor("#$solidColor"))
        background.setStroke(
            resources.getDimension(com.intuit.sdp.R.dimen._4sdp).toInt(),
            Color.parseColor("#$strokeColor"))
    }
}
