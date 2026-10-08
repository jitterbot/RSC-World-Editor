package com.openrsc.server.content;

import com.openrsc.server.constants.Classes;
import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.rsc.ActionSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerClass {

	private Player player;
	private Classes chosenClass;

	public PlayerClass(Player player, Classes chosenClass) {
		this.player = player;
		this.chosenClass = chosenClass;
	}

	public void init() {
		List<Item> starterItems = new ArrayList<>();
		switch (chosenClass) {
			                                  
			case ADVENTURER:
				                                         
				                                  
				                                   
				                     
				                                               
				player.getSkills().setExperienceAndLevel(Skill.ATTACK.id(), 400, 2, false);
				player.getSkills().setExperienceAndLevel(Skill.STRENGTH.id(), 400, 2, false);
				player.getSkills().setExperienceAndLevel(Skill.DEFENSE.id(), 400, 2, false);
				player.getSkills().setExperienceAndLevel(Skill.RANGED.id(), 400, 2, false);
				if (player.getConfig().DIVIDED_GOOD_EVIL) {
					player.getSkills().setExperienceAndLevel(Skill.PRAYGOOD.id(), 400, 2, false);
					player.getSkills().setExperienceAndLevel(Skill.PRAYEVIL.id(), 400, 2, false);
					player.getSkills().setExperienceAndLevel(Skill.GOODMAGIC.id(), 400, 2, false);
					player.getSkills().setExperienceAndLevel(Skill.EVILMAGIC.id(), 400, 2, false);
				} else {
					player.getSkills().setExperienceAndLevel(Skill.PRAYER.id(), 400, 2, false);
					player.getSkills().setExperienceAndLevel(Skill.MAGIC.id(), 400, 2, false);
				}
				player.getSkills().setExperienceAndLevel(Skill.HITS.id(), 4000, 11, false);
				starterItems.addAll(Arrays.asList(new Item(ItemId.TINDERBOX.id()), new Item(ItemId.BRONZE_AXE.id()), new Item(ItemId.JUG.id()), new Item(ItemId.POT.id())));
				break;
			case WARRIOR:
				                              
				          
				                                           
				player.getSkills().setExperienceAndLevel(Skill.ATTACK.id(), 800, 3, false);
				player.getSkills().setExperienceAndLevel(Skill.STRENGTH.id(), 800, 3, false);
				player.getSkills().setExperienceAndLevel(Skill.DEFENSE.id(), 800, 3, false);
				player.getSkills().setExperienceAndLevel(Skill.HITS.id(), 4400, 12, false);
				starterItems.addAll(Arrays.asList(new Item(ItemId.BRONZE_SHORT_SWORD.id()), new Item(ItemId.WOODEN_SHIELD.id())));
				break;
			case WIZARD:
				                      
				          
				                                       
				if (player.getConfig().DIVIDED_GOOD_EVIL) {
					player.getSkills().setExperienceAndLevel(Skill.GOODMAGIC.id(), 2400, 7, false);
				} else {
					player.getSkills().setExperienceAndLevel(Skill.MAGIC.id(), 2400, 7, false);
				}
				player.getSkills().setExperienceAndLevel(Skill.HITS.id(), 3600, 10, false);
				starterItems.addAll(Arrays.asList(new Item(ItemId.BLUE_WIZARDSHAT.id()), new Item(ItemId.STAFF.id())));
				break;
			case NECROMANCER:
				                      
				          
				                                        
				if (player.getConfig().DIVIDED_GOOD_EVIL) {
					player.getSkills().setExperienceAndLevel(Skill.EVILMAGIC.id(), 2400, 7, false);
				} else {
					player.getSkills().setExperienceAndLevel(Skill.MAGIC.id(), 2400, 7, false);
				}
				player.getSkills().setExperienceAndLevel(Skill.HITS.id(), 3600, 10, false);
				starterItems.addAll(Arrays.asList(new Item(ItemId.BLACK_WIZARDSHAT.id()), new Item(ItemId.STAFF.id())));
				break;
			case RANGER:
				           
				          
				                                      
				player.getSkills().setExperienceAndLevel(Skill.RANGED.id(), 2000, 6, false);
				player.getSkills().setExperienceAndLevel(Skill.HITS.id(), 4400, 12, false);
				starterItems.addAll(Arrays.asList(new Item(ItemId.SHORTBOW.id()), new Item(ItemId.BRONZE_ARROWS.id(), 10)));
				break;
			case MINER:
				           
				          
				                          
				player.getSkills().setExperienceAndLevel(Skill.MINING.id(), 2400, 7, false);
				player.getSkills().setExperienceAndLevel(Skill.HITS.id(), 3600, 10, false);
				starterItems.addAll(Arrays.asList(new Item(ItemId.BRONZE_PICKAXE.id())));
				break;
		}
		ActionSender.sendStats(player);
		for (Item item : starterItems) {
			player.getCarriedItems().getInventory().add(item, false);
		}
		ActionSender.sendInventory(player);
	}

}
