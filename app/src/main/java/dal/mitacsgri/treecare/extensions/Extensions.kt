package dal.mitacsgri.treecare.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import androidx.lifecycle.MutableLiveData

fun Any.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, toString(), duration).show()
}

fun Activity.startNextActivity(activity : Class<*>, delay : Long = 0) {
    Handler().postDelayed( {
        startActivity(Intent(this, activity))
    }, delay)
}

//Creating MutableLiveData with default value
fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { value = initialValue }