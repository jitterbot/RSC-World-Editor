package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

public interface PlayerRangePlayerTrigger {
	   
                                              
    
	void onPlayerRangePlayer(Player player, Player affectedMob);
	   
                                                                   
    
	boolean blockPlayerRangePlayer(Player player, Player affectedMob);
}
