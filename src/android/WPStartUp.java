package kr.co.itsm.plugin;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;

public class WPStartUp extends BroadcastReceiver {   

    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * 폰 재시작 할때 서비스 등록
         */
        Intent i = new Intent(context,WPService.class);
        context.startService(i);
    }
}