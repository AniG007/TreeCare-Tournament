package dal.mitacsgri.treecare.extensions

import android.content.Context
import android.widget.Toast

fun Any.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, toString(), duration).show()
}