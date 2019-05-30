package dal.mitacsgri.treecare

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import gri.unity.treecare.UnityPlayerActivity

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        closeSplashScreen()
    }

    private fun closeSplashScreen() {
        Handler().postDelayed( {
            startActivity(Intent(this@SplashScreenActivity, UnityPlayerActivity::class.java))
        }, 4000)
    }
}