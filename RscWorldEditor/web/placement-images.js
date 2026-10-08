'use strict';
window.placementImages=(()=>{
 const images=new Map();
 function get(kind,id){
  const key=`${assets?.artVersion||''}/${kind}/${id}`;
  let entry=images.get(key);
  if(!entry){const image=new Image();entry={image,ready:false,failed:false};images.set(key,entry);image.onload=()=>{entry.ready=true;draw();};image.onerror=()=>{entry.failed=true;draw()};image.src=`/api/placement/${kind}/${id}.png?v=${encodeURIComponent(assets?.artVersion||'')}`;}
  return entry;
 }
 function draw2D(kind,r,p){
  if(!$(kind==='npcs'?'showPeople':'showItems').checked)return;
  let entry=get(kind,r.id),size=kind==='npcs'?2:1.25;
  if(entry.ready){ctx.imageSmoothingEnabled=false;let w=size*entry.image.naturalWidth/entry.image.naturalHeight;if(kind==='npcs'){const def=assets.npcs.find(n=>n.id===r.id);w*=((def?.imageWidth??145/128)/(145/128));size*=((def?.imageHeight??220/128)/(220/128));}ctx.drawImage(entry.image,p.u+.5-w/2,p.v+.5-size/2,w,size)}
  else{ctx.fillStyle=kind==='npcs'?'#efc778':'#ece2bd';ctx.fillRect(p.u+.25,p.v+.25,.5,.5)}
 }
 const previousSymbol=symbol;
 symbol=function(c,item,kind){if(!['npcs','items'].includes(kind))return previousSymbol(c,item,kind);let g=c.getContext('2d');g.clearRect(0,0,c.width,c.height);const entry=get(kind,item.id);const paint=()=>{g.clearRect(0,0,c.width,c.height);g.imageSmoothingEnabled=false;const h=kind==='items'?c.height*.65:c.height,w=h*entry.image.naturalWidth/entry.image.naturalHeight;g.drawImage(entry.image,(c.width-w)/2,(c.height-h)/2,w,h)};if(entry.ready)paint();else{previousSymbol(c,item,kind);entry.image.addEventListener('load',paint,{once:true})}};
 return {get,draw2D};
})();
