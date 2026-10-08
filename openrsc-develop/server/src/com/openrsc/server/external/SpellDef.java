package com.openrsc.server.external;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

   
                                    
   
public final class SpellDef extends EntityDef {

	   
                                                
    
	private int exp;

	   
                                       
    
	private int reqLevel;

	   
                                                      
    
	private HashMap<Integer, Integer> requiredRunes;

	   
                                                      
    
	private int runeCount;

	   
                         
                       
       
                                             
                                             
       
                  
                                                        
    
	private int type;

	   
                                     
    
	private boolean members;

	   
                                            
                          
    
	private boolean evil;

	public int getExp() {
		return exp;
	}

	public int getReqLevel() {
		return reqLevel;
	}

	public int getRuneCount() {
		return runeCount;
	}

	public Set<Entry<Integer, Integer>> getRunesRequired() {
		return requiredRunes.entrySet();
	}

	public int getSpellType() {
		return type;
	}

	public boolean isMembers() {
		return members;
	}

	public boolean isEvil() {
		return evil;
	}
}
