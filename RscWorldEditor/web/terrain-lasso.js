'use strict';
window.terrainLasso=(()=>{
 tools.splice(tools.findIndex(t=>t[0]==='lasso')+1,0,['landlasso','⌁ Lasso terrain & contents']);
 let cells=new Set(),gesture=null,context=null,replace=true;
 const key=(u,v)=>v*MAP_SIZE+u,coords=k=>({u:k%MAP_SIZE,v:Math.floor(k/MAP_SIZE)});
 function remember(){context={rows:state.objects,floor,project:state.projectKey,x:state.xmax,y:state.ymin}}
 function valid(){if(context&&(context.rows!==state?.objects||context.floor!==floor||context.project!==state?.projectKey||context.x!==state?.xmax||context.y!==state?.ymin)){cells.clear();gesture=null;context=null}return cells}
 function reset(){cells.clear();gesture=null;context=null;draw();if(tool==='landlasso')settings()}
 function point(u,v){return window.world3D?.active?window.world3D.project(u,v):{x:panX+u*scale,y:panY+v*scale}}
 function pixel(e){const r=canvas.getBoundingClientRect();return {x:(e.clientX-r.left)*canvas.width/r.width,y:(e.clientY-r.top)*canvas.height/r.height}}
 function footprintCells(row){const p=footprint(row),result=[];for(let v=p.v;v<p.v+p.depth;v++)for(let u=p.u-p.w+1;u<=p.u;u++)result.push({u,v});return result}
 function selectedRow(row){const p=local(row);return p.h===floor&&cells.has(key(p.u,p.v))}
 function select(poly){
  cells.clear();for(let v=0;v<MAP_SIZE;v++)for(let u=0;u<MAP_SIZE;u++)if(tile(u,v)?.[2]!==250&&window.sceneryLasso.contains(point(u+.5,v+.5),poly))cells.add(key(u,v));
                                                                                       
  let changed=true;while(changed){changed=false;for(const row of state.objects){if(local(row).h!==floor)continue;const ps=footprintCells(row);if(!ps.some(p=>cells.has(key(p.u,p.v))))continue;for(const p of ps){if(p.u<0||p.v<0||p.u>=MAP_SIZE||p.v>=MAP_SIZE){cells.clear();toast('This structure crosses the editing boundary. Move the view and select it again.');return}if(!cells.has(key(p.u,p.v))){cells.add(key(p.u,p.v));changed=true}}}}
  remember();settings();draw();toast(cells.size?`${cells.size} tiles selected, including their contents. Drag the highlighted ground to move.`:'Draw around some existing ground.');
 }
 function destination(du,dv){
  if(!valid().size)return 'Select a chunk of ground first.';
  const target=new Set();for(const k of cells){const p=coords(k),u=p.u+du,v=p.v+dv;if(u<0||v<0||u>=MAP_SIZE||v>=MAP_SIZE)return 'Keep the whole chunk inside the editing area.';target.add(key(u,v));if(!replace&&!cells.has(key(u,v))&&tile(u,v)?.[2]!==250)return 'This destination has terrain. Enable Replace destination terrain & contents to move here.';}
  if(replace)return '';
  for(const row of state.objects){if(local(row).h!==floor||selectedRow(row))continue;if(footprintCells(row).some(p=>target.has(key(p.u,p.v))))return 'The destination contains another structure. Choose a clear space.';}
  for(const name of ['npcs','boundaries','items'])for(const row of state[name]){const p=local(row);if(p.h===floor&&!selectedRow(row)&&target.has(key(p.u,p.v)))return 'The destination contains other placements. Choose a clear space.';}
  return '';
 }
 function move(du,dv){
  if(!du&&!dv)return false;const error=destination(du,dv);if(error){toast(error);return false}
  const snapshot=[...cells].map(k=>({...coords(k),data:copy(tile(coords(k).u,coords(k).v))}));
  const rows=['objects','npcs','boundaries','items'].flatMap(name=>state[name].filter(selectedRow));
  begin();
  if(replace){
   const moving=new Set(rows),target=new Set(snapshot.map(p=>key(p.u+du,p.v+dv)));
   for(const name of ['objects','npcs','boundaries','items'])state[name]=state[name].filter(row=>{
    const at=local(row);if(at.h!==floor||moving.has(row))return true;
    if(name==='objects')return !footprintCells(row).some(p=>p.u>=0&&p.v>=0&&p.u<MAP_SIZE&&p.v<MAP_SIZE&&target.has(key(p.u,p.v)));
    return !target.has(key(at.u,at.v));
   });
  }
  for(const p of snapshot)touch(p.u,p.v).splice(0,7,0,0,250,0,0,0,0);
  for(const p of snapshot)touch(p.u+du,p.v+dv).splice(0,7,...p.data);
  for(const row of rows)for(const name of ['pos','start','min','max'])if(row[name]){row[name].X-=du;row[name].Y+=dv}
  cells=new Set(snapshot.map(p=>key(p.u+du,p.v+dv)));commit();remember();inspect();settings();draw();return true;
 }
 const deleteDialog=document.createElement('dialog');deleteDialog.id='terrainDeleteDialog';
 deleteDialog.setAttribute('aria-labelledby','terrainDeleteQuestion');
 const deleteQuestion=document.createElement('p');deleteQuestion.id='terrainDeleteQuestion';deleteQuestion.textContent='Are you sure you want to delete this selection?';
 const deleteActions=document.createElement('div');deleteActions.className='terrain-delete-actions';
 const deleteYes=document.createElement('button');deleteYes.textContent='Yes';
 const deleteNo=document.createElement('button');deleteNo.textContent='No';deleteNo.className='primary';deleteNo.autofocus=true;
 deleteActions.append(deleteYes,deleteNo);deleteDialog.append(deleteQuestion,deleteActions);document.body.append(deleteDialog);
 let pendingDelete=null;
 function askDelete(){if(!valid().size||gesture||deleteDialog.open)return;pendingDelete={context,cells:new Set(cells)};deleteDialog.showModal()}
 deleteNo.onclick=()=>deleteDialog.close();
 deleteDialog.addEventListener('close',()=>{pendingDelete=null;canvas.focus()});
 deleteYes.onclick=()=>{
  const pending=pendingDelete;
  if(!pending||tool!=='landlasso'||!valid().size||context!==pending.context||cells.size!==pending.cells.size||[...cells].some(k=>!pending.cells.has(k))){deleteDialog.close();return}
  begin();
  for(const name of ['objects','npcs','boundaries','items'])state[name]=state[name].filter(row=>!selectedRow(row));
  for(const k of cells){const p=coords(k);touch(p.u,p.v).splice(0,7,0,0,250,0,0,0,0)}
  commit();deleteDialog.close();reset();inspect();toast('Selection deleted. Ctrl Z restores it.');
 };
 canvas.addEventListener('pointerdown',e=>{if(tool!=='landlasso'||!state||e.button!==0||space||e.altKey||window.world3D?.loading)return;e.preventDefault();e.stopImmediatePropagation();valid();canvas.focus();canvas.setPointerCapture(e.pointerId);const p=position(e);if(p.u>=0&&p.v>=0&&p.u<MAP_SIZE&&p.v<MAP_SIZE&&cells.has(key(p.u,p.v)))gesture={kind:'move',start:p,du:0,dv:0,height:terrainVertexHeight(p.u,p.v),origin:window.world3D?.active?window.world3D.plane(p.x,p.y,terrainVertexHeight(p.u,p.v)):null};else{cells.clear();remember();gesture={kind:'select',points:[pixel(e)]}}draw()},true);
 canvas.addEventListener('pointermove',e=>{if(tool!=='landlasso'||!gesture)return;e.stopImmediatePropagation();if(gesture.kind==='select'){const p=pixel(e),last=gesture.points[gesture.points.length-1];if(Math.hypot(p.x-last.x,p.y-last.y)>3)gesture.points.push(p)}else{const p=position(e);if(gesture.origin){const at=window.world3D.plane(p.x,p.y,gesture.height);gesture.du=Math.floor(at.u)-Math.floor(gesture.origin.u);gesture.dv=Math.floor(at.v)-Math.floor(gesture.origin.v)}else{gesture.du=p.u-gesture.start.u;gesture.dv=p.v-gesture.start.v}}draw()},true);
 canvas.addEventListener('pointerup',e=>{if(tool!=='landlasso'||!gesture)return;e.stopImmediatePropagation();const g=gesture;gesture=null;if(g.kind==='select'){if(g.points.length>=3)select(g.points)}else move(g.du,g.dv);draw()},true);
 function cancel(){gesture=null;draw()}
 canvas.addEventListener('pointercancel',cancel,true);canvas.addEventListener('lostpointercapture',cancel,true);window.addEventListener('blur',cancel);
 document.addEventListener('keydown',e=>{if(tool!=='landlasso'||document.activeElement.isContentEditable||['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName)||document.querySelector('dialog[open]'))return;if(e.key==='Escape'){e.preventDefault();e.stopImmediatePropagation();reset();return}if(e.ctrlKey||e.metaKey||gesture)return;if(e.key==='Delete'&&valid().size){e.preventDefault();e.stopImmediatePropagation();askDelete();return}const d={ArrowLeft:[-1,0],ArrowRight:[1,0],ArrowUp:[0,-1],ArrowDown:[0,1]}[e.key];if(d&&valid().size){e.preventDefault();e.stopImmediatePropagation();move(...d)}},true);
 const oldSettings=settings;settings=function(){oldSettings();if(tool!=='landlasso')return;valid();$('toolName').textContent='Lasso terrain';$('toolDesc').textContent='Using the lasso terrain tool, draw around the whole area you want to pick up and then drag the highlighted chunk.';$('settings').replaceChildren();
  let p;
  let label=document.createElement('label'),check=document.createElement('input');check.type='checkbox';check.checked=replace;check.onchange=()=>{replace=check.checked;draw()};label.append(check,document.createTextNode(' Replace destination terrain & contents'));$('settings').append(label);
  p=document.createElement('p');p.textContent='This lasso tool will cut anything and everything in the current spot and the former terrain will become a blank space that you will need to refill again.';$('settings').append(p);
  const clear=document.createElement('button');clear.textContent='Clear selection';clear.onclick=reset;$('settings').append(clear);

 };
 const oldPalette=renderPalette;renderPalette=function(){oldPalette();if(tool!=='landlasso')return;$('paletteTitle').textContent='Terrain & contents';$('assets').replaceChildren();};
 const oldTool=setTool;setTool=function(name){if(name!=='landlasso'){cells.clear();gesture=null;context=null}oldTool(name)};
 const oldHint=window.world3D.hint;window.world3D.hint=function(){oldHint();if(tool==='landlasso')$('mapHint').textContent='Drag to move contents, press Esc to cancel or Ctrl Z to Undo.'};
 function edges(du=0,dv=0){ctx.beginPath();for(const k of cells){const p=coords(k),u=p.u+du,v=p.v+dv;for(const [dx,dy,a,b] of [[0,-1,[u,v],[u+1,v]],[1,0,[u+1,v],[u+1,v+1]],[0,1,[u+1,v+1],[u,v+1]],[-1,0,[u,v+1],[u,v]]]){const nu=p.u+dx,nv=p.v+dy;if(nu>=0&&nu<MAP_SIZE&&nv>=0&&nv<MAP_SIZE&&cells.has(key(nu,nv)))continue;const x=point(...a),y=point(...b);ctx.moveTo(x.x,x.y);ctx.lineTo(y.x,y.y)}}ctx.stroke()}
 function overlay(){if(tool!=='landlasso'||!state)return;valid();ctx.save();ctx.setTransform(1,0,0,1,0,0);ctx.lineWidth=3;ctx.strokeStyle='#ffc56f';edges();
  if(gesture?.kind==='select'){ctx.beginPath();gesture.points.forEach((p,i)=>ctx[i?'lineTo':'moveTo'](p.x,p.y));ctx.closePath();ctx.fillStyle='#ffc56f22';ctx.fill();ctx.stroke()}
  if(gesture?.kind==='move'){ctx.setLineDash([7,5]);ctx.strokeStyle=destination(gesture.du,gesture.dv)?'#ff7766':'#7fffd4';edges(gesture.du,gesture.dv)}ctx.restore();
 }
 const oldRender=render;render=function(){oldRender();overlay()};renderTools();
 return {select,move,destination,overlay};
})();
