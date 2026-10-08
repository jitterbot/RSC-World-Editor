package com.openrsc.server.net.rsc.generators.impl;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.external.GameObjectLoc;
import com.openrsc.server.external.ItemLoc;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.RSCString;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.Packet;
import com.openrsc.server.net.PacketBuilder;
import com.openrsc.server.net.rsc.GameNetworkException;
import com.openrsc.server.net.rsc.PayloadValidator;
import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.generators.PayloadGenerator;
import com.openrsc.server.net.rsc.struct.AbstractStruct;
import com.openrsc.server.net.rsc.struct.outgoing.*;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MathUtil;
import com.openrsc.server.util.rsc.StringUtil;

import java.util.HashMap;
import java.util.Map;

   
                                                                                               
  
                                                            
  
                                                                                
  
                                                                                  
      
public class Payload202Generator implements PayloadGenerator<OpcodeOut> {
	private static final Map<OpcodeOut, Integer> opcodes202 = new HashMap<OpcodeOut, Integer>() {{
		put(OpcodeOut.SEND_LOGOUT_REQUEST_CONFIRM, 222);        
		put(OpcodeOut.SEND_QUESTS, 224);        
		put(OpcodeOut.SEND_DUEL_OPPONENTS_ITEMS, 63);        
		put(OpcodeOut.SEND_TRADE_ACCEPTED, 18);        
		put(OpcodeOut.SEND_TRADE_OPEN_CONFIRM, 251);        
		put(OpcodeOut.SEND_WORLD_INFO, 131);        
		put(OpcodeOut.SEND_DUEL_SETTINGS, 198);        
		put(OpcodeOut.SEND_EXPERIENCE, 211);        
		put(OpcodeOut.SEND_BUBBLE, 23);        
		put(OpcodeOut.SEND_BANK_OPEN, 93);        
		put(OpcodeOut.SEND_SCENERY_HANDLER, 27);        
		put(OpcodeOut.SEND_PRIVACY_SETTINGS, 158);        
		put(OpcodeOut.SEND_SYSTEM_UPDATE, 72);        
		put(OpcodeOut.SEND_INVENTORY, 114);        
		put(OpcodeOut.SEND_APPEARANCE_SCREEN, 207);        
		put(OpcodeOut.SEND_NPC_COORDS, 77);        
		put(OpcodeOut.SEND_DEATH, 165);        
		put(OpcodeOut.SEND_STOPSLEEP, 103);        
		put(OpcodeOut.SEND_BOX2, 148);        
		put(OpcodeOut.SEND_INVENTORY_UPDATEITEM, 228);        
		put(OpcodeOut.SEND_BOUNDARY_HANDLER, 95);        
		put(OpcodeOut.SEND_TRADE_WINDOW, 4);        
		put(OpcodeOut.SEND_TRADE_OTHER_ITEMS, 250);        
		put(OpcodeOut.SEND_GROUND_ITEM_HANDLER, 109);        
		put(OpcodeOut.SEND_SHOP_OPEN, 253);        
		put(OpcodeOut.SEND_UPDATE_NPC, 190);        
		put(OpcodeOut.SEND_IGNORE_LIST, 2);        
		put(OpcodeOut.SEND_FATIGUE, 126);        
		put(OpcodeOut.SEND_SLEEPSCREEN, 219);        
		put(OpcodeOut.SEND_PRIVATE_MESSAGE, 170);        
		put(OpcodeOut.SEND_INVENTORY_REMOVE_ITEM, 191);        
		put(OpcodeOut.SEND_TRADE_CLOSE, 187);        
		put(OpcodeOut.SEND_SERVER_MESSAGE, 48);        
		put(OpcodeOut.SEND_SHOP_CLOSE, 220);        
		put(OpcodeOut.SEND_FRIEND_LIST, 249);        
		put(OpcodeOut.SEND_FRIEND_UPDATE, 25);        
		put(OpcodeOut.SEND_EQUIPMENT_STATS, 177);        
		put(OpcodeOut.SEND_STATS, 180);        
		put(OpcodeOut.SEND_STAT, 208);        
		put(OpcodeOut.SEND_TRADE_OTHER_ACCEPTED, 92);        
		put(OpcodeOut.SEND_DUEL_CONFIRMWINDOW, 147);        
		put(OpcodeOut.SEND_DUEL_WINDOW, 229);        
		put(OpcodeOut.SEND_WELCOME_INFO, 248);        
		put(OpcodeOut.SEND_CANT_LOGOUT, 136);        
		put(OpcodeOut.SEND_PLAYER_COORDS, 145);        
		put(OpcodeOut.SEND_SLEEPWORD_INCORRECT, 15);        
		put(OpcodeOut.SEND_BANK_CLOSE, 171);        
		put(OpcodeOut.SEND_PLAY_SOUND, 11);        
		put(OpcodeOut.SEND_PRAYERS_ACTIVE, 209);        
		put(OpcodeOut.SEND_DUEL_ACCEPTED, 197);        
		put(OpcodeOut.SEND_REMOVE_WORLD_ENTITY, 115);        
		put(OpcodeOut.SEND_BOX, 64);        
		put(OpcodeOut.SEND_DUEL_CLOSE, 160);        
		put(OpcodeOut.SEND_UPDATE_PLAYERS, 53);        
		put(OpcodeOut.SEND_GAME_SETTINGS, 152);        
		put(OpcodeOut.SEND_SLEEP_FATIGUE, 168);        
		put(OpcodeOut.SEND_OPTIONS_MENU_OPEN, 223);        
		put(OpcodeOut.SEND_BANK_UPDATE, 139);        
		put(OpcodeOut.SEND_OPTIONS_MENU_CLOSE, 127);        
		put(OpcodeOut.SEND_DUEL_OTHER_ACCEPTED, 65);        
	}};
	private final Map<OpcodeOut, Integer> opcodes;
	private final Payload203Generator gen;

	public Payload202Generator(Map<OpcodeOut, Integer> opcodes) {
		this.opcodes = opcodes;
		gen = new Payload203Generator(opcodes);
	}

	public Payload202Generator() {
		this(opcodes202);
	}

	@Override
	public PacketBuilder fromOpcodeEnum(OpcodeOut opcode, Player player) {
		return gen.fromOpcodeEnum(opcode, player);
	}

	@Override
	public Packet generate(AbstractStruct<OpcodeOut> payload, Player player) {
		PacketBuilder builder = fromOpcodeEnum(payload.getOpcode(), player);
		boolean possiblyValid = PayloadValidator.isPayloadCorrectInstance(payload, payload.getOpcode());
		if (builder != null && possiblyValid) {
			switch (payload.getOpcode()) {
				case SEND_FRIEND_LIST:
					FriendListStruct fl = (FriendListStruct) payload;
					int friendSize = fl.listSize;
					builder.writeByte((byte) friendSize);
					for (int i = 0; i < friendSize; i++) {
						builder.writeLong(DataConversions.usernameToHash(fl.name[i]));
						builder.writeByte((byte) fl.worldNumber[i]);
					}
					return builder.toPacket();

				case SEND_FRIEND_UPDATE:
					FriendUpdateStruct fr = (FriendUpdateStruct) payload;
					builder.writeLong(DataConversions.usernameToHash(fr.name));
					builder.writeByte((byte) fr.worldNumber);
					return builder.toPacket();
			}
		}
		return gen.generate(payload, player);
	}
}
