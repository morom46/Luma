package com.focus.personal;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){FocusStore.get(c);Reminders.schedule(c);}
}
