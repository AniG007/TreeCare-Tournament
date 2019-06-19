package dal.mitacsgri.treecare.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R

class MainActivity : AppCompatActivity() {

    private val mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (findNavController(R.id.navHostFragment).currentDestination?.id == R.id.loginFragment) {


        }
    }
}
