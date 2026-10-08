package com.openrsc.server.io;

import com.openrsc.server.constants.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Sector {
	   
                                                        
    
	private final Tile[] tiles;

	   
                                            
    
	public Sector() {
		tiles = new Tile[Constants.REGION_SIZE * Constants.REGION_SIZE];
		for (int i = 0; i < tiles.length; i++) {
			tiles[i] = new Tile();
		}
	}

	   
                                                                      
    
	static Sector unpack(ByteBuffer in) throws IOException {
		final int length = Constants.REGION_SIZE * Constants.REGION_SIZE;
		if (in.remaining() < (10 * length)) {
			throw new IOException("Provided buffer too short");
		}
		Sector sector = new Sector();

		for (int i = 0; i < length; i++) {
			sector.setTile(i, Tile.unpack(in));
		}

		return sector;
	}

	   
                                    
    
	public Tile getTile(int i) {
		return tiles[i];
	}

	   
                                     
    
	public Tile getTile(int x, int y) {
		return getTile(x * Constants.REGION_SIZE + y);
	}

	   
                                                
    
	public ByteBuffer pack() throws IOException {
		ByteBuffer out = ByteBuffer.allocate(10 * tiles.length);

		for (Tile tile : tiles) {
			out.put(tile.pack());
		}

		out.flip();
		return out;
	}

	   
                                         
    
	public void setTile(int x, int y, Tile t) {
		setTile(x * Constants.REGION_SIZE + y, t);
	}

	   
                                    
    
	public void setTile(int i, Tile t) {
		tiles[i] = t;
	}

      
                                                
                                       
      
                                                  
                                                           
    
  
      
                                               
                                       
      
                                                 
                                                          
    
  
      
                                                
                                       
      
                                                  
                                                           
    
  
      
                                               
                                       
      
                                                 
                                                          
    
}
