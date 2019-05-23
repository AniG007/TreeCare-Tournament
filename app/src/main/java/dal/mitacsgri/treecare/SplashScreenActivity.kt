package dal.mitacsgri.treecare

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun closeSplashScreen() {
        Handler().postDelayed( {
            startActivity(Intent(this@SplashScreenActivity, MenuActivity::class.java))
        }, 4000)
    }
}