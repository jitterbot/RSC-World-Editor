'use strict';
                                                                            
window.groundFloorGuides=(()=>{
 const label=document.createElement('label');
 label.title='While underground, show where ground-floor ladders and stairs sit directly above. Dashed guides are references, not placed objects or linked travel destinations.';
 const toggle=document.createElement('input');toggle.type='checkbox';toggle.id='groundFloorGuides';
 label.append(toggle,document.createTextNode(' Ground-floor entrances'));
 $('cameraTools').append(label);
 toggle.onchange=()=>draw();
 function entrance(id){
  const definition=assets.objects[id];if(!definition||definition.deleted)return false;
  const source=assets.objects[definition.sourceId];
  return /\b(ladders?|stairs?|staircases?|stairways?|steps)\b/i.test((definition.name||'')+' '+(source?.name||''));
 }
 function guides(){
  if(!state||floor!==3||!toggle.checked)return [];
  return state.objects.filter(row=>local(row).h===0&&entrance(row.id)).map(row=>({row,p:footprint(row)}));
 }
 function project(u,v){return window.world3D?.active?window.world3D.project(u,v):{x:panX+u*scale,y:panY+v*scale}}
 function overlay(){
  toggle.disabled=floor!==3;
  if(!state)return;
  const rows=guides();if(!rows.length)return;
  ctx.save();ctx.setTransform(1,0,0,1,0,0);ctx.lineWidth=2;ctx.strokeStyle='#7de9ff';ctx.fillStyle='#7de9ff18';
  for(const {row,p} of rows){
   const a=p.u-p.w+1,b=p.v,c=p.u+1,d=p.v+p.depth;
   const points=[[a,b],[c,b],[c,d],[a,d]].map(([u,v])=>project(u,v));
   ctx.setLineDash([6,4]);ctx.beginPath();points.forEach((point,i)=>ctx[i?'lineTo':'moveTo'](point.x,point.y));ctx.closePath();ctx.fill();ctx.stroke();
   const centre=project((a+c)/2,(b+d)/2);ctx.setLineDash([]);ctx.font='12px sans-serif';ctx.textAlign='center';ctx.textBaseline='bottom';
   const text=(assets.objects[row.id].name||'Entrance')+' · ground floor';
   ctx.lineWidth=4;ctx.strokeStyle='#10242a';ctx.strokeText(text,centre.x,centre.y-6);ctx.fillStyle='#a5f1ff';ctx.fillText(text,centre.x,centre.y-6);
   ctx.lineWidth=2;ctx.strokeStyle='#7de9ff';ctx.fillStyle='#7de9ff18';
  }
  ctx.restore();
 }
 const previousRender=render;render=function(){previousRender();overlay()};
 toggle.disabled=floor!==3;
 return {entrance,guides,overlay};
})();
