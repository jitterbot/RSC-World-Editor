package com.openrsc.server.model.container;

import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.entity.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

                                                              
  
                                                                  
                                                                   
                                                                 
                                                             
   
public class CarriedItems {

	private Inventory inventory;                                            
	private Equipment equipment;                                            
	private final Player player;                                         

	public CarriedItems(Player player) {
		this.player = player;
	}

	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	public Inventory getInventory() {
		return this.inventory;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public Equipment getEquipment() {
		return this.equipment;
	}

	                              
                                                             
                                                  
    
	public boolean hasCatalogID(final int catalogID) {
		return this.hasCatalogID(catalogID, Optional.of(false));
	}

	public boolean hasCatalogID(final int catalogID, final Optional<Boolean> noted) {
		                                                                
		if (!noted.isPresent()) {
			if (getInventory().hasCatalogID(catalogID))
				return true;
			else
				return getEquipment().hasCatalogID(catalogID);
		} else {
			boolean isNoted = noted.get();
			if (getInventory().hasCatalogID(catalogID, isNoted))
				return true;
			else
				return getEquipment().hasCatalogID(catalogID);
		}

	}

	                         
                                                
                                                                           
                                   
    
	public void shatter(Item item) {
		Item itemToShatter = getEquipment().get(
			getEquipment().searchEquipmentForItem(item.getCatalogId())
		);
		if (player.getConfig().WANT_EQUIPMENT_TAB && itemToShatter != null) {
			player.getCarriedItems().getEquipment().remove(itemToShatter, 1);
		} else {
			itemToShatter = getInventory().get(
				getInventory().getLastIndexById(item.getCatalogId())
			);
			if (itemToShatter == null) return;
			remove(itemToShatter);
		}
		player.message("Your " + player.getWorld().getServer().getEntityHandler().getItemDef(itemToShatter.getCatalogId()).getName() + " shatters");
	}

	                        
                                                              
                                                              
                                                               
    
	public long remove(Item item) {
		return remove(item, true);
	}

	                                 
	public long remove(Item item, boolean updateClient) {
		                                                                             
		Item toRemove = item;
		if (item.getItemId() == -1) {
			toRemove = getInventory().get(
				getInventory().getLastIndexById(item.getCatalogId(), Optional.of(item.getNoted()))
			);
			if (toRemove != null) {
				item = new Item(toRemove.getCatalogId(), item.getAmount(), toRemove.getNoted(), toRemove.getItemId());
			}
		}

		                                                                                               
		if (toRemove != null && getInventory().countId(toRemove.getCatalogId(), Optional.of(toRemove.getNoted())) >= item.getAmount()) {
			return getInventory().remove(item, updateClient);
		}
		else {
			toRemove = getEquipment().get(
				getEquipment().searchEquipmentForItem(item.getCatalogId())
			);
			if (toRemove != null && toRemove.getAmount() >= item.getAmount()) {
				item = new Item(toRemove.getCatalogId(), item.getAmount(), toRemove.getNoted(), toRemove.getItemId());
				return getEquipment().remove(item, item.getAmount());
			}
		}
		return -1;
	}

	public boolean remove(final Item... items)
	{
		return remove(items, true);
	}

	   
                                                                                            
   
                                     
                                                                     
                                                                   
    
	public boolean remove(final Item[] items, final boolean updateClient)
	{
		if (items.length == 0)
		{
			                    
			return false;
		}

		final List<Item> inventoryItems = new ArrayList<>(items.length);
		List<Item> equipmentItems = null;

		if (player.getConfig().WANT_EQUIPMENT_TAB)
		{
			equipmentItems = new ArrayList<>(items.length);
		}

		synchronized (inventory.getItems())
		{
			synchronized (equipment.getList())
			{
				for (final Item item : items)
				{
					int idx = inventory.getLastIndexById(item.getCatalogId(), Optional.of(item.getNoted()));

					if (idx != -1)
					{
						                                   
						final Item invItem = inventory.get(idx);
						final ItemDefinition itemDef = invItem.getDef(player.getWorld());

						if (itemDef == null)
						{
							return false;
						}

						if ((itemDef.isStackable() || invItem.getNoted()) && invItem.getAmount() < item.getAmount())
						{
							                        
							return false;
						}

						                                                      
						final Item removeItem = new Item(invItem.getCatalogId(), item.getAmount(), invItem.getNoted(), invItem.getItemId());

						inventoryItems.add(removeItem);
						continue;
					}

					if (equipmentItems == null)
					{
						return false;
					}

					idx = equipment.searchEquipmentForItem(item.getCatalogId());

					if (idx == -1)
					{
						return false;
					}

					                                   
					final Item equipItem = equipment.get(idx);
					final ItemDefinition itemDef = equipItem.getDef(player.getWorld());

					if (itemDef == null)
					{
						return false;
					}

					if (itemDef.isStackable() && equipItem.getAmount() < item.getAmount())
					{
						                        
						return false;
					}

					                                                      
					final Item removeItem = new Item(equipItem.getCatalogId(), item.getAmount(), equipItem.getNoted(), equipItem.getItemId());

					equipmentItems.add(removeItem);
				}

				                                                                                

				for (final Item item : inventoryItems)
				{
					inventory.remove(item, updateClient);
				}

				if (equipmentItems != null)
				{
					for (final Item item : equipmentItems)
					{
						equipment.remove(item, item.getAmount(), updateClient);
					}
				}
			}
		}

		return true;
	}
}
