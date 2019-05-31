package dal.mitacsgri.treecare.provider

import android.content.Context
import android.content.SharedPreferences
import dal.mitacsgri.treecare.R

class SharedPreferencesProvider(val context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.unity_shared_preferences),
        Context.MODE_PRIVATE)

    public fun storeDailyStepCount(stepCount: Int) {
        with(sharedPref.edit()) {
            putInt(context.getString(R.string.daily_step_count), stepCount)
            apply()
        }
    }

    public fun storeLastDaysStepCount(stepCount: Int) {
        with(sharedPref.edit()) {
            putInt(context.getString(R.string.last_days_step_count), stepCount)
            apply()
        }
    }
}