package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

   
                                         
   
public interface PlayerLogoutTrigger {
	   
                                                                                               
    
	void onPlayerLogout(Player player);
	boolean blockPlayerLogout(Player player);
}
