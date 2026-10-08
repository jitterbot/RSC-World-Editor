package com.openrsc.server.model.container;

import com.openrsc.server.constants.IronmanMode;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.database.impl.mysql.queries.logging.DeathLog;
import com.openrsc.server.database.struct.PlayerInventory;
import com.openrsc.server.external.Gauntlets;
import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.Prayers;
import com.openrsc.server.model.struct.UnequipRequest;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Inventory {
	                                                                 
	   
                           
    
	private static final Logger LOGGER = LogManager.getLogger();
	   
                                            
    
	public static final int MAX_SIZE = 30;

	                                                      
	   
                                                    
    
	private List<Item> list = Collections.synchronizedList(new ArrayList<>());

	   
                                                        
    
	private Player player;

	                                                                  
	                                                                  
	public Inventory(Player player, PlayerInventory[] inventory) {
		this.player = player;
		for (int i = 0; i < inventory.length; i++) {
			Item item = new Item(inventory[i].itemId, inventory[i].item.getItemStatus());
			ItemDefinition itemDef = item.getDef(player.getWorld());
			if (item.isWieldable(player.getWorld()) && inventory[i].item.isWielded()) {
				if (itemDef != null) {
					if (!player.getConfig().WANT_EQUIPMENT_TAB) {
						item.getItemStatus().setWielded(true);
						player.updateWornItems(itemDef.getWieldPosition(), itemDef.getAppearanceId(), itemDef.getWearableId(), true);
					}
				}
			}
			list.add(item);
		}
	}
	                                                                  
	                                                                  

	                                                                  
	                                                                  
	public List<Item> getItems() {
		                                                                                                                                                                     
		synchronized (list) {
			return list;
		}
	}

	public ListIterator<Item> iterator() {
		synchronized (list) {
			return list.listIterator();
		}
	}

	                                                                  
	                                                                  
	public Boolean add(Item item) {
		return add(item, true);
	}

	public Boolean add(Item itemToAdd, boolean sendInventory) {
		synchronized (list) {
			final int MAXSTACK = !player.getConfig().SHORT_MAX_STACKS ? Integer.MAX_VALUE : (Short.MAX_VALUE - Short.MIN_VALUE);

			                                                             
			if (itemToAdd.getAmount() <= 0) {
				return false;
			}

			                              
			ItemDefinition itemDef = itemToAdd.getDef(player.getWorld());
			if (itemDef == null) {
				if (player.isEvent()) {
					player.message("Item def doesn't exist for item " + itemToAdd.getItemId());
				}
				return false;
			}

			                                   
			if (player.getConfig().RESTRICT_ITEM_ID >= 0 && player.getConfig().RESTRICT_ITEM_ID < itemToAdd.getCatalogId()) {
				if (player.isEvent()) {
					player.message("World doesn't allow itemid " + itemToAdd.getCatalogId() + "; only allows up to " + player.getConfig().RESTRICT_ITEM_ID);
				}
				return false;
			}

			if (player.getWorld().getPlayer(DataConversions.usernameToHash(player.getUsername())) == null) {
				return false;
			}

			if (player.getClientLimitations().maxItemId < itemToAdd.getCatalogId()) {
				player.message("Your client could not receive " + itemToAdd.getDef(player.getWorld()).getName() + ", it drops to the ground!");
				player.getWorld().registerItem(
					new GroundItem(player.getWorld(), itemToAdd.getCatalogId(), player.getX(), player.getY(),
						itemToAdd.getAmount(), player, itemToAdd.getNoted()),
					player.getConfig().GAME_TICK * 150);

				return false;
			}

			                                                                      
			Item existingStack = null;
			int index = -1;
			if (itemDef.isStackable() || itemToAdd.getNoted()) {
				for (Item inventoryItem : list) {
					++index;
					                              
					if (inventoryItem.getCatalogId() != itemToAdd.getCatalogId())
						continue;

					                                 
					if (itemToAdd.getNoted() != inventoryItem.getNoted())
						continue;

					                                     
					if (inventoryItem.getAmount() == MAXSTACK)
						continue;

					existingStack = inventoryItem;
					break;
				}
			}

			                                                                           
			if (existingStack == null) {

				                   
				itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount(), itemToAdd.getNoted());

				                                            
				if (list.size() >= MAX_SIZE) {
					if (player.getConfig().MESSAGE_FULL_INVENTORY) {
						player.message("Your Inventory is full, the " + itemToAdd.getDef(player.getWorld()).getName() + " drops to the ground!");
					}
					player.getWorld().registerItem(
						new GroundItem(player.getWorld(), itemToAdd.getCatalogId(), player.getX(), player.getY(),
							itemToAdd.getAmount(), player, itemToAdd.getNoted()),
							player.getConfig().GAME_TICK * 150);

					return false;
				}

				                                                                     
				long itemID = player.getWorld().getServer().getDatabase().incrementMaxItemId(player);
				itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount(), itemToAdd.getNoted(), itemID);

				                              
				list.add(itemToAdd);

				                   
				if (sendInventory)
					ActionSender.sendInventoryUpdateItem(player, list.size() - 1);

			                                                                         
			} else {

				                                                 
				int remainingSize = MAXSTACK - existingStack.getAmount();

				                                                                        
				if (remainingSize >= itemToAdd.getAmount()) {

					                                           
					existingStack.changeAmount(itemToAdd.getAmount());

					                   
					if (sendInventory)
						ActionSender.sendInventoryUpdateItem(player, index);

				                                                                                        
				} else {

					                                  
					                   
					itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount() - remainingSize);

					                                                                  
					if (list.size() >= MAX_SIZE) {
						if (player.getConfig().MESSAGE_FULL_INVENTORY) {
							player.message("Your Inventory is full, the " + itemToAdd.getDef(player.getWorld()).getName() + " drops to the ground!");
						}
						player.getWorld().registerItem(
							new GroundItem(player.getWorld(), itemToAdd.getCatalogId(), player.getX(), player.getY(),
								itemToAdd.getAmount(), player, itemToAdd.getNoted()),
								player.getConfig().GAME_TICK * 150);
						return false;
					}

					                                                
					existingStack.setAmount(MAXSTACK);
					long itemID = player.getWorld().getServer().getDatabase().incrementMaxItemId(player);
					itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount(), itemToAdd.getNoted(), itemID);

					                              
					list.add(itemToAdd);

					                                  
					ActionSender.sendInventoryUpdateItem(player, index);
					ActionSender.sendInventoryUpdateItem(player, list.size() - 1);
				}
			}
			return true;
		}
	}

	public long remove(Item item, boolean sendInventory) {
		return remove(item, sendInventory, false);
	}

	public long remove(Item item, boolean sendInventory, boolean bypassItemId) {
		synchronized (list) {
			                                       
			if (list.isEmpty())
				return -1;

			int catalogId = item.getCatalogId();
			int amount = item.getAmount();
			long itemID = item.getItemId();

			if (itemID == -1) {
				return -1;
			}

			int size = list.size();
			ListIterator<Item> iterator = list.listIterator(size);
			boolean continueRemoval = bypassItemId;
			int amountToRemove = amount;
			for (int index = size - 1; iterator.hasPrevious(); index--) {
				Item inventoryItem = iterator.previous();
				                                       
				                                                                              
				                                                                              
				if ((inventoryItem.getItemId() != itemID && !continueRemoval) || (continueRemoval &&
					(inventoryItem.getCatalogId() != catalogId || inventoryItem.getNoted() != item.getNoted())))
					continue;
				                          
				ItemDefinition inventoryDef = inventoryItem.getDef(player.getWorld());
				if (inventoryDef == null)
					continue;

				if (inventoryDef.isStackable() || inventoryItem.getNoted()) {

					                                                                 
					if (inventoryItem.getAmount() < amountToRemove) {
						amountToRemove -= inventoryItem.getAmount();

						                    
						iterator.remove();

						                    
						if (sendInventory)
							ActionSender.sendRemoveItem(player, index);

						continueRemoval = true;
					}

					                                                         
					else if (inventoryItem.getAmount() == amountToRemove) {
						amountToRemove -= inventoryItem.getAmount();

						                    
						iterator.remove();

						                    
						if (sendInventory)
							ActionSender.sendRemoveItem(player, index);

						continueRemoval = amountToRemove > 0;

					                                  
					} else {

						                                      
						inventoryItem.changeAmount(-amountToRemove);
						amountToRemove = 0;

						                    
						if (sendInventory)
							ActionSender.sendInventoryUpdateItem(player, index);

						continueRemoval = false;
					}

				                     
				} else {

					                                                                              

					                                                                 
					if (inventoryItem.isWielded())
						player.getCarriedItems().getEquipment().unequipItem(
							new UnequipRequest(player, inventoryItem, UnequipRequest.RequestType.FROM_INVENTORY, false)
						);

					amountToRemove -= inventoryItem.getAmount();

					                         
					iterator.remove();

					                    
					if (sendInventory)
						ActionSender.sendRemoveItem(player, index);

					continueRemoval = amountToRemove > 0;
				}

				if (!continueRemoval) return inventoryItem.getItemId();
			}
			if (continueRemoval) {
				                            
				return -1;
			}
			System.out.println("Item not found: " + item.getItemId() + " for player " + player.getUsername());
		}
		return -1;
	}

	public void replace(Item itemToReplace, Item newItem, boolean sendInventory) {

	}

	                                               
	public void swap(int slot, int to) {
		if (slot < 0 || to < 0 || to == slot) {
			return;
		}

		final int invSize = list.size();

		if (slot >= invSize || to >= invSize) {
			return;
		}

		final Item item1 = get(slot);
		final Item item2 = get(to);

		if (item1 == null || item2 == null)
		{
			return;
		}

		list.set(slot, item2);
		list.set(to, item1);
		ActionSender.sendInventory(player);
	}

	                                                 
	public boolean insert(int slot, int to) {
		if (slot < 0 || to < 0 || to == slot) {
			return false;
		}

		final int invSize = list.size();

		if (slot >= invSize || to >= invSize) {
			return false;
		}

		final Item item = list.get(slot);

		if (item == null) {
			return false;
		}

		final Item[] array = list.toArray(new Item[0]);
		array[slot] = null;

		if (slot > to) {
			int shiftFrom = to;
			int shiftTo = slot;
			for (int i = (to + 1); i < slot; i++) {
				if (array[i] == null) {
					shiftTo = i;
					break;
				}
			}
			Item[] slice = new Item[shiftTo - shiftFrom];
			System.arraycopy(array, shiftFrom, slice, 0, slice.length);
			System.arraycopy(slice, 0, array, shiftFrom + 1, slice.length);
		} else {
			int sliceStart = slot + 1;
			int sliceEnd = to;
			for (int i = (sliceEnd - 1); i >= sliceStart; i--) {
				if (array[i] == null) {
					sliceStart = i;
					break;
				}
			}
			Item[] slice = new Item[sliceEnd - sliceStart + 1];
			System.arraycopy(array, sliceStart, slice, 0, slice.length);
			System.arraycopy(slice, 0, array, sliceStart - 1, slice.length);
		}

		array[to] = item;
		list = new ArrayList<Item>(Arrays.asList(array));
		return true;
	}

	public void dropOnDeath(final Mob mob) {
		                                                  
		                                                            
		final TreeMap<Integer, ArrayList<Item>> deathItemsMap = new TreeMap<>(Collections.reverseOrder());

		                                                       
		final ArrayList<Item> deathItemsList = new ArrayList<>();

		Integer key;
		ArrayList<Item> value;
		ItemDefinition def;

		                                                                               
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			final Equipment equipment = player.getCarriedItems().getEquipment();

			for (int i = 0; i < Equipment.SLOT_COUNT; i++) {
				final Item item = equipment.get(i);

				if (item == null) continue;

				def = item.getDef(player.getWorld());
				key = def.isStackable() || item.getNoted() ? -1 : def.getDefaultPrice();                           
				value = deathItemsMap.getOrDefault(key, new ArrayList<>());
				value.add(item);
				deathItemsMap.put(key, value);
			}
		}

		                                                  
		for (final Item item : getItems()) {
			def = item.getDef(player.getWorld());
			key = def.isStackable() || item.getNoted() ? -1 : def.getDefaultPrice();                           
			value = deathItemsMap.getOrDefault(key, new ArrayList<>());
			value.add(item);
			deathItemsMap.put(key, value);
		}

		deathItemsMap.values().forEach((list) -> {
			list.sort((c1, c2) -> {
				if (c1.getCatalogId() != c2.getCatalogId()) {
					return c1.getCatalogId() - c2.getCatalogId();
				}
				return c1.getAmount() - c2.getAmount();
			});
			deathItemsList.addAll(list);
		});
		deathItemsMap.clear();

		final ListIterator<Item> iterator = deathItemsList.listIterator();

		                                                           
		if (!player.isSkulled() && !player.isIronMan(IronmanMode.Ultimate.id())) {
			for (int items = 1; items <= 3 && iterator.hasNext(); items++) {
				if (iterator.next().getDef(player.getWorld()).isStackable()) {
					iterator.previous();
					break;
				}
			}
		}

		                                                       
		if (player.getPrayers().isPrayerActivated(Prayers.PROTECT_ITEMS) && iterator.hasNext()) {
			if (iterator.next().getDef(player.getWorld()).isStackable()) {
				iterator.previous();
			}
		}

		final DeathLog deathLog = new DeathLog(player, mob, false);

		                                                                                         
		while (iterator.hasNext()) {
			Item item = iterator.next();
			item = new Item(item.getCatalogId(), item.getAmount(), item.getNoted());

			                                                     
			if (player.getCarriedItems().remove(item, false) == -1) continue;

			                       
			deathLog.addDroppedItem(item);

			                                            
			if (item.getDef(player.getWorld()).isUntradable()) {
				final GroundItem groundItem = new GroundItem(player.getWorld(), item.getCatalogId(), player.getX(),
					player.getY(), item.getAmount(), player, item.getNoted());
				player.getWorld().registerItem(groundItem, player.getConfig().GAME_TICK * 1000);
				continue;
			}

			                                          
			if (mob == null || mob.isNpc()) {
				final GroundItem groundItem = new GroundItem(player.getWorld(), item.getCatalogId(), player.getX(),
					player.getY(), item.getAmount(), null, item.getNoted());
				player.getWorld().registerItem(groundItem, player.getConfig().GAME_TICK * 1000);

				groundItem.setAttribute("killedByMob", player.getUsernameHash());

				continue;
			}

			final Player playerMob = (Player) mob;

			                                                                     
			final Player dropOwner = playerMob.getIronMan() == IronmanMode.None.id() ? playerMob : player;

			final GroundItem groundItem = new GroundItem(player.getWorld(), item.getCatalogId(), player.getX(),
				player.getY(), item.getAmount(), dropOwner, item.getNoted());

			groundItem.setAttribute("playerKill", true);
			groundItem.setAttribute("killerHash", playerMob.getUsernameHash());

			player.getWorld().registerItem(groundItem, player.getConfig().GAME_TICK * 1000);
		}

		deathLog.build();
		player.getWorld().getServer().getGameLogger().addQuery(deathLog);

		                                                                                                        
		                                                                        
		if (player.getQuestStage(Quests.FAMILY_CREST) == -1) {
			if (player.getCache().hasKey("famcrest_gauntlets")) {
				final int itemId = Gauntlets.getById(player.getCache().getInt("famcrest_gauntlets")).catalogId();

				if (!player.getBank().hasItemId(itemId) && !player.getCarriedItems().hasCatalogID(itemId)) {
					add(new Item(itemId, 1), false);
				}
			} else {
				                                                   
				player.setSuspiciousPlayer(true, "Missing famcrest_gauntlets cache key but quest is completed.");
			}
		}

		ActionSender.sendInventory(player);
		ActionSender.sendEquipmentStats(player);
		player.getUpdateFlags().setAppearanceChanged(true);
	}

	                                                                  
                                                                      
	public Item get(int index) {
		synchronized (list) {
			if (index < 0 || index >= list.size()) {
				return null;
			}
			return list.get(index);
		}
	}

	public Item get(Item item) {
		synchronized (list) {
			for (int index = list.size() - 1; index >= 0; index--) {
				if (list.get(index).equals(item) && list.get(index).getAmount() >= item.getAmount()) {
					return list.get(index);
				}
			}
		}
		return null;
	}

	public boolean contains(Item i) {
		                       
		                           
		   
		return hasCatalogID(i.getCatalogId());
	}

	public int countId(int id) {
		return countId(id, Optional.of(false));
	}

	public int countId(int id, Optional<Boolean> noted) {
		synchronized (list) {
			int temp = 0;
			for (Item i : list) {
				if (i.getCatalogId() == id && (!noted.isPresent() || (i.getNoted() == noted.get()))) {
					final int amount = i.getAmount();
					if (amount > Integer.MAX_VALUE - temp)
						return Integer.MAX_VALUE;
					temp += amount;
				}
			}
			return temp;
		}
	}

	public int countSlotsOccupied(Item item, int totalAmount) {
		synchronized (list) {
			int slots = 0;
			int amountFound = 0;
			for (int x = list.size() - 1; x >= 0; x--) {
				if (amountFound >= totalAmount) break;
				Item i = list.get(x);
				if (i.getCatalogId() == item.getCatalogId() && i.getItemStatus().getNoted() == item.getItemStatus().getNoted()) {
					slots++;
					amountFound += i.getAmount();
				}
			}
			return slots;
		}
	}

	public int getLastIndexById(int id) {
		return getLastIndexById(id, Optional.empty());
	}

	public int getLastIndexById(int id, Optional<Boolean> wantNoted) {
		synchronized (list) {
			for (int index = list.size() - 1; index >= 0; index--) {
				Item item = list.get(index);
				if (item.getCatalogId() == id && (!wantNoted.isPresent() || item.getNoted() == wantNoted.get())) {
					return index;
				}
			}
		}
		return -1;
	}

	public boolean hasInInventory(int id) {
		synchronized (list) {
			for (Item i : list) {
				if (i.getCatalogId() == id)
					return true;
			}
		}
		return false;
	}

	public boolean hasCatalogID(int id) {
		synchronized (list) {
			for (Item i : list) {
				if (i.getCatalogId() == id)
					return true;
			}
		}

		if (player.getConfig().WANT_EQUIPMENT_TAB)
			return player.getCarriedItems().getEquipment().searchEquipmentForItem(id) != -1;
		else
			return false;
	}

	public boolean hasCatalogID(int id, boolean noted) {
		synchronized (list) {
			for (Item i : list) {
				if (i.getCatalogId() == id && i.getNoted() == noted)
					return true;
			}
		}

		if (player.getConfig().WANT_EQUIPMENT_TAB)
			return player.getCarriedItems().getEquipment().searchEquipmentForItem(id) != -1;
		else
			return false;
	}
	                                                                  
	                                                                  
	                                                                  
	public boolean canHold(int itemCatelogId, int amount) {
		synchronized (list) {
			return (MAX_SIZE - list.size()) >= getRequiredSlots(itemCatelogId, amount, false);
		}
	}

	public boolean canHold(Item item) {
		synchronized (list) {
			return (MAX_SIZE - list.size()) >= getRequiredSlots(item);
		}
	}

	public boolean canHold(Item item, int addition) {
		synchronized (list) {
			return (MAX_SIZE - list.size() + addition) >= getRequiredSlots(item);
		}
	}

	public boolean full() {
		synchronized (list) {
			return list.size() >= MAX_SIZE;
		}
	}

	public int getFreedSlots(Item item) {
		return ((item.getDef(player.getWorld()).isStackable() || item.getNoted()) && countId(item.getCatalogId(), Optional.of(item.getNoted())) > item.getAmount() ? 0 : 1);
	}

	public int getFreedSlots(List<Item> items) {
		int freedSlots = 0;
		for (Item item : items) {
			freedSlots += getFreedSlots(item);
		}
		return freedSlots;
	}

	public int getRequiredSlots(final Item item) {
		return this.getRequiredSlots(item.getCatalogId(), item.getAmount(), item.getNoted());
	}

	public int getRequiredSlots(final int itemCatalogId, final int itemAmount, final boolean isNoted) {
		synchronized (this.list) {
			final int maxItemStack = this.player.getConfig().SHORT_MAX_STACKS ?
				(Short.MAX_VALUE - Short.MIN_VALUE) :
				Integer.MAX_VALUE;

			final ItemDefinition itemDef = this.player.getWorld()
				.getServer()
				.getEntityHandler()
				.getItemDef(itemCatalogId);

			                                            
			if (itemDef == null) return maxItemStack;

			if (!itemDef.isStackable() && !isNoted) return itemAmount;

			                           
			for (final Item inventoryItem : this.list) {
				if (inventoryItem.getCatalogId() == itemCatalogId &&
					inventoryItem.getNoted() == isNoted &&
					inventoryItem.getAmount() != maxItemStack)
					return itemAmount > maxItemStack - inventoryItem.getAmount() ? 1 : 0;
			}

			                    
			return 1;
		}
	}

	public int getRequiredSlots(List<Item> items) {
		int requiredSlots = 0;
		for (Item item : items) {
			requiredSlots += getRequiredSlots(item);
		}
		return requiredSlots;
	}

	public int size() {
		synchronized (list) {
			return list.size();
		}
	}

	public int getFreeSlots() {
		return MAX_SIZE - size();
	}
	                                                                  
	                                                                  
	public void sort() {
		synchronized (list) {
			Collections.sort(list);
		}
	}
	                                                                  
}
