package com.openrsc.server.model;

import com.openrsc.server.model.entity.player.Player;

public class MenuOptionListener {
	   
                                                
    
	protected String[] options;
	private Player owner;

	   
                                                    
    
	public MenuOptionListener(final String[] options) {
		this.options = options;
	}

	   
                                      
    
	public final String getOption(final int index) {
		if (index < 0 || index >= options.length) {
			return null;
		}
		return options[index];
	}

	public final String[] getOptions() {
		return options;
	}

	   
                                          
    
	public final void handleReply(final int option, final String reply) {
		getOwner().setOption(option);
	}

	   
                                                      
    
	public final void setOwner(final Player owner) {
		this.owner = owner;
	}

	   
                                              
    
	public Player getOwner() {
		return owner;
	}
}
