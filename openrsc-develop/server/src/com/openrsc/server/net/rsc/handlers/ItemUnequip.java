package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.model.container.Equipment;
import com.openrsc.server.model.container.Equipment.EquipmentSlot;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.struct.UnequipRequest;
import com.openrsc.server.net.rsc.PayloadProcessor;
import com.openrsc.server.net.rsc.enums.OpcodeIn;
import com.openrsc.server.net.rsc.struct.incoming.EquipStruct;
import com.openrsc.server.plugins.triggers.RemoveObjTrigger;

import static com.openrsc.server.net.rsc.handlers.ItemEquip.passCheck;

public class ItemUnequip implements PayloadProcessor<EquipStruct, OpcodeIn> {
	                                 
	                                                                             
	                                                                             
	                                                                     
	public void process(EquipStruct payload, Player player) throws Exception {
		OpcodeIn opcode = payload.getOpcode();

		                                
		if (opcode == null) {
			return;
		}

		                                                     
		if (!passCheck(player, opcode)) {
			return;
		}

		UnequipRequest request = new UnequipRequest();
		request.player = player;
		request.sound = true;

		if (opcode == OpcodeIn.ITEM_UNEQUIP_FROM_INVENTORY) {
			player.resetAllExceptDueling();

			int inventorySlot = payload.slotIndex;
			if (inventorySlot < 0 || inventorySlot >= 30) {
				player.setSuspiciousPlayer(true, "inventorySlot < 0 or inventorySlot >= 30");
				return;
			}

			request.item = player.getCarriedItems().getInventory().get(inventorySlot);
			request.inventorySlot = inventorySlot;
			request.requestType = UnequipRequest.RequestType.FROM_INVENTORY;
		} else if (opcode == OpcodeIn.ITEM_UNEQUIP_FROM_EQUIPMENT) {
			if (!player.getConfig().WANT_EQUIPMENT_TAB) return;

			player.resetAllExceptDueling();

			request.equipmentSlot = EquipmentSlot.get(payload.slotIndex);
			if (request.equipmentSlot == null) {
				player.setSuspiciousPlayer(true, "tried to unequip something out of range");
				return;
			}

			request.requestType = UnequipRequest.RequestType.FROM_EQUIPMENT;

			                                                                 
			Equipment.correctIndex(request);
		} else if (opcode == OpcodeIn.ITEM_REMOVE_TO_BANK) {
			if (!player.getConfig().WANT_EQUIPMENT_TAB) return;

			player.resetAllExceptBank();

			request.equipmentSlot = EquipmentSlot.get(payload.slotIndex);
			if (request.equipmentSlot == null) {
				player.setSuspiciousPlayer(true, "tried to unequip something out of range");
				return;
			}

			request.requestType = UnequipRequest.RequestType.FROM_BANK;

			                                                                
			Equipment.correctIndex(request);
		}

		                 
		else return;

		                                           
		if (request.item == null || !request.item.getDef(player.getWorld()).isWieldable()) {
			player.setSuspiciousPlayer(true, "tried to unequip item null or not wieldable");
			return;
		}

		player.getWorld().getServer().getPluginHandler().handlePlugin(RemoveObjTrigger.class, player, new Object[]{player, request.inventorySlot, request});
	}
}
