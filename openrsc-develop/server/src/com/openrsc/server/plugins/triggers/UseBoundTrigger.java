package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;

public interface UseBoundTrigger {
	   
                                                               
    
	void onUseBound(Player player, GameObject gameObject, Item item);
	   
                                                                                  
    
	boolean blockUseBound(Player player, GameObject gameObject, Item item);
}
