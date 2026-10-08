package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

public interface CatGrowthTrigger {

	   
                                                                                     
    
	public void onCatGrowth(Player player);

	   
                                                                                     
   
           
    
	boolean blockCatGrowth(Player player);
}
