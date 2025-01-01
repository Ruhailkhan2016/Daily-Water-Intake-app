package com.example.dailywaterintake;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WaterIntakePrefs";
    private static final String KEY_TOTAL_WATER = "totalWaterConsumed";
    private static final String KEY_DATE = "lastDate";
    private static final String KEY_FIRST_RUN = "isFirstRun";
    private static final String CHANNEL_ID = "WaterReminderChannel";

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private int totalWaterConsumed = 0;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        EditText edtWaterIntake = findViewById(R.id.edtWaterIntake);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        EditText edtReminderInterval = findViewById(R.id.edtReminderInterval);
        Button btnSetReminder = findViewById(R.id.btnSetReminder);
        Switch switchReminder = findViewById(R.id.switchReminder);
        TextView tvTotalConsumed = findViewById(R.id.tvTotalConsumed);
        TextView tvMessage = findViewById(R.id.tvMessage);

        // Initialize AlarmManager and DatabaseHelper
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        databaseHelper = new DatabaseHelper(this);

        // Create Notification Channel for Vibration
        createNotificationChannel();

        // Check Notification Settings
        checkAndRequestNotificationPermission();

        // Request POST_NOTIFICATIONS permission for Android 13+
        requestPostNotificationsPermission();

        // SharedPreferences for storing water intake
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // First Run Initialization
        if (preferences.getBoolean(KEY_FIRST_RUN, true)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(KEY_TOTAL_WATER, 0); // Reset total water consumed
            editor.putString(KEY_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())); // Set today's date
            editor.putBoolean(KEY_FIRST_RUN, false); // Mark as not the first run
            editor.apply();
        }

        // Handle daily reset
        String savedDate = preferences.getString(KEY_DATE, "");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!savedDate.equals(currentDate)) {
            totalWaterConsumed = 0;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_DATE, currentDate);
            editor.putInt(KEY_TOTAL_WATER, totalWaterConsumed);
            editor.apply();
        } else {
            totalWaterConsumed = preferences.getInt(KEY_TOTAL_WATER, 0);
        }

        tvTotalConsumed.setText("Total water consumed: " + totalWaterConsumed + " ml");

        // Add Water Button
        btnAdd.setOnClickListener(v -> {
            String input = edtWaterIntake.getText().toString();

            if (input.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a valid amount!", Toast.LENGTH_SHORT).show();
                return;
            }

            int waterIntake = Integer.parseInt(input);
            totalWaterConsumed += waterIntake;

            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(KEY_TOTAL_WATER, totalWaterConsumed);
            editor.apply();

            databaseHelper.insertOrUpdateWaterIntake(currentDate, totalWaterConsumed);

            tvTotalConsumed.setText("Total water consumed: " + totalWaterConsumed + " ml");

            if (totalWaterConsumed < 1500) {
                tvMessage.setText("Drink more water");
            } else if (totalWaterConsumed >= 1500 && totalWaterConsumed < 2500) {
                tvMessage.setText("Good Job! You're staying hydrated");
            } else {
                tvMessage.setText("You're Drinking enough water");
            }

            // Clear the input field after adding
            edtWaterIntake.setText("");
        });

        // View History Button
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Set Reminder Button
        btnSetReminder.setOnClickListener(v -> {
            String intervalInput = edtReminderInterval.getText().toString();

            if (intervalInput.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a valid interval!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int intervalMinutes = Integer.parseInt(intervalInput);

                if (intervalMinutes <= 0) {
                    Toast.makeText(MainActivity.this, "Interval must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!canScheduleExactAlarms()) {
                    Toast.makeText(MainActivity.this, "Exact alarms are not allowed. Enable them in settings.", Toast.LENGTH_SHORT).show();
                    requestExactAlarmPermission();
                    return;
                }

                // Convert minutes to milliseconds
                long intervalMillis = intervalMinutes * 60 * 1000;

                // Debugging Log
                Log.d("MainActivity", "Setting Reminder: Interval " + intervalMinutes + " minutes");

                // Create Intent for ReminderReceiver
                Intent intent = new Intent(MainActivity.this, ReminderReceiver.class);
                pendingIntent = PendingIntent.getBroadcast(
                        MainActivity.this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Schedule Alarm
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + intervalMillis,
                        pendingIntent
                );

                Toast.makeText(MainActivity.this, "Reminder set successfully!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid input! Please enter a valid number.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("ReminderError", "Error setting reminder", e);
                Toast.makeText(MainActivity.this, "An error occurred. Check logs.", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle Reminder Switch
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(MainActivity.this, "Reminder Enabled", Toast.LENGTH_SHORT).show();
            } else {
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                }
                Toast.makeText(MainActivity.this, "Reminder Disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Create Notification Channel with Vibration
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Water Reminder",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for water intake reminders");
            channel.enableLights(true);
            channel.enableVibration(true);

            // Vibration pattern: vibrate for 6 seconds
            channel.setVibrationPattern(new long[]{0, 6000});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Check Notification Settings
    private void checkAndRequestNotificationPermission() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!notificationManager.areNotificationsEnabled()) {
                Toast.makeText(this, "Notifications are disabled. Please enable them in settings.", Toast.LENGTH_LONG).show();

                // Redirect to App Notification Settings
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            }
        }
    }

    // Request POST_NOTIFICATIONS permission for Android 13+
    private void requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityResultLauncher<String> requestPermissionLauncher =
                        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                            if (!isGranted) {
                                Toast.makeText(this, "Notifications permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
                            }
                        });

                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // Check if exact alarms are allowed
    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 or above
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // For lower Android versions
    }

    // Redirect user to enable exact alarms in settings
    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    // Disable Battery Optimization
    private void disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        }
    }
}
