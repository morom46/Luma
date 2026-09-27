package com.focus.personal;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.content.pm.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.os.SystemClock;
import org.json.*;

public class BlockerService extends AccessibilityService {
    private long lastBlock=0;
    public static boolean safePackage(Context c,String pkg){if(pkg==null||pkg.equals(c.getPackageName())||pkg.equals("android")||pkg.equals("com.android.settings")||pkg.equals("com.android.systemui")||pkg.contains("packageinstaller")||pkg.contains("permissioncontroller")||pkg.contains("dialer")||pkg.contains("telecom")||pkg.contains("incallui")||pkg.contains("launcher")||pkg.equals("com.sec.android.app.launcher")||pkg.equals("com.samsung.android.dialer")||pkg.equals("com.samsung.android.app.telephonyui")||pkg.contains("emergency"))return true;for(ResolveInfo home:c.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))if(home.activityInfo.packageName.equals(pkg))return true;return false;}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||event.getPackageName()==null)return;String pkg=event.getPackageName().toString();if(safePackage(this,pkg)||!FocusStore.get(this).focusing())return;JSONObject s=FocusStore.get(this).settings();if(s==null)return;JSONArray apps=s.optJSONArray("blocked");if(apps==null)return;for(int i=0;i<apps.length();i++)if(pkg.equals(apps.optString(i))){if(SystemClock.elapsedRealtime()-lastBlock<700)return;lastBlock=SystemClock.elapsedRealtime();performGlobalAction(GLOBAL_ACTION_HOME);Toast.makeText(this,"Focus session running. Pause it to open this app.",Toast.LENGTH_SHORT).show();return;}}
    @Override public void onInterrupt(){}
}
