package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;

public interface PlayerRangeNpcTrigger {
	void onPlayerRangeNpc(Player player, Npc npc);
	   
                                                                   
    
	boolean blockPlayerRangeNpc(Player player, Npc npc);
}
