package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Intent
import dal.mitacsgri.treecare.screens.SettingsActivity
import dal.mitacsgri.treecare.unity.UnityPlayerActivity

/**
 * Created by Devansh on 24-06-2019
 */
class TreeCareUnityActivity : UnityPlayerActivity() {

    fun Launch() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}