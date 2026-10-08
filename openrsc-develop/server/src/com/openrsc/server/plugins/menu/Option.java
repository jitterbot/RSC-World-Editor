package com.openrsc.server.plugins.menu;

public abstract class Option {
	   
                  
    
	private String option;

	   
                           
   
                              
    
	public Option(final String string) {
		option = string;
	}

	   
                                                                
    
	public abstract void action();

	   
                               
   
           
    
	public String getOption() {
		return option;
	}
}
