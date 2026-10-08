package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;

public interface KillNpcTrigger {
	void onKillNpc(Player player, Npc npc);
	   
                                                                                                                                  
    
	boolean blockKillNpc(Player player, Npc npc);
}
