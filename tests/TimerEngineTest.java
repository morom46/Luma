import com.focus.personal.TimerEngine;

public class TimerEngineTest {
    static class Clock implements TimerEngine.Clock {long ms=10000,epoch=1800000000000L;public long elapsed(){return ms;}public long wall(){return epoch;}void advance(long n){ms+=n;epoch+=n;}}
    static int passed=0;
    static void eq(Object a,Object b,String name){if(!a.equals(b))throw new AssertionError(name+": "+a+" != "+b);passed++;}
    public static void main(String[] args){
        Clock c=new Clock();TimerEngine e=new TimerEngine(c);TimerEngine.Config cfg=new TimerEngine.Config();cfg.work=1;cfg.shortBreak=1;cfg.longBreak=2;cfg.every=2;cfg.sessions=4;
        e.start(cfg,"Study","A note","task1");c.advance(10000);eq(e.remaining(),50000L,"counts down");e.pause();c.advance(90000);eq(e.remaining(),50000L,"pause excludes wall time");e.start(cfg,"","","");c.advance(10000);e.distract();eq(e.distractions,1,"distraction recorded");eq(e.longestFocusMs,20000L,"focus excludes pause");c.advance(45000);TimerEngine.Session s=e.tick();eq(s.durationMs,60000L,"overrun capped at deadline");eq(s.endedAt,c.wall()-5000,"completion timestamp is deadline");eq(s.note,"A note","resume preserves note");eq(s.longestFocusMs,40000L,"longest uninterrupted stretch");eq(e.phase,"break","work to short break");eq(e.status,"idle","break waits for user");eq(e.tick()==null,true,"completion is idempotent");
        e.start(cfg,"Study","","");c.advance(60000);eq(e.tick()==null,true,"breaks not logged as work");eq(e.phase,"work","break to work");e.start(cfg,"Study","","");c.advance(60000);e.tick();eq(e.phase,"long","second session gets long break");eq(e.remaining(),120000L,"long break length");
        e.reset();e.setMode("stopwatch");e.start(cfg,"Work","","");c.advance(7200000);eq(e.elapsed(),7200000L,"stopwatch unbounded by timer");eq(e.tick()==null,true,"stopwatch does not auto-complete");s=e.finish(true);eq(s.durationMs,7200000L,"stopwatch saves actual work");eq(e.round,0,"manual finish resets rounds");e.reset();eq(e.status,"idle","reset goes idle");eq(e.elapsed(),0L,"reset drops progress");
        e.setMode("timer");e.start(cfg,"","","");c.epoch+=3600000;eq(e.remaining(),60000L,"wall clock change cannot move deadline");e.distract();e.pause();int hits=e.distractions;e.distract();eq(e.distractions,hits,"paused taps ignored");e.reset();cfg.work=0;boolean invalid=false;try{e.start(cfg,"","","");}catch(IllegalArgumentException x){invalid=true;}eq(invalid,true,"invalid configuration rejected");
        System.out.println("Timer engine: "+passed+" assertions passed");
    }
}
