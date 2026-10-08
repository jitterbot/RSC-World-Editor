package com.openrsc.server.external;

public class FiremakingDef {
	   
                               
    
	public int exp;
	   
                                        
    
	public int length;
	   
                                                     
    
	public int level;

	public int getExp() {
		return exp;
	}

	public int getLength() {
		return length * 1000;
	}

	public int getRequiredLevel() {
		return level;
	}
}
