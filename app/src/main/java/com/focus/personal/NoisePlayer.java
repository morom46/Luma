package com.focus.personal;

import android.content.Context;
import android.media.*;
import java.util.Random;

public final class NoisePlayer {
    private final AudioManager manager;
    private AudioFocusRequest focus;
    private String mode="off";
    private volatile int generation=0;
    private volatile boolean muted=false;
    public NoisePlayer(Context c){manager=c.getSystemService(AudioManager.class);}
    public synchronized void setMode(String next){if(next.equals(mode))return;stop();mode=next;if(next.equals("off"))return;AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->{muted=change!=AudioManager.AUDIOFOCUS_GAIN;}).build();muted=manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED;final int token=++generation;final String sound=next;new Thread(()->{AudioTrack track=null;try{int rate=22050;track=new AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(Math.max(8192,AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT))).setTransferMode(AudioTrack.MODE_STREAM).build();track.play();Random random=new Random();short[] buffer=new short[2048];double brown=0,low=0;long count=0;while(generation==token){for(int i=0;i<buffer.length;i++){double white=random.nextDouble()*2-1;brown=(brown+0.02*white)/1.02;low=.96*low+.04*white;double value=sound.equals("brown noise")?brown*3:sound.equals("waves")?low*2*(.45+.4*Math.sin((count++)/(double)rate*.45)):sound.equals("rain")?.5*white+.5*low:white;buffer[i]=(short)(muted?0:value*5000);}track.write(buffer,0,buffer.length);} }catch(Exception ignored){}finally{if(track!=null){try{track.stop();}catch(Exception ignored){}track.release();}}},"FocusAmbient").start();}
    public synchronized void stop(){generation++;mode="off";if(focus!=null){manager.abandonAudioFocusRequest(focus);focus=null;}}
}
