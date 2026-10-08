package com.openrsc.server.content.market.task;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.content.market.MarketItem;
import com.openrsc.server.database.GameDatabaseException;
import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.DiscordService;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Optional;

public class NewMarketItemTask extends MarketTask {

	   
                            
    
	private static final Logger LOGGER = LogManager.getLogger();

	private MarketItem newItem;
	private Player owner;
	private ArrayList<Item> itemsToAuction;

	public NewMarketItemTask(Player player, MarketItem item) {
		this.owner = player;
		this.newItem = item;
	}

	@Override
	public void doTask() {
		ItemDefinition def = owner.getWorld().getServer().getEntityHandler().getItemDef(newItem.getCatalogID());

		boolean updateDiscord = false;

		if (newItem.getCatalogID() == ItemId.COINS.id() || def.isUntradable()) {
			ActionSender.sendBox(owner, "@red@[Auction House - Error] % @whi@ You cannot sell that item on auction house!", false);
			return;
		}
		if (newItem.getPrice() < 1) {
			ActionSender.sendBox(owner, "@red@[Auction House - Error] % @whi@ Price must be greater than zero", false);
			return;
		}
		if (newItem.getAmount() < 1) {
			ActionSender.sendBox(owner, "@red@[Auction House - Error] % @whi@ Amount must be greater than zero", false);
			return;
		}

		                                              
		if (owner.getCarriedItems().getInventory().countId(newItem.getCatalogID(), Optional.empty()) < newItem.getAmount()) {
			return;
		}

		                     
		                                                   
                 
               

                    
                                                                                                                      
                                                                                                                                                     
           
           
                                                                       
                                                                
                                                      
                                         
            
                                                                                                                                                                                                                                                          
            
     
    
     

		if (owner.getWorld().getPlayer(DataConversions.usernameToHash(owner.getUsername())) == null) {
			return;
		}

		                                                        
		this.itemsToAuction = new ArrayList<>();
		int totalAuctionCount = 0;
		for (Item x : owner.getCarriedItems().getInventory().getItems()) {
			if (x.getCatalogId() != newItem.getCatalogID()) continue;
			if (x.getAmount() + totalAuctionCount == newItem.getAmount()) {
				totalAuctionCount += x.getAmount();
				this.itemsToAuction.add(x);
				break;
			}
			else if (x.getAmount() + totalAuctionCount > newItem.getAmount()) {
				int partialAmount = newItem.getAmount() - totalAuctionCount;
				totalAuctionCount += partialAmount;
				this.itemsToAuction.add(new Item(x.getCatalogId(), partialAmount, x.getNoted(), x.getItemId()));
				break;
			}
			else if (x.getAmount() + totalAuctionCount < newItem.getAmount()) {
				totalAuctionCount += x.getAmount();
				this.itemsToAuction.add(x);
			}
		}

		                  
		if (this.itemsToAuction.size() == 0) return;

		                           
		for (Item x : this.itemsToAuction) {
			owner.getCarriedItems().remove(x);
		}

		try {
		owner.getWorld().getServer().getDatabase().newAuction(newItem);
		                                                                                                                                                                                                                                                                 
		ActionSender.sendBox(owner, "@gre@[Auction House - Success] % @whi@ Auction has been listed % " + newItem.getAmount() + "x @yel@" + def.getName() + " @whi@for @yel@" + newItem.getPrice() + "gp", false);
		updateDiscord = true;
		} catch (GameDatabaseException e) {
			Item item = new Item(newItem.getCatalogID(), newItem.getAmount());
			if (item.getDef(owner.getWorld()).isStackable()) {
				for (int i = 0; i < newItem.getAmount(); i++) {
					owner.getCarriedItems().getInventory().add(new Item(newItem.getCatalogID(), 1));
				}
			} else {
				owner.getCarriedItems().getInventory().add(new Item(newItem.getCatalogID(), newItem.getAmount()));
			}
			ActionSender.sendBox(owner, "@red@[Auction House - Error] % @whi@ Failed to add item to Auction. % Item(s) have been returned to your inventory.", false);
			LOGGER.catching(e);
		}
		owner.save();
		owner.getWorld().getMarket().addRequestOpenAuctionHouseTask(owner);
		if (updateDiscord) {
			DiscordService ds = owner.getWorld().getServer().getDiscordService();
			if (ds != null) {
				ds.auctionAdd(newItem);
			}
		}
	}
}
