                                                                          
import copy, hashlib, json, time, re
import quest_editor
from pathlib import Path

LIVE = ['server/conf/server/defs/WorkshopNpcs.json',
        'server/conf/server/defs/WorkshopDialogue.properties',
        'Client_Base/Cache/WorkshopNpcs.properties']

def load(work):
    path=work.project/'WorkshopNpcs.json'
    work.custom_npcs=json.loads(path.read_text(encoding='utf-8')) if path.exists() else []
    work.base_npcs=copy.deepcopy(work.art.npcs)
    refresh(work)

def refresh(work):
    work.art.npcs=copy.deepcopy(work.base_npcs)
    work.npc_defs=[n for n in work.npc_defs if not n.get('custom')]
    for entry in work.custom_npcs:
        n=entry['definition'];work.art.npcs[n['id']]=n
        work.npc_defs.append(dict(id=n['id'],name=n['name']+' Â· Custom Â· #'+str(n['id']),gameName=n['name'],description=n['description'],level=0,attackable=False,aggressive=False,commands=[],custom=True,imageWidth=n['camera1']/128,imageHeight=n['camera2']/128))
    work.npc_ids=set(work.art.npcs)
    work.art.cache.clear()
    work.art.version=hashlib.sha256(json.dumps(work.art.npcs,sort_keys=True).encode()).hexdigest()[:16]

def catalog(work):
    if hasattr(work,'appearance_catalog'):return {**work.appearance_catalog,'custom':work.custom_npcs}
    source=(work.base/'Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java').read_text(encoding='utf-8')
    names={int(i):name for name,i in re.findall(r'animations.add\(new AnimationDef\("([^"]+)"[^\n]*?//\s*(\d+)\s*$',source,re.M)}
    slots=[]
    for index,label in enumerate(['Head','Shirt','Trousers','Hand gear 1','Hand gear 2','Hat / helmet','Body armour','Leg armour','Gloves','Boots','Neck / accessory','Cape'],1):
        choices={}
        for n in work.base_npcs.values():
            sprite=n.get('sprites'+str(index),-1)
            if sprite>=0 and sprite<len(work.art.animations):
                if index<=3 and work.art.animations[sprite][1]!=index:continue
                try:work.art.sprite(work.art.animations[sprite][0])
                except (KeyError,FileNotFoundError):continue
                choices.setdefault(sprite,n['name'])
        slots.append({'label':label,'choices':[{'id':-1,'name':'None'}]+[{'id':k,'name':names.get(k,'Style')+' Â· '+v+' Â· '+str(k)} for k,v in sorted(choices.items())]})
    allowed=[{v['id'] for v in slot['choices']} for slot in slots]
    items={}
    for filename in ('ItemDefs.json','ItemDefsCustom.json','ItemDefsPatch18.json'):
        path=work.base/'server/conf/server/defs'/filename
        if path.exists():
            for item in next(iter(json.loads(path.read_text(encoding='utf-8-sig')).values())):items[item['id']]=item
    equipment=[i for i in items.values() if i.get('isWearable')]
    sizes=[{'name':label,'npc':ident,'width':work.base_npcs[ident]['camera1'],'height':work.base_npcs[ident]['camera2']} for label,ident in [('Adult',11),('Wilough',781),('Shilop',715),('Gnome',399),('Dwarf',94),('Giant',61)]]
    work.appearance_catalog={'sizes':sizes,'slots':slots,'equipment':equipment,'templates':[n for n in work.base_npcs.values() if n.get('sprites1',-1)>=0 and n.get('sprites2',-1)>=0 and all(n.get('sprites'+str(i+1),-1) in choices for i,choices in enumerate(allowed))]}
    return {**work.appearance_catalog,'custom':work.custom_npcs}

def text(value,label,limit):
    if not isinstance(value,str) or not value.strip() or len(value)>limit or any(ord(c)<32 and c!='\n' for c in value):raise ValueError(label+' is required (maximum '+str(limit)+' characters).')
    return value.strip()

def dialogue_text(value):
    if not isinstance(value,str):raise ValueError('Add at least one NPC line.')
    lines=value.split('\n')
    if not 1<=len(lines)<=10:raise ValueError('Use at most ten NPC lines per page.')
    return '\n'.join(text(line,'NPC line '+str(i+1),500) for i,line in enumerate(lines))

def spoken_lines(node):
    raw=node.get('lines')
    if raw is None:raw=[{'speaker':'npc','text':v} for v in dialogue_text(node.get('text')).split('\n')]
    if not isinstance(raw,list) or not 1<=len(raw)<=10:raise ValueError('Add between 1 and 10 spoken lines per branch.')
    result=[]
    for line in raw:
        if not isinstance(line,dict) or line.get('speaker') not in ('npc','player'):raise ValueError('Choose NPC or player for each spoken line.')
        value=text(line.get('text'),'Spoken line',500)
        if '\n' in value:raise ValueError('Use Add response for a separate spoken line.')
        result.append({'speaker':line['speaker'],'text':value})
    return result

def conversation_nodes(nodes):
    if not isinstance(nodes,list) or not 1<=len(nodes)<=40:raise ValueError('Add between 1 and 40 dialogue pages.')
    clean=[];ids=set()
    for node in nodes:
        ident=node.get('id')
        if type(ident)!=int or not 0<=ident<=9999 or ident in ids:raise ValueError('Invalid dialogue page ID.')
        ids.add(ident);replies=node.get('replies',[])
        if not isinstance(replies,list) or len(replies)>4:raise ValueError('Use at most four replies per page.')
        lines=spoken_lines(node)
        clean.append({'id':ident,'lines':lines,'text':'\n'.join(l['text'] for l in lines),'replies':[{'text':text(r.get('text'),'Player reply',80),'next':r.get('next')} for r in replies]})
    for node in clean:
        for r in node['replies']:
            if type(r['next'])!=int or r['next'] not in ids|{-1}:raise ValueError('A reply leads to a missing dialogue page.')
    return clean

def validate(work,p):
    n=copy.deepcopy(work.base_npcs[11])
    raw=p.get('definition',{})
    n['name']=text(raw.get('name'),'Name',40);n['description']=text(raw.get('description'),'Description',160)
    for index,slot in enumerate(catalog(work)['slots'],1):
        value=raw.get('sprites'+str(index))
        if type(value)!=int or value not in {v['id'] for v in slot['choices']}:raise ValueError('Choose a valid '+slot['label'].lower()+'.')
        n['sprites'+str(index)]=value
    if any(n['sprites'+str(i)]<0 for i in (1,2,3)):raise ValueError('Choose a head, shirt and trousers.')
    for field in ('hairColour','skinColour','topColour','bottomColour'):
        value=raw.get(field)
        if type(value)!=int or not 0<=value<=0xffffff:raise ValueError('Invalid colour.')
        n[field]=value
    n.update(command='',command2='',attackable=0,aggressive=0,isMembers=0,combatlvl=0,attack=1,strength=1,defense=1,hits=10,ranged=False,walkModel=6,combatModel=6,combatSprite=5,respawnTime=30,roundMode=0)
    for dimension,default in [('camera1',145),('camera2',220)]:
        value=raw.get(dimension,default)
        if type(value)!=int or not 32<=value<=512:raise ValueError('Invalid character size.')
        n[dimension]=value
    clean=conversation_nodes(p.get('nodes'))
    required=p.get('requiresGhostspeak',False)
    if type(required)!=bool:raise ValueError('Choose whether this character requires Ghostspeak.')
    ghost_nodes=conversation_nodes(p.get('ghostNodes')) if required or p.get('ghostNodes') else []
    base_definition=copy.deepcopy(n)
    equipment=p.get('equipment',[])
    if not isinstance(equipment,list) or len(equipment)>14 or any(type(i)!=int for i in equipment) or len(set(equipment))!=len(equipment):raise ValueError('Invalid equipment selection.')
    items={i['id']:i for i in catalog(work)['equipment']}
    used=0;slots=set()
    for ident in equipment:
        if ident not in items:raise ValueError('Unknown equipment item.')
        item=items[ident];slot=item['wearSlot'];mask=item['wearableID']
        if slot in slots or used&mask:raise ValueError('Equipment overlaps another selected item.')
        slots.add(slot);used|=mask
        sprite=item['appearanceID']-1
        if sprite<0:continue
        if not 0<=sprite<len(work.art.animations):raise ValueError('Missing wearable artwork.')
        if slot>11:
                                                                             
            for j in range(1,13):n['sprites'+str(j)]=-1
            n['sprites1']=sprite
        else:
            for j in range(12):
                if mask&(1<<j):n['sprites'+str(j+1)]=-1
            n['sprites'+str(slot+1)]=sprite
    return {'definition':n,'baseDefinition':base_definition,'equipment':equipment,'nodes':clean,'requiresGhostspeak':required,'ghostNodes':ghost_nodes}

def save(work,p,write_json):
    if p.get('projectKey')!=hashlib.sha256(str(work.project.resolve()).encode()).hexdigest():raise ValueError('The active project changed. Reopen the NPC creator.')
    revision=hashlib.sha256(json.dumps(work.custom_npcs,sort_keys=True).encode()).hexdigest()
    if p.get('libraryRevision')!=revision:raise ValueError('The NPC library changed in another window. Reopen the creator.')
    entry=validate(work,p);ident=p.get('id');rows=copy.deepcopy(work.custom_npcs)
    if ident is None:
        if len(rows)>=100:raise ValueError('The custom NPC library is full (100 characters).')
        ident=max(work.base_npcs)+1+len(rows)
        entry['definition']['id']=ident;rows.append(entry)
    else:
        matches=[i for i,e in enumerate(rows) if e['definition']['id']==ident]
        if not matches:raise ValueError('Only your custom NPCs can be edited.')
        entry['definition']['id']=ident;rows[matches[0]]=entry
    path=work.project/'WorkshopNpcs.json'
    if path.exists():
        import shutil
        backup=work.project/'backups'/'npc-library';backup.mkdir(parents=True,exist_ok=True)
        shutil.copy2(path,backup/(str(time.time_ns())+'.json'))
    write_json(path,rows);work.custom_npcs=rows;refresh(work)
    return {'id':ident,'message':'NPC saved to this mapâ€™s library. Place it from People, then '+('Export a complete map bundle.' if work.meta.get('imported') else 'Save & Test.')}

def revision(work):return hashlib.sha256(json.dumps(work.custom_npcs,sort_keys=True).encode()).hexdigest()

def part_image(work,slot,sprite):
    if not 1<=slot<=12:raise ValueError('Unknown appearance slot.')
    choices=catalog(work)['slots'][slot-1]['choices']
    if sprite not in {c['id'] for c in choices}:raise ValueError('Unknown appearance part.')
    if not hasattr(work,'part_images'):work.part_images={}
    key=(slot,sprite)
    if key not in work.part_images:
        n=copy.deepcopy(work.base_npcs[11])
        for index in range(4,13):n['sprites'+str(index)]=-1
        n.update(hairColour=0x69432b,skinColour=0xecb98f,topColour=0x76939b,bottomColour=0x534c43)
        if slot==2 and sprite==4:n['sprites1']=3
        if slot==1 and sprite==3:n['sprites2']=4
        n['sprites'+str(slot)]=sprite
        ident=max(work.art.npcs)+1;work.art.npcs[ident]=n
        try:work.part_images[key]=work.art.image('npcs',ident)
        finally:work.art.npcs.pop(ident,None);work.art.cache.pop(('npcs',ident),None)
    return work.part_images[key]

def properties(rows):
    def escape(value):
        value=str(value);out=''
        for c in value:
            if c in '\\=:#! ':out+='\\'+c
            elif c=='\n':out+='\\n'
            elif ord(c)>126:
                encoded=c.encode('utf-16-be');out+=''.join('\\u'+encoded[i:i+2].hex() for i in range(0,len(encoded),2))
            else:out+=c
        return out
    return ''.join(str(k)+'='+escape(v)+'\n' for k,v in rows.items()).encode('ascii')

def deployment(work):
    defs=[e['definition'] for e in work.custom_npcs]
                                                                             
                                                                             
    client_defs=[work.base_npcs[i] for i in range(836,max(work.base_npcs)+1)]+defs
    client={'count':len(client_defs),'start':836};dialogue={}
    for n in client_defs:
        for key,value in n.items():client[str(n['id'])+'.'+key]=int(value) if isinstance(value,bool) else value
    for entry in work.custom_npcs:
        n=entry['definition'];prefix=str(n['id'])+'.'
        dialogue[prefix+'requiresGhostspeak']='true' if entry.get('requiresGhostspeak') else 'false'
        conversations=[(prefix,entry['nodes'])]
        if entry.get('requiresGhostspeak'):
            conversations.append((prefix+'ghost.',conversation_nodes(entry.get('ghostNodes'))))
        for conversation_prefix,nodes in conversations:
            dialogue[conversation_prefix+'start']=nodes[0]['id']
            for node in nodes:
                key=conversation_prefix+str(node['id'])+'.';dialogue[key+'text']=node['text'];dialogue[key+'count']=len(node['replies'])
                lines=spoken_lines(node);dialogue[key+'lineCount']=len(lines)
                for i,line in enumerate(lines):
                    dialogue[key+'line.'+str(i)+'.speaker']=line['speaker'];dialogue[key+'line.'+str(i)+'.text']=line['text']
                for i,r in enumerate(node['replies']):dialogue[key+str(i)+'.text']=r['text'];dialogue[key+str(i)+'.next']=r['next']
    return {**dict(zip(LIVE,[json.dumps({'npcs':defs}).encode(),properties(dialogue),properties(client)])),**quest_editor.deployment(work)}
