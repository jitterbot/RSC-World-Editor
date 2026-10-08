package com.openrsc.server.external;

   
                                   
   
public class ItemSmeltingDef {

	   
                             
    
	public int barId;
	   
                                    
    
	public int exp;
	   
                                             
    
	public ReqOreDef[] reqOres;
	   
                                    
    
	public int requiredLvl;

	public int getBarId() {
		return barId;
	}

	public int getExp() {
		return exp;
	}

	public int getReqLevel() {
		return requiredLvl;
	}

	public ReqOreDef[] getReqOres() {
		return reqOres;
	}

}
