package com.focus.personal;

/** Pure state machine. Elapsed time is monotonic; wall time is used only for history. */
public final class TimerEngine {
    public interface Clock { long elapsed(); long wall(); }
    public static final class Config {
        public int work=25, shortBreak=5, longBreak=30, every=4, sessions=8;
        public void validate(){
            if(work<1||work>180||shortBreak<1||shortBreak>60||longBreak<1||longBreak>90||every<1||every>12||sessions<1||sessions>12)throw new IllegalArgumentException("Invalid timer configuration");
        }
    }
    public static final class Session {
        public long startedAt, endedAt, durationMs, longestFocusMs;
        public int distractions;
        public String label, note, taskId;
    }
    public final Clock clock;
    public Config config=new Config();
    public String status="idle",mode="timer",phase="work",label="",note="",taskId="";
    public long totalMs=25*60000L,baseMs=0,startedElapsed=0,sessionStartedAt=0,lastDistractionMs=0,longestFocusMs=0,revision=0;
    public int distractions=0,round=0;
    public TimerEngine(Clock clock){this.clock=clock;}
    public long elapsed(){long n=baseMs+(status.equals("running")?Math.max(0,clock.elapsed()-startedElapsed):0);return mode.equals("timer")?Math.min(totalMs,n):n;}
    public long remaining(){return Math.max(0,totalMs-elapsed());}
    public boolean isFocusing(){return status.equals("running")&&phase.equals("work")&&(mode.equals("stopwatch")||remaining()>0);}
    public void start(Config next,String label,String note,String taskId){
        if(status.equals("running"))return;
        if(status.equals("idle")){
            next.validate();config=next;this.label=label;this.note=note;this.taskId=taskId;
            totalMs=(phase.equals("work")?config.work:phase.equals("long")?config.longBreak:config.shortBreak)*60000L;
            baseMs=0;distractions=0;lastDistractionMs=0;longestFocusMs=0;sessionStartedAt=clock.wall();
        }
        startedElapsed=clock.elapsed();status="running";
    }
    public void pause(){if(status.equals("running")){baseMs=elapsed();status="paused";}}
    public void distract(){if(isFocusing()){long n=elapsed();longestFocusMs=Math.max(longestFocusMs,n-lastDistractionMs);lastDistractionMs=n;distractions++;}}
    public Session tick(){if(status.equals("running")&&mode.equals("timer")&&remaining()==0)return finish(false);return null;}
    public Session finish(boolean stop){
        if(status.equals("idle"))return null;
        long spent=elapsed();Session s=null;
        if(phase.equals("work")&&spent>=1000){s=new Session();s.startedAt=sessionStartedAt;s.endedAt=clock.wall();if(!stop&&status.equals("running")&&mode.equals("timer"))s.endedAt-=Math.max(0,baseMs+clock.elapsed()-startedElapsed-totalMs);s.durationMs=spent;s.distractions=distractions;s.label=label;s.note=note;s.taskId=taskId;s.longestFocusMs=Math.max(longestFocusMs,spent-lastDistractionMs);}
        if(stop||mode.equals("stopwatch")){phase="work";round=0;}else if(phase.equals("work")){round++;if(round>=config.sessions){phase="work";round=0;}else phase=round%config.every==0?"long":"break";}else phase="work";
        status="idle";baseMs=0;distractions=0;lastDistractionMs=0;longestFocusMs=0;
        totalMs=(phase.equals("work")?config.work:phase.equals("long")?config.longBreak:config.shortBreak)*60000L;
        revision++;return s;
    }
    public void reset(){status="idle";phase="work";baseMs=0;round=0;distractions=0;lastDistractionMs=0;longestFocusMs=0;totalMs=config.work*60000L;revision++;}
    public void setMode(String value){if(!status.equals("idle"))throw new IllegalStateException("Timer active");if(!value.equals("timer")&&!value.equals("stopwatch"))throw new IllegalArgumentException("Mode");mode=value;reset();}
}
