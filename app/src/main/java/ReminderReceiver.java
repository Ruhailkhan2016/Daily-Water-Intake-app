package com.example.dailywaterintake;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "WaterReminderChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderReceiver", "Reminder triggered");

        // Create a notification manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create or update the notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Water Reminder",
                    NotificationManager.IMPORTANCE_HIGH // High priority
            );
            channel.setDescription("Channel for water intake reminders");
            channel.enableLights(true); // Enable LED light notifications
            channel.enableVibration(true); // Enable vibration
            channel.setVibrationPattern(new long[]{0, 500, 200, 500}); // Custom vibration pattern
            notificationManager.createNotificationChannel(channel);
        }

        // Create an Intent to open the MainActivity when the notification is clicked
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app icon
                .setContentTitle("Drink Water Reminder") // Notification title
                .setContentText("It's time to drink water and stay hydrated!") // Notification content
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for pre-Oreo devices
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable default sound, vibration, and lights
                .setAutoCancel(true) // Automatically cancel the notification when tapped
                .setContentIntent(pendingIntent); // Attach the PendingIntent to the notification

        // Show the notification
        notificationManager.notify(1, builder.build());
    }
}
