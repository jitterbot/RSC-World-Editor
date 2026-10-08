package com.openrsc.server.plugins.triggers;


import com.openrsc.server.constants.Spells;
import com.openrsc.server.model.entity.player.Player;

public interface SpellInvTrigger {
	   
                                   
    
	void onSpellInv(Player player, Integer invIndex, Integer itemID, Spells spellEnum);
	   
                                               
   
           
    
	boolean blockSpellInv(Player player, Integer invIndex, Integer itemID, Spells spellEnum);
}
