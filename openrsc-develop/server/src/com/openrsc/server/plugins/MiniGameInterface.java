package com.openrsc.server.plugins;

import com.openrsc.server.model.entity.player.Player;

public interface MiniGameInterface {
	   
                                             
   
           
    
	public int getMiniGameId();

	   
                                               
   
           
    
	public String getMiniGameName();

	   
                                                           
   
           
    
	public boolean isMembers();

	   
                                   
   
                 
    
	public void handleReward(Player player);
}
