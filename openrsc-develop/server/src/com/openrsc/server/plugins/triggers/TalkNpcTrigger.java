package com.openrsc.server.plugins.triggers;


import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;

public interface TalkNpcTrigger {
	   
                                       
   
                 
              
    
	void onTalkNpc(Player player, Npc npc);
	   
                                                       
    
	boolean blockTalkNpc(Player player, Npc npc);
}
