package com.focus.personal;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private FocusStore store;
    private String exportText="";
    private boolean dark=true,keepAwake=false,standby=false,standbyNight=false,standbyDim=false,foreground=false;
    private String displaySignature="";
    private static final String ORIGIN="https://focus.local/";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable awakeTick=new Runnable(){public void run(){updateAwake();handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);store=FocusStore.get(this);if(state!=null)exportText=state.getString("export","");
        root=new FrameLayout(this);web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});}
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setBlockNetworkLoads(true);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportZoom(false);settings.setTextZoom(100);
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){Uri u=request.getUrl();try{if(!"https".equals(u.getScheme())||!"focus.local".equals(u.getHost()))return blocked();String path=u.getPath();String file=(path==null||path.equals("/"))?"index.html":path.substring(1);if(!Arrays.asList("index.html","app.js","model.js","style.css","standby.js","standby.css","inter.ttf").contains(file))return blocked();String mime=file.endsWith("js")?"text/javascript":file.endsWith("css")?"text/css":file.endsWith("ttf")?"font/ttf":"text/html";return new WebResourceResponse(mime,"UTF-8",getAssets().open(file));}catch(Exception e){return blocked();}}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
            @Override public void onPageFinished(WebView view,String url){view.evaluateJavascript("window.onNativeResume&&window.onNativeResume()",null);}
        });
        web.addJavascriptInterface(new Bridge(),"Android");web.loadUrl(ORIGIN);Notifications.channels(this);Reminders.schedule(this);handler.post(awakeTick);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,()->back());
    }
    private WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    @Override protected void onResume(){super.onResume();foreground=true;displaySignature="";if(store!=null){if(store.tick())Notifications.complete(this);refresh();}if(web!=null)web.evaluateJavascript("window.onNativeResume&&window.onNativeResume()",null);}
    @Override protected void onPause(){foreground=false;updateAwake();super.onPause();}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus&&web!=null){displaySignature="";applyDisplay();}}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("export",exportText);super.onSaveInstanceState(out);}
    @Override public void onBackPressed(){back();}
    private void back(){web.evaluateJavascript("window.handleBack?window.handleBack():false",result->{if(!"true".equals(result))moveTaskToBack(true);});}
    @Override protected void onDestroy(){handler.removeCallbacks(awakeTick);if(web!=null){web.removeJavascriptInterface("Android");web.destroy();}super.onDestroy();}
    private void notice(String message){runOnUiThread(()->web.evaluateJavascript("window.nativeNotice&&window.nativeNotice("+JSONObject.quote(message)+")",null));}
    private void updateAwake(){if(foreground&&(keepAwake||standby)&&store.running())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);applyDisplay();}
    private void applyDisplay(){
        if(web==null)return;
        boolean immersive=foreground&&standby;
        boolean black=dark||immersive;
        boolean dim=immersive&&standbyDim&&store.running();
        String signature=immersive+":"+black+":"+dim+":"+standbyNight;
        if(signature.equals(displaySignature))return;
        displaySignature=signature;
        int background=black?Color.BLACK:Color.WHITE;
        root.setBackgroundColor(background);web.setBackgroundColor(background);
        getWindow().setStatusBarColor(background);getWindow().setNavigationBarColor(background);
        WindowManager.LayoutParams attributes=getWindow().getAttributes();
        attributes.screenBrightness=dim?(standbyNight?0.06f:0.20f):WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(attributes);
        if(Build.VERSION.SDK_INT>=30){
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null){
                controller.setSystemBarsAppearance(black?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                if(immersive){controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);controller.hide(WindowInsets.Type.systemBars());}
                else controller.show(WindowInsets.Type.systemBars());
            }
        }else{
            int flags=black?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            if(immersive)flags|=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        root.requestApplyInsets();
    }
    private void notifyPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);}
    private void refresh(){AlarmReceiver.scheduleTimer(this);if(store.running()){try{startForegroundService(new Intent(this,FocusService.class));}catch(Exception e){notice("Android could not start background timing. Keep the app open and check battery settings.");}}else stopService(new Intent(this,FocusService.class));updateAwake();}
    public final class Bridge {
        @JavascriptInterface public String getData(){return store.getData();}
        @JavascriptInterface public void setData(String value){try{store.setData(value);runOnUiThread(()->Reminders.schedule(MainActivity.this));}catch(Exception e){notice("Your changes could not be saved.");}}
        @JavascriptInterface public String getHistory(){return store.history();}
        @JavascriptInterface public String snapshot(){return store.snapshot();}
        @JavascriptInterface public String command(String action,String payload){try{String result=store.command(action,new JSONObject(payload));runOnUiThread(()->{if(action.equals("start"))notifyPermission();refresh();});return result;}catch(Exception e){notice("Could not change the timer: check its settings.");return store.snapshot();}}
        @JavascriptInterface public void appearance(boolean isDark,boolean awake){runOnUiThread(()->{dark=isDark;keepAwake=awake;updateAwake();});}
        @JavascriptInterface public void standbyDisplay(boolean enabled,boolean night,boolean dim){runOnUiThread(()->{standby=enabled;standbyNight=night;standbyDim=dim;updateAwake();});}
        @JavascriptInterface public void requestNotifications(){runOnUiThread(()->notifyPermission());}
        @JavascriptInterface public void refreshService(){runOnUiThread(()->refresh());}
        @JavascriptInterface public String apps(){JSONArray array=new JSONArray();PackageManager pm=getPackageManager();List<ResolveInfo> apps=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);apps.sort((a,b)->a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));Set<String> seen=new HashSet<>();for(ResolveInfo app:apps){String pkg=app.activityInfo.packageName;if(!BlockerService.safePackage(MainActivity.this,pkg)&&seen.add(pkg))try{array.put(new JSONObject().put("pkg",pkg).put("name",app.loadLabel(pm).toString()));}catch(Exception ignored){}}return array.toString();}
        @JavascriptInterface public boolean blockerEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return enabled!=null&&enabled.contains(new ComponentName(MainActivity.this,BlockerService.class).flattenToString());}
        @JavascriptInterface public void openSettings(String type){runOnUiThread(()->{Intent intent;if(type.equals("accessibility"))intent=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);else if(type.equals("notifications"))intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());else intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()));try{startActivity(intent);}catch(Exception e){notice("Open this app in Android Settings.");}});}
        @JavascriptInterface public void exportData(String text){if(text.length()>5_000_000){notice("Backup is too large to export.");return;}exportText=text;runOnUiThread(()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"focus-backup-"+new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())+".json"),20));}
        @JavascriptInterface public void importData(){runOnUiThread(()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),21));}
        @JavascriptInterface public boolean restoreData(String text){boolean ok=store.restore(text);if(ok)runOnUiThread(()->{Reminders.schedule(MainActivity.this);refresh();});return ok;}
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(result!=RESULT_OK||intent==null||intent.getData()==null)return;try{if(request==20){try(OutputStream out=getContentResolver().openOutputStream(intent.getData(),"wt")){out.write(exportText.getBytes(StandardCharsets.UTF_8));}notice("Backup exported");}else if(request==21){ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(intent.getData())){byte[] b=new byte[8192];int n;while((n=in.read(b))>0){bytes.write(b,0,n);if(bytes.size()>5_000_000)throw new IOException("Backup too large");}}String text=bytes.toString("UTF-8");web.evaluateJavascript("window.restoreData("+JSONObject.quote(text)+")",null);}}catch(Exception e){notice("Could not read or write that backup file.");}}
}
