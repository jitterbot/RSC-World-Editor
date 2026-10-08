package com.openrsc.server.external;

   
                                          
   
public class GameObjectDef extends EntityDef {

	   
                                   
    
	public String command1;
	   
                                    
    
	public String command2;
	   
                                                  
    
	public int groundItemVar;
	   
                            
    
	public int height;
	public String objectModel;
	   
                    
           
                              
                                                
    
	public int type;

	   
                           
    
	public int width;

	public String getCommand1() {
		return command1.toLowerCase();
	}

	public String getCommand2() {
		return command2.toLowerCase();
	}

	public int getGroundItemVar() {
		return groundItemVar;
	}

	public int getHeight() {
		return height;
	}

	public String getObjectModel() {
		return objectModel;
	}

	                                               
	          
	                                         
	          
	          
	public int getType() {
		return type;
	}

	public int getWidth() {
		return width;
	}
}
