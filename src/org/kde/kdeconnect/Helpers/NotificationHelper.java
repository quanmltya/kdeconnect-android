package org.kde.kdeconnect.Helpers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;

import org.kde.kdeconnect.UserInterface.MaterialActivity;

public class NotificationHelper {

    public static void notifyCompat(NotificationManager notificationManager, int notificationId, Notification notification) {
        try {
            notificationManager.notify(notificationId, notification);
        } catch(Exception e) {
            //4.1 will throw an exception about not having the VIBRATE permission, ignore it.
            //https://android.googlesource.com/platform/frameworks/base/+/android-4.2.1_r1.2%5E%5E!/
        }
    }

    public static void notifyCompat(NotificationManager notificationManager, String tag, int notificationId, Notification notification) {
        try {
            notificationManager.notify(tag, notificationId, notification);
        } catch(Exception e) {
            //4.1 will throw an exception about not having the VIBRATE permission, ignore it.
            //https://android.googlesource.com/platform/frameworks/base/+/android-4.2.1_r1.2%5E%5E!/
        }
    }

    public static PendingIntent createPendingIntentForActivity(Context context, Class<? extends Activity> activity) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(activity);
        stackBuilder.addNextIntent(new Intent(context, activity));
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        //TODO: Investigate why we are doing all this instead of just:
        //Intent intent = new Intent(context, activity);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        return pendingIntent;

    }
}
