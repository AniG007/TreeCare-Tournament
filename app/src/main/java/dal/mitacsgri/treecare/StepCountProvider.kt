//package dal.mitacsgri.treecare
//
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.util.Log
//
//class StepCountProvider : SensorEventListener {
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//
//    }
//
//    override fun onSensorChanged(event: SensorEvent?) {
//        val sensor = event?.sensor
//        val values = event?.values
//        var value = -1
//
//        if (values!!.isNotEmpty()) {
//            value = values[0].toInt()
//            Log.d("Sensor value: ", "" + value)
//            Log.d("Sensor type: ", "" + sensor)
//        }
//
//        if (sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
//            steps++
//            stepsCountText.text = "Steps: $steps"
//        }
//    }
//}