'use strict';
window.buildTools=(()=>{
  let catalog=null,shape='round',falloff=.65,wallMode='line',category='all',gesture=null,edgeHover=null,wallErase=false;
  const oldEntries=entries,oldPalette=renderPalette,oldSettings=settings,oldSculpt=sculpt,oldPaint=paint;
  tools.splice(3,0,['walls','▥ Walls & fences']);
  function load(data){catalog=data;for(let d of data.tiles){let known=terrain.find(t=>t.id===d.id);if(known)Object.assign(known,{texture:d.texture,color:d.color,name:known.name+' · #'+d.id});else terrain.push({...d});catalogCache(d)}for(let i=0;i<256;i++){let name=i<64?'Snow / grey':i<128?'Grass':i<192?'Sand / earth':'Dark earth';terrain.push({id:'ground'+i,overlay:0,ground:i,name:name+' · colour '+i,color:groundColor(i)})}renderTools();renderPalette();settings();draw()}
  function catalogCache(d){materials[d.id]=terrain.find(t=>t.id===d.id)||d;window.world3D?.invalidate()}
  entries=function(){return tool==='walls'?(catalog?.walls||[]):oldEntries()};
  function preview(c,item){let g=c.getContext('2d');g.fillStyle='#253b43';g.fillRect(0,0,c.width,c.height);let t=item.texture;
    if(t>=0&&t<60&&atlas.complete&&atlas.naturalWidth){g.imageSmoothingEnabled=false;if(tool==='walls'){g.save();g.translate(c.width/2,c.height/2);g.rotate(-Math.PI/2);g.drawImage(atlas,t%8*128,Math.floor(t/8)*128,128,128,-(c.height-10)/2,-(c.width-20)/2,c.height-10,c.width-20);g.restore()}else g.drawImage(atlas,t%8*128,Math.floor(t/8)*128,128,128,10,5,c.width-20,c.height-10)}else{g.fillStyle=item.color||'#9da18c';g.fillRect(10,5,c.width-20,c.height-10)}
  }
  let atlas=new Image();atlas.src='/textures.png';atlas.onload=()=>{if(['paint','walls','newarea'].includes(tool))renderPalette()};
  renderPalette=function(){if(!['paint','walls'].includes(tool)||!catalog)return oldPalette();$('paletteTitle').textContent=tool==='walls'?'Walls, windows, doors & fences':'Ground surfaces';$('search').hidden=false;$('search').placeholder='Search name, material or #ID…';$('assets').replaceChildren();let filters=document.createElement('select');filters.className='material-filter';let options=tool==='walls'?[['all','All 214 styles'],['wall','Walls'],['window','Windows / glass'],['door','Doors / gates / frames'],['fence','Fences / railings / hedges']]:[['all','All ground surfaces'],['overlay','Ground overlays'],['ground','Ground colours']];for(let [v,label]of options){let o=document.createElement('option');o.value=v;o.textContent=label;filters.append(o)}if(!options.some(o=>o[0]===category))category='all';filters.value=category;filters.onchange=()=>{category=filters.value;renderPalette()};$('assets').append(filters);let q=$('search').value.toLowerCase(),shown=0;entries().forEach((item,i)=>{let n=item.name.toLowerCase();if(!n.includes(q))return;if(tool==='paint'&&category==='ground'&&item.overlay!==0)return;if(tool==='paint'&&category==='overlay'&&item.overlay===0)return;if(tool==='walls'&&category!=='all'){let match={wall:/wall|brick|timber/,window:/window|glass|slit/,door:/door|gate|frame/,fence:/fence|rail|hedge/}[category];if(!match?.test(n))return}shown++;let b=document.createElement('button');b.className='asset'+(choice===i?' selected':'');b.title=item.name+(item.blocked?' · Blocks movement':'');let c=document.createElement('canvas');c.width=128;c.height=80;preview(c,item);let label=document.createElement('span');label.textContent=item.name;b.append(c,label);b.onclick=()=>{choice=i;if(tool==='walls'&&/window|door|gate|frame|glass/i.test(item.name))wallMode='segment';renderPalette();settings();draw()};$('assets').append(b)});let text=document.createElement('p');text.className='muted';text.textContent=shown+' materials · variants retain their original game IDs';$('assets').append(text)};
  settings=function(){oldSettings();if(['paint','blend','height','erase','roof','opening'].includes(tool)){let slider=$('settings').querySelector('input[type=range]');if(slider){slider.max=41;slider.value=brush}field('Brush shape',select([['round','Round'],['square','Square']],shape,v=>{shape=v;draw()}))}if(tool==='height'){let softness=document.createElement('input');softness.type='range';softness.min=0;softness.max=100;softness.value=falloff*100;softness.oninput=()=>falloff=+softness.value/100;field('Soft edge · hard ← → soft',softness);let b=document.createElement('button');b.className='wide'+(heightMode==='ramp'?' active':'');b.textContent='↗ Ramp: drag from start to end';b.onclick=()=>{window.world3D?.editing();heightMode='ramp';settings()};$('settings').append(b);let note=document.createElement('p');note.className='notice';note.textContent=heightMode==='ramp'?'Drag a ramp from the current ground height to “Flatten to height”. Brush size sets its width. Release to apply; Escape cancels.':'Soft edges taper Raise, Lower, Smooth and Flatten. Shift temporarily reverses Raise / Lower. Each complete stroke is one undo.';$('settings').append(note)}if(tool==='walls')wallSettings();if(tool==='build'&&catalog){let selects=$('settings').querySelectorAll('select');replaceOptions(selects[0],catalog.walls.filter(d=>!d.interactive).map(d=>[d.id,d.name]),buildStyle.wall);replaceOptions(selects[1],catalog.tiles.filter(d=>!d.blocked).map(d=>[d.id,d.name]),buildStyle.floor);replaceOptions(selects[2],[[0,'Open courtyard'],[1,'Tiled roof'],[2,'Planks'],[3,'Stone'],[4,'Thatch'],[5,'Marble'],[6,'Tent / fabric']],buildStyle.roof)}};
  function replaceOptions(el,options,value){if(!el)return;el.replaceChildren();for(let [id,name]of options){let o=document.createElement('option');o.value=id;o.textContent=name;el.append(o)}el.value=value}
  function wallSettings(){$('toolName').textContent='Draw walls & fences';$('toolDesc').textContent='Drag a line or enclosure. Click an edge to replace it with a window, door or another style.';$('mapHint').textContent='Choose a style and drag a line or rectangle. Click an edge to replace it or press Ctrl Z to Undo.';field('Placement',select([['line','Straight line'],['rectangle','Rectangle enclosure'],['diagonal','Diagonal line: 45°'],['segment','Single edge: doors & windows']],wallMode,v=>{wallMode=v;draw()}));let remove=document.createElement('button');remove.className='wide';remove.textContent=wallErase?'Erase edges: ON':'Erase edges: OFF';remove.onclick=()=>{wallErase=!wallErase;settings()};$('settings').append(remove);let item=entries()[choice];if(item){let c=document.createElement('canvas');c.width=200;c.height=150;c.className='selected-model';preview(c,item);$('settings').append(c);let note=document.createElement('p');note.className='notice';note.textContent=item.name+' · '+(item.blocked?'Blocks movement. ':'Passable. ')+(item.interactive?'Placed as a game boundary. Special quest doors keep their game scripts.':'Placed in the landscape.');$('settings').append(note)}}
  let shift=false;document.addEventListener('keydown',e=>{shift=e.shiftKey;if(e.key==='Escape'){gesture=null;edgeHover=null;draw()}});document.addEventListener('keyup',e=>shift=e.shiftKey);window.addEventListener('blur',()=>{shift=false;gesture=null});
  function weight(dx,dy){let r=brush/2,d=shape==='round'?Math.hypot(dx,dy):Math.max(Math.abs(dx),Math.abs(dy));if(d>r)return 0;if(!falloff)return 1;return Math.min(1,Math.max(0,(r-d)/(r*falloff)))}
  sculpt=function(u,v){if(heightMode==='pick')return oldSculpt(u,v);if(heightMode==='ramp'||!transaction)return;transaction.weights??=new Map();let half=Math.floor(brush/2),mode=shift?(heightMode==='raise'?'lower':heightMode==='lower'?'raise':heightMode):heightMode;for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){let current=tile(x,y),w=weight(x-u,y-v),key=floor*MAP_CELLS+y*MAP_SIZE+x;if(!current||current[2]===250||w<=0||w<=(transaction.weights.get(key)||0))continue;let before=transaction.sculptBase?.get(key)||transaction.tiles.get(key)||copy(current),value=before[0];if(mode==='raise')value+=heightStep*w;else if(mode==='lower')value-=heightStep*w;else if(mode==='flatten')value+=(heightTarget-value)*w;else{let total=0,count=0;for(let dy=-1;dy<=1;dy++)for(let dx=-1;dx<=1;dx++){let n=tile(x+dx,y+dy);if(n&&n[2]!==250){let original=transaction.sculptBase?.get(floor*MAP_CELLS+(y+dy)*MAP_SIZE+x+dx)||transaction.tiles.get(floor*MAP_CELLS+(y+dy)*MAP_SIZE+x+dx);total+=(original||n)[0];count++}}value+=Math.max(-heightStep,Math.min(heightStep,total/count-value))*w}transaction.weights.set(key,w);touch(x,y)[0]=Math.max(0,Math.min(255,Math.round(value)))}draw()};
  paint=function(u,v){if(!['paint','erase'].includes(tool)||shape==='square'||tool==='paint'&&paintMode==='soft')return oldPaint(u,v);let size=brush,half=Math.floor(size/2);brush=1;try{for(let y=-half;y<=half;y++)for(let x=-half;x<=half;x++)if(Math.hypot(x,y)<=size/2)oldPaint(u+x,v+y)}finally{brush=size}};
  function point(u,v){if(window.world3D?.active)return window.world3D.project(u,v);return {x:panX+u*scale,y:panY+v*scale}}
  function nearest(e){let p=position(e);if(!tile(p.u,p.v))return null;let corners=[[p.u,p.v],[p.u+1,p.v],[p.u+1,p.v+1],[p.u,p.v+1]],best=corners[0],distance=Infinity;for(let [u,v]of corners){let q=point(u,v),d=Math.hypot(q.x-p.x,q.y-p.y);if(d<distance){distance=d;best=[u,v]}}let segments=[[p.u,p.v,5],[p.u,p.v,4],[p.u,p.v+1,5],[p.u-1,p.v,4]],edge=null;if(tile(p.u,p.v)[6]>0&&tile(p.u,p.v)[6]<24000)segments.push([p.u,p.v,tile(p.u,p.v)[6]<12000?6:7]);distance=Infinity;for(let candidate of segments){let [a,b]=ends(candidate),aa=point(...a),bb=point(...b),dx=bb.x-aa.x,dy=bb.y-aa.y,t=Math.max(0,Math.min(1,((p.x-aa.x)*dx+(p.y-aa.y)*dy)/(dx*dx+dy*dy||1))),d=Math.hypot(p.x-aa.x-t*dx,p.y-aa.y-t*dy);if(d<distance){distance=d;edge=candidate}}return {u:best[0],v:best[1],edge,tile:p}}
  function ends([u,v,slot]){if(slot===6)return [[u+1,v],[u,v+1]];if(slot===7)return [[u,v],[u+1,v+1]];return slot===5?[[u,v],[u+1,v]]:[[u+1,v],[u+1,v+1]]}
  function edges(a,b,mode=wallMode){if(mode==='segment'||a.u===b.u&&a.v===b.v)return [a.edge];let result=[];function horizontal(x,xx,y){for(let u=Math.min(x,xx);u<Math.max(x,xx);u++)result.push([u,y,5])}function vertical(y,yy,x){for(let v=Math.min(y,yy);v<Math.max(y,yy);v++)result.push([x-1,v,4])}if(mode==='diagonal'){let dx=Math.sign(b.u-a.u)||1,dy=Math.sign(b.v-a.v)||1,n=Math.max(Math.abs(b.u-a.u),Math.abs(b.v-a.v));for(let i=0;i<n;i++){let x=a.u+i*dx,y=a.v+i*dy;result.push([Math.min(x,x+dx),Math.min(y,y+dy),dx===dy?7:6])}}else if(mode==='rectangle'){horizontal(a.u,b.u,a.v);horizontal(a.u,b.u,b.v);vertical(a.v,b.v,a.u);vertical(a.v,b.v,b.u)}else if(Math.abs(b.u-a.u)>=Math.abs(b.v-a.v))horizontal(a.u,b.u,a.v);else vertical(a.v,b.v,a.u);return result}
  function placeEdges(list,id,erase=wallErase){let item=catalog.walls[id-1];for(let edge of list){if(!edge)continue;let [u,v,slot]=edge;if(!erase&&floor>0&&!editableFloorTile(u,v))continue;let t=touch(u,v);if(!t)continue;let pos=world(u,v),direction=slot===5?0:slot===4?1:slot===6?3:2;state.boundaries=state.boundaries.filter(r=>!(r.pos.X===pos.X&&r.pos.Y===pos.Y&&r.direction===direction));t[slot>=6?6:slot]=erase?0:item?.interactive?0:id+(slot===7?12000:0);if(!erase&&item?.interactive)state.boundaries.push({id:id-1,pos,direction})}}
  function ramp(a,b){let dx=b.u-a.u,dy=b.v-a.v,len2=dx*dx+dy*dy;if(!len2)return;let start=tile(a.u,a.v)[0],radius=brush/2;for(let v=Math.max(0,Math.floor(Math.min(a.v,b.v)-radius));v<=Math.min(MAP_LAST,Math.ceil(Math.max(a.v,b.v)+radius));v++)for(let u=Math.max(0,Math.floor(Math.min(a.u,b.u)-radius));u<=Math.min(MAP_LAST,Math.ceil(Math.max(a.u,b.u)+radius));u++){let t=Math.max(0,Math.min(1,((u-a.u)*dx+(v-a.v)*dy)/len2)),distance=Math.hypot(u-a.u-t*dx,v-a.v-t*dy);if(distance>radius)continue;let w=falloff?Math.min(1,(radius-distance)/(radius*falloff)):1,target=start+(heightTarget-start)*t;touch(u,v)[0]=Math.round(tile(u,v)[0]*(1-w)+target*w)}}
  canvas.addEventListener('pointerdown',e=>{if(e.button!==0||space||e.altKey||window.world3D?.loading)return;if(tool!=='walls'&&!(tool==='height'&&heightMode==='ramp'))return;let p=nearest(e);if(!p)return;e.stopImmediatePropagation();canvas.focus();canvas.setPointerCapture(e.pointerId);gesture={a:p,b:p,kind:tool==='walls'?'wall':'ramp'};draw()},true);
  canvas.addEventListener('pointermove',e=>{if(tool!=='walls'&&!gesture)return;let p=nearest(e);edgeHover=p;if(gesture&&p){e.stopImmediatePropagation();gesture.b=p}draw()},true);
  canvas.addEventListener('pointerup',e=>{if(!gesture)return;e.stopImmediatePropagation();let g=gesture;gesture=null;begin();if(g.kind==='wall')placeEdges(edges(g.a,g.b),entries()[choice]?.id||1);else ramp(g.a.tile,g.b.tile);commit();inspect();draw()},true);
  canvas.addEventListener('pointercancel',()=>{gesture=null;draw()},true);
  function overlay(){let list=gesture?.kind==='wall'?edges(gesture.a,gesture.b):tool==='walls'&&edgeHover?[edgeHover.edge]:[];ctx.save();ctx.lineWidth=4;ctx.strokeStyle=wallErase?'#ff8a7b':'#ffe09b';for(let e of list){if(!e)continue;let [a,b]=ends(e),aa=point(...a),bb=point(...b);ctx.beginPath();ctx.moveTo(aa.x,aa.y);ctx.lineTo(bb.x,bb.y);ctx.stroke()}if(gesture?.kind==='ramp'){let a=point(gesture.a.tile.u,gesture.a.tile.v),b=point(gesture.b.tile.u,gesture.b.tile.v);ctx.beginPath();ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);ctx.stroke()}ctx.restore()}
  const originalRender=render;render=function(){originalRender();if(!window.world3D?.active)overlay()};
  return {get tileDefs(){return catalog?.tiles||[]},previewSurface:preview,load,overlay,edges,placeEdges,ramp,showAllMaterials(){category='all'},get shape(){return shape},set shape(v){shape=v},get falloff(){return falloff},set falloff(v){falloff=v}};
})();

                                                                            
                                                                              
window.areaCreator=(()=>{
  const surfaces=[{name:'Grass',ground:78,overlay:0},{name:'Lush grass',ground:66,overlay:0},{name:'Sand',ground:130,overlay:0},{name:'Earth',ground:192,overlay:0},{name:'Water',ground:78,overlay:2},{name:'Invisible',ground:0,overlay:8}];
  let elevation=110,surface=0,gesture=null;
  const previousSettings=settings,previousPalette=renderPalette,previousPosition=position,previousRender=render,previousSetTool=setTool;
  tools.unshift(['newarea','＋ Create area']);
  function bounds(a,b){return {a:Math.max(0,Math.min(a.u,b.u)),b:Math.max(0,Math.min(a.v,b.v)),c:Math.min(MAP_LAST,Math.max(a.u,b.u)),d:Math.min(MAP_LAST,Math.max(a.v,b.v))}}
  function emptyCount(rect,h=floor){let count=0;for(let v=rect.b;v<=rect.d;v++)for(let u=rect.a;u<=rect.c;u++)if(tile(u,v,h)?.[2]===250)count++;return count}
  function create(rect,height=elevation,material=surfaces[surface],h=floor){
    if(!Number.isInteger(height)||height<0||height>255)throw Error('Choose a height between 0 and 255.');
    let count=0;
    begin();
    for(let v=Math.max(0,rect.b);v<=Math.min(MAP_LAST,rect.d);v++)for(let u=Math.max(0,rect.a);u<=Math.min(MAP_LAST,rect.c);u++){
                                                                             
                                                                            
      if(tile(u,v,h)?.[2]!==250)continue;
      let t=touch(u,v,h);t[0]=height;t[1]=material.ground;t[2]=material.overlay;count++;
    }
    commit();return count;
  }
  function screen(u,v){return window.world3D?.active?window.world3D.project(u,v,elevation*3/128):{x:panX+u*scale,y:panY+v*scale}}
  function point(e){let r=canvas.getBoundingClientRect(),x=e.clientX-r.left,y=e.clientY-r.top,p=window.world3D?.active?window.world3D.plane(x,y,elevation*3/128):{u:(x-panX)/scale,v:(y-panY)/scale};return {u:Math.floor(p.u),v:Math.floor(p.v),x,y}}
  position=function(e){return tool==='newarea'?point(e):previousPosition(e)};
  setTool=function(name){gesture=null;return previousSetTool(name)};
  renderPalette=function(){if(tool!=='newarea')return previousPalette();$('paletteTitle').textContent='Choose your starting surface';$('search').hidden=true;$('assets').replaceChildren();surfaces.forEach((item,i)=>{let b=document.createElement('button');b.className='asset'+(surface===i?' selected':'');b.title=item.name;let c=document.createElement('canvas');c.width=128;c.height=80;c.setAttribute('aria-label',item.name+' surface preview');let material=item.overlay?window.buildTools.tileDefs.find(d=>d.id===item.overlay):null;window.buildTools.previewSurface(c,{texture:material?.texture,color:item.overlay===8?'#000000':groundColor(item.ground)});let label=document.createElement('span');label.textContent=item.name;b.append(c,label);b.onclick=()=>{surface=i;renderPalette();settings();draw()};$('assets').append(b)})};
  settings=function(){if(tool!=='newarea')return previousSettings();$('toolName').textContent='Create a new area';$('toolDesc').textContent='Make new terrain in a blank space on the map. You can edit your new terrain with your tool options.';$('mapHint').textContent='Drag over blank space and release to create terrain, press Esc to cancel or Ctrl Z to Undo.';$('settings').replaceChildren();field('Starting surface',select(surfaces.map((s,i)=>[i,s.name]),surface,v=>{surface=+v;renderPalette();draw()}));let height=document.createElement('input');height.type='number';height.min=0;height.max=255;height.value=elevation;height.onchange=()=>{elevation=Math.max(0,Math.min(255,Math.round(Number(height.value)||0)));height.value=elevation;gesture=null;draw()};field('Starting ground height',height);let match=document.createElement('button');match.className='wide';match.textContent='Match nearby land height';match.onclick=()=>{let reference=lastTarget||{u:MAP_HALF,v:MAP_HALF},best=null,distance=Infinity;for(let v=0;v<MAP_SIZE;v++)for(let u=0;u<MAP_SIZE;u++){let t=tile(u,v);if(t[2]===250)continue;let d=(u-reference.u)**2+(v-reference.v)**2;if(d<distance){distance=d;best=t}}if(!best){toast('No existing land in this area. Set a starting height manually.');return}elevation=best[0];gesture=null;settings();draw()};$('settings').append(match);if(surface===4){let water=document.createElement('p');water.className='muted';water.textContent='Water blocks ordinary walking. Paint a land connection or add travel separately.';$('settings').append(water)}};
  function line(a,b){let aa=screen(...a),bb=screen(...b);ctx.moveTo(aa.x,aa.y);ctx.lineTo(bb.x,bb.y)}
  function overlay(){if(tool!=='newarea'||!state)return;ctx.save();ctx.setLineDash([3,5]);ctx.lineWidth=1;ctx.strokeStyle='#a9d9cd35';ctx.beginPath();for(let i=0;i<=MAP_SIZE;i+=8){line([i,0],[i,MAP_SIZE]);line([0,i],[MAP_SIZE,i])}ctx.stroke();ctx.setLineDash([]);let box=gesture?bounds(gesture.a,gesture.b):hover&&tile(hover.u,hover.v)?bounds(hover,hover):null;if(box){ctx.beginPath();for(let [u,v]of [[box.a,box.b],[box.c+1,box.b],[box.c+1,box.d+1],[box.a,box.d+1]]){let p=screen(u,v);ctx.lineTo(p.x,p.y)}ctx.closePath();ctx.fillStyle='#8edfc633';ctx.fill();ctx.strokeStyle='#a3f7cf';ctx.lineWidth=2;ctx.stroke();let count=emptyCount(box);status(`${box.c-box.a+1} × ${box.d-box.b+1} tiles · ${count} blank tiles to create · height ${elevation}`)}ctx.restore()}
  render=function(){previousRender();overlay()};
  canvas.addEventListener('pointerdown',e=>{if(tool!=='newarea'||e.button!==0||space||e.altKey||window.world3D?.loading)return;let p=point(e);if(!tile(p.u,p.v))return;e.preventDefault();e.stopImmediatePropagation();canvas.focus();canvas.setPointerCapture(e.pointerId);gesture={a:p,b:p,h:floor};hover=p;draw()},true);
  canvas.addEventListener('pointermove',e=>{if(tool!=='newarea'||!gesture)return;e.stopImmediatePropagation();let p=point(e);gesture.b={...p,u:Math.max(0,Math.min(MAP_LAST,p.u)),v:Math.max(0,Math.min(MAP_LAST,p.v))};hover=gesture.b;draw()},true);
  canvas.addEventListener('pointerup',e=>{if(!gesture)return;e.stopImmediatePropagation();let g=gesture;gesture=null;if(tool!=='newarea'||g.h!==floor){draw();return}let count=create(bounds(g.a,g.b));hover=null;toast(count?`Created ${count} new terrain tiles. Sculpt or paint them next; Ctrl Z undoes this area.`:'That rectangle contains no blank tiles. Existing land was left unchanged.');draw()},true);
  function cancel(){if(gesture){gesture=null;draw()}}
  canvas.addEventListener('pointercancel',cancel,true);window.addEventListener('blur',cancel);document.addEventListener('keydown',e=>{if(e.key==='Escape')cancel()});$('floor').addEventListener('change',cancel);
  return {create,bounds,emptyCount,point,get elevation(){return elevation}};
})();


                                                                             
window.terrainTools=(()=>{
 let timer=null;
 const stop=()=>{if(timer!==null){clearInterval(timer);timer=null}};
 function pulse(){
  if(!['height','blend'].includes(tool)||tool==='height'&&!['raise','lower','smooth','flatten'].includes(heightMode)||drag?.kind!=='paint'||!transaction||window.world3D?.loading){stop();return;}
  if(tool==='blend'){window.groundBlender.apply(drag.u,drag.v);return;}
  const {u,v}=drag,half=Math.floor(brush/2)+1;
  transaction.sculptBase=new Map();transaction.weights=new Map();
  for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){let t=tile(x,y);if(t)transaction.sculptBase.set(floor*MAP_CELLS+y*MAP_SIZE+x,copy(t))}
  sculpt(u,v);updateHeightReadout();
 }
 canvas.addEventListener('pointerdown',e=>{stop();if(e.button===0&&['height','blend'].includes(tool)&&drag?.kind==='paint'&&transaction)timer=setInterval(pulse,150)});
 canvas.addEventListener('pointerup',stop,true);canvas.addEventListener('pointercancel',stop,true);
 window.addEventListener('blur',()=>{stop();if(drag?.kind==='paint'){commit();drag=null}});
 document.addEventListener('keydown',e=>{if(e.key==='Escape'){stop();if(drag?.kind==='paint'){commit();drag=null}}});
 function nearestColour(rgb){let best=0,score=Infinity;for(let i=0;i<256;i++){let d=groundRGB(i).reduce((n,v,k)=>n+(v-rgb[k])**2,0);if(d<score){score=d;best=i}}return best}
 function chooseGround(id){window.buildTools.showAllMaterials();const i=terrain.findIndex(t=>t.overlay===0&&t.ground===id);if(i<0)return false;choice=i;return true}
 function sample(u,v){const t=tile(u,v);if(!t||t[2]===250)return false;const overlay=t[2],ground=t[1];setTool('paint');window.buildTools.showAllMaterials();$('search').value='';if(overlay===0)chooseGround(ground);else choice=terrain.findIndex(t=>(t.overlay??t.id)===overlay);if(choice<0)choice=0;renderPalette();settings();draw();toast('Picked '+terrain[choice].name+'. Paint to reuse it.');return true}
 tools.push(['pickground','⌖ Pick colour']);
 const previousPalette=renderPalette,previousSettings=settings;
 renderPalette=function(){if(tool!=='pickground')return previousPalette();$('paletteTitle').textContent='Pick ground colour or texture';$('search').hidden=true;$('assets').replaceChildren();};
 settings=function(){
  if(tool==='pickground'){$('toolName').textContent='Pick from the map';$('toolDesc').textContent='Click land to sample it, then paint with the matching surface.';$('settings').replaceChildren();return;}
  previousSettings();if(tool!=='paint')return;
  const button=document.createElement('button');button.className='wide';button.textContent='⌖ Pick colour / texture from map';button.onclick=()=>setTool('pickground');$('settings').append(button);
  const current=terrain[choice],id=current?.ground??78,input=document.createElement('input');input.type='color';input.value='#'+groundRGB(id).map(v=>v.toString(16).padStart(2,'0')).join('');
  const label=document.createElement('p');label.className='muted';label.textContent='Choose a colour; the brush uses the closest colour supported by RSC.'+((current?.overlay??current?.id)===0?' Selected: '+current.name+'.':'');
  input.onchange=()=>{const rgb=[1,3,5].map(i=>parseInt(input.value.slice(i,i+2),16));const selected=nearestColour(rgb);chooseGround(selected);$('search').value='';renderPalette();settings();draw();toast('Ground colour '+selected+' selected (closest RSC colour).')};
  field('Ground colour picker',input);$('settings').append(label);
 };
 canvas.addEventListener('pointerdown',e=>{if(tool!=='pickground'||e.button!==0||space||e.altKey||window.world3D?.loading)return;e.preventDefault();e.stopImmediatePropagation();canvas.focus();const p=position(e);if(!sample(p.u,p.v))toast('Pick an existing ground tile, not blank space.');},true);
 return {pulse,sample,nearestColour,chooseGround};
})();

                                                                         
window.groundBlender=(()=>{
 let strength=65;
 const previousPaint=paint,previousSettings=settings,previousPalette=renderPalette;
 tools.splice(tools.findIndex(t=>t[0]==='paint')+1,0,['blend','◒ Blend']);
 function apply(u,v){
  if(!transaction)return;

  const half=Math.floor(brush/2),radius=brush/2,
        sampleRadius=Math.max(4,Math.min(14,Math.round(brush*.42))),
        sigma=Math.max(2,sampleRadius*.58),
        band=id=>Math.floor(Math.max(0,Math.min(255,id))/64),
        inside=(x,y)=>{
          const d=window.buildTools.shape==='square'
            ?Math.max(Math.abs(x-u),Math.abs(y-v))
            :Math.hypot(x-u,y-v);
          return d<radius?d:null;
        },
                                                                             
                                                                               
        bayer=[
          [0,8,2,10],
          [12,4,14,6],
          [3,11,1,9],
          [15,7,13,5]
        ];

  const source=new Map();
  for(let y=v-half-sampleRadius;y<=v+half+sampleRadius;y++)for(let x=u-half-sampleRadius;x<=u+half+sampleRadius;x++){
    const t=tile(x,y);
    if(t&&t[2]===0)source.set(`${x},${y}`,t[1]);
  }

  const changes=[];

  for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){
    const distance=inside(x,y);
    if(distance===null)continue;
    const key=`${x},${y}`,currentId=source.get(key);
    if(currentId===undefined)continue;

    const ownBand=band(currentId);
    let sameSum=0,sameWeight=0,total=[0,0,0],rgbWeight=0,sawOtherBand=false;

    for(let dy=-sampleRadius;dy<=sampleRadius;dy++)for(let dx=-sampleRadius;dx<=sampleRadius;dx++){
      const d2=dx*dx+dy*dy;
      if(d2>sampleRadius*sampleRadius)continue;
      const nid=source.get(`${x+dx},${y+dy}`);
      if(nid===undefined)continue;

      const w=Math.exp(-d2/(2*sigma*sigma));
      if(band(nid)===ownBand){sameSum+=nid*w;sameWeight+=w}
      else sawOtherBand=true;

      const c=groundRGB(nid);
      rgbWeight+=w;
      for(let k=0;k<3;k++)total[k]+=c[k]*w;
    }

    if(!sameWeight&&!rgbWeight)continue;

                                                                               
    const t=Math.max(0,Math.min(1,distance/radius));
    const falloff=.5+.5*Math.cos(Math.PI*t);
    const amount=Math.min(1,(strength/100)*(.35+.65*falloff));
    let result=currentId;

    if(!sawOtherBand&&sameWeight){
                                                                          
                                                                             
      const target=sameSum/sameWeight;
      const blended=currentId+(target-currentId)*amount;
      const lo=Math.floor(blended),hi=Math.ceil(blended),frac=blended-lo;
      const threshold=(bayer[(y&3)][(x&3)]+.5)/16;
      result=frac>threshold?hi:lo;
      result=Math.max(ownBand*64,Math.min(ownBand*64+63,result));
    }else if(rgbWeight){
                                                                        
                                                                        
      const avg=total.map(v=>v/rgbWeight),cur=groundRGB(currentId),
            mixed=cur.map((c,k)=>c+(avg[k]-c)*Math.min(.85,amount*.72));
      result=window.terrainTools.nearestColour(mixed);
    }

    if(result!==currentId)changes.push([x,y,result]);
  }

  for(const [x,y,id]of changes)touch(x,y)[1]=id;
  draw();
 }
 paint=function(u,v){if(tool==='blend')return apply(u,v);return previousPaint(u,v)};
 renderPalette=function(){if(tool!=='blend')return previousPalette();$('paletteTitle').textContent='Blend existing ground colours';$('search').hidden=true;$('assets').replaceChildren();};
 settings=function(){
  previousSettings();if(tool!=='blend')return;
  $('toolName').textContent='Blend ground colours';$('toolDesc').textContent='Soften colours already painted on the ground.';$('mapHint').textContent='Drag across colour edges and hold to blend more, or press Ctrl Z to Undo.';
  const input=document.createElement('input');input.type='range';input.min=10;input.max=100;input.value=strength;
  const value=document.createElement('p');value.className='muted';value.textContent=strength+'%';input.oninput=()=>{strength=+input.value;value.textContent=strength+'%'};
  field('Blend strength',input);$('settings').append(value);
 };
 return {apply};
})();

                                                                        
window.buildingSurfaces=(()=>{
 let roof=1,openingMode='cut',floorMaterial=3;
 const oldPaint=paint,oldSettings=settings,oldPalette=renderPalette;
 tools.splice(tools.findIndex(t=>t[0]==='build')+1,0,['roof','⌂ Paint roof'],['opening','□ Floor opening']);
 const roofs=[[1,'Tiled roof'],[2,'Planks'],[3,'Stone'],[4,'Thatch'],[5,'Marble'],[6,'Tent / fabric'],[0,'Remove roof']];
 function apply(u,v){
  if(!transaction)return;
  if(tool==='opening'&&![0,1,2].includes(floor))return;
  const half=Math.floor(brush/2);
  for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){
   if(window.buildTools.shape==='round'&&Math.hypot(x-u,y-v)>brush/2)continue;
   const value=tool==='roof'?roof:openingMode==='cut'?(floor===0?10:0):floorMaterial;
   const t=value!==0?editableFloorTile(x,y):tile(x,y);if(!t||t[2]===250)continue;
   const slot=tool==='roof'?3:2;
   if(t[slot]!==value)touch(x,y)[slot]=value;
  }
  draw();
 }
 paint=function(u,v){if(['roof','opening'].includes(tool))return apply(u,v);return oldPaint(u,v)};
 renderPalette=function(){if(!['roof','opening'].includes(tool))return oldPalette();$('paletteTitle').textContent=tool==='roof'?'Roof your existing building':'Openings for ladders and stairs';$('search').hidden=true;$('assets').replaceChildren();
  ;
 };
 settings=function(){oldSettings();if(!['roof','opening'].includes(tool))return;
  $('toolName').textContent=tool==='roof'?'Paint roof':'Cut or fill floor openings';
  $('toolDesc').textContent=tool==='roof'?'Add a roof above the selected storey without rebuilding its walls.':'Cut an opening for ladders or stairs without removing walls or scenery.';
  $('mapHint').textContent='Click or drag to paint, or press Ctrl Z to Undo.';
  if(tool==='opening'&&floor!==0&&floorMaterial===0)floorMaterial=3;
  if(tool==='roof'){
   field('Roof style',select(roofs,roof,v=>{roof=+v;draw()}));
   const show=document.createElement('button');show.className='wide';show.textContent='Show roofs in 3D';show.onclick=()=>{$('roofs3d').checked=true;window.world3D?.invalidate();draw()};$('settings').append(show);
  }else{
   field('Opening action',select([['cut','Cut opening'],['fill','Fill opening']],openingMode,v=>{openingMode=v;settings()}));
   if(openingMode==='fill')field('Floor material',select([...(floor===0?[[0,'Natural ground']]:[]),[3,'Timber'],[5,'Stone'],[13,'Blue'],[15,'Purple']],floorMaterial,v=>floorMaterial=+v));
   const note=document.createElement('p');note.className='notice';note.textContent=[0,1,2].includes(floor)?'Use a 1-tile brush for a ladder, or a larger brush / drag for stairs. Add the matching ladder-down or stair scenery separately. This tool changes the floor surface; it does not create a travel destination.':'Choose Ground floor, Upper floor 1 or Upper floor 2 to cut an opening.';$('settings').append(note);
  }
 };
 const oldTool=setTool;setTool=function(name){if(['roof','opening'].includes(name)){brush=1;if(name==='roof'){$('roofs3d').checked=true;window.world3D?.invalidate()}}return oldTool(name)};
 return {apply};
})();

                                                                               
window.lowerWallGuide=(()=>{
 const label=document.createElement('label'),toggle=document.createElement('input');
 toggle.type='checkbox';toggle.checked=true;toggle.id='lowerWallGuide';
 label.title='Show translucent wall outlines from the floor below for alignment';label.append(toggle,' Lower walls');
 $('cameraTools').append(label);toggle.onchange=()=>draw();
 function segments(){
  if(!state||![1,2].includes(floor))return [];
  const result=[];
  for(let v=0;v<MAP_SIZE;v++)for(let u=0;u<MAP_SIZE;u++){
   const t=tile(u,v,floor-1);if(!t)continue;
   if(t[4])result.push([u+1,v,u+1,v+1,t[4]]);
   if(t[5])result.push([u,v,u+1,v,t[5]]);
   if(t[6]>0&&t[6]<24000)result.push(diagonalWall(u,v,t[6]));
  }
  for(const r of state.boundaries){const p=local(r);if(p.h!==floor-1)continue;
   if(r.direction===0)result.push([p.u,p.v,p.u+1,p.v,r.id+1]);
   else if(r.direction===1)result.push([p.u+1,p.v,p.u+1,p.v+1,r.id+1]);
   else if(r.direction===2)result.push([p.u,p.v,p.u+1,p.v+1,r.id+1]);
   else if(r.direction===3)result.push([p.u+1,p.v,p.u,p.v+1,r.id+1]);
  }
  return result;
 }
 function overlay(){
  label.hidden=![1,2].includes(floor);if(label.hidden||!toggle.checked||!state)return;
  ctx.save();ctx.setTransform(1,0,0,1,0,0);ctx.strokeStyle='rgba(143,225,245,.6)';ctx.fillStyle='rgba(143,225,245,.12)';ctx.lineWidth=1.25;
  const is3d=window.world3D?.active;
  for(const [u,v,uu,vv,id]of segments()){
   if(hiddenBarrierWall(id))continue;
   const height=(window.world3D?.catalog.walls[id-1]?.height??192)/128;
   const top=(x,y)=>is3d?window.world3D.project(x,y):{x:panX+x*scale,y:panY+y*scale};
   const a=top(u,v),b=top(uu,vv);ctx.beginPath();ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);
   if(is3d){const c=window.world3D.project(uu,vv,terrainVertexHeight(uu,vv)-height),d=window.world3D.project(u,v,terrainVertexHeight(u,v)-height);ctx.lineTo(c.x,c.y);ctx.lineTo(d.x,d.y);ctx.closePath();ctx.fill()}
   ctx.stroke();
  }
  ctx.restore();
 }
 const oldRender=render;render=function(){oldRender();overlay()};
 return {segments};
})();
