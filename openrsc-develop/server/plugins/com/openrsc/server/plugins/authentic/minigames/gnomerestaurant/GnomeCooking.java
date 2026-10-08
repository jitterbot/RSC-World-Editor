package com.openrsc.server.plugins.authentic.minigames.gnomerestaurant;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpInvTrigger;
import com.openrsc.server.plugins.triggers.UseLocTrigger;
import com.openrsc.server.util.rsc.Formulae;

import java.util.Optional;

import static com.openrsc.server.plugins.Functions.*;

public class GnomeCooking implements OpInvTrigger, UseLocTrigger {

	private boolean canCook(Item item, GameObject object) {
		for (GnomeCook c : GnomeCook.values()) {
			if (item.getCatalogId() == c.uncookedID && inArray(object.getID(), 119)) {
				return true;
			}
		}
		return false;
	}

	protected int GNOMECRUNCHIE = 0;
	protected int CHOC_CRUNCHIE = 1;
	protected int WORM_CRUNCHIE = 2;
	protected int TOAD_CRUNCHIE = 3;
	protected int SPICY_CRUNCHIE = 4;
	protected int CHEESE_AND_TOMATO_BATTA = 5;
	private int TOAD_BATTA = 6;
	protected int WORM_BATTA = 7;
	protected int FRUIT_BATTA = 8;
	protected int VEG_BATTA = 9;
	protected int CHOC_BOMB = 10;
	protected int VEGBALL = 11;
	protected int WORM_HOLE = 12;
	private int TANGLED_TOADS_LEGS = 13;

	protected String[] recipeStrings = {

		                  
		String.format("%d!", ItemId.GNOMECRUNCHIE_DOUGH.id()),

		                   
		String.format("%d%d-%d%d-%d%d-%d-%d-%d%d!",
			ItemId.CHOCOLATE_BAR.id(), ItemId.GIANNE_DOUGH.id(),                                           
			ItemId.CHOCOLATE_BAR.id(), ItemId.GIANNE_DOUGH.id(),                                           
			ItemId.GNOME_SPICE.id(), ItemId.GIANNE_DOUGH.id(),                                           
			ItemId.GIANNE_DOUGH.id(),                                 
			ItemId.GNOMECRUNCHIE_DOUGH.id(),                                       
			ItemId.CHOCOLATE_DUST.id(), ItemId.GNOMECRUNCHIE.id()),                                             

		                   
		String.format("%d%d-%d%d-%d%d-%d%d-%d-%d-%d%d!",
			ItemId.GNOME_SPICE.id(), ItemId.GIANNE_DOUGH.id(),                                         
			ItemId.KING_WORM.id(), ItemId.GIANNE_DOUGH.id(),                                       
			ItemId.KING_WORM.id(), ItemId.GIANNE_DOUGH.id(),                                         
			ItemId.EQUA_LEAVES.id(), ItemId.GIANNE_DOUGH.id(),                                          
			ItemId.GIANNE_DOUGH.id(),                                 
			ItemId.GNOMECRUNCHIE_DOUGH.id(),                                      
			ItemId.GNOME_SPICE.id(), ItemId.GNOMECRUNCHIE.id()),                                            

		                   
		String.format("%d%d-%d%d-%d%d-%d-%d-%d%d!",
			ItemId.GNOME_SPICE.id(), ItemId.GIANNE_DOUGH.id(),                                         
			ItemId.TOAD_LEGS.id(), ItemId.GIANNE_DOUGH.id(),                                       
			ItemId.TOAD_LEGS.id(), ItemId.GIANNE_DOUGH.id(),                                         
			ItemId.GIANNE_DOUGH.id(),                                 
			ItemId.GNOMECRUNCHIE_DOUGH.id(),                                       
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMECRUNCHIE.id()),                                          

		                    
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d-%d%d!",
			ItemId.GNOME_SPICE.id(), ItemId.GIANNE_DOUGH.id(),                                         
			ItemId.GNOME_SPICE.id(), ItemId.GIANNE_DOUGH.id(),                                         
			ItemId.GNOME_SPICE.id(), ItemId.GIANNE_DOUGH.id(),                                           
			ItemId.EQUA_LEAVES.id(), ItemId.GIANNE_DOUGH.id(),                                          
			ItemId.EQUA_LEAVES.id(), ItemId.GIANNE_DOUGH.id(),                                          
			ItemId.GIANNE_DOUGH.id(),                                
			ItemId.GNOMECRUNCHIE_DOUGH.id(),                                        
			ItemId.GNOME_SPICE.id(), ItemId.GNOMECRUNCHIE.id()),                                            

		                            
		String.format("%d%d-%d%d-%d-%d%d!",
			ItemId.CHEESE.id(), ItemId.GNOMEBATTA.id(),                                  
			ItemId.TOMATO.id(), ItemId.GNOMEBATTA.id(),                                  
			ItemId.GNOMEBATTA.id(),                               
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id()),                                        

		                                                              
		String.format("%d%d-%d%d-%d%d-%d%d-%d!",
			ItemId.EQUA_LEAVES.id(), ItemId.TOAD_LEGS.id(),                                      
			ItemId.GNOME_SPICE.id(), ItemId.TOAD_LEGS.id(),                                      
			ItemId.TOAD_LEGS.id(), ItemId.GNOMEBATTA.id(),                                       
			ItemId.CHEESE.id(), ItemId.GNOMEBATTA.id(),                                   
			ItemId.GNOMEBATTA.id()),                              

		                                              
		String.format("%d%d-%d%d-%d%d-%d-%d%d!",
			ItemId.GNOME_SPICE.id(), ItemId.KING_WORM.id(),                                      
			ItemId.KING_WORM.id(), ItemId.GNOMEBATTA.id(),                                     
			ItemId.CHEESE.id(), ItemId.GNOMEBATTA.id(),                                    
			ItemId.GNOMEBATTA.id(),                              
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id()),                                        

		                
		String.format("%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d%d-%d%d-%d%d!",
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id(),                                       
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id(),                                       
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id(),                                         
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id(),                                        
			ItemId.GNOMEBATTA.id(),                              
			ItemId.PINEAPPLE_CHUNKS.id(), ItemId.GNOMEBATTA.id(),                                            
			ItemId.DICED_ORANGE.id(), ItemId.GNOMEBATTA.id(),                                          
			ItemId.LIME_CHUNKS.id(), ItemId.GNOMEBATTA.id(),                                         
			ItemId.GNOME_SPICE.id(), ItemId.GNOMEBATTA.id()),                                        

		              
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d-%d%d!",
			ItemId.ONION.id(), ItemId.GNOMEBATTA.id(),                                 
			ItemId.TOMATO.id(), ItemId.GNOMEBATTA.id(),                                  
			ItemId.TOMATO.id(), ItemId.GNOMEBATTA.id(),                                    
			ItemId.CABBAGE.id(), ItemId.GNOMEBATTA.id(),                                    
			ItemId.DWELLBERRIES.id(), ItemId.GNOMEBATTA.id(),                                         
			ItemId.GNOMEBATTA.id(),                             
			ItemId.CHEESE.id(), ItemId.GNOMEBATTA.id(),                                    
			ItemId.GNOMEBATTA.id(),                               
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBATTA.id()),                                        

		               
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d%d-%d%d!",
			ItemId.CHOCOLATE_BAR.id(), ItemId.GNOMEBOWL.id(),                                        
			ItemId.CHOCOLATE_BAR.id(), ItemId.GNOMEBOWL.id(),                                        
			ItemId.CHOCOLATE_BAR.id(), ItemId.GNOMEBOWL.id(),                                          
			ItemId.CHOCOLATE_BAR.id(), ItemId.GNOMEBOWL.id(),                                         
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBOWL.id(),                                       
			ItemId.GNOMEBOWL.id(),                            
			ItemId.CREAM.id(), ItemId.GNOMEBOWL.id(),                                  
			ItemId.CREAM.id(), ItemId.GNOMEBOWL.id(),                                  
			ItemId.CHOCOLATE_DUST.id(), ItemId.GNOMEBOWL.id()),                                          

		             
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d%d!",
			ItemId.ONION.id(), ItemId.GNOMEBOWL.id(),                                
			ItemId.ONION.id(), ItemId.GNOMEBOWL.id(),                                
			ItemId.POTATO.id(), ItemId.GNOMEBOWL.id(),                                   
			ItemId.POTATO.id(), ItemId.GNOMEBOWL.id(),                                  
			ItemId.GNOME_SPICE.id(), ItemId.GNOMEBOWL.id(),                                       
			ItemId.GNOMEBOWL.id(),                            
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBOWL.id()),                                        

		               
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d%d!",
			ItemId.KING_WORM.id(), ItemId.GNOMEBOWL.id(),                                    
			ItemId.KING_WORM.id(), ItemId.GNOMEBOWL.id(),                                    
			ItemId.KING_WORM.id(), ItemId.GNOMEBOWL.id(),                                      
			ItemId.KING_WORM.id(), ItemId.GNOMEBOWL.id(),                                     
			ItemId.KING_WORM.id(), ItemId.GNOMEBOWL.id(),                                     
			ItemId.KING_WORM.id(), ItemId.GNOMEBOWL.id(),                                    
			ItemId.ONION.id(), ItemId.GNOMEBOWL.id(),                                  
			ItemId.ONION.id(), ItemId.GNOMEBOWL.id(),                                  
			ItemId.GNOME_SPICE.id(), ItemId.GNOMEBOWL.id(),                                       
			ItemId.GNOMEBOWL.id(),                            
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBOWL.id()),                                         

		                                             
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d!",
			ItemId.CHEESE.id(), ItemId.GNOMEBOWL.id(),                                 
			ItemId.CHEESE.id(), ItemId.GNOMEBOWL.id(),                                 
			ItemId.TOAD_LEGS.id(), ItemId.GNOMEBOWL.id(),                                      
			ItemId.TOAD_LEGS.id(), ItemId.GNOMEBOWL.id(),                                     
			ItemId.TOAD_LEGS.id(), ItemId.GNOMEBOWL.id(),                                     
			ItemId.TOAD_LEGS.id(), ItemId.GNOMEBOWL.id(),                                    
			ItemId.TOAD_LEGS.id(), ItemId.GNOMEBOWL.id(),                                      
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBOWL.id(),                                        
			ItemId.EQUA_LEAVES.id(), ItemId.GNOMEBOWL.id(),                                       
			ItemId.DWELLBERRIES.id(), ItemId.GNOMEBOWL.id(),                                       
			ItemId.GNOME_SPICE.id(), ItemId.GNOMEBOWL.id(),                                         
			ItemId.GNOME_SPICE.id(), ItemId.GNOMEBOWL.id(),                                         
			ItemId.GNOMEBOWL.id()),                                 
	};

	private void handleGnomeCooking(final Item item, Player player, final GameObject object) {
		GnomeCook gc = null;
		for (GnomeCook c : GnomeCook.values()) {
			if (item.getCatalogId() == c.uncookedID && inArray(object.getID(), 119)) {
				gc = c;
			}
		}
		                                                                     
		thinkbubble(item);
		player.playSound("cooking");
		if (player.getCarriedItems().remove(item) > -1) {
			mes(gc.messages[0]);
			delay(5);
			if (!burnFood(player, item.getCatalogId(), player.getSkills().getLevel(Skill.COOKING.id()))) {
				player.message(gc.messages[1]);

				                                        
				if (item.getCatalogId() == ItemId.GNOMEBATTA_DOUGH.id() || item.getCatalogId() == ItemId.GNOMEBOWL_DOUGH.id()) {
					give(player, gc.cookedID, 1);
					return;
				}

				                    
				boolean recipeSuccess = addGnomeRecipeCache(player, -1, item.getCatalogId());
				if (recipeSuccess) {
					player.incExp(Skill.COOKING.id(), gc.experience, true);

					             
					if (player.getCache().getString("gnome_recipe").equals(recipeStrings[TOAD_BATTA])) {
						give(player, ItemId.TOAD_BATTA.id(), 1);
						resetGnomeCooking(player);
					}

					                     
					else if (player.getCache().getString("gnome_recipe").equals(recipeStrings[TANGLED_TOADS_LEGS])) {
						give(player, ItemId.TANGLED_TOADS_LEGS.id(), 1);
						resetGnomeCooking(player);
					}

					               
					else if (gc.cookedID == ItemId.GNOMEBATTA.id() || gc.cookedID == ItemId.GNOMEBOWL.id()
						|| gc.cookedID == ItemId.GNOMECRUNCHIE.id()) {
						give(player, gc.cookedID, 1);
					}
				}
			}
			else {
				give(player, gc.burntID, 1);
				player.message(gc.messages[2]);
				resetGnomeCooking(player);
			}
		}
	}

	private boolean mouldDough(Item item, Player player) {
		if (player.getCarriedItems().hasCatalogID(ItemId.GNOMEBATTA_DOUGH.id(), Optional.of(false))
			|| player.getCarriedItems().hasCatalogID(ItemId.GNOMEBOWL_DOUGH.id(), Optional.of(false))
			|| player.getCarriedItems().hasCatalogID(ItemId.GNOMECRUNCHIE_DOUGH.id(), Optional.of(false))
			|| player.getCarriedItems().hasCatalogID(ItemId.GNOMEBATTA.id(), Optional.of(false))
			|| player.getCarriedItems().hasCatalogID(ItemId.GNOMEBOWL.id(), Optional.of(false))
			|| player.getCarriedItems().hasCatalogID(ItemId.GNOMECRUNCHIE.id(), Optional.of(false))) {
			mes("you need to finish, eat or drop the unfinished dish you hold");
			delay(3);
			player.message("before you can make another - giannes rules");
			return false;
		}
		player.message("which shape would you like to mould");
		int menu = multi(player,
			"gnomebatta",
			"gnomebowl",
			"gnomecrunchie");
		if (menu != -1) {
			player.setOption(-1);
			if (menu == 0) {
				if (player.getSkills().getLevel(Skill.COOKING.id()) < 25) {
					player.message("you need a cooking level of 25 to mould dough batta's");
					return false;
				}

				thinkbubble(item);
				player.getCarriedItems().remove(new Item(item.getCatalogId()));
				mes("you attempt to mould the dough into a gnomebatta");
				delay(5);
				player.message("You manage to make some gnome batta dough");
				give(player, ItemId.GNOMEBATTA_DOUGH.id(), 1);

				                                         
				addGnomeRecipeCache(player, -1, ItemId.GIANNE_DOUGH.id());

			} else if (menu == 1) {
				if (player.getSkills().getLevel(Skill.COOKING.id()) < 30) {
					player.message("you need a cooking level of 30 to mould dough bowls");
					return false;
				}

				thinkbubble(item);
				player.getCarriedItems().remove(new Item(item.getCatalogId()));
				mes("you attempt to mould the dough into a gnome bowl");
				delay(5);
				player.message("You manage to make some gnome bowl dough");
				give(player, ItemId.GNOMEBOWL_DOUGH.id(), 1);

				                                         
				addGnomeRecipeCache(player, -1, ItemId.GIANNE_DOUGH.id());

			} else if (menu == 2) {
				if (player.getSkills().getLevel(Skill.COOKING.id()) < 15) {
					player.message("you need a cooking level of 15 to mould crunchies");
					return false;
				}

				thinkbubble(item);
				player.getCarriedItems().remove(new Item(item.getCatalogId()));
				mes("you attempt to mould the dough into gnome crunchies");
				delay(5);
				player.message("You manage to make some gnome crunchies dough");
				give(player, ItemId.GNOMECRUNCHIE_DOUGH.id(), 1);

				                                         
				addGnomeRecipeCache(player, -1, ItemId.GIANNE_DOUGH.id());
			}
			player.incExp(Skill.COOKING.id(), 100, true);
		}
		return true;

	}

	@Override
	public void onOpInv(Player player, Integer invIndex, Item item, String command) {
		if (item.getCatalogId() == ItemId.GIANNE_DOUGH.id()) {
			mouldDough(item, player);
		}
	}

	@Override
	public boolean blockOpInv(Player player, Integer invIndex, Item item, String command) {
		return item.getCatalogId() == ItemId.GIANNE_DOUGH.id();
	}

	@Override
	public boolean blockUseLoc(Player player, GameObject obj, Item item) {
		return canCook(item, obj);
	}

	@Override
	public void onUseLoc(Player player, GameObject obj, Item item) {
		handleGnomeCooking(item, player, obj);
	}

	private boolean burnFood(Player player, int itemId, int myCookingLvl) {
		return Formulae.burnFood(player, itemId, myCookingLvl);
	}

	protected boolean addGnomeRecipeCache(final Player player, int baseId, int actionId) {
		String recipeString = "";

		                                    
		if (player.getCache().hasKey("gnome_recipe")) {
			recipeString = player.getCache().getString("gnome_recipe") + "-";
		}

		                                                             
		String baseIdString = "";
		if (baseId == -1)
			baseIdString = actionId + "";
		else
			baseIdString = actionId + "" + baseId;

		recipeString += baseIdString;

		                                                
		                                           
		                                                                           
		if (recipeString.length() == 3 && !recipeString.equals(ItemId.GNOMECRUNCHIE_DOUGH.id() + ""))
			return false;

		                                                                                                      
		String alternateRecipestring = "";
		for (String recipe : recipeStrings) {

			            
			if (recipe.equals(recipeString + "!")) {
				player.getCache().store("gnome_recipe", recipeString + "!");
				return true;
			}

			                     
			if (recipe.startsWith(recipeString)) {
				player.getCache().store("gnome_recipe", recipeString);
				return true;
			}

			if (alternateRecipestring.equals("") && recipe.startsWith(baseIdString)) {
				alternateRecipestring = baseIdString;
			}

		}

		player.getCache().store("gnome_recipe", baseIdString);
		return false;
	}

	enum GnomeCook {
		GNOME_BATTA_DOUGH(ItemId.GNOMEBATTA_DOUGH.id(), ItemId.GNOMEBATTA.id(), ItemId.BURNT_GNOMEBATTA.id(), 120, 1,
			"You cook the gnome batta in the oven...",
			"You remove the gnome batta from the oven",
			"You accidentally burn the gnome batta"),

		GNOME_BOWL_DOUGH(ItemId.GNOMEBOWL_DOUGH.id(), ItemId.GNOMEBOWL.id(), ItemId.BURNT_GNOMEBOWL.id(), 120, 1,
			"You cook the gnome bowl in the oven...",
			"You remove the gnome bowl from the oven",
			"You accidentally burn the gnome bbowl"),

		GNOME_CRUNCHIE_DOUGH(ItemId.GNOMECRUNCHIE_DOUGH.id(), ItemId.GNOMECRUNCHIE.id(), ItemId.BURNT_GNOMECRUNCHIE.id(), 120, 1,
			"You cook the gnome crunchie in the oven...",
			"You remove the gnome crunchie from the oven",
			"You accidentally burn the gnome crunchie"),

		GNOME_BATTA_ALREADY_COOKED(ItemId.GNOMEBATTA.id(), ItemId.GNOMEBATTA.id(), ItemId.BURNT_GNOMEBATTA.id(), 120, 1,
			"You cook the gnome batta in the oven...",
			"You remove the gnome batta from the oven",
			"You accidentally burn the gnome batta"),

		GNOME_BOWL_ALREADY_COOKED(ItemId.GNOMEBOWL.id(), ItemId.GNOMEBOWL.id(), ItemId.BURNT_GNOMEBOWL.id(), 120, 1,
			"You cook the gnome bowl in the oven...",
			"You remove the gnome bowl from the oven",
			"You accidentally burn the gnome bbowl");

		private int uncookedID;
		private int cookedID;
		private int burntID;
		private int experience;
		private int requiredLevel;
		private String[] messages;

		GnomeCook(int uncookedID, int cookedID, int burntID, int experience, int reqlevel, String... cookingMessages) {
			this.uncookedID = uncookedID;
			this.cookedID = cookedID;
			this.burntID = burntID;
			this.experience = experience;
			this.requiredLevel = reqlevel;
			this.messages = cookingMessages;
		}
	}
}
