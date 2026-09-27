package com.focus.personal;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import org.json.JSONObject;

public class FocusService extends Service {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FocusStore store;private NoisePlayer noise;private int ticks=0;
    private final Runnable ticker=new Runnable(){public void run(){if(store.tick())Notifications.complete(FocusService.this);if(!store.running()){stopSelf();return;}JSONObject settings=store.settings();noise.setMode(settings==null?"off":settings.optString("sound","off"));if(++ticks%30==0)store.save();handler.postDelayed(this,1000);}};
    @Override public void onCreate(){super.onCreate();store=FocusStore.get(this);noise=new NoisePlayer(this);Notifications.channels(this);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){if(Build.VERSION.SDK_INT>=34)startForeground(1,Notifications.running(this),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(1,Notifications.running(this));if(!store.running()){stopSelf();return START_NOT_STICKY;}AlarmReceiver.scheduleTimer(this);handler.removeCallbacks(ticker);handler.post(ticker);return START_STICKY;}
    @Override public void onDestroy(){handler.removeCallbacks(ticker);noise.stop();store.save();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
