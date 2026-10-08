                                                                   
import bz2,copy,hashlib,json,math,struct,time
import xml.etree.ElementTree as ET
from pathlib import Path

LIVE=['server/conf/server/defs/GameObjectDef.xml',
      'server/conf/server/defs/extras/ObjectWoodcutting.xml',
      'Client_Base/Cache/WorkshopObjects.properties',
      'Client_Base/Cache/video/models.orsc',
      'server/conf/server/defs/WorkshopObjects.json']
LIMIT=100

def xml_bytes(root):return ET.tostring(root,encoding='utf-8',xml_declaration=True)
def name_hash(name):
    value=0
    for c in name.upper():value=(value*61+ord(c)-32)&0xffffffff
    return value

def archive_rows(data):
    unpacked=int.from_bytes(data[:3],'big');packed=int.from_bytes(data[3:6],'big')
    if len(data)!=packed+6:raise ValueError('Invalid model archive length.')
    body=data[6:] if unpacked==packed else bz2.decompress(b'BZh1'+data[6:])
    if len(body)!=unpacked:raise ValueError('Invalid unpacked model archive length.')
    count=int.from_bytes(body[:2],'big');offset=2+10*count;rows={}
    for i in range(count):
        header=body[2+10*i:12+10*i];key=int.from_bytes(header[:4],'big');size=int.from_bytes(header[4:7],'big');length=int.from_bytes(header[7:10],'big')
        raw=body[offset:offset+length];offset+=length
        if size!=length:raw=bz2.decompress(b'BZh1'+raw)
        if len(raw)!=size:raise ValueError('Invalid model data.')
        rows[key]=raw
    if offset!=len(body):raise ValueError('Invalid model archive directory.')
    return rows

def archive_bytes(rows):
    body=len(rows).to_bytes(2,'big')+b''.join(k.to_bytes(4,'big')+len(v).to_bytes(3,'big')*2 for k,v in rows.items())+b''.join(rows.values())
    return len(body).to_bytes(3,'big')*2+body

def scaled_model(raw,scale):
    data=bytearray(raw);count=int.from_bytes(data[:2],'big')
    if len(data)<4+6*count:raise ValueError('Incomplete source model.')
    for axis,percent in enumerate(scale):
        for i in range(count):
            offset=4+2*(axis*count+i);value=struct.unpack_from('>h',data,offset)[0]
            value=math.floor(value*percent/100+0.5)
            if not -32768<=value<=32767:raise ValueError('This model is too large at that size. Reduce the size.')
            struct.pack_into('>h',data,offset,value)
    return bytes(data)

def read_base(work):
    root=ET.parse(work.base/LIVE[0]).getroot()
                                                                           
    while len(root) and root[-1].findtext('objectModel','').startswith('wsobj_'):root.remove(root[-1])
    if any(e.findtext('objectModel','').startswith('wsobj_') for e in root):raise ValueError('Custom scenery definitions are not a contiguous library.')
    return root

def load(work):
    work.base_object_xml=read_base(work)
    work.objects=work.objects[:len(work.base_object_xml)]
    work.base_objects=copy.deepcopy(work.objects)
    path=work.project/'WorkshopObjects.json'
    deployed=work.base/LIVE[4]
    work.custom_objects=json.loads(path.read_text(encoding='utf-8')) if path.exists() else json.loads(deployed.read_text(encoding='utf-8')) if deployed.exists() else []
    if not path.exists():path.write_text(json.dumps(work.custom_objects),encoding='utf-8')
    work.object_models=archive_rows((work.base/LIVE[3]).read_bytes())
    work.woodcut_xml=ET.parse(work.base/LIVE[1]).getroot()
    work.woodcut_sources={int(e.findtext('int')):e for e in work.woodcut_xml if int(e.findtext('int'))<len(work.base_objects)}
    for o in work.base_objects:
        o['canChop']=o['id'] in work.woodcut_sources and o['id']!=245 and work.base_object_xml[o['id']].findtext('command1','').lower()=='chop'
    if len(work.custom_objects)>LIMIT:raise ValueError('Too many custom scenery objects.')
    for i,row in enumerate(work.custom_objects):
        if row.get('id')!=len(work.base_objects)+i:raise ValueError('Custom scenery IDs are not contiguous.')
        validate(work,row)
    refresh(work)

def refresh(work):
    work.objects=copy.deepcopy(work.base_objects)
    for row in work.custom_objects:
        work.objects.append(dict(id=row['id'],name=row['name']+' · Custom · #'+str(row['id']),gameName=row['name'],description=row['description'],width=row['width'],height=row['height'],solid=row['solid'],model='wsobj_'+str(row['id']),sourceId=row['sourceId'],scale=row['scale'],custom=True,deleted=row.get('deleted',False),canChop=row['chop']))

def revision(work):return hashlib.sha256(json.dumps(work.custom_objects,sort_keys=True).encode()).hexdigest()
def catalog(work):return dict(objects=work.objects,custom=[r for r in work.custom_objects if not r.get('deleted')],revision=revision(work))
def integer(value,lo,hi,label):
    if type(value)!=int or not lo<=value<=hi:raise ValueError(label+' must be between '+str(lo)+' and '+str(hi)+'.')
    return value
def text(value,label,limit):
    if not isinstance(value,str) or not value.strip() or len(value)>limit or any(ord(c)<32 for c in value):raise ValueError(label+' is required (up to '+str(limit)+' characters).')
    return value.strip()
def validate(work,p):
    source=integer(p.get('sourceId'),0,len(work.base_objects)-1,'Source object')
    raw=work.object_models.get(name_hash(work.base_objects[source]['model']+'.ob3'))
    if raw is None:raise ValueError('This object has no available model to copy.')
    scale=p.get('scale',[100,100,100])
    if not isinstance(scale,list) or len(scale)!=3:raise ValueError('Set width, height and depth percentages.')
    scale=[integer(v,10,400,'Model size') for v in scale]
    scaled_model(raw,scale)
    for field in ('solid','chop'):
        if type(p.get(field,False))!=bool:raise ValueError('Invalid '+field+' option.')
    chop=p.get('chop',False)
    if chop and not work.base_objects[source]['canChop']:raise ValueError('This source does not support ordinary woodcutting.')
    return dict(sourceId=source,name=text(p.get('name'),'Name',60),description=text(p.get('description'),'Examine text',500),scale=scale,width=integer(p.get('width'),1,16,'Ground width'),height=integer(p.get('height'),1,16,'Ground depth'),solid=p.get('solid',False),chop=chop)

def save(work,p,write_json):
    if p.get('projectKey')!=hashlib.sha256(str(work.project.resolve()).encode()).hexdigest():raise ValueError('The active map changed. Reopen the structure editor.')
    if p.get('revision')!=revision(work):raise ValueError('The scenery library changed in another window. Reopen the structure editor.')
    row=validate(work,p.get('object',{}));ident=p.get('id');rows=copy.deepcopy(work.custom_objects)
    if ident is None:
        vacant=next((i for i,r in enumerate(rows) if r.get('deleted')),None)
        if vacant is None:
            if len(rows)>=LIMIT:raise ValueError('This map supports up to 100 custom scenery objects.')
            ident=len(work.base_objects)+len(rows);row['id']=ident;rows.append(row)
        else:
            ident=rows[vacant]['id'];row['id']=ident;rows[vacant]=row
    else:
        if type(ident)!=int:raise ValueError('Invalid object ID.')
        index=next((i for i,v in enumerate(rows) if v['id']==ident and not v.get('deleted')),None)
        if index is None:raise ValueError('Only custom copies can be edited.')
        row['id']=ident;rows[index]=row
    path=work.project/'WorkshopObjects.json'
    if path.exists():
        backup=work.project/'backups/scenery-library';backup.mkdir(parents=True,exist_ok=True);(backup/(str(time.time_ns())+'.json')).write_bytes(path.read_bytes())
    write_json(path,rows);work.custom_objects=rows;refresh(work)
    return dict(id=ident,message='Scenery saved to this map’s library. Place it from Scenery, then Save draft or export a complete map bundle.')

def deployment(work):
    import npc_creator
    root=copy.deepcopy(work.base_object_xml);wood=copy.deepcopy(work.woodcut_xml)
    for e in list(wood):
        if int(e.findtext('int'))>=len(work.base_objects):wood.remove(e)
    models=dict(work.object_models)
    for i in range(len(work.base_objects),len(work.base_objects)+LIMIT):models.pop(name_hash('wsobj_'+str(i)+'.ob3'),None)
    props={'start':len(root),'count':len(work.custom_objects)}
    for entry in work.custom_objects:
        row=validate(work,entry);ident=entry['id'];model='wsobj_'+str(ident)
        if entry.get('deleted'):row.update(name='Deleted structure',description='Deleted custom scenery.',solid=False,chop=False,width=1,height=1)
        source=work.base_object_xml[row['sourceId']];element=copy.deepcopy(source)
        fields=dict(name=row['name'],description=row['description'],width=row['width'],height=row['height'],type=1 if row['solid'] else 0,groundItemVar=0,objectModel=model,command1='Chop' if row['chop'] else 'WalkTo',command2='Examine')
        for field,value in fields.items():
            child=element.find(field)
            if child is None:child=ET.SubElement(element,field)
            child.text=str(value);props[str(ident)+'.'+field]=value
        root.append(element)
        models[name_hash(model+'.ob3')]=scaled_model(work.object_models[name_hash(source.findtext('objectModel')+'.ob3')],row['scale'])
        if row['chop']:
            extra=copy.deepcopy(work.woodcut_sources[row['sourceId']]);extra.find('int').text=str(ident);wood.append(extra)
    return dict(zip(LIVE,[xml_bytes(root),xml_bytes(wood),npc_creator.properties(props),archive_bytes(models),json.dumps(work.custom_objects).encode('utf-8')]))

def delete(work,p,write_json):
    """Keep other IDs stable; remove references across all map floors and stamps."""
    import zipfile
    if p.get('confirmed') is not True:raise ValueError('Confirm deletion first.')
    if p.get('projectKey')!=hashlib.sha256(str(work.project.resolve()).encode()).hexdigest():raise ValueError('The active map changed. Reopen the structure editor.')
    if p.get('revision')!=revision(work):raise ValueError('The scenery library changed. Reopen the structure editor.')
    disk_meta=json.loads(work.meta_path.read_text(encoding='utf-8'))
    if p.get('mapRevision')!=work.meta['revision'] or disk_meta['revision']!=work.meta['revision']:raise ValueError('The map changed in another window. Reload before deleting.')
    ident=p.get('id')
    if type(ident)!=int:raise ValueError('Choose a custom structure to delete.')
    rows=copy.deepcopy(work.custom_objects)
    row=next((r for r in rows if r['id']==ident and not r.get('deleted')),None)
    if row is None:raise ValueError('Only your custom structures can be deleted.')
    row['deleted']=True
    placements=[r for r in work.locs['objects'] if r['id']!=ident]
    prefab_path=work.project/'prefabs.json'
    prefabs=json.loads(prefab_path.read_text(encoding='utf-8')) if prefab_path.exists() else []
    for prefab in prefabs:prefab['objects']=[r for r in prefab.get('objects',[]) if r.get('id')!=ident]
    meta=copy.deepcopy(work.meta);meta.update(revision=meta['revision']+1,lastSaved=time.strftime('%Y-%m-%d %H:%M:%S'))
    updates={'WorkshopObjects.json':rows,'SceneryLocs.json':{'sceneries':placements},'project.json':meta}
    if prefab_path.exists():updates['prefabs.json']=prefabs
    previous={name:(work.project/name).read_bytes() if (work.project/name).exists() else None for name in updates}
    backup=work.project/'backups/scenery-library';backup.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(backup/('delete-'+str(time.time_ns())+'.zip'),'w',zipfile.ZIP_DEFLATED) as archive:
        for name,data in previous.items():
            if data is not None:archive.writestr(name,data)
    try:
        for name,data in updates.items():write_json(work.project/name,data)
    except Exception:
        for name,data in previous.items():
            if data is None:(work.project/name).unlink(missing_ok=True)
            else:(work.project/name).write_bytes(data)
        raise
    count=len(work.locs['objects'])-len(placements)
    work.custom_objects=rows;work.locs['objects']=placements;work.meta=meta;refresh(work)
    return {'id':ident,'removed':count,'message':'Structure deleted from the library, map and saved stamps. A backup was kept. Save draft or export to apply it to your game.'}
