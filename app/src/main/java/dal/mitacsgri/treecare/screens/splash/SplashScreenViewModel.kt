package dal.mitacsgri.treecare.screens.splash

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.firebase.auth.FirebaseAuth
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import java.util.*

/**
 * Created by Devansh on 20-06-2019
 */

class SplashScreenViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val stepCountRepository: StepCountRepository)
    : ViewModel() {

    val isLoginDone = sharedPrefsRepository.isLoginDone
    private lateinit var mClient: GoogleApiClient

    //This variable is accessed synchronously. The moment its value reaches 2, we move to new fragment
    //Value 2 means both the steps counts have been obtained
    val stepCountDataFetchedCounter = MutableLiveData<Int>().default(0)

    fun storeDailyStepsGoal(goal: Int) {
        sharedPrefsRepository.storeDailyStepsGoal(goal)
    }

    fun resetDailyGoalCheckedFlag() {
        //Will execute only once in each day, when the app is opened for thr first time in the day
        if (sharedPrefsRepository.lastOpenedDayPlus1 < Date().time) {
            sharedPrefsRepository.isDailyGoalChecked = 0

            val cal = Calendar.getInstance()
            val now = Date()
            cal.apply {
                time = now
                set(Calendar.MILLISECOND, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR, 0)
            }

            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH)

            //Doing this to prevent rounding off at the end of the year
            if (day == 31 && month == 12) cal.add(Calendar.YEAR, 1)

            cal.add(Calendar.DAY_OF_YEAR, 1)

            Log.v("Current time: ", Date().time.toString())
            Log.v("Time to store: ", cal.timeInMillis.toString())

            //This will delay the resetting of flag by 15 minutes, as Google Fit takes some time to update step count
            sharedPrefsRepository.lastOpenedDayPlus1 = cal.timeInMillis + 15_000*60
        }
    }

    fun setupFitApiToGetData(context: Context) {

        val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
            Log.e("Connection failed", it.toString())
        }

        mClient = GoogleApiClient.Builder(context)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .setAccountName(FirebaseAuth.getInstance().currentUser?.email)
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    stepCountRepository.apply {

                        getTodayStepCountData(mClient) {
                            sharedPrefsRepository.storeDailyStepCount(it)
                            Log.d("DailyStepCount", it.toString())
                            increaseStepCountDataFetchedCounter()
                        }

                        //Execute only once each day, dailyGoalChecked will be set as true(1) by Unity
                        //Execute once to prevent updating lastLeafCount every time
                        if (sharedPrefsRepository.isDailyGoalChecked == 0) {
                            //Get aggregate step count up to the last day
                            getStepCountDataOverARange(
                                mClient,
                                sharedPrefsRepository.lastLoginTime,
                                DateTime().withTimeAtStartOfDay().millis
                            ) {

                                sharedPrefsRepository.lastLeafCount =
                                    calculateLeafCountFromStepCount(it, 5000)
                            }
                        }

                        //Get aggregate leaf count up to today
                        getStepCountDataOverARange(mClient,
                            sharedPrefsRepository.lastLoginTime,
                            DateTime().plusDays(1).withTimeAtStartOfDay().millis) {

                            val leafCount = calculateLeafCountFromStepCount(it, 5000)
                            sharedPrefsRepository.currentLeafCount = leafCount
                            Log.d("Current leaf count", leafCount.toString())
                            increaseStepCountDataFetchedCounter()
                        }
                    }
                }
                override fun onConnectionSuspended(p0: Int) {}
            })
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()
    }

    private inline fun increaseStepCountDataFetchedCounter() {
        synchronized(stepCountDataFetchedCounter) {
            stepCountDataFetchedCounter.value = stepCountDataFetchedCounter.value?.plus(1)
            Log.d("Counter value", stepCountDataFetchedCounter.value.toString())
        }
    }

    private fun calculateLeafCountFromStepCount(stepCount: Int, dailyGoal: Int): Int {
        var leafCount = stepCount / 1000
        if (stepCount < dailyGoal) {
            leafCount -= Math.ceil((dailyGoal - stepCount) / 1000.0).toInt()
            if (leafCount < 0) leafCount = 0
        }
        return leafCount
    }

    private fun testGameByManipulatingSharedPrefsData(sharedPrefsRepository: SharedPreferencesRepository) {
        sharedPrefsRepository.apply {
            storeDailyStepsGoal(5000)
            //storeLastDayStepCount(0)
            //storeDailyStepCount(8000)
            //dailyGoalChecked(0)
//            storeLeafCountBeforeToday(8)
//            storeLastDayStepCount(8000)

//            with(sharedPref.edit()) {
//                //putInt(getString(R.string.leaf_count_before_today), 50)
//                //putInt(getString(R.string.is_first_run), 1)
//                putInt(getString(R.string.leaves_gained_today), 6)
//
//                putString(getString(R.string.goal_achieved_streak), "1111111")
//                storeInt(R.string.current_day, 6)
//                storeInt(R.string.total_fruits_on_tree, 8)
//
//                apply()
//            }
        }
    }

}