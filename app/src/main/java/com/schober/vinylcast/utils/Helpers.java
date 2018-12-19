package com.schober.vinylcast.utils;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.Formatter;

import com.schober.vinylcast.R;
import com.schober.vinylcast.service.MediaRecorderService;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Allen on 3/26/17.
 */

public class Helpers {

    private Helpers(){}	// not to be instantiated

    public static void createStopNotification(MediaSessionCompat mediaSession, Service context, Class<?> serviceClass, int NOTIFICATION_ID) {

        PendingIntent stopIntent = PendingIntent
                .getService(context, 0, getIntent(MediaRecorderService.REQUEST_TYPE_STOP, context, serviceClass),
                        PendingIntent.FLAG_CANCEL_CURRENT);

        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("my_service", "My Background Service", context);
        }

        // Start foreground service to avoid unexpected kill
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setDeleteIntent(stopIntent)
                // Add a pause button
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_stop_black_24dp, context.getString(R.string.stop),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_STOP)))
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_STOP)))
                .setSmallIcon(R.drawable.ic_album_black_24dp)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        context.startForeground(NOTIFICATION_ID, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static String createNotificationChannel(String channelId, String channelName, Context context) {
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(chan);
        return channelId;
    }

    public static Intent getIntent(String requestType, Context con, Class<?> serviceClass) {
        Intent intent = new Intent(con, serviceClass);
        intent.putExtra(MediaRecorderService.REQUEST_TYPE, requestType);
        return intent;
    }

    public static String getIpAddress(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }
}
