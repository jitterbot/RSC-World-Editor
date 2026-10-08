package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.struct.EquipRequest;

public interface WearObjTrigger {

	void onWearObj(Player player, Integer invIndex, EquipRequest request);

	   
                                                                   
    
	boolean blockWearObj(Player player, Integer invIndex, EquipRequest request);
}
