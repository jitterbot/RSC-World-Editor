                                                                                   
import copy,hashlib,json,time,uuid
from pathlib import Path
LIVE='server/conf/server/defs/WorkshopQuests.json'

def load(work):
    path=work.project/'WorkshopQuests.json'
    return json.loads(path.read_text(encoding='utf-8')) if path.exists() else []

def revision(rows):return hashlib.sha256(json.dumps(rows,sort_keys=True).encode()).hexdigest()
def catalog(work):
    rows=load(work)
    path=work.project/'quest-drafts.json'
    drafts=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    return {'conversationVersion':1,'questOptionsVersion':1,'quests':rows,'revision':revision(rows),'drafts':drafts,'npcs':work.npc_defs,'items':work.item_defs}

def save_draft(work,p,write_json):
    if p.get('projectKey')!=hashlib.sha256(str(work.project.resolve()).encode()).hexdigest():raise ValueError('The active map changed. Return to your quest’s map.')
    q=p.get('quest')
    if not isinstance(q,dict) or len(json.dumps(q))>500000:raise ValueError('Quest recovery data is too large.')
    ident=q.get('id') or 'new'
    if ident!='new' and ident not in {v['id'] for v in load(work)}:raise ValueError('Unknown quest.')
    path=work.project/'quest-drafts.json';rows=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    rows[ident]=q;write_json(path,rows)
    return {'saved':True}

def integer(value,lo,hi,label):
    if type(value)!=int or not lo<=value<=hi:raise ValueError('Choose a valid '+label+'.')
    return value

def text(value,label,limit=500):
    if not isinstance(value,str) or not value.strip() or len(value)>limit or any(ord(c)<32 and c!='\n' for c in value):raise ValueError(label+' is required (up to '+str(limit)+' characters).')
    return value.strip()

def conversation(raw, action, label):
    if not isinstance(raw,dict):raise ValueError(label+': invalid conversation.')
    nodes=raw.get('nodes')
    if not isinstance(nodes,list) or not 1<=len(nodes)<=60:raise ValueError(label+': use 1 to 60 conversation parts.')
    result={'entry':text(raw.get('entry'),label+' opening part',80),'nodes':[]}
    ids=set()
    for node in nodes:
        if not isinstance(node,dict):raise ValueError(label+': invalid conversation part.')
        ident=text(node.get('id'),'Conversation part',80)
        if ident in ('end','start','advance'):raise ValueError(label+': reserved conversation part name.')
        if ident in ids:raise ValueError(label+': duplicate conversation part.')
        ids.add(ident)
        out={'id':ident,'title':text(node.get('title'),'Conversation part name',80),'lines':[],'choices':[]}
        lines=node.get('lines',[]);choices=node.get('choices',[])
        if not isinstance(lines,list) or len(lines)>80:raise ValueError(label+': use up to 80 speech cards per part.')
        for line in lines:
            if not isinstance(line,dict) or line.get('speaker') not in ('npc','player','chat'):raise ValueError(label+': choose NPC, player, or chat box.')
            out['lines'].append({'speaker':line['speaker'],'text':text(line.get('text'),'Dialogue text',2000)})
        if not isinstance(choices,list) or len(choices)>4:raise ValueError(label+': use up to 4 reply choices per part.')
        for c in choices:
            if not isinstance(c,dict):raise ValueError(label+': invalid reply choice.')
            out['choices'].append({'text':text(c.get('text'),'Player reply option',80),'next':text(c.get('next'),'Reply destination',80)})
        out['ending']=text(node.get('ending','end'),'Conversation ending',80)
        result['nodes'].append(out)
    if result['entry'] not in ids:raise ValueError(label+': the opening part is missing.')
    for node in result['nodes']:
        if any(c['next'] not in ids for c in node['choices']):raise ValueError(label+': a reply points to a missing conversation part.')
        if node['ending'] not in ('end',action) and node['ending'] not in ids:raise ValueError(label+': choose a valid ending.')
                                                                                         
    byid={n['id']:n for n in result['nodes']};seen=set();todo=[result['entry']];can_finish=False
    while todo:
        ident=todo.pop()
        if ident in seen:continue
        seen.add(ident);n=byid[ident]
        if n['choices']:todo.extend(c['next'] for c in n['choices'])
        elif n['ending'] in ids:todo.append(n['ending'])
        elif n['ending']==action:can_finish=True
    if not can_finish:raise ValueError(label+': add a reachable '+('Start quest' if action=='start' else 'Complete this step' if action=='advance' else 'End conversation')+' ending.')
    return result

def skill_rows(rows,field,maximum,label):
    if not isinstance(rows,list) or len(rows)>18:raise ValueError(label+': add up to 18 skills.')
    result=[];seen=set()
    for row in rows:
        if not isinstance(row,dict):raise ValueError(label+': invalid skill row.')
        skill=integer(row.get('skill'),0,17,label+' skill')
        if skill in seen:raise ValueError(label+': choose each skill only once.')
        seen.add(skill);result.append({'skill':skill,field:integer(row.get(field),1,maximum,label+' '+field)})
    return result

def guide_fields(value,label):
    if value is None:value={}
    if not isinstance(value,dict):raise ValueError(label+': invalid guide.')
    return {key:text(value[key],label+' '+key,1000) if value.get(key) else '' for key in ('next','where','bring','complete')}

def validate(work,q):
    if not isinstance(q,dict):raise ValueError('Invalid quest.')
    q=copy.deepcopy(q)
    if 'conversation' in q:q['intro']=q.get('intro') or 'Conversation';q['accept']=q.get('accept') or 'Yes'
    if 'finishConversation' in q:q['finish']=q.get('finish') or 'Thank you.'
    result={'name':text(q.get('name'),'Quest name',60),'intro':text(q.get('intro'),'Opening dialogue',2000),'accept':text(q.get('accept'),'Accept reply',80),'finish':text(q.get('finish'),'Completion dialogue',2000),'enabled':q.get('enabled') is True}
    def npc(raw):
        if raw not in work.npc_ids:raise ValueError('Choose an NPC from this map’s library.')
        return raw
    if 'conversation' in q:result['conversation']=conversation(q['conversation'],'start','Quest introduction')
    if 'finishConversation' in q:result['finishConversation']=conversation(q['finishConversation'],'end','Completion dialogue')
    result['giver']=npc(integer(q.get('giver'),0,99999,'quest giver'))
    result['location']=text(q['location'],'Quest area name',120) if q.get('location') else ''
    result['requirements']=skill_rows(q.get('requirements',[]),'level',99,'Level requirements')
    result['guide']=guide_fields(q.get('guide'),'Quest guide')
    steps=q.get('steps')
    if not isinstance(steps,list) or not 1<=len(steps)<=20:raise ValueError('Add between 1 and 20 quest steps.')
    result['steps']=[]
    for i,s in enumerate(steps):
        if s.get('kind') not in ('talk','bring','check','give'):raise ValueError('Choose a step type.')
        if 'conversation' in s:s['speech']=s.get('speech') or 'Conversation';s['reply']=s.get('reply') or 'Continue'
        step={'kind':s['kind'],'npc':npc(integer(s.get('npc'),0,99999,'step NPC')),'objective':text(s.get('objective'),'Step '+str(i+1)+' objective',160),'speech':text(s.get('speech'),'Step '+str(i+1)+' dialogue',2000),'reply':text(s.get('reply'),'Step '+str(i+1)+' player reply',80),'reminder':text(s.get('reminder'),'Step '+str(i+1)+' reminder',500)}
        if s['kind']!='talk':
            item=integer(s.get('item'),0,99999,'item')
            if str(item) not in work.art.items:raise ValueError('Choose an item from the picture picker.')
            step.update(item=item,amount=integer(s.get('amount'),1,1000000,'item quantity'))
        if 'conversation' in s:step['conversation']=conversation(s['conversation'],'advance','Step '+str(i+1))
        step['location']=text(s['location'],'Step area name',120) if s.get('location') else ''
        step['guide']=guide_fields(s.get('guide'),'Step '+str(i+1)+' guide')
        step['anyOrderWithPrevious']=s.get('anyOrderWithPrevious') is True
        if step['anyOrderWithPrevious'] and (i==0 or s['kind']!='bring' or steps[i-1]['kind']!='bring' or steps[i-1]['npc']!=s['npc']):raise ValueError('Any-order deliveries must follow another delivery to the same character.')
        result['steps'].append(step)
    reward=q.get('reward',{})
    result['reward']={'coins':integer(reward.get('coins',0),0,10000000,'coin reward'),'xp':integer(reward.get('xp',0),0,10000000,'experience reward'),'skill':integer(reward.get('skill',0),0,17,'reward skill')}
    experience=reward.get('experience')
    if experience is None:experience=[{'skill':result['reward']['skill'],'xp':result['reward']['xp']}] if result['reward']['xp'] else []
    result['reward']['experience']=skill_rows(experience,'xp',10000000,'Experience rewards')
    first=result['reward']['experience'][0] if result['reward']['experience'] else {'skill':0,'xp':0}
    result['reward'].update(first)
    item=reward.get('item',-1)
    integer(item,-1,99999,'reward item')
    if item>=0:
        if str(item) not in work.art.items:raise ValueError('Choose a valid reward item.')
        result['reward'].update(item=item,amount=integer(reward.get('amount'),1,1000000,'reward quantity'))
    else:result['reward'].update(item=-1,amount=0)
    return result

def save(work,p,write_json):
    if p.get('projectKey')!=hashlib.sha256(str(work.project.resolve()).encode()).hexdigest():raise ValueError('The map changed. Return to the map this quest belongs to before saving.')
    rows=load(work)
    if p.get('revision')!=revision(rows):raise ValueError('The quest library changed in another window. Your unfinished quest is still in recovery.')
    q=validate(work,p.get('quest'));ident=p['quest'].get('id')
    if ident:
        old=next((v for v in rows if v['id']==ident),None)
        if old is None:raise ValueError('That quest no longer exists.')
                                                                                       
        if old.get('published') and (old['giver']!=q['giver'] or [(s['kind'],s['npc'],s.get('item'),s.get('amount')) for s in old['steps']]!=[(s['kind'],s['npc'],s.get('item'),s.get('amount')) for s in q['steps']]):raise ValueError('This quest has been enabled. Duplicate it to change its steps; existing player progress will stay safe.')
        q.update(id=ident,published=old.get('published',False) or q['enabled']);rows[rows.index(old)]=q
    else:
        if len(rows)>=100:raise ValueError('This map supports up to 100 quests.')
        q.update(id=uuid.uuid4().hex,published=q['enabled']);rows.append(q)
    path=work.project/'WorkshopQuests.json'
    if path.exists():
        backup=work.project/'backups/quest-library';backup.mkdir(parents=True,exist_ok=True);(backup/(str(time.time_ns())+'.json')).write_bytes(path.read_bytes())
    write_json(path,rows)
    recovery=work.project/'quest-drafts.json'
    if recovery.exists():
        drafts=json.loads(recovery.read_text(encoding='utf-8'));drafts.pop(ident or 'new',None);write_json(recovery,drafts)
    return {'quest':q,'revision':revision(rows),'message':'Quest saved. Save draft to sync a linked game, or export the complete map bundle.'}

def deployment(work):return {LIVE:json.dumps({'quests':[q for q in load(work) if q.get('enabled')]}).encode('utf-8')}
