package dal.mitacsgri.treecare.backgroundtasks.jobs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobRequest
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.DAILY_GOAL_NOTIFICATION_CHANNEL_ID
import dal.mitacsgri.treecare.consts.FCM_NOTIFICATION_CHANNEL_ID
import dal.mitacsgri.treecare.extensions.getMapFormattedDate
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.repository.StepCountRepository
import dal.mitacsgri.treecare.screens.MainActivity
import expandDailyGoalMapIfNeeded
import org.joda.time.DateTime
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.random.Random

class DailyGoalNotificationJob: DailyJob(), KoinComponent {

    private val sharedPrefsRepository: SharedPreferencesRepository by inject()
    private val stepCountRepository: StepCountRepository by inject()

    companion object {

        const val TAG = "DailyGoalNotificationJob"

        fun scheduleJob(startTime: Long, endTime: Long) {
            schedule(JobRequest.Builder(TAG), startTime,endTime)
        }
    }

    override fun onRunDailyJob(p0: Params): DailyJobResult {

        createNotificationChannel()

        stepCountRepository.getTodayStepCountData {
            createNotification(it)
        }

        return DailyJobResult.SUCCESS
    }

    private fun createNotification(remainingSteps: Int) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title= getNotificationTitle()
        val body = getNotificationBody(remainingSteps)

        val notificationBuilder = NotificationCompat.Builder(context, FCM_NOTIFICATION_CHANNEL_ID)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun getNotificationBody(remainingSteps: Int): CharSequence {
        return if (remainingSteps > 0)
            buildSpannedString {
                append("You are ")
                bold {
                    append("$remainingSteps steps ")
                }
                append("away from achieving your daily goal")
            }
        else
            context.getString(R.string.goal_completed_notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                DAILY_GOAL_NOTIFICATION_CHANNEL_ID,
                "Daily goal notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getRemainingDailyGoalSteps(): Int {
        expandDailyGoalMapIfNeeded(sharedPrefsRepository.user.dailyGoalMap)
        val dailyGoal = sharedPrefsRepository.user
            .dailyGoalMap[DateTime().getMapFormattedDate()] ?: 5000
        val dailyStepCount = sharedPrefsRepository.getDailyStepCount()

        return if (dailyGoal > dailyStepCount) dailyGoal - dailyStepCount else 0
    }

    private fun getNotificationTitle(): String {
        val titlesArray = arrayListOf<String>()
        titlesArray.add(context.getString(R.string.daily_goal_notification_title1))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title2))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title3))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title4))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title5))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title6))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title7))
        titlesArray.add(context.getString(R.string.daily_goal_notification_title8))

        return titlesArray[Random.nextInt(0, 8)]
    }
}