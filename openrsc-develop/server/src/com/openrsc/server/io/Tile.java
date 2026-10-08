package com.openrsc.server.io;

import java.io.IOException;
import java.nio.ByteBuffer;

   
                                                    
   
public class Tile {
	   
                                             
    
	short diagonalWalls = 0;
	   
                              
    
	byte groundElevation = 0;
	   
                          
    
	byte groundOverlay = 0;
	   
                               
    
	byte groundTexture = 0;
	   
                                                      
    
	byte horizontalWall = 0;
	   
                                           
    
	byte roofTexture = 0;
	   
                                                    
    
	byte verticalWall = 0;

	   
                                                                    
    
	static Tile unpack(ByteBuffer in) throws IOException {
		if (in.remaining() < 10) {
			throw new IOException("Provided buffer too short");
		}
		Tile tile = new Tile();

		tile.groundElevation = in.get();
		tile.groundTexture = in.get();
		tile.groundOverlay = in.get();
		tile.roofTexture = in.get();
		tile.horizontalWall = in.get();
		tile.verticalWall = in.get();
		tile.diagonalWalls = (short)in.getInt();

		return tile;
	}

	   
                                              
    
	ByteBuffer pack() throws IOException {
		ByteBuffer out = ByteBuffer.allocate(10);

		out.put(groundElevation);
		out.put(groundTexture);
		out.put(groundOverlay);
		out.put(roofTexture);

		out.put(horizontalWall);
		out.put(verticalWall);
		out.putInt(diagonalWalls);

		out.flip();
		return out;
	}


}
