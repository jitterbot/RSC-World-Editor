package com.openrsc.server.plugins.custom.itemactions.pets;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpInvTrigger;

import static com.openrsc.server.plugins.Functions.config;

public class BabyBlueDragonCrystal implements OpInvTrigger {
                                                                                  

	protected Player petOwnerA;

	@Override
	public boolean blockOpInv(Player player, Integer invIndex, Item item, String command) {
		return command.equalsIgnoreCase("inspect");
	}

	@Override
	public void onOpInv(Player player, Integer invIndex, Item item, String command) {
		                        
		System.out.println("Pet item clicked");
		int id = item.getCatalogId();

		if (id == ItemId.A_RED_CRYSTAL.id())
			if (config().WANT_PETS)
				handleBabyBlueDragon(player, item, command);
			else
				player.message("Nothing interesting happens");
	}

	private void handleBabyBlueDragon(Player player, Item item, String command) {
		if (config().DEBUG)
		System.out.println("Pet spawn attempt");
		if (config().WANT_PETS){
			if (player.getCarriedItems().hasCatalogID(ItemId.A_RED_CRYSTAL.id())) {
				if (command.equalsIgnoreCase("inspect")) {
					if (player.getCarriedItems().hasCatalogID(ItemId.A_GLOWING_RED_CRYSTAL.id())) {

						player.message("You may only summon one pet at a time!");
						return;
					}
				}
			}
		}
	}

	public boolean blockPlayerLogin(Player player) {
		return true;
	}

	           
                                           
                                                                                 
                                                                                            
                                                                                 
                                                                                     
    
   
    
}
