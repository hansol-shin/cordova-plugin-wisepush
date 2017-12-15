package kr.co.itsm.plugin;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.wp.android.service.WPMessage;

public class WPPlugin extends CordovaPlugin {
    private static final String TAG = "WPPlugin";
	private static final String PREFERENCE_KEY = "kr.co.itsm.plugin.WPNotification";
	private static final String SOUND = "kr.co.itsm.plugin.WPNotification.SOUND";
	private static final String VIBRATE = "kr.co.itsm.plugin.WPNotification.VIBRATE";
	private static final String SNOOZE = "kr.co.itsm.plugin.WPNotification.SNOOZE";

    private CordovaInterface cordova;

    private WPService mService;
    private boolean mBound = false;
    public static CordovaWebView gWebView;
	public static String notificationCallBack = "WPClient.onNotificationReceived";
	public static Boolean notificationCallBackReady = false;
	
	private ServiceConnection mConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            WPService.LocalBinder binder = (WPService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.setNotificationCallback(new WPService.NotificationCallback() {
                @Override
                public void onMessageReceived(WPMessage msg) {
					String callBack = "javascript:" + notificationCallBack + "()";
					if(notificationCallBackReady && gWebView != null){
						Log.d(TAG, "\tSent PUSH to view: " + callBack);
						gWebView.sendJavascript(callBack);
					}
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0){
            mBound = false;
        }
    };

    public WPPlugin() {}

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
        gWebView = webView;

        Intent intent = new Intent(cordova.getActivity(), WPService.class);
        cordova.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

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
                    boolean bConnected = mService.isConnected();
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
                        String deviceId = mService.getDeviceId();
                        callbackContext.success( deviceId );
                        Log.d(TAG,"\tDevice ID: "+ deviceId);
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
							String clientId = mService.getClientId();
							callbackContext.success( clientId );
							Log.d(TAG,"\tClient ID: "+ clientId);
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
							mService.subscribeToTopic(topics);
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
							mService.unsubscribeFromTopic(topics);
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

		//cordova.getThreadPool().execute(new Runnable() {
		//	public void run() {
		//	  //
		//	}
		//});

		//cordova.getActivity().runOnUiThread(new Runnable() {
        //    public void run() {
        //      //
        //    }
        //});
		return true;
    }

    @Override
	public void onDestroy() {
		gWebView = null;
		notificationCallBackReady = false;
		this.cordova.getActivity().unbindService(mConnection);
	}
}
