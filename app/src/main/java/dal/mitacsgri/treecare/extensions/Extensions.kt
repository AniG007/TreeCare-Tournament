package dal.mitacsgri.treecare.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import org.joda.time.DateTime

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

fun DateTime.toTimestamp() = Timestamp(millis/1000, 0)

fun Timestamp.toDateTime() = DateTime(seconds*1000)

fun DateTime.getStringRepresentation() = toString("EEE MMM d, H:m")

//Notify because if LiveData has a list, adding elements to list won't notify the observer
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}