package com.focus.personal;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import org.json.*;

public final class Notifications {
    public static final String RUNNING="focus-running",ALERTS="focus-alerts";
    public static void channels(Context c){NotificationManager nm=c.getSystemService(NotificationManager.class);NotificationChannel ongoing=new NotificationChannel(RUNNING,"Running focus timer",NotificationManager.IMPORTANCE_LOW);ongoing.setSound(null,null);ongoing.enableVibration(false);nm.createNotificationChannel(ongoing);NotificationChannel alerts=new NotificationChannel(ALERTS,"Session completion and reminders",NotificationManager.IMPORTANCE_HIGH);alerts.enableVibration(true);nm.createNotificationChannel(alerts);}
    public static PendingIntent open(Context c){return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static Notification running(Context c){FocusStore s=FocusStore.get(c);String title="Focus session",message="Focus session in progress";long ms=0;boolean countdown=s.countdown();try{JSONObject j=new JSONObject(s.snapshot());boolean work=j.getString("phase").equals("work");title=work?(j.optString("label").isEmpty()?"Stay focused":j.getString("label")):"Take a break";message=work?"Focus session in progress":"Break in progress";ms=j.getLong(countdown?"remainingMs":"elapsedMs");}catch(Exception ignored){}PendingIntent pause=PendingIntent.getBroadcast(c,40,new Intent(c,AlarmReceiver.class).setAction("pause"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);Notification.Builder builder=new Notification.Builder(c,RUNNING).setSmallIcon(R.drawable.ic_notification).setColor(Color.rgb(0,167,186)).setContentTitle(title).setContentText(message).setContentIntent(open(c)).setOngoing(true).setOnlyAlertOnce(true).setWhen(System.currentTimeMillis()+(countdown?ms:-ms)).setUsesChronometer(true).setChronometerCountDown(countdown).setCategory(Notification.CATEGORY_STOPWATCH).addAction(new Notification.Action.Builder(null,"Pause",pause).build());if(Build.VERSION.SDK_INT>=36&&countdown){Bundle extras=new Bundle();extras.putBoolean("android.requestPromotedOngoing",true);builder.addExtras(extras);}return builder.build();}
    public static void alert(Context c,int id,String title,String text){channels(c);try{c.getSystemService(NotificationManager.class).notify(id,new Notification.Builder(c,ALERTS).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text).setContentIntent(open(c)).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build());}catch(SecurityException ignored){}}
    public static void complete(Context c){String next=FocusStore.get(c).phase();alert(c,2,"Session complete",next.equals("work")?"Ready when you are for your next focus session.":"Nice work. Your break is ready when you are.");}
}
