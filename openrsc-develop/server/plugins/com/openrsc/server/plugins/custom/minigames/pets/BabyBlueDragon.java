package com.openrsc.server.plugins.custom.minigames.pets;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.UseNpcTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class BabyBlueDragon implements UseNpcTrigger {

	@Override
	public boolean blockUseNpc(Player player, Npc npc, Item item) {
		return npc.getID() == NpcId.BABY_BLUE_DRAGON.id() && item.getCatalogId() == ItemId.A_GLOWING_RED_CRYSTAL.id();
	}

	@Override
	public void onUseNpc(Player player, Npc npc, Item item) {
		if (config().WANT_PETS) {
			npc.resetPath();
			                   
			npc.face(player);
			player.face(npc);
			thinkbubble(item);
			player.message("You attempt to put the baby blue dragon in the crystal.");
			delay(2);
			                                                                                                                                                  
                           
                                              
                      
                                         
                           
                                          
                           
                                          
                           
                                        
                             
                                         
                               
                                             
     
                                                                                                                                                                                                                 
                                 
                                     
                             
      
			if (random(0, 4) != 0) {
				player.message("You catch the baby blue dragon in the crystal.");
				player.getCarriedItems().remove(new Item(ItemId.A_GLOWING_RED_CRYSTAL.id()));
				give(player, ItemId.A_RED_CRYSTAL.id(), 1);
				npc.remove();
			} else {
				player.message("The baby blue dragon manages to get away from you!");
			}
		}
	}
}
