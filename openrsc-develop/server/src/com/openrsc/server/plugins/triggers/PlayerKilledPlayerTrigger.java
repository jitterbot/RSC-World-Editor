package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

public interface PlayerKilledPlayerTrigger {
	void onPlayerKilledPlayer(Player killer, Player killed);
	   
                                                                                                                                     
    
	boolean blockPlayerKilledPlayer(Player killer, Player killed);
}
