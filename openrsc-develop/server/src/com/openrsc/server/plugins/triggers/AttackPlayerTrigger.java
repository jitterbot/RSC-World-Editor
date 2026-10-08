package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

public interface AttackPlayerTrigger {
	void onAttackPlayer(Player player, Player affectedmob);
	   
                                                                  
    
	boolean blockAttackPlayer(Player player, Player affectedmob);
}
