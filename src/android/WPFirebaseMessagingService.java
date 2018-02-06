/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.co.itsm.plugin;

import android.app.NotificationChannel;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.wp.android.service.WPCallback;
import com.wp.android.service.WPClient;
import com.wp.android.service.WPException;
import com.wp.android.service.WPMessage;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Map;

public class WPFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "WPFirebaseMsgService";
    private static final String PREFERENCE_KEY = "kr.co.itsm.plugin.WPNotification";
    private static final String SOUND = "kr.co.itsm.plugin.WPNotification.SOUND";
    private static final String VIBRATE = "kr.co.itsm.plugin.WPNotification.VIBRATE";
    private static final String SNOOZE = "kr.co.itsm.plugin.WPNotification.SNOOZE";

    static final String KEY_ID = "id";
    static final String KEY_TITLE = "title";
    static final String KEY_TOPIC = "topic";
    static final String KEY_SUMMARY = "summary";
    static final String KEY_URL = "url";
    static final String KEY_IMG_URL = "imgUrl";
    static final String KEY_SEND_DATETIME = "sendDatetime";

    static final int SEND_MESSAGE = -2;

    static Handler mHandler;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "onMessageReceived From: " + remoteMessage.getFrom() + ", To:" + remoteMessage.getTo());

        if (remoteMessage.getData() != null) {
            Log.d(TAG, "data=" + remoteMessage.getData().toString());
            if (mHandler != null) {
                Message msg = mHandler.obtainMessage();
                msg.what = SEND_MESSAGE;
                msg.obj = remoteMessage.getData();
                mHandler.sendMessage(msg);
            }

            showNotification(remoteMessage.getData());
        }

    }

    private void showNotification(final Map<String, String> data) {
        final SharedPreferences sharedPref = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean(SNOOZE, true))
            return;
            
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WPMessage msg = new WPMessage();

                    if (data != null && data.containsKey(WPFirebaseMessagingService.KEY_ID) && data.containsKey(WPFirebaseMessagingService.KEY_URL)) {

                        msg.setId(Long.valueOf(data.get(WPFirebaseMessagingService.KEY_ID)));
                        msg.setTitle(data.get(WPFirebaseMessagingService.KEY_TITLE));
                        msg.setSummary(data.get(WPFirebaseMessagingService.KEY_SUMMARY));
                        msg.setUrl(data.get(WPFirebaseMessagingService.KEY_URL));
                        msg.setImgUrl(data.get(WPFirebaseMessagingService.KEY_IMG_URL));
                        msg.setSendDatetime(Long.valueOf(data.get(WPFirebaseMessagingService.KEY_SEND_DATETIME)));
                        msg.setReceiveDatetime(System.currentTimeMillis());

                    } else {
                        Log.e(TAG, "올바르지 않은 FCM 메세지 포멧:" + data);
                        return;
                    }

                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getUrl()));
                    PendingIntent pendingIntent = PendingIntent.getActivity(WPFirebaseMessagingService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(WPFirebaseMessagingService.this)
                            //.setSmallIcon(getApplicationInfo().icon)
                            .setSmallIcon(getResources().getIdentifier("ic_stat_pohang_", "drawable", getPackageName()))
                            .setContentTitle(msg.getTitle())
                            .setContentText(msg.getSummary())
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        String channelId = "some_channel_id";
                        CharSequence channelName = "Some Channel";
                        int importance = NotificationManager.IMPORTANCE_LOW;
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build();

                        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
                        if (sharedPref.getBoolean(SOUND, true))
                        {
                            notificationChannel.setSound(defaultSoundUri,audioAttributes);
                        }
                        if (sharedPref.getBoolean(VIBRATE, true))
                        {
                            notificationChannel.enableVibration(true);
                            notificationChannel.setVibrationPattern(new long[]{500, 200, 1000});
                        }

                        notificationChannel.enableLights(true);
                        notificationChannel.setLightColor(Color.RED);
                        nm.createNotificationChannel(notificationChannel);
                        notificationBuilder.setChannelId(channelId);
                    }



                    if (sharedPref.getBoolean(SOUND, true))
                        notificationBuilder.setSound(defaultSoundUri);
                    if (sharedPref.getBoolean(VIBRATE, true))
                        notificationBuilder.setVibrate(new long[]{500, 200, 1000});


                    if (msg.getImgUrl() != null && !msg.getImgUrl().isEmpty()) {
                        URL url = new URL(msg.getImgUrl());
                        URLConnection conn = url.openConnection();
                        conn.connect();
                        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                        Bitmap imgBitmap = BitmapFactory.decodeStream(bis);
                        bis.close();

                        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
                        style.setBigContentTitle(msg.getTitle());
                        style.setSummaryText(msg.getTitle());
                        style.bigPicture(imgBitmap);
                        notificationBuilder.setStyle(style);
                    }

                    nm.notify((int)(long)msg.getId() /* ID of notification */, notificationBuilder.build());
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }).start();
    }

}
