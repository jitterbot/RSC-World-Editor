package com.openrsc.server.plugins;

import com.openrsc.server.model.entity.player.Player;

public interface QuestInterface {
	   
                                
   
           
    
	int getQuestId();

	   
                                  
   
           
    
	String getQuestName();

	   
                                          
   
           
    
	int getQuestPoints();

	   
                                                       
   
           
    
	boolean isMembers();

	   
                                   
   
                 
    
	void handleReward(Player player);
}
