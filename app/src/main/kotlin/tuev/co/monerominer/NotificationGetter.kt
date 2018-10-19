package tuev.co.monerominer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import tuev.co.tumine.NotificationObjectGetterTemplate

public class NotificationGetter: NotificationObjectGetterTemplate() {
    override fun getNotification(context: Context): Notification? {
        val channelId2 = "miner_info"
        val mNotifyManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelRetr2: NotificationChannel?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannelRetr2 = mNotifyManager.getNotificationChannel(channelId2)
            if (notificationChannelRetr2 == null) {
                val notificationChannel: NotificationChannel
                val channelName = "Info"
                val importance = NotificationManager.IMPORTANCE_MIN
                notificationChannel = NotificationChannel(channelId2, channelName, importance)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                mNotifyManager.createNotificationChannel(notificationChannel)
            }
        }


        val openActivityIntent = PendingIntent.getActivity(context, 52344, Intent(context, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT)
        val notification = NotificationCompat.Builder(context, channelId2)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Info")
                .setContentIntent(openActivityIntent)
                .setContentText("This mines XMR for the hardworking devs").build()
        return notification
    }
}