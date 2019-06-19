package dal.mitacsgri.treecare.screens.splash


import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.provider.SharedPreferencesRepository
import dal.mitacsgri.treecare.provider.StepCountRepository
import java.util.*

class SplashScreenFragment : Fragment() {

    private val SPLASH_SCREEN_DELAY = 5000L
    private lateinit var sharedPrefProvider: SharedPreferencesRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_splash_screen, container, false)

        sharedPrefProvider = SharedPreferencesRepository(view.context)

        sharedPrefProvider.apply {

            storeDailyStepsGoal(5000)

            //testGameByManipulatingSharedPrefsData(this)
            resetDailyGoalCheckedFlag(sharedPrefProvider)

            if (isLoginDone) setupFitApiToGetData(view.context)

            if (isLoginDone) navigateWithDelay(R.id.action_splashScreenFragment_to_modeSelectionFragment)
            else navigateWithDelay(R.id.action_splashScreenFragment_to_loginFragment)
        }
        return view;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}

    private fun navigateWithDelay(actionResId: Int, delay: Long = SPLASH_SCREEN_DELAY) {
        Handler().postDelayed({
            findNavController().navigate(actionResId)
        }, delay)
    }

    private fun resetDailyGoalCheckedFlag(sharedPrefProviderParam: SharedPreferencesRepository) {
        //Will execute only once in each day, when the app is opened for thr first time in the day
        if (sharedPrefProviderParam.lastOpenedDayPlus1 < Date().time) {
            sharedPrefProviderParam.dailyGoalChecked(0)

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

            sharedPrefProviderParam.lastOpenedDayPlus1 = cal.timeInMillis
        }
    }

    private fun setupFitApiToGetData(context: Context) {

        var authInProgress = false
        val SIGN_IN_CODE = 1000
        var mClient: GoogleApiClient? = null
        val stepCountProvider = StepCountRepository(context)

        val connectionFailedImpl = GoogleApiClient.OnConnectionFailedListener {
            if (!authInProgress) {
                try {
                    authInProgress = true
                    it.startResolutionForResult(activity, SIGN_IN_CODE)
                } catch (e: IntentSender.SendIntentException) {

                }
            } else {
                "Logging you in".toast(context)
            }
        }

        mClient = GoogleApiClient.Builder(context)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(object: GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    stepCountProvider.apply {
                        getTodayStepCountData(mClient!!) {
                            sharedPrefProvider.storeDailyStepCount(it)
                            Log.d("DailyStepCount", it.toString())
                            //startNextActivity(UnityPlayerActivity::class.java)
                        }

                        getLastDayStepCountData(mClient!!) {
                            sharedPrefProvider.storeLastDayStepCount(it)
                            Log.d("LastDayStepCount", it.toString())
                        }

                    }
                }

                override fun onConnectionSuspended(p0: Int) {}
            })
            .addOnConnectionFailedListener(connectionFailedImpl)
            .build()
        mClient.connect()
    }

    private fun testGameByManipulatingSharedPrefsData(sharedPrefsProvider: SharedPreferencesRepository) {
        sharedPrefsProvider.apply {
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
