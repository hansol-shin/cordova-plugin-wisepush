package kr.co.itsm.plugin;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.wp.android.service.WPDeviceUtils;

import android.content.pm.PackageInfo;
import android.os.Build;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class WPFirebaseInstanceIDService extends FirebaseInstanceIdService {

    static Handler mHandler;
    static final int TOKEN_REFRESH = -1;
    private static final String TAG = "WPFirebaseIIDService";

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = TOKEN_REFRESH;
            msg.obj = refreshedToken;
            mHandler.sendMessage(msg);

            updateDevice(refreshedToken);
        }
    }

    private void updateDevice(final String deviceId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String clientId = new WPDeviceUtils(WPFirebaseInstanceIDService.this).getDeviceId();
                    URL url = new URL("https://smart.pohang.go.kr/api/device");

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestMethod("PUT");

                    PackageInfo i = getPackageManager().getPackageInfo(getPackageName(), 0);

                    JSONObject body = new JSONObject();
                    Log.d(TAG, clientId);
                    body.put("clientId", clientId);

                    body.put("deviceId", deviceId);
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
