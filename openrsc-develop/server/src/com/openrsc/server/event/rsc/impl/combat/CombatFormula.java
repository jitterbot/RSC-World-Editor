package com.openrsc.server.event.rsc.impl.combat;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.content.SkillCapes;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.Prayers;
import com.openrsc.server.util.rsc.DataConversions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

import static com.openrsc.server.constants.ItemId.*;

public class CombatFormula {
	   
                   
    
	private static final Logger LOGGER = LogManager.getLogger();

	   
                                                         
                                                                               
                                                                                      
                                                
                                               
                                 
    
	private static int calculateMeleeDamage(final Mob source) {
		int maxRoll = getMeleeDamage(source);
		int chosenHit = maxRoll <= 0 ? 0 : (DataConversions.getRandom().nextInt(maxRoll) + 320) / 640;
		return chosenHit;
	}

	   
                                                          
                                                                               
                                                                                      
                                                
                                               
                                                  
                                 
    
	private static int calculateRangedDamage(final Mob source, final int bowId, final int arrowId) {
		int maxRoll = getRangedDamage(source, bowId, arrowId);
		int chosenHit = (DataConversions.getRandom().nextInt(maxRoll) + 320) / 640;
		return chosenHit;
	}

	   
                                                          
                                                   
                                 
    
	public static int calculateMagicDamage(final double spellPower) {
		                                                                                                                                            
		                                                                                                       
		return DataConversions.getRandom().nextInt((int)Math.floor(spellPower) + 1);
	}

	   
                                                                      
   
                                                    
                                 
    
	public static int calculateGodSpellDamage(final Player source) {
		int[] godCapes = new int[] {
			ZAMORAK_CAPE.id(),
			SARADOMIN_CAPE.id(),
			GUTHIX_CAPE.id()
		};

		                                                                                      
		boolean hasCapeEquipped = false;
		for (int capeId : godCapes) {
			if (source.getCarriedItems().getEquipment().hasEquipped(capeId)) {
				hasCapeEquipped = true;
				break;
			}
		}
		boolean hasChargeBenefit = source.isCharged() && hasCapeEquipped;
		int godSpellMax = hasChargeBenefit ? 25 : 18;

		return calculateMagicDamage(godSpellMax);
	}

	   
                                                                      
   
                                 
    
	public static int calculateIbanSpellDamage() {
		                                                               
		                                                                                                                    
		return calculateMagicDamage(15);
	}

	   
                                              
   
                                                
                                               
                                                                      
    
	private static boolean calculateAccuracy(final double accuracy, final double defence) {
		double hitChance;
		if (accuracy > defence) {
			hitChance = 1 - ((defence + 2) / (2 * (accuracy + 1)));
		} else {
			hitChance = (accuracy) / (2 * (defence + 1));
		}

		double rand = Math.random();
		boolean didHit = rand <= hitChance;

		return didHit;
	}


	   
                                        
   
                                                
                                                     
                                                                      
    
	private static boolean calculateMeleeAccuracy(final Mob source, final Mob victim) {
		return calculateAccuracy(getMeleeAccuracy(source), getMeleeDefence(victim));
	}

	   
                                         
   
                                                
                                                            
                                                     
                                                                      
    
	private static boolean calculateRangedAccuracy(final Mob source, final int bowId, final Mob victim) {
		return calculateAccuracy(getRangedAccuracy(source, bowId), getMeleeDefence(victim));
	}

	   
                                                                          
   
                                                
                                                     
                              
    
	public static int doMeleeDamage(final Mob source, final Mob victim) {
		boolean isHit = calculateMeleeAccuracy(source, victim);
		boolean wasHit = isHit;
		int damage = calculateMeleeDamage(source);
		if (victim instanceof Player) {
			                                       
			Player playerVictim = (Player)victim;
			if (isHit) {
				int damageToPlayer = damage;
				int blockedDamage = 0;

				                    
				if (SkillCapes.shouldActivate((Player) victim, DEFENSE_CAPE) && damageToPlayer > 0) {
					damage /= 2;
					blockedDamage = damage;
				}

				playerVictim.updateDamageAndBlockedDamageTracking(source, damageToPlayer, blockedDamage);
			}
		}
		if (source instanceof Player) {
			while(SkillCapes.shouldActivate((Player)source, ATTACK_CAPE, isHit)){
				isHit = calculateMeleeAccuracy(source, victim);
			}
			if (!wasHit && isHit)
				((Player) source).message("@red@Your Attack cape has prevented a zero hit");

			final double maximum = (double) (getMeleeDamage(source) + 320) / 640;
			if (damage >= (maximum * 0.5) && SkillCapes.shouldActivate((Player) source, STRENGTH_CAPE, isHit)) {
				damage += (maximum*0.2);
				((Player) source).message("@ora@Your Strength cape has granted you a critical hit");
			}
		}

		                                                                                                

		return isHit ? damage : 0;
	}

	   
                                                                          
   
                                                
                                                  
                                                     
                                                     
                              
    
	public static int doRangedDamage(final Mob source, final int bowId, final int arrowId, final Mob victim, final boolean skillCape) {
		boolean isHit = calculateRangedAccuracy(source, bowId, victim);

		if (!isHit) return 0;

		if (skillCape) {
			int maxHit = (getRangedDamage(source, bowId, arrowId) + 320) / 640;
			return DataConversions.getRandom().nextInt(maxHit * 2);
		}

		                                                                                                

		return calculateRangedDamage(source, bowId, arrowId);
	}

	   
                                                 
                                                
                       
    
	private static int getMeleeDamage(final Mob source) {
		final int styleBonus = styleBonus(source, 2);
		final double prayerBonus = addPrayers(source, Prayers.BURST_OF_STRENGTH,
			Prayers.SUPERHUMAN_STRENGTH,
			Prayers.ULTIMATE_STRENGTH);

		final int bonusConstant = source.isPlayer() ? 8 : 0;
		final double maxRoll = (Math.floor(source.getSkills().getLevel(Skill.STRENGTH.id()) * prayerBonus) + bonusConstant + styleBonus) * (source.getWeaponPowerPoints() + 64);
		return (int)maxRoll;
	}

	   
                                                  
   
                                                
                       
    
	private static int getRangedDamage(final Mob source, final int bowId, final int arrowId) {
		final int bonusConstant = source.isPlayer() ? 8 : 0;                                                                                                
		final int power = source.getConfig().RETRO_RANGED_DAMAGE ? rangedPowerRetro(bowId) : rangedPower(arrowId);
		final double maxRoll = (source.getSkills().getLevel(Skill.RANGED.id()) + bonusConstant) * (power + 1 + 64);
		return (int)maxRoll;
	}

	   
                                               
   
                                                  
                             
    
	private static double getMeleeDefence(final Mob defender) {
		final int styleBonus = styleBonus(defender, 1);
		final double prayerBonus = addPrayers(defender, Prayers.THICK_SKIN,
			Prayers.ROCK_SKIN,
			Prayers.STEEL_SKIN);
		final int bonusConstant = defender.isPlayer() ? 8 : 0;
		final double defense = (Math.floor(defender.getSkills().getLevel(Skill.DEFENSE.id()) * prayerBonus) + bonusConstant + styleBonus) * (defender.getArmourPoints() + 64);
		return defense;
	}


	   
                                                 
   
                                                  
                                                      
                               
    
	private static double getRangedAccuracy(final Mob attacker, final int bowId) {
		final int bonusConstant = attacker.isPlayer() ? 8 : 0;                                                                                                
		return (attacker.getSkills().getLevel(Skill.RANGED.id()) + bonusConstant) * (rangedAim(bowId) + 1 + 64);
	}

	   
                                                
   
                                                  
                              
    
	private static double getMeleeAccuracy(final Mob attacker) {
		final int styleBonus = styleBonus(attacker, 0);
		final double prayerBonus = addPrayers(attacker, Prayers.CLARITY_OF_THOUGHT,
			Prayers.IMPROVED_REFLEXES,
			Prayers.INCREDIBLE_REFLEXES);

		final int bonusConstant = attacker.isPlayer() ? 8 : 0;
		final double accuracy = (Math.floor(attacker.getSkills().getLevel(Skill.ATTACK.id()) * prayerBonus) + bonusConstant + styleBonus) * (attacker.getWeaponAimPoints() + 64);

		return accuracy;
	}

	   
                                                                                         
   
                                                  
                                                              
    
	protected static int styleBonus(final Mob attacker, final int skill) {
		if (attacker.isNpc())
			return 0;

		final int style = attacker.getCombatStyle();
		if (style == Skills.CONTROLLED_MODE)
			return 1;

		return (skill == Skill.ATTACK.id() && style == Skills.ACCURATE_MODE) || (skill == Skill.DEFENSE.id() && style == Skills.DEFENSIVE_MODE)
			|| (skill == Skill.STRENGTH.id() && style == Skills.AGGRESSIVE_MODE) ? 3 : 0;
	}

	   
                                                         
   
                                              
                                                                                  
    
	protected static double addPrayers(final Mob source, final int prayer1, final int prayer2, final int prayer3) {
		if (source.isPlayer()) {
			final Player sourcePlayer = (Player) source;
			if (sourcePlayer.getPrayers().isPrayerActivated(prayer3)) {
				return 1.15D;
			}
			if (sourcePlayer.getPrayers().isPrayerActivated(prayer2)) {
				return 1.1D;
			}
			if (sourcePlayer.getPrayers().isPrayerActivated(prayer1)) {
				return 1.05D;
			}
		}
		return 1.0D;
	}

	   
                                                                      
   
                                                                            
    
	private static int rangedPowerRetro(final int bowId) {
		switch (ItemId.getById(bowId)) {
			case SHORTBOW:
				return 14;
			case LONGBOW:
				return 20;
			case CROSSBOW:
			case PHOENIX_CROSSBOW:
				return 22;
			default:
				return 0;
		}
	}

	   
                                                                         
    
	private static int rangedPower(final int arrowId) {
		   
                                                 
                                                          
                                     
                                                               
                                                                                                                                               
                                                                                                      
                                                        
     
		switch (ItemId.getById(arrowId)) {
			case BRONZE_THROWING_DART:
			case POISONED_BRONZE_THROWING_DART:
			case BRONZE_ARROWS:
			case POISON_BRONZE_ARROWS:
				return 15;
			case IRON_THROWING_DART:
			case POISONED_IRON_THROWING_DART:
				return 17;
			case IRON_ARROWS:
			case POISON_IRON_ARROWS:
			case CROSSBOW_BOLTS:
			case POISON_CROSSBOW_BOLTS:
				return 20;
			case STEEL_THROWING_DART:
			case POISONED_STEEL_THROWING_DART:
				return 22;
			case STEEL_ARROWS:
			case POISON_STEEL_ARROWS:
			case MITHRIL_THROWING_DART:
			case POISONED_MITHRIL_THROWING_DART:
			case BRONZE_THROWING_KNIFE:
			case POISONED_BRONZE_THROWING_KNIFE:
				return 25;
			case ADAMANTITE_THROWING_DART:
			case POISONED_ADAMANTITE_THROWING_DART:
				return 27;
			case RUNE_THROWING_DART:
			case POISONED_RUNE_THROWING_DART:
			case MITHRIL_ARROWS:
			case POISON_MITHRIL_ARROWS:
			case OYSTER_PEARL_BOLTS:
			case IRON_THROWING_KNIFE:
			case POISONED_IRON_THROWING_KNIFE:
				return 30;
			case ADAMANTITE_ARROWS:
			case POISON_ADAMANTITE_ARROWS:
			case STEEL_THROWING_KNIFE:
			case POISONED_STEEL_THROWING_KNIFE:
			case BLACK_THROWING_KNIFE:
			case POISONED_BLACK_THROWING_KNIFE:
				return 35;
			case RUNE_ARROWS:
			case POISON_RUNE_ARROWS:
			case MITHRIL_THROWING_KNIFE:
			case POISONED_MITHRIL_THROWING_KNIFE:
				return 40;
			case ADAMANTITE_THROWING_KNIFE:
			case POISONED_ADAMANTITE_THROWING_KNIFE:
				return 45;
			case RUNE_THROWING_KNIFE:
			case POISONED_RUNE_THROWING_KNIFE:
			case DRAGON_ARROWS:
			case POISON_DRAGON_ARROWS:
			case DRAGON_BOLTS:
			case POISON_DRAGON_BOLTS:
				return 50;
			case BRONZE_SPEAR:
			case POISONED_BRONZE_SPEAR:
				return 29;
			case IRON_SPEAR:
			case POISONED_IRON_SPEAR:
				return 37;
			case STEEL_SPEAR:
			case POISONED_STEEL_SPEAR:
				return 46;
			case MITHRIL_SPEAR:
			case POISONED_MITHRIL_SPEAR:
				return 53;
			case ADAMANTITE_SPEAR:
			case POISONED_ADAMANTITE_SPEAR:
				return 61;
			case RUNE_SPEAR:
			case POISONED_RUNE_SPEAR:
				return 69;
			default:
				return 0;
		}
	}

	   
                                                     
    
	private static int rangedAim(final int bowId) {
		   
                                                        
                                                        
               
    
                                                           
                                
    
                                                           
           
    
                                                         
                                                         
            
     
		switch (ItemId.getById(bowId)) {
			case SHORTBOW:
				return 10;
			case CROSSBOW:
			case PHOENIX_CROSSBOW:
				return 12;
			case LONGBOW:
			case OAK_SHORTBOW:
				return 15;
			case WILLOW_SHORTBOW:
			case OAK_LONGBOW:
				return 20;
			case BRONZE_THROWING_DART:
			case POISONED_BRONZE_THROWING_DART:
			case MAPLE_SHORTBOW:
			case WILLOW_LONGBOW:
				return 25;
			case IRON_THROWING_DART:
			case POISONED_IRON_THROWING_DART:
			case BRONZE_THROWING_KNIFE:
			case POISONED_BRONZE_THROWING_KNIFE:
			case YEW_SHORTBOW:
			case MAPLE_LONGBOW:
				return 30;
			case STEEL_THROWING_DART:
			case POISONED_STEEL_THROWING_DART:
			case IRON_THROWING_KNIFE:
			case POISONED_IRON_THROWING_KNIFE:
			case MAGIC_SHORTBOW:
			case YEW_LONGBOW:
				return 35;
			case MITHRIL_THROWING_DART:
			case POISONED_MITHRIL_THROWING_DART:
			case BLACK_THROWING_KNIFE:
			case POISONED_BLACK_THROWING_KNIFE:
			case STEEL_THROWING_KNIFE:
			case POISONED_STEEL_THROWING_KNIFE:
			case MAGIC_LONGBOW:
			case DRAGON_CROSSBOW:
				return 40;
			case ADAMANTITE_THROWING_DART:
			case POISONED_ADAMANTITE_THROWING_DART:
			case MITHRIL_THROWING_KNIFE:
			case POISONED_MITHRIL_THROWING_KNIFE:
				return 45;
			case RUNE_THROWING_DART:
			case POISONED_RUNE_THROWING_DART:
			case ADAMANTITE_THROWING_KNIFE:
			case POISONED_ADAMANTITE_THROWING_KNIFE:
				return 50;
			case RUNE_THROWING_KNIFE:
			case POISONED_RUNE_THROWING_KNIFE:
				return 55;
			case BRONZE_SPEAR:
			case POISONED_BRONZE_SPEAR:
				return 25;
			case IRON_SPEAR:
			case POISONED_IRON_SPEAR:
				return 33;
			case STEEL_SPEAR:
			case POISONED_STEEL_SPEAR:
				return 41;
			case MITHRIL_SPEAR:
			case POISONED_MITHRIL_SPEAR:
				return 49;
			case ADAMANTITE_SPEAR:
			case POISONED_ADAMANTITE_SPEAR:
				return 57;
			case RUNE_SPEAR:
			case POISONED_RUNE_SPEAR:
				return 65;
			default:
				return 0;
		}
	}
}
