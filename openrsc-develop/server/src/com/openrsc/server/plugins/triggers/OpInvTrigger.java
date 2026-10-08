package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;

public interface OpInvTrigger {

	   
                                                   
   
                 
                   
               
    
	void onOpInv(Player player, Integer invIndex, Item item, String command);

	   
                                           
    
	boolean blockOpInv(Player player, Integer invIndex, Item item, String command);
}
