package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Intent
import dal.mitacsgri.treecare.screens.gamesettings.SettingsActivity
import dal.mitacsgri.treecare.unity.UnityPlayerActivity

/**
 * Created by Devansh on 24-06-2019
 */
class TreeCareUnityActivity : UnityPlayerActivity() {

    //Called from Unity
    fun Launch() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}