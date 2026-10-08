package com.openrsc.server.model.entity.update;

import com.openrsc.server.model.entity.player.Player;

public class Bubble {
	   
                      
    
	private int itemID;
	   
                             
    
	private Player owner;

	public Bubble(Player owner, int itemID) {
		this.owner = owner;
		this.itemID = itemID;
	}

	public int getID() {
		return itemID;
	}

	public Player getOwner() {
		return owner;
	}

}
