package com.openrsc.server.plugins;

import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.container.Inventory;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.InvocationTargetException;
import static com.openrsc.server.plugins.Functions.*;

                                                                                   
public final class WorkshopQuestTalk implements TalkNpcTrigger {
 private final List<Object> fallback;
 public WorkshopQuestTalk(List<Object> fallback){this.fallback=fallback;}
 public static List<JSONObject> quests(Player p,Npc n){
  List<JSONObject> result=new ArrayList<>();Path file=Paths.get(p.getConfig().CONFIG_DIR,"defs","WorkshopQuests.json");
  if(!Files.isRegularFile(file))return result;
  try{JSONArray rows=new JSONObject(new String(Files.readAllBytes(file),StandardCharsets.UTF_8)).getJSONArray("quests");
   for(int i=0;i<rows.length();i++){JSONObject q=rows.getJSONObject(i);if(!q.optBoolean("enabled",false))continue;boolean matches=q.getInt("giver")==n.getID();JSONArray steps=q.getJSONArray("steps");for(int j=0;j<steps.length();j++)matches|=steps.getJSONObject(j).getInt("npc")==n.getID();if(matches)result.add(q);}
  }catch(Exception e){throw new IllegalStateException("Cannot load Rsc World Editor quests",e);}return result;
 }
 public boolean blockTalkNpc(Player p,Npc n){return !quests(p,n).isEmpty();}
 private static String key(JSONObject q){return "wsq_"+q.getString("id");}
 private static int progress(Player p,JSONObject q){return p.getCache().hasKey(key(q))?p.getCache().getInt(key(q)):0;}
 private static void speak(Player p,Npc n,String text){npcsay(p,n,text.split("\\n"));}
 public void onTalkNpc(Player p,Npc n){
  if(WorkshopGhostspeak.blocked(p,n))return;
  List<JSONObject> rows=quests(p,n);int page=0;
  while(true){List<String> options=new ArrayList<>();int from=page*2,to=Math.min(from+2,rows.size());for(int i=from;i<to;i++)options.add(rows.get(i).getString("name")+(progress(p,rows.get(i))<0?" (complete)":""));
   boolean more=rows.size()>2;if(more)options.add("More quests");options.add("Something else");
   int selected=multi(p,n,options.toArray(new String[0]));if(selected<0)return;
   if(selected<to-from){run(p,n,rows.get(from+selected));return;}
   if(more&&selected==to-from){page=(to==rows.size()?0:page+1);continue;}
   for(Object original:fallback)try{TalkNpcTrigger.class.getMethod("onTalkNpc",Player.class,Npc.class).invoke(original,p,n);}catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException)throw (RuntimeException)e.getCause();throw new IllegalStateException(e.getCause());}catch(Exception e){throw new IllegalStateException(e);}return;
  }
 }
 private static boolean converse(Player p,Npc n,JSONObject owner,String field,String action){
  JSONObject conversation=owner.optJSONObject(field);if(conversation==null)return false;
  JSONArray nodes=conversation.getJSONArray("nodes");Map<String,JSONObject> parts=new HashMap<>();
  for(int i=0;i<nodes.length();i++){JSONObject part=nodes.getJSONObject(i);parts.put(part.getString("id"),part);}
  String next=conversation.getString("entry");
  for(int visit=0;visit<200;visit++){
   JSONObject part=parts.get(next);if(part==null)return false;
   JSONArray lines=part.getJSONArray("lines");
   for(int i=0;i<lines.length();i++){JSONObject line=lines.getJSONObject(i);String speaker=line.getString("speaker");
    for(String text:line.getString("text").split("\\n")){
     if(speaker.equals("player"))say(p,n,text);
     else if(speaker.equals("chat")){p.message(text);delay(3);}
     else npcsay(p,n,text);
    }
   }
   JSONArray choices=part.getJSONArray("choices");
   if(choices.length()>0){String[] labels=new String[choices.length()];for(int i=0;i<labels.length;i++)labels[i]=choices.getJSONObject(i).getString("text");
    int choice=multi(p,n,labels);if(choice<0||choice>=labels.length)return false;next=choices.getJSONObject(choice).getString("next");
   }else{next=part.getString("ending");if(next.equals(action))return true;if(next.equals("end"))return false;}
  }
  p.message("This conversation has looped too many times. Please speak to the character again.");return false;
 }
 private static void finish(Player p,Npc n,JSONObject q){if(q.has("finishConversation"))converse(p,n,q,"finishConversation","end");else speak(p,n,q.getString("finish"));}
 private static void run(Player p,Npc n,JSONObject q){
  if(WorkshopGhostspeak.blocked(p,n))return;
  int stage=progress(p,q);JSONArray steps=q.getJSONArray("steps");
  if(stage<0){finish(p,n,q);p.message("Quest complete: "+q.getString("name"));return;}
  if(stage==0){if(n.getID()!=q.getInt("giver")){p.message("This quest has not started. Speak to "+p.getWorld().getServer().getEntityHandler().getNpcDef(q.getInt("giver")).getName()+".");return;}
   if(q.has("conversation")){if(!converse(p,n,q,"conversation","start"))return;}else{
    speak(p,n,q.getString("intro"));if(multi(p,n,q.getString("accept"),"Not right now")!=0)return;}
   if(WorkshopGhostspeak.blocked(p,n))return;
   if(progress(p,q)!=0)return;p.getCache().set(key(q),1);p.message("Quest started: "+q.getString("name"));p.message(steps.getJSONObject(0).getString("objective"));return;}
  if(stage>steps.length()){p.message("This quest was changed. Please contact the world creator.");return;}
  JSONObject step=steps.getJSONObject(stage-1);p.message("Quest: "+q.getString("name")+" - Step "+stage+" of "+steps.length());p.message(step.getString("objective"));
  if(n.getID()!=step.getInt("npc")){speak(p,n,step.getString("reminder"));return;}
  Inventory inv=p.getCarriedItems().getInventory();String kind=step.getString("kind");int item=step.optInt("item",-1),amount=step.optInt("amount",0);
  if((kind.equals("bring")||kind.equals("check"))&&inv.countId(item,Optional.of(false))<amount){speak(p,n,step.getString("reminder"));return;}
  if(step.has("conversation")){if(!converse(p,n,step,"conversation","advance"))return;}else{
   speak(p,n,step.getString("speech"));if(multi(p,n,step.getString("reply"),"Not yet")!=0)return;}
                                                                              
  if(WorkshopGhostspeak.blocked(p,n))return;
  if(progress(p,q)!=stage)return;
  if((kind.equals("bring")||kind.equals("check"))&&inv.countId(item,Optional.of(false))<amount){p.message("You no longer have the required items.");return;}
  Map<Integer,Integer> amounts=new LinkedHashMap<>();if(kind.equals("give"))amounts.put(item,amount);
  boolean complete=stage==steps.length();JSONObject reward=q.getJSONObject("reward");
  if(complete){if(reward.getInt("coins")>0)amounts.merge(10,reward.getInt("coins"),Integer::sum);if(reward.optInt("item",-1)>=0)amounts.merge(reward.getInt("item"),reward.getInt("amount"),Integer::sum);}
  List<Item> gifts=new ArrayList<>();for(Map.Entry<Integer,Integer> e:amounts.entrySet())gifts.add(new Item(e.getKey(),e.getValue()));
  if(inv.getRequiredSlots(gifts)>Inventory.MAX_SIZE-inv.size()){p.message("Please make room in your inventory, then talk to me again. Your progress and items are unchanged.");return;}
  if(kind.equals("bring"))p.getCarriedItems().remove(new Item(item,amount));
  for(Map.Entry<Integer,Integer> e:amounts.entrySet())give(p,e.getKey(),e.getValue());
  p.getCache().set(key(q),complete?-1:stage+1);
  if(complete){if(reward.getInt("xp")>0)incStat(p,reward.getInt("skill"),reward.getInt("xp"),0);p.message("@gre@Quest complete: "+q.getString("name"));finish(p,n,q);}
  else{p.message("Step complete.");p.message(steps.getJSONObject(stage).getString("objective"));}
 }
}
