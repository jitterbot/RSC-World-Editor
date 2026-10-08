package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;
import com.openrsc.server.util.rsc.MathUtil;

public class StatInfoStruct  extends AbstractStruct<OpcodeOut> {

	public int currentAttack = 1;
	public int maxAttack = 1;
	public int experienceAttack;

	public int currentDefense = 1;
	public int maxDefense = 1;
	public int experienceDefense;

	public int currentStrength = 1;
	public int maxStrength = 1;
	public int experienceStrength;

	public int currentHits = 1;
	public int maxHits = 1;
	public int experienceHits;

	public int currentRanged = 1;
	public int maxRanged = 1;
	public int experienceRanged;

	                  
	public int currentInfluence = 1;                           
	public int maxInfluence = 1;                           
	public int experienceInfluence;                           

	                  
	public int currentPrayGood = 1;                           
	public int maxPrayGood = 1;                           
	public int experiencePrayGood;                           

	                  
	public int currentPrayEvil = 1;                           
	public int maxPrayEvil = 1;                           
	public int experiencePrayEvil;                           

	public int currentPrayer = 1;                           
	public int maxPrayer = 1;                           
	public int experiencePrayer;                           

	                  
	public int currentGoodMagic = 1;                           
	public int maxGoodMagic = 1;                           
	public int experienceGoodMagic;                           

	                  
	public int currentEvilMagic = 1;                           
	public int maxEvilMagic = 1;                           
	public int experienceEvilMagic;                           

	public int currentMagic = 1;                           
	public int maxMagic = 1;                           
	public int experienceMagic;                           

	public int currentCooking = 1;
	public int maxCooking = 1;
	public int experienceCooking;

	public int currentWoodcutting = 1;
	public int maxWoodcutting = 1;
	public int experienceWoodcutting;

	                  
	public int currentTailoring = 1;
	public int maxTailoring = 1;
	public int experienceTailoring;

	public int currentFletching = 1;
	public int maxFletching = 1;
	public int experienceFletching;

	public int currentFishing = 1;
	public int maxFishing = 1;
	public int experienceFishing;

	public int currentFiremaking = 1;
	public int maxFiremaking = 1;
	public int experienceFiremaking;

	public int currentCrafting = 1;
	public int maxCrafting = 1;
	public int experienceCrafting;

	public int currentSmithing = 1;
	public int maxSmithing = 1;
	public int experienceSmithing;

	public int currentMining = 1;
	public int maxMining = 1;
	public int experienceMining;

	public int currentHerblaw = 1;
	public int maxHerblaw = 1;
	public int experienceHerblaw;

	                                                          
	public int currentCarpentry = 1;
	public int maxCarpentry = 1;
	public int experienceCarpentry;

	public int currentAgility = 1;
	public int maxAgility = 1;
	public int experienceAgility;

	public int currentThieving = 1;
	public int maxThieving = 1;
	public int experienceThieving;

	                   
	public int currentRunecrafting = 1;
	public int maxRunecrafting = 1;
	public int experienceRunecrafting;

	                   
	public int currentHarvesting = 1;
	public int maxHarvesting = 1;
	public int experienceHarvesting;

	public int questPoints;
	public int computedInfluence = 1;                              
	public int computedExperienceInfluence;                                                   
	public boolean useInfluence = false;
	public boolean hasPrayDrain = false;

	public int getCurrentPrayGood() {
		int min = Math.min(currentPrayGood, currentPrayer);
		int max = Math.max(currentPrayGood, currentPrayer);
		return (!hasPrayDrain || min > 0) ? max : 0;
	}

	public int getCurrentPrayEvil() {
		int min = Math.min(currentPrayEvil, currentPrayer);
		int max = Math.max(currentPrayEvil, currentPrayer);
		return (!hasPrayDrain || min > 0) ? max : 0;
	}

	public int getCurrentPrayer() {
		int min = Math.min(currentPrayer, Math.min(currentPrayGood, currentPrayEvil));
		int max = Math.max(currentPrayer, Math.max(currentPrayGood, currentPrayEvil));
		return (!hasPrayDrain || min > 0) ? max : 0;
	}

	public int getCurrentGoodMagic() {
		return Math.max(currentGoodMagic, currentMagic);
	}

	public int getCurrentEvilMagic() {
		return Math.max(currentEvilMagic, currentMagic);
	}

	public int getCurrentMagic() {
		return Math.max(currentMagic, Math.max(currentGoodMagic, currentEvilMagic));
	}

	public int getCurrentInfluence() {
		return useInfluence ? currentInfluence : computedInfluence;
	}

	public int getMaxPrayGood() {
		return Math.max(maxPrayGood, maxPrayer);
	}

	public int getMaxPrayEvil() {
		return Math.max(maxPrayEvil, maxPrayer);
	}

	public int getMaxPrayer() {
		return Math.max(maxPrayer, Math.max(maxPrayGood, maxPrayEvil));
	}

	public int getMaxGoodMagic() {
		return Math.max(maxGoodMagic, maxMagic);
	}

	public int getMaxEvilMagic() {
		return Math.max(maxEvilMagic, maxMagic);
	}

	public int getMaxMagic() {
		return Math.max(maxMagic, Math.max(maxGoodMagic, maxEvilMagic));
	}

	public int getMaxInfluence() {
		return useInfluence ? maxInfluence : computedInfluence;
	}

	public int getExperiencePrayGood() {
		return MathUtil.maxUnsigned(experiencePrayGood, experiencePrayer);
	}

	public int getExperiencePrayEvil() {
		return MathUtil.maxUnsigned(experiencePrayEvil, experiencePrayer);
	}

	public int getExperiencePrayer() {
		return MathUtil.maxUnsigned(experiencePrayer, MathUtil.maxUnsigned(experiencePrayGood, experiencePrayEvil));
	}

	public int getExperienceGoodMagic() {
		return MathUtil.maxUnsigned(experienceGoodMagic, experienceMagic);
	}

	public int getExperienceEvilMagic() {
		return MathUtil.maxUnsigned(experienceEvilMagic, experienceMagic);
	}

	public int getExperienceMagic() {
		return MathUtil.maxUnsigned(experienceMagic, MathUtil.maxUnsigned(experienceGoodMagic, experienceEvilMagic));
	}

	public int getExperienceInfluence() {
		return useInfluence ? experienceInfluence : computedExperienceInfluence;
	}
}
