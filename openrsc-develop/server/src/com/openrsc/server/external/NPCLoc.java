package com.openrsc.server.external;

public class NPCLoc {
	   
                     
    
	public int id;
	   
                        
    
	public int maxX;
	   
                        
    
	public int maxY;
	   
                        
    
	public int minX;
	   
                        
    
	public int minY;
	   
                    
    
	public int startX;
	   
                    
    
	public int startY;

	public NPCLoc() { }

	public NPCLoc(int id, int startX, int startY, int minX, int maxX, int minY, int maxY) {
		this.id = id;
		this.startX = startX;
		this.startY = startY;
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}

	public int getId() {
		return id;
	}

	public int maxX() {
		return maxX;
	}

	public int maxY() {
		return maxY;
	}

	public int minX() {
		return minX;
	}

	public int minY() {
		return minY;
	}

	public int startX() {
		return startX;
	}

	public int startY() {
		return startY;
	}
}
