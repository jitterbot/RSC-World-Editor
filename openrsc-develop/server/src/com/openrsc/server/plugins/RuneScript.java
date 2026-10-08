package com.openrsc.server.plugins;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.event.rsc.PluginTask;
import com.openrsc.server.model.MenuOptionListener;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.Shop;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.npc.NpcInteraction;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.ScriptContext;
import com.openrsc.server.model.entity.update.Bubble;
import com.openrsc.server.model.entity.update.ChatMessage;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.Formulae;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RuneScript {
	   
                            
    
	private static final Logger LOGGER = LogManager.getLogger();

	                                                  
	final static int BIG_VAR_MAX = 268435455;
	                                                              
	final static int VAR_MAX = 127;
	                                   
	final static int VAR_MIN = 0;
	                                                    
	final static int FLOOR_OFFSET = 944;

	   
                                                                                     
                    
    
	public static void thinkbubble() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Item item = scriptContext.getInteractingInventory();
		if (item == null) return;

		final Bubble bubble = new Bubble(player, item.getCatalogId());
		Npc npc;
		if ((npc = scriptContext.getInteractingNpc()) != null) {
			                   
			player.face(npc);
		}
		player.getUpdateFlags().setActionBubble(bubble);
	}

	   
                               
                                                                   
    
	@Deprecated
	public static boolean ifmale() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return true;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return true;

		final boolean isMale = player.isMale();
		scriptContext.setExecutionFlag(isMale);
		return isMale;
	}

	   
                                                                             
    
	public static void nodefault() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		scriptContext.setShouldBlockDefault(true);
	}

	   
                                             
                                                                                                
                                                  
    
	public static void openshop(final Shop shop)
	{
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		player.setAccessingShop(shop);
		ActionSender.showShop(player, shop);
	}

	   
                                              
                                                                
                                                                                           
    
	@Deprecated
	public static int displaybalance() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return -1;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return -1;

		return player.getBank().countId(ItemId.COINS.id());
	}

	   
                                                                                    
    
	public static void delay() { delay(1); }

	   
                                                                                   
                                                  
    
	public static void delay(final int ticks) {
		final PluginTask pluginTask = PluginTask.getContextPluginTask();
		if (pluginTask == null)
			return;
		                                                                         
		pluginTask.pause(ticks);
	}

	   
                                                                                               
                   
                   
    
	public static void pause(final int mindelay, final int maxdelay) {
		final PluginTask pluginTask = PluginTask.getContextPluginTask();
		if (pluginTask == null)
			return;

		final int ticks = DataConversions.random(mindelay, maxdelay);
		pluginTask.pause(ticks);
	}

	   
                                                                                               
                                                                                   
                   
                   
    
	public static void modpause(final int mindelay, final int maxdelay) {
		final PluginTask pluginTask = PluginTask.getContextPluginTask();
		if (pluginTask == null)
			return;

		int ticks = DataConversions.random(mindelay, maxdelay);
		final int playerCount = pluginTask.getWorld().getPlayers().size();
		if (playerCount > 60) {
			ticks = (ticks*60)/playerCount;
		}
		pluginTask.pause(ticks);
	}

	   
                                  
                                                                
           
    
	public static boolean ifrandom(final int probability) {
		ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();

		final boolean isRandom = probability >= DataConversions.random(1, 255);
		scriptContext.setExecutionFlag(isRandom);
		return isRandom;
	}

	   
                                                                
                                                
    
	public static void jump(final Runnable function) {
		function.run();
		end();
	}

	   
                                                          
                                                              
                                                            
                                                             
                                        
    
	public static void fork() {
		       
	}

	   
                                                      
    
	public static void end() {
		end("Script ended");
	}

	public static void end(final String message) {
		throw new ScriptEndedException(message);
	}

	   
                              
                   
    
	public static void mes(final String... messages) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				final Npc npc = scriptContext.getInteractingNpc();
				if (npc != null) {
					if (npc.isRemoved()) {
						player.setBusy(false);
						return;
					}
				}
				player.message(message);
			}
		}
	}

	   
                                                                           
                                                                            
                                          
                                                                  
    
	public static void say(final String... messages) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Npc npc = scriptContext.getInteractingNpc();

		if (npc != null && !player.inCombat()) {
			NpcInteraction interaction = NpcInteraction.NPC_TALK_TO;
			NpcInteraction.setInteractions(npc, player, interaction);
		}
		for (final String message : messages) {
			if (Functions.deliverMessage(player, npc, message)) return;
			delay(Functions.normalizeTicks(Functions.calcDelay(message), player.getConfig().GAME_TICK));
		}
	}

	   
                                    
                                                              
                                                  
                                                                   
                                    
    
	public static int multi(final String... options) {
		return multi(true, options);
	}

	   
                                    
                                                                                      
                                                                                       
                                     
                                                              
                                                  
                                                                   
                                    
    
	public static int multi(final boolean sendToClient, final String... options) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return -1;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return -1;

		final Npc npc = scriptContext.getInteractingNpc();

		LOGGER.info("enter multi, " + PluginTask.getContextPluginTask().getDescriptor() + " tick " + PluginTask.getContextPluginTask().getWorld().getServer().getCurrentTick());
		final long start = System.currentTimeMillis();
		if (npc != null) {
			if (npc.isRemoved()) {
				player.resetMenuHandler();
				return -1;
			} else {
				if (!player.inCombat()) {
					NpcInteraction interaction = NpcInteraction.NPC_TALK_TO;
					NpcInteraction.setInteractions(npc, player, interaction);
				}
				npc.setMultiTimeout(start);
				                                                                                                           
				npc.setPlayerWantsNpc(false);
				                   
			}
		}
		                   
		player.setMenuHandler(new MenuOptionListener(options));
		ActionSender.sendMenu(player, options);

		while (!player.checkUnderAttack()) {
			                                                                                                                                                                            
			if (npc != null && (npc.getMultiTimeout() == -1 || npc.getMultiTimeout() > start)) {
				player.resetMenuHandler();
				return -1;
			}

			if (player.getOption() != -1) {
				if (npc != null && options[player.getOption()] != null) {
					if (sendToClient)
						say(options[player.getOption()]);
				}
				return player.getOption();
			} else if (Functions.multiMenuNeedsCancel(start, player, npc)) {
				player.resetMenuHandler();
				return -1;
			}

			delay();
		}
		player.releaseUnderAttack();
		return -1;

	}

	   
                                                                                   
                                                                     
    
	public static void changelevel(final int level) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		final int currentFloor = player.getY() / FLOOR_OFFSET;                                       
		final int newY = player.getY() + ((level-currentFloor)*FLOOR_OFFSET);

		if (newY < 0 || newY > FLOOR_OFFSET*4) {
			return;
		}

		player.teleport(player.getX(), newY);
	}

	   
                                              
    
	public static void changelevelup() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		final int newY = player.getY() + FLOOR_OFFSET;

		                                                   
		if (newY > FLOOR_OFFSET*4) {
			return;
		}

		player.teleport(player.getX(), newY);
	}

	   
                                                
    
	public static void changeleveldown() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		final int newY = Formulae.getNewY(player.getY(), false);

		                                                   
		if (newY < 0) {
			return;
		}

		player.teleport(player.getX(), newY);
	}

	   
                                                                   
                                                                    
                                                                          
                                        
                          
                         
                                                                  
                                                                                     
    
	@Deprecated
	public static boolean ifstatrandom(final int stat, final int baseProbability, final int topProbability) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final int statLevel = player.getLevel(stat);
		final int probability = (int)Math.floor(Functions.lerp(baseProbability, topProbability, (float)statLevel / 100.0f));
		final boolean isStatRandom = probability >= DataConversions.random(1, 255);
		scriptContext.setExecutionFlag(isStatRandom);
		return isStatRandom;
	}

	   
                                                                                            
                                              
   
                  
                 
                    
    
	public static void advancestat(final int skillId, final int baseXp, final int expPerLvl) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		player.incExp(skillId, player.getSkills().getMaxStat(skillId) * expPerLvl + baseXp, true);
	}

	   
                                                                                   
                                                   
                                                
                                                           
    
	public static void addstat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		Functions.addstat(player, statId, constant, percent);
	}

	   
                                                                                          
                                                   
                                                
                                                           
    
	public static void substat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		Functions.substat(player, statId, constant, percent);
	}

	   
                                                                                   
                                                           
                                                   
                                                
                                                           
    
	public static void healstat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		Functions.healstat(player, statId, constant, percent);
	}

	   
                                                                   
                                   
                                                                           
    
	public static boolean ifstatup(final int statId) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isStatUp = Functions.isstatup(player, statId);
		scriptContext.setExecutionFlag(isStatUp);
		return isStatUp;
	}

	   
                                                                   
                                   
                                                                           
    
	public static boolean ifstatdown(final int statId) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isStatDown = Functions.isstatdown(player, statId);
		scriptContext.setExecutionFlag(isStatDown);
		return isStatDown;
	}

	   
                                                                                          
                                   
                                                             
                                                                          
    
	public static boolean ifstatabove(final int statId, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isStatAbove = player.getSkills().getLevel(statId) > value;
		scriptContext.setExecutionFlag(isStatAbove);
		return isStatAbove;
	}

	   
                                                                                  
                                   
                                                    
                                                      
                                                                                   
    
	public static boolean ifstatatleast(final int statId, final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isStatAtLeast = player.getSkills().getLevel(statId) >= player.getCache().getInt(variable) + value;
		scriptContext.setExecutionFlag(isStatAtLeast);
		return isStatAtLeast;
	}

	   
                                                          
                                                                      
    
	public static void giveqp(final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		player.incQuestPoints(value);
	}

	   
                                                                                     
                                                            
                                                                                                 
    
	public static boolean ifqp(int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isQp = player.getQuestPoints() >= value;
		scriptContext.setExecutionFlag(isQp);
		return isQp;
	}

	   
                                                                                  
                                                
                                                               
                                                                                 
    
	public static boolean ifvar(String variable, int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isVar = player.getCache().getInt(variable) == value;
		scriptContext.setExecutionFlag(isVar);
		return isVar;
	}

	   
                                                                                         
                                                
                                                               
                                                                                     
    
	public static boolean ifvarmore(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isVarMore = player.getCache().getInt(variable) > value;
		scriptContext.setExecutionFlag(isVarMore);
		return isVarMore;
	}

	   
                                                                                      
                                                
                                                               
                                                                                  
    
	public static boolean ifvarless(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isVarLess = player.getCache().getInt(variable) < value;
		scriptContext.setExecutionFlag(isVarLess);
		return isVarLess;
	}

	   
                                             
                                            
                                                          
    
	public static void setvar(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		if (value < VAR_MIN || value > VAR_MAX) {
			throw new IllegalArgumentException(String.format("Value must be %d-%d", VAR_MIN, VAR_MAX));
		}

		player.getCache().set(variable, value);
	}

	   
                                                      
                                                    
                                            
                                  
    
	public static void addvar(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		int newValue = player.getCache().getInt(variable) + value;

		if (newValue < VAR_MIN || newValue > VAR_MAX) {
			throw new IllegalArgumentException("Provided value will put var out of range");
		}

		player.getCache().set(variable, newValue);
	}

	   
                                                             
                                                    
                                            
                                       
    
	public static void subvar(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		int newValue = player.getCache().getInt(variable) - value;

		if (newValue < VAR_MIN || newValue > VAR_MAX) {
			throw new IllegalArgumentException("Provided value will put var out of range");
		}

		player.getCache().set(variable, newValue);
	}

	   
                                                                        
                
    
	public static void randomvar(final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		if (value > (VAR_MIN+1)) {
			throw new IllegalArgumentException("Value cannot be greater than " + (VAR_MIN)+1);
		}

		player.getCache().set("random", DataConversions.random(0, value-1));
	}

	   
                                                          
                                                              
                                            
                                  
    
	public static void addbigvar(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		int newValue = player.getCache().getInt(variable) + value;

		if (newValue < VAR_MIN || newValue > BIG_VAR_MAX) {
			throw new IllegalArgumentException("Provided value will put var out of range");
		}

		player.getCache().set(variable, newValue);
	}

	   
                                                                 
                                                              
                                            
                                       
    
	public static void subbigvar(final String variable, final int value) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		int newValue = player.getCache().getInt(variable) - value;

		if (newValue < VAR_MIN || newValue > BIG_VAR_MAX) {
			throw new IllegalArgumentException("Provided value will put var out of range");
		}

		player.getCache().set(variable, newValue);
	}

	   
                                                                                             
                                                    
                                                                   
                                                                                         
    
	public static boolean ifbigvarmore(String variable, int value) {
		return ifvarmore(variable, value);
	}

	   
                                                      
                                                                   
    
	public static void setcoord(final Point coordinate) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		scriptContext.setInteractingCoordinate(coordinate);
	}

	   
                                                               
    
	public static void playercoord() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		scriptContext.setInteractingCoordinate(player.getLocation());
	}

	   
                                                       
                                                             
                                                     
                                                             
                                                                                       
    
	public static void addobject(final int object, final int count, final int time) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Point interactingCoordinate = scriptContext.getInteractingCoordinate();
		if (interactingCoordinate == null) return;

		player.getWorld().registerItem(
			new GroundItem(player.getWorld(), object, interactingCoordinate.getX(), interactingCoordinate.getY(), count, player),
			player.getConfig().GAME_TICK * time);
	}

	   
                                                      
                                         
    
	public static void addnpc(final int npc) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Point interactingCoordinate = scriptContext.getInteractingCoordinate();
		if (interactingCoordinate == null) return;

		final Npc newNpc = new Npc(player.getWorld(), npc, interactingCoordinate.getX(), interactingCoordinate.getY());
		newNpc.setShouldRespawn(false);
		player.getWorld().registerNpc(newNpc);
	}

	   
                                                         
                                                                        
    
	public static void addloc(final int location) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Point interactingCoordinate = scriptContext.getInteractingCoordinate();
		if (interactingCoordinate == null) return;

		final GameObject obj = new GameObject(player.getWorld(), interactingCoordinate, location, 0, 0);
		obj.getWorld().registerGameObject(obj);
	}

	   
                                                                       
                                                                                             
    
	public static boolean ifblocked() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;
		final Point interactingCoordinate = scriptContext.getInteractingCoordinate();
		if (interactingCoordinate == null) return false;

		final boolean isBlocked = (player.getWorld().getTile(interactingCoordinate).traversalMask & 64) != 0;
		scriptContext.setExecutionFlag(isBlocked);
		return isBlocked;
	}

	   
                                                 
    
	public static void teleport() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Point interactingCoordinate = scriptContext.getInteractingCoordinate();
		if (interactingCoordinate == null) return;

		player.teleport(interactingCoordinate.getX(), interactingCoordinate.getY());
	}

	   
                                                                                       
                                              
    
	public static void showeffect(final int type) {
		       
	}

	   
                                                                                
                                    
                                       
    
	public static void give(final int object, final int count) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		final Item item = new Item(object, count);
		if (!item.getDef(player.getWorld()).isStackable() && count > 1) {
			for (int i = 0; i < count; i++) {
				player.getCarriedItems().getInventory().add(new Item(object, 1));
			}
		} else {
			player.getCarriedItems().getInventory().add(item);
		}
	}

	   
                                                                                
                            
                                    
                                       
    
	public static void remove(final int object, final int count) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;

		final Item itemToRemove = new Item(object, count);

		                         
		if (itemToRemove.getDef(player.getWorld()).isStackable()) {
			player.getCarriedItems().remove(itemToRemove);
		}

		                             
		else {
			for (int i = 0; i < count; i++) {
				player.getCarriedItems().remove(new Item(object));
			}
		}
	}

	   
                                                         
                                    
                                                                              
    
	public static boolean ifworn(final int object) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isWorn = player.getCarriedItems().getEquipment().hasCatalogID(object);
		scriptContext.setExecutionFlag(isWorn);
		return isWorn;
	}

	   
                                                        
                                    
                                       
                                                                              
    
	public static boolean ifheld(final int object, final int count) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		final boolean isHeld = player.getCarriedItems().getInventory().countId(object) >= count;
		scriptContext.setExecutionFlag(isHeld);
		return isHeld;
	}

	   
                                                                                
                                                                
                                                                                   
                              
    
	public static void sellinv(final int percentage) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Item interactingItem = scriptContext.getInteractingInventory();
		if (interactingItem == null) return;

		final int value = interactingItem.getDef(player.getWorld()).getDefaultPrice();
		final int amount = interactingItem.getAmount();

		remove(interactingItem.getCatalogId(), amount);
		give(ItemId.COINS.id(), amount*(value*(percentage/100)));
	}

	   
                                                                   
    
	public static void delinv() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Item interactingItem = scriptContext.getInteractingInventory();
		if (interactingItem == null) return;

		remove(interactingItem.getCatalogId(), interactingItem.getAmount());
	}

	   
                                                                         
                                                                                  
                    
    
	public static boolean ifobjectvisible() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;
		final GroundItem interactingGroundItem = scriptContext.getInteractingGroundItem();
		if (interactingGroundItem == null) return false;


		final boolean isObjectVisible = player.getViewArea().getVisibleGroundItem(interactingGroundItem.getID(), interactingGroundItem.getLocation(), player) != null
			&& player.canReach(interactingGroundItem);
		scriptContext.setExecutionFlag(isObjectVisible);
		return isObjectVisible;
	}

	   
                                                                             
    
	public static void takeobject() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final GroundItem interactingGroundItem = scriptContext.getInteractingGroundItem();
		if (interactingGroundItem == null) return;

		                                  
		delobject();

		                              
		give(interactingGroundItem.getID(), interactingGroundItem.getAmount());
	}

	   
                                     
    
	public static void delobject() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final GroundItem interactingGroundItem = scriptContext.getInteractingGroundItem();
		if (interactingGroundItem == null) return;

		player.getViewArea().getVisibleGroundItem(interactingGroundItem.getID(),
			interactingGroundItem.getLocation(), player).remove();
	}

	   
                                                           
                                              
    
	public static void changeloc(final int location) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final GameObject interactingLocation = scriptContext.getInteractingLocation();
		if (interactingLocation == null) return;

		                          
		delloc();

		                  
		final GameObject obj = new GameObject(
			interactingLocation.getWorld(),
			interactingLocation.getLocation(),
			location,
			0,
			0);
		obj.getWorld().registerGameObject(obj);
	}

	   
                                                                        
                             
    
	public static void upstairs() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final GameObject stairs = scriptContext.getInteractingLocation();
		if (stairs == null) return;

		final int newY = player.getY() + FLOOR_OFFSET;

		                                                   
		if (newY > FLOOR_OFFSET*4) {
			return;
		}

		int[] coords = {stairs.getX(), newY};
		switch (stairs.getDirection()) {
			case 0:
				coords[1] += (stairs.getGameObjectDef().getHeight());
				break;
			case 2:
				coords[0] += (stairs.getGameObjectDef().getHeight());
				break;
			case 4:
				coords[1] += (-1);
				break;
			case 6:
				coords[0] += (-1);
				break;
		}

		player.teleport(coords[0], coords[1]);
	}

	   
                                                                          
                             
    
	public static void downstairs() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final GameObject stairs = scriptContext.getInteractingLocation();
		if (stairs == null) return;

		final int newY = player.getY() - FLOOR_OFFSET;

		                                                        
		if (newY < 0) {
			return;
		}

		int[] coords = {stairs.getX(), newY};
		switch (stairs.getDirection()) {
			case 0:
				coords[1] += (-1);
				break;
			case 2:
				coords[0] += (-1);
				break;
			case 4:
				coords[1] += (stairs.getGameObjectDef().getHeight());
				break;
			case 6:
				coords[0] += (stairs.getGameObjectDef().getHeight());
				break;
		}

		player.teleport(coords[0], coords[1]);
	}

	   
                                       
    
	public static void delloc() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final GameObject interactingLocation = scriptContext.getInteractingLocation();
		if (interactingLocation == null) return;

		interactingLocation.getWorld().unregisterGameObject(interactingLocation);
	}

	   
                                                           
                                                                         
    
	public static void changebound(final int boundary) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final GameObject interactingBoundary = scriptContext.getInteractingBoundary();
		if (interactingBoundary == null) return;

		                          
		interactingBoundary.getWorld().unregisterGameObject(interactingBoundary);

		                                                         
		final GameObject newBoundary = new GameObject(interactingBoundary.getWorld(),
			interactingBoundary.getLocation(),
			boundary,
			interactingBoundary.getDirection(),
			interactingBoundary.getType());
		newBoundary.getWorld().registerGameObject(newBoundary);
	}

	   
                                                               
    
	public static void boundaryteleport() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final GameObject interactingBoundary = scriptContext.getInteractingBoundary();
		if (interactingBoundary == null) return;

		switch (interactingBoundary.getDirection()) {
			case 0:
				if (interactingBoundary.getLocation().equals(player.getLocation())) {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY() - 1);
				} else {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY());
				}
				break;
			case 1:
				if (interactingBoundary.getLocation().equals(player.getLocation())) {
					player.teleport(interactingBoundary.getX() - 1, interactingBoundary.getY());
				} else {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY());
				}
				break;
			case 2:
				        
				if (interactingBoundary.getX() == player.getX() && interactingBoundary.getY() == player.getY() + 1) {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY() + 1);
				} else if (interactingBoundary.getX() == player.getX() - 1 && interactingBoundary.getY() == player.getY()) {
					player.teleport(interactingBoundary.getX() - 1, interactingBoundary.getY());
				}

				       
				else if (interactingBoundary.getX() == player.getX() && interactingBoundary.getY() == player.getY() - 1) {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY() - 1);
				} else if (interactingBoundary.getX() == player.getX() + 1 && interactingBoundary.getY() == player.getY()) {
					player.teleport(interactingBoundary.getX() + 1, interactingBoundary.getY());
				}
				break;
			case 3:
				        
				if (interactingBoundary.getX() == player.getX() && interactingBoundary.getY() == player.getY() - 1) {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY() - 1);
				} else if (interactingBoundary.getX() == player.getX() + 1 && interactingBoundary.getY() == player.getY()) {
					player.teleport(interactingBoundary.getX() + 1, interactingBoundary.getY());
				}

				       
				else if (interactingBoundary.getX() == player.getX() && interactingBoundary.getY() == player.getY() + 1) {
					player.teleport(interactingBoundary.getX(), interactingBoundary.getY() + 1);
				} else if (interactingBoundary.getX() == player.getX() - 1 && interactingBoundary.getY() == player.getY()) {
					player.teleport(interactingBoundary.getX() - 1, interactingBoundary.getY());
				}
				break;
		}
	}

	   
                                                       
                                                                          
                                        
                                            
                                                                         
    
	public static boolean ifnearnpc(final int npc) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		                           
		final Iterable<Npc> npcsInView = player.getViewArea().getNpcsInView();

		for (final Npc npcInView : npcsInView) {
			                                                            
			boolean isNpc = npcInView.getID() == npc;
			boolean npcIsBusy = npcInView.isBusy();
			if (isNpc && !npcIsBusy) {
				scriptContext.setInteractingNpc(npcInView);
				scriptContext.setExecutionFlag(true);
				return true;
			}
		}

		scriptContext.setExecutionFlag(false);
		return false;
	}

	   
                                                                              
                                                              
                                                                          
                                        
                                            
                                                                         
    
	public static boolean ifnearvisnpc(final int npc) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;

		                           
		final Iterable<Npc> npcsInView = player.getViewArea().getNpcsInView();

		for (final Npc npcInView : npcsInView) {
			                                                                        
			                                 
			boolean isNpc = npcInView.getID() == npc;
			boolean isInRange = npcInView.withinRange(player, 8);
			boolean isReachable = player.canReach(npcInView);
			boolean npcIsBusy = npcInView.isBusy();
			if (isNpc && isInRange && isReachable && !npcIsBusy) {
				scriptContext.setInteractingNpc(npcInView);
				scriptContext.setExecutionFlag(true);
				return true;
			}
		}

		scriptContext.setExecutionFlag(false);
		return false;
	}

	   
                                                                                  
                                                                                
                  
                                                        
    
	public static void npcsay(final String... messages) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Npc npc = scriptContext.getInteractingNpc();

		if (npc != null && !player.inCombat()) {
			NpcInteraction interaction = NpcInteraction.NPC_TALK_TO;
			NpcInteraction.setInteractions(npc, player, interaction);
		}

		                                          
		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				if (npc != null) {
					if (npc.isRemoved()) {
						return;
					}
					npc.getUpdateFlags().setChatMessage(new ChatMessage(npc, message, player));
				}
			}

			delay(Functions.normalizeTicks(Functions.calcDelay(message), player.getConfig().GAME_TICK));
		}
	}

	   
                                                           
                                                                     
    
	public static void npcbusy() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		npc.setBusy(true);
	}

	   
                                                               
                                                                     
    
	public static void npcunbusy() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		npc.setBusy(true);
	}

	   
                                  
                     
    
	public static void shootnpc(final int projectile) {
		       
	}

	   
                                                  
    
	public static void npcattack() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		npc.startCombat(player);
	}

	   
                                                       
                                                                               
    
	public static boolean ifnpcvisible() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return false;

		final boolean isNpcVisible = player.canReach(npc);
		scriptContext.setExecutionFlag(isNpcVisible);
		return isNpcVisible;
	}

	   
                                                                                
                                                   
                                                
                                                           
    
	public static void addnpcstat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		final int currentLevel = npc.getSkills().getLevel(statId);
		final int newLevel = currentLevel + (int)(constant + (currentLevel * percent) / 100.0);
		npc.getSkills().setLevel(statId, newLevel);
	}

	   
                                                                                       
                                                   
                                                
                                                                
    
	public static void subnpcstat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		final int currentLevel = npc.getSkills().getLevel(statId);
		final int newLevel = currentLevel - (int)(constant + (currentLevel * percent) / 100.0);
		npc.getSkills().setLevel(statId, newLevel);
	}

	   
                                                                                
                                                        
                                                   
                                                
                                                           
    
	public static void healnpcstat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		final int currentLevel = npc.getSkills().getLevel(statId);
		final int newLevel = currentLevel + (int)(constant + (currentLevel * percent) / 100.0);
		npc.getSkills().setLevel(statId,
			Math.min(newLevel, npc.getSkills().getMaxStat(statId)));
	}

	   
                                                                
                                   
                                                                           
    
	public static boolean ifnpcstatup(final int statId) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return false;

		final boolean isNpcStatUp = npc.getSkills().getLevel(statId) > npc.getSkills().getMaxStat(statId);
		scriptContext.setExecutionFlag(isNpcStatUp);
		return isNpcStatUp;
	}

	   
                                                                
                                   
                                                                           
    
	public static boolean ifnpcstatdown(final int statId) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return false;

		final boolean isNpcStatDown = npc.getSkills().getLevel(statId) < npc.getSkills().getMaxStat(statId);
		scriptContext.setExecutionFlag(isNpcStatDown);
		return isNpcStatDown;
	}

	   
                                  
    
	public static void delnpc() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		npc.setShouldRespawn(false);
		npc.remove();
	}

	   
                                                 
                                    
    
	public static void changenpc(final int npc) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc oldNpc = scriptContext.getInteractingNpc();
		if (oldNpc == null) return;

		                     
		delnpc();

		                
		final Npc newNpc = new Npc(oldNpc.getWorld(), npc, oldNpc.getX(), oldNpc.getY());
		newNpc.setShouldRespawn(false);
		oldNpc.getWorld().registerNpc(newNpc);
	}

	   
                                                                                        
                                                          
    
	public static void npcretreat(int time) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Npc npc = scriptContext.getInteractingNpc();
		if (npc == null) return;

		npc.getBehavior().retreat(time);
	}

	   
                                                                                          
                                                   
                                                
                                                           
    
	public static void addplaystat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getInteractingPlayer();
		if (player == null) return;

		final int currentLevel = player.getSkills().getLevel(statId);
		final int newLevel = currentLevel + (int)(constant + (currentLevel * percent) / 100.0);
		player.getSkills().setLevel(statId, newLevel);
	}

	   
                                                                                                 
                                                   
                                                
                                                                
    
	public static void subplaystat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getInteractingPlayer();
		if (player == null) return;

		final int currentLevel = player.getSkills().getLevel(statId);
		final int newLevel = currentLevel - (int)(constant + (currentLevel * percent) / 100.0);
		player.getSkills().setLevel(statId, newLevel);
	}

	   
                                                                                          
                                                           
                                                   
                                                
                                                           
    
	public static void healplaystat(final int statId, final int constant, final int percent) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getInteractingPlayer();
		if (player == null) return;

		final int currentLevel = player.getSkills().getLevel(statId);
		final int newLevel = currentLevel + (int)(constant + (currentLevel * percent) / 100.0);
		player.getSkills().setLevel(statId,
			Math.min(newLevel, player.getSkills().getMaxStat(statId)));
	}

	   
                                                                          
                                   
                                                                           
    
	public static boolean ifplaystatup(final int statId) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getInteractingPlayer();
		if (player == null) return false;

		final boolean isPlayerStatUp = player.getSkills().getLevel(statId) > player.getSkills().getMaxStat(statId);
		scriptContext.setExecutionFlag(isPlayerStatUp);
		return isPlayerStatUp;
	}

	   
                                                                          
                                   
                                                                           
    
	public static boolean ifplaystatdown(final int statId) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getInteractingPlayer();
		if (player == null) return false;

		final boolean isPlayerStatDown = player.getSkills().getLevel(statId) < player.getSkills().getMaxStat(statId);
		scriptContext.setExecutionFlag(isPlayerStatDown);
		return isPlayerStatDown;
	}

	   
                                                    
                                                                     
    
	public static void omes(final String... messages) {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return;
		final Player player = scriptContext.getInteractingPlayer();
		if (player == null) return;

		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				player.message(message);
			}
		}
	}

	   
                                                                        
                                                                                         
    
	public static boolean ifplayervisible() {
		final ScriptContext scriptContext = PluginTask.getContextPluginTask().getScriptContext();
		if (scriptContext == null) return false;
		final Player player = scriptContext.getContextPlayer();
		if (player == null) return false;
		final Player interactingPlayer = scriptContext.getInteractingPlayer();
		if (interactingPlayer == null) return false;

		final boolean isPlayerVisable = player.canReach(interactingPlayer);
		scriptContext.setExecutionFlag(isPlayerVisable);
		return isPlayerVisable;
	}

	   
                                           
                     
    
	public static void shootplayer(final int projectile) {
		       
	}
}
