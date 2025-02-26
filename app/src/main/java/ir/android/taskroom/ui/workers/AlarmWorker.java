package ir.android.taskroom.ui.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Locale;
import java.util.Random;

import ir.android.taskroom.R;
import ir.android.taskroom.ui.activity.AlarmActivity;
import ir.android.taskroom.ui.activity.MainActivity;
import ir.android.taskroom.ui.activity.NotificationActivity;
import ir.android.taskroom.utils.EnglishInit;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AlarmWorker extends Worker {
    public static Ringtone ringtone;
    public NotificationManager mNotifyManager;
    private static final String PRIMARY_CHANNEL_ID =
            "primary_notification_channel";

    public AlarmWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        createVibrate();
        Integer reminderType = getInputData().getInt("reminderType", 0);
        switch (reminderType) {
            case 0:
                createNotificationChannel();
                startNotification();
                break;
            case 1:
                startAlarmActivity();
                break;
        }
        return Result.success();
    }

    private void createVibrate() {
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(1000);
        }
    }

    private void startNotification() {
        int notificationId = new Random().nextInt(); // just use a counter in some util class...
        PendingIntent dismissIntent = NotificationActivity.getDismissIntent(notificationId, getApplicationContext());

        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder
                (getApplicationContext(), PRIMARY_CHANNEL_ID)
                .setContentTitle(getInputData().getString("alarmTitle"))
                .setContentText(EnglishInit.getCurrentTime())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getInputData().getString("alarmExpandableText")))
//                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.splash_img))
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(R.drawable.ic_red_close, getApplicationContext().getResources().getString(R.string.done), dismissIntent)
                .setOngoing(true);

        mNotifyManager.notify(notificationId, builder.build());
    }

    private void startAlarmActivity() {
        Intent i = new Intent(getApplicationContext(), AlarmActivity.class);
        if (Build.MANUFACTURER.toLowerCase(Locale.ROOT).equals("xiaomi")) {
            i.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            i.putExtra("extra_pkgname", getApplicationContext().getPackageName());
        }
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        i.putExtra("alarmTitle", getInputData().getString("alarmTitle"));
        getApplicationContext().startActivity(i);

    }

    public void createNotificationChannel() {

        // Create a notification manager object.
        mNotifyManager =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

        // Notification channels are only available in OREO and higher.
        // So, add a check on SDK version.
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {

            // Create the NotificationChannel with all the parameters.
            NotificationChannel notificationChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_ID,
                            getApplicationContext().getString(R.string.job_service_notification),
                            NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription
                    (getApplicationContext().getString(R.string.notification_channel_description));

            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

}
