'use strict';

// Presentation only. Both timer displays read the same authoritative snapshot.
window.FocusStandby = (() => {
  let manual=false, dismissed=false, active=false, quiet=false, hideTimer=null;
  let nativeSignature='', orientation=matchMedia('(orientation: landscape)').matches;
  const media=matchMedia('(orientation: landscape)');
  const overlay=document.createElement('section');
  overlay.id='standby';
  overlay.hidden=true;
  overlay.tabIndex=-1;
  overlay.setAttribute('aria-label','StandBy clock and focus timer');
  overlay.innerHTML=`
    <div class="standby-content" id="standby-content">
      <div class="standby-clock">
        <div class="standby-eyebrow" id="standby-date"></div>
        <div class="standby-clock-digits" id="standby-clock" aria-label="Current time"></div>
        <div class="standby-caption">A little space to focus.</div>
      </div>
      <div class="standby-session">
        <div class="standby-eyebrow" id="standby-phase"></div>
        <div class="standby-digits" id="standby-time" role="timer" aria-label="Session time"></div>
        <div class="standby-label" id="standby-label"></div>
        <div class="standby-track" id="standby-track"><i id="standby-progress"></i></div>
        <div class="standby-caption" id="standby-meta"></div>
      </div>
    </div>
    <div class="standby-toolbar" id="standby-toolbar" role="group" aria-label="StandBy controls">
      <button data-action="standbyClose" aria-label="Exit StandBy">${icon('close')}<span>Exit</span></button>
      <button data-action="standbyNight" id="standby-night" aria-pressed="false">${icon('moon')}<span>Night</span></button>
      <button data-action="standbyDim" id="standby-dim" aria-pressed="true">${icon('dim')}<span>Dim</span></button>
      <button data-action="toggle" id="standby-toggle">${icon('pause')}<span>Pause</span></button>
      <button data-action="distraction" id="standby-distraction">${icon('focus')}<span>Distracted</span></button>
    </div>
    <button class="standby-reveal" id="standby-reveal" data-action="standbyReveal" aria-label="Show StandBy controls" hidden>Tap for controls</button>`;
  document.body.appendChild(overlay);
  const toolbar=$('standby-toolbar');
  const text=(id,value)=>{const node=$(id);if(node.textContent!==value)node.textContent=value;};

  function sendNative(force=false){
    const night=!!data.settings.standbyNight;
    const dim=active&&quiet&&!!data.settings.standbyDim&&state.status==='running';
    const signature=[active,night,dim].join(':');
    if(native&&(force||signature!==nativeSignature))Android.standbyDisplay(active,night,dim);
    nativeSignature=signature;
  }
  function setQuiet(value){
    quiet=value;
    overlay.classList.toggle('quiet',value);
    if(value&&toolbar.contains(document.activeElement))overlay.focus({preventScroll:true});
    toolbar.inert=value;
    $('standby-reveal').hidden=!value;
    sendNative();
  }
  function reveal(){
    if(!active)return;
    clearTimeout(hideTimer);
    setQuiet(false);
    if(state.status==='running')hideTimer=setTimeout(()=>{
      if(active&&state.status==='running'&&!$('sheet').open)setQuiet(true);
    },8000);
  }
  function sync(){
    const next=M.shouldStandby({status:state.status,manual,auto:data.settings.standbyAuto,
      landscape:orientation,page,dismissed,active,dialog:$('sheet').open});
    if(state.status==='idle')manual=false;
    if(next!==active){
      active=next;
      overlay.hidden=!active;
      document.body.classList.toggle('standby-active',active);
      if(active){overlay.focus({preventScroll:true});reveal();}
      else{clearTimeout(hideTimer);quiet=false;toolbar.inert=false;}
      sendNative();
    }
    if(!active)return;
    overlay.classList.toggle('night',!!data.settings.standbyNight);
    $('standby-night').setAttribute('aria-pressed',String(!!data.settings.standbyNight));
    $('standby-dim').setAttribute('aria-pressed',String(!!data.settings.standbyDim));
    const now=new Date();
    text('standby-clock',now.toLocaleTimeString(undefined,{hour:'2-digit',minute:'2-digit',hourCycle:'h23'}));
    text('standby-date',now.toLocaleDateString(undefined,{weekday:'long',day:'numeric',month:'short'}));
    text('standby-phase',state.status==='paused'?'Paused':state.phase==='work'?(state.mode==='stopwatch'?'Stopwatch':'Focus time'):state.phase==='long'?'Long break':'Take a break');
    const ms=state.mode==='stopwatch'?state.elapsedMs:state.remainingMs;
    const digits=clockText(ms);
    text('standby-time',digits);
    $('standby-time').classList.toggle('with-hours',digits.length>5);
    text('standby-label',state.label||'One thing at a time');
    text('standby-meta',state.status==='paused'?'Resume when you are ready':state.mode==='stopwatch'?`${state.distractions} distraction${state.distractions===1?'':'s'} noticed`:'Until '+new Date(Date.now()+state.remainingMs).toLocaleTimeString(undefined,{hour:'2-digit',minute:'2-digit',hourCycle:'h23'}));
    $('standby-track').hidden=state.mode==='stopwatch';
    $('standby-progress').style.width=(100*Math.max(0,Math.min(1,state.remainingMs/Math.max(1,state.totalMs))))+'%';
    const [x,y]=M.standbyShift(Date.now());
    $('standby-content').style.transform=`translate(${x}px,${y}px)`;
    const label=state.status==='running'?'Pause':'Resume';
    const button=$('standby-toggle');
    if(button.dataset.state!==label){button.innerHTML=icon(state.status==='running'?'pause':'play')+`<span>${label}</span>`;button.dataset.state=label;reveal();}
    $('standby-distraction').disabled=state.status!=='running'||state.phase!=='work';
    sendNative();
  }
  function enter(){
    if(state.status==='idle'){notify('Start a timer or stopwatch, then open StandBy.');return;}
    closeSheet();manual=true;dismissed=false;sync();
  }
  function exit(){manual=false;dismissed=true;sync();if(page==='timer')document.querySelector('[data-action="standbyOpen"]')?.focus({preventScroll:true});}
  function toggle(key){data.settings[key]=!data.settings[key];save();if(active){reveal();sync();}else render();}
  document.addEventListener('click',event=>{
    const action=event.target.closest('[data-action]')?.dataset.action;
    if(action==='standbyOpen')enter();
    else if(action==='standbyClose')exit();
    else if(action==='standbyAuto')toggle('standbyAuto');
    else if(action==='standbyNight')toggle('standbyNight');
    else if(action==='standbyDim')toggle('standbyDim');
    else if(action==='standbyReveal')reveal();
  });
  overlay.addEventListener('pointerdown',()=>reveal());
  overlay.addEventListener('keydown',()=>reveal());
  document.addEventListener('keydown',event=>{if(event.key==='Escape'&&active&&!$('sheet').open){event.preventDefault();exit();}});
  media.addEventListener('change',event=>{if(event.matches!==orientation){orientation=event.matches;dismissed=false;sync();}});
  document.addEventListener('visibilitychange',()=>{if(!document.hidden){sync();reveal();sendNative(true);}});
  sync();
  return {sync,exit,isActive:()=>active,newSession:()=>{dismissed=false;}};
})();
