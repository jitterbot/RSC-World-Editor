package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.player.Player;

public interface TakeObjTrigger {
	   
                                       
    
	void onTakeObj(Player player, GroundItem groundItem);
	   
                                                                     
    
	boolean blockTakeObj(Player player, GroundItem groundItem);
}
