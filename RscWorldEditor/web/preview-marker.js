'use strict';
window.previewMarker=(()=>{
 let marker=null,picking=false;
 function location(){return marker&&marker.project===state?.projectKey?marker:null}
 function hint(){if(picking)$('mapHint').textContent='Click the map to place your preview marker, or press Esc to cancel.'}
 function finish(){picking=false;$('preview').setAttribute('aria-pressed','false');settings();draw()}
 function choose(){if(!state)return;$('previewDialog').close();picking=true;$('preview').setAttribute('aria-pressed','true');hint();draw()}
 $('preview').onclick=()=>{if(picking){finish();return}if(location())showPreview();else choose()};
 const button=document.createElement('button');button.id='choosePreviewLocation';button.textContent='Choose location';button.onclick=choose;
 $('previewDialog').querySelector('.dialog-foot').prepend(button);
 window.addEventListener('pointerdown',e=>{
  if(!picking||e.target!==canvas||e.button!==0||e.altKey||space)return;
  e.preventDefault();e.stopImmediatePropagation();
  if(window.world3D?.loading)return;
  const p=window.world3D?.active?window.world3D.position(e):position(e),t=tile(p.u,p.v);
  if(!t||t[2]===250){toast('Choose a spot on the existing map.');return}
  const w=world(p.u,p.v,0);marker={X:w.X,Y:w.Y,floor,project:state.projectKey};finish();showPreview();
 },true);
 window.addEventListener('keydown',e=>{if(picking&&e.key==='Escape'){e.preventDefault();e.stopImmediatePropagation();finish()}},true);
 const oldTool=setTool;setTool=function(name){if(picking){picking=false;$('preview').setAttribute('aria-pressed','false')}return oldTool(name)};
 const oldHint=window.world3D.hint;window.world3D.hint=function(){oldHint();hint()};
 const oldRender=render;render=function(){oldRender();hint();const m=location();if(!m||m.floor!==floor)return;
  const u=state.xmax-m.X,v=m.Y-state.ymin;if(u<0||v<0||u>=MAP_SIZE||v>=MAP_SIZE)return;
  const p=window.world3D.active?window.world3D.project(u+.5,v+.5):{x:panX+(u+.5)*scale,y:panY+(v+.5)*scale};
  ctx.save();ctx.setTransform(1,0,0,1,0,0);ctx.strokeStyle='#ffe05c';ctx.lineWidth=2;ctx.fillStyle='#080808cc';
  ctx.beginPath();ctx.arc(p.x,p.y,9,0,Math.PI*2);ctx.fill();ctx.stroke();ctx.beginPath();ctx.moveTo(p.x-14,p.y);ctx.lineTo(p.x+14,p.y);ctx.moveTo(p.x,p.y-14);ctx.lineTo(p.x,p.y+14);ctx.stroke();
  ctx.font='11px Arial';ctx.textAlign='center';ctx.fillStyle='#080808';ctx.fillRect(p.x-28,p.y-34,56,17);ctx.fillStyle='#ffe05c';ctx.fillText('Preview',p.x,p.y-22);ctx.restore();
 };
 return {location,choose};
})();
