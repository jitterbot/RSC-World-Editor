package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

public interface PlayerDeathTrigger {
	   
                             
   
                 
    
	void onPlayerDeath(Player player);
	   
                                                                                            
   
                 
           
    
	boolean blockPlayerDeath(Player player);
}
