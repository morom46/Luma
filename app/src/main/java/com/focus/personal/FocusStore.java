package com.focus.personal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import org.json.*;
import java.util.UUID;

public final class FocusStore {
    private static FocusStore instance;
    public static synchronized FocusStore get(Context c){if(instance==null)instance=new FocusStore(c.getApplicationContext());return instance;}
    private final Context context;
    private final SharedPreferences prefs;
    public final TimerEngine engine;
    private JSONArray history;
    private final int boot;
    private FocusStore(Context c){context=c;prefs=c.getSharedPreferences("focus",Context.MODE_PRIVATE);boot=Settings.Global.getInt(c.getContentResolver(),"boot_count",0);engine=new TimerEngine(new TimerEngine.Clock(){public long elapsed(){return SystemClock.elapsedRealtime();}public long wall(){return System.currentTimeMillis();}});
        try{history=new JSONArray(prefs.getString("history","[]"));JSONObject j=new JSONObject(prefs.getString("timer","{}"));engine.status=j.optString("status","idle");engine.mode=j.optString("mode","timer");engine.phase=j.optString("phase","work");engine.label=j.optString("label","");engine.note=j.optString("note","");engine.taskId=j.optString("taskId","");engine.totalMs=j.optLong("totalMs",1500000);engine.baseMs=j.optLong("baseMs");engine.startedElapsed=j.optLong("startedElapsed");engine.sessionStartedAt=j.optLong("sessionStartedAt");engine.lastDistractionMs=j.optLong("lastDistractionMs");engine.longestFocusMs=j.optLong("longestFocusMs");engine.distractions=j.optInt("distractions");engine.round=j.optInt("round");engine.revision=j.optLong("revision");engine.config=config(j.optJSONObject("config"));if(j.optInt("boot",boot)!=boot&&engine.status.equals("running")){engine.baseMs=j.optLong("checkpointMs");engine.status="paused";save();}}
        catch(Exception e){history=new JSONArray();engine.reset();}
    }
    private TimerEngine.Config config(JSONObject j){TimerEngine.Config c=new TimerEngine.Config();if(j!=null){c.work=j.optInt("work",25);c.shortBreak=j.optInt("short",5);c.longBreak=j.optInt("long",30);c.every=j.optInt("every",4);c.sessions=j.optInt("sessions",8);}c.validate();return c;}
    private JSONObject configJson()throws JSONException{return new JSONObject().put("work",engine.config.work).put("short",engine.config.shortBreak).put("long",engine.config.longBreak).put("every",engine.config.every).put("sessions",engine.config.sessions);}
    public synchronized boolean tick(){long rev=engine.revision;TimerEngine.Session s=engine.tick();if(engine.revision!=rev){append(s);save();return true;}return false;}
    public synchronized String command(String action,JSONObject p){tick();try{switch(action){case "start":engine.start(config(p.optJSONObject("config")),p.optString("label",engine.label),p.optString("note",engine.note),p.optString("taskId",engine.taskId));break;case "setup":if(engine.status.equals("idle")){engine.config=config(p.getJSONObject("config"));engine.label=p.optString("label","");engine.note=p.optString("note","");engine.taskId=p.optString("taskId","");engine.totalMs=(engine.phase.equals("work")?engine.config.work:engine.phase.equals("long")?engine.config.longBreak:engine.config.shortBreak)*60000L;}break;case "pause":engine.pause();break;case "finish":append(engine.finish(true));break;case "reset":engine.reset();break;case "distraction":engine.distract();break;case "mode":engine.setMode(p.getString("mode"));break;case "note":engine.note=p.optString("note","");break;default:throw new IllegalArgumentException("Unknown command");}save();}catch(Exception e){throw new IllegalArgumentException(e);}return snapshot();}
    private void append(TimerEngine.Session s){if(s==null)return;try{history.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("startedAt",s.startedAt).put("endedAt",s.endedAt).put("durationMs",s.durationMs).put("longestFocusMs",s.longestFocusMs).put("distractions",s.distractions).put("label",s.label.isEmpty()?"Unlabelled":s.label).put("note",s.note).put("taskId",s.taskId));}catch(JSONException e){throw new IllegalStateException(e);}}
    public synchronized String snapshot(){try{return new JSONObject().put("status",engine.status).put("mode",engine.mode).put("phase",engine.phase).put("label",engine.label).put("note",engine.note).put("taskId",engine.taskId).put("totalMs",engine.totalMs).put("elapsedMs",engine.elapsed()).put("remainingMs",engine.remaining()).put("round",engine.round).put("distractions",engine.distractions).put("revision",engine.revision).toString();}catch(Exception e){return "{}";}}
    public synchronized void save(){try{JSONObject j=new JSONObject(snapshot()).put("baseMs",engine.baseMs).put("startedElapsed",engine.startedElapsed).put("sessionStartedAt",engine.sessionStartedAt).put("lastDistractionMs",engine.lastDistractionMs).put("longestFocusMs",engine.longestFocusMs).put("config",configJson()).put("boot",boot).put("checkpointMs",engine.elapsed());prefs.edit().putString("timer",j.toString()).putString("history",history.toString()).commit();}catch(Exception e){throw new IllegalStateException(e);}}
    public synchronized String getData(){return prefs.getString("data","{}");}
    public synchronized JSONObject settings(){try{return new JSONObject(getData()).optJSONObject("settings");}catch(Exception e){return null;}}
    public synchronized void setData(String value){try{JSONObject d=new JSONObject(value);validateData(d);prefs.edit().putString("data",value).commit();}catch(Exception e){throw new IllegalArgumentException("Invalid data",e);}}
    public synchronized String history(){return history.toString();}
    private void validateData(JSONObject d)throws JSONException{if(d.optInt("version")!=1||d.getJSONArray("tasks").length()>20000||d.getJSONArray("events").length()>20000||d.getJSONArray("labels").length()>500)throw new JSONException("Invalid backup");config(d.getJSONObject("settings"));for(String key:new String[]{"tasks","events"}){JSONArray a=d.getJSONArray(key);for(int i=0;i<a.length();i++){JSONObject row=a.getJSONObject(i);if(row.getString("id").length()>150||row.getString("title").length()>1000)throw new JSONException("Invalid row");}}}
    public synchronized boolean restore(String value){try{if(!engine.status.equals("idle"))return false;JSONObject d=new JSONObject(value);validateData(d);JSONArray h=d.getJSONArray("history");if(h.length()>100000)return false;for(int i=0;i<h.length();i++){JSONObject s=h.getJSONObject(i);if(s.getLong("durationMs")<0||s.getLong("durationMs")>604800000||s.getLong("endedAt")<0)throw new JSONException("Invalid session");}history=h;d.remove("history");setData(d.toString());engine.reset();save();return true;}catch(Exception e){return false;}}
    public synchronized boolean running(){return engine.status.equals("running");}
    public synchronized boolean focusing(){return engine.isFocusing();}
    public synchronized long remaining(){return engine.remaining();}
    public synchronized boolean countdown(){return engine.mode.equals("timer");}
    public synchronized String phase(){return engine.phase;}
}
