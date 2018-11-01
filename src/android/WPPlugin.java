package kr.co.itsm.plugin;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.wp.android.service.WPCallback;
import com.wp.android.service.WPClient;
import com.wp.android.service.WPException;
import com.wp.android.service.WPMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Map;

public class WPPlugin extends CordovaPlugin {
    private static final String TAG = "WPPlugin";
	private static final String PREFERENCE_KEY = "kr.co.itsm.plugin.WPNotification";
	private static final String SOUND = "kr.co.itsm.plugin.WPNotification.SOUND";
	private static final String VIBRATE = "kr.co.itsm.plugin.WPNotification.VIBRATE";
	private static final String SNOOZE = "kr.co.itsm.plugin.WPNotification.SNOOZE";

	private static final String SERVER_URI = "tcp://wp.hssa.me:48702";
//	private static final String SERVER_URI = "tcp://itsmpohang.hssa.me:48702";

    private CordovaInterface cordova;

    private WPClient mClient;
    public static CordovaWebView gWebView;
	public static String notificationCallBack = "WPClient.onNotificationReceived";
	public static Boolean notificationCallBackReady = false;

    public WPPlugin() {}

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
		gWebView = webView;
		
		mClient = new WPClient(cordova.getActivity().getApplicationContext(), SERVER_URI, new WPCallback() {

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
				Log.d(TAG, "received msg:" + msg.toString());
				String callBack = "javascript:" + notificationCallBack + "()";
				showNotification(msg);
				if(notificationCallBackReady && gWebView != null && gWebView.isInitialized()){
					Log.d(TAG, "\tSent PUSH to view: " + callBack);
					gWebView.sendJavascript(callBack);
				}
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
            }
        });

        // Intent intent = new Intent(cordova.getActivity(), WPService.class);
        // cordova.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "==> WPClient initialize");
    }

    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG,"==> WPPlugin execute: "+ action);

        try{
			// READY //
			if (action.equals("ready")) {
				//
				callbackContext.success();
            }
            // VERIFY CONNECTION //
            else if (action.equals("isConnected")) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
                    boolean bConnected = mClient.isConnected();
                    callbackContext.success( bConnected?"Connected":"Disconnected" );
                    Log.d(TAG,"\tConnected: "+ bConnected);
					}
				});
            }
			// GET Device ID //
			else if (action.equals("getDeviceId")) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
                    try{
						
						String deviceId = mClient.getDeviceId();
                        callbackContext.success( deviceId );
                    }catch(Exception e){
                        Log.d(TAG,"\tError retrieving device id");
                    }
					}
				});
            }
            // GET Client ID //
			else if (action.equals("getClientId")) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						try{
							String clientId = mClient.getClientId();
							callbackContext.success( clientId );
						}catch(Exception e){
							Log.d(TAG,"\tError retrieving client id");
						}
					}
				});
			}
			// NOTIFICATION CALLBACK REGISTER //
			else if (action.equals("registerNotification")) {
				notificationCallBackReady = true;
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						
					}
				});
			}
			// UN/SUBSCRIBE TOPICS //
			else if (action.equals("subscribeToTopic")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try{
							JSONArray array = args.getJSONArray(0);
							int length = array.length();
							String[] topics = new String[length];
							for (int i = 0; i < length; i++)
								topics[i] = array.getString(i);
							mClient.subscribe(topics);
							callbackContext.success();
						}catch(Exception e){
							callbackContext.error(e.getMessage());
						}
					}
				});
			}
			else if (action.equals("unsubscribeFromTopic")) {
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try{
							JSONArray array = args.getJSONArray(0);
							int length = array.length();
							String[] topics = new String[length];
							for (int i = 0; i < length; i++)
								topics[i] = array.getString(i);
							mClient.unsubscribe(topics);
							callbackContext.success();
						}catch(Exception e){
							callbackContext.error(e.getMessage());
						}
					}
				});
			}
			else if (action.equals("getPreferences")) {
				JSONObject obj = new JSONObject();
				SharedPreferences sharedPref = cordova.getActivity().getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
				obj.put("sound", sharedPref.getBoolean(SOUND, true));
				obj.put("vibrate", sharedPref.getBoolean(VIBRATE, true));
				obj.put("snooze", sharedPref.getBoolean(SNOOZE, true));
				callbackContext.success(obj);
			}
			else if (action.equals("setPreferences")) {
				SharedPreferences sharedPref = cordova.getActivity().getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putBoolean(SOUND, args.getBoolean(0));
				editor.putBoolean(VIBRATE, args.getBoolean(1));
				editor.putBoolean(SNOOZE, args.getBoolean(2));
				editor.commit();
				callbackContext.success();
			}
			else{
				callbackContext.error("Method not found");
				return false;
			}
		}catch(Exception e){
			Log.d(TAG, "ERROR: onPluginAction: " + e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		}

		return true;
    }

	private void showNotification(final WPMessage msg) {
		final SharedPreferences sharedPref = cordova.getActivity().getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
		if (!sharedPref.getBoolean(SNOOZE, true))
			return;

		new Thread(new Runnable() {
			@Override
			public void run() {
			try {
				NotificationManager nm = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getUrl()));
				PendingIntent pendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(cordova.getActivity())
						//.setSmallIcon(getApplicationInfo().icon)
						.setSmallIcon(cordova.getActivity().getResources().getIdentifier("ic_stat_pohang_", "drawable", cordova.getActivity().getPackageName()))
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
