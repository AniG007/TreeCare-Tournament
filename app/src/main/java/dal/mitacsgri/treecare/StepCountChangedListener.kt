package dal.mitacsgri.treecare

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.koin.core.KoinComponent
import org.koin.core.inject

class StepCountChangedListener(private val context: Context): SensorEventListener, KoinComponent {

    private val sharedPrefsRepository: SharedPreferencesRepository by inject()

    private var lastStepCount = 0
    private var isFirstRun = true
    private var stepCountDelta = 0

    init {
        Log.d("Sensor listener", "Created")
        "Created".toast(context)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor
        //"Step".toast(context)
        if (event != null)
            stepCountDelta = event.values[0].toInt() - lastStepCount
        sharedPrefsRepository.storeDailyStepCount(
            sharedPrefsRepository.getDailyStepCount() + stepCountDelta)
        sharedPrefsRepository.getDailyStepCount().toast(context)

//        if (sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
//            if (isFirstRun) lastStepCount = event.values[0].toInt()
//            else {
//                isFirstRun = false
//                stepCountDelta = event.values[0].toInt() - lastStepCount
//                sharedPrefsRepository.storeDailyStepCount(
//                    sharedPrefsRepository.getDailyStepCount() + stepCountDelta)
//                sharedPrefsRepository.getDailyStepCount().toast(context)
//                Log.d("Step detector", sharedPrefsRepository.getDailyStepCount().toString())
//                "Step detector: " + sharedPrefsRepository.getDailyStepCount().toast(context)
//            }
//        }
    }
}