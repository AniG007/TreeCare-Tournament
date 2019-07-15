package dal.mitacsgri.treecare.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.gson.Gson
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

fun DateTime.getStringRepresentation(): String = toString("MMM d, H:m")

//Notify because if LiveData has a list, adding elements to list won't notify the observer
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}

fun LayoutInflater.createFragmentViewWithStyle(
    activity: Activity?, layout: Int, style: Int,
    root: ViewGroup? = null, attachToRoot: Boolean = false): View =
        cloneInContext(ContextThemeWrapper(activity, style)).inflate(layout, root, attachToRoot)

fun TextInputEditText.validate(message: String, action: (String) -> Boolean = { it.isNotEmpty() }) {
    addTextChangedListener(object: TextWatcher {
        val parentLayout = parent.parent as TextInputLayout
        override fun afterTextChanged(s: Editable?) {
            parentLayout.error = if (action(text.toString())) null else message
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun <T> List<T>.toArrayList(): ArrayList<T> {
    val arrayList = arrayListOf<T>()
    forEach {
        arrayList.add(it)
    }
    return arrayList
}

fun <T> Any.toJson() = Gson().toJson(this as T)

fun MaterialButton.disable() {
    isEnabled = false
    background.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
}

fun getCardItemDescriptorText(property: String, value: String) =
        buildSpannedString {
            bold {
                append("$property: ")
            }
            append(value)
        }