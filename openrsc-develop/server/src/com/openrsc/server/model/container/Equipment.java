package com.openrsc.server.model.container;

import com.openrsc.server.constants.*;
import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.struct.EquipRequest;
import com.openrsc.server.model.struct.UnequipRequest;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Optional;

public class Equipment {

	private static final Logger LOGGER = LogManager.getLogger();
	public static final int SLOT_COUNT = 14;
	private final Item[] list = new Item[SLOT_COUNT];
	private final Player player;

	private static final int[] chainBodyIds = {
		ItemId.BRONZE_CHAIN_MAIL_BODY.id(),
		ItemId.IRON_CHAIN_MAIL_BODY.id(),
		ItemId.STEEL_CHAIN_MAIL_BODY.id(),
		ItemId.BLACK_CHAIN_MAIL_BODY.id(),
		ItemId.MITHRIL_CHAIN_MAIL_BODY.id(),
		ItemId.ADAMANTITE_CHAIN_MAIL_BODY.id(),
		ItemId.RUNE_CHAIN_MAIL_BODY.id(),
		ItemId.DRAGON_SCALE_MAIL.id()
	};
	private static final int[] chainTopIds = {
		ItemId.BRONZE_CHAIN_MAIL_TOP.id(),
		ItemId.IRON_CHAIN_MAIL_TOP.id(),
		ItemId.STEEL_CHAIN_MAIL_TOP.id(),
		ItemId.BLACK_CHAIN_MAIL_TOP.id(),
		ItemId.MITHRIL_CHAIN_MAIL_TOP.id(),
		ItemId.ADAMANTITE_CHAIN_MAIL_TOP.id(),
		ItemId.RUNE_CHAIN_MAIL_TOP.id(),
		ItemId.DRAGON_SCALE_MAIL_TOP.id()
	};

	public Equipment(Player player) {
		synchronized (list) {
			this.player = player;
			for (int slotID = 0; slotID < SLOT_COUNT; slotID++)
				list[slotID] = null;
		}
	}

	                          

	public Item[] getList() {
		synchronized (list) {
			return this.list;
		}
	}

	public Item getAmmoItem() {
		synchronized (list) {
			return list[EquipmentSlot.SLOT_AMMO.getIndex()];
		}
	}

	public Item getNeckItem() {
		synchronized (list) {
			return list[EquipmentSlot.SLOT_NECK.getIndex()];
		}
	}

	public Item getRingItem() {
		synchronized (list) {
			return list[EquipmentSlot.SLOT_RING.getIndex()];
		}
	}

	                                 

	                       
	                                           
	public int add(Item item) {
		synchronized (list) {
			ItemDefinition itemDef = item.getDef(player.getWorld());
			if (itemDef == null || !itemDef.isWieldable())
				return -1;

			int slotID = itemDef.getWieldPosition();

			if (slotID < 0 || slotID >= Equipment.SLOT_COUNT)
				return -1;

			if (list[slotID] == null) {
				Item toEquip = new Item(item.getCatalogId(), item.getAmount(), item.getNoted());
				long itemID = player.getWorld().getServer().getDatabase().incrementMaxItemId(player);
				toEquip = new Item(toEquip.getCatalogId(), toEquip.getAmount(), toEquip.getNoted(), itemID);
				list[slotID] = toEquip;
				return slotID;
			} else {
				if (itemDef.isStackable()
					&& list[slotID].getCatalogId() == item.getCatalogId()) {
					list[slotID].changeAmount(item.getAmount());
					return slotID;
				}
			}
		}
		return -1;
	}

	                               
	                                                                                            
	public int remove(Item item, int amount) {
		return remove(item, amount, true);
	}

	public int remove(Item item, int amount, boolean updateClient) {
		synchronized (list) {
			long itemId = item.getItemId();
			for (int slotID = 0; slotID < SLOT_COUNT; slotID++) {
				Item curEquip = list[slotID];
				if (curEquip == null || curEquip.getDef(player.getWorld()) == null)
					continue;
				ItemDefinition curEquipDef = curEquip.getDef(player.getWorld());

				if (curEquip.getItemId() == itemId) {
					int curAmount = curEquip.getAmount();
					if (!curEquipDef.isStackable() && amount > 1)
						return -1;

					if (curAmount > amount) {
						list[slotID].changeAmount(-amount);
					} else if (curAmount < amount) {
						return -1;
					} else {
						list[slotID] = null;
						int appearanceId = player.getSettings().getAppearance().getSprite(curEquipDef.getWieldPosition());
						int wieldPosition = curEquipDef.getWieldPosition();
						if (wieldPosition > 4) {
							appearanceId = 0;
						}
						player.updateWornItems(wieldPosition,
							appearanceId,
							curEquipDef.getWearableId(), false);
					}
					if (updateClient) {
						ActionSender.sendEquipmentStats(player);
					}
					return slotID;
				}
			}
			return -1;
		}
	}

	                                         
	                               
	public boolean unequipItem(UnequipRequest request) {
		return unequipItem(request, true);
	}

	public boolean unequipItem(UnequipRequest request, boolean updateClient) {
		if (request.item == null || !request.item.isWieldable(player.getWorld())) {
			return false;
		}

		                                        
		if (!hasEquipped(request.item.getCatalogId())) {
			player.setSuspiciousPlayer(true, "tried to unequip something they don't have equipped");
			return false;
		}

		                             
		if ((request.requestType == UnequipRequest.RequestType.FROM_EQUIPMENT
			|| request.requestType == UnequipRequest.RequestType.FROM_BANK)
			&& !player.getConfig().WANT_EQUIPMENT_TAB) {
			player.setSuspiciousPlayer(true, "tried to unequip from a container they can't");
			return false;
		}

		switch (request.requestType) {
			case FROM_INVENTORY:
				request.item.setWielded(false);
				ItemDefinition curEquipDef = request.item.getDef(player.getWorld());
				                                                                                     
				player.updateWornItems(
					curEquipDef.getWieldPosition(),
					player.getSettings().getAppearance().getSprite(curEquipDef.getWieldPosition()),
					curEquipDef.getWearableId(),
					false
				);

				if (player.getConfig().WANT_CUSTOM_SPRITES && player.getConfig().FORM_FITTING_CHAINMAIL) {
					for (int i = 0; i < chainTopIds.length; ++i) {
						if (request.item.getCatalogId() == chainTopIds[i]) {
							                                                         
							if (player.getCarriedItems().remove(request.item) == -1) {
								break;
							}
							player.getCarriedItems().getInventory().add(new Item(chainBodyIds[i]));
							break;
						}
					}
				}

				break;
			case FROM_EQUIPMENT:
				synchronized (list) {
					synchronized (player) {
						                                               
						if (player.getCarriedItems().getInventory().full()) {
							player.message("You need more inventory space to unequip that.");
							return false;
						}
						if (remove(request.item, request.item.getAmount()) == -1)
							return false;
						request.item.setWielded(false);

						Item itemToAdd = request.item;
						if (player.getConfig().WANT_CUSTOM_SPRITES && player.getConfig().FORM_FITTING_CHAINMAIL) {
							for (int i = 0; i < chainTopIds.length; ++i) {
								if (request.item.getCatalogId() == chainTopIds[i]) {
									itemToAdd = new Item(chainBodyIds[i]);
									break;
								}
							}
						}

						player.getCarriedItems().getInventory().add(itemToAdd, updateClient);

					}
				}
				break;
			case FROM_BANK:
				synchronized (list) {
					synchronized (player.getBank().getItems()) {
						                                          
						if (player.getBank().full()) {
							player.message("You need more bank space to unequip that.");
							return false;
						}
						if (remove(request.item, request.item.getAmount()) == -1)
							return false;
						request.item.setWielded(false);

						Item itemToAdd = request.item;
						if (player.getConfig().WANT_CUSTOM_SPRITES && player.getConfig().FORM_FITTING_CHAINMAIL) {
							for (int i = 0; i < chainTopIds.length; ++i) {
								if (request.item.getCatalogId() == chainTopIds[i]) {
									itemToAdd = new Item(chainBodyIds[i]);
									break;
								}
							}
						}

						player.getBank().add(itemToAdd, updateClient);
						if (updateClient) {
							ActionSender.showBank(player);
						}
					}
				}
				break;
			case CHECK_IF_EQUIPMENT_TAB:
				if (player.getConfig().WANT_EQUIPMENT_TAB) {
					request.requestType = UnequipRequest.RequestType.FROM_EQUIPMENT;
				} else {
					request.requestType = UnequipRequest.RequestType.FROM_INVENTORY;
				}
				return unequipItem(request);
		}

		                           
		AppearanceId appearance = AppearanceId.getById(request.item.getDef(player.getWorld()).getAppearanceId());
		if (appearance.getSuggestedWieldPosition() == AppearanceId.SLOT_MORPHING_RING) {
			player.exitMorph();
		}

		if (request.sound) {
			player.playSound("click");
		}

		if (updateClient) {
			ActionSender.sendEquipmentStats(player, request.item.getDef(player.getWorld()).getWieldPosition());
			player.getUpdateFlags().setAppearanceChanged(true);
			ActionSender.sendInventory(player);
		}
		return true;
	}

	                                     
	                             
	public boolean equipItem(EquipRequest request) {
		return equipItem(request, true);
	}
	public boolean equipItem(EquipRequest request, boolean updateClient) {
		                                  
		if (request.item.getNoted())
			return false;

		                                                   
		if (player.getConfig().WANT_CUSTOM_SPRITES && player.getConfig().FORM_FITTING_CHAINMAIL) {
			Item newItem = null;
			if (player.isMale()) {
				for (int i = 0; i < chainTopIds.length; ++i) {
					if (chainTopIds[i] == request.item.getCatalogId()) {
						newItem = new Item(chainBodyIds[i]);
					}
				}
			} else {
				for (int i = 0; i < chainBodyIds.length; ++i) {
					if (chainBodyIds[i] == request.item.getCatalogId()) {
						newItem = new Item(chainTopIds[i]);
					}
				}
			}
			if (newItem != null) {
				if (request.requestType == EquipRequest.RequestType.FROM_BANK && player.getBank().remove(request.item.getCatalogId(), 1)) {
					player.getBank().add(newItem);
				} else if (request.requestType == EquipRequest.RequestType.FROM_INVENTORY && player.getCarriedItems().remove(request.item) != -1) {
					player.getCarriedItems().getInventory().add(newItem);
				} else {
					return false;
				}
				request.item = newItem;
			}
		}

		                                                 
		if (!ableToEquip(request.item))
			return false;

		                                                                   
		switch (request.requestType) {
			case FROM_INVENTORY:
				if (!equipItemFromInventory(request, updateClient))
					return false;
				break;
			case FROM_BANK:
				if (!equipItemFromBank(request, updateClient))
					return false;
				break;
			default:
				LOGGER.error("Unknown Equip request by " + request.player);
				return false;
		}

		if (request.sound)
			player.playSound("click");

		ItemDefinition itemDef = request.item.getDef(player.getWorld());
		if (morphAllowsUpdate(itemDef)) {
			player.updateWornItems(itemDef.getWieldPosition(), itemDef.getAppearanceId(), itemDef.getWearableId(), true);
		}

		if (updateClient) {
			ActionSender.sendEquipmentStats(player, request.item.getDef(player.getWorld()).getWieldPosition());
			player.getUpdateFlags().setAppearanceChanged(true);
		}
		return true;
	}

	private boolean morphAllowsUpdate(ItemDefinition newlyWieldedItem) {
		if (newlyWieldedItem.getWieldPosition() == AppearanceId.SLOT_MORPHING_RING) {
			return true;
		} else {
			if (player.getCarriedItems().getEquipment() == null) {
				return true;
			}
			if (player.getCarriedItems().getEquipment().getRingItem() == null) {
				return true;
			}
			final ItemDefinition wornRingDef = player.getCarriedItems().getEquipment().getRingItem().getDef(player.getWorld());
			if (wornRingDef.getAppearanceId() == AppearanceId.NOTHING.id()) {
				return true;
			}
		}
		return false;
	}

	                                                  
	                                                     
	private boolean equipItemFromInventory(EquipRequest request, boolean updateClient) {
		synchronized (player.getCarriedItems()) {
			if (player.getConfig().WANT_EQUIPMENT_TAB) {                                

				ItemDefinition itemDef = request.item.getDef(player.getWorld());
				if (itemDef == null)
					return false;

				ArrayList<Item> items = gatherConflictingItems(request);

				                                                    
				                                         
				if (player.getCarriedItems().getInventory().getFreeSlots() + 1 < items.size()) {
					player.message("You need more inventory space to equip that.");
					return false;
				}

				                                                       
				Item toEquip = player.getCarriedItems().getInventory().get(
					player.getCarriedItems().getInventory().getLastIndexById(
						request.item.getCatalogId(), Optional.of(false)
					)
				);
				if (toEquip == null)
					return false;

				if (player.getWorld().getPlayer(DataConversions.usernameToHash(player.getUsername())) == null) {
					return false;
				}

				                                          
				for (Item item : items) {
					remove(item, item.getAmount(), updateClient);                         
				}
				player.getCarriedItems().getInventory().remove(toEquip, updateClient);                         

				for (Item item : items) {
					int id = item.getCatalogId();
					if (player.getConfig().WANT_CUSTOM_SPRITES && player.getConfig().FORM_FITTING_CHAINMAIL) {
						for (int i = 0; i < chainTopIds.length; ++i) {
							if (chainTopIds[i] == item.getCatalogId()) {
								id = chainBodyIds[i];
								break;
							}
						}
					}
					player.getCarriedItems().getInventory().add(                    
						new Item(id, item.getAmount()), updateClient);
				}
				add(new Item(toEquip.getCatalogId(), toEquip.getAmount()));                    

			} else {                                   
				unequipConflictingItems(request);
				request.item.setWielded(true);
			}

		}
		                       
		if (updateClient) {
			ActionSender.sendInventory(player);
		}
		return true;
	}

	                                             
	                                                   
	private boolean equipItemFromBank(EquipRequest request, boolean updateClient) {
		synchronized (list) {
			synchronized (player.getBank().getItems()) {
				if (!request.player.getConfig().WANT_EQUIPMENT_TAB) {
					request.player.setSuspiciousPlayer(true, "Tried to equip from bank on a world without equipment tab");
					return false;
				}

				ItemDefinition itemDef = request.item.getDef(player.getWorld());
				if (itemDef == null)
					return false;

				ArrayList<Item> itemsToUnequip = gatherConflictingItems(request);

				                                                    
				                                         
				if (player.getFreeBankSlots() < itemsToUnequip.size()) {
					player.message("You need more bank space to equip that.");
					return false;
				}

				Item toEquip = player.getBank().get(
					player.getBank().getFirstIndexById(request.item.getCatalogId())
				);
				if (toEquip == null) {
					return false;
				}

				if (player.getWorld().getPlayer(DataConversions.usernameToHash(player.getUsername())) == null) {
					return false;
				}

				                                                    
				for (Item item : itemsToUnequip) {
					remove(item, item.getAmount(), updateClient);
				}

				if (!itemDef.isStackable()) {
					player.getBank().remove(toEquip.getCatalogId(), 1, updateClient);
					for (Item item : itemsToUnequip) {
						int id = item.getCatalogId();
						if (player.getConfig().WANT_CUSTOM_SPRITES && player.getConfig().FORM_FITTING_CHAINMAIL) {
							for (int i = 0; i < chainTopIds.length; ++i) {
								if (chainTopIds[i] == item.getCatalogId()) {
									id = chainBodyIds[i];
									break;
								}
							}
						}

						player.getBank().add(new Item(id, item.getAmount()), updateClient);
					}

					if (toEquip.getAmount() > 1) {
						add(new Item(toEquip.getCatalogId(), 1));
					} else {
						add(request.item);
					}
				} else {
					int amountToRemoveAndEquip = Math.min(toEquip.getAmount(), request.item.getAmount());

					player.getBank().remove(toEquip.getCatalogId(), amountToRemoveAndEquip, updateClient);
					for (Item item : itemsToUnequip) {
						player.getBank().add(new Item(item.getCatalogId(), item.getAmount()), updateClient);
					}

					if (amountToRemoveAndEquip != request.item.getAmount()) {
						add(new Item(request.item.getCatalogId(), amountToRemoveAndEquip));
					} else {
						add(request.item);
					}
				}
			}
		}

		                     
		if (updateClient) {
			ActionSender.showBank(player);
		}
		return true;
	}

	private ArrayList<Item> gatherConflictingItems(EquipRequest request) {
		                               
		ArrayList<Item> items = new ArrayList<>();
		for (int slotID = 0; slotID < Equipment.SLOT_COUNT; slotID++) {
			Item item = list[slotID];
			if (item != null && request.item.wieldingAffectsItem(player.getWorld(), item)) {
				if (request.item.getDef(player.getWorld()).isStackable()) {
					if (request.item.getCatalogId() == item.getCatalogId())
						continue;
				}
				items.add(item);
			}
		}
		return items;
	}

	                                                   
	                                                              
	private boolean unequipConflictingItems(EquipRequest request) {
		synchronized (player.getCarriedItems().getInventory()) {
			for (Item item : player.getCarriedItems().getInventory().getItems()) {
				if (request.item.wieldingAffectsItem(player.getWorld(), item) && item.isWielded()) {
					if (!player.getCarriedItems().getEquipment().unequipItem(new UnequipRequest(player, item, UnequipRequest.RequestType.FROM_INVENTORY, false), false))
						return false;
				}
			}
		}
		return true;
	}

	                                 

	                                         
	                                                     
	                                     
	                                            
	public int searchEquipmentForItem(int id) {
		synchronized (list) {
			Item item;
			for (int slotID = 0; slotID < SLOT_COUNT; slotID++) {
				item = list[slotID];
				if (item != null && item.getCatalogId() == id)
					return slotID;
			}
			return -1;
		}
	}

	                               
	                                                     
	                                                 
	                                            
	public boolean hasCatalogID(int catalogID) {
		return searchEquipmentForItem(catalogID) != -1;
	}

	                      
	                                                      
	                                            
	public Item get(int slotID) {
		synchronized (list) {
			if (slotID < 0 || slotID >= SLOT_COUNT) {
				return null;
			}
			return list[slotID];
		}
	}

	                              
	                                                                                  
	public boolean hasEquipped(int id) {
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			return searchEquipmentForItem(id) != -1;
		} else {
			for (Item i : player.getCarriedItems().getInventory().getItems()) {
				if (i.getCatalogId() == id && i.isWielded()) {
					return true;
				}
			}
		}
		return false;
	}

	                               
	                                                          
	public boolean ableToEquip(Item item) {
		                                                             
		boolean hasRequirement = !player.getConfig().NO_LEVEL_REQUIREMENT_WIELD;

		int requiredLevel = hasRequirement ? item.getDef(player.getWorld()).getRequiredLevel() : 1;
		int requiredSkillIndex = item.getDef(player.getWorld()).getRequiredSkillIndex();
		String itemLower = item.getDef(player.getWorld()).getName().toLowerCase();
		Optional<Integer> optionalLevel = Optional.empty();
		Optional<Integer> optionalSkillIndex = Optional.empty();
		boolean ableToWield = true;
		boolean bypass = !player.getConfig().STRICT_CHECK_ALL &&
			(itemLower.startsWith("poisoned") &&
				((itemLower.endsWith("throwing dart") && !player.getConfig().STRICT_PDART_CHECK) ||
					(itemLower.endsWith("throwing knife") && !player.getConfig().STRICT_PKNIFE_CHECK) ||
					(itemLower.endsWith("spear") && !player.getConfig().STRICT_PSPEAR_CHECK))
			);

		                             
		if (itemLower.endsWith("spear") || itemLower.endsWith("throwing knife")) {
			optionalLevel = Optional.of(requiredLevel <= 10 ? requiredLevel : requiredLevel + 5);
			optionalSkillIndex = Optional.of(Skill.ATTACK.id());
		}
		                         
		if (item.getCatalogId() == ItemId.STAFF_OF_IBAN.id()) {
			optionalLevel = Optional.of(requiredLevel);
			optionalSkillIndex = Optional.of(Skill.ATTACK.id());
		}

		                                         
		if (itemLower.contains("battlestaff")) {
			optionalLevel = Optional.of(requiredLevel);
			optionalSkillIndex = Optional.of(Skill.ATTACK.id());
		}

		if (optionalLevel.isPresent() && !hasRequirement) {
			optionalLevel = Optional.of(1);
		}

		                                            
		if (player.getSkills().getMaxStat(requiredSkillIndex) < requiredLevel) {
			if (!bypass) {
				player.message("You are not a high enough level to use this item");
				player.message("You need to have a " + player.getWorld().getServer().getConstants().getSkills().getSkillName(requiredSkillIndex) + " level of " + requiredLevel);
				ableToWield = false;
			}
		}
		if (optionalSkillIndex.isPresent() && player.getSkills().getMaxStat(optionalSkillIndex.get()) < optionalLevel.get()) {
			if (!bypass) {
				player.message("You are not a high enough level to use this item");
				player.message("You need to have a " + player.getWorld().getServer().getConstants().getSkills().getSkillName(optionalSkillIndex.get()) + " level of " + optionalLevel.get());
				ableToWield = false;
			}
		}

		                                
		if (item.getDef(player.getWorld()).isFemaleOnly() && player.isMale()) {
			player.message("It doesn't fit!");
			player.message("Perhaps I should get someone to adjust it for me");
			ableToWield = false;
		}

		                               
		if (!player.getConfig().EQUIP_QUEST_ITEMS_WITHOUT_QUESTS && (item.getCatalogId() == ItemId.RUNE_PLATE_MAIL_BODY.id() || item.getCatalogId() == ItemId.RUNE_PLATE_MAIL_TOP.id())
			&& (player.getQuestStage(Quests.DRAGON_SLAYER) != -1)) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the dragon slayer quest");
			return false;
		}

		               
		else if (!player.getConfig().EQUIP_QUEST_ITEMS_WITHOUT_QUESTS && item.getCatalogId() == ItemId.DRAGON_SWORD.id() && player.getQuestStage(Quests.LOST_CITY) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the Lost city of zanaris quest");
			return false;
		}

		                    
		else if (!player.getConfig().EQUIP_QUEST_ITEMS_WITHOUT_QUESTS && item.getCatalogId() == ItemId.DRAGON_AXE.id() && player.getQuestStage(Quests.HEROS_QUEST) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the Hero's guild entry quest");
			return false;
		}

		                       
		else if (!player.getConfig().EQUIP_QUEST_ITEMS_WITHOUT_QUESTS && item.getCatalogId() == ItemId.DRAGON_SQUARE_SHIELD.id() && player.getQuestStage(Quests.LEGENDS_QUEST) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the legend's guild quest");
			return false;
		}

		                                                                 
		                                                                              
		                                                                           

		                        
		else if (item.getCatalogId() == ItemId.STAFF_OF_GUTHIX.id() && (hasEquipped(ItemId.ZAMORAK_CAPE.id()) || hasEquipped(ItemId.SARADOMIN_CAPE.id()))) {                            
			player.message("you may not wield this staff while wearing a cape of another god");
			return false;
		} else if (item.getCatalogId() == ItemId.STAFF_OF_SARADOMIN.id() && (hasEquipped(ItemId.ZAMORAK_CAPE.id()) || hasEquipped(ItemId.GUTHIX_CAPE.id()))) {                          
			player.message("you may not wield this staff while wearing a cape of another god");
			return false;
		} else if (item.getCatalogId() == ItemId.STAFF_OF_ZAMORAK.id() && (hasEquipped(ItemId.SARADOMIN_CAPE.id()) || hasEquipped(ItemId.GUTHIX_CAPE.id()))) {                             
			player.message("you may not wield this staff while wearing a cape of another god");
			return false;
		} else if (item.getCatalogId() == ItemId.GUTHIX_CAPE.id() && (hasEquipped(ItemId.STAFF_OF_ZAMORAK.id()) || hasEquipped(ItemId.STAFF_OF_SARADOMIN.id()))) {                           
			player.message("you may not wear this cape while wielding staffs of the other gods");
			return false;
		} else if (item.getCatalogId() == ItemId.SARADOMIN_CAPE.id() && (hasEquipped(ItemId.STAFF_OF_ZAMORAK.id()) || hasEquipped(ItemId.STAFF_OF_GUTHIX.id()))) {                         
			player.message("you may not wear this cape while wielding staffs of the other gods");
			return false;
		} else if (item.getCatalogId() == ItemId.ZAMORAK_CAPE.id() && (hasEquipped(ItemId.STAFF_OF_GUTHIX.id()) || hasEquipped(ItemId.STAFF_OF_SARADOMIN.id()))) {                            
			player.message("you may not wear this cape while wielding staffs of the other gods");
			return false;
		}

		                                 
		  
                                                                   
                                                                    
                                                                   
          
   
    

		                                     
		  
                                                                               
                                                                    
                                                           
          
   
    

		                  
		else if ((item.getCatalogId() == ItemId.IRONMAN_HELM.id() || item.getCatalogId() == ItemId.IRONMAN_PLATEBODY.id() || item.getCatalogId() == ItemId.IRONMAN_PLATE_TOP.id()
			|| item.getCatalogId() == ItemId.IRONMAN_PLATELEGS.id() || item.getCatalogId() == ItemId.IRONMAN_PLATED_SKIRT.id()) && !player.isIronMan(IronmanMode.Ironman.id())) {
			player.message("You need to be an Ironman to wear this");
			return false;
		} else if ((item.getCatalogId() == ItemId.ULTIMATE_IRONMAN_HELM.id() || item.getCatalogId() == ItemId.ULTIMATE_IRONMAN_PLATEBODY.id() || item.getCatalogId() == ItemId.ULTIMATE_IRONMAN_PLATE_TOP.id()
			|| item.getCatalogId() == ItemId.ULTIMATE_IRONMAN_PLATELEGS.id() || item.getCatalogId() == ItemId.ULTIMATE_IRONMAN_PLATED_SKIRT.id()) && !player.isIronMan(IronmanMode.Ultimate.id())) {
			player.message("You need to be an Ultimate Ironman to wear this");
			return false;
		} else if ((item.getCatalogId() == ItemId.HARDCORE_IRONMAN_HELM.id() || item.getCatalogId() == ItemId.HARDCORE_IRONMAN_PLATEBODY.id() || item.getCatalogId() == ItemId.HARDCORE_IRONMAN_PLATE_TOP.id()
			|| item.getCatalogId() == ItemId.HARDCORE_IRONMAN_PLATELEGS.id() || item.getCatalogId() == ItemId.HARDCORE_IRONMAN_PLATED_SKIRT.id()) && !player.isIronMan(IronmanMode.Hardcore.id())) {
			player.message("You need to be a Hardcore Ironman to wear this");
			return false;
		} else if (item.getCatalogId() == 2254 && player.getQuestStage(Quests.LEGENDS_QUEST) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the Legends Quest");
			return false;
		}

		return ableToWield;
	}

	                                                

	                            
	                                                             
	public int getWeaponAim() {
		int total = 1;
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			synchronized (list) {
				for (Item item : list)
					total += item == null ? 0 : item.getDef(player.getWorld()).getWeaponAimBonus();
			}
		} else {
			synchronized (player.getCarriedItems().getInventory().getItems()) {
				for (Item item : player.getCarriedItems().getInventory().getItems()) {
					if (item.isWielded()) {
						total += item.getDef(player.getWorld()).getWeaponAimBonus();
					}
				}
			}
		}
		return total;
	}

	                              
	                                                               
	public int getWeaponPower() {
		int total = 1;
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			synchronized (list) {
				for (Item item : list)
					total += item == null ? 0 : item.getDef(player.getWorld()).getWeaponPowerBonus();
			}
		} else {
			synchronized (player.getCarriedItems().getInventory().getItems()) {
				for (Item item : player.getCarriedItems().getInventory().getItems()) {
					if (item.isWielded()) {
						total += item.getDef(player.getWorld()).getWeaponPowerBonus();
					}
				}
			}
		}
		return total;
	}

	                         
	                                                               
	public int getArmour() {
		int total = 1;
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			synchronized (list) {
				for (Item item : list)
					total += item == null ? 0 : item.getDef(player.getWorld()).getArmourBonus();
			}
		} else {
			synchronized (player.getCarriedItems().getInventory().getItems()) {
				for (Item item : player.getCarriedItems().getInventory().getItems()) {
					if (item.isWielded()) {
						total += item.getDef(player.getWorld()).getArmourBonus();
					}
				}
			}
		}
		return total;
	}

	                        
	                                                              
	public int getMagic() {
		int total = 1;
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			synchronized (list) {
				for (Item item : list)
					total += item == null ? 0 : item.getDef(player.getWorld()).getMagicBonus();
			}
		} else {
			synchronized (player.getCarriedItems().getInventory().getItems()) {
				for (Item item : player.getCarriedItems().getInventory().getItems()) {
					if (item.isWielded()) {
						total += item.getDef(player.getWorld()).getMagicBonus();
					}
				}
			}
		}
		return total;
	}

	                         
	                                                               
	public int getPrayer() {
		int total = 1;
		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			synchronized (list) {
				for (Item item : list)
					total += item == null ? 0 : item.getDef(player.getWorld()).getPrayerBonus();
			}
		} else {
			synchronized (player.getCarriedItems().getInventory().getItems()) {
				for (Item item : player.getCarriedItems().getInventory().getItems()) {
					if (item.isWielded()) {
						total += item.getDef(player.getWorld()).getPrayerBonus();
					}
				}
			}
		}
		return total;
	}

	                          
	                                             
	                                                           
	public int equipCount() {
		synchronized (list) {
			int total = 0;
			for (Item item : list) {
				if (item != null)
					total++;
			}
			return total;
		}
	}

	                            
                                                    
                                                                                
    
	public enum EquipmentSlot {
		SLOT_LARGE_HELMET(0),
		SLOT_PLATE_BODY(1),
		SLOT_PLATE_LEGS(2),
		SLOT_OFFHAND(3),
		SLOT_MAINHAND(4),
		SLOT_MEDIUM_HELMET(5),
		SLOT_CHAIN_BODY(6),
		SLOT_SKIRT(7),
		SLOT_GLOVES(8),
		SLOT_BOOTS(9),
		SLOT_NECK(10),
		SLOT_CAPE(11),
		SLOT_AMMO(12),
		SLOT_RING(13);
		int index;

		EquipmentSlot(int index) {
			this.index = index;
		}

		public int getIndex() {
			return this.index;
		}

		public static EquipmentSlot get(int index) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (slot.getIndex() == index)
					return slot;
			}
			return null;
		}
	}

	public static void correctIndex(UnequipRequest request) {
		if (request.equipmentSlot == EquipmentSlot.SLOT_LARGE_HELMET) {
			if (request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_LARGE_HELMET.getIndex()) != null) {
				request.item = request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_LARGE_HELMET.getIndex());
				request.equipmentSlot = EquipmentSlot.SLOT_LARGE_HELMET;
			} else if (request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_MEDIUM_HELMET.getIndex()) != null) {
				request.item = request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_MEDIUM_HELMET.getIndex());
				request.equipmentSlot = EquipmentSlot.SLOT_MEDIUM_HELMET;
			}
		} else if (request.equipmentSlot == EquipmentSlot.SLOT_PLATE_BODY) {
			if (request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_PLATE_BODY.getIndex()) != null) {
				request.item = request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_PLATE_BODY.getIndex());
				request.equipmentSlot = EquipmentSlot.SLOT_PLATE_BODY;
			} else if (request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_CHAIN_BODY.getIndex()) != null) {
				request.item = request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_CHAIN_BODY.getIndex());
				request.equipmentSlot = EquipmentSlot.SLOT_CHAIN_BODY;
			}
		} else if (request.equipmentSlot == Equipment.EquipmentSlot.SLOT_PLATE_LEGS) {
			if (request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_PLATE_LEGS.getIndex()) != null) {
				request.item = request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_PLATE_LEGS.getIndex());
				request.equipmentSlot = EquipmentSlot.SLOT_PLATE_LEGS;
			} else if (request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_SKIRT.getIndex()) != null) {
				request.item = request.player.getCarriedItems().getEquipment().get(EquipmentSlot.SLOT_SKIRT.getIndex());
				request.equipmentSlot = EquipmentSlot.SLOT_SKIRT;
			}
		} else if (request.equipmentSlot.getIndex() > 4) {
			request.item = request.player.getCarriedItems().getEquipment().get(request.equipmentSlot.getIndex() + 3);
			request.equipmentSlot = EquipmentSlot.get(request.equipmentSlot.getIndex() + 3);
		} else {
			request.item = request.player.getCarriedItems().getEquipment().get(request.equipmentSlot.getIndex());
			request.equipmentSlot = EquipmentSlot.get(request.equipmentSlot.getIndex());
		}
	}
}
