'use strict';
const MAP_SIZE=288,MAP_LAST=MAP_SIZE-1,MAP_CELLS=MAP_SIZE*MAP_SIZE,MAP_HALF=MAP_SIZE/2;

const $=id=>document.getElementById(id),copy=v=>JSON.parse(JSON.stringify(v));

let state,assets,token,tool='paint',choice=0,floor=0,brush=3,rotation=0,selection=null,hover=null,drag=null,dirty=false,history=[],future=[],transaction=null,clipboard=null,customPrefabs=[],previewAngle=888;
let wallDefs=[],wallMagicSelection=new Set(),surfaceMagicSelection=new Set();

let scale=5,panX=0,panY=0,cw=1,ch=1,space=false,frame=0;

const canvas=$('map'),ctx=canvas.getContext('2d');
const mapDimensions=document.createElement('span');mapDimensions.className='muted';mapDimensions.style.display='block';mapDimensions.textContent='288 × 288 tiles · 6 × 6 chunks';$('areaName').after(mapDimensions);

const terrain=[{id:0,name:'Grass',color:'#779060'},{id:'sand',overlay:0,ground:130,name:'Al Kharid sand',color:'#bd8d00'},{id:2,name:'Water',color:'#438399'},{id:1,name:'Path',color:'#b8a184'},{id:5,name:'Stone floor',color:'#b8bbac'},{id:3,name:'Timber deck',color:'#ae8351'},{id:6,name:'Red floor',color:'#b27c68'},{id:13,name:'Blue floor',color:'#769fb2'},{id:15,name:'Purple floor',color:'#967caf'},{id:11,name:'Lava',color:'#d17c48'}];

terrain.push({id:'lush',overlay:0,ground:66,name:'Lush grass',color:'#069000'},{id:'dry',overlay:0,ground:110,name:'Dry grass',color:'#8a9000'},{id:'earth',overlay:0,ground:192,name:'Earth',color:'#603000'});
const materials=Object.fromEntries(terrain.map(t=>[t.id,t]));

const tools=[['paint','▧ Paint'],['height','▲ Height'],['build','⌂ Build'],['stamp','▣ Stamp'],['objects','♣ Scenery'],['npcs','♟ People'],['items','◆ Items'],['select','Select & Inspect'],['erase','◇ Erase']];

let heightMode='raise',heightStep=6,heightTarget=110;

let modelIndex={},modelImages=new Map();

let npcRadius=3;
let paintMode='solid',paintStrength=65,protectPaintedTextures=true;
const toolMemory={};
let lastTarget=null;
let buildStyle={wall:15,floor:3,roof:1,door:'auto'},eraseMode='all';

let builtin=[{name:'Timber house',type:'house',w:10,h:10},{name:'Stone shop',type:'shop',w:12,h:10},{name:'Watchtower',type:'tower',w:8,h:8},{name:'Stone bridge',type:'bridge',w:7,h:13},{name:'Timber pier',type:'pier',w:7,h:17}];

function toast(s){$('toast').textContent=s;$('toast').classList.add('show');clearTimeout(toast.timer);toast.timer=setTimeout(()=>$('toast').classList.remove('show'),6500)}

function status(s){$('statusText').textContent=s}

async function api(path,data){let r=await fetch(path,data===undefined?{}:{method:'POST',headers:{'Content-Type':'application/json','X-Workshop-Token':token},body:JSON.stringify(data)});let b=await r.json();if(!r.ok)throw Error(b.error||'The action failed');return b}

async function busy(text,fn){$('busyText').textContent=text;$('busy').hidden=false;try{return await fn()}catch(e){toast(e.message);status(e.message);return null}finally{$('busy').hidden=true}}

function mark(full=true){if(full)window.world3D?.invalidate();dirty=true;$('saveState').textContent='Unsaved changes';status('Draft changed · live game unchanged');draw()}

function tile(u,v,h=floor){return u>=0&&v>=0&&u<MAP_SIZE&&v<MAP_SIZE?state.tiles[h]?.[v*MAP_SIZE+u]:null}

function begin(){transaction={tiles:new Map(),before:{},region:{xmax:state.xmax,ymin:state.ymin},view:window.world3D?.camera?{u:window.world3D.camera.u,v:window.world3D.camera.v,y:window.world3D.camera.y}:null};for(const k of ['objects','npcs','boundaries','items'])transaction.before[k]=copy(state[k])}

function touch(u,v,h=floor){window.world3D?.invalidateTile(u,v,h);let t=tile(u,v,h);if(!t)return null;let key=h*MAP_CELLS+v*MAP_SIZE+u;if(transaction&&!transaction.tiles.has(key))transaction.tiles.set(key,copy(t));return t}

                                                                         
                                                                                
function editableFloorTile(u,v){let t=tile(u,v);if(!t)return null;if(t[2]===250){if(floor===0)return null;t=touch(u,v);t[2]=0;}return t;}

function commit(){if(!transaction)return;let tx=transaction;transaction=null;tx.after={};let differs=tx.tiles.size>0,placementsChanged=false;for(const k of ['objects','npcs','boundaries','items']){tx.after[k]=copy(state[k]);if(JSON.stringify(tx.before[k])!==JSON.stringify(tx.after[k])){differs=true;placementsChanged=true}}if(!differs)return;tx.tiles=[...tx.tiles].map(([key,before])=>({key,before,after:copy(state.tiles[Math.floor(key/MAP_CELLS)][key%MAP_CELLS])}));history.push(tx);if(history.length>60)history.shift();future=[];mark(placementsChanged);updateUndo()}

function applyTx(tx,side){for(const p of tx.tiles)state.tiles[Math.floor(p.key/MAP_CELLS)][p.key%MAP_CELLS]=copy(p[side]);for(const k in tx[side])state[k]=copy(tx[side][k]);mark();updateUndo();inspect()}

let historyBusy=false;
function changeHistory(from,to,side){
  if(historyBusy||window.world3D?.loading)return;
  const tx=from[from.length-1];if(!tx){updateUndo();return;}
  const sameRegion=()=>!tx.region||(state.xmax===tx.region.xmax&&state.ymin===tx.region.ymin);
  const apply=()=>{from.pop();to.push(tx);applyTx(tx,side);status(side==='before'?'Undid last edit':'Redid last edit')};
  if(sameRegion()){apply();return;}
                                                                             
                                                                              
  historyBusy=true;updateUndo();
  return (async()=>{
    try{
      const loaded=await window.world3D.navigate(tx.region.xmax-MAP_HALF,tx.region.ymin+MAP_HALF);
      if(!loaded||!sameRegion()){toast('Could not open the edited area. Your undo history is preserved; try again.');return;}
      if(tx.view)Object.assign(window.world3D.camera,tx.view);
      apply();
    }catch(error){toast('Could not undo or redo: '+error.message)}
    finally{historyBusy=false;updateUndo();draw()}
  })();
}
function undo(){return changeHistory(history,future,'before')}
function redo(){return changeHistory(future,history,'after')}
function updateUndo(){$('undo').disabled=historyBusy||!history.length;$('redo').disabled=historyBusy||!future.length}

function worldMaxX(){return state?.maxX??943}
function worldMinX(){return state?.minX??0}
function world(u,v,h=floor){return {X:state.xmax-u,Y:state.ymin+v+h*944}}

function local(r){let p=r.pos||r.start;return {u:state.xmax-p.X,v:p.Y%944-state.ymin,h:Math.floor(p.Y/944)}}

function area(){if(!selection)return null;return {a:Math.min(selection.u,selection.x),b:Math.min(selection.v,selection.y),c:Math.max(selection.u,selection.x),d:Math.max(selection.v,selection.y)}}

function inside(r,a,b,c,d,h=floor){let p=local(r);return p.h===h&&p.u>=a&&p.u<=c&&p.v>=b&&p.v<=d}

function removeAt(a,b,c,d,h=floor,keys=['objects','npcs','boundaries','items']){for(let k of keys)state[k]=state[k].filter(r=>!inside(r,a,b,c,d,h))}

function rememberTool(){toolMemory[tool]={choice,rotation,search:$('search').value||'',scroll:$('assets').scrollTop||0}}
function setTool(name){window.world3D?.editing();rememberTool();tool=name;wallMagicSelection.clear();surfaceMagicSelection.clear();let saved=toolMemory[name];choice=saved?.choice??0;rotation=saved?.rotation??0;$('search').value=saved?.search??'';selection=null;renderTools();renderPalette();$('assets').scrollTop=saved?.scroll??0;settings();draw()}
function heightReading(){let point=hover||lastTarget;if(!state||!point)return null;let t=tile(point.u,point.v);if(!t)return null;let p=world(point.u,point.v);return {height:t[0],X:p.X,Y:p.Y,material:tileMaterial(t)?.name||'Terrain'}}
function updateHeightReadout(){let r=heightReading();$('groundHeight').textContent=r?String(r.height):'N/A';$('heightLocation').textContent=r?`World ${r.X}, ${r.Y} · ${r.material}`:'Point at a tile to see its height';$('heightContext').textContent=r?(hover?'Tile height under your mouse':'Last pointed tile'):'Available with every tool';let tip=$('heightTooltip');tip.hidden=!hover||!r;if(r){$('coords').textContent=`World ${r.X}, ${r.Y} · ${r.material} · Height ${r.height}`;tip.textContent=`Height ${r.height}`;if(hover){tip.style.left=Math.max(4,Math.min(cw-112,hover.x+18))+'px';tip.style.top=Math.max(4,Math.min(ch-36,hover.y+22))+'px'}}}


function renderTools(){$('paletteTitle').hidden=!['newarea','paint','walls','stamp','objects','npcs','items'].includes(tool);$('tools').replaceChildren();for(const [id,label] of tools){let b=document.createElement('button');b.textContent=label.replace(/^[^A-Za-z]+/,'');b.className=tool===id?'active':'';b.onclick=()=>setTool(id);$('tools').append(b)}}

function entries(){if(tool==='paint')return terrain;if(tool==='objects')return assets.objects;if(tool==='npcs')return assets.npcs;if(tool==='items')return assets.items;if(tool==='stamp')return [...builtin,...customPrefabs];return []}

function symbol(c,item,kind){const g=c.getContext('2d');g.fillStyle='#233a41';g.fillRect(0,0,96,48);g.strokeStyle='#e4d0a1';g.lineWidth=2;if(kind==='paint'){g.fillStyle=item.color;g.fillRect(8,7,80,34);if(item.id===2){g.strokeStyle='#a6c6cf';for(let y=15;y<40;y+=8){g.beginPath();g.moveTo(17,y);g.lineTo(78,y);g.stroke()}}}else if(kind==='npcs'){g.fillStyle='#d9b984';g.beginPath();g.arc(48,14,6,0,7);g.fill();g.fillRect(39,23,18,16);g.fillRect(39,37,6,7);g.fillRect(51,37,6,7)}else if(kind==='stamp'){g.fillStyle='#c4bca6';g.fillRect(25,19,46,22);g.fillStyle='#b58558';g.beginPath();g.moveTo(18,20);g.lineTo(48,5);g.lineTo(78,20);g.fill();g.fillStyle='#233a41';g.fillRect(44,28,8,13)}else{let n=item.name.toLowerCase();if(/tree|bush|hedge/.test(n)){g.fillStyle='#b79c6d';g.fillRect(44,27,8,16);g.fillStyle='#78a06c';g.beginPath();g.arc(48,18,15,0,7);g.fill()}else if(/fountain|well/.test(n)){g.fillStyle='#81a6b0';g.beginPath();g.ellipse(48,30,23,10,0,0,7);g.fill();g.fillStyle='#c6c4b7';g.fillRect(45,10,6,23)}else{g.fillStyle='#b49c78';g.fillRect(28,12,40,28);g.strokeRect(28,12,40,28);g.beginPath();g.moveTo(28,12);g.lineTo(68,40);g.moveTo(68,12);g.lineTo(28,40);g.stroke()}}}

function renderPalette(){let titles={paint:'Terrain brushes',height:'Sculpt the terrain',build:'Draw a building',stamp:'Reusable structures',objects:'Scenery library',npcs:'People of the world',items:'Placeable ground items',select:'Make your own prefab',inspect:'Explore your map',erase:'Make room for something new'};$('paletteTitle').textContent=titles[tool];$('search').hidden=!['objects','npcs','items','stamp'].includes(tool);$('assets').replaceChildren();if(tool==='stamp'){let intro=document.createElement('p');intro.className='muted';intro.textContent='Here are some premade structures that you can add to your world.';$('assets').append(intro)}let list=entries(),q=$('search').value.toLowerCase();let shown=0;list.forEach((e,i)=>{if(e.deleted||!(e.name+' '+(e.description||'')).toLowerCase().includes(q)||shown++>=180)return;let b=document.createElement('button');b.className='asset'+(choice===i?' selected':'');b.title=e.name+(e.description?"\nExamine: "+e.description:"");let c=document.createElement('canvas');c.width=128;c.height=96;if(tool==='objects'){c.className='model-thumbnail';paintModelCard(c,e.id,0)}else symbol(c,e,tool);let span=document.createElement('span');span.textContent=e.name;b.append(c,span);b.onclick=()=>{choice=i;rotation=0;renderPalette();settings();draw()};$('assets').append(b)});}

function field(label,input){input.setAttribute('aria-label',label);let el=document.createElement('label');el.className='field';el.textContent=label;el.append(input);$('settings').append(el)}

function select(options,value,onchange){let s=document.createElement('select');for(let [v,n] of options){let o=document.createElement('option');o.value=v;o.textContent=n;s.append(o)}s.value=value;s.onchange=()=>onchange(s.value);return s}

function settings(){queueMicrotask(()=>window.world3D?.hint());let names={paint:'Terrain brush',height:'Shape the ground',build:'Draw a building',stamp:'Place a structure',objects:'Place scenery',npcs:'Place a person',items:'Place an item',select:'Select & Inspect',erase:'Eraser'};$('toolName').textContent=names[tool];$('toolDesc').textContent={paint:'Choose a surface and paint it onto your map.',height:'Drag to raise hills, lower valleys, smooth slopes or level a building site.',build:'Drag a footprint. The editor does the construction.',stamp:'One click places a complete, reusable structure.',objects:'Choose an asset, then click its position.',items:'Choose an item and click a tile, including a table tile. These are collectible item spawns.',npcs:'Choose how far this NPC can wander. Walls and blocked terrain still limit its movement.',select:'Use this tool to keep your favourite designs and reuse them again!',erase:'Clear only the layers you choose.'}[tool];$('mapHint').textContent=tool==='height'?'Drag to shape the ground, use 3D to view slopes or press Ctrl Z to Undo.':tool==='objects'?'Click to place an object, press R to rotate or drag with the right mouse button to move the view.':'Drag to edit, drag with the right mouse button to move the view or scroll to zoom.';$('settings').replaceChildren();if(['paint','blend','height','erase','roof','opening'].includes(tool)){let r=document.createElement('input');r.type='range';r.min=1;r.max=11;r.step=2;r.value=brush;r.oninput=()=>{brush=+r.value;$('brushValue').textContent=brush+' tiles'};field('Brush size',r);let n=document.createElement('span');n.id='brushValue';n.className='muted';n.textContent=brush+' tiles';$('settings').append(n)}if(tool==='paint')paintSettings();if(tool==='height')heightSettings();if(tool==='npcs')npcSettings();if(tool==='items')itemSettings();if(tool==='build'){field('Walls',select([[15,'Timber'],[1,'Stone'],[8,'Tall stone'],[5,'Standard fence'],[128,'Low fence'],[6,'Railings']],buildStyle.wall,v=>buildStyle.wall=+v));field('Floor',select([[3,'Wood'],[5,'Stone'],[13,'Blue'],[15,'Purple']],buildStyle.floor,v=>buildStyle.floor=+v));field('Roof',select([[1,'Red tiles'],[2,'Wood'],[3,'Stone'],[0,'Open courtyard']],buildStyle.roof,v=>buildStyle.roof=+v));field('Entrance',select([['auto','Automatic: ground entrance only'],['none','None: fully enclosed'],['S','South'],['N','North'],['E','East'],['W','West']],buildStyle.door,v=>buildStyle.door=v))}if(tool==='stamp'){field('Built-in entrance',select([['auto','Automatic: ground entrance only'],['none','None: fully enclosed'],['S','South'],['N','North'],['E','East'],['W','West']],buildStyle.door,v=>{buildStyle.door=v;draw()}))}if(['objects','stamp'].includes(tool)){let b=document.createElement('button');b.className='wide';b.textContent='Rotate ↻  '+rotation+'°  · R';b.onclick=()=>{rotation=(rotation+(tool==='objects'?45:90))%360;settings();draw()};$('settings').append(b);let p=document.createElement('div');p.className='notice';p.textContent=entries()[choice]?.name||'Choose an asset';$('settings').append(p);if(tool==='objects'){field('Rotation',select(Array.from({length:8},(_,i)=>[i*45,i*45+'°'+(i%2?' · diagonal':'')]),rotation,v=>{rotation=+v;settings();draw()}));modelSettings()}}if(tool==='erase')field('Clear',select([['void','Remove land → blank space'],['all','Everything on this floor'],['placements','Scenery & people'],['walls','Walls only'],['roof','Roof only'],['wallsroof','Walls & roof'],['terrain','Floor textures (empty on upper floors)']],eraseMode,v=>{eraseMode=v;settings()}));if(tool==='erase'&&eraseMode==='void'){let note=document.createElement('p');note.className='notice';note.textContent='Returns land to blank space and removes walls, roofs, scenery, NPCs and items anchored on those tiles, on the selected floor only. Ctrl Z restores the stroke. Use a small brush to trim edges.';$('settings').append(note)}if(tool==='select'){for(let [label,fn] of [['Copy selection · Ctrl C',copySelection],['Paste · Ctrl V',pasteMode],['Save as prefab',savePrefab],['Delete selection',deleteSelection],['Delete land in selection',deleteLandSelection]]){let b=document.createElement('button');b.className='wide';b.textContent=label;b.onclick=fn;$('settings').append(b)}}}

function footprint(r){let p=local(r),d=assets.objects[r.id],w=d?.width||1,h=d?.height||1;if(r.direction%4!==0)[w,h]=[h,w];return {...p,w,depth:h}}

                                                                             
function diagonalWall(u,v,value){return value<12000?[u+1,v,u,v+1,value]:[u,v,u+1,v+1,value-12000]}

function wallDef(id){return wallDefs.find(w=>w.id===id)||null}
function wallLabel(id){let d=wallDef(id);return d?`${d.kind||d.name} · #${id} · ${d.blocked?'BLOCKING':'PASSABLE'}`:`Wall #${id}`}
function wallSegments(h=floor){
 const out=[];
 for(let v=0;v<MAP_SIZE;v++)for(let u=0;u<MAP_SIZE;u++){
  const t=tile(u,v,h);if(!t)continue;
  if(t[4])out.push({key:`${u},${v},4`,u,v,slot:4,id:t[4],a:[u+1,v],b:[u+1,v+1]});
  if(t[5])out.push({key:`${u},${v},5`,u,v,slot:5,id:t[5],a:[u,v],b:[u+1,v]});
  if(t[6]>0&&t[6]<24000){let [ax,ay,bx,by,id]=diagonalWall(u,v,t[6]);out.push({key:`${u},${v},6`,u,v,slot:6,id,a:[ax,ay],b:[bx,by]})}
 }
 return out;
}
function wallSegmentsAt(u,v,h=floor){return wallSegments(h).filter(s=>s.u===u&&s.v===v)}
function endpointKey(p){return p[0]+','+p[1]}
function magicSelectWall(seed){
 const all=wallSegments(),byEnd=new Map(),byKey=new Map(all.map(s=>[s.key,s]));
 for(const s of all)for(const p of [s.a,s.b]){let k=endpointKey(p);if(!byEnd.has(k))byEnd.set(k,[]);byEnd.get(k).push(s)}
 const selected=new Set(),queue=[seed.key];
 while(queue.length){let key=queue.pop(),s=byKey.get(key);if(!s||selected.has(key)||s.id!==seed.id)continue;selected.add(key);for(const p of [s.a,s.b])for(const n of byEnd.get(endpointKey(p))||[])if(n.id===seed.id&&!selected.has(n.key))queue.push(n.key)}
 wallMagicSelection=selected;draw();inspect();toast(`Selected ${selected.size} connected ${wallDef(seed.id)?.kind||'wall'} segment${selected.size===1?'':'s'} (#${seed.id}).`)
}
function selectedWallSegments(){let map=new Map(wallSegments().map(s=>[s.key,s]));return [...wallMagicSelection].map(k=>map.get(k)).filter(Boolean)}
function setSelectedWallType(id){
 id=Math.round(+id);if(!Number.isInteger(id)||id<1||id>11999){toast('Enter a valid wall ID.');return}
 let list=selectedWallSegments();if(!list.length){toast('Magic-select a wall first.');return}
 begin();for(const s of list){let t=touch(s.u,s.v);if(s.slot===6)t[6]=t[6]>=12000?id+12000:id;else t[s.slot]=id}commit();wallMagicSelection=new Set(list.map(s=>s.key));inspect();draw();
}
function removeSelectedWalls(){let list=selectedWallSegments();if(!list.length)return;begin();for(const s of list){let t=touch(s.u,s.v);t[s.slot]=0}commit();wallMagicSelection.clear();inspect();draw()}


function surfaceKey(t){
  if(!t||t[2]===250)return null;
  return t[2]===0?`ground:${t[1]}`:`overlay:${t[2]}`;
}
function surfaceTileKey(u,v){return `${u},${v}`}
function magicSelectSurface(u,v){
  const seed=tile(u,v);
  if(!seed||seed[2]===250){toast('Choose a painted ground, path or water tile.');return}
  const target=surfaceKey(seed),selected=new Set(),queue=[[u,v]];
  while(queue.length){
    const [x,y]=queue.pop(),key=surfaceTileKey(x,y);
    if(selected.has(key)||x<0||y<0||x>=MAP_SIZE||y>=MAP_SIZE)continue;
    const t=tile(x,y);
    if(surfaceKey(t)!==target)continue;
    selected.add(key);
    queue.push([x+1,y],[x-1,y],[x,y+1],[x,y-1]);
  }
  surfaceMagicSelection=selected;
  draw();inspect();
  toast(`Selected ${selected.size} connected surface tile${selected.size===1?'':'s'}.`);
}
function selectedSurfaceTiles(){
  const out=[];
  for(const key of surfaceMagicSelection){
    const [u,v]=key.split(',').map(Number),t=tile(u,v);
    if(t)out.push({u,v,t});
  }
  return out;
}
function adjustSelectedSurfaceHeight(delta){
  delta=Math.round(+delta);
  if(!Number.isFinite(delta)||delta===0){toast('Enter a non-zero height amount.');return}
  const list=selectedSurfaceTiles();
  if(!list.length){toast('Magic-select a surface first.');return}
  begin();
  for(const {u,v} of list){
    const t=touch(u,v);
    t[0]=Math.max(0,Math.min(255,t[0]+delta));
  }
  commit();
  inspect();draw();
  status(`${delta<0?'Lowered':'Raised'} ${list.length} selected surface tile${list.length===1?'':'s'} by ${Math.abs(delta)}.`);
}

function draw(){if(!state||frame)return;frame=requestAnimationFrame(render)}

function hiddenBarrierWall(id){if(!$('hideBarriers')?.checked)return false;const d=wallDef(id)||window.world3D?.catalog?.walls?.[id-1];return [17,18,87,120].includes(id)||!!(d&&d.texture===12345678&&d.back===12345678);}
function hiddenBarrierTile(id){return $('hideBarriers')?.checked && id===8;}
function render(){frame=0;if(window.world3D?.active){window.world3D.render();return;}ctx.setTransform(1,0,0,1,0,0);ctx.fillStyle=(floor===1||floor===2)?'#000000':'#c5cbbb';ctx.fillRect(0,0,cw,ch);ctx.save();ctx.translate(panX,panY);ctx.scale(scale,scale);const u0=Math.max(0,Math.floor(-panX/scale)-1),v0=Math.max(0,Math.floor(-panY/scale)-1),u1=Math.min(MAP_SIZE,Math.ceil((cw-panX)/scale)+1),v1=Math.min(MAP_SIZE,Math.ceil((ch-panY)/scale)+1);let blocked=$('blocked').checked;for(let v=v0;v<v1;v++)for(let u=u0;u<u1;u++){let t=tile(u,v);ctx.fillStyle=(t[2]===0?groundColor(t[1]):tileMaterial(t)?.color)||(t[2]===250?'#364849':'#779060');const emptyUpper=(floor===1||floor===2)&&(t[2]===0||t[2]===250);if(emptyUpper)ctx.fillStyle='#000000';if(t[3]&&!emptyUpper)ctx.fillStyle=t[3]===3?'#74858a':t[3]===2?'#a08a64':'#9b7760';ctx.fillRect(u,v,1.02,1.02);if(!emptyUpper&&$('showSlopes').checked){let west=tile(u-1,v)?.[0]??t[0],north=tile(u,v-1)?.[0]??t[0],shade=Math.max(-.52,Math.min(.52,(t[0]-110)/330+(t[0]-west+t[0]-north)/55));ctx.fillStyle=shade>=0?`rgba(255,248,202,${shade})`:`rgba(15,33,48,${-shade})`;ctx.fillRect(u,v,1.02,1.02)}if(hiddenBarrierTile(t[2])){ctx.fillStyle=(floor===1||floor===2)?'#000':groundColor(t[1]);ctx.fillRect(u,v,1.02,1.02)}if(t[2]===8&&!hiddenBarrierTile(t[2])){ctx.fillStyle='#000';ctx.fillRect(u,v,1.02,1.02)}if(t[6]>0&&t[6]<24000&&!hiddenBarrierWall(t[6]>12000?t[6]-12000:t[6])){const c=groundCorner(u,v,terrainCornerDefs());if(groundCornerInvisible(u,v,c)){const pts=[[u,v],[u+1,v],[u+1,v+1],[u,v+1]];ctx.fillStyle='#000';ctx.beginPath();for(const i of [(c+3)%4,c,(c+1)%4])ctx.lineTo(...pts[i]);ctx.closePath();ctx.fill()}}if(blocked&&!hiddenBarrierTile(t[2])&&assets.blocked.includes(t[2])){ctx.fillStyle='#ae484578';ctx.fillRect(u,v,1,1)}}if(scale>11){ctx.lineWidth=.04;ctx.strokeStyle='#10242a18';ctx.beginPath();for(let i=0;i<=MAP_SIZE;i++){ctx.moveTo(i,0);ctx.lineTo(i,MAP_SIZE);ctx.moveTo(0,i);ctx.lineTo(MAP_SIZE,i)}ctx.stroke()}ctx.strokeStyle='#354447';ctx.lineWidth=Math.max(.13,1/scale);ctx.beginPath();for(let v=v0;v<v1;v++)for(let u=u0;u<u1;u++){let t=tile(u,v);if(t[4]&&!hiddenBarrierWall(t[4])){ctx.moveTo(u+1,v);ctx.lineTo(u+1,v+1)}if(t[5]&&!hiddenBarrierWall(t[5])){ctx.moveTo(u,v);ctx.lineTo(u+1,v)}if(t[6]>0&&t[6]<24000&&!hiddenBarrierWall(t[6]>12000?t[6]-12000:t[6])){ctx.moveTo(u+(t[6]<12000?1:0),v);ctx.lineTo(u+(t[6]<12000?0:1),v+1)}}ctx.stroke();if(wallMagicSelection.size){ctx.strokeStyle='#ffd36a';ctx.lineWidth=Math.max(.35,3/scale);ctx.beginPath();for(const s of selectedWallSegments()){if(hiddenBarrierWall(s.id))continue;ctx.moveTo(...s.a);ctx.lineTo(...s.b)}ctx.stroke()}
if(surfaceMagicSelection.size){
  ctx.fillStyle='#7fe7ff33';
  ctx.strokeStyle='#7fe7ff';
  ctx.lineWidth=Math.max(.18,1.5/scale);
  for(const {u,v} of selectedSurfaceTiles()){
    ctx.fillRect(u,v,1,1);
    ctx.strokeRect(u+.05,v+.05,.9,.9);
  }
}for(let r of state.objects){let p=footprint(r);if(p.h!==floor)continue;let d=assets.objects[r.id];drawMapModel(r.id,r.direction,p.u-p.w+1,p.v,p.w,p.depth,false);if(blocked&&d?.solid){let hh=(r.direction%4===0?d.height:d.width);ctx.fillStyle='#b14e5477';ctx.fillRect(p.u-p.w+1,p.v,p.w,hh)}}for(let r of state.npcs){let p=local(r);if(p.h===floor)window.placementImages?.draw2D('npcs',r,p)}for(let r of state.boundaries){let p=local(r);if(p.h!==floor||hiddenBarrierWall(r.id+1))continue;ctx.fillStyle='#b25252';ctx.fillRect(p.u+.1,p.v+.1,.8,.8)}for(let r of state.items){let p=local(r);if(p.h===floor)window.placementImages?.draw2D('items',r,p)}let a=area();if(a){ctx.fillStyle='#f5d89225';ctx.fillRect(a.a,a.b,a.c-a.a+1,a.d-a.b+1);ctx.strokeStyle='#fff0b7';ctx.lineWidth=2/scale;ctx.strokeRect(a.a,a.b,a.c-a.a+1,a.d-a.b+1)}if(hover){let {u,v}=hover,w=brush,h=brush,x=u-Math.floor(brush/2),y=v-Math.floor(brush/2);if(tool==='objects'){let o=entries()[choice];w=rotation%180?o?.height:o?.width;h=rotation%180?o?.width:o?.height;x=u-w+1;y=v}else if(tool==='stamp'){let p=stampData();w=p?.w||1;h=p?.h||1;x=u;y=v}else if(!['paint','blend','height','erase','roof','opening'].includes(tool)){w=h=1;x=u;y=v}ctx.fillStyle='#f6e0a228';ctx.fillRect(x,y,w,h);ctx.strokeStyle='#fff6d4';ctx.lineWidth=1.5/scale;ctx.strokeRect(x,y,w,h);if(tool==='npcs'&&npcRadius>0){ctx.save();ctx.strokeStyle='#ffe4a1';ctx.setLineDash([3/scale,3/scale]);ctx.strokeRect(u-npcRadius,v-npcRadius,npcRadius*2+1,npcRadius*2+1);ctx.restore()}if(tool==='objects'){let o=entries()[choice];if(o)drawMapModel(o.id,rotation/45,x,y,w,h,true)}}ctx.restore();updateHeightReadout();$('zoomValue').textContent=Math.round(scale/5*100)+'%'}

function fit(){if(window.world3D?.active){window.world3D.fit();return;}scale=Math.min((cw-50)/MAP_SIZE,(ch-50)/MAP_SIZE);panX=(cw-MAP_SIZE*scale)/2;panY=(ch-MAP_SIZE*scale)/2;draw()}

function resize(){let r=$('mapWrap').getBoundingClientRect();cw=canvas.width=Math.round(r.width);ch=canvas.height=Math.round(r.height);draw()}

function position(e){if(window.world3D?.active)return window.world3D.position(e);let r=canvas.getBoundingClientRect();return {u:Math.floor((e.clientX-r.left-panX)/scale),v:Math.floor((e.clientY-r.top-panY)/scale),x:e.clientX-r.left,y:e.clientY-r.top}}

function paint(u,v){if(tool==='paint'&&paintMode==='soft'){softPaint(u,v);return}if(tool==='height'){sculpt(u,v);return}let half=Math.floor(brush/2);for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){let current=tool==='paint'?editableFloorTile(x,y):tile(x,y);if(!current)continue;if(tool==='erase'&&eraseMode==='void'){clearLandTile(x,y);continue}let t=touch(x,y);if(tool==='paint'){
  let material=terrain[choice],overlay=material.overlay??material.id;
  if(overlay===0&&protectPaintedTextures&&current[2]!==0){
                                                                          
                                                                              
    t[1]=material.ground??78;
    continue;
  }
  t[2]=overlay;
  if(t[2]===0)t[1]=material.ground??78;
}else{if(['all','terrain'].includes(eraseMode)&&t[2]!==250){t[2]=0;if(floor!==1&&floor!==2)t[1]=78}if(['all','roof','wallsroof'].includes(eraseMode))t[3]=0;if(['all','walls','wallsroof'].includes(eraseMode)){t[4]=t[5]=t[6]=0}if(['all','placements'].includes(eraseMode))removeAt(x,y,x,y)}}draw()}

function addObject(id,u,v,dir=0){state.objects.push({id,pos:world(u,v),direction:dir})}

function addNpc(id,u,v){let p=world(u,v),r={id,start:p,min:copy(p),max:copy(p)};setNpcRadius(r,npcRadius);state.npcs.push(r)}

function build(a,b,c,d,style=buildStyle,h=floor){if(c-a<4||d-b<4)return false;removeAt(a-1,b,c,d,h);for(let u=a;u<c;u++)for(let v=b;v<d;v++){let t=touch(u,v,h);if(t){t[2]=style.floor;t[3]=style.roof;t[4]=t[5]=t[6]=0}}for(let u=a;u<c;u++){let n=touch(u,b,h),s=touch(u,d,h);if(n)n[5]=style.wall;if(s)s[5]=style.wall}for(let v=b;v<d;v++){let w=touch(a-1,v,h),e=touch(c-1,v,h);if(w)w[4]=style.wall;if(e)e[4]=style.wall}let door=entranceFor(style.door,h);let mx=Math.floor((a+c)/2),my=Math.floor((b+d)/2);if(['N','S'].includes(door)){for(let u=mx-1;u<=mx+1;u++){let t=touch(u,door==='N'?b:d,h);if(t)t[5]=0}}else if(['E','W'].includes(door))for(let v=my-1;v<=my+1;v++){let t=touch(door==='W'?a-1:c-1,v,h);if(t)t[4]=0}return true}

function makeBuiltin(p){let w=p.w,h=p.h,tiles=Array.from({length:w*h},()=>[110,78,0,0,0,0,0]),objects=[];let at=(u,v)=>tiles[v*w+u];if(['bridge','pier'].includes(p.type)){for(let v=0;v<h;v++)for(let u=1;u<w-1;u++)at(u,v)[2]=p.type==='pier'?3:5;for(let v=0;v<h;v++){at(0,v)[4]=11;at(w-2,v)[4]=11}}else{for(let v=1;v<h-2;v++)for(let u=2;u<w-2;u++){at(u,v)[2]=p.type==='house'?3:5;at(u,v)[3]=p.type==='tower'?3:1}for(let u=2;u<w-2;u++){at(u,1)[5]=p.type==='house'?15:1;at(u,h-2)[5]=p.type==='house'?15:1}for(let v=1;v<h-2;v++){at(1,v)[4]=p.type==='house'?15:1;at(w-3,v)[4]=p.type==='house'?15:1}let door=entranceFor(buildStyle.door,floor);if(['N','S'].includes(door)){for(let u=Math.floor(w/2)-1;u<=Math.floor(w/2)+1;u++)at(u,door==='N'?1:h-2)[5]=0}else if(['E','W'].includes(door)){for(let v=Math.floor(h/2)-1;v<=Math.floor(h/2)+1;v++)at(door==='W'?1:w-3,v)[4]=0}objects.push({id:p.type==='shop'?18:3,u:4,v:3,direction:0})}return {name:p.name,w,h,tiles,objects,npcs:[],boundaries:[],items:[],keepElevation:true}}

function rotatePrefab(p){let q={...copy(p),w:p.h,h:p.w,tiles:Array.from({length:p.w*p.h},()=>[110,78,0,0,0,0,0])};for(let v=0;v<p.h;v++)for(let u=0;u<p.w;u++){let t=p.tiles[v*p.w+u],x=p.h-1-v,y=u,out=q.tiles[y*q.w+x];out[0]=t[0];out[1]=t[1];out[2]=t[2];out[3]=t[3];out[4]=t[5];if(t[4]&&y+1<q.h)q.tiles[(y+1)*q.w+x][5]=t[4];out[6]=t[6]?t[6]>12000?t[6]-12000:t[6]+12000:0}for(let k of ['objects','npcs','boundaries','items'])q[k]=(p[k]||[]).map(r=>{let z={...r,u:p.h-1-r.v,v:r.u};if(k==='npcs'&&r.roam)z.roam={minX:r.roam.minY,maxX:r.roam.maxY,minY:-r.roam.maxX,maxY:-r.roam.minX};if(k==='objects'){let def=assets.objects[r.id],w=r.direction%4===0?def.width:def.height;z.v-=w-1;z.direction=(r.direction+2)%8}else if(k==='boundaries')z.direction=(r.direction+1)%4;return z});return q}

function stampData(){let p=clipboard&&tool==='stamp'&&choice===-1?clipboard:entries()[choice];if(!p)return null;p=p.type?makeBuiltin(p):p;for(let i=0;i<rotation/90;i++)p=rotatePrefab(p);return p}

function placeStamp(u,v){let p=stampData();if(!p)return;if(u<0||v<0||u+p.w>MAP_SIZE||v+p.h>MAP_SIZE){toast('Keep the whole stamp inside the editing area.');return}removeAt(u,v,u+p.w-1,v+p.h-1);let elevation=tile(u,v)[0];for(let y=0;y<p.h;y++)for(let x=0;x<p.w;x++){let t=touch(u+x,v+y),s=p.tiles[y*p.w+x];for(let i=0;i<7;i++)t[i]=i===0&&p.keepElevation?elevation:s[i]}for(let k of ['objects','npcs','boundaries','items'])for(let r of p[k]||[]){let p2=world(u+r.u,v+r.v);if(k==='npcs'){let placed={id:r.id,start:p2,min:copy(p2),max:copy(p2)};applyNpcOffsets(placed,r.roam||{minX:0,maxX:0,minY:0,maxY:0});state[k].push(placed)}else {let row=copy(r);delete row.u;delete row.v;row.pos=p2;state[k].push(row)}}}

function copySelection(){let a=area();if(!a){toast('Drag a selection first.');return}let p={name:'Copied selection',w:a.c-a.a+1,h:a.d-a.b+1,tiles:[]};for(let v=a.b;v<=a.d;v++)for(let u=a.a;u<=a.c;u++)p.tiles.push(copy(tile(u,v)));for(let k of ['objects','npcs','boundaries','items'])p[k]=state[k].filter(r=>inside(r,a.a,a.b,a.c,a.d)).map(r=>{let z=copy(r),l=local(r);if(k==='npcs')z.roam=npcOffsets(r);delete z.pos;delete z.start;delete z.min;delete z.max;return {...z,u:l.u-a.a,v:l.v-a.b}});clipboard=p;toast('Selection copied. Press Ctrl V to place it.');return p}

function pasteMode(){if(!clipboard){toast('Copy a selection first.');return}setTool('stamp');choice=-1;rotation=0;renderPalette();settings();draw();toast('Click to place your copied selection. R rotates it.')}

async function savePrefab(){let p=copySelection();if(!p)return;let name=prompt('Name this reusable structure:','My building');if(!name?.trim())return;p.name=name.trim().slice(0,60);customPrefabs.push(p);await busy('Saving your prefab…',()=>api('/api/prefabs',{prefabs:customPrefabs}));toast('Saved to your Stamp library.')}

function clearLandTile(u,v){
 const current=tile(u,v);if(!current)return;
 const empty=[0,0,250,0,0,0,0];
 if(current.some((n,i)=>n!==empty[i])){const t=touch(u,v);for(let i=0;i<7;i++)t[i]=empty[i]}
 removeAt(u,v,u,v);
}
function deleteLandSelection(){
 const a=area();if(!a){toast('Select a rectangle of land first.');return}
 begin();for(let v=a.b;v<=a.d;v++)for(let u=a.a;u<=a.c;u++)clearLandTile(u,v);commit();inspect();draw();
 status('Selected land removed on this floor · Ctrl Z to restore');
}
function deleteSelection(){let a=area();if(!a)return;begin();for(let v=a.b;v<=a.d;v++)for(let u=a.a;u<=a.c;u++){let t=touch(u,v);t[2]=t[3]=t[4]=t[5]=t[6]=0;t[1]=78}removeAt(a.a,a.b,a.c,a.d);commit()}

function inspect(){
 let a=area(),u=a?.a,v=a?.b;if(u===undefined){$('selectionInfo').textContent='Click a tile with Select & Inspect to see terrain, walls and placements.';return}
 let t=tile(u,v);if(!t)return;let p=world(u,v),el=$('selectionInfo');el.replaceChildren();
 let info=document.createElement('div');info.textContent=`${p.X}, ${p.Y} · ${tileMaterial(t)?.name||'Terrain'} · ${a.c-a.a+1} × ${a.d-a.b+1} tiles`;el.append(info);
 let elev=document.createElement('input');elev.type='number';elev.min=0;elev.max=255;elev.value=t[0];elev.title='Elevation';elev.style.width='80px';let b=document.createElement('button');b.textContent='Set height';b.onclick=()=>{let val=+elev.value;if(!Number.isInteger(val)||val<0||val>255)return;begin();for(let y=a.b;y<=a.d;y++)for(let x=a.a;x<=a.c;x++)touch(x,y)[0]=val;commit()};let row=document.createElement('div');row.className='row';row.append(elev,b);el.append(row);

 const surfaceButton=document.createElement('button');
 surfaceButton.className='wide';
 surfaceButton.textContent='Magic select connected surface';
 surfaceButton.onclick=()=>magicSelectSurface(u,v);
 el.append(surfaceButton);

 if(surfaceMagicSelection.size){
   const box=document.createElement('div');
   box.className='notice';
   box.textContent=`Surface selection: ${surfaceMagicSelection.size} connected tile${surfaceMagicSelection.size===1?'':'s'}.`;
   el.append(box);

   const amount=document.createElement('input');
   amount.type='number';amount.min=1;amount.max=255;amount.value=6;
   amount.style.width='72px';amount.title='Height change';

   const lower=document.createElement('button');
   lower.textContent='Lower selected';
   lower.onclick=()=>adjustSelectedSurfaceHeight(-Math.abs(+amount.value||0));

   const raise=document.createElement('button');
   raise.textContent='Raise selected';
   raise.onclick=()=>adjustSelectedSurfaceHeight(Math.abs(+amount.value||0));

   const clear=document.createElement('button');
   clear.textContent='Clear surface selection';
   clear.className='wide';
   clear.onclick=()=>{surfaceMagicSelection.clear();inspect();draw()};

   const sr=document.createElement('div');
   sr.className='row';
   sr.append(amount,lower,raise);
   el.append(sr,clear);
 }

 const walls=wallSegmentsAt(u,v);
 if(walls.length){let head=document.createElement('div');head.className='divider';el.append(head);let title=document.createElement('strong');title.textContent='Walls on this tile';el.append(title);for(const w of walls){let r=document.createElement('div');r.className='object-row';let d=wallDef(w.id),txt=document.createElement('span');txt.textContent=wallLabel(w.id)+(w.slot===4?' · East edge':w.slot===5?' · North edge':' · Diagonal');txt.title=d?.name||'';let magic=document.createElement('button');magic.textContent='Magic select';magic.onclick=()=>magicSelectWall(w);r.append(txt,magic);el.append(r)}}
                                                                    
 state.boundaries.forEach((r,i)=>{if(!inside(r,a.a,a.b,a.c,a.d))return;let id=r.id+1,d=wallDef(id);let row=document.createElement('div');row.className='object-row';let s=document.createElement('span');s.textContent=`Boundary: ${wallLabel(id)} · dir ${r.direction}`;s.title=d?.name||'';let remove=document.createElement('button');remove.textContent='Remove';remove.onclick=()=>{begin();state.boundaries.splice(i,1);commit();inspect()};row.append(s,remove);el.append(row)});
 if(wallMagicSelection.size){let box=document.createElement('div');box.className='notice';box.textContent=`Magic wall selection: ${wallMagicSelection.size} connected segment${wallMagicSelection.size===1?'':'s'}.`;el.append(box);let input=document.createElement('input');input.type='number';input.min=1;input.max=11999;input.placeholder='Wall ID';let first=selectedWallSegments()[0];if(first)input.value=first.id;let replace=document.createElement('button');replace.textContent='Replace selected wall';replace.onclick=()=>setSelectedWallType(input.value);let remove=document.createElement('button');remove.textContent='Remove selected walls';remove.onclick=removeSelectedWalls;let rr=document.createElement('div');rr.className='row';rr.append(input,replace);el.append(rr,remove)}
 for(let k of ['objects','npcs','items'])state[k].forEach((r,i)=>{if(!inside(r,a.a,a.b,a.c,a.d))return;let n=k==='objects'?assets.objects[r.id]?.name:k==='npcs'?(assets.npcs.find(n=>n.id===r.id)?.name||'Unknown NPC #'+r.id):(assets.items?.find(n=>n.id===r.id)?.name||'Ground item #'+r.id);let row=document.createElement('div');row.className='object-row';let s=document.createElement('span');s.textContent=n||k;let remove=document.createElement('button');remove.textContent='Remove';remove.onclick=()=>{begin();state[k].splice(i,1);commit();inspect()};if(k==='objects'){let model=document.createElement('canvas');model.width=96;model.height=96;model.className='inspect-model';paintModelCard(model,r.id,r.direction);el.append(model);let rotate=document.createElement('button');rotate.textContent='Rotate ↻ '+r.direction*45+'°';rotate.title='Turn this scenery by 45° (including diagonals)';rotate.onclick=()=>{begin();r.direction=(r.direction+1)%8;commit();inspect()};row.append(rotate);let edit=document.createElement('button');edit.textContent=assets.objects[r.id]?.custom?'Edit structure':'Copy & resize';edit.onclick=()=>window.objectCreator.open(r.id,!assets.objects[r.id]?.custom);row.append(edit)}row.append(s,remove);el.append(row);if(k==='npcs')npcInspector(el,r);if(k==='items')itemInspector(el,r)})
}


canvas.addEventListener('pointerdown',e=>{if(!state)return;canvas.focus();let p=position(e);if(e.button===2||e.button===1||space){drag={kind:'pan',x:p.x,y:p.y,px:panX,py:panY};canvas.setPointerCapture(e.pointerId);return}if(!tile(p.u,p.v))return;hover=p;lastTarget={u:p.u,v:p.v};draw();canvas.setPointerCapture(e.pointerId);if(tool==='height'&&heightMode==='pick'){heightTarget=tile(p.u,p.v)[0];heightMode='flatten';settings();toast('Picked height '+heightTarget+'. Drag to flatten the ground.');return}if(['paint','blend','height','erase','roof','opening'].includes(tool)){begin();drag={kind:'paint',u:p.u,v:p.v};paint(p.u,p.v)}else if(['select','build'].includes(tool)){selection={u:p.u,v:p.v,x:p.u,y:p.v};drag={kind:tool};draw()}else {begin();if(tool==='objects'){let o=entries()[choice],w=rotation%180?o.height:o.width,h=rotation%180?o.width:o.height;if(p.u-w+1<0||p.v+h>MAP_SIZE){toast('The scenery footprint must fit inside the editing area.')}else addObject(o.id,p.u,p.v,rotation/45)}else if(tool==='npcs'){if(assets.blocked.includes(tile(p.u,p.v)[2]))toast('Choose a walkable tile for this NPC.');else addNpc(entries()[choice].id,p.u,p.v)}else if(tool==='items')addItem(p.u,p.v);else if(tool==='stamp')placeStamp(p.u,p.v);commit()}});

canvas.addEventListener('pointermove',e=>{let p=position(e);hover=tile(p.u,p.v)?p:null;if(hover)lastTarget={u:p.u,v:p.v};if(hover){let w=world(p.u,p.v);$('coords').textContent=`World ${w.X}, ${w.Y} · ${tileMaterial(tile(p.u,p.v))?.name||'Terrain'} · Height ${tile(p.u,p.v)[0]}`}if(drag?.kind==='pan'){panX=drag.px+p.x-drag.x;panY=drag.py+p.y-drag.y}else if(drag?.kind==='paint'&&hover&&(p.u!==drag.u||p.v!==drag.v)){let n=Math.max(Math.abs(p.u-drag.u),Math.abs(p.v-drag.v),1);for(let i=1;i<=n;i++)paint(Math.round(drag.u+(p.u-drag.u)*i/n),Math.round(drag.v+(p.v-drag.v)*i/n));drag.u=p.u;drag.v=p.v}else if(drag&&hover&&['select','build'].includes(drag.kind)){selection.x=Math.max(0,Math.min(MAP_LAST,p.u));selection.y=Math.max(0,Math.min(MAP_LAST,p.v))}draw()});

canvas.addEventListener('pointerup',()=>{if(drag?.kind==='paint')commit();if(drag?.kind==='build'){let a=area();begin();if(!build(a.a,a.b,a.c,a.d))toast('Make the building at least 5 × 5 tiles.');commit()}drag=null;inspect();draw()});

canvas.addEventListener('pointercancel',()=>{commit();drag=null});canvas.addEventListener('contextmenu',e=>e.preventDefault());canvas.addEventListener('pointerleave',()=>{if(!drag){hover=null;draw()}});

function zoom(f,x=cw/2,y=ch/2){if(window.world3D?.active){window.world3D.zoom(f,x,y);return;}let next=Math.max(1,Math.min(32,scale*f));panX=x-(x-panX)*next/scale;panY=y-(y-panY)*next/scale;scale=next;draw()}

canvas.addEventListener('wheel',e=>{e.preventDefault();let p=position(e);zoom(e.deltaY<0?1.13:1/1.13,p.x,p.y)},{passive:false});

async function save(){let result=await api('/api/save',state);state.revision=result.revision;state.lastSaved=result.lastSaved;dirty=false;window.world3D?.refreshOverview();$('saveState').textContent=result.gameSyncError?'Draft saved · game sync failed':result.gameSyncPending?'Draft saved · game update queued':result.gameSynced?'Draft + game saved':'Draft saved';status(result.gameSyncError?'Draft saved, but game files were not updated: '+result.gameSyncError:result.gameSyncPending?'Saved to game folder · stop and start the game to apply':result.gameSynced?'Saved to game · restart the game to load changes':'Saved '+result.lastSaved);if(result.gameSyncError)toast('Your draft is safe. Game sync failed: '+result.gameSyncError);return result}

async function check(){let r=await api('/api/check',{});$('checks').textContent=r.warnings.length?r.warnings.join(' '):r.message;return r}

async function showPreview(){await busy('Rendering your draft in RSC…',async()=>{if(dirty)await save();let a=area(),u=a?Math.floor((a.a+a.c)/2):MAP_HALF,v=a?Math.floor((a.b+a.d)/2):MAP_HALF;let marker=window.previewMarker?.location(),p=marker||world(u,v,0);let r=await api('/api/preview',{x:p.X,y:p.Y,floor:marker?.floor??floor,angle:previewAngle,hideRoofs:$('hideRoofs').checked});$('previewImage').src=r.image;if(!$('previewDialog').open)$('previewDialog').showModal();status(r.message)})}

$('save').onclick=()=>busy('Saving your draft…',save);$('deploy').onclick=()=>busy('Backing up and updating the local game…',async()=>{if(dirty)await save();let r=await api('/api/deploy',{});state.lastDeployed=r.lastDeployed;toast(r.message);status(r.message)});$('check').onclick=()=>busy('Checking placements…',async()=>{if(dirty)await save();await check()});$('preview').onclick=showPreview;$('closePreview').onclick=()=>$('previewDialog').close();$('refreshPreview').onclick=showPreview;$('rotatePreview').onclick=()=>{previewAngle=(previewAngle+256)%1024;showPreview()};$('closeHelp').onclick=()=>$('helpDialog').close();$('undo').onclick=undo;$('redo').onclick=redo;$('search').oninput=()=>renderPalette();$('fit').onclick=fit;$('blocked').onchange=draw;$('showSlopes').onchange=draw;$('floor').onchange=()=>{floor=+$('floor').value;selection=null;wallMagicSelection.clear();surfaceMagicSelection.clear();settings();inspect();draw()};$('zoomIn').onclick=()=>zoom(1.25);$('zoomOut').onclick=()=>zoom(.8);

$('goto').onclick=()=>busy('Opening another area…',async()=>{if(dirty)await save();let x=+$('gotoX').value,y=+$('gotoY').value;if(!Number.isInteger(x)||!Number.isInteger(y)||x<worldMinX()||x>worldMaxX()||y<0||y>943)throw Error(`Use X ${worldMinX()} to ${worldMaxX()}, Y 0 to 943.`);state=await api(`/api/region?xmax=${Math.max(worldMinX()+MAP_LAST,Math.min(worldMaxX(),x+MAP_HALF))}&ymin=${Math.max(0,Math.min((944-MAP_SIZE),y-MAP_HALF))}`);customPrefabs=state.prefabs;selection=null;hover=null;lastTarget=null;$('areaName').textContent=`Around ${x}, ${y}`;updateUndo();fit();inspect()});

document.addEventListener('keydown',e=>{if(['INPUT','SELECT','TEXTAREA'].includes(document.activeElement.tagName)||document.querySelector('dialog[open]'))return;if(e.code==='Space'){space=true;e.preventDefault()}if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='s'){e.preventDefault();$('save').click()}if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='z'){e.preventDefault();e.shiftKey?redo():undo()}if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='y'){e.preventDefault();redo()}if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='c'){if(window.getSelection()?.toString())return;e.preventDefault();copySelection()}if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='v'){e.preventDefault();pasteMode()}if(e.key.toLowerCase()==='r'&&['stamp','objects'].includes(tool)){rotation=(rotation+(tool==='objects'?45:90))%360;settings();draw()}if(e.key==='Escape'){selection=null;surfaceMagicSelection.clear();hover=null;inspect();draw()}});document.addEventListener('keyup',e=>{if(e.code==='Space')space=false});window.addEventListener('blur',()=>space=false);window.addEventListener('beforeunload',e=>{if(dirty){e.preventDefault();e.returnValue=''}});

new ResizeObserver(resize).observe($('mapWrap'));



function modelImage(id,callback){let key=modelIndex[assets.objects[id]?.sourceId??id];if(key===undefined)return null;let entry=modelImages.get(key);if(!entry){let img=new Image();entry={img,ready:false,waiting:[]};modelImages.set(key,entry);img.onload=()=>{entry.ready=true;for(let fn of entry.waiting)fn();entry.waiting=[];draw()};img.onerror=()=>{entry.waiting=[]};img.src='/thumbnails/'+key+'.png'}if(callback&&!entry.ready)entry.waiting.push(callback);return entry.ready?entry.img:null}

function paintModelCard(c,id,direction){let g=c.getContext('2d');g.clearRect(0,0,c.width,c.height);let img=modelImage(id,()=>paintModelCard(c,id,direction));if(img){let size=Math.min(c.width,c.height);g.imageSmoothingEnabled=false;g.drawImage(img,(direction%8)*128,0,128,128,(c.width-size)/2,0,size,size)}else{g.fillStyle='#96aab4';g.font='11px Segoe UI';g.fillText('Loading model…',8,35)}}

function examineSettings(entry){if(!entry)return;let box=document.createElement('div');box.className='notice';let heading=document.createElement('strong');heading.textContent='Examine';let text=document.createElement('p');text.textContent=entry.description||'No examine text is defined for this asset.';text.style.whiteSpace='pre-wrap';text.style.margin='6px 0 0';box.append(heading,text);$('settings').append(box);}
function modelSettings(){examineSettings(entries()[choice]);let id=entries()[choice]?.id;if(id===undefined)return;let c=document.createElement('canvas');c.width=240;c.height=240;c.className='selected-model';c.setAttribute('aria-label','Selected scenery model at '+rotation+' degrees');paintModelCard(c,id,rotation/45);$('settings').append(c);let note=document.createElement('div');note.className='muted';note.textContent='This is a 3D Preview of the select RSC model. Press R or rotate to turn the selected object 45 degrees which also includes diagonals.';$('settings').append(note)}

function drawMapModel(id,direction,x,y,w,h,ghost){if(assets.objects[id]?.custom&&window.objectCreator?.drawMap(id,direction,x,y,w,h,ghost))return;let image=modelImage(id);ctx.save();if(ghost)ctx.globalAlpha=.85;let size=Math.max(w,h,ghost?40/scale:1);if(image){ctx.imageSmoothingEnabled=false;ctx.drawImage(image,(direction%8)*128,128,128,128,x+w/2-size/2,y+h/2-size/2,size,size)}else{ctx.fillStyle='#b49c78';ctx.fillRect(x+.1,y+.1,Math.max(.8,w-.2),Math.max(.8,h-.2))}if(ghost||scale>=7){let angle=direction*Math.PI/4,cx=x+w/2,cy=y+h/2,len=Math.max(w,h)/2+.5,dx=-Math.sin(angle),dy=Math.cos(angle);ctx.strokeStyle=ghost?'#fff3a5':'#ffe5a3';ctx.fillStyle=ctx.strokeStyle;ctx.lineWidth=(ghost?2:1.2)/scale;ctx.beginPath();ctx.moveTo(cx,cy);ctx.lineTo(cx+dx*len,cy+dy*len);ctx.stroke();let tx=cx+dx*len,ty=cy+dy*len,a=4/scale;ctx.beginPath();ctx.moveTo(tx,ty);ctx.lineTo(tx-dx*a+dy*a*.6,ty-dy*a-dx*a*.6);ctx.lineTo(tx-dx*a-dy*a*.6,ty-dy*a+dx*a*.6);ctx.closePath();ctx.fill()}ctx.restore()}

function heightSettings(){let modes=document.createElement('div');modes.className='height-modes';for(let [value,label] of [['raise','↑ Raise'],['lower','↓ Lower'],['smooth','≈ Smooth'],['flatten','▰ Flatten'],['pick','⌖ Pick height']]){let b=document.createElement('button');b.textContent=label;b.className=heightMode===value?'active':'';b.onclick=()=>{window.world3D?.editing();heightMode=value;settings();draw()};modes.append(b)}$('settings').append(modes);let strength=document.createElement('input');strength.type='range';strength.min=1;strength.max=24;strength.value=heightStep;let amount=document.createElement('span');amount.className='muted';amount.textContent=heightStep+' height units per dab';strength.oninput=()=>{heightStep=+strength.value;amount.textContent=heightStep+' height units per dab'};field('Strength',strength);$('settings').append(amount);let target=document.createElement('input');target.type='number';target.min=0;target.max=255;target.value=heightTarget;target.onchange=()=>{heightTarget=Math.max(0,Math.min(255,Math.round(+target.value)||0));target.value=heightTarget};field('Flatten to height',target);let tip=document.createElement('p');tip.className='notice';tip.textContent=heightMode==='pick'?'Click the ground to copy its height. The brush will switch to Flatten.':heightMode==='flatten'?'Drag to level the ground to the chosen height. Pick height can match an existing path or building site.':heightMode==='smooth'?'Drag across uneven ground to soften it. Repeat strokes for gentler slopes.':'Click for a small change; hold to keep sculpting. Increase brush size for broad hills, or reduce strength for fine adjustments. Show slopes keeps terrain shading visible with every tool.';$('settings').append(tip)}

function sculpt(u,v){if(heightMode==='pick'){let t=tile(u,v);if(t){heightTarget=t[0];heightMode='flatten';settings();toast('Picked height '+heightTarget+'. Drag to flatten the ground.')}return}let half=Math.floor(brush/2);for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){if(!tile(x,y)||Math.hypot(x-u,y-v)>brush/2)continue;let key=floor*MAP_CELLS+y*MAP_SIZE+x;if(transaction.tiles.has(key))continue;let before=tile(x,y)[0],value=before;if(heightMode==='raise')value+=heightStep;else if(heightMode==='lower')value-=heightStep;else if(heightMode==='flatten')value=heightTarget;else{let total=0,count=0;for(let dy=-1;dy<=1;dy++)for(let dx=-1;dx<=1;dx++){let n=tile(x+dx,y+dy);if(n){let saved=transaction.tiles.get(floor*MAP_CELLS+(y+dy)*MAP_SIZE+x+dx);total+=saved?saved[0]:n[0];count++}}let target=Math.round(total/count);value=before+Math.max(-heightStep,Math.min(heightStep,target-before))}touch(x,y)[0]=Math.max(0,Math.min(255,Math.round(value)))}draw()}




function tileMaterial(t){return t[2]===0&&t[1]>=128&&t[1]<192?materials.sand:materials[t[2]]}
function entranceFor(value,h){return value==='auto'?(h===0?'S':'none'):value}
function npcOffsets(r){return {minX:r.min.X-r.start.X,maxX:r.max.X-r.start.X,minY:r.min.Y-r.start.Y,maxY:r.max.Y-r.start.Y}}
function applyNpcOffsets(r,o){let p=r.start,base=Math.floor(p.Y/944)*944;r.min={X:Math.max(worldMinX(),p.X+o.minX),Y:Math.max(base,p.Y+o.minY)};r.max={X:Math.min(worldMaxX(),p.X+o.maxX),Y:Math.min(base+943,p.Y+o.maxY)}}
function setNpcRadius(r,radius){radius=Math.max(0,Math.min(12,Math.round(+radius)||0));applyNpcOffsets(r,{minX:-radius,maxX:radius,minY:-radius,maxY:radius})}
function npcInspector(el,r){let wrap=document.createElement('div');wrap.className='npc-roaming';let label=document.createElement('label');label.className='field';label.textContent='Wander radius (tiles; 0 = stay here)';let input=document.createElement('input');input.type='number';input.min=0;input.max=12;input.value=Math.max(...Object.values(npcOffsets(r)).map(Math.abs));label.append(input);let button=document.createElement('button');button.textContent='Apply wandering';button.className='wide';button.onclick=()=>{begin();setNpcRadius(r,input.value);commit();inspect()};wrap.append(label,button);el.append(wrap)}
function npcSettings(){let npc=entries()[choice];if(npc){let details=document.createElement('p');details.className='notice';details.textContent=npc.name+'\n'+(npc.description||'')+'\n'+(npc.aggressive?'Aggressive: may attack players on sight.':'Not normally aggressive.')+(npc.commands?.length?'\nActions: '+npc.commands.join(', '):'');details.style.whiteSpace='pre-line';$('settings').append(details)}let slider=document.createElement('input');slider.type='range';slider.min=0;slider.max=12;slider.value=npcRadius;let value=document.createElement('p');value.className='notice';let update=()=>{npcRadius=+slider.value;value.textContent=npcRadius?'Wanders up to '+npcRadius+' tiles from its starting spot. The dashed outline shows the roaming area.':'Stays at its starting spot until its game behaviour moves it.';draw()};slider.oninput=update;field('Wander radius',slider);$('settings').append(value);update();let repair=document.createElement('button');repair.className='wide';repair.textContent='Enable roaming for men & farm animals here';repair.onclick=()=>{begin();let count=0;for(let r of state.npcs){let def=assets.npcs.find(n=>n.id===r.id);let name=(def?.gameName||def?.name)?.toLowerCase();if(local(r).h===floor&&/^(man|woman|cow|chicken|sheep|ram)$/.test(name||'')&&Object.values(npcOffsets(r)).every(v=>v===0)){setNpcRadius(r,npcRadius||3);count++}}commit();toast(count+' stationary men / farm animals updated. Save & Test applies this to the game.')};$('settings').append(repair);let note=document.createElement('p');note.className='muted';note.textContent='For other existing NPCs, choose Select & Inspect, click their tile, and Apply wandering. Special scripted NPCs may still stay put.';$('settings').append(note)}

async function init(){await busy('Opening your map…',async()=>{assets=await api('/api/assets');token=assets.token;modelIndex=await api('/thumbnails/index.json');let materialCatalog=await api('/materials.json');wallDefs=materialCatalog.walls||[];state=await api('/api/region');customPrefabs=state.prefabs;$('projectName').textContent=state.name;$('areaName').textContent=`Around ${state.xmax-MAP_HALF}, ${state.ymin+MAP_HALF}`;$('saveState').textContent=state.lastSaved?'Draft saved':'';renderTools();renderPalette();settings();updateUndo();resize();fit();status('')})}


                                                                           
                                                                            
function terrainCornerDefs(){return window.buildTools?.tileDefs||[]}
function groundCornerInvisible(u,v,corner,h=floor){
 if(corner<0)return false;
 if(h===1||h===2)return true;
 if(h!==3)return false;
 const dx=(corner===1||corner===2)?1:-1,dy=corner<2?-1:1;
 return tile(u+dx,v,h)?.[2]===8&&tile(u,v+dy,h)?.[2]===8;
}
function groundCorner(u,v,defs,h=floor){
 const t=tile(u,v,h),d=defs[t?.[2]-1];if(!d||t[2]===250||d.tileType===4||d.tileType===5||(d.tileType===2&&!(t[6]>0&&t[6]<24000)))return -1;
 const kind=(x,y)=>{const n=tile(x,y,h);return !n||n[2]===0||n[2]===250?-1:defs[n[2]-1]?.tileType===2?1:0};
 const k=kind(u,v),left=kind(u-1,v)!==k,right=kind(u+1,v)!==k,north=kind(u,v-1)!==k,south=kind(u,v+1)!==k;
 if(right&&north)return 1;if(left&&south)return 3;if(left&&north)return 0;if(right&&south)return 2;return -1;
}
                                                                           
function groundRGB(id){let i=id%64,r,g,b=0;if(id<64){r=255-i*4;g=255-Math.floor(i*1.75);b=r}else if(id<128){r=i*3;g=144}else if(id<192){r=192-Math.floor(i*1.5);g=144-Math.floor(i*1.5)}else{r=96-Math.floor(i*1.5);g=48+Math.floor(i*1.5)}return [r,g,b].map(v=>Math.floor(v/8)*8)}
function groundColor(id){return 'rgb('+groundRGB(id).join(',')+')'}
function blendGround(a,b,amount){
  if(amount>=1)return b;
  if(amount<=0)return a;

  const band=id=>Math.floor(Math.max(0,Math.min(255,id))/64);

                                                       
                                                                     
                                                                       
  if(band(a)===band(b)){
    return Math.max(0,Math.min(255,Math.round(a+(b-a)*amount)));
  }

                                                                          
                                                                      
  const c=groundRGB(a),d=groundRGB(b),
        target=c.map((v,i)=>v+(d[i]-v)*amount),
        bands=[band(a),band(b)],
        candidates=[];

  for(const bn of bands){
    const start=bn*64,end=start+64;
    for(let i=start;i<end;i++)candidates.push(i);
  }

  let best=a,score=Infinity;
  for(const i of candidates){
    const rgb=groundRGB(i),
          distance=rgb.reduce((sum,v,k)=>sum+(v-target[k])**2,0);
    if(distance<score){score=distance;best=i}
  }
  return best;
}
function paintSettings(){
 const material=terrain[choice],overlay=material.overlay??material.id,colours=overlay===0;
 if(colours){
  const protect=document.createElement('input');
  protect.type='checkbox';protect.checked=protectPaintedTextures;
  protect.onchange=()=>{protectPaintedTextures=protect.checked;settings()};
  field('Protect existing paths / water / textures',protect);
  const protectionNote=document.createElement('p');protectionNote.className='muted';
  protectionNote.textContent=protectPaintedTextures?'Texture protection is ON. Your path and water textures are protected.':'Texture protection is OFF. Your ground painting will replace paths and water.';
  $('settings').append(protectionNote);
 }
 field('Brush edge',select([['solid','Solid: replace surface'],['soft','Soft: blend edges']],paintMode,v=>{paintMode=v;settings();draw()}));
 if(paintMode!=='soft')return;
 if(colours){const input=document.createElement('input');input.type='range';input.min=10;input.max=100;input.step=5;input.value=paintStrength;const value=document.createElement('span');value.className='muted';value.textContent=paintStrength+'% strength';input.oninput=()=>{paintStrength=+input.value;value.textContent=paintStrength+'% strength'};field('Blend strength',input);$('settings').append(value);}
 const note=document.createElement('p');note.className='notice';note.textContent=colours?'Use a wide brush for gradual ground-colour transitions. With texture protection enabled, paths, water and other overlays stay intact.':'Textures use a connected brush footprint with native diagonal corners where supported. RSC stores one texture per tile, so textures cannot fade transparently together. Heights stay unchanged.';$('settings').append(note);
}

function softPaint(u,v){
 if(!transaction)return;
 const material=terrain[choice],overlay=material.overlay??material.id,target=material.ground??78,radius=brush/2,half=Math.floor(brush/2);
 transaction.blendWeights??=new Map();
 for(let y=v-half;y<=v+half;y++)for(let x=u-half;x<=u+half;x++){
  const current=tile(x,y),distance=window.buildTools?.shape==='square'?Math.max(Math.abs(x-u),Math.abs(y-v)):Math.hypot(x-u,y-v);
  if(!current||distance>=radius||current[2]===250&&floor===0)continue;
  if(current[2]===250)editableFloorTile(x,y);
  const t=1-distance/radius,weight=(paintStrength/100)*t*t*(3-2*t),key=floor*MAP_CELLS+y*MAP_SIZE+x;
  if(weight<=(transaction.blendWeights.get(key)||0))continue;
  transaction.blendWeights.set(key,weight);
  const saved=transaction.tiles.get(key),before=saved&&saved[2]!==250?saved:copy(current),next=copy(before);
  if(overlay===0&&before[2]===0)next[1]=blendGround(before[1],target,weight);
  else if(overlay===0&&protectPaintedTextures&&before[2]!==0){
                                                                         
                                                                              
    next[1]=blendGround(before[1],target,weight);
    next[2]=before[2];
  }
                                                                             
                                                                              
  else if(t>=.25){next[2]=overlay;if(overlay===0)next[1]=target;}
  if(next[1]!==current[1]||next[2]!==current[2]){const out=touch(x,y);out[1]=next[1];out[2]=next[2];}
 }
 draw();
}

init();

let itemAmount=1,itemRespawn=30;
function itemNumber(label,value,min,max,changed,parent){
 const input=document.createElement('input');input.type='number';input.min=min;input.max=max;input.step=1;input.value=value;
 input.onchange=()=>{let n=Number(input.value);if(!Number.isInteger(n)||n<min||n>max){input.value=value;toast(label+' must be a whole number from '+min+' to '+max);return}value=n;changed(n)};
 if(parent){let l=document.createElement('label');l.className='field';l.textContent=label;l.append(input);parent.append(l)}else field(label,input);return input;
}
function itemSettings(){
 examineSettings(entries()[choice]);
 itemNumber('Quantity',itemAmount,1,2147483647,v=>itemAmount=v);
 itemNumber('Respawn seconds (0 = no respawn)',itemRespawn,0,86400,v=>itemRespawn=v);
 let p=document.createElement('p');p.className='notice';p.textContent="Your item will match a table's heigh when placed on a table.";$('settings').append(p);
}
function addItem(u,v){let item=entries()[choice];if(!item)return;if(tile(u,v)[2]===250){toast('Create land here before placing an item.');return}state.items.push({id:item.id,pos:world(u,v),amount:itemAmount,respawn:itemRespawn});$('showItems').checked=true;}
function itemInspector(parent,r){itemNumber('Quantity',r.amount,1,2147483647,v=>{begin();r.amount=v;commit()},parent);itemNumber('Respawn seconds (0 = no respawn)',r.respawn,0,86400,v=>{begin();r.respawn=v;commit()},parent);}


$('openDisclaimer').onclick=()=>$('disclaimerDialog').showModal();
$('closeDisclaimer').onclick=()=>$('disclaimerDialog').close();
