package com.openrsc.server.event.rsc.impl.combat.scripts.all;

import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.event.rsc.impl.combat.scripts.CombatAggroScript;
import com.openrsc.server.event.rsc.impl.combat.scripts.OnCombatStartScript;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.update.ChatMessage;

public class SalarinTheTwistedDrain implements CombatAggroScript, OnCombatStartScript {

	                                        
	                                                                                          
	                                                                        

	                      

	@Override
	public boolean shouldExecute(Mob attacker, Mob defender) {
		return attacker.isNpc() && !((Npc)attacker).executedAggroScript()
				&& attacker.getID() == NpcId.SALARIN_THE_TWISTED.id();
	}

	@Override
	public void executeScript(Mob attacker, Mob defender) {
		if (attacker.isNpc()) {
			Player player = (Player) defender;
			Npc npc = (Npc) attacker;

			npc.getUpdateFlags().setChatMessage(new ChatMessage(npc, "Amshalaraz Nithcosh dimarilo", player));

			player.message("You suddenly feel much weaker");

			int[] stats = {Skill.ATTACK.id(), Skill.STRENGTH.id()};
			boolean sendUpdate = player.getClientLimitations().supportsSkillUpdate;
			for(int affectedStat : stats) {
				                                
				int lowerBy = (int) Math.floor(((player.getSkills().getLevel(affectedStat) + 20) * 0.5));
				                       
				final int newStat = Math.max(0, player.getSkills().getLevel(affectedStat) - lowerBy);
				player.getSkills().setLevel(affectedStat, newStat, sendUpdate);
			}
			if (!sendUpdate) {
				player.getSkills().sendUpdateAll();
			}
		}
	}
}
