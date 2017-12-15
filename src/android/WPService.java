package kr.co.itsm.plugin;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.wp.android.service.WPCallback;
import com.wp.android.service.WPClient;
import com.wp.android.service.WPException;
import com.wp.android.service.WPMessage;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class WPService extends Service {
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        WPService getService() {
            return WPService.this;
        }
    }
    private NotificationCallback notificationCallback = null;
    public interface NotificationCallback {
        void onMessageReceived(WPMessage msg);
    }

    private static final String TAG = "WPService";
    private static final String SERVER_URI = "tcp://wp.hssa.me:48702";
    private WPClient mClient = null;

    private static final String PREFERENCE_KEY = "kr.co.itsm.plugin.WPNotification";
    private static final String SNOOZE = "kr.co.itsm.plugin.WPNotification.SNOOZE";
    private static final String SOUND = "kr.co.itsm.plugin.WPNotification.SOUND";
    private static final String VIBRATE = "kr.co.itsm.plugin.WPNotification.VIBRATE";

//    private NotificationCompat.Builder getNotification(){
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
//        return builder.setContentTitle("test")
//        .setContentText("test")//\n"+app.getPlan().getUsage())
//        .setSmallIcon(getApplicationInfo().icon);
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        Intent restartServiceTask = new Intent(getApplicationContext(),this.getClass());
        restartServiceTask.setPackage(getPackageName());
        PendingIntent restartPendingIntent =PendingIntent.getService(getApplicationContext(), 1,restartServiceTask, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager myAlarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        myAlarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent);

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    @Override
    public void onCreate(){
        unregisterRestartAlarm();
        connect();
        super.onCreate();
    }

    public void connect() {
        if (mClient != null && mClient.isConnected())
            return;

        mClient = new WPClient(getApplicationContext(), SERVER_URI, new WPCallback() {
            
            @Override
            public void onError(WPException e) {
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onConnect(String serverURI) {
                Log.d(TAG, "connect to " + serverURI);
                String[] topics = {"topics/all", "topics/android"};
                mClient.subscribe(topics);
            }

            @Override
            public void onDisconnect() {
                Log.d(TAG, "disconnected...");
            }

            @Override
            public void onReceived(final WPMessage msg) {
                final SharedPreferences sharedPref = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (!sharedPref.getBoolean(SNOOZE, true))
                    return;

                Log.d(TAG, "received msg:" + msg.toString());
                if (notificationCallback != null) {
                    notificationCallback.onMessageReceived(msg);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getUrl()));
                            PendingIntent pendingIntent = PendingIntent.getActivity(WPService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(WPService.this)
                                    // .setSmallIcon(getApplicationInfo().icon)
                                    .setSmallIcon(getResources().getIdentifier("ic_stat_pohang_", "drawable", getPackageName()))
                                    .setContentTitle(msg.getTitle())
                                    .setContentText(msg.getSummary())
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent);


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

                            nm.notify(0 /* ID of notification */, notificationBuilder.build());
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage());
                        }
                    }
                }).start();
            }

            @Override
            public void onSubscribe(String[] topics) {
                Log.d(TAG, "subscribe " + Arrays.toString(topics));
            }

            @Override
            public void onUnsubscribe(String[] topics) {
                Log.d(TAG, "unsubscribe " + Arrays.toString(topics));
            }

            @Override
            public void onChangeDeviceId(String deviceId) {
                Log.d(TAG, "change deviceId:" + deviceId);
                WPService.this.updateDevice();
            }
        });

        this.updateDevice();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(TAG, "Service Destroy");
        registerRestartAlarm(); // 서비스가 죽을 때 알람 등록
        super.onDestroy();
    }

    void registerRestartAlarm() {
        Log.d(TAG, "registerRestartAlarm");
        Intent intent = new Intent(WPService.this, WPStartUp.class);
        intent.setAction("ACTION.Restart.WPService");
        PendingIntent sender = PendingIntent.getBroadcast(
                WPService.this, 0, intent, 0); // 브로드케스트할 Intent
        long firstTime = SystemClock.elapsedRealtime();  // 현재 시간
        firstTime += 1 * 1000; // 10초 후에 알람이벤트 발생
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE); // 알람 서비스 등록
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                10 * 1000, sender); // 알람이
    }

    void unregisterRestartAlarm() {
        Log.d(TAG, "unregisterRestartAlarm");
        Intent intent = new Intent(WPService.this, WPStartUp.class);
        intent.setAction("ACTION.Restart.WPService");
        PendingIntent sender = PendingIntent.getBroadcast(
                WPService.this, 0, intent, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    public void setNotificationCallback(NotificationCallback cb) {
        this.notificationCallback = cb;
    }

    public boolean isConnected() {
        return mClient.isConnected();
    }

    public String getClientId() throws WPException {
        return mClient.getClientId();
    }

    public String getDeviceId() throws WPException {
        return mClient.getDeviceId();
    }

    public void subscribeToTopic(String[] topics) throws WPException {
        mClient.subscribe(topics);
    }

    public void unsubscribeFromTopic(String[] topics) throws WPException {
        mClient.unsubscribe(topics);
    }

    private void updateDevice() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://smart.pohang.go.kr:9001/device");

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestMethod("PUT");

                    PackageInfo i = getPackageManager().getPackageInfo(getPackageName(), 0);

                    JSONObject body = new JSONObject();
                    Log.d(TAG, mClient.getClientId());
                    body.put("clientId", mClient.getClientId());
                    String fcmToken = mClient.getDeviceId();
                    if (fcmToken.equals(""))
                        return;
                    body.put("deviceId", mClient.getDeviceId());
                    body.put("devicePlatform", "ANDROID");
                    body.put("deviceModel", Build.MODEL);
                    body.put("appVer", i.versionName);

                    OutputStream os = urlConnection.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int code = urlConnection.getResponseCode();
                    Log.d(TAG, "REGISTER DEVICE: " + code);

                    urlConnection.disconnect();

                } catch (Exception e) {

                }
            }
        }).start();
    }
}
