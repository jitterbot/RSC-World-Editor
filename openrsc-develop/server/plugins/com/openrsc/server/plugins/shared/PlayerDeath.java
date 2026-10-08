package com.openrsc.server.plugins.shared;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.PlayerDeathTrigger;

public class PlayerDeath implements PlayerDeathTrigger {
	@Override
	public void onPlayerDeath(Player player) {
		                
	}

	@Override
	public boolean blockPlayerDeath(Player player) {
		return false;
	}
}
