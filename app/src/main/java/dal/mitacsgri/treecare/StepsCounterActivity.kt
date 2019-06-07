package dal.mitacsgri.treecare

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_dummy_display.*



class StepsCounterActivity : AppCompatActivity(), SensorEventListener {

    private var steps = 0
    private var stepDetectorSensor : Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dummy_display)
        stepsCountText.text = "Steps: 0"

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if(stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor
        val values = event?.values
        var value = -1
        var countSD = 0

        if (values!!.isNotEmpty()) {
            value = values[0].toInt()
            Log.d("Value: ", "" + value)
        }

        if (sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepsDetectorText.text = (++countSD).toString()
        }

        if (sensor?.type == Sensor.TYPE_STEP_COUNTER) {


            stepsCountText.text = event.values[0].toString()
        }
    }
}
