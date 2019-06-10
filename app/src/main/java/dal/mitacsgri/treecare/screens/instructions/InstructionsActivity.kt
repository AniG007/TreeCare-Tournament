package dal.mitacsgri.treecare.screens.instructions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.provider.SharedPreferencesProvider

class InstructionsActivity : AppCompatActivity() {

    private lateinit var sharedPrefProvider: SharedPreferencesProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        sharedPrefProvider = SharedPreferencesProvider(this)
        sharedPrefProvider.hasInstructionsDisplayed = true
    }
}
