package com.openrsc.server.model.container;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.external.*;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;

import java.util.HashMap;
import java.util.Map;

public class Item implements Comparable<Item> {

	                                                                     
	   
                                                
    
	protected final Map<String, Object> attributes = new HashMap<String, Object>();
	   
                                         
    
	public final static int ITEM_ID_UNASSIGNED = -1;
	   
                                                                      
    
	private ItemStatus itemStatus;
	   
                                                 
    
	private long itemId;
	                                                                     
	                                                                      

	@Override
	public boolean equals(Object o) {
		if (o instanceof Item) {
			Item item = (Item) o;
			return item.getCatalogId() == getCatalogId()
				&& item.getNoted() == getNoted();
		}
		return false;
	}

	@Override
	public String toString() {
		return "Item(" + getCatalogId() + ", " + getAmount() + ", " + getNoted() + ")";
	}
	                                                                   
	                                                                    
	public Item(int catalogId) {
		this(catalogId, 1, false);
	}

	public Item(int catalogId, int amount) {
		this(catalogId, amount, false);
	}

	public Item(int catalogId, int amount, boolean noted) {
		this(catalogId, amount, noted, ITEM_ID_UNASSIGNED);
	}

	public Item(int catalogId, int amount, boolean noted, long itemId) {
		itemStatus = new ItemStatus();
		itemStatus.setCatalogId(catalogId);
		itemStatus.setAmount(amount);
		itemStatus.setNoted(noted);
		itemStatus.setDurability(100);
	this.itemId = itemId;
	}

	public Item(long itemId, ItemStatus itemStatus) {
		this.itemId = itemId;
		this.itemStatus = itemStatus;
	}
	                                                                
	                                                                
	public final void setItemId(long itemId) {
		this.itemId = itemId;
	}

	public void setAmount(int amount) {
		this.itemStatus.setAmount(amount);

	}

	public void changeAmount(int delta) {
		setAmount(getAmount() + delta);
	}

	public void setNoted(boolean noted) {
		this.itemStatus.setNoted(noted);
	}

	public final void setCatalogId(int newid) {
		itemStatus.setCatalogId(newid);
	}

	public final void setItemStatus(ItemStatus itemStatus) {
		this.itemStatus = itemStatus;
	}

	public void setWielded(boolean wielded) {
		this.itemStatus.setWielded(wielded);
	}
	                                                                 
	                                                                  
	public final long getItemId() {
		return itemId;
	}

	public final ItemStatus getItemStatus() {
		return itemStatus;
	}

	public final int getCatalogId() {
		return itemStatus.getCatalogId();
	}

	private final int getCatalogIdAuthenticNoting(int maxItemId, World world) {
		if (getNoted()) {
			                                                                               
			                                                                                   
			                                                                   
			                                                        

			                                                                                                        
			if (maxItemId >= ItemId.SHANTAY_DESERT_PASS.id()) {
				return ItemId.SHANTAY_DESERT_PASS.id();
			}
			                                                                      
			return ItemId.BRONZE_ARROWS.id();
		} else {
			if (getCatalogId() <= maxItemId) {
				return getCatalogId();
			}

			                                                                    
			if (getDef(world).isStackable() || getNoted()) {
				return ItemId.BRONZE_ARROWS.id();
			} else {
				return ItemId.IRON_MACE.id();
			}
		}
	}

	private final int getUnobtaniumPlaceholderId(World world) {
		if (getDef(world).isStackable() || getNoted()) {
			return ItemId.UNOBTANIUM_STACKABLE.id();
		} else {
			return ItemId.UNOBTANIUM.id();
		}
	}

	                                                                                               
	public final int getSafeItemId(Player player) {
		int safeId;
		if (player.isUsingCustomClient()) {
			safeId = getCatalogId();
			if (safeId > player.getClientLimitations().maxItemId) {
				safeId = getUnobtaniumPlaceholderId(player.getWorld());
			}
		} else {
			safeId = getCatalogIdAuthenticNoting(player.getClientLimitations().maxItemId, player.getWorld());
		}
		return safeId;
	}

	public int getAmount() {
		return itemStatus.getAmount();
	}

	public boolean getNoted() { return itemStatus.getNoted(); }

	public boolean isWielded() {
		return itemStatus.isWielded();
	}
	                                                                 
                                                                     
	public int compareTo(Item item) {
		                                    
             
   
                               
            
   
                                                                        

		                                                                                                                   
		                                                                                                                                                                      
		return item.getCatalogId() - getCatalogId();
	}

	public int eatingHeals(World world) {
		if (!isEdible(world)) {
			return 0;
		}
		return world.getServer().getEntityHandler().getItemEdibleHeals(getCatalogId());
	}

	                                                                                                       
	                                                                                    
	public boolean canLevelDependentHeal(World world) {
		if (!isEdible(world)) {
			return false;
		}
		return world.getServer().getConfig().MEAT_HEAL_LEVEL_DEPENDENT
			&& (getCatalogId() == ItemId.COOKEDMEAT.id()
			|| getCatalogId() == ItemId.BREAD.id()
			|| getDef(world).getName().toLowerCase().contains("pie"));
	}

	public ItemCookingDef getCookingDef(World world) {
		return world.getServer().getEntityHandler().getItemCookingDef(getCatalogId());
	}

	public ItemPerfectCookingDef getPerfectCookingDef(World world) {
		return world.getServer().getEntityHandler().getItemPerfectCookingDef(getCatalogId());
	}

	public ItemDefinition getDef(World world) {
		return world.getServer().getEntityHandler().getItemDef(getCatalogId());
	}

	public ItemSmeltingDef getSmeltingDef(World world) {
		return world.getServer().getEntityHandler().getItemSmeltingDef(getCatalogId());
	}

	public ItemUnIdentHerbDef getUnIdentHerbDef(World world) {
		return world.getServer().getEntityHandler().getItemUnIdentHerbDef(getCatalogId());
	}

	public boolean isEdible(World world) {
		return world.getServer().getEntityHandler().getItemEdibleHeals(getCatalogId()) > 0;
	}

	public boolean isNoteable(World world) {
		return getDef(world).isNoteable();
	}

	public boolean isWieldable(World world) {
		return getDef(world).isWieldable();
	}

	public boolean wieldingAffectsItem(World world, Item i) {
		if (!i.isWieldable(world) || !isWieldable(world)) {
			return false;
		}
		for (int affected : world.getServer().getEntityHandler().getAffectedTypes(getDef(world).getWearableId())) {
			if (i.getDef(world).getWearableId() == affected) {
				return true;
			}
		}
		return false;
	}

	public void removeAttribute(String string) {
		attributes.remove(string);
	}

	public void setAttribute(String string, Object object) {
		attributes.put(string, object);
	}
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String string) {
		return (T) attributes.get(string);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String string, T fail) {
		T object = (T) attributes.get(string);
		if (object != null) {
			return object;
		}
		return fail;
	}
}
