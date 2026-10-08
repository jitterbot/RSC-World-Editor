package com.openrsc.server.external;

   
                                  
   
public class ObjectFishDef {

	   
                                             
    
	public int exp;
	   
                      
    
	public int fishId;
	   
                                      
    
	public int requiredLevel;

	   
                                                                                 
    
	public int lowRate;
	public int highRate;

	public SkillSuccessRate bounds;

	public double[] rate;                                                   

	public int getExp() {
		return exp;
	}

	public int getId() {
		return fishId;
	}

	public int getReqLevel() {
		return requiredLevel;
	}

	public double getRate(int level) {
		return rate[level];
	}
}
