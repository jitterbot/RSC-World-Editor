package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

   
                                       
   
public interface PlayerLoginTrigger {
	   
                             
    
	void onPlayerLogin(Player player);
	boolean blockPlayerLogin(Player player);
}
