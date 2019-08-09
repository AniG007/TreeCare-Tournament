package dal.mitacsgri.treecare.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.StepCountChangedListener
import dal.mitacsgri.treecare.consts.STEP_MONITOR_SERVICE_NOTIF_CHANNEL_ID
import dal.mitacsgri.treecare.screens.MainActivity

class StepDetectorService: Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, STEP_MONITOR_SERVICE_NOTIF_CHANNEL_ID)
            .setContentTitle("TreeCare")
            .setContentText("Working towards keeping you fit")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounter != null) {
            sensorManager.registerListener(
                StepCountChangedListener(Sensor.TYPE_STEP_DETECTOR), stepCounter, SensorManager.SENSOR_DELAY_FASTEST)
        } else if (stepDetector != null) {
            sensorManager.registerListener(
                StepCountChangedListener(Sensor.TYPE_STEP_COUNTER), stepDetector, SensorManager.SENSOR_DELAY_FASTEST)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                STEP_MONITOR_SERVICE_NOTIF_CHANNEL_ID,
                "Step Monitor Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.apply {
                setSound(null, null)
                enableVibration(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}