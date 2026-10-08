package com.openrsc.server.plugins.triggers;

import com.openrsc.server.model.entity.player.Player;

public interface WineFermentTrigger {

	   
                                                                                            
    
	public void onWineFerment(Player player);

	   
                                                                                            
   
           
    
	boolean blockWineFerment(Player player);
}
