'use strict';
                                                                               
function terrainVertexHeight(u,v){
 const x=Math.max(0,Math.min(MAP_LAST,Math.floor(u))),y=Math.max(0,Math.min(MAP_LAST,Math.floor(v))),t=tile(x,y);
 if(t&&t[2]!==250)return t[0]*3/128;
 const neighbours=[[x-1,y],[x,y-1],[x-1,y-1]].map(([a,b])=>tile(a,b)).filter(t=>t&&t[2]!==250);
 return neighbours.length?neighbours.reduce((sum,t)=>sum+t[0],0)/neighbours.length*3/128:0;
}
function terrainBorderFaces(u,v,points){
 const result=[],edges=[[0,-1,0,1],[1,0,1,2],[0,1,2,3],[-1,0,3,0]];
 for(const [du,dv,a,b]of edges){
  const x=u+du,y=v+dv,n=tile(x,y),wx=state.xmax-x,wy=state.ymin+y;
                                                                       
  if(n?n[2]!==250:wx>=worldMinX()&&wx<=worldMaxX()&&wy>=0&&wy<=943)continue;
  const first=points[a],last=points[b];
  if(first[1]<=0&&last[1]<=0)continue;
  result.push([first,last,[last[0],0,last[2]],[first[0],0,first[2]]]);
 }
 return result;
}
                                                               
                                                               
function roofFaceIndices(u,v,h){
 const diagonal=tile(u,v,h)?.[6]||0;
 const has=(du,dv)=>!!tile(u+du,v+dv,h)?.[3];
 if(diagonal>12000&&diagonal<24000){
  if(!has(1,-1))return [[3,2,0]];
  if(!has(-1,1))return [[1,0,2]];
 }else if(diagonal>0&&diagonal<12000){
  if(!has(-1,-1))return [[2,1,3]];
  if(!has(1,1))return [[0,3,1]];
 }
                                                                         
 return has(1,-1)||has(-1,1)?[[0,3,1],[2,1,3]]:[[1,0,2],[3,2,0]];
}
                                                                 
                                                                             
function upperFloorFaceIndices(u,v,h,defs){
 const corner=groundCorner(u,v,defs,h);
 return corner<0?[[0,1,2],[0,2,3]]:[[(corner+3)%4,(corner+1)%4,(corner+2)%4]];
}
                                                                              
window.world3D = (() => {
  const surface=$('scene3d'), gl=surface.getContext('webgl',{
    antialias:false,
    alpha:false,
    depth:true,
    stencil:false,
    preserveDrawingBuffer:false,
    powerPreference:'default'
});
  const camera={u:MAP_HALF,v:MAP_HALF,y:2.6,yaw:-.55,pitch:.85,scale:8};
  let graphicsLost=false,resume3D=!!gl,resourceGeneration=0,pickSized=false,pickKey=null,meshVersion=0;const attributes=[];
  let active=!!gl, hand=true, navigation=null, loading=false, invalid=true, meshFloor=-1;
  let library={mapping:{},models:{}}, catalog={tiles:[],walls:[],textures:[]};
  let buffer,count=0,groundCount=0,program,pickBuffer,pickTexture,pickDepth,atlasTexture;
  let meshBarriers,meshUpper,meshRoof,meshScenery,meshBlocked,meshSlopes,ready=false;
  let overviewImage=new Image(); overviewImage.src='/api/overview.png';overviewImage.onload=()=>overview();
  const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
  const ground=terrainVertexHeight;
  function rgb(c){if(c<0){let n=-1-c;return [(n>>10&31)/31,(n>>5&31)/31,(n&31)/31]}return [.8,.8,.8]}
  function project(u,v,y=ground(u,v)){
    let dx=u-camera.u,dz=v-camera.v,dy=y-camera.y,c=Math.cos(camera.yaw),s=Math.sin(camera.yaw),z=-dx*s+dz*c;
    return {x:cw/2+(dx*c+dz*s)*camera.scale,y:ch/2+(z*Math.sin(camera.pitch)-dy*Math.cos(camera.pitch))*camera.scale};
  }
  function plane(x,y,height=camera.y){let rx=(x-cw/2)/camera.scale,rz=((y-ch/2)/camera.scale+(height-camera.y)*Math.cos(camera.pitch))/Math.sin(camera.pitch),c=Math.cos(camera.yaw),s=Math.sin(camera.yaw);return {u:camera.u+rx*c-rz*s,v:camera.v+rx*s+rz*c}}
  function shader(type,source){let s=gl.createShader(type);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(s));return s}
  function initialize(){
    const generation=++resourceGeneration;
    program=gl.createProgram();
    gl.attachShader(program,shader(gl.VERTEX_SHADER,`attribute vec3 position;attribute vec3 colour;attribute vec3 ident;attribute vec3 texcoord;uniform vec3 centre;uniform vec4 view;uniform vec2 size;varying vec3 color;varying vec3 pick;varying vec3 uv;
    void main(){vec3 p=position-centre;float c=cos(view.x),s=sin(view.x);float x=p.x*c+p.z*s;float z=-p.x*s+p.z*c;float y=z*sin(view.y)-p.y*cos(view.y);float depth=-z*cos(view.y)-p.y*sin(view.y);gl_Position=vec4(x*view.z*2.0/size.x,-y*view.z*2.0/size.y,depth/1000.0,1.0);color=colour;pick=ident;uv=texcoord;}`));
    gl.attachShader(program,shader(gl.FRAGMENT_SHADER,`precision mediump float;varying vec3 color;varying vec3 pick;varying vec3 uv;uniform bool picking;uniform bool spriteMode;uniform sampler2D atlas;
    void main(){if(picking){gl_FragColor=vec4(pick,1.0);return;}if(uv.z < -1.5)discard;if(spriteMode){vec4 t=texture2D(atlas,uv.xy);if(t.a<.4)discard;gl_FragColor=t;return;}vec4 c=vec4(color,1.0);if(uv.z>=0.0){float id=floor(uv.z+0.5);vec2 tile=vec2(mod(id,8.0),floor(id/8.0));vec2 coord=(tile+(vec2(.5)+fract(uv.xy)*127.0)/128.0)/8.0;vec4 t=texture2D(atlas,coord);if(t.a<.4)discard;c*=t;}gl_FragColor=c;}`));
    gl.linkProgram(program);if(!gl.getProgramParameter(program,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(program));
    gl.useProgram(program);buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);
    ['position','colour','ident','texcoord'].forEach((name,i)=>{let a=gl.getAttribLocation(program,name);attributes[i]=a;gl.enableVertexAttribArray(a);gl.vertexAttribPointer(a,3,gl.FLOAT,false,48,i*12)});
    atlasTexture=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,atlasTexture);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,1,1,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([200,200,200,255]));
    let image=new Image();image.onload=()=>{if(generation!==resourceGeneration||graphicsLost||gl.isContextLost())return;gl.bindTexture(gl.TEXTURE_2D,atlasTexture);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,image);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);draw()};image.src='/textures.png';
    pickBuffer=gl.createFramebuffer();pickTexture=gl.createTexture();pickDepth=gl.createRenderbuffer();gl.enable(gl.DEPTH_TEST);ready=true;
  }
                                                                                   
  const chunks=[];
  function owns(c,u,v){return (u>=c.u0||(c.u0===0&&u<0))&&(u<c.u1||(c.u1===MAP_SIZE&&u>=MAP_SIZE))&&(v>=c.v0||(c.v0===0&&v<0))&&(v<c.v1||(c.v1===MAP_SIZE&&v>=MAP_SIZE))}
  function dirtyPoint(u,v){for(const c of chunks)if(owns(c,u,v))c.dirty=true}
  function invalidateTile(u,v,h){
                                                                               
    for(const c of chunks)if(u>=c.u0-2&&u<c.u1+2&&v>=c.v0-2&&v<c.v1+2)c.dirty=true;
                                                                                 
    for(const r of state.objects){const p=footprint(r),cx=p.u-p.w/2+1,cz=p.v+p.depth/2;
      if(Math.abs(Math.floor(cx)-u)<=1&&Math.abs(Math.floor(cz)-v)<=1)dirtyPoint(cx,cz);
    }
  }
  function buildMesh(){
    if(!chunks.length)for(let v=0;v<MAP_SIZE;v+=48)for(let u=0;u<MAP_SIZE;u+=48)chunks.push({u0:u,v0:v,u1:Math.min(u+48,MAP_SIZE),v1:Math.min(v+48,MAP_SIZE),buffer:gl.createBuffer(),dirty:true,count:0,groundCount:0});
    const full=invalid||meshFloor!==floor||meshBarriers!==$('hideBarriers').checked||meshUpper!==$('upperLevels3d').checked||meshRoof!==$('roofs3d').checked||meshScenery!==$('scenery3d').checked||meshBlocked!==$('blocked').checked||meshSlopes!==$('showSlopes').checked;
    for(const c of chunks)if(full||c.dirty)buildChunk(c);
  }
  function visibleChunk(c){
    if(!c.count)return false;
    const b=c.bounds;let left=Infinity,top=Infinity,right=-Infinity,bottom=-Infinity;
    for(const u of [b[0],b[3]])for(const y of [b[1],b[4]])for(const v of [b[2],b[5]]){const p=project(u,v,y);left=Math.min(left,p.x);right=Math.max(right,p.x);top=Math.min(top,p.y);bottom=Math.max(bottom,p.y)}
    return right>=-2&&left<=cw+2&&bottom>=-2&&top<=ch+2;
  }
  function drawChunks(picking=false){for(const c of chunks){if(!visibleChunk(c))continue;bindVertices(c.buffer);gl.drawArrays(gl.TRIANGLES,0,picking?c.groundCount:c.count)}}
  function buildChunk(chunk){
    const {u0,v0,u1,v1}=chunk;buffer=chunk.buffer;
    let data=[],borders=[];chunk.bounds=[Infinity,Infinity,Infinity,-Infinity,-Infinity,-Infinity];meshVersion++;pickKey=null;
    function face(points,color,tex=-1,id=0,uv=null){
      if(points.length<3)return;
      let a=points[0],b=points[1],c=points[2],ab=b.map((v,i)=>v-a[i]),ac=c.map((v,i)=>v-a[i]);
      let normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],len=Math.hypot(...normal)||1;
      let shade=$('showSlopes').checked?.62+.38*Math.abs((normal[0]*-.35+normal[1]*.85+normal[2]*.4)/len):1;
      const emit=i=>{let p=points[i],tc=uv?.[i]||[p[0],p[2]];for(let j=0;j<3;j++){chunk.bounds[j]=Math.min(chunk.bounds[j],p[j]);chunk.bounds[j+3]=Math.max(chunk.bounds[j+3],p[j])}data.push(...p,...color.map(v=>v*shade),(id&255)/255,(id>>8&255)/255,(id>>16&255)/255,...tc,tex)};
      for(let i=1;i<points.length-1;i++){emit(0);emit(i);emit(i+1)}
    }
                                                                                       
    function wall(u,v,uu,vv,id){if(hiddenBarrierWall(id))return;let d=catalog.walls[id-1];if(!d)return;let h=d.height/128,y=ground(u-1,v),yy=ground(uu-1,vv);face([[u,y,v],[uu,yy,vv],[uu,yy+h,vv],[u,y+h,v]],rgb(d.texture),d.texture>=0&&d.texture<60?d.texture:-1,0,[[0,0],[0,.999],[.999,.999],[.999,0]])}
    for(let v=v0;v<v1;v++)for(let u=u0;u<u1;u++){
      let t=tile(u,v);if(t[2]===250)continue;
      let d=catalog.tiles[t[2]-1],col=t[2]&&d?rgb(d.texture):groundRGB(t[1]).map(v=>v/255),tex=t[2]&&d?.texture>=0&&d.texture<60?d.texture:-1;
      if($('blocked').checked&&!hiddenBarrierTile(t[2])&&assets.blocked.includes(t[2])){col=[.85,.25,.25];tex=-1}
      if(((floor===1||floor===2)&&t[2]===0)||t[2]===8)tex=-2;
      if(hiddenBarrierTile(t[2])&&floor!==1&&floor!==2){col=groundRGB(t[1]).map(v=>v/255);tex=-1;}
                                                                   
                                                            
                                                               
                                                                        
                                                                      
                                                  
      const points=[
        [u,ground(u-1,v),v],
        [u+1,ground(u,v),v],
        [u+1,ground(u,v+1),v+1],
        [u,ground(u-1,v+1),v+1]
      ],id=v*MAP_SIZE+u+1;
      for(const face of ((floor===1||floor===2)?[]:terrainBorderFaces(u,v,points)))borders.push([face,groundRGB(t[1]).map(c=>c/255*.8)]);
      const corner=((floor!==1&&floor!==2)&&($('blocked').checked||hiddenBarrierTile(t[2])))?-1:groundCorner(u,v,catalog.tiles);
      if(corner<0){face([points[0],points[1],points[2]],col,tex,id);face([points[0],points[2],points[3]],col,tex,id)}
      else{const prev=(corner+3)%4,next=(corner+1)%4,opposite=(corner+2)%4;face([points[prev],points[corner],points[next]],groundRGB(t[1]).map(v=>v/255),groundCornerInvisible(u,v,corner)?-2:-1,id);face([points[prev],points[next],points[opposite]],col,tex,id)}
    }
    groundCount=data.length/12;
    for(const [points,colour]of borders)face(points,colour);
    for(let v=v0;v<v1;v++)for(let u=u0;u<u1;u++){
      let t=tile(u,v);if(t[4])wall(u+1,v,u+1,v+1,t[4]);if(t[5])wall(u,v,u+1,v,t[5]);
      if(t[6]>0&&t[6]<24000)wall(...diagonalWall(u,v,t[6]));
      if(t[3]&&$('roofs3d').checked){let roofs=[[64,6],[64,3],[96,2],[80,33],[80,15],[90,49]],roof=roofs[t[3]-1]||roofs[0];let roofHeight=(x,z)=>ground(x,z)+1.5+([[x-1,z-1],[x,z-1],[x-1,z],[x,z]].every(([a,b])=>tile(a,b)?.[3])?roof[0]/128:0);const points=[[u,roofHeight(u,v),v],[u+1,roofHeight(u+1,v),v],[u+1,roofHeight(u+1,v+1),v+1],[u,roofHeight(u,v+1),v+1]];for(const indices of roofFaceIndices(u,v,floor))face(indices.map(i=>points[i]),[.95,.95,.95],roof[1])}
    }
    for(let r of state.boundaries){let p=local(r);if(p.h!==floor||!owns(chunk,p.u,p.v))continue;let d=r.direction;if(d===0)wall(p.u,p.v,p.u+1,p.v,r.id+1);else if(d===1)wall(p.u+1,p.v,p.u+1,p.v+1,r.id+1);else if(d===2)wall(p.u,p.v,p.u+1,p.v+1,r.id+1);else if(d===3)wall(p.u+1,p.v,p.u,p.v+1,r.id+1)}
    if($('scenery3d').checked)for(let r of state.objects){
      let p=footprint(r);if(p.h!==floor)continue;let definition=assets.objects[r.id],model=library.models[library.mapping[definition?.sourceId??r.id]];if(!model)continue;
      let angle=r.direction*Math.PI/4,c=Math.cos(angle),s=Math.sin(angle),cx=p.u-p.w/2+1,cz=p.v+p.depth/2,base=ground(cx,cz);if(!owns(chunk,cx,cz))continue;
      let size=definition?.scale||[100,100,100];let vertices=model.v.map(v=>v.map((value,i)=>Math.round(value*size[i]/100))).map(([x,y,z])=>[cx-(x*c+z*s)/128,base-y/128,cz+(z*c-x*s)/128]);
      for(let [color,indices] of model.f){let pts=indices.map(i=>vertices[i]);let tex=color>=0&&color<60?color:-1;
        let minX=Math.min(...pts.map(p=>p[0])),maxX=Math.max(...pts.map(p=>p[0])),minZ=Math.min(...pts.map(p=>p[2])),maxZ=Math.max(...pts.map(p=>p[2]));
        let axis=maxX-minX>=maxZ-minZ?0:2,lo=Math.min(...pts.map(p=>p[axis])),hi=Math.max(...pts.map(p=>p[axis])),low=Math.min(...pts.map(p=>p[1])),high=Math.max(...pts.map(p=>p[1]));
        face(pts,rgb(color),tex,0,pts.map(p=>[(p[axis]-lo)/(hi-lo||1)*.999,(high-p[1])/(high-low||1)*.999]));
      }
    }
                                                                               
                                                                      
    if($('upperLevels3d').checked && floor<2){
      const height=(u,v,h)=>{
        const x=Math.max(0,Math.min(MAP_LAST,Math.floor(u))),z=Math.max(0,Math.min(MAP_LAST,Math.floor(v)));
        let y=ground(u,v);
        for(let level=floor;level<h;level++){
          let rise=1.5;
          for(const [a,b] of [[x,z],[x-1,z],[x,z-1],[x-1,z-1]]){
            const t=tile(a,b,level);if(!t)continue;
            for(const id of [t[4],t[5],t[6]>12000?t[6]-12000:t[6]]){
              const wall=catalog.walls[id-1];if(wall)rise=Math.max(rise,wall.height/128);
            }
          }
          y+=rise;
        }
        return y;
      };
      for(let h=floor+1;h<=2;h++){
        const upperWall=(u,v,uu,vv,id)=>{
          const d=catalog.walls[id-1];if(!d||hiddenBarrierWall(id))return;
          const y=height(u,v,h),yy=height(uu,vv,h),rise=d.height/128;
          face([[u,y,v],[uu,yy,vv],[uu,yy+rise,vv],[u,y+rise,v]],rgb(d.texture),d.texture>=0&&d.texture<60?d.texture:-1,0,[[0,0],[0,.999],[.999,.999],[.999,0]]);
        };
        for(let v=v0;v<v1;v++)for(let u=u0;u<u1;u++){
          const t=tile(u,v,h);if(!t||t[2]===250)continue;
          const d=catalog.tiles[t[2]-1];
          if(t[2]!==0&&t[2]!==8&&d){
            const points=[[u,height(u,v,h),v],[u+1,height(u+1,v,h),v],[u+1,height(u+1,v+1,h),v+1],[u,height(u,v+1,h),v+1]];
            for(const indices of upperFloorFaceIndices(u,v,h,catalog.tiles))face(indices.map(i=>points[i]),rgb(d.texture),d.texture>=0&&d.texture<60?d.texture:-1);
          }
          if(t[4])upperWall(u+1,v,u+1,v+1,t[4]);
          if(t[5])upperWall(u,v,u+1,v,t[5]);
          if(t[6]>0&&t[6]<24000)upperWall(...diagonalWall(u,v,t[6]));
          if(t[3]&&$('roofs3d').checked){
            const roof=[[64,6],[64,3],[96,2],[80,33],[80,15],[90,49]][t[3]-1]||[64,6];
            const top=(x,z)=>height(x,z,h+1)+([[x-1,z-1],[x,z-1],[x-1,z],[x,z]].every(([a,b])=>tile(a,b,h)?.[3])?roof[0]/128:0);
            const points=[[u,top(u,v),v],[u+1,top(u+1,v),v],[u+1,top(u+1,v+1),v+1],[u,top(u,v+1),v+1]];for(const indices of roofFaceIndices(u,v,h))face(indices.map(i=>points[i]),[.95,.95,.95],roof[1]);
          }
        }
        for(const r of state.boundaries){const p=local(r);if(p.h!==h||!owns(chunk,p.u,p.v))continue;
          if(r.direction===0)upperWall(p.u,p.v,p.u+1,p.v,r.id+1);
          else if(r.direction===1)upperWall(p.u+1,p.v,p.u+1,p.v+1,r.id+1);
          else if(r.direction===2)upperWall(p.u,p.v,p.u+1,p.v+1,r.id+1);
          else if(r.direction===3)upperWall(p.u+1,p.v,p.u,p.v+1,r.id+1);
        }
      }
    }
    gl.bindBuffer(gl.ARRAY_BUFFER,buffer);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(data),gl.DYNAMIC_DRAW);count=data.length/12;chunk.count=count;chunk.groundCount=groundCount;chunk.dirty=false;invalid=false;meshFloor=floor;meshBarriers=$('hideBarriers').checked;meshUpper=$('upperLevels3d').checked;meshRoof=$('roofs3d').checked;meshScenery=$('scenery3d').checked;meshBlocked=$('blocked').checked;meshSlopes=$('showSlopes').checked;
  }
  let spriteBuffer;
  let spriteTextures=new WeakMap();
  function bindVertices(target){gl.bindBuffer(gl.ARRAY_BUFFER,target);['position','colour','ident','texcoord'].forEach((name,i)=>gl.vertexAttribPointer(attributes[i],3,gl.FLOAT,false,48,i*12))}
  function renderPlacements(){
    if(!window.placementImages)return;
    if(!spriteBuffer)spriteBuffer=gl.createBuffer();
    bindVertices(spriteBuffer);gl.uniform1i(gl.getUniformLocation(program,'spriteMode'),1);
    const c=Math.cos(camera.yaw),s=Math.sin(camera.yaw),sp=Math.sin(camera.pitch),cp=Math.cos(camera.pitch);
    for(const kind of ['items','npcs']){
      if(!$(kind==='npcs'?'showPeople':'showItems').checked)continue;
      for(const r of state[kind]){
        const p=local(r);if(p.h!==floor||p.u<0||p.u>=MAP_SIZE||p.v<0||p.v>=MAP_SIZE)continue;
        const u=p.u+.5,v=p.v+.5,y=ground(u,v)+.015,screen=project(u,v,y);
        if(screen.x< -150||screen.x>cw+150||screen.y< -150||screen.y>ch+150)continue;
        const entry=window.placementImages.get(kind,r.id);
        if(!entry.ready)continue;
        let texture=spriteTextures.get(entry.image);
        if(!texture){texture=gl.createTexture();spriteTextures.set(entry.image,texture);gl.bindTexture(gl.TEXTURE_2D,texture);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,entry.image);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE)}else gl.bindTexture(gl.TEXTURE_2D,texture);
        const def=kind==='npcs'?assets.npcs.find(n=>n.id===r.id):null;
        const w=kind==='npcs'?(def?.imageWidth||1.13):.85,h=kind==='npcs'?(def?.imageHeight||1.72):.57;
        const points=[[-w/2,0,0,1],[w/2,0,1,1],[w/2,h,1,0],[-w/2,h,0,0]],vertices=[];
        for(const i of [0,1,2,0,2,3]){const [dx,dy,tx,ty]=points[i];vertices.push(u+dx*c+dy*s*sp,y+dy*cp,v+dx*s-dy*c*sp,1,1,1,0,0,0,tx,ty,-1)}
        gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(vertices),gl.DYNAMIC_DRAW);gl.drawArrays(gl.TRIANGLES,0,6);
      }
    }
    gl.uniform1i(gl.getUniformLocation(program,'spriteMode'),0);gl.bindTexture(gl.TEXTURE_2D,atlasTexture);bindVertices(buffer);
  }
  let lastMeshBuild=0;
  function prepare(){
    if(!pickSized||surface.width!==cw||surface.height!==ch){pickSized=true;surface.width=cw;surface.height=ch;gl.bindTexture(gl.TEXTURE_2D,pickTexture);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,cw,ch,0,gl.RGBA,gl.UNSIGNED_BYTE,null);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.bindRenderbuffer(gl.RENDERBUFFER,pickDepth);gl.renderbufferStorage(gl.RENDERBUFFER,gl.DEPTH_COMPONENT16,cw,ch);gl.bindFramebuffer(gl.FRAMEBUFFER,pickBuffer);gl.framebufferTexture2D(gl.FRAMEBUFFER,gl.COLOR_ATTACHMENT0,gl.TEXTURE_2D,pickTexture,0);gl.framebufferRenderbuffer(gl.FRAMEBUFFER,gl.DEPTH_ATTACHMENT,gl.RENDERBUFFER,pickDepth);gl.bindFramebuffer(gl.FRAMEBUFFER,null)}
    const needsMesh=chunks.some(c=>c.dirty)||invalid||meshBarriers!==$('hideBarriers').checked||meshUpper!==$('upperLevels3d').checked||meshFloor!==floor||meshRoof!==$('roofs3d').checked||meshScenery!==$('scenery3d').checked||meshBlocked!==$('blocked').checked||meshSlopes!==$('showSlopes').checked;
    if(needsMesh){
      const now=performance.now();
      if(!drag||now-lastMeshBuild>=100){
        buildMesh();
        lastMeshBuild=now;
      }
    }
    gl.useProgram(program);gl.viewport(0,0,cw,ch);gl.uniform3f(gl.getUniformLocation(program,'centre'),camera.u,camera.y,camera.v);gl.uniform4f(gl.getUniformLocation(program,'view'),camera.yaw,camera.pitch,camera.scale,0);gl.uniform2f(gl.getUniformLocation(program,'size'),cw,ch);gl.bindTexture(gl.TEXTURE_2D,atlasTexture);
  }
  let lastPickRead=0,lastPickResult={u:-1,v:-1,x:0,y:0},lastPickMeshKey='';
  function floorEditingPick(hit){
    if(hit.u>=0||floor===0||!['paint','walls','roof','build','opening'].includes(tool))return hit;
    const p=plane(hit.x,hit.y,0),u=Math.floor(p.u),v=Math.floor(p.v);
    return tile(u,v)?.[2]===250?{u,v,x:hit.x,y:hit.y}:hit;
  }
  function position(e){
    let r=canvas.getBoundingClientRect(),x=e.clientX-r.left,y=e.clientY-r.top;
    if(!ready||graphicsLost||!state||loading||x<0||x>=cw||y<0||y>=ch)return {u:-1,v:-1,x,y};
    prepare();
    const key=[meshVersion,cw,ch,camera.u,camera.v,camera.y,camera.yaw,camera.pitch,camera.scale].join(',');
    gl.bindFramebuffer(gl.FRAMEBUFFER,pickBuffer);
                                                                                 
    if(pickKey!==key){
      gl.disable(gl.DITHER);gl.clearColor(0,0,0,1);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
      gl.uniform1i(gl.getUniformLocation(program,'picking'),1);drawChunks(true);
      gl.enable(gl.DITHER);pickKey=key;
    }

                                                                                      
    const now=performance.now();
    if(key===lastPickMeshKey && now-lastPickRead<50){
      gl.bindFramebuffer(gl.FRAMEBUFFER,null);
      return floorEditingPick({u:lastPickResult.u,v:lastPickResult.v,x,y});
    }

    let pixel=new Uint8Array(4);
    gl.readPixels(Math.floor(x),ch-1-Math.floor(y),1,1,gl.RGBA,gl.UNSIGNED_BYTE,pixel);
    gl.bindFramebuffer(gl.FRAMEBUFFER,null);
    let id=pixel[0]+pixel[1]*256+pixel[2]*65536-1;
    lastPickRead=now;
    lastPickMeshKey=key;
    lastPickResult={u:id<0?-1:id%MAP_SIZE,v:id<0?-1:Math.floor(id/MAP_SIZE),x,y};
    return floorEditingPick(lastPickResult);
  }
  function outline(a,b,c,d,color='#ffe3a0',fill=true){ctx.beginPath();for(let [u,v] of [[a,b],[c,b],[c,d],[a,d]]){let p=project(u,v);ctx.lineTo(p.x,p.y)}ctx.closePath();if(fill){ctx.fillStyle='#ffe6ad22';ctx.fill()}ctx.strokeStyle=color;ctx.lineWidth=2;ctx.stroke()}
  function render(){if(!ready||graphicsLost)return;prepare();gl.bindFramebuffer(gl.FRAMEBUFFER,null);if(floor===1||floor===2)gl.clearColor(0,0,0,1);else gl.clearColor(.105,.175,.20,1);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);gl.uniform1i(gl.getUniformLocation(program,'picking'),0);drawChunks();renderPlacements();ctx.setTransform(1,0,0,1,0,0);ctx.clearRect(0,0,cw,ch);let a=area();if(a)outline(a.a,a.b,a.c+1,a.d+1);if(hover&&!navigation){let {u,v}=hover;if(['paint','blend','height','erase','roof','opening'].includes(tool)){let half=Math.floor(brush/2);if(window.buildTools?.shape==='round'){ctx.beginPath();for(let i=0;i<=40;i++){let a=i*Math.PI/20,p=project(u+.5+Math.cos(a)*brush/2,v+.5+Math.sin(a)*brush/2);ctx.lineTo(p.x,p.y)}ctx.strokeStyle='#ffe3a0';ctx.lineWidth=2;ctx.fillStyle='#ffe6ad22';ctx.fill();ctx.stroke()}else outline(u-half,v-half,u+half+1,v+half+1)}else if(tool==='objects'){let o=entries()[choice];if(o){let w=rotation%180?o.height:o.width,h=rotation%180?o.width:o.height;outline(u-w+1,v,u+1,v+h)}}else outline(u,v,u+1,v+1)}window.buildTools?.overlay();updateHeightReadout();$('zoomValue').textContent=Math.round(camera.scale/8*100)+'%';overview();}
  function fit(){camera.u=MAP_HALF;camera.v=MAP_HALF;camera.y=ground(MAP_HALF,MAP_HALF);camera.scale=Math.max(.5,Math.min((cw-50)/(MAP_SIZE*1.29),(ch-70)/(MAP_SIZE*1.01)));draw()}
  function focus(){let a=area();camera.u=a?(a.a+a.c+1)/2:MAP_HALF;camera.v=a?(a.b+a.d+1)/2:MAP_HALF;camera.y=ground(camera.u,camera.v);camera.scale=a?clamp(Math.min(cw/(a.c-a.a+6),ch/(a.d-a.b+6)),5,40):18;draw()}
  function zoom(f,x=cw/2,y=ch/2){let a=plane(x,y);camera.scale=clamp(camera.scale*f,.5,64);let b=plane(x,y);camera.u+=a.u-b.u;camera.v+=a.v-b.v;draw()}
  function hint(){if(active&&(tool!=='walls'||hand))$('mapHint').textContent=hand?'Drag to move the view, drag with the middle mouse button to rotate the view or scroll to zoom.':'Left-click to edit. Drag with the right mouse button or hold Space to move the view. Use the middle mouse button or hold Alt while dragging to rotate the view, or scroll to zoom.'}
  function overview(){let c=$('overview'),g=c.getContext('2d');g.fillStyle='#17353e';g.fillRect(0,0,188,188);if(overviewImage.complete&&overviewImage.naturalWidth)g.drawImage(overviewImage,0,0,188,188);if(!state)return;let k=188/(worldMaxX()+1-worldMinX()),ky=188/944;g.strokeStyle='#ffe1a0';g.lineWidth=1.5;g.strokeRect((worldMaxX()-state.xmax)*k,state.ymin*ky,MAP_SIZE*k,MAP_SIZE*ky);let x=(worldMaxX()-state.xmax+camera.u)*k,y=(state.ymin+camera.v)*ky;g.fillStyle='#fff';g.beginPath();g.arc(x,y,3,0,7);g.fill()}
  async function navigate(x,y){if(loading||!state)return false;let xmax=clamp(Math.round(x+MAP_HALF),worldMinX()+MAP_LAST,worldMaxX()),ymin=clamp(Math.round(y-MAP_HALF),0,(944-MAP_SIZE));if(xmax===state.xmax&&ymin===state.ymin)return true;loading=true;hover=null;let loaded=await busy('Saving draft and opening the next area…',async()=>{if(dirty)await save();let next=await api(`/api/region?xmax=${xmax}&ymin=${ymin}`);let oldX=state.xmax,oldY=state.ymin;state=next;camera.u+=xmax-oldX;camera.v+=oldY-ymin;customPrefabs=state.prefabs;selection=null;lastTarget=null;updateUndo();inspect();invalid=true;$('areaName').textContent=`Around ${Math.round(x)}, ${Math.round(y)}`;status('Area loaded · draft saved · undo history kept');return true});loading=false;draw();return loaded===true}
  function continueMap(){if(!active||loading||!state)return;if(camera.u<38||camera.u>MAP_SIZE-38||camera.v<38||camera.v>MAP_SIZE-38)navigate(state.xmax-camera.u,state.ymin+camera.v)}
  async function jump(x,y){if(!state||loading)return;if(!await navigate(x,y))return;camera.u=state.xmax-x;camera.v=y-state.ymin;camera.y=ground(camera.u,camera.v);camera.scale=Math.max(camera.scale,12);if(!active)fit();draw()}
  canvas.addEventListener('pointerdown',e=>{if(loading){e.stopImmediatePropagation();return}if(!active||!state)return;if(e.button===2||e.button===1||space||hand||e.altKey){e.preventDefault();e.stopImmediatePropagation();canvas.focus();canvas.setPointerCapture(e.pointerId);navigation={kind:e.button===1||e.altKey?'orbit':'pan',x:e.clientX,y:e.clientY,u:camera.u,v:camera.v,yaw:camera.yaw,pitch:camera.pitch};hover=null;canvas.style.cursor='grabbing'}},true);
  canvas.addEventListener('pointermove',e=>{if(!navigation)return;e.stopImmediatePropagation();let dx=e.clientX-navigation.x,dy=e.clientY-navigation.y;if(navigation.kind==='orbit'){camera.yaw=navigation.yaw+dx*.008;camera.pitch=clamp(navigation.pitch+dy*.006,.25,1.55)}else{let c=Math.cos(camera.yaw),s=Math.sin(camera.yaw),rx=dx/camera.scale,rz=dy/camera.scale/Math.sin(camera.pitch);camera.u=clamp(navigation.u-rx*c+rz*s,state.xmax-worldMaxX(),state.xmax-worldMinX());camera.v=clamp(navigation.v-rx*s-rz*c,-state.ymin,943-state.ymin)}draw()},true);
  function stopNavigation(e){if(!navigation)return;e.stopImmediatePropagation();let pan=navigation.kind==='pan';navigation=null;canvas.style.cursor=hand?'grab':'crosshair';if(pan)continueMap();draw()}
  canvas.addEventListener('pointerup',stopNavigation,true);canvas.addEventListener('pointercancel',stopNavigation,true);
  window.addEventListener('blur',()=>{navigation=null;canvas.style.cursor=hand?'grab':'crosshair'});
  document.addEventListener('keydown',e=>{if(!active||loading||['INPUT','SELECT','TEXTAREA'].includes(document.activeElement.tagName)||document.querySelector('dialog[open]'))return;let k=e.key.toLowerCase();if(e.ctrlKey||e.metaKey)return;if(k==='h'){$('handTool').click();e.preventDefault()}if(k==='home'){$('resetCamera').click();e.preventDefault()}if(k==='f'){focus();e.preventDefault()}if(k==='q'||k==='e'){camera.yaw+=k==='q'?-.12:.12;draw()}if(k.startsWith('arrow')){e.preventDefault();let dx=k==='arrowleft'?-4:k==='arrowright'?4:0,dz=k==='arrowup'?-4:k==='arrowdown'?4:0,c=Math.cos(camera.yaw),s=Math.sin(camera.yaw);camera.u=clamp(camera.u+dx*c-dz*s,state.xmax-worldMaxX(),state.xmax-worldMinX());camera.v=clamp(camera.v+dx*s+dz*c,-state.ymin,943-state.ymin);draw()}});
  document.addEventListener('keyup',e=>{if(e.key.startsWith('Arrow'))continueMap()});
  function showMode(use3D){active=use3D;surface.hidden=!active;$('mode3d').classList.toggle('active',active);$('mode2d').classList.toggle('active',!active);hint();draw()}
  function restoreGraphics(){
    if(!gl||gl.isContextLost())return false;
    ready=false;graphicsLost=false;chunks.length=0;attributes.length=0;spriteBuffer=null;spriteTextures=new WeakMap();
    buffer=program=pickBuffer=pickTexture=pickDepth=atlasTexture=null;count=groundCount=0;
    pickSized=false;pickKey=null;lastPickRead=0;lastPickResult={u:-1,v:-1,x:0,y:0};lastPickMeshKey='';lastMeshBuild=0;meshFloor=-1;invalid=true;
    try{initialize();$('mode3d').title='';showMode(resume3D);return true}
    catch(error){ready=false;showMode(false);$('mode3d').title='Click to retry 3D';toast('Could not restore 3D. Click 3D to retry.');console.error('3D restoration failed',error);return false}
  }
  $('mode3d').onclick=()=>{resume3D=true;if(gl?.isContextLost()){toast('3D is reconnecting after a graphics reset.');return}if(gl&&(graphicsLost||!ready)){restoreGraphics();return}if(!gl){toast('3D graphics are unavailable. Reopen the editor to try again.');return}showMode(true)};
  $('mode2d').onclick=()=>{resume3D=false;active=false;surface.hidden=true;hand=false;$('handTool').classList.remove('active');$('mode2d').classList.add('active');$('mode3d').classList.remove('active');settings();fit()};
  $('handTool').onclick=()=>{hand=!hand;$('handTool').classList.toggle('active',hand);canvas.style.cursor=hand?'grab':'crosshair';hint()};
  $('resetCamera').onclick=()=>{camera.yaw=-.55;camera.pitch=.85;fit()};$('focusCamera').onclick=focus;
  $('showPeople').onchange=$('showItems').onchange=()=>draw();
  $('hideBarriers').onchange=$('upperLevels3d').onchange=$('roofs3d').onchange=$('scenery3d').onchange=()=>{invalid=true;draw()};
  $('overview').onclick=e=>{let r=e.currentTarget.getBoundingClientRect();jump(Math.round(worldMaxX()-(e.clientX-r.left)/r.width*(worldMaxX()-worldMinX())),Math.round((e.clientY-r.top)/r.height*943))};
  $('toggleNavigator').onclick=()=>{let hidden=!$('overview').hidden;$('overview').hidden=hidden;$('overview').style.display=hidden?'none':'block';$('landmark').hidden=hidden;$('navStatus').hidden=hidden};
  $('landmark').onchange=()=>{if($('landmark').value)jump(...$('landmark').value.split(',').map(Number))};
  $('goto').onclick=()=>{let x=+$('gotoX').value,y=+$('gotoY').value;if(!Number.isInteger(x)||!Number.isInteger(y)||x<worldMinX()||x>worldMaxX()||y<0||y>943){toast(`Use X ${worldMinX()} to ${worldMaxX()}, Y 0 to 943.`);return}jump(x,y)};
  surface.addEventListener('webglcontextlost',e=>{
    e.preventDefault();resume3D=active||resume3D;graphicsLost=true;ready=false;resourceGeneration++;pickKey=null;navigation=null;
    if(transaction)commit();drag=null;
    canvas.dispatchEvent(new Event('pointercancel'));
    showMode(false);
    $('mode3d').title='Reconnecting 3D after a graphics reset';
    toast('Graphics reset detected. Reconnecting 3D...');
  });
  surface.addEventListener('webglcontextrestored',()=>{if(restoreGraphics())toast(resume3D?'3D restored.':'3D is available again.');});
  if(gl){try{initialize()}catch(e){active=false;toast('3D setup failed: '+e.message)}}else surface.hidden=true;
  Promise.all([api('/models3d.json'),api('/materials.json')]).then(([models,materials])=>{library=models;catalog=materials;invalid=true;window.buildTools?.load(materials);draw()}).catch(e=>toast('Could not load the 3D asset library: '+e.message));
  function editing(){hand=false;$('handTool').classList.remove('active');canvas.style.cursor='crosshair'}
  if(active){$('handTool').classList.add('active');canvas.style.cursor='grab'}
  function refreshOverview(){overviewImage.src='/api/overview.png?revision='+state.revision+'&project='+state.projectKey}
  return {editing,refreshOverview,get active(){return active},get loading(){return loading},get catalog(){return catalog},camera,position,render,fit,zoom,project,plane,hint,focus,jump,navigate,invalidate(){invalid=true},invalidateTile,outline};
})();