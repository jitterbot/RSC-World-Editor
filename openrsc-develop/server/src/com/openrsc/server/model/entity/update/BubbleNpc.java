package com.openrsc.server.model.entity.update;

import com.openrsc.server.model.entity.Mob;

public class BubbleNpc {
	   
                      
    
	private int itemID;
	   
                             
    
	private Mob owner;

	public BubbleNpc(Mob owner, int itemID) {
		this.owner = owner;
		this.itemID = itemID;
	}

	public int getID() {
		return itemID;
	}

	public Mob getOwner() {
		return owner;
	}

}
