package com.openrsc.client.entityhandling.instances;

import com.openrsc.client.entityhandling.EntityHandler;
import com.openrsc.client.entityhandling.defs.ItemDef;

public class Item {
	public final static int ID_NOTHING = -1;
	                                    
	   
                            
    
	private ItemDef itemDef;

	   
                            
    
	private int amount;

	   
                                    
    
	private boolean equipped;

	   
                                 
    
	private boolean noted;

	   
                                                  
    
	private int durability;

	   
                                              
    
	private int charges;
	                
	@Override
	public Item clone() {
		Item ret = new Item();
		ret.itemDef = this.itemDef;
		ret.amount = this.amount;
		ret.equipped = this.equipped;
		ret.noted = this.noted;
		ret.durability = this.durability;
		ret.charges = this.charges;
		return ret;
	}
	                                   
	public Item() {
		this.itemDef = null;
		this.amount = -1;
		this.durability = 0;
		this.charges = 0;
		this.noted = false;
		this.equipped = false;
	}

	public Item(ItemDef itemDef) {
		this.itemDef = itemDef;
		this.amount = 1;
		this.durability = 100;
		this.charges = 0;
		this.noted = false;
		this.equipped = false;
	}
	                
	                                           
	public ItemDef getItemDef() {
		return this.itemDef;
	}

	public int getAmount() {
		return this.amount;
	}

	public int getDurability() {
		return this.durability;
	}

	public int getCharges() {
		return this.charges;
	}

	public boolean getNoted() {
		return this.noted;
	}

	public boolean getEquipped() {
		return this.equipped;
	}
	                
	                                           
	public void setItemDef(ItemDef itemDef) {
		this.itemDef = itemDef;
	}

	public void setItemDef(int catalogID) {
		this.itemDef = EntityHandler.getItemDef(catalogID);
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public void setEquipped(boolean equipped) {
		this.equipped = equipped;
	}

	public void setNoted(boolean noted) {
		this.noted = noted;
	}

	public void setDurability(int durability) {
		this.durability = durability;
	}

	public void setCharges(int charges) {
		this.charges = charges;
	}

	public int getCatalogID() {
		if (this.itemDef == null)
			return ID_NOTHING;
		else
			return this.itemDef.id;
	}

	public String toString() {
		return "Item(" + getCatalogID() + ", " + getAmount() + ", " + getNoted() + ")";
	}
}
