package dal.mitacsgri.treecare

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.koin.core.KoinComponent
import org.koin.core.inject

class StepCountChangedListener(private val context: Context): SensorEventListener, KoinComponent {

    private val sharedPrefsRepository: SharedPreferencesRepository by inject()

    private var lastStepCount = 0
    private var stepCountDelta = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor

        if (sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCountDelta = event.values[0].toInt() - lastStepCount
            sharedPrefsRepository.storeDailyStepCount(
                sharedPrefsRepository.getDailyStepCount() + stepCountDelta
            )
        }
    }
}