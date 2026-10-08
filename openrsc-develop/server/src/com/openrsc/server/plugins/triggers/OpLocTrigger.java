package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;

public interface OpLocTrigger {
	   
                                            
    
	void onOpLoc(Player player, GameObject obj, String command);
	   
                                                     
    
	boolean blockOpLoc(Player player, GameObject obj, String command);
}
