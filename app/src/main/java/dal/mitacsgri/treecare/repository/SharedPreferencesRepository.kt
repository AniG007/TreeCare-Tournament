package dal.mitacsgri.treecare.repository

import android.content.Context
import android.content.SharedPreferences
import dal.mitacsgri.treecare.R

class SharedPreferencesRepository(val context: Context) {

    val sharedPref: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.unity_shared_preferences),
        Context.MODE_PRIVATE
    )

    //This value neeeds to be obtained from the database to check if the user is using the app for the first time or not
    var isFirstRun
        get() = getBoolean(R.string.is_first_run, true)
        set(value) {
            storeBoolean(R.string.is_first_run, value)
        }

    //This needs to be as int because this will be used by Unity and Unity does not support boolean prefs
    var isDailyGoalChecked
        get() = sharedPref.getInt(context.getString(R.string.daily_goal_checked), 0)
        set(value) {
            storeInt(R.string.daily_goal_checked, value)
        }

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

    var hasInstructionsDisplayed: Boolean
        get() = sharedPref.getBoolean(context.getString(R.string.has_instructions_displayed), false)
        set(value) {
            with(sharedPref.edit()) {
                putBoolean(context.getString(R.string.has_instructions_displayed), value)
                apply()
            }
        }

    var lastLoginTime: Long
        get() = sharedPref.getLong(context.getString(R.string.last_login_time_ms), 0)
        set(value) {
            with(sharedPref.edit()) {
                putLong(context.getString(R.string.last_login_time_ms), value)
                apply()
            }
        }

    var lastLogoutTime: Long
        get() = sharedPref.getLong(context.getString(R.string.last_logout_time_ms), 0)
        set(value) {
            with(sharedPref.edit()) {
                putLong(context.getString(R.string.last_logout_time_ms), value)
                apply()
            }
        }

    var lastLeafCount: Int
        get() = getInt(R.string.last_leaf_count)
        set(value) {
            storeInt(R.string.last_leaf_count, value)
        }

    var currentLeafCount: Int
        get() = getInt(R.string.current_leaf_count)
        set(value) {
            storeInt(R.string.current_leaf_count, value)
        }

    var lastFruitCount
        get() = getInt(R.string.last_fruit_count)
        set(value) { storeInt(R.string.last_fruit_count, value) }

    var currentFruitCount
        get() = getInt(R.string.current_fruit_count)
        set(value) {
            if (value < 0) return
            storeInt(R.string.current_fruit_count, value)
        }

    var currentDayOfWeek
        get() = getInt(R.string.current_day_of_week)
        set(value) { storeInt(R.string.current_day_of_week, value) }

    fun storeDailyStepCount(stepCount: Int) {
        storeInt(R.string.daily_step_count, stepCount)
    }

    fun getDailyStepCount() = getInt(R.string.daily_step_count)

    fun storeLastDayStepCount(stepCount: Int) {
        storeInt(R.string.last_day_step_count, stepCount)
    }

    fun getLastDayStepCount() = getInt(R.string.last_day_step_count)

    fun storeDailyStepsGoal(goal: Int) {
        storeInt(R.string.daily_steps_goal, goal)
    }

    fun getDailyStepsGoal() = getInt(R.string.daily_steps_goal)

    fun storeLeafCountBeforeToday(leafCount: Int) {
        storeInt(R.string.leaf_count_before_today, leafCount)
    }

    private fun storeInt(key: Int, value: Int) {
        with(sharedPref.edit()) {
            putInt(context.getString(key), value)
            apply()
        }
    }

    private fun getInt(key: Int) = sharedPref.getInt(context.getString(key), 0)

    private fun storeBoolean(key: Int, value: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(context.getString(key), value)
            apply()
        }
    }

    private fun getBoolean(key: Int, defValue: Boolean)
            = sharedPref.getBoolean(context.getString(key), defValue)
}