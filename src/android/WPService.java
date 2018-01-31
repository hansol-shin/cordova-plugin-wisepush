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

import android.app.NotificationChannel;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.graphics.Color;
import android.media.AudioAttributes;


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

    
public class WPService extends JobService {
    private static final int JOB_ID = 1;

    private NotificationCallback notificationCallback = null;
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        WPService getService() {
            return WPService.this;
        }
    }
    public interface NotificationCallback {
        void onMessageReceived(WPMessage msg);
    }

    private static final String TAG = "WPService";
    private static final String SERVER_URI = "tcp://wp.hssa.me:48702";
    // private static final String SERVER_URI = "tcp://itsmpohang.hssa.me:48702";
    private WPClient mClient = null;
    private static WPService minstance;

    private static final String PREFERENCE_KEY = "kr.co.itsm.plugin.WPNotification";
    private static final String SNOOZE = "kr.co.itsm.plugin.WPNotification.SNOOZE";
    private static final String SOUND = "kr.co.itsm.plugin.WPNotification.SOUND";
    private static final String VIBRATE = "kr.co.itsm.plugin.WPNotification.VIBRATE";

    public WPService() {
        if(minstance == null)
            minstance = WPService.this;
    }

    public static WPService schedule(Context context) {
        ComponentName component = new ComponentName(context, WPService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setPersisted (true)
                .setMinimumLatency(1)
                .setOverrideDeadline(1);
 
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(jobScheduler.getAllPendingJobs().size() > 0)
        {
            jobScheduler.cancelAll();
        }
        jobScheduler.schedule(builder.build());

        if(minstance == null)
            new WPService();

         return minstance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        connect();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // whether or not you would like JobScheduler to automatically retry your failed job.

        schedule(getApplicationContext());
        return false;
    }
                                    

    @Override
    public void onTaskRemoved(Intent rootIntent) {
                                    
                                    
        schedule(getApplicationContext());
        super.onTaskRemoved(rootIntent);
    }  

                                            
                                            
    public void connect() {
        if (minstance.mClient != null && minstance.mClient.isConnected())
            return;

        minstance.mClient = new WPClient(getApplicationContext(), SERVER_URI, new WPCallback() {
            
            @Override
            public void onError(WPException e) {
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onConnect(String serverURI) {
                Log.d(TAG, "connect to " + serverURI);
                String[] topics = {"topics/all", "topics/android"};
                minstance.mClient.subscribe(topics);
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
                    minstance.notificationCallback.onMessageReceived(msg);
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

                            nm.notify((int)(long)msg.getId() /* ID of notification */, notificationBuilder.build());
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

        minstance.updateDevice();
    }
                            

    public void setNotificationCallback(NotificationCallback cb) {
        minstance.notificationCallback = cb;
    }

    public boolean isConnected() {
        return minstance.mClient.isConnected();
    }

    public String getClientId() throws WPException {
        return minstance.mClient.getClientId();
    }

    public String getDeviceId() throws WPException {
        return minstance.mClient.getDeviceId();
    }

    public void subscribeToTopic(String[] topics) throws WPException {
        minstance.mClient.subscribe(topics);
    }

    public void unsubscribeFromTopic(String[] topics) throws WPException {
        minstance.mClient.unsubscribe(topics);
    }

    private void updateDevice() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://smart.pohang.go.kr/api/device");

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestMethod("PUT");

                    PackageInfo i = getPackageManager().getPackageInfo(getPackageName(), 0);

                    JSONObject body = new JSONObject();
                    Log.d(TAG, minstance.mClient.getClientId());
                    body.put("clientId", minstance.mClient.getClientId());
                    String fcmToken = mClient.getDeviceId();
                    if (fcmToken.equals(""))
                        return;


                    body.put("deviceId", minstance.mClient.getDeviceId());
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
