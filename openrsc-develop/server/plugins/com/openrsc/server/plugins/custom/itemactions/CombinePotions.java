package com.openrsc.server.plugins.custom.itemactions;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.UseInvTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class CombinePotions implements UseInvTrigger {

	int[][] combinePotions = {
		{ItemId.ONE_ATTACK_POTION.id(), ItemId.TWO_ATTACK_POTION.id(), ItemId.FULL_ATTACK_POTION.id()},
		{ItemId.ONE_STAT_RESTORATION_POTION.id(), ItemId.TWO_STAT_RESTORATION_POTION.id(), ItemId.FULL_STAT_RESTORATION_POTION.id()},
		{ItemId.ONE_DEFENSE_POTION.id(), ItemId.TWO_DEFENSE_POTION.id(), ItemId.FULL_DEFENSE_POTION.id()},
		{ItemId.ONE_RESTORE_PRAYER_POTION.id(), ItemId.TWO_RESTORE_PRAYER_POTION.id(), ItemId.FULL_RESTORE_PRAYER_POTION.id()},
		{ItemId.ONE_SUPER_ATTACK_POTION.id(), ItemId.TWO_SUPER_ATTACK_POTION.id(), ItemId.FULL_SUPER_ATTACK_POTION.id()},
		{ItemId.ONE_FISHING_POTION.id(), ItemId.TWO_FISHING_POTION.id(), ItemId.FULL_FISHING_POTION.id()},
		{ItemId.ONE_SUPER_STRENGTH_POTION.id(), ItemId.TWO_SUPER_STRENGTH_POTION.id(), ItemId.FULL_SUPER_STRENGTH_POTION.id()},
		{ItemId.ONE_SUPER_DEFENSE_POTION.id(), ItemId.TWO_SUPER_DEFENSE_POTION.id(), ItemId.FULL_SUPER_DEFENSE_POTION.id()},
		{ItemId.ONE_RANGING_POTION.id(), ItemId.TWO_RANGING_POTION.id(), ItemId.FULL_RANGING_POTION.id()},
		{ItemId.ONE_CURE_POISON_POTION.id(), ItemId.TWO_CURE_POISON_POTION.id(), ItemId.FULL_CURE_POISON_POTION.id()},
		{ItemId.ONE_POTION_OF_ZAMORAK.id(), ItemId.TWO_POTION_OF_ZAMORAK.id(), ItemId.FULL_POTION_OF_ZAMORAK.id()},
		{ItemId.ONE_MAGIC_POTION.id(), ItemId.TWO_MAGIC_POTION.id(), ItemId.FULL_MAGIC_POTION.id()},
		{ItemId.ONE_POTION_OF_SARADOMIN.id(), ItemId.TWO_POTION_OF_SARADOMIN.id(), ItemId.FULL_POTION_OF_SARADOMIN.id()},
	};

	@Override
	public void onUseInv(Player player, Integer invIndex, Item item1, Item item2) {

		                                               
		if (!config().WANT_DECANTING) {
			player.message("Nothing interesting happens");
			return;
		}

		if (item1.getItemStatus().getNoted() || item2.getItemStatus().getNoted())
			return;

		                                
		                                
		if (item1.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id() || item1.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id()) {
			if (player.getCarriedItems().remove(new Item(ItemId.ONE_STRENGTH_POTION.id())) > -1 && player.getCarriedItems().remove(new Item(ItemId.TWO_STRENGTH_POTION.id())) > -1) {
				give(player, ItemId.THREE_STRENGTH_POTION.id(), 1);
				player.message("You combine 2 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase() + " with 1 dose of " + item2.getDef(player.getWorld()).getName().toLowerCase());
				give(player, ItemId.EMPTY_VIAL.id(), 1);                      
			}
		}
		                            
		else if (item1.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id() || item1.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id()) {
			if (player.getCarriedItems().remove(new Item(ItemId.ONE_STRENGTH_POTION.id())) > -1 && player.getCarriedItems().remove(new Item(ItemId.THREE_STRENGTH_POTION.id())) > -1) {
				give(player, ItemId.FULL_STRENGTH_POTION.id(), 1);
				player.message("You combine 3 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase() + " with 1 dose of " + item2.getDef(player.getWorld()).getName().toLowerCase());
				give(player, ItemId.EMPTY_VIAL.id(), 1);                      
			}
		}
		                            
		else if (item1.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id()) {
			if (player.getCarriedItems().remove(new Item(ItemId.TWO_STRENGTH_POTION.id())) > -1 && player.getCarriedItems().remove(new Item(ItemId.TWO_STRENGTH_POTION.id())) > -1) {
				give(player, ItemId.FULL_STRENGTH_POTION.id(), 1);
				player.message("You combine two 2 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase());
				give(player, ItemId.EMPTY_VIAL.id(), 1);                      
			}
		}
		                            
		else if (item1.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id()) {
			if (player.getCarriedItems().remove(new Item(ItemId.ONE_STRENGTH_POTION.id())) > -1 && player.getCarriedItems().remove(new Item(ItemId.ONE_STRENGTH_POTION.id())) > -1) {
				give(player, ItemId.TWO_STRENGTH_POTION.id(), 1);
				player.message("You combine 1 dose of " + item1.getDef(player.getWorld()).getName().toLowerCase() + " with 1 dose of " + item2.getDef(player.getWorld()).getName().toLowerCase());
				give(player, ItemId.EMPTY_VIAL.id(), 1);                      
			}
		}
		                                                                  
		else if (item1.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id()) {
			if (player.getCarriedItems().remove(new Item(ItemId.THREE_STRENGTH_POTION.id()	)) > -1 && player.getCarriedItems().remove(new Item(ItemId.THREE_STRENGTH_POTION.id())) > -1) {
				give(player, ItemId.FULL_STRENGTH_POTION.id(), 1);          
				give(player, ItemId.TWO_STRENGTH_POTION.id(), 1);          
				player.message("You combine two 3 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase());
			}
		}
		                                       
		else {
			for (int i = 0; i < combinePotions.length; i++) {
				                           
				if ((item1.getCatalogId() == combinePotions[i][0] && item2.getCatalogId() == combinePotions[i][1]) || (item2.getCatalogId() == combinePotions[i][0] && item1.getCatalogId() == combinePotions[i][1])) {
					if (player.getCarriedItems().remove(new Item(combinePotions[i][0])) > -1 && player.getCarriedItems().remove(new Item(combinePotions[i][1])) > -1) {
						player.message("You combine 2 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase() + " with 1 dose of " + item2.getDef(player.getWorld()).getName().toLowerCase());
						player.getCarriedItems().getInventory().add(new Item(combinePotions[i][2]));              
						player.message("to a full 3 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase());
						give(player, ItemId.EMPTY_VIAL.id(), 1);                      
						player.message("you get an empty vial over");
						return;
					}
				}
				                           
				else if (item1.getCatalogId() == combinePotions[i][0] && item2.getCatalogId() == combinePotions[i][0]) {
					if (player.getCarriedItems().remove(new Item(combinePotions[i][0])) > -1 && player.getCarriedItems().remove(new Item(combinePotions[i][0])) > -1) {
						player.message("You combine two 1 dose of " + item1.getDef(player.getWorld()).getName().toLowerCase());
						player.getCarriedItems().getInventory().add(new Item(combinePotions[i][1]));              
						player.message("to 2 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase());
						give(player, ItemId.EMPTY_VIAL.id(), 1);                      
						player.message("you get an empty vial over");
						return;
					}
				}
				                           
				else if (item1.getCatalogId() == combinePotions[i][1] && item2.getCatalogId() == combinePotions[i][1]) {
					if (player.getCarriedItems().remove(new Item(combinePotions[i][1])) > -1 && player.getCarriedItems().remove(new Item(combinePotions[i][1])) > -1) {
						player.message("You combine two 2 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase());
						player.getCarriedItems().getInventory().add(new Item(combinePotions[i][2]));              
						player.getCarriedItems().getInventory().add(new Item(combinePotions[i][0]));              
						player.message("to a full 3 doses of " + item1.getDef(player.getWorld()).getName().toLowerCase() + " and 1 dose of " + item1.getDef(player.getWorld()).getName().toLowerCase());
						return;
					}
				}
			}
		}
	}

	@Override
	public boolean blockUseInv(Player player, Integer invIndex, Item item1, Item item2) {
		                                
		if (item1.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id() || item1.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id()) {
			return true;
		}
		                            
		if (item1.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id() || item1.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id()) {
			return true;
		}
		                            
		if (item1.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.TWO_STRENGTH_POTION.id()) {
			return true;
		}
		                            
		if (item1.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.ONE_STRENGTH_POTION.id()) {
			return true;
		}
		if (item1.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id() && item2.getCatalogId() == ItemId.THREE_STRENGTH_POTION.id()) {
			return true;
		}
		for (int i = 0; i < combinePotions.length; i++) {
			if ((item1.getCatalogId() == combinePotions[i][0] && item2.getCatalogId() == combinePotions[i][1]) || (item2.getCatalogId() == combinePotions[i][0] && item1.getCatalogId() == combinePotions[i][1])) {
				return true;
			}
			if (item1.getCatalogId() == combinePotions[i][1] && item2.getCatalogId() == combinePotions[i][1]) {
				return true;
			}
			if (item1.getCatalogId() == combinePotions[i][0] && item2.getCatalogId() == combinePotions[i][0]) {
				return true;
			}
		}
		return false;
	}
}
