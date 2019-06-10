package dal.mitacsgri.treecare.provider

import android.content.Context
import android.content.SharedPreferences
import dal.mitacsgri.treecare.R

class SharedPreferencesProvider(val context: Context) {

    public val sharedPref: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.unity_shared_preferences),
        Context.MODE_PRIVATE
    )

    var isLoginDone: Boolean
        get() = sharedPref.getBoolean(context.getString(R.string.login_done), false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean(context.getString(R.string.login_done), value)
                apply()
            }
        }

    var lastOpenedDayPlus1: Long
        get() = sharedPref.getLong(context.getString(R.string.last_opened_day), 0)
        set(value) {
            with(sharedPref.edit()) {
                putLong(context.getString(R.string.last_opened_day), value)
                apply()
            }
        }

    fun storeDailyStepCount(stepCount: Int) {
        storeInt(R.string.daily_step_count, stepCount)
    }

    fun storeLastDayStepCount(stepCount: Int) {
        storeInt(R.string.last_day_step_count, stepCount)
    }

    fun storeDailyStepsGoal(goal: Int) {
        storeInt(R.string.daily_steps_goal, goal)
    }

    fun dailyGoalChecked(value: Int) {
        storeInt(R.string.daily_goal_checked, value)
    }

    private fun storeInt(key: Int, value: Int) {
        with(sharedPref.edit()) {
            putInt(context.getString(key), value)
            apply()
        }
    }
}