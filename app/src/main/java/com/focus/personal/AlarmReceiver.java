package com.focus.personal;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    public static void scheduleTimer(Context c){FocusStore s=FocusStore.get(c);AlarmManager am=c.getSystemService(AlarmManager.class);PendingIntent pi=PendingIntent.getBroadcast(c,30,new Intent(c,AlarmReceiver.class).setAction("timer"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);if(s.running()&&s.countdown()){long when=SystemClock.elapsedRealtime()+s.remaining();try{am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);}catch(SecurityException e){am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);}}}
    @Override public void onReceive(Context c,Intent intent){FocusStore s=FocusStore.get(c);String a=intent.getAction();if("pause".equals(a)){s.command("pause",new JSONObject());scheduleTimer(c);c.stopService(new Intent(c,FocusService.class));}else if("timer".equals(a)){if(s.tick())Notifications.complete(c);if(!s.running())c.stopService(new Intent(c,FocusService.class));else scheduleTimer(c);}else if("reminder".equals(a)){Reminders.fire(c);}}
}
