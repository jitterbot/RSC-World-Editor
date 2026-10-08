package com.openrsc.server.plugins.authentic.minigames.gnomebar;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpInvTrigger;
import com.openrsc.server.plugins.triggers.UseLocTrigger;

import java.util.Optional;

import static com.openrsc.server.plugins.Functions.*;

public class GnomeBartending implements OpInvTrigger, UseLocTrigger {

	private boolean canHeat(Item item, GameObject object) {
		if ((item.getCatalogId() == ItemId.FULL_COCKTAIL_GLASS.id() || item.getCatalogId() == ItemId.HALF_COCKTAIL_GLASS.id()
			|| item.getCatalogId() == ItemId.ODD_LOOKING_COCKTAIL.id()) && inArray(object.getID(), 119)) {
			return true;
		}
		return false;
	}

	protected int FRUIT_BLAST = 0;
	protected int PINEAPPLE_PUNCH = 1;
	protected int DRUNK_DRAGON = 2;
	protected int SHORT_GREEN_GUY = 3;
	protected int CHOC_SATURDAY = 4;
	protected int BLURBERRY_SPECIAL = 5;
	protected int WIZARD_BLIZZARD = 6;

	protected String[] recipeStrings = {
		                
		String.format("%d%d-%d%d-%d%d-%d-%d%d!",
			ItemId.LEMON.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.ORANGE.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.FRESH_PINEAPPLE.id(), ItemId.COCKTAIL_SHAKER.id(),                                            
			ItemId.COCKTAIL_SHAKER.id(),                              
			ItemId.LEMON_SLICES.id(), ItemId.FULL_COCKTAIL_GLASS.id()),                                    

		                    
		String.format("%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d%d-%d%d!",
			ItemId.FRESH_PINEAPPLE.id(), ItemId.COCKTAIL_SHAKER.id(),                                          
			ItemId.FRESH_PINEAPPLE.id(), ItemId.COCKTAIL_SHAKER.id(),                                          
			ItemId.LEMON.id(), ItemId.COCKTAIL_SHAKER.id(),                                        
			ItemId.ORANGE.id(), ItemId.COCKTAIL_SHAKER.id(),                                        
			ItemId.COCKTAIL_SHAKER.id(),                              
			ItemId.PINEAPPLE_CHUNKS.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                       
			ItemId.LIME_CHUNKS.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                    
			ItemId.LIME_SLICES.id(), ItemId.FULL_COCKTAIL_GLASS.id()),                                    

		                 
		String.format("%d%d-%d%d-%d%d-%d-%d%d-%d%d-%d!",
			ItemId.VODKA.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.GIN.id(), ItemId.COCKTAIL_SHAKER.id(),                                    
			ItemId.DWELLBERRIES.id(), ItemId.COCKTAIL_SHAKER.id(),                                               
			ItemId.COCKTAIL_SHAKER.id(),                              
			ItemId.PINEAPPLE_CHUNKS.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                        
			ItemId.CREAM.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                            
			ItemId.FULL_COCKTAIL_GLASS.id()),                          

		                          
		String.format("%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d%d!",
			ItemId.VODKA.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.LIME.id(), ItemId.COCKTAIL_SHAKER.id(),                                     
			ItemId.LIME.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.LIME.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.COCKTAIL_SHAKER.id(),                              
			ItemId.EQUA_LEAVES.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                  
			ItemId.LIME_SLICES.id(), ItemId.FULL_COCKTAIL_GLASS.id()),                                    

		                  
		String.format("%d%d-%d%d-%d%d-%d-%d%d-%d-%d%d-%d%d!",
			ItemId.WHISKY.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.MILK.id(), ItemId.COCKTAIL_SHAKER.id(),                                     
			ItemId.EQUA_LEAVES.id(), ItemId.COCKTAIL_SHAKER.id(),                                              
			ItemId.COCKTAIL_SHAKER.id(),                              
			ItemId.CHOCOLATE_BAR.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                 
			ItemId.FULL_COCKTAIL_GLASS.id(),                         
			ItemId.CREAM.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                              
			ItemId.CHOCOLATE_DUST.id(), ItemId.FULL_COCKTAIL_GLASS.id()),                                       

		                      
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d%d-%d%d-%d%d!",
			ItemId.VODKA.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.GIN.id(), ItemId.COCKTAIL_SHAKER.id(),                                    
			ItemId.BRANDY.id(), ItemId.COCKTAIL_SHAKER.id(),                                         
			ItemId.LEMON.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.LEMON.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.ORANGE.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.COCKTAIL_SHAKER.id(),                               
			ItemId.DICED_ORANGE.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                      
			ItemId.DICED_LEMON.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                    
			ItemId.LIME_SLICES.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                  
			ItemId.EQUA_LEAVES.id(), ItemId.FULL_COCKTAIL_GLASS.id()),                                     

		                    
		String.format("%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d%d-%d-%d%d-%d%d!",
			ItemId.FRESH_PINEAPPLE.id(), ItemId.COCKTAIL_SHAKER.id(),                                          
			ItemId.ORANGE.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.LEMON.id(), ItemId.COCKTAIL_SHAKER.id(),                                        
			ItemId.LIME.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.VODKA.id(), ItemId.COCKTAIL_SHAKER.id(),                                       
			ItemId.VODKA.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.GIN.id(), ItemId.COCKTAIL_SHAKER.id(),                                      
			ItemId.COCKTAIL_SHAKER.id(),                               
			ItemId.PINEAPPLE_CHUNKS.id(), ItemId.FULL_COCKTAIL_GLASS.id(),                                        
			ItemId.LIME_SLICES.id(), ItemId.FULL_COCKTAIL_GLASS.id()),                                  
	};

	private void handleCocktailHeating(final Item item, Player player, final GameObject object) {
		mes("you briefly place the drink in the oven");
		delay(3);
		player.message("you remove the warm drink");
		if (item.getCatalogId() == ItemId.FULL_COCKTAIL_GLASS.id()) {
			boolean recipeSuccess = addCocktailRecipeCache(player, -1, item.getCatalogId());                 
			if (recipeSuccess) {
				               
				if (player.getCache().getString("cocktail_recipe").equals(recipeStrings[DRUNK_DRAGON])) {
					player.getCarriedItems().remove(new Item(item.getCatalogId()));
					give(player, ItemId.DRUNK_DRAGON.id(), 1);
					resetGnomeBartending(player);
				}
			} else {
				player.getCarriedItems().remove(new Item(item.getCatalogId()));
				player.getCarriedItems().getInventory().add(new Item(ItemId.ODD_LOOKING_COCKTAIL.id()));
				resetGnomeBartending(player);
			}
		}
	}

	private void pourGlass(Item item, Player player) {
		if (player.getCarriedItems().hasCatalogID(ItemId.COCKTAIL_GLASS.id(), Optional.of(false))) {
			String recipe = "";
			if (player.getCache().hasKey("cocktail_recipe")) {
				recipe = player.getCache().getString("cocktail_recipe");
			}
			if (!recipe.isEmpty() && !recipe.contains("-" + ItemId.COCKTAIL_SHAKER.id())) {
				boolean full = false;
				for (String chkRecipe : recipeStrings) {
					if (chkRecipe.startsWith(recipe + "-" + ItemId.COCKTAIL_SHAKER.id())) {
						full = true;
						break;
					}
				}
				player.getCarriedItems().remove(new Item(ItemId.COCKTAIL_GLASS.id()));
				if (full) {
					player.getCarriedItems().getInventory().add(new Item(ItemId.FULL_COCKTAIL_GLASS.id()));
				} else {
					player.getCarriedItems().getInventory().add(new Item(ItemId.HALF_COCKTAIL_GLASS.id()));
				}
				addCocktailRecipeCache(player, -1, item.getCatalogId());
				mes("you pour the contents into a glass");
			} else {
				mes("you need to put some contents into the shaker");
			}
			delay();
		} else {
			player.message("first you'll need a glass to pour the drink into");
		}
	}

	@Override
	public void onOpInv(Player player, Integer invIndex, Item item, String command) {
		if (item.getCatalogId() == ItemId.COCKTAIL_SHAKER.id()) {
			pourGlass(item, player);
		}
	}

	@Override
	public boolean blockOpInv(Player player, Integer invIndex, Item item, String command) {
		return item.getCatalogId() == ItemId.COCKTAIL_SHAKER.id();
	}

	@Override
	public boolean blockUseLoc(Player player, GameObject obj, Item item) {
		return canHeat(item, obj);
	}

	@Override
	public void onUseLoc(Player player, GameObject obj, Item item) {
		handleCocktailHeating(item, player, obj);
	}

	protected boolean addCocktailRecipeCache(final Player player, int baseId, int actionId) {
		String recipeString = "";

		                                    
		if (player.getCache().hasKey("cocktail_recipe")) {
			recipeString = player.getCache().getString("cocktail_recipe") + "-";
		}

		                                                             
		String baseIdString = "";
		if (baseId == -1)
			baseIdString = actionId + "";
		else
			baseIdString = actionId + "" + baseId;

		recipeString += baseIdString;

		                                                                                                            
		String alternateRecipestring = "";
		for (String recipe : recipeStrings) {

			            
			if (recipe.equals(recipeString + "!")) {
				player.getCache().store("cocktail_recipe", recipeString + "!");
				return true;
			}

			                     
			if (recipe.startsWith(recipeString)) {
				player.getCache().store("cocktail_recipe", recipeString);
				return true;
			}

			if (alternateRecipestring.equals("") && recipe.startsWith(baseIdString)) {
				alternateRecipestring = baseIdString;
			}

		}

		player.getCache().store("cocktail_recipe", baseIdString);
		return false;
	}
}
