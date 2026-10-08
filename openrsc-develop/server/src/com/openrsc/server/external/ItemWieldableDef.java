package com.openrsc.server.external;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

   
                                   
   
public class ItemWieldableDef {

	   
                                          
    
	public int armourPoints;
	   
                                         
    
	public int magicPoints;
	   
                                          
    
	public int prayerPoints;
	   
                                         
    
	public int rangePoints;
	   
                                            
    
	public HashMap<Integer, Integer> requiredStats;
	   
                         
    
	public int sprite;
	   
                
    
	public int type;
	   
                                              
    
	public int weaponAimPoints;
	   
                                                
    
	public int weaponPowerPoints;
	   
                                                                    
    
	private boolean femaleOnly;
	   
                                      
    
	private int wieldPos;

	public boolean femaleOnly() {
		return femaleOnly;
	}

	public int getArmourPoints() {
		return armourPoints;
	}

	public int getMagicPoints() {
		return magicPoints;
	}

	public int getPrayerPoints() {
		return prayerPoints;
	}

	public int getRangePoints() {
		return rangePoints;
	}

	public int getSprite() {
		return sprite;
	}

	public Set<Entry<Integer, Integer>> getStatsRequired() {
		return requiredStats.entrySet();
	}

	public int getType() {
		return type;
	}

	public int getWeaponAimPoints() {
		return weaponAimPoints;
	}

	public int getWeaponPowerPoints() {
		return weaponPowerPoints;
	}

	public int getWieldPos() {
		return wieldPos;
	}
}
