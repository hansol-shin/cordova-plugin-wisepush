package kr.co.itsm.plugin;

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

import kr.go.pohang.smart.MainActivity;

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
    private static final String SERVER_URI = "tcp://itsmpohang.hssa.me:48702";
    private WPClient mClient;

    private static final String PREFERENCE_KEY = "kr.co.itsm.plugin.WPNotification";
    private static final String SOUND = "kr.co.itsm.plugin.WPNotification.SOUND";
    private static final String VIBRATE = "kr.co.itsm.plugin.WPNotification.VIBRATE";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    @Override
    public void onCreate(){
        mClient = new WPClient(getApplicationContext(), SERVER_URI, new WPCallback() {

            @Override
            public void onError(WPException e) {
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onConnect(String serverURI) {
                Log.d(TAG, "connect to " + serverURI);
            }

            @Override
            public void onDisconnect() {
                Log.d(TAG, "disconnected...");
            }

            @Override
            public void onReceived(final WPMessage msg) {
                Log.d(TAG, "received msg:" + msg.toString());
                if (notificationCallback != null) {
                    notificationCallback.onMessageReceived(msg);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            Intent intent = new Intent(WPService.this, MainActivity.class);
                            intent.putExtra("notification", "tabbed");
                            PendingIntent pendingIntent = PendingIntent.getActivity(WPService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(WPService.this)
                                    .setSmallIcon(getApplicationInfo().icon)
                                    .setContentTitle(msg.getTitle())
                                    .setContentText(msg.getSummary())
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent);

                            SharedPreferences sharedPref = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
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
    public void onDestroy(){

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
                    URL url = new URL("http://itsmpohang.hssa.me:9001/device");

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestMethod("PUT");

                    PackageInfo i = getPackageManager().getPackageInfo(getPackageName(), 0);

                    JSONObject body = new JSONObject();
                    body.put("clientId", mClient.getClientId());
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
