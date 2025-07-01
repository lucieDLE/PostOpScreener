package org.rti.ttfinder.utils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.rti.ttfinder.MyApplication;
import org.rti.ttfinder.R;

import java.util.Arrays;

public class NotificationUtils {

    public static void updateNotification(int progress, String prefixMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.getContext(), "tt_screener")
                .setContentTitle(progress == 100 ? prefixMessage + " grading completed" : prefixMessage + " grading in progress")
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setOngoing(progress != 100);

        if (progress != 100) {
            builder.setProgress(100, progress, false);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MyApplication.getContext());
        notificationManager.notify(1, builder.build());
    }

}
