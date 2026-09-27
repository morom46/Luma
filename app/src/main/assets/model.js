(function(root){
  'use strict';
  const isoDate = d => {const x=new Date(d);return `${x.getFullYear()}-${String(x.getMonth()+1).padStart(2,'0')}-${String(x.getDate()).padStart(2,'0')}`;};
  function nextDate(date,repeat){const d=new Date(date+'T12:00:00');if(repeat==='daily')d.setDate(d.getDate()+1);else if(repeat==='weekdays'){do{d.setDate(d.getDate()+1);}while([0,6].includes(d.getDay()));}else if(repeat==='weekly')d.setDate(d.getDate()+7);else if(repeat==='alternate')d.setDate(d.getDate()+2);else return null;return isoDate(d);}
  const defaults=()=>({version:1,tasks:[],events:[],labels:['Work','Study','Personal'],settings:{work:25,short:5,long:30,every:4,sessions:8,goal:480,dark:true,sound:'off',keepAwake:false,blocked:[],standbyAuto:true,standbyNight:false,standbyDim:true}});
  function validData(d){
    const string=(v,n=3000)=>typeof v==='string'&&v.length<=n;
    const date=v=>string(v,10)&&(v===''||/^\d{4}-\d{2}-\d{2}$/.test(v)&&Number.isFinite(new Date(v+'T12:00:00').getTime()));
    const time=v=>string(v,5)&&(v===''||/^([01]\d|2[0-3]):[0-5]\d$/.test(v));
    const repeat=v=>['none','daily','weekdays','weekly','alternate'].includes(v);
    if(!d||d.version!==1||!Array.isArray(d.tasks)||!Array.isArray(d.events)||!Array.isArray(d.labels)||!d.settings||d.tasks.length>20000||d.events.length>20000||d.labels.length>500||!d.labels.every(l=>string(l,40)))return false;
    const s=d.settings;
    for(const [key,min,max] of [['work',1,180],['short',1,60],['long',1,90],['every',1,12],['sessions',1,12],['goal',1,1440]])if(!Number.isInteger(s[key])||s[key]<min||s[key]>max)return false;
    if(typeof s.dark!=='boolean'||typeof s.keepAwake!=='boolean'||!['off','white noise','brown noise','rain','waves'].includes(s.sound)||!Array.isArray(s.blocked)||!s.blocked.every(p=>string(p,255)))return false;
    // Optional in 1.0 backups; defaults are merged when loading older app data.
    for(const key of ['standbyAuto','standbyNight','standbyDim'])if(key in s&&typeof s[key]!=='boolean')return false;
    const row=r=>r&&string(r.id,150)&&r.id.length>0&&string(r.title,200)&&string(r.label,40)&&string(r.notes)&&date(r.date)&&repeat(r.repeat);
    if(!d.tasks.every(t=>row(t)&&time(t.time)&&typeof t.done==='boolean'&&typeof t.trackProgress==='boolean'&&Number.isFinite(t.progress)&&t.progress>=0&&t.progress<=100&&Array.isArray(t.subtasks)&&t.subtasks.length<=30&&t.subtasks.every(x=>x&&string(x.title)&&typeof x.done==='boolean')))return false;
    return d.events.every(e=>row(e)&&e.date&&time(e.start)&&e.start&&time(e.end)&&e.end>e.start&&Number.isInteger(e.reminder)&&e.reminder>=-1&&e.reminder<=1440);
  }
  function validBackup(d){return validData(d)&&Array.isArray(d.history)&&d.history.length<=100000&&d.history.every(s=>s&&typeof s.id==='string'&&typeof s.label==='string'&&typeof s.note==='string'&&Number.isFinite(s.startedAt)&&Number.isFinite(s.endedAt)&&s.endedAt>=s.startedAt&&Number.isFinite(s.durationMs)&&s.durationMs>=0&&s.durationMs<=604800000&&Number.isInteger(s.distractions)&&s.distractions>=0&&Number.isFinite(s.longestFocusMs)&&s.longestFocusMs>=0&&s.longestFocusMs<=s.durationMs);}
  function occurs(event,date){if(event.date===date)return true;if(event.date>date||!event.repeat||event.repeat==='none')return false;const a=new Date(event.date+'T12:00:00'),b=new Date(date+'T12:00:00'),days=Math.round((b-a)/86400000);if(event.repeat==='daily')return true;if(event.repeat==='weekdays')return ![0,6].includes(b.getDay());if(event.repeat==='weekly')return days%7===0;if(event.repeat==='alternate')return days%2===0;return false;}
  function totalMinutes(sessions,date){return sessions.filter(s=>!date||isoDate(s.endedAt)===date).reduce((n,s)=>n+s.durationMs/60000,0);}
  function completeTask(data,id,now=Date.now()){const task=data.tasks.find(t=>t.id===id);if(!task)return;task.done=!task.done;task.completedAt=task.done?now:null;task.progress=task.done?100:0;if(task.done&&task.repeat&&task.repeat!=='none'){let date=nextDate(task.date||isoDate(now),task.repeat);while(date<isoDate(now))date=nextDate(date,task.repeat);if(!data.tasks.some(t=>t.parentRepeatId===task.id)){data.tasks.push({...task,id:'task-'+now+'-'+Math.random().toString(36).slice(2,6),parentRepeatId:task.id,date,done:false,completedAt:null,progress:0,subtasks:(task.subtasks||[]).map(s=>({...s,done:false}))});}}}
  function shouldStandby({status,manual=false,auto=true,landscape=false,page='timer',dismissed=false,active=false,dialog=false}){
    if(!['running','paused'].includes(status))return false;
    if(manual)return true;
    if(status==='paused'&&!active)return false;
    return auto&&landscape&&page==='timer'&&!dismissed&&!dialog;
  }
  function standbyShift(epoch){const offsets=[[0,0],[3,-2],[-2,-3],[-3,2],[2,3]];return offsets[Math.floor(epoch/60000)%offsets.length];}
  const api={isoDate,nextDate,defaults,validData,validBackup,occurs,totalMinutes,completeTask,shouldStandby,standbyShift};if(typeof module!=='undefined')module.exports=api;root.FocusModel=api;
})(typeof globalThis!=='undefined'?globalThis:this);
