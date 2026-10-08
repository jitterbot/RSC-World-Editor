package com.openrsc.server.plugins.triggers;

import com.openrsc.server.constants.Spells;
import com.openrsc.server.model.entity.player.Player;

public interface SpellPlayerTrigger {
	   
                                 
    
	void onSpellPlayer(Player player, Player affectedPlayer, Spells spellEnum);
	   
                                               
   
           
    
	boolean blockSpellPlayer(Player player, Player affectedPlayer, Spells spellEnum);
}
