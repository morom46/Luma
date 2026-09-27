package com.focus.personal;

import android.app.*;
import android.content.*;
import org.json.*;
import java.time.*;
import java.util.*;

/** One pending alarm; recompute the next occurrence after each fire/edit/reboot. */
public final class Reminders {
    private static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,31,new Intent(c,AlarmReceiver.class).setAction("reminder"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static final class Item {long time;String key,title;Item(long t,String k,String s){time=t;key=k;title=s;}}
    private static List<Item> candidates(Context c){List<Item> list=new ArrayList<>();try{JSONObject data=new JSONObject(FocusStore.get(c).getData());JSONArray tasks=data.optJSONArray("tasks"),events=data.optJSONArray("events");if(tasks!=null)for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);if(t.optBoolean("done")||t.optString("date").isEmpty()||t.optString("time").isEmpty())continue;long time=LocalDateTime.parse(t.getString("date")+"T"+t.getString("time")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();list.add(new Item(time,"task:"+t.getString("id")+":"+time,t.getString("title")));}if(events!=null)for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);int minutes=e.optInt("reminder",0);if(minutes<0)continue;LocalDate initial=LocalDate.parse(e.getString("date"));String repeat=e.optString("repeat","none");LocalDate from=LocalDate.now().minusDays(1);if(initial.isAfter(from))from=initial;for(int day=0;day<10;day++){LocalDate d=from.plusDays(day);long distance=java.time.temporal.ChronoUnit.DAYS.between(initial,d);boolean occurs=distance==0||distance>0&&(repeat.equals("daily")||repeat.equals("weekly")&&distance%7==0||repeat.equals("alternate")&&distance%2==0||repeat.equals("weekdays")&&d.getDayOfWeek().getValue()<6);if(!occurs)continue;long time=d.atTime(LocalTime.parse(e.getString("start"))).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()-minutes*60000L;list.add(new Item(time,"event:"+e.getString("id")+":"+time,e.getString("title")));}}}catch(Exception ignored){}return list;}
    public static void schedule(Context c){AlarmManager am=c.getSystemService(AlarmManager.class);PendingIntent pi=alarm(c);am.cancel(pi);long now=System.currentTimeMillis(),next=Long.MAX_VALUE;android.content.SharedPreferences prefs=c.getSharedPreferences("reminders",0);for(Item i:candidates(c))if(i.time>now&&!prefs.getBoolean(i.key,false))next=Math.min(next,i.time);if(next==Long.MAX_VALUE)return;try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,pi);}catch(SecurityException e){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,pi);}}
    public static void fire(Context c){long now=System.currentTimeMillis();android.content.SharedPreferences p=c.getSharedPreferences("reminders",0);for(Item i:candidates(c))if(i.time<=now+1500&&i.time>now-86400000&&!p.getBoolean(i.key,false)){Notifications.alert(c,100+Math.abs(i.key.hashCode()%100000),i.title,"Your planned time is here.");p.edit().putBoolean(i.key,true).apply();}schedule(c);}
}
