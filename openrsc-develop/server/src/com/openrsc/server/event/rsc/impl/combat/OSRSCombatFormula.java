package com.openrsc.server.event.rsc.impl.combat;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.content.SkillCapes;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.Prayers;
import com.openrsc.server.util.rsc.DataConversions;

import static com.openrsc.server.constants.ItemId.ATTACK_CAPE;
import static com.openrsc.server.constants.ItemId.STRENGTH_CAPE;

public class OSRSCombatFormula {

	   
                                     
                                        
    
	private static boolean rollHit(final double hitChance) {
		return hitChance >= Math.random();
	}

	   
                                
    
	private static int rollDamage(final int maxHit) {
		return DataConversions.random(0, maxHit);
	}

	   
                                                 
    
	public static class Melee {
		   
                                     
                                         
                                                                       
          
                                         
                                                 
                                                   
     
		private static int calcEffectiveStrength(final Mob attacker) {
			final int styleBonus = CombatFormula.styleBonus(attacker, Skill.STRENGTH.id());
			final double prayerBonus = CombatFormula.addPrayers(attacker, Prayers.BURST_OF_STRENGTH,
				Prayers.SUPERHUMAN_STRENGTH,
				Prayers.ULTIMATE_STRENGTH);

			return (int)(attacker.getSkills().getLevel(Skill.STRENGTH.id()) * prayerBonus) + styleBonus + 8;
		}

		   
                                
                                                   
               
                     
                                         
                                                 
                                                     
     
		private static int calcMaxHit(final Mob attacker) {
			return (calcEffectiveStrength(attacker) * (attacker.getWeaponPowerPoints() + 64) + 320) / 640;
		}

		   
                                   
                                         
                                                                     
          
                                         
                                                 
                                                          
     
		private static int calcEffectiveAttackLevel(final Mob attacker) {
			final int styleBonus = CombatFormula.styleBonus(attacker, Skill.ATTACK.id());
			final double prayerBonus = CombatFormula.addPrayers(attacker, Prayers.CLARITY_OF_THOUGHT,
				Prayers.IMPROVED_REFLEXES,
				Prayers.INCREDIBLE_REFLEXES);

			return (int)(attacker.getSkills().getLevel(Skill.ATTACK.id()) * prayerBonus) + styleBonus + 8;
		}

		   
                                                              
                                         
                                                 
                                     
     
		private static int calcAttackRoll(final Mob attacker) {
			return calcEffectiveAttackLevel(attacker) * (attacker.getWeaponAimPoints() + 64);
		}

		   
                                    
                                         
                                                                      
          
                                         
                                                 
                                                   
     
		private static int calcEffectiveDefense(final Mob defender) {
			final int styleBonus = CombatFormula.styleBonus(defender, Skill.DEFENSE.id());
			final double prayerBonus = CombatFormula.addPrayers(defender, Prayers.THICK_SKIN,
				Prayers.ROCK_SKIN,
				Prayers.STEEL_SKIN);

			return (int)(defender.getSkills().getLevel(Skill.DEFENSE.id()) * prayerBonus) + styleBonus + 8;
		}

		   
           
                                         
                   
                                                                
                                                 
                                              
     
		private static int calcDefenseRoll(final Mob defender) {
			if (defender.isNpc()) {
				return (defender.getSkills().getLevel(Skill.DEFENSE.id()) + 9) * 64;
			} else {
				return calcEffectiveDefense(defender) * (defender.getArmourPoints() + 64);
			}
		}

		   
                                                                      
                                                
                                                
                              
     
		private static double calcHitChance(final Mob attacker, final Mob defender) {
			final int attackRoll = calcAttackRoll(attacker);
			final int defenseRoll = calcDefenseRoll(defender);

			if (attackRoll > defenseRoll) {
				return 1 - ((defenseRoll + 2.0)/(2.0 * (attackRoll + 1.0)));
			} else {
				return 1 - (attackRoll/(2.0 * (defenseRoll + 1.0)));
			}
		}

		public static int doMeleeDamage(final Mob attacker, final Mob defender) {
			                                      
			if (attacker.isNpc() && attacker.getSkills().getLevel(Skill.STRENGTH.id()) < 5)
				return 0;

			final double hitChance = calcHitChance(attacker, defender);

			boolean isHit = rollHit(hitChance);
			boolean wasHit = isHit;
			int damage = rollDamage(calcMaxHit(attacker));

			                                       
			if (attacker.isPlayer()) {
				while(SkillCapes.shouldActivate((Player)attacker, ATTACK_CAPE, isHit)){
					isHit = rollHit(hitChance);
				}
				if (!wasHit && isHit)
					((Player) attacker).message("@red@Your Attack cape has prevented a zero hit");

				                                         
				final int maxHit = calcMaxHit(attacker);
				if (damage >= maxHit - (maxHit * 0.5) && SkillCapes.shouldActivate((Player) attacker, STRENGTH_CAPE, isHit)) {
					damage += (maxHit*0.2);
					((Player) attacker).message("@ora@Your Strength cape has granted you a critical hit");
				}
			}

			return isHit ? damage : 0;
		}
	}

	   
                                                  
    
	public static class Ranged {
		   
                                                                                                    
                                                   
                                                
                                         
     
		private static int calcEffectiveRangeStrength(final Mob attacker) {
			return attacker.getSkills().getLevel(Skill.RANGED.id()) + 8;
		}

		   
                                                                                                        
                                           
                                                
                                                       
                                                           
     
		private static int calcMaxHit(final Mob attacker, final int arrowId) {
			return (int)(0.5 + ((calcEffectiveRangeStrength(attacker) * (rangedPower(arrowId) + 64.0)) / 640.0));
		}

		   
                                                                                                  
                                                   
                                                
                                       
     
		private static int calcEffectiveRangedAttack(final Mob attacker) {
			return attacker.getSkills().getLevel(Skill.RANGED.id()) + 8;
		}

		   
                                                                                                      
                                           
                                                
                                              
                                                           
     
		private static int calcAttackRoll(final Mob attacker, final int bowId) {
			return calcEffectiveRangedAttack(attacker) * (rangedAim(bowId) + 64);
		}

		   
                                          
                                      
                             
     
		private static int calcDefenseRoll(final Mob defender) {
			return (defender.getSkills().getLevel(Skill.DEFENSE.id()) + 9) * 64;
		}

		   
                                           
                                      
                                      
                                                            
     
		private static double calcHitChance(final Mob attacker, final Mob defender, final int bowId) {
			final int attackRoll = calcAttackRoll(attacker, bowId);
			final int defenseRoll = calcDefenseRoll(defender);

			if (attackRoll > defenseRoll) {
				return 1.0 - ((defenseRoll + 2.0)/(2.0 * attackRoll + 1.0));
			} else {
				return (attackRoll)/(2.0 * defenseRoll + 1.0);
			}
		}

		   
                                                                            
                                      
                                                    
                                                            
                                      
                                                  
     
		public static int doRangedDamage(final Mob attacker, final int bowId, final int arrowId, final Mob defender, final boolean skillCape) {
			boolean isHit = rollHit(calcHitChance(attacker, defender, bowId));
			return isHit ? rollDamage(calcMaxHit(attacker, arrowId) * (skillCape ? 2 : 1)) : 0;
		}

		   
                                                 
     
		private static int rangedPower(final int arrowId) {
			                           
			switch (ItemId.getById(arrowId)) {
				case BRONZE_THROWING_DART:
				case POISONED_BRONZE_THROWING_DART:
					return 1;
				case IRON_THROWING_DART:
				case POISONED_IRON_THROWING_DART:
					return 2;
				case BRONZE_THROWING_KNIFE:
				case POISONED_BRONZE_THROWING_KNIFE:
				case STEEL_THROWING_DART:
				case POISONED_STEEL_THROWING_DART:
					return 3;
				case IRON_THROWING_KNIFE:
				case POISONED_IRON_THROWING_KNIFE:
					return 4;
				case BRONZE_ARROWS:
				case POISON_BRONZE_ARROWS:
				case STEEL_THROWING_KNIFE:
				case POISONED_STEEL_THROWING_KNIFE:
					return 7;
				case BLACK_THROWING_KNIFE:
				case POISONED_BLACK_THROWING_KNIFE:
					return 8;
				case MITHRIL_THROWING_DART:
				case POISONED_MITHRIL_THROWING_DART:
					return 9;
				case CROSSBOW_BOLTS:
				case POISON_CROSSBOW_BOLTS:
				case BRONZE_SPEAR:
				case POISONED_BRONZE_SPEAR:
				case IRON_ARROWS:
				case POISON_IRON_ARROWS:
				case MITHRIL_THROWING_KNIFE:
				case POISONED_MITHRIL_THROWING_KNIFE:
					return 10;
				case ADAMANTITE_THROWING_KNIFE:
				case POISONED_ADAMANTITE_THROWING_KNIFE:
					return 14;
				case IRON_SPEAR:
				case POISONED_IRON_SPEAR:
				case STEEL_ARROWS:
				case POISON_STEEL_ARROWS:
					return 16;
				case ADAMANTITE_THROWING_DART:
				case POISONED_ADAMANTITE_THROWING_DART:
					return 17;
				case STEEL_SPEAR:
				case POISONED_STEEL_SPEAR:
				case MITHRIL_ARROWS:
				case POISON_MITHRIL_ARROWS:
				case OYSTER_PEARL_BOLTS:
					return 22;
				case RUNE_THROWING_KNIFE:
				case POISONED_RUNE_THROWING_KNIFE:
					return 24;
				case RUNE_THROWING_DART:
				case POISONED_RUNE_THROWING_DART:
					return 26;
				case MITHRIL_SPEAR:
				case POISONED_MITHRIL_SPEAR:
				case ADAMANTITE_ARROWS:
				case POISON_ADAMANTITE_ARROWS:
					return 31;
				case ADAMANTITE_SPEAR:
				case POISONED_ADAMANTITE_SPEAR:
				case RUNE_ARROWS:
				case POISON_RUNE_ARROWS:
					return 49;
				case RUNE_SPEAR:
				case POISONED_RUNE_SPEAR:
				case DRAGON_ARROWS:
				case POISON_DRAGON_ARROWS:
					return 60;
				case DRAGON_BOLTS:
				case POISON_DRAGON_BOLTS:
					return 122;
				default:
					return 0;
			}
		}

		   
                                                      
     
		private static int rangedAim(final int bowId) {
			                            
			                                                                                       
			                                                             
			switch (ItemId.getById(bowId)) {
				case BRONZE_THROWING_DART:
				case POISONED_BRONZE_THROWING_DART:
				case IRON_THROWING_DART:
				case POISONED_IRON_THROWING_DART:
				case STEEL_THROWING_DART:
				case POISONED_STEEL_THROWING_DART:
				case MITHRIL_THROWING_DART:
				case POISONED_MITHRIL_THROWING_DART:
				case ADAMANTITE_THROWING_DART:
				case POISONED_ADAMANTITE_THROWING_DART:
				case RUNE_THROWING_DART:
				case POISONED_RUNE_THROWING_DART:
					return 1;
				case BRONZE_THROWING_KNIFE:
				case POISONED_BRONZE_THROWING_KNIFE:
					return 4;
				case IRON_THROWING_KNIFE:
				case POISONED_IRON_THROWING_KNIFE:
					return 5;
				case CROSSBOW:
				case PHOENIX_CROSSBOW:
					return 6;
				case SHORTBOW:
				case STEEL_THROWING_KNIFE:
				case POISONED_STEEL_THROWING_KNIFE:
					return 8;
				case LONGBOW:
				case BLACK_THROWING_KNIFE:
				case POISONED_BLACK_THROWING_KNIFE:
					return 10;
				case MITHRIL_THROWING_KNIFE:
				case POISONED_MITHRIL_THROWING_KNIFE:
					return 11;
				case OAK_SHORTBOW:
					return 14;
				case ADAMANTITE_THROWING_KNIFE:
				case POISONED_ADAMANTITE_THROWING_KNIFE:
					return 15;
				case BRONZE_SPEAR:
				case POISONED_BRONZE_SPEAR:
				case OAK_LONGBOW:
					return 16;
				case WILLOW_SHORTBOW:
					return 20;
				case IRON_SPEAR:
				case POISONED_IRON_SPEAR:
				case WILLOW_LONGBOW:
					return 22;
				case RUNE_THROWING_KNIFE:
				case POISONED_RUNE_THROWING_KNIFE:
					return 25;
				case MAPLE_SHORTBOW:
					return 29;
				case STEEL_SPEAR:
				case POISONED_STEEL_SPEAR:
				case MAPLE_LONGBOW:
					return 31;
				case YEW_SHORTBOW:
					return 47;
				case MITHRIL_SPEAR:
				case POISONED_MITHRIL_SPEAR:
				case YEW_LONGBOW:
					return 49;
				case MAGIC_SHORTBOW:
					return 69;
				case ADAMANTITE_SPEAR:
				case POISONED_ADAMANTITE_SPEAR:
				case MAGIC_LONGBOW:
					return 71;
				case RUNE_SPEAR:
				case POISONED_RUNE_SPEAR:
					return 93;
				case DRAGON_CROSSBOW:
					return 94;
				default:
					return 0;
			}
		}
	}
}
