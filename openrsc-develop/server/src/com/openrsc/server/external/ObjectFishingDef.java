package com.openrsc.server.external;

import com.openrsc.server.model.world.World;
import com.openrsc.server.util.rsc.Formulae;

   
                                           
   
public class ObjectFishingDef {

	   
                                                  
    
	public int baitId;
	   
                                    
    
	public ObjectFishDef[] defs;
	   
                                           
    
	public int netId;
	   
                                        
    
	private int depletion;
	   
                                                 
    
	private int respawnTime;

	public int getBaitId() {
		return baitId;
	}

	public ObjectFishDef[] getFishDefs() {
		return defs;
	}

	   
                                                                                              
    
	public int cascade;

	public int getNetId() {
		return netId;
	}

	public int getReqLevel(World world) {
		int requiredLevel = world.getServer().getConfig().PLAYER_LEVEL_LIMIT;
		for (ObjectFishDef def : defs) {
			if (def.getReqLevel() < requiredLevel) {
				requiredLevel = def.getReqLevel();
			}
		}
		return requiredLevel;
	}

	public int getDepletion() {
		return depletion;
	}

	public int getRespawnTime() {
		return respawnTime;
	}

	public ObjectFishDef fishingAttemptResult(int level) {
		double roll = Math.random();
		for (ObjectFishDef def : defs) {
			if (def.getRate(level) > roll) {
				return def;
			}
		}
		return null;
	}

	   
                                                                                            
    
	void calculateFishRates() {
		final int maxLevelToCalcFor = 143;

		SkillSuccessRate[] bounds = new SkillSuccessRate[defs.length];
		int i = 0;
		for (ObjectFishDef def : defs) {
			def.bounds = new SkillSuccessRate(def.lowRate, def.highRate, def.requiredLevel);
			bounds[i++] = def.bounds;
		}
		double[] rateSoFar = new double[maxLevelToCalcFor];
		if (cascade == 1) {
			for (int fishDefIdx = 0; fishDefIdx < defs.length; fishDefIdx++) {
				defs[fishDefIdx].rate = new double[maxLevelToCalcFor];
				for (int level = 0; level < maxLevelToCalcFor; level++) {
					if (level >= defs[fishDefIdx].requiredLevel) {
						rateSoFar[level] += Formulae.cascadeInterp(bounds, level, fishDefIdx);
						defs[fishDefIdx].rate[level] = rateSoFar[level];
					}
				}
			}
		} else {
			for (ObjectFishDef def : defs) {
				def.rate = new double[maxLevelToCalcFor];
				for (int level = 0; level < maxLevelToCalcFor; level++) {
					if (level >= def.requiredLevel) {
						                                                   
						def.rate[level] = Formulae.interp(def.lowRate, def.highRate, level);
					}
				}
			}
		}
	}
}
