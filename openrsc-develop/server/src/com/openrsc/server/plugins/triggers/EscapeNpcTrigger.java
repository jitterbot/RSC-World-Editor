package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;

public interface EscapeNpcTrigger {
	   
                                      
   
                 
              
    
	void onEscapeNpc(Player player, Npc npc);
	   
                                                                   
    
	boolean blockEscapeNpc(Player player, Npc npc);
}
