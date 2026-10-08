package com.openrsc.server.content.achievement;

import com.openrsc.server.Server;
import com.openrsc.server.content.achievement.Achievement.TaskType;
import com.openrsc.server.database.GameDatabaseException;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.Entity;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;

public class AchievementSystem {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final int ACHIEVEMENT_COMPLETED = 2;
	private static final int ACHIEVEMENT_STARTED = 1;

	private LinkedList<Achievement> loadedAchievements = new LinkedList<Achievement>();

	private final Server server;

	public AchievementSystem(final Server server) {
		this.server = server;
	}

	public void load() {
		try {
			loadedAchievements = getServer().getDatabase().getAchievements();
		} catch (GameDatabaseException e) {
			LOGGER.catching(e);
		}
	}

	public void unload() {
		loadedAchievements.clear();
	}

	public LinkedList<Achievement> getAchievements() {
		return loadedAchievements;
	}

	public ArrayList<Achievement> getAvailableQuestsForEntity(final Player player, final Entity e) {
		final ArrayList<Achievement> tasksAvailable = new ArrayList<Achievement>();
		for (final Achievement task : loadedAchievements) {
			if (e.getID() == task.getStartID()) {
				if (!isPlayerCanStartQuest(player, task)) {
					continue;
				}
				switch (task.getAchievementType()) {
					case TALK_TO_NPC:
						if (e.isNpc()) {
							if (!tasksAvailable.contains(task)) {
								tasksAvailable.add(task);
							}
						}
						break;
					case PICK_UP_ITEM:
						if (e instanceof GroundItem && task.getStartID() == e.getID()
							&& isPlayerCanStartQuest(player, task)) {
							if (!tasksAvailable.contains(task)) {
								tasksAvailable.add(task);
							}
						}
						break;
					case USE_OBJECT:
						if (e instanceof GameObject && task.getStartID() == e.getID()
							&& isPlayerCanStartQuest(player, task)) {
							if (!tasksAvailable.contains(task)) {
								tasksAvailable.add(task);
							}
						}
						break;
				}
			}
		}
		return tasksAvailable;
	}

	public boolean isPlayerCanStartQuest(final Player player, final Achievement task) {
		if (playerCompletedQuest(player, task)) {
			return false;
		}
		return true;
	}

	                                                                              
                                      
                                                                        
                                                         
                     
                                                 
    
                                             
                                        
                    
                                                                 
            
                                                    
     
                                                    
                                                   
                                                      
      
     

                    
                                  
                                 

                                                                                       
                                                                    
                                 
               
      
                                           
                                                     
                                       
                                                                                           
                
                             
                                                                                       
        
         
                                                                  
                           
                                                                                              
                  
                               
                                                                                       
          
           
      
     

                                   
                                  

                                 
                                                                   
                           
                             
                                                                                           
                                            
             
      
     
                                
                                                                         
                          
                            
                                                                                         
                                            
             
      
     
                         
                                                                                                      
                                                                             
                                                            
     
                        
                                                                                                  
                                                                          
                                                          
     
                                  
                                                         
                                 
                                                            
                                                                          
                                                                                    
                                                                               
                                                                     
                         
                                                       
        
                                                             
                                                      
       
      
     
                                                   
                                                   
                                                                                               
     
                                                                                       
           
                    
                                                                    
            
                                                           
     
                                                                          
    
   
    

	public String getTaskProgressText(final Player player, final int id) {
		String questInfo = "Task Progress: %";

		final Achievement quest = loadedAchievements.get(id);
		if (quest == null) {
			return "Quest not found";
		}
		for (AchievementTask task : quest.getTasks()) {
			String taskHeader = "";
			if (task.getTask() == TaskType.KILL_NPC) {
				taskHeader = "Slay " + task.getAmount() + " of monster " + player.getWorld().getServer().getEntityHandler().getNpcDef(task.getId()).name
					+ ": ";
			} else if (task.getTask() == TaskType.GATHER_ITEM) {
				taskHeader = "Gather " + task.getAmount() + " of item "
					+ player.getWorld().getServer().getEntityHandler().getItemDef(task.getId()).getName() + ": ";
			} else if (task.getTask() == TaskType.DO_QUEST) {
				taskHeader = "Complete Quest " + player.getWorld().getQuest(task.getId()).getQuestName() + ": ";
			}
			int taskProgress = getTaskProgress(player, task);
			questInfo += (taskProgress == task.getAmount() ? "@gre@" : "@red@") + taskHeader + " "
				+ getTaskProgress(player, task) + "/" + task.getAmount() + "@whi@ %";
		}
		return questInfo;
	}

	public int getTaskProgress(final Player player, final AchievementTask task) {
		if (task.getTask() == TaskType.DO_QUEST) {
			return player.getQuestStage(task.getId()) == -1 ? 1 : 0;
		}
		if (!player.getCache().hasKey("simpletask[" + task.getId() + "]_task_" + task.getTask().toString())) {
			return 0;
		}
		return player.getCache().getInt("simpletask[" + task.getId() + "]_task_" + task.getTask().toString());
	}

	public void setQuestStage(final Player player, final int questID, final int stage) {
		player.getCache().set("simpletask[" + questID + "]_stage", (int) stage);
	}

	public void setQuestStage(final Player player, final Achievement quest, final int stage) {
		player.getCache().set("simpletask[" + quest.getId() + "]_stage", (int) stage);
	}

	public boolean playerCompletedQuest(final Player player, final Achievement quest) {
		if (player.getCache().hasKey("simpletask[" + quest.getId() + "]_stage")) {
			return player.getCache().getInt("simpletask[" + quest.getId() + "]_stage") == ACHIEVEMENT_COMPLETED;
		}
		return false;
	}

	public boolean playerCompletedQuest(final Player player,final  int id) {
		if (player.getCache().hasKey("simpletask[" + id + "]_stage")) {
			return player.getCache().getInt("simpletask[" + id + "]_stage") == ACHIEVEMENT_COMPLETED;
		}
		return false;
	}

	public boolean playerCanFinishQuest(final Player player, final Achievement quest) {
		int completedTasks = 0;
		for (AchievementTask task : quest.getTasks()) {
			if (getTaskProgress(player, task) >= task.getAmount()) {
				completedTasks++;
			}
		}
		if (quest.getTasks().size() == completedTasks) {
			return true;
		}
		return false;
	}

	private boolean playerStartedQuest(final Player player, final Achievement quest) {
		if (player.getCache().hasKey("simpletask[" + quest.getId() + "]_stage")) {
			return player.getCache().getInt("simpletask[" + quest.getId() + "]_stage") == ACHIEVEMENT_STARTED;
		}
		return false;
	}

	public void checkAndIncGatherItemTasks(final Player player, final Item item) {
		for (final Achievement quest : loadedAchievements) {
			if (!playerStartedQuest(player, quest))
				continue;

			for (final AchievementTask task : quest.getTasks()) {
				if (task.getTask() == TaskType.GATHER_ITEM) {
					if (task.getId() == item.getCatalogId() && getTaskProgress(player, task) < task.getAmount()) {
						final int newAmount = getTaskProgress(player, task) + item.getAmount();
						player.getCache().set("simpletask[" + task.getId() + "]_task_" + task.getTask().toString(),
							newAmount);
						if (newAmount == task.getAmount()) {
							player.message("@gre@You have completed task gather item " + item.getDef(player.getWorld()).getName() + "x"
								+ newAmount + "!");
						}
					}
				}
			}
		}
	}

	public void checkAndIncSlayNpcTasks(final Player player, final Npc npc) {
		for (final Achievement quest : loadedAchievements) {
			if (!playerStartedQuest(player, quest))
				continue;

			for (final AchievementTask task : quest.getTasks()) {
				if (task.getTask() == TaskType.KILL_NPC) {
					if (task.getId() == npc.getID() && getTaskProgress(player, task) < task.getAmount()) {
						final int newAmount = getTaskProgress(player, task) + 1;
						player.getCache().set("simpletask[" + task.getId() + "]_task_" + task.getTask().toString(),
							newAmount);
						if (newAmount == task.getAmount()) {
							player.message("@gre@You have completed slay npc" + npc.getDef().getName() + "x" + newAmount
								+ "!");
						}
					}
				}
			}
		}
	}

	public void achievementListGUI(final Player player, final int achievement, final int status) {
		final com.openrsc.server.net.PacketBuilder s = new com.openrsc.server.net.PacketBuilder();
		s.setID(50);
		s.writeByte((byte) 2);
		s.writeInt(achievement);
		s.writeByte((byte) status);
		player.write(s.toPacket());
	}

	public  void achievementListGUI(final Player player) {
		try {
			final com.openrsc.server.net.PacketBuilder s = new com.openrsc.server.net.PacketBuilder();
			final LinkedList<Achievement> availableTasks = getAchievements();
			s.setID(50);
			s.writeByte((byte) 1);
			s.writeShort(availableTasks.size());
			for (final Achievement task : availableTasks) {
				s.writeInt(task.getId());
				s.writeByte((byte) player.getAchievementStatus(task.getId()));
				s.writeString(task.getName());
				s.writeString(task.getTitle());
				s.writeString((task.getDesc() != null ? task.getDesc() : "No description.."));
				            
			}
			player.write(s.toPacket());
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public Server getServer() {
		return server;
	}
}
