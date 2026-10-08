package com.openrsc.server.event.rsc.impl.combat.scripts.all;

import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.event.rsc.impl.combat.scripts.CombatScript;
import com.openrsc.server.model.entity.Mob;

public class TutorialIslandRat implements CombatScript {

	@Override
	public void executeScript(Mob attacker, Mob victim) {
		                         
		                                                                                          

		                                                                            
		                                                                    
		                                                                               
		attacker.damage(attacker.getSkills().getLevel(Skill.HITS.id()));
	}

	@Override
	public boolean shouldExecute(Mob attacker, Mob victim) {
		if (attacker.isNpc()) {
			return attacker.getID() == NpcId.RAT_TUTORIAL.id() && victim.getSkills().getLevel(Skill.HITS.id()) <= 3;
		}
		return false;
	}

	@Override
	public boolean shouldCombatStop() {
		return false;
	}

}
