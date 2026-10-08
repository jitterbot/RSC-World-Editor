package com.openrsc.server.model.container;

import com.openrsc.server.constants.IronmanMode;
import com.openrsc.server.constants.ItemId;
import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.struct.UnequipRequest;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.openrsc.server.plugins.Functions.validatebankpin;


public class Bank {
	   
                            
    
	private static final Logger LOGGER = LogManager.getLogger();

	                                                      
	private List<Item> list = Collections.synchronizedList(new ArrayList<>());
	private final Player player;
	private final BankPreset[] bankPresets;

	public Bank(final Player player) {
		this.player = player;
		this.bankPresets = new BankPreset[BankPreset.PRESET_COUNT];
		for (int i = 0; i < bankPresets.length; ++i)
			bankPresets[i] = new BankPreset(player);
	}

	   
                                                  
                         
    
	public boolean add(Item itemToAdd) { return add(itemToAdd, true); }

	public boolean add(Item itemToAdd, boolean updateClient) {
		synchronized(list) {
			                         
			if (itemToAdd.getAmount() <= 0) {
				return false;
			}

			                                                                
			Item existingStack = null;
			int index = -1;

			for (Item bankItem : list) {
				++index;
				                                  
				if (bankItem.getCatalogId() != itemToAdd.getCatalogId())
					continue;

				                                                 
				if (bankItem.getAmount() == Integer.MAX_VALUE)
					continue;

				                                                  
				existingStack = bankItem;
				break;
			}

			if (player.getWorld().getPlayer(DataConversions.usernameToHash(player.getUsername())) == null) {
				return false;
			}

			                                                                   
			if (existingStack == null) {
				                                       
				if (list.size() >= player.getWorld().getMaxBankSize())
					return false;

				itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount());
				long itemID = player.getWorld().getServer().getDatabase().incrementMaxItemId(player);

				itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount(), false, itemID);

				                         
				list.add(itemToAdd);

				                         
				if (updateClient) {
					ActionSender.updateBankItem(player, list.size() - 1, itemToAdd, itemToAdd.getAmount());
				}

			                                                   
			} else {

				                                                                                        
				int remainingSize = Integer.MAX_VALUE - existingStack.getAmount();

				                                                                         
				if (remainingSize >= itemToAdd.getAmount()) {

					                                      
					existingStack.changeAmount(itemToAdd.getAmount());

					                         
					if (updateClient) {
						ActionSender.updateBankItem(player, index, existingStack, existingStack.getAmount());
					}

				                                                                                                 
				} else {

					                                                                   
					existingStack.setAmount(Integer.MAX_VALUE);

					                                                                                      
					itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount() - remainingSize);
					long itemID = player.getWorld().getServer().getDatabase().incrementMaxItemId(player);

					itemToAdd = new Item(itemToAdd.getCatalogId(), itemToAdd.getAmount(), false, itemID);

					                                        
					list.add(itemToAdd);

					                                  
					if (updateClient) {
						ActionSender.updateBankItem(player, index, existingStack, Integer.MAX_VALUE);
						ActionSender.updateBankItem(player, list.size() - 1, itemToAdd, itemToAdd.getAmount());
					}
				}
			}
			return true;
		}
	}

	public boolean remove(Item item) {
		return remove(item, true);
	}

	public boolean remove(final Item item, final boolean updateClient) {
		return this.remove(item.getCatalogId(), item.getAmount(), updateClient);
	}

	public boolean remove(int catalogID, int amount) {
		return remove(catalogID, amount, true);
	}

	public boolean remove(final int catalogID, final int amount, final boolean updateClient) {
		synchronized(this.list) {
			if (this.player.getWorld().getPlayer(DataConversions.usernameToHash(this.player.getUsername())) == null)
				return false;

			int bankItemIndex;
			Item bankItem = null;

			for (bankItemIndex = this.list.size() - 1; bankItemIndex >= 0; bankItemIndex--) {
				final Item item = this.list.get(bankItemIndex);

				if (item.getCatalogId() == catalogID) {
					bankItem = item;
					break;
				}
			}

			if (bankItem == null) return false;

			final int amountToRemove = Math.min(amount, bankItem.getAmount());

			if (amountToRemove == bankItem.getAmount()) {
				this.list.remove(bankItemIndex);
				if (updateClient)
					ActionSender.updateBankItem(this.player, bankItemIndex, bankItem, 0);
			} else {
				bankItem.setAmount(bankItem.getAmount() - amountToRemove);

				if (updateClient)
					ActionSender.updateBankItem(this.player, bankItemIndex, bankItem, bankItem.getAmount());
			}

			return true;
		}
	}

	public boolean canRemoveAtLeast1(int catalogID) {
		int bankIndex = getFirstIndexById(catalogID);
		Item bankItem = get(bankIndex);

		                                                
		if (bankItem == null) return false;

		if (player.getWorld().getPlayer(DataConversions.usernameToHash(player.getUsername())) == null) {
			return false;
		}

		return true;
	}

	public boolean canHold(Item item) {
		synchronized(list) {
			return (getPlayer().getWorld().getMaxBankSize() - list.size()) >= getRequiredSlots(item);
		}
	}

	public boolean contains(Item i) {
		synchronized(list) {
			return list.contains(i);
		}
	}

	public int countId(int catalogID) {
		synchronized(list) {
			int ret = 0;
			for (Item i : list) {
				if (i.getCatalogId() == catalogID) {
					final int amount = i.getAmount();
					if (amount > Integer.MAX_VALUE - ret)
						return Integer.MAX_VALUE;
					ret += amount;
				}
			}
			return ret;
		}
	}

	public boolean full() {
		synchronized(list) {
			return list.size() >= getPlayer().getWorld().getMaxBankSize();
		}
	}

	public Item get(int index) {
		synchronized(list) {
			if (index < 0 || index >= list.size()) {
				return null;
			}
			return list.get(index);
		}
	}

	public Item get(Item item) {
		synchronized(list) {
			for (Item i : list) {
				if (item.equals(i)) {
					return i;
				}
			}
			return null;
		}
	}

	public int getFirstIndexById(int id) {
		synchronized(list) {
			for (int index = 0; index < list.size(); index++) {
				if (list.get(index).getCatalogId() == id) {
					return index;
				}
			}
			return -1;
		}
	}

	public List<Item> getItems() {
		                                                                                                                                                                     
		synchronized(list) {
			return list;
		}
	}

	public int getRequiredSlots(Item item) {
		synchronized(list) {
			                                               
			for (Item bankItem : list) {
				                              
				if (bankItem.getCatalogId() != item.getCatalogId())
					continue;

				                                     
				if (bankItem.getAmount() == Integer.MAX_VALUE)
					continue;

				                                                         
				int remainingSize = Integer.MAX_VALUE - bankItem.getAmount();
				return remainingSize < item.getAmount() ? 1 : 0;
			}

			                             
			return 1;
		}
	}

	public boolean hasItemId(int id) {
		synchronized(list) {
			for (Item i : list) {
				if (i.getCatalogId() == id)
					return true;
			}

			return false;
		}
	}

	public ListIterator<Item> iterator() {
		synchronized(list) {
			return list.listIterator();
		}
	}

	public int size() {
		synchronized(list) {
			return list.size();
		}
	}

	public boolean swap(int slot, int to) {
		synchronized(list) {
			if (slot < 0 || to < 0 || slot == to) {
				return false;
			}

			final int bankSize = list.size();

			if (slot >= bankSize || to >= bankSize) {
				return false;
			}

			final Item item1 = get(slot);
			final Item item2 = get(to);

			if (item1 == null || item2 == null) {
				return false;
			}

			list.set(slot, item2);
			list.set(to, item1);
			return true;
		}
	}

	public boolean insert(int slot, int to) {
		synchronized(list) {
			if (slot < 0 || to < 0 || to == slot) {
				return false;
			}

			final int bankSize = list.size();

			if (slot >= bankSize || to >= bankSize) {
				return false;
			}

			final Item item = get(slot);

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
	}

	public void setTab(int int1) {
		                                  

	}


	public void depositAllFromInventory() {
		synchronized (list) {
			synchronized (player.getCarriedItems().getInventory().getItems()) {
				try {
					for (int i = player.getCarriedItems().getInventory().getItems().size(); i-- > 0;) {
						Item item = player.getCarriedItems().getInventory().getItems().get(i);
						depositItemFromInventory(item.getCatalogId(), item.getAmount(), true);
					}
				} catch (Exception ex) {
					LOGGER.error(ex.getMessage());
				}
			}
		}
	}

	public void depositAllFromEquipment() {
		synchronized (list) {
			synchronized (player.getCarriedItems().getEquipment().getList()) {
				try {
					for (int slot = 0; slot < Equipment.SLOT_COUNT; slot++) {
						Item item = player.getCarriedItems().getEquipment().get(slot);
						if (item == null || item.getCatalogId() == ItemId.NOTHING.id()) continue;
						UnequipRequest uer = new UnequipRequest(player, item, UnequipRequest.RequestType.FROM_BANK, false);
						uer.equipmentSlot = Equipment.EquipmentSlot.get(slot);
						                              
						if (!player.getCarriedItems().getEquipment().unequipItem(uer)) {
							player.message("Failed to bank: " + item.getDef(player.getWorld()).getName());
							return;
						}
					}
				}
				catch (Exception ex) {
					LOGGER.error(ex.getMessage());
				}
			}
		}
	}

	   
                            
                                                                                                         
                                                  
                                                                                                                    
                  
    
	public void withdrawItemToInventory(final Integer catalogID, final Integer requestedAmount,
										final Boolean wantsNotes) {
		this.withdrawItemToInventory(catalogID, requestedAmount, wantsNotes, true);
	}

	public void withdrawItemToInventory(final Integer catalogID, final Integer requestedAmount,
										final Boolean wantsNotes, final boolean updateClient) {
		synchronized (this.list) {
			synchronized (this.player.getCarriedItems().getInventory().getItems()) {
				if (this.list.isEmpty()) return;

				Item bankItem = null;

				for (int i = this.list.size() - 1; i >= 0; i--) {
					final Item item = this.list.get(i);

					if (item.getCatalogId() == catalogID) {
						bankItem = item;
						break;
					}
				}

				if (bankItem == null) return;

				int amountToWithdraw = Math.min(requestedAmount, bankItem.getAmount());

				final ItemDefinition itemDef = bankItem.getDef(this.player.getWorld());
				if (itemDef == null) return;

				final boolean withdrawNoted = wantsNotes && itemDef.isNoteable();

				final int requiredInventorySlots = this.player.getCarriedItems()
					.getInventory()
					.getRequiredSlots(bankItem.getCatalogId(), amountToWithdraw, withdrawNoted);

				final int freeInventorySlots = this.player.getCarriedItems().getInventory().getFreeSlots();

				boolean limitedSlots = false;

				if (requiredInventorySlots > freeInventorySlots) {
					if (itemDef.isStackable() || withdrawNoted) {
						this.player.message("You don't have room to hold everything!");
						return;
					}

					amountToWithdraw = freeInventorySlots;
					limitedSlots = true;
				}

				final Item item = new Item(bankItem.getCatalogId(), amountToWithdraw, withdrawNoted,
					bankItem.getItemId());

				if (this.player.isUsingCustomClient()) {
					if (!this.remove(item, updateClient)) return;

					this.addToInventory(item, itemDef, amountToWithdraw, updateClient);

					if (limitedSlots && requestedAmount > amountToWithdraw)
						this.player.message("You don't have room to hold everything!");

					return;
				}

				                                                                             
				                               
				this.addToInventory(item, itemDef, amountToWithdraw, updateClient);

				if (limitedSlots && requestedAmount > amountToWithdraw)
					this.player.message("You don't have room to hold everything!");

				                                                                               
				                                                                         
				                                         
				try {
					if (!this.remove(item, updateClient))
						this.removeFromInventory(item, itemDef, amountToWithdraw, updateClient);
				} catch (final Exception e) {
					                                        
					                                                                                           
					LOGGER.error(e.getMessage(), e);
					this.removeFromInventory(item, itemDef, amountToWithdraw, updateClient);
				}
			}
		}
	}

	public void depositItemFromInventory(final int catalogID, int requestedAmount, final Boolean updateClient) {
		synchronized (list) {
			List<Item> items = player.getCarriedItems().getInventory().getItems();
			synchronized (items) {

				                                                
				requestedAmount = Math.min(requestedAmount, player.getCarriedItems().getInventory().countId(catalogID, Optional.empty()));
				if (requestedAmount <= 0) {
					if (!player.isUsingCustomClient() && catalogID == 1030) {                                
						player.playerServerMessage(MessageType.QUEST, "Try using the note on the Banker instead.");
					}
					return;
				}

				Item depositItem = player.getCarriedItems().getInventory().get(
					player.getCarriedItems().getInventory().getLastIndexById(catalogID)
				);
				if (depositItem == null) return;

				                                               
				                                                  
				                    
				int itemToAddCatalogId = depositItem.getCatalogId();
				int itemToAddAmount = requestedAmount;

				if (player.getConfig().WANT_CERT_DEPOSIT && player.getAttribute("swap_cert", false)) {
					itemToAddCatalogId = uncertedID(itemToAddCatalogId);

					if (itemToAddCatalogId != depositItem.getCatalogId()) {
						itemToAddAmount *= 5;
					}
				}

				Item itemToAdd = new Item(itemToAddCatalogId, itemToAddAmount);

				                                                               
				if (!canHold(itemToAdd)) {
					player.message("You don't have room for that in your bank");
					return;
				}

				                                                     
				if (!add(itemToAdd, updateClient)) return;

				                                                                                
				                                                                       
				                                                             

				                            
				ItemDefinition depositDef = depositItem.getDef(player.getWorld());
				if (depositDef == null) return;
				removeFromInventory(depositItem, depositDef, requestedAmount, updateClient);
			}
		}
	}

	                                                                                       
	private void addToInventory(Item item, ItemDefinition def, int requestedAmount, boolean updateClient) {
		if (def.isStackable() || item.getNoted()) {
			item = new Item(item.getCatalogId(), requestedAmount, item.getNoted());

			if (!player.getCarriedItems().getInventory().canHold(item)) {
				player.message("You don't have room to hold everything!");
				return;
			}

			                                                                           
			if (!player.getCarriedItems().getInventory().add(item, updateClient)) {
				                                                                                              
				if (player.getClientLimitations().maxItemId < item.getCatalogId()) {
					return;
				}
				add(item);
			}
		} else {
			for (int i = 1; i <= requestedAmount; i++) {
				item = new Item(item.getCatalogId(), 1, item.getNoted());

				if (!player.getCarriedItems().getInventory().canHold(item)) {
					player.message("You don't have room to hold everything!");
					return;
				}

				                                                                           
				if (!player.getCarriedItems().getInventory().add(item, updateClient)) {
					                                                                                              
					if (player.getClientLimitations().maxItemId < item.getCatalogId()) {
						break;
					}
					add(item);
					break;
				}
			}
		}
		if (updateClient && player.isUsingCustomClient()) {
			ActionSender.sendInventory(player);
		}
	}

	                                                          
	private void removeFromInventory(Item item, ItemDefinition def, int requestedAmount, boolean updateClient) {
		int slotAmount = 1;
		if (def.isStackable() || item.getNoted()) {
			slotAmount = Math.min(requestedAmount, item.getAmount());
		}

		                                     
		item = new Item(item.getCatalogId(), slotAmount, item.getNoted(), item.getItemId());
		if (player.getCarriedItems().getInventory().remove(item, updateClient) == -1) return;

		if (slotAmount < requestedAmount) {
			                
			item = player.getCarriedItems().getInventory().get(
				player.getCarriedItems().getInventory().getLastIndexById(item.getCatalogId())
			);
			removeFromInventory(item, def, requestedAmount - slotAmount, updateClient);
		}
	}

	private static int uncertedID(int itemID) {
		if (itemID == ItemId.IRON_ORE_CERTIFICATE.id()) {
			return ItemId.IRON_ORE.id();
		} else if (itemID == ItemId.COAL_CERTIFICATE.id()) {
			return ItemId.COAL.id();
		} else if (itemID == ItemId.MITHRIL_ORE_CERTIFICATE.id()) {
			return ItemId.MITHRIL_ORE.id();
		} else if (itemID == ItemId.SILVER_CERTIFICATE.id()) {
			return ItemId.SILVER.id();
		} else if (itemID == ItemId.GOLD_CERTIFICATE.id()) {
			return ItemId.GOLD.id();
		} else if (itemID == ItemId.IRON_BAR_CERTIFICATE.id()) {
			return ItemId.IRON_BAR.id();
		} else if (itemID == ItemId.STEEL_BAR_CERTIFICATE.id()) {
			return ItemId.STEEL_BAR.id();
		} else if (itemID == ItemId.MITHRIL_BAR_CERTIFICATE.id()) {
			return ItemId.MITHRIL_BAR.id();
		} else if (itemID == ItemId.SILVER_BAR_CERTIFICATE.id()) {
			return ItemId.SILVER_BAR.id();
		} else if (itemID == ItemId.GOLD_BAR_CERTIFICATE.id()) {
			return ItemId.GOLD_BAR.id();
		} else if (itemID == ItemId.LOBSTER_CERTIFICATE.id()) {
			return ItemId.LOBSTER.id();
		} else if (itemID == ItemId.RAW_LOBSTER_CERTIFICATE.id()) {
			return ItemId.RAW_LOBSTER.id();
		} else if (itemID == ItemId.SWORDFISH_CERTIFICATE.id()) {
			return ItemId.SWORDFISH.id();
		} else if (itemID == ItemId.RAW_SWORDFISH_CERTIFICATE.id()) {
			return ItemId.RAW_SWORDFISH.id();
		} else if (itemID == ItemId.BASS_CERTIFICATE.id()) {
			return ItemId.BASS.id();
		} else if (itemID == ItemId.RAW_BASS_CERTIFICATE.id()) {
			return ItemId.RAW_BASS.id();
		} else if (itemID == ItemId.SHARK_CERTIFICATE.id()) {
			return ItemId.SHARK.id();
		} else if (itemID == ItemId.RAW_SHARK_CERTIFICATE.id()) {
			return ItemId.RAW_SHARK.id();
		} else if (itemID == ItemId.YEW_LOGS_CERTIFICATE.id()) {
			return ItemId.YEW_LOGS.id();
		} else if (itemID == ItemId.MAPLE_LOGS_CERTIFICATE.id()) {
			return ItemId.MAPLE_LOGS.id();
		} else if (itemID == ItemId.WILLOW_LOGS_CERTIFICATE.id()) {
			return ItemId.WILLOW_LOGS.id();
		} else if (itemID == ItemId.DRAGON_BONE_CERTIFICATE.id()) {
			return ItemId.DRAGON_BONES.id();
		} else if (itemID == ItemId.LIMPWURT_ROOT_CERTIFICATE.id()) {
			return ItemId.LIMPWURT_ROOT.id();
		} else if (itemID == ItemId.PRAYER_POTION_CERTIFICATE.id()) {
			return ItemId.FULL_RESTORE_PRAYER_POTION.id();
		} else if (itemID == ItemId.SUPER_ATTACK_POTION_CERTIFICATE.id()) {
			return ItemId.FULL_SUPER_ATTACK_POTION.id();
		} else if (itemID == ItemId.SUPER_DEFENSE_POTION_CERTIFICATE.id()) {
			return ItemId.FULL_SUPER_DEFENSE_POTION.id();
		} else if (itemID == ItemId.SUPER_STRENGTH_POTION_CERTIFICATE.id()) {
			return ItemId.FULL_SUPER_STRENGTH_POTION.id();
		} else {
			return itemID;
		}
	}

	public Player getPlayer() {
		return player;
	}

	public BankPreset getBankPreset(int slot) { return this.bankPresets[slot]; }

	public void quickFeature(Npc npc, Player player, boolean auction) {
		if (player.isIronMan(IronmanMode.Ultimate.id())) {
			player.message("As an Ultimate Ironman, you cannot use the bank.");
			return;
		}

		if(validatebankpin(player, npc)) {
			if (auction) {
				player.getWorld().getMarket().addPlayerCollectItemsTask(player);
			} else {
				if (player.getConfig().BATCH_PROGRESSION && player.getBatchProgressBar()) {
					player.message("Please finish what you're doing to access your bank.");
					return;
				}
				player.setAccessingBank(true);
				ActionSender.showBank(player);
			}
		}
	}
}
