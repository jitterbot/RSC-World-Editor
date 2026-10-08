                                                                                   
from pathlib import Path
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import base64, io, re, zlib
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
import npc_creator
import quest_editor
import object_creator
import argparse, collections, hashlib, json, os, secrets, shutil, socket, sqlite3, struct, subprocess, threading, time, urllib.parse, xml.etree.ElementTree as ET, zipfile

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent
BASE=ROOT/'openrsc-develop'
PROJECT=HERE/'project'
FILES={'objects':('SceneryLocs.json','sceneries'),'npcs':('NpcLocs.json','npclocs'),'boundaries':('BoundaryLocs.json','boundaries'),'items':('GroundItems.json','grounditems')}
TOKEN=secrets.token_urlsafe(32)
LOCK=threading.RLock()
EDIT_SIZE=288
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def write_json(p,value):
    p.parent.mkdir(parents=True,exist_ok=True);tmp=p.with_suffix(p.suffix+'.tmp')
    tmp.write_text(json.dumps(value,separators=(',',':')),encoding='utf-8');os.replace(tmp,p)
def packed_name(x,y,h): return f'h{h}x{(x+2304)//48}y{(y+1776)//48}',(((x+2304)%48)*48+(y+1776)%48)*10
def port_open(port=43594):
    try:
        with socket.create_connection(('127.0.0.1',port),timeout=.5): return True
    except OSError: return False
                                                                                             
class PlacementArt:
    ORDER=(11,2,9,7,1,6,10,0,5,8,3,4)
    def __init__(self,base,npcs):
        self.base=base;self.npcs=npcs;self.cache={};self.raw={}
        profiles=json.loads((HERE/'placement-art.json').read_text())
        source=base/'Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java'
        profile=profiles.get(digest(source) if source.exists() else '',profiles['default'])
        self.animations=profile['animations'];self.items=profile['items'];self.specials=profile.get('specials',{})
        equipment=HERE/'equipment-animations.json'
        self.animation_blue={}
        if equipment.exists():
            expanded=json.loads(equipment.read_text())
            self.animations=[[a['number'],a['colour']] for a in expanded]
            self.animation_blue={a['id']:a['blue'] for a in expanded}
        self.archive=base/'Client_Base/Cache/video/Authentic_Sprites.orsc'
        self.version=hashlib.sha256((str(base)+json.dumps(npcs,sort_keys=True)+json.dumps(profile)).encode()).hexdigest()[:16]
    def sprite(self,ident):
        if ident not in self.raw:
            with zipfile.ZipFile(self.archive) as z:data=z.read(str(ident))
            w,h,shift,x,y,fw,fh=struct.unpack_from('>ii?iiii',data)
            self.raw[ident]=(w,h,x if shift else 0,y if shift else 0,fw or w,fh or h,struct.unpack_from('>'+str(w*h)+'I',data,25))
        return self.raw[ident]
    def custom_sprite(self,location):
        if not hasattr(self,'custom'):
            import gzip
            data=gzip.decompress((self.base/'Client_Base/Cache/video/Custom_Sprites.osar').read_bytes());cursor=0;self.custom={}
            def read(fmt):
                nonlocal cursor
                result=struct.unpack_from(fmt,data,cursor);cursor+=struct.calcsize(fmt);return result
            def string():
                nonlocal cursor
                end=data.index(0,cursor);value=data[cursor:end].decode('latin1');cursor=end+1;return value
            for _ in range(read('>B')[0]):
                group=string()
                for _ in range(read('>H')[0]):
                    name=string();kind=read('>B')[0]
                    if kind in (1,2,3):read('>B')
                    frames=read('>B')[0];count=read('>B')[0]+1
                    palette=[read('>BBB') for _ in range(count)]
                    for frame in range(frames):
                        w,h,shift,x,y,fw,fh=read('>HH?hhHH');indices=data[cursor:cursor+w*h];cursor+=w*h
                        if group=='items' and frame==0:
                            colors=tuple((palette[i][0]<<16)|(palette[i][1]<<8)|palette[i][2] for i in indices)
                            self.custom[group+':'+name]=(w,h,x if shift else 0,y if shift else 0,fw or w,fh or h,colors)
        return self.custom[location]
    @staticmethod
    def tint(c,primary=0,skin=0,blue=0):
        r,g,b=(c>>16)&255,(c>>8)&255,c&255
        if r==g==b:
            mask=primary or 0xffffff
            return tuple((r*((mask>>s)&255))>>8 for s in (16,8,0))
        if r==255 and g==b:
            mask=skin or 0xffffff
            return tuple((v*((mask>>s)&255))>>8 for v,s in zip((r,g,b),(16,8,0)))
        if blue and blue!=0xffffff and r==g and b!=g:return tuple((((blue>>s)&255)*r*b)>>16 for s in (16,8,0))
        return r,g,b
    @staticmethod
    def png(w,h,pixels):
        def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
        rows=b''.join(b'\0'+pixels[y*w*4:(y+1)*w*4] for y in range(h))
        return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b'')
    def image(self,kind,ident):
        key=(kind,ident)
        if key in self.cache:return self.cache[key]
        special=self.specials.get(f'{kind}/{ident}')
        if special:
            image=(HERE/'placement-specials'/special).read_bytes();self.cache[key]=image;return image
        layers=[]
        if kind=='npcs':
            n=self.npcs[ident];w,h=96,144
            for layer in self.ORDER:
                anim=n.get('sprites'+str(layer+1),-1)
                if anim<0:continue
                sprite,colour=self.animations[anim]
                primary={1:n.get('hairColour',0),2:n.get('topColour',0),3:n.get('bottomColour',0)}.get(colour,colour)
                layers.append((sprite,primary,n.get('skinColour',0) if colour in (1,2,3) or anim>=230 else 0,self.animation_blue.get(anim,0)))
        else:
            sprite,primary,blue,location=self.items[str(ident)];w,h=48,32
            try:self.sprite(sprite)
            except KeyError:sprite=location
            layers=[(sprite,primary,0,blue)]
        pixels=bytearray(w*h*4)
        for sprite,primary,skin,blue in layers:
            sw,sh,sx,sy,fw,fh,data=self.custom_sprite(sprite) if isinstance(sprite,str) else self.sprite(sprite)
            for y in range(h):
                yy=y*fh//h-sy
                if yy<0 or yy>=sh:continue
                for x in range(w):
                    xx=x*fw//w-sx
                    if xx<0 or xx>=sw:continue
                    c=data[yy*sw+xx]
                    if c==0:continue
                    off=(y*w+x)*4;pixels[off:off+4]=bytes((*self.tint(c,primary,skin,blue),255))
        image=self.png(w,h,pixels)
        self.cache[key]=image
        return image

class Workshop:
    def __init__(self,base=BASE,project=PROJECT):
        self.base=base;self.project=project;self.project.mkdir(parents=True,exist_ok=True)
        self.meta_path=project/'project.json'
        defs=base/'server/conf/server/defs'
        self.objects=[{'id':i,'name':e.findtext('name','Object').strip(),'width':int(e.findtext('width','1')),'height':int(e.findtext('height','1')),'solid':e.findtext('type') in ('1','2'),'model':e.findtext('objectModel',''),'description':e.findtext('description','').strip()} for i,e in enumerate(ET.parse(defs/'GameObjectDef.xml').getroot())]
                                                                                  
        item_definitions={e['id']:e for e in json.loads((defs/'ItemDefs.json').read_text(encoding='utf-8'))['item']}
        item_names={i:e['name'] for i,e in item_definitions.items()}
        for filename in ('ItemDefsCustom.json','ItemDefsPatch18.json'):
            source=defs/filename
            if source.exists():
                extra=json.loads(source.read_text(encoding='utf-8-sig'))
                for e in extra.get('item',extra.get('items',[])):
                    item_definitions[e['id']]={**item_definitions.get(e['id'],{}),**e}
                    item_names[e['id']]=item_definitions[e['id']]['name']
        ore_names={149:'Clay',150:'Copper',151:'Iron',152:'Gold',153:'Mithril',154:'Adamantite',155:'Coal',202:'Tin',266:'Blurite',383:'Silver',409:'Runite'}
        for entry in ET.parse(defs/'extras/ObjectMining.xml').getroot():
            object_id=int(entry.findtext('int')); mining=entry.find('ObjectMiningDef')
            ore_id=int(mining.findtext('oreId')); obj=self.objects[object_id]
            ore=ore_names.get(ore_id,item_names.get(ore_id,'Mining resource'))
            obj['gameName']=obj['name'];obj['ore']=ore;obj['miningLevel']=int(mining.findtext('requiredLvl'))
            suffix=' (tutorial)' if object_id==496 else ' (quest)' if ore_id not in ore_names else ''
            obj['name']=f"{ore} rock{suffix} · #{object_id}"
        for obj in self.objects:
            if obj['model'].lower() in ('redberrybush','cadavaberrybush','dwellberrybush','jangerberrybush','whiteberrybush','depletedbush'):
                obj['name']+=' [unsupported texture - avoid]'
        for object_id in (98,99):
            self.objects[object_id]['name']=f'Depleted rock (no ore) · #{object_id}'
        fence_names={45:'Wooden railing / fence',597:'Gnome fence: variant 1 (blocking)',718:'Gnome fence: variant 2 (blocking)',951:'Gnome fence: pass-through decoration',691:'Bridge blockade / gnome fence (scripted)',284:'Hedge fence'}
        for object_id,label in fence_names.items():
            self.objects[object_id]['name']=f'{label} · #{object_id}'
        self.npc_defs=[]
                                                                              
                                                                                
        npc_definitions={}
        for filename in ('NpcDefs.json','NpcDefsCustom.json'):
            source=defs/filename
            if filename=='NpcDefsCustom.json' and not source.exists():continue
            for definition in json.loads(source.read_text(encoding='utf-8-sig'))['npcs']:
                npc_definitions[definition['id']]=definition
        for n in npc_definitions.values():
            attackable=bool(n.get('attackable',0));level=n.get('combatlvl',0)
            label='Attackable' if attackable else 'Non-combat'
            self.npc_defs.append({'id':n['id'],'name':f"{n['name']} · {label} · Lv {level} · #{n['id']}", 'gameName':n['name'],'level':level,'attackable':attackable,'aggressive':bool(n.get('aggressive',0)),'description':n.get('description',''),'commands':[n[k] for k in ('command','command2') if n.get(k)]})
        self.art=PlacementArt(base,npc_definitions)
        self.item_defs=[{'id':int(i),'name':item_names.get(int(i),'Item #'+i),'description':item_definitions.get(int(i),{}).get('description','')} for i in self.art.items]
        for n in self.npc_defs:
            raw=npc_definitions[n['id']];n['imageWidth']=raw.get('camera1',145)/128;n['imageHeight']=raw.get('camera2',220)/128
        self.npc_ids={n['id'] for n in self.npc_defs}
        self.doors=list(ET.parse(defs/'DoorDef.xml').getroot())
        self.overlay_defs=list(ET.parse(defs/'TileDef.xml').getroot())
        self.tile_block={i+1 for i,e in enumerate(self.overlay_defs) if e.findtext('objectType')!='0'}|{250}
        if not self.meta_path.exists():
            self.meta={'name':'Imported map','xmax':207,'ymin':576,'size':144,'expected':self.live_hashes(),'revision':0,'lastSaved':None,'lastDeployed':None}
            source=base/'server/conf/server/data/Custom_Landscape.orsc'
            if not source.is_file() or 'custom_landscape: true' not in (base/'server/local.conf').read_text():source=source.with_name('Authentic_Landscape.orsc')
            shutil.copy2(source,project/'Landscape.orsc')
            for filename,_ in FILES.values(): shutil.copy2(defs/'locs'/filename,project/filename)
            write_json(self.meta_path,self.meta)
        self.reload()
        npc_creator.load(self)
        object_creator.load(self)
        for rel in object_creator.LIVE:
            if rel not in self.meta.get("expected",{}):
                self.meta.setdefault("expected",{})[rel]=digest(self.base/rel) if (self.base/rel).exists() else None
                write_json(self.meta_path,self.meta)
        if quest_editor.LIVE not in self.meta.get('expected',{}):
            self.meta.setdefault('expected',{})[quest_editor.LIVE]=digest(self.base/quest_editor.LIVE) if (self.base/quest_editor.LIVE).exists() else None
            write_json(self.meta_path,self.meta)
    def reload(self):
        self.meta=json.loads(self.meta_path.read_text())
        with zipfile.ZipFile(self.project/'Landscape.orsc') as z:self.map={n:bytearray(z.read(n)) for n in z.namelist()}
        self.locs={key:json.loads((self.project/f).read_text())[rows] for key,(f,rows) in FILES.items()}
    @property
    def min_x(self):
        bounds=self.base/'world-bounds.json'
        return int(json.loads(bounds.read_text()).get('minX',0)) if bounds.exists() else 0
    @property
    def max_x(self):return int(self.meta.get('maxX',943))
    def live_files(self):
        return ['server/conf/server/data/Custom_Landscape.orsc','Client_Base/Cache/video/Custom_Landscape.orsc','server/local.conf']+['server/conf/server/defs/locs/'+f for f,_ in FILES.values()]+npc_creator.LIVE+[quest_editor.LIVE]+object_creator.LIVE
    def live_hashes(self):return {f:(digest(self.base/f) if (self.base/f).exists() else None) for f in self.live_files()}
    def tile(self,x,y,h):
        n,o=packed_name(x,y,h)
        return list(struct.unpack_from('>6Bi',self.map[n],o)) if n in self.map else [0,0,250,0,0,0,0]
    def region(self,xmax=None,ymin=None):
        old_size=self.meta.get('size',144)
        xmax=max(self.min_x+EDIT_SIZE-1,min(self.max_x,self.meta['xmax']+(EDIT_SIZE-old_size)//2)) if xmax is None else int(xmax);ymin=max(0,min(944-EDIT_SIZE,self.meta['ymin']-(EDIT_SIZE-old_size)//2)) if ymin is None else int(ymin)
        if not self.min_x+EDIT_SIZE-1<=xmax<=self.max_x or not 0<=ymin<=944-EDIT_SIZE: raise ValueError('Choose a centre within the RSC world.')
        size=EDIT_SIZE
        def inside(p):return xmax-size<p.get('X',-99999)<=xmax and ymin<=p.get('Y',-1)%944<ymin+size and 0<=p.get('Y',-1)<3776
        result={'minX':self.min_x,'maxX':self.max_x,'projectKey':hashlib.sha256(str(self.project.resolve()).encode()).hexdigest(),'imported':bool(self.meta.get('imported')),'xmax':xmax,'ymin':ymin,'size':size,'revision':self.meta['revision'],'name':self.meta['name'],'lastSaved':self.meta.get('lastSaved'),'lastDeployed':self.meta.get('lastDeployed')}
        result['tiles']=[[self.tile(xmax-u,ymin+v,h) for v in range(size) for u in range(size)] for h in range(4)]
        for key,rows in self.locs.items():result[key]=[r for r in rows if inside(r.get('pos',r.get('start',{})))]
        result['prefabs']=json.loads((self.project/'prefabs.json').read_text()) if (self.project/'prefabs.json').exists() else []
        return result
    def validate_payload(self,p):
        if p.get('projectKey')!=hashlib.sha256(str(self.project.resolve()).encode()).hexdigest():raise ValueError('Another map was opened. Reload this window before saving; its edits have not been applied to the new map.')
        if json.loads(self.meta_path.read_text())['revision']!=self.meta['revision']:
            self.reload()
        if p.get('revision')!=self.meta['revision']:raise ValueError('This draft changed in another window. Reload before saving.')
        xmax=p.get('xmax');ymin=p.get('ymin')
        if not isinstance(xmax,int) or not isinstance(ymin,int) or not self.min_x+EDIT_SIZE-1<=xmax<=self.max_x or not 0<=ymin<=944-EDIT_SIZE:raise ValueError('Invalid map area.')
        layers=p.get('tiles',[])
        if len(layers)!=4 or any(len(a)!=EDIT_SIZE*EDIT_SIZE for a in layers):raise ValueError('Incomplete map data.')
        for a in layers:
            for t in a:
                if len(t)!=7 or any(type(v)!=int for v in t) or any(not 0<=v<=255 for v in t[:6]) or not 0<=t[6]<=65535:raise ValueError('Invalid tile values.')
                if t[2] not in (0,250) and t[2]>len(self.overlay_defs):raise ValueError('Unknown floor material.')
                if t[3]>6 or max(t[4:6])>len(self.doors):raise ValueError('Unknown roof or wall.')
        for key in FILES:
            if not isinstance(p.get(key),list) or len(p[key])>20000:raise ValueError('Invalid placements.')
            for r in p[key]:
                pos=r.get('pos',r.get('start',{}));x=pos.get('X');y=pos.get('Y');ident=r.get('id')
                if type(x)!=int or type(y)!=int or not xmax-EDIT_SIZE<x<=xmax or not 0<=y<3776 or not ymin<=y%944<ymin+EDIT_SIZE:raise ValueError('Placement outside this editing area.')
                if type(ident)!=int or ident<0:raise ValueError('Invalid asset.')
                if key=='objects' and (ident>=len(self.objects) or self.objects[ident].get('deleted')):raise ValueError('Unknown scenery.')
                if key=='items':
                    if str(ident) not in self.art.items:raise ValueError('Unknown item.')
                    if type(r.get('amount'))!=int or not 1<=r['amount']<=2147483647:raise ValueError('Invalid item quantity.')
                    if type(r.get('respawn'))!=int or not 0<=r['respawn']<=86400:raise ValueError('Invalid item respawn time.')
                if key=='npcs' and ident not in self.npc_ids:raise ValueError('Unknown NPC.')
                if key in ('objects','boundaries') and r.get('direction') not in range(8):raise ValueError('Invalid rotation.')
                if key=='npcs':
                    for bound in ('min','max'):
                        point=r.get(bound,{})
                        if type(point.get('X'))!=int or type(point.get('Y'))!=int or not self.min_x<=point['X']<=self.max_x or not 0<=point['Y']<3776:raise ValueError('Invalid NPC movement bounds.')
    def save(self,p):
        self.validate_payload(p);xmax,ymin=p['xmax'],p['ymin'];updated={n:bytearray(b) for n,b in self.map.items()}
        for h,a in enumerate(p['tiles']):
            for i,t in enumerate(a):
                n,o=packed_name(xmax-i%EDIT_SIZE,ymin+i//EDIT_SIZE,h)
                if n not in updated and t==[0,0,250,0,0,0,0]:continue
                if n not in updated:updated[n]=bytearray(struct.pack('>6Bi',0,0,250,0,0,0,0)*2304)
                struct.pack_into('>6Bi',updated[n],o,*t)
        locs={}
        for key,rows in self.locs.items():
            def outside(r):
                pos=r.get('pos',r.get('start',{}));x=pos.get('X',-1);y=pos.get('Y',-1)
                return not(xmax-EDIT_SIZE<x<=xmax and 0<=y<3776 and ymin<=y%944<ymin+EDIT_SIZE)
            replacements=iter(p[key]);locs[key]=[]
            for row in rows:
                if outside(row):locs[key].append(row)
                else:
                    replacement=next(replacements,None)
                    if replacement is not None:locs[key].append(replacement)
            locs[key].extend(replacements)
                                                                                          
        checkpoint=self.project/'previous-draft.zip'
        with zipfile.ZipFile(checkpoint,'w',zipfile.ZIP_DEFLATED) as z:
            for f in ['Landscape.orsc','project.json']+[a for a,_ in FILES.values()]:z.write(self.project/f,f)
        tmp=self.project/'Landscape.tmp'
        with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED) as z:
            for n,b in updated.items():z.writestr(n,b)
        os.replace(tmp,self.project/'Landscape.orsc')
        for key,(f,rows) in FILES.items():write_json(self.project/f,{rows:locs[key]})
        self.map=updated;self.locs=locs
        self.meta.update(xmax=xmax,ymin=ymin,size=EDIT_SIZE,revision=self.meta['revision']+1,lastSaved=time.strftime('%Y-%m-%d %H:%M:%S'))
        write_json(self.meta_path,self.meta)
        result={'revision':self.meta['revision'],'lastSaved':self.meta['lastSaved']}
        try:
            sync=self.sync_game()
            if sync=='pending':result['gameSyncPending']=True
            elif sync:result['gameSynced']=True
        except Exception as error:
            result['gameSyncError']=str(error)
        return result
    def sync_game(self):
        link=self.project/'game-sync.json'
        if not link.exists():return False
        target=Path(json.loads(link.read_text(encoding='utf-8'))['gameFolder']).resolve()
        if target!=self.base.resolve():raise ValueError('The linked game does not match this draft.')
        if not (target/'server/default.conf').is_file():raise ValueError('The linked game folder is missing.')
        updates={}
        landscape=(self.project/'Landscape.orsc').read_bytes()
        for rel in ('server/conf/server/data/Custom_Landscape.orsc','Client_Base/Cache/video/Custom_Landscape.orsc'):
            updates[target/rel]=landscape
        for filename,_ in FILES.values():updates[target/'server/conf/server/defs/locs'/filename]=(self.project/filename).read_bytes()
        updates.update({target/rel:data for rel,data in {**npc_creator.deployment(self),**object_creator.deployment(self)}.items()})
                                                                                     
        for filename in ('default.conf','local.conf'):
            conf=target/'server'/filename
            if not conf.exists():continue
            raw=conf.read_bytes();text=raw.decode('utf-8')
            text,count=re.subn(r'(?m)^(\s*custom_landscape:\s*)(?:true|false)\b',r'\g<1>true',text)
            if count!=1:raise ValueError('Could not identify the custom landscape setting in '+filename)
            encoded=text.encode('utf-8')
            if encoded!=raw:updates[conf]=encoded
                                                                                
        pending=target.parent/'Rsc World Editor-pending-save.zip'
        temporary=pending.with_suffix('.tmp')
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as archive:
            for path,data in updates.items():archive.writestr(path.relative_to(target).as_posix(),data)
        os.replace(temporary,pending)
        if port_open(43610):return 'pending'
        optional={target/rel for rel in npc_creator.LIVE+[quest_editor.LIVE]+object_creator.LIVE}
        previous={path:(path.read_bytes() if path.exists() else None) for path in updates if path in optional or path.exists()}
        missing=set(updates)-set(previous)
        if missing:raise ValueError('A required game file is missing: '+str(next(iter(missing))))
        if all(previous[path]==data for path,data in updates.items()):
            pending.unlink(missing_ok=True);return True
                                                                                         
        backup=target.parent/'Rsc World Editor backups';backup.mkdir(exist_ok=True)
        temporary=backup/'previous-map.tmp'
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as archive:
            for path,data in previous.items():
                if data is not None:archive.writestr(path.relative_to(target).as_posix(),data)
            archive.writestr('rscworldeditor-new-files.json',json.dumps([path.relative_to(target).as_posix() for path,data in previous.items() if data is None]))
        os.replace(temporary,backup/'previous-map.zip')
        try:
            for path,data in updates.items():
                path.parent.mkdir(parents=True,exist_ok=True)
                temporary=path.with_name(path.name+'.rscworldeditor.tmp');temporary.write_bytes(data);os.replace(temporary,path)
            for path,data in updates.items():
                if path.read_bytes()!=data:raise IOError('Verification failed for '+path.name)
        except Exception:
            for path,data in previous.items():
                if data is None:path.unlink(missing_ok=True)
                else:path.write_bytes(data)
            raise
        pending.unlink(missing_ok=True)
        return True
    def check(self):
                                                                                             
        warnings=[];xmax=self.meta['xmax'];ymin=self.meta['ymin'];occupied=set()
        for r in self.locs['objects']:
            x,y=r['pos']['X'],r['pos']['Y'];h=y//944;yy=y%944
            if not xmax-self.meta.get('size',144)<x<=xmax or not ymin<=yy<ymin+self.meta.get('size',144):continue
            d=self.objects[r['id']];ww,hh=d['width'],d['height']
            if r['direction'] not in (0,4):ww,hh=hh,ww
            if not d['solid']:continue
            for dx in range(ww):
                for dy in range(hh):occupied.add((x+dx,yy+dy,h))
        for n in self.locs['npcs']:
            x,y=n['start']['X'],n['start']['Y'];h=y//944;yy=y%944
            if not xmax-self.meta.get('size',144)<x<=xmax or not ymin<=yy<ymin+self.meta.get('size',144):continue
            if self.tile(x,yy,h)[2] in self.tile_block or (x,yy,h) in occupied:warnings.append(f"NPC at {x}, {y} is on water or blocked scenery.")
        if xmax-self.meta.get('size',144)<120<=xmax and ymin<=648<ymin+self.meta.get('size',144) and (self.tile(120,648,0)[2] in self.tile_block or (120,648,0) in occupied):warnings.append('The Lumbridge arrival tile (120, 648) is blocked.')
        return {'warnings':warnings[:30],'message':'Placement checks passed.' if not warnings else f'{len(warnings)} placement issues need attention.'}
    def deploy(self):
        if self.meta.get('imported'):raise ValueError('This is an imported map. Export it to your chosen game; Save & Test is reserved for the original Workshop project.')
        checks=self.check()
        if checks['warnings']:raise ValueError(checks['message']+' '+checks['warnings'][0])
        if self.live_hashes()!=self.meta['expected']:raise ValueError('The live world changed outside Rsc World Editor. Your draft is safe; reconcile those changes before replacing it.')
        db=self.base/'server/inc/sqlite/preservation.db'
        if port_open():
            with sqlite3.connect(f'file:{db.as_posix()}?mode=ro',uri=True) as conn:
                if conn.execute('SELECT count(*) FROM players WHERE online<>0').fetchone()[0]:raise ValueError('Log out of the game first, then click Save & Test again.')
        control('Stop')
        stamp=time.strftime('%Y%m%d-%H%M%S')+'-'+secrets.token_hex(2)
        backup=self.project/'backups'/stamp;backup.mkdir(parents=True)
        for rel in self.live_files():
            dest=backup/rel;dest.parent.mkdir(parents=True,exist_ok=True)
            if (self.base/rel).exists():shutil.copy2(self.base/rel,dest)
        try:
            for rel in self.live_files()[:2]:shutil.copy2(self.project/'Landscape.orsc',self.base/rel)
            for filename,_ in FILES.values():shutil.copy2(self.project/filename,self.base/'server/conf/server/defs/locs'/filename)
            for rel,data in {**npc_creator.deployment(self),**object_creator.deployment(self)}.items():(self.base/rel).write_bytes(data)
            config=(self.base/'server/local.conf').read_text().replace('custom_landscape: false','custom_landscape: true')
            (self.base/'server/local.conf').write_text(config)
            control('StartServer');control('StartClient')
        except Exception:
            control('Stop')
            for rel in self.live_files():
                if (backup/rel).exists():shutil.copy2(backup/rel,self.base/rel)
                elif rel in [quest_editor.LIVE]+object_creator.LIVE:(self.base/rel).unlink(missing_ok=True)
            raise
        self.meta.update(expected=self.live_hashes(),lastDeployed=time.strftime('%Y-%m-%d %H:%M:%S'))
        write_json(self.meta_path,self.meta)
        return {'message':'World backed up and updated. The game is ready.','backup':stamp,'lastDeployed':self.meta['lastDeployed']}
def control(action):
    environment={k.upper():v for k,v in os.environ.items()}
    if action in ('StartServer','StartClient'):
        logs=ROOT/'logs';logs.mkdir(exist_ok=True)
        server=action=='StartServer';name='server' if server else 'client'
        if server and port_open():return
        java=BASE/'Portable_Windows/zulu8.50.0.51-ca-jdk8.0.275-win_x64/bin'/('java.exe' if server else 'javaw.exe')
        args=['-Xms256m','-Xmx1024m','-Dopenrsc.bindAddress=127.0.0.1','-DcoloredLogging=false','-cp','core.jar;lib/*','com.openrsc.server.Server','default.conf'] if server else ['-Xms256m','-Xmx768m','-jar','Open_RSC_Client.jar']
        with (logs/(name+'.log')).open('w') as out,(logs/(name+'-errors.log')).open('w') as err:
            process=subprocess.Popen([str(java),*args],cwd=BASE/('server' if server else 'Client_Base'),env=environment,stdin=subprocess.DEVNULL,stdout=out,stderr=err,creationflags=0x08000000 if server else 0)
        (logs/(name+'.pid')).write_text(str(process.pid))
        if server:
            for _ in range(60):
                if process.poll() is not None:raise ValueError('Game server failed to start. See logs/server-errors.log.')
                if port_open():return
                time.sleep(.5)
            raise ValueError('Timed out waiting for the game server.')
        return
    result=subprocess.run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(HERE/'game-control.ps1'),action],env=environment,capture_output=True,text=True,timeout=65,creationflags=0x08000000)
    if result.returncode:raise ValueError((result.stderr or result.stdout).strip()[-1200:])
def preview(work,p):
    x=int(p.get('x',120));y=int(p.get('y',648));h=int(p.get('floor',0));angle=int(p.get('angle',888))%1024
    if not work.min_x<=x<=work.max_x or not 0<=y<=943 or not 0<=h<=3:raise ValueError('Choose a preview point inside the map.')
    if not port_open():control('StartServer')
    dest=work.project/'preview';(dest/'video').mkdir(parents=True,exist_ok=True)
    shutil.copy2(work.project/'Landscape.orsc',dest/'video/Custom_Landscape.orsc')
    object_files=object_creator.deployment(work)
    (dest/'WorkshopObjects.properties').write_bytes(object_files[object_creator.LIVE[2]])
    (dest/'models.orsc').write_bytes(object_files[object_creator.LIVE[3]])
    write_json(dest/'view.json',{'x':x,'y':y,'floor':h,'angle':angle,'hideRoofs':bool(p.get('hideRoofs',False))})
    java=BASE/'Portable_Windows/zulu8.50.0.51-ca-jdk8.0.275-win_x64/bin/java.exe'
    cp=os.pathsep.join(map(str,[BASE/'Client_Base/Open_RSC_Client.jar',BASE/'server/lib/json-20190722.jar',HERE/'classes']))
    result=subprocess.run([str(java),'-Xmx1200m','-cp',cp,'orsc.graphics.three.EditorPreview',str(work.project)],cwd=BASE/'Client_Base',capture_output=True,text=True,timeout=60,creationflags=0x08000000)
    (dest/'render.log').write_text(result.stdout+'\n'+result.stderr)
    if result.returncode or not (dest/'scene.png').exists():raise ValueError('Preview did not finish. Your draft is saved; see project/preview/render.log.')
    return {'image':'/preview.png?t='+str(time.time()),'message':'Rendered using the RSC client. NPCs and moving characters are not shown.'}
def overview_png(work):
    revision=work.meta['revision']
    if getattr(work,'overview_revision',None)==revision:return work.overview_image
    size=(work.max_x+1-work.min_x+1)//2;height=472;pixels=bytearray(bytes((28,53,62))*(size*height))
    definitions=json.loads((HERE/'web/materials.json').read_text(encoding='utf-8'))['tiles']
    colours=[]
    for k in range(256):
        i=k%64
        if k<64:r,g,b=255-i*4,255-int(i*1.75),255-i*4
        elif k<128:r,g,b=i*3,144,0
        elif k<192:r,g,b=192-int(i*1.5),144-int(i*1.5),0
        else:r,g,b=96-int(i*1.5),48+int(i*1.5),0
        colours.append(bytes((r//8*8,g//8*8,b//8*8)))
    for name,data in work.map.items():
        match=re.fullmatch(r'h0x([0-9]+)y([0-9]+)',name)
        if not match:continue
        sx,sy=map(int,match.groups())
        for xx in range(0,48,2):
            x=sx*48+xx-2304
            if not work.min_x<=x<=work.max_x:continue
            for yy in range(0,48,2):
                y=sy*48+yy-1776
                if not 0<=y<944:continue
                offset=(xx*48+yy)*10;overlay=data[offset+2]
                if overlay==250:continue
                colour=colours[data[offset+1]]
                if overlay in (2,7,11,25):colour=bytes((58,115,145))
                elif 0<overlay<=len(definitions):colour=bytes.fromhex(definitions[overlay-1]['color'][1:])
                target=((y//2)*size+(work.max_x-x)//2)*3;pixels[target:target+3]=colour
    rows=b''.join(b'\x00'+pixels[y*size*3:(y+1)*size*3] for y in range(height))
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    work.overview_image=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',size,height,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(rows,6))+chunk(b'IEND',b'')
    work.overview_revision=revision;return work.overview_image

def map_sources():
    sources=[]
    for ident in ('original','extended','blank'):
        folder=HERE/'templates'/ident
        info=json.loads((folder/'template.json').read_text())
        sources.append({'id':'template:'+ident,'name':info['name'],'path':str(folder/'Landscape.orsc'),'placements':str(folder),'assetBase':str(BASE)})
    saved=[]
    for meta in (HERE/'projects').glob('*/project.json'):
        info=json.loads(meta.read_text(encoding='utf-8'))
        if info.get('lastSaved') and (meta.parent/'Landscape.orsc').exists():saved.append((info['lastSaved'],meta.stat().st_mtime_ns,meta,info))
    if saved:
        _,_,meta,info=max(saved,key=lambda row:row[:2])
        sources.append({'id':'draft:'+meta.parent.name,'name':'Resume last saved draft · '+info['name'],'path':str(meta.parent/'Landscape.orsc'),'placements':str(meta.parent),'assetBase':str(BASE)})
    return sources

def template_map(ident,destination=None):
    if ident not in ('original','extended','blank'):raise ValueError('Unknown map template.')
    template=HERE/'templates'/ident
    info=json.loads((template/'template.json').read_text())
    destination=destination or HERE/'projects'/('map-'+time.strftime('%Y%m%d-%H%M%S')+'-'+secrets.token_hex(3))
    destination.mkdir(parents=True,exist_ok=False)
    for filename in ['Landscape.orsc']+[f for f,_ in FILES.values()]:shutil.copy2(template/filename,destination/filename)
    write_json(destination/'project.json',{'name':info['name'],'xmax':287 if ident=='blank' else 263,'ymin':0 if ident=='blank' else 504,'size':EDIT_SIZE,'maxX':info['maxX'],'revision':0,'imported':True,'expected':{},'lastSaved':None,'lastDeployed':None})
    return Workshop(base=BASE,project=destination)

def default_map():
    project=HERE/'projects/original-rsc'
    if (project/'project.json').is_file():return Workshop(base=BASE,project=project)
    return template_map('original',project)

def open_map(payload,destination=None):
    asset_base=BASE
    if str(payload.get('source','')).startswith('template:'):return template_map(payload['source'].split(':',1)[1],destination)
    if payload.get('source')=='workshop-draft':return Workshop(project=HERE/'project')
    if payload.get('source'):
        match=next((s for s in map_sources() if s['id']==payload['source']),None)
        if not match:raise ValueError('That map source is unavailable.')
        if payload['source'].startswith('draft:'):return Workshop(base=Path(match['assetBase']),project=Path(match['path']).parent)
        asset_base=Path(match['assetBase'])
        landscape=Path(match['path']).read_bytes();name=match['name'];placements={key:json.loads((Path(match['placements'])/f).read_text(encoding='utf-8')) for key,(f,rows) in FILES.items()}
    else:
        name=str(payload.get('name','Imported map'))[:120]
        if not name.lower().endswith('.orsc'):raise ValueError('Choose Custom_Landscape.orsc or Authentic_Landscape.orsc, not a models or sprites file.')
        landscape=base64.b64decode(payload.get('landscape',''),validate=True);placements=payload.get('placements',{})
    if len(landscape)>8000000:raise ValueError('Landscape archive is too large.')
    with zipfile.ZipFile(io.BytesIO(landscape)) as archive:
        sections=archive.infolist()
        if not sections or len(sections)>4096:raise ValueError('Not a supported RSC landscape archive.')
        for section in sections:
            if not re.fullmatch(r'h[0-3]x[0-9]{1,3}y[0-9]{1,3}',section.filename) or section.file_size!=23040:raise ValueError('Not a supported RSC landscape archive. Choose a Landscape.orsc file.')
            archive.read(section)                                               
    for key,(filename,rows) in FILES.items():
        if key in placements and (not isinstance(placements[key],dict) or not isinstance(placements[key].get(rows),list)):raise ValueError('Invalid companion file: '+filename)
    destination=destination or HERE/'projects'/('import-'+time.strftime('%Y%m%d-%H%M%S')+'-'+secrets.token_hex(3))
    work=Workshop(base=asset_base,project=destination)
    (destination/'Landscape.orsc').write_bytes(landscape)
    for key,(filename,rows) in FILES.items():write_json(destination/filename,placements.get(key,{rows:[]}))
    work.meta.update(name=name,imported=True,maxX=999,sourceName=name,revision=0,lastSaved=None,lastDeployed=None)
    write_json(work.meta_path,work.meta);work.reload();return work

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def send(self,status,data,kind='application/json'):
        body=json.dumps(data).encode() if kind=='application/json' else data
        self.send_response(status);self.send_header('Content-Type',kind);self.send_header('Content-Length',str(len(body)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(body)
    def do_GET(self):
        try:
            url=urllib.parse.urlparse(self.path);q=urllib.parse.parse_qs(url.query)
            if url.path=='/api/region':
                with LOCK: self.send(200,WORK.region(q.get('xmax',[None])[0],q.get('ymin',[None])[0]))
            elif url.path=='/api/overview.png':
                with LOCK:self.send(200,overview_png(WORK),'image/png')
            elif url.path=='/api/sources':self.send(200,map_sources())
            elif url.path=='/api/export':
                with LOCK:
                    archive=q.get('format',['orsc'])[0]=='zip'
                    if archive:
                        target=io.BytesIO()
                        with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
                            z.write(WORK.project/'Landscape.orsc','Custom_Landscape.orsc')
                            z.writestr('world-bounds.json',json.dumps({'minX':WORK.min_x,'maxX':WORK.max_x,'minY':0,'maxY':943,'floorStride':944}))
                            for filename,_ in FILES.values():z.write(WORK.project/filename,filename)
                            if WORK.custom_npcs or WORK.custom_objects or quest_editor.load(WORK):
                                z.writestr('WorkshopObjects-library.json',json.dumps(WORK.custom_objects))
                                z.writestr('WorkshopNpcs-library.json',json.dumps(WORK.custom_npcs))
                                z.writestr('WorkshopQuests-library.json',json.dumps(quest_editor.load(WORK)))
                                for rel,data in {**npc_creator.deployment(WORK),**object_creator.deployment(WORK)}.items():z.writestr(rel,data)
                        body=target.getvalue();name='Rsc World Editor-map-bundle.zip'
                    else:body=(WORK.project/'Landscape.orsc').read_bytes();name='Custom_Landscape.orsc'
                    self.send_response(200);self.send_header('Content-Type','application/octet-stream');self.send_header('Content-Disposition','attachment; filename='+name);self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
            elif re.fullmatch(r'/api/npc-part/[0-9]+/-?[0-9]+\.png',url.path):
                slot,ident=url.path.split('/')[-2:]
                with LOCK:image=npc_creator.part_image(WORK,int(slot),int(ident[:-4]))
                self.send(200,image,'image/png')
            elif url.path=='/api/object-creator':self.send(200,object_creator.catalog(WORK))
            elif url.path=='/api/quests':self.send(200,quest_editor.catalog(WORK))
            elif url.path=='/api/npc-creator':self.send(200,{**npc_creator.catalog(WORK),'libraryRevision':npc_creator.revision(WORK)})
            elif url.path=='/api/assets':self.send(200,{'app':'Rsc World Editor','objects':WORK.objects,'npcs':WORK.npc_defs,'items':WORK.item_defs,'artVersion':WORK.art.version,'blocked':sorted(WORK.tile_block),'token':TOKEN})
            elif re.fullmatch(r'/api/placement/(npcs|items)/[0-9]+\.png',url.path):
                _,_,_,kind,name=url.path.split('/')
                with LOCK:image=WORK.art.image(kind,int(name[:-4]))
                self.send(200,image,'image/png')
            elif url.path=='/preview.png':self.send(200,(WORK.project/'preview/scene.png').read_bytes(),'image/png')
            elif url.path=='/thumbnails/index.json':self.send(200,json.loads((HERE/'thumbnails/index.json').read_text()))
            elif url.path.startswith('/thumbnails/') and url.path.endswith('.png') and url.path[12:-4].isdigit():
                self.send(200,(HERE/'thumbnails'/url.path.rsplit('/',1)[1]).read_bytes(),'image/png')
            elif url.path in ('/preview-marker.js','/terrain-lasso.js','/ground-floor-guides.js','/lasso-move.js','/object-creator.js','/quest-editor.js','/npc-creator.js','/placement-images.js','/map-files.js','/world3d.js','/build-tools.js','/models3d.json','/materials.json','/textures.png','/overview.png'):
                kind={'/preview-marker.js':'text/javascript; charset=utf-8','/terrain-lasso.js':'text/javascript; charset=utf-8','/ground-floor-guides.js':'text/javascript; charset=utf-8','/lasso-move.js':'text/javascript; charset=utf-8','/object-creator.js':'text/javascript; charset=utf-8','/quest-editor.js':'text/javascript; charset=utf-8','/npc-creator.js':'text/javascript; charset=utf-8','/placement-images.js':'text/javascript; charset=utf-8','/map-files.js':'text/javascript; charset=utf-8','/build-tools.js':'text/javascript; charset=utf-8','/materials.json':'application/json','/textures.png':'image/png','/world3d.js':'text/javascript; charset=utf-8','/models3d.json':'application/json','/overview.png':'image/png'}[url.path]
                data=(HERE/'web'/url.path[1:]).read_bytes()
                if url.path in ('/models3d.json','/materials.json'): self.send(200,json.loads(data))
                else: self.send(200,data,kind)
            elif url.path in ('/','/app.js','/style.css'):
                f={'/':'index.html','/app.js':'app.js','/style.css':'style.css'}[url.path]
                kind={'/':'text/html; charset=utf-8','/app.js':'text/javascript; charset=utf-8','/style.css':'text/css; charset=utf-8'}[url.path]
                self.send(200,(HERE/'web'/f).read_bytes(),kind)
            else:self.send(404,{'error':'Not found'})
        except Exception as e:self.send(400,{'error':str(e)})
    def do_POST(self):
        global WORK
        if self.headers.get('X-Workshop-Token')!=TOKEN:return self.send(403,{'error':'Reload the editor to reconnect.'})
        if self.headers.get('Origin') not in (None,f'http://127.0.0.1:{self.server.server_port}',f'http://localhost:{self.server.server_port}'):return self.send(403,{'error':'Invalid origin.'})
        try:
            length=int(self.headers.get('Content-Length','0'))
            if not 0<length<(40000000 if self.path=='/api/open' else 12000000):raise ValueError('Request too large.')
            p=json.loads(self.rfile.read(length))
            with LOCK:
                if self.path=='/api/open':
                    WORK=open_map(p);result=WORK.region()
                elif self.path=='/api/save':result=WORK.save(p)
                elif self.path=='/api/quest-draft':result=quest_editor.save_draft(WORK,p,write_json)
                elif self.path=='/api/quest-save':result=quest_editor.save(WORK,p,write_json)
                elif self.path=='/api/object-delete':result=object_creator.delete(WORK,p,write_json)
                elif self.path=='/api/object-save':result=object_creator.save(WORK,p,write_json)
                elif self.path=='/api/npc-save':result=npc_creator.save(WORK,p,write_json)
                elif self.path=='/api/npc-preview':
                    entry=npc_creator.validate(WORK,{**p,'nodes':[{'id':0,'text':'Preview','replies':[]}]})
                    ident=max(WORK.art.npcs)+1
                    WORK.art.npcs[ident]=entry['definition']
                    try:result={'image':'data:image/png;base64,'+base64.b64encode(WORK.art.image('npcs',ident)).decode()}
                    finally:WORK.art.npcs.pop(ident,None);WORK.art.cache.pop(('npcs',ident),None)
                elif self.path=='/api/check':result=WORK.check()
                elif self.path=='/api/deploy':result=WORK.deploy()
                elif self.path=='/api/preview':result=preview(WORK,p)
                elif self.path=='/api/prefabs':
                    prefabs=p.get('prefabs',[])
                    if len(prefabs)>80 or len(json.dumps(prefabs))>3000000:raise ValueError('Prefab library is too large.')
                    if any(r.get('id') not in range(len(WORK.objects)) or WORK.objects[r['id']].get('deleted') for prefab in prefabs for r in prefab.get('objects',[])):raise ValueError('A stamp contains deleted scenery. Reload the map.')
                    write_json(WORK.project/'prefabs.json',prefabs);result={'message':'Prefab library saved.'}
                else:raise ValueError('Unknown action.')
            self.send(200,result)
        except Exception as e:self.send(400,{'error':str(e)})
def main():
    global WORK
    args=argparse.ArgumentParser();args.add_argument('--port',type=int,default=8765);args.add_argument('--parent-pid',type=int);opt=args.parse_args()
    WORK=default_map();server=ThreadingHTTPServer(('127.0.0.1',opt.port),Handler)
    if opt.parent_pid:
        def watch_parent():
            import ctypes
            kernel=ctypes.WinDLL('kernel32',use_last_error=True)
            kernel.OpenProcess.restype=ctypes.c_void_p
            kernel.WaitForSingleObject.argtypes=[ctypes.c_void_p,ctypes.c_ulong]
            kernel.CloseHandle.argtypes=[ctypes.c_void_p]
            handle=kernel.OpenProcess(0x00100000,False,opt.parent_pid)
            if handle:
                kernel.WaitForSingleObject(handle,0xffffffff);kernel.CloseHandle(handle)
            server.shutdown()
        threading.Thread(target=watch_parent,daemon=True).start()
    print(f'Rsc World Editor ready: http://127.0.0.1:{server.server_port}',flush=True);server.serve_forever()
if __name__=='__main__':main()

