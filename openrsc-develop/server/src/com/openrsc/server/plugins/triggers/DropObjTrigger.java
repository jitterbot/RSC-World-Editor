package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;

public interface DropObjTrigger {
	   
                                    
    
	void onDropObj(Player player, Integer invIndex, Item item, Boolean fromInventory);

	   
                                                                   
    
	boolean blockDropObj(Player player, Integer invIndex, Item item, Boolean fromInventory);
}
