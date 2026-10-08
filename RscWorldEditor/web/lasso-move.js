'use strict';
                                                                                     
window.sceneryLasso=(()=>{
 tools.splice(tools.findIndex(t=>t[0]==='select'),0,['lasso','⌁ Lasso & move']);
 let selected=[],gesture=null,context=null,pasting=false,pasteTarget=null,rotationPivot=null;
 function valid(){if(context&&(context.rows!==state?.objects||context.floor!==floor||context.project!==state?.projectKey||context.x!==state?.xmax||context.y!==state?.ymin)){selected=[];gesture=null;context=null;pasting=false;pasteTarget=null}return selected}
 function remember(){rotationPivot=null;context={rows:state.objects,floor,project:state.projectKey,x:state.xmax,y:state.ymin}}
 function point(u,v){return window.world3D?.active?window.world3D.project(u,v):{x:panX+u*scale,y:panY+v*scale}}
 function screen(e){let r=canvas.getBoundingClientRect();return {x:(e.clientX-r.left)*canvas.width/r.width,y:(e.clientY-r.top)*canvas.height/r.height}}
 function contains(p,poly){let inside=false;for(let i=0,j=poly.length-1;i<poly.length;j=i++){let a=poly[i],b=poly[j];if((a.y>p.y)!==(b.y>p.y)&&p.x<(b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x)inside=!inside}return inside}
 function box(row,du=0,dv=0){let p=footprint(row);return {a:p.u-p.w+1+du,b:p.v+dv,c:p.u+1+du,d:p.v+p.depth+dv}}
 function corners(b){return [[b.a,b.b],[b.c,b.b],[b.c,b.d],[b.a,b.d]].map(([u,v])=>point(u,v))}
 function centre(row){let b=box(row);return point((b.a+b.c)/2,(b.b+b.d)/2)}
 function reset(){selected=[];gesture=null;context=null;pasting=false;pasteTarget=null;if(tool==='lasso')settings();draw()}
 function selectionAt(poly){return state.objects.map((row,index)=>({row,index})).filter(({row})=>local(row).h===floor&&!assets.objects[row.id]?.deleted&&contains(centre(row),poly)).map(({index})=>index)}
 function checkRows(rows,du=0,dv=0){
  for(const row of rows){if(!assets.objects[row.id]||assets.objects[row.id].deleted)return 'This selection includes deleted scenery. Copy it again.';
   let b=box(row,du,dv);if(b.a<0||b.b<0||b.c>MAP_SIZE||b.d>MAP_SIZE)return 'Keep the entire group inside the editing area.';
   for(let v=b.b;v<b.d;v++)for(let u=b.a;u<b.c;u++){if(!tile(u,v)||tile(u,v)[2]===250)return 'Place the whole group on existing terrain.'}
  }return '';
 }
 function destination(du,dv){return checkRows(valid().map(i=>state.objects[i]),du,dv)}
 function copiedRows(p){return clipboard.rows.map(r=>{let row=JSON.parse(JSON.stringify(r.row));row.pos={X:state.xmax-p.u-r.u,Y:state.ymin+p.v+r.v+floor*944};return row})}
 function copyGroup(){
  if(gesture)return false;
  if(!valid().length){toast('Lasso some structures first.');return false}
  const rows=selected.map(i=>state.objects[i]),a=Math.min(...rows.map(r=>box(r).a)),b=Math.min(...rows.map(r=>box(r).b));
  clipboard={kind:'lasso-scenery',project:state.projectKey,rows:rows.map(row=>({row:JSON.parse(JSON.stringify(row)),u:local(row).u-a,v:local(row).v-b}))};
  pasting=false;pasteTarget=null;settings();toast(`${rows.length} structures copied. Press Ctrl V, then click where the copy should go.`);return true;
 }
 function startPaste(){
  if(clipboard?.kind!=='lasso-scenery'){toast('Copy a selection first.');return false}
  if(clipboard.project!==state?.projectKey){toast('This selection belongs to another map. Copy structures from this map first.');return false}
  setTool('lasso');valid();remember();gesture=null;pasting=true;pasteTarget=null;settings();draw();toast('Move over the map and click to place the copy. Escape cancels.');return true;
 }
 function placeCopy(p){
  if(!pasting||!p||clipboard?.kind!=='lasso-scenery'||clipboard.project!==state?.projectKey)return false;
  const rows=copiedRows(p),error=checkRows(rows);if(error){toast(error);return false}
  begin();const first=state.objects.length;state.objects.push(...rows);selected=rows.map((_,i)=>first+i);commit();remember();pasting=false;pasteTarget=null;settings();inspect();draw();toast(`Placed ${rows.length} copied structures. Ctrl Z undoes the copy.`);return true;
 }
 function rotationBox(row){const p=footprint(row),u=state.xmax-row.pos.X,v=row.pos.Y-state.ymin-floor*944;return {a:u-p.w+1,b:v,c:u+1,d:v+p.depth}}
 function rotatedRows(rows,pivot,turns){
  return rows.map(original=>{let row=JSON.parse(JSON.stringify(original));for(let turn=0;turn<turns;turn++){
   const b=rotationBox(row),u=pivot.u+pivot.v-b.b-1,v=pivot.v+b.a-pivot.u;
   row.pos.X=state.xmax-u;row.pos.Y=state.ymin+v+floor*944;row.direction=(row.direction+2)%8;
  }return row});
 }
 function rotateGroup(reverse=false){
  valid();if(gesture)return false;
  const turns=reverse?3:1;
  if(pasting){
   if(clipboard?.kind!=='lasso-scenery'||clipboard.project!==state?.projectKey)return false;
   const rows=rotatedRows(copiedRows({u:0,v:0}),{u:0,v:0},turns),a=Math.min(...rows.map(r=>rotationBox(r).a)),b=Math.min(...rows.map(r=>rotationBox(r).b));
   clipboard={...clipboard,rows:rows.map(row=>({row,u:state.xmax-row.pos.X-a,v:row.pos.Y-state.ymin-floor*944-b}))};draw();return true;
  }
  if(!selected.length){toast('Lasso some structures first.');return false}
  const rows=selected.map(i=>state.objects[i]),bounds=rows.map(r=>box(r));
  const pivot=rotationPivot||{u:Math.floor((Math.min(...bounds.map(b=>b.a))+Math.max(...bounds.map(b=>b.c)))/2),v:Math.floor((Math.min(...bounds.map(b=>b.b))+Math.max(...bounds.map(b=>b.d)))/2)};
  const rotated=rotatedRows(rows,pivot,turns),error=checkRows(rotated);if(error){toast(error);return false}
  begin();selected.forEach((index,i)=>state.objects[index]=rotated[i]);commit();remember();rotationPivot=pivot;inspect();draw();return true;
 }
 const previousCopySelection=copySelection;copySelection=function(){if(tool==='lasso')return copyGroup();return previousCopySelection()};
 const previousPasteMode=pasteMode;pasteMode=function(){if(clipboard?.kind==='lasso-scenery')return startPaste();return previousPasteMode()};
 const previousStampData=stampData;stampData=function(){if(clipboard?.kind==='lasso-scenery'&&tool==='stamp'&&choice===-1)return null;return previousStampData()};
 function move(du,dv){if(!valid().length||(!du&&!dv))return false;let error=destination(du,dv);if(error){toast(error);return false}begin();for(const index of selected){let row=state.objects[index];row.pos.X-=du;row.pos.Y+=dv}commit();remember();inspect();draw();return true}
 function hit(p){return valid().some(i=>contains(p,corners(box(state.objects[i])))||Math.hypot(p.x-centre(state.objects[i]).x,p.y-centre(state.objects[i]).y)<10)}
 canvas.addEventListener('pointerdown',e=>{
  if(tool!=='lasso'||!state||e.button!==0||space||e.altKey||window.world3D?.loading)return;
  e.preventDefault();e.stopImmediatePropagation();valid();canvas.focus();canvas.setPointerCapture(e.pointerId);let pixel=screen(e),p=position(e);
  if(pasting){placeCopy(p);return}
  if(selected.length&&hit(pixel)&&tile(p.u,p.v)){gesture={kind:'move',start:p,du:0,dv:0};}
  else{selected=[];remember();gesture={kind:'lasso',points:[pixel]};}draw();
 },true);
 canvas.addEventListener('pointermove',e=>{
  if(tool!=='lasso')return;valid();if(pasting){pasteTarget=position(e);e.stopImmediatePropagation();draw();return}if(!gesture)return;e.stopImmediatePropagation();
  if(gesture.kind==='lasso'){let p=screen(e),last=gesture.points[gesture.points.length-1];if(Math.hypot(p.x-last.x,p.y-last.y)>=3)gesture.points.push(p)}
  else{let p=position(e);gesture.invalid=!tile(p.u,p.v);if(!gesture.invalid){gesture.du=p.u-gesture.start.u;gesture.dv=p.v-gesture.start.v}}
  draw();
 },true);
 canvas.addEventListener('pointerup',e=>{
  if(tool!=='lasso'||!gesture)return;e.stopImmediatePropagation();let g=gesture;gesture=null;
  if(g.kind==='lasso'){if(g.points.length>=3)selected=selectionAt(g.points);remember();toast(selected.length?`${selected.length} structure${selected.length===1?'':'s'} selected. Drag a highlighted footprint to move.`:'No structures selected. Draw around their ground footprints.');}
  else if(g.invalid)toast('Choose a destination on the map.');else move(g.du,g.dv);
  settings();draw();
 },true);
 function cancel(){if(gesture){gesture=null;draw()}}
 canvas.addEventListener('pointercancel',cancel,true);canvas.addEventListener('lostpointercapture',cancel,true);window.addEventListener('blur',cancel);
 document.addEventListener('keydown',e=>{
  if(tool!=='lasso'||['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName)||document.querySelector('dialog[open]'))return;
  if(e.key==='Escape'){e.stopImmediatePropagation();e.preventDefault();reset();return}
  if(e.ctrlKey||e.metaKey||gesture)return;
  if(e.key.toLowerCase()==='r'){e.preventDefault();e.stopImmediatePropagation();rotateGroup(e.shiftKey);return}
  if(pasting)return;
  const offsets={ArrowLeft:[-1,0],ArrowRight:[1,0],ArrowUp:[0,-1],ArrowDown:[0,1]};
  if(offsets[e.key]&&valid().length){e.stopImmediatePropagation();e.preventDefault();move(...offsets[e.key]);}
 },true);
 const previousSettings=settings;settings=function(){previousSettings();if(tool!=='lasso')return;valid();$('toolName').textContent='Lasso & move structures';$('toolDesc').textContent='Draw a freehand loop around the current ground area of your choice to select and move scenery. You can rotate the entire group by 90 degree rotations. With this tool, terrain, walls, roofs, people and items stay put. Only your scenery will move.';$('settings').replaceChildren();
  let count=document.createElement('p');count.className='notice';count.textContent=pasting?'Click on the map to place your copied group. Escape cancels.':selected.length?`${selected.length} selected · drag a highlighted footprint to move`:'';if(count.textContent)$('settings').append(count);

  for(const [label,action,disabled] of [['Rotate group ↻ · R',()=>rotateGroup(),!selected.length&&!pasting],['Rotate group ↺ · Shift R',()=>rotateGroup(true),!selected.length&&!pasting],['Copy selection · Ctrl C',copyGroup,!selected.length],['Paste copy · Ctrl V',startPaste,clipboard?.kind!=='lasso-scenery']]){let b=document.createElement('button');b.className='wide';b.textContent=label;b.disabled=disabled;b.onclick=action;$('settings').append(b)}
  let clear=document.createElement('button');clear.textContent='Clear lasso selection';clear.className='wide';clear.disabled=!selected.length&&!pasting;clear.onclick=reset;$('settings').append(clear);
  let tips=document.createElement('p');tips.className='muted';tips.textContent='Ctrl C: copy · Ctrl V: click to paste · R / Shift R: rotate group · Arrow keys: move one map tile · Escape: cancel / clear · Ctrl Z: undo. You can lasso in either 2D or 3D.';$('settings').append(tips);
 };
 const previousPalette=renderPalette;renderPalette=function(){previousPalette();if(tool!=='lasso')return;$('paletteTitle').textContent='Move trees & scenery';$('assets').replaceChildren();};
 const previousSetTool=setTool;setTool=function(name){if(name!=='lasso'){selected=[];gesture=null;context=null;pasting=false;pasteTarget=null}previousSetTool(name)};
 const previousHint=window.world3D.hint;window.world3D.hint=function(){previousHint();if(tool==='lasso')$('mapHint').textContent='Draw a loop to select and drag to move scenery. Press Ctrl C to copy, Ctrl V to paste, R to rotate, Esc to cancel or Ctrl Z to Undo.'};
 function outline(poly,fill,color){ctx.beginPath();poly.forEach((p,i)=>ctx[i?'lineTo':'moveTo'](p.x,p.y));ctx.closePath();ctx.strokeStyle=color;ctx.fillStyle=fill;ctx.lineWidth=2;ctx.fill();ctx.stroke()}
 function overlay(){if(tool!=='lasso'||!state)return;valid();ctx.save();ctx.setTransform(1,0,0,1,0,0);
  for(const i of selected){outline(corners(box(state.objects[i])),'#ffe3a033','#ffe3a0');let p=centre(state.objects[i]);ctx.beginPath();ctx.arc(p.x,p.y,4,0,Math.PI*2);ctx.fillStyle='#ffe3a0';ctx.fill();}
  if(pasting&&pasteTarget){const rows=copiedRows(pasteTarget),error=checkRows(rows);ctx.setLineDash([6,4]);for(const row of rows)outline(corners(box(row)),error?'#ff776633':'#7fffd433',error?'#ff7766':'#7fffd4');ctx.setLineDash([])}
  if(gesture?.kind==='lasso'&&gesture.points.length>1)outline(gesture.points,'#76dedb22','#76dedb');
  if(gesture?.kind==='move'){let error=gesture.invalid||destination(gesture.du,gesture.dv);ctx.setLineDash([6,4]);for(const i of selected)outline(corners(box(state.objects[i],gesture.du,gesture.dv)),error?'#ff776633':'#7fffd433',error?'#ff7766':'#7fffd4')}
  ctx.restore();
 }
 const previousRender=render;render=function(){previousRender();overlay()};
 renderTools();
 return {contains,selectionAt,destination,move,overlay,copyGroup,startPaste,placeCopy,rotateGroup};
})();
