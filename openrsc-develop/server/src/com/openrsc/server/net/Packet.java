package com.openrsc.server.net;

import io.netty.buffer.ByteBuf;

public class Packet {
	   
                      
    
	public static long nextPacketNumber = 0;

	   
               
    
	private final int opcode;

	   
                      
    
	private final long packetNumber;

	   
                
    
	private final ByteBuf payload;

	public Packet(final int opcode, final ByteBuf payload) {
		this.opcode = opcode;
		this.payload = payload;
		this.packetNumber = getNextPacketNumber();
	}

	   
                                                                      
                                   
   
                                                               
    
	public boolean isRaw() {
		return opcode == -1;
	}

	   
                     
   
                       
    
	public int getID() {
		return opcode;
	}

	   
                      
   
                        
    
	public ByteBuf getBuffer() {
		return payload;
	}

	   
                     
   
                       
    
	public int getLength() {
		return payload.capacity();
	}

	   
                        
   
                          
    
	public byte read() {
		return payload.readByte();
	}

	   
                        
   
                               
    
	public void read(final byte[] b) {
		payload.readBytes(b);
	}

	   
                 
   
                          
    
	public byte readByte() {
		return read();
	}


	public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = payload.readByte();
        }
	    return bytes;
	}

	   
                           
   
                             
    
	public int readUnsignedByte() {
		return payload.readByte() & 0xff;
	}

	   
                  
   
                    
    
	public short readShort() {
		return payload.readShort();
	}

	public short readAnotherShort() {
		try {
			return (short) ((short) ((payload.readByte() & 0xff) << 8) | (short) (payload.readByte() & 0xff));
		} catch (Exception e) {
			System.out.println("Error reading packet (short)");
			return 0;
		}
	}

	public int readUnsignedShort() {
		return payload.readUnsignedShort();
	}


	   
                     
   
                       
    
	public int readInt() {
		return payload.readInt();
	}

	   
                 
   
                   
    
	public long readLong() {
		return payload.readLong();
	}

	   
                        
   
                       
    
	public String readString() {
		StringBuilder bldr = new StringBuilder();
		byte b;
		while (payload.readableBytes() > 0 && (b = payload.readByte()) != 10)
			bldr.append((char) b);
		return bldr.toString();
	}

	   
                        
   
                       
    
	public String readZeroPaddedString() {
		StringBuilder bldr = new StringBuilder();
		byte b;
		if (payload.readByte() != 0) {
			return "";
		}
		while (payload.readableBytes() > 0 && (b = payload.readByte()) != 0)
			bldr.append((char) b);
		return bldr.toString();
	}

	   
                        
   
                       
    
	public String readString(int len) {
		StringBuilder bldr = new StringBuilder();
		int length = len;
		while (payload.readableBytes() > 0 && length-- > 0)
			bldr.append((char) payload.readByte());
		return bldr.toString();
	}

	   
                            
   
                                         
                             
                             
    
	public void read(final byte[] is, final int offset, final int length) {
		for (int i = 0; i < length; i++)
			is[offset + i] = read();
	}

	public byte[] readRemainingData() {
		byte[] data = new byte[payload.readableBytes()];
		payload.readBytes(data);
		return data;
	}

	public int getReadableBytes() {
		return payload.readableBytes();
	}

	public int getSmart08_16() {
		int byte1 = getBuffer().getByte(getBuffer().readerIndex()) & 0xFF;
		return byte1 < 128 ? getBuffer().readUnsignedByte() : getBuffer().readUnsignedShort() - 32768;
	}

	public long getPacketNumber() {
		return packetNumber;
	}

	public static long getNextPacketNumber() {
		return nextPacketNumber++;
	}

	public static void printPacket(Packet packet, String direction) {
		int length = packet.getReadableBytes();
		int opcode = packet.getID();
		ByteBuf buffer = packet.getBuffer();
		System.out.print(String.format("%s Packet Opcode %d:", direction, opcode));
		for (int i=0; i < length; i++) {
			System.out.print(String.format(" %d", Byte.toUnsignedInt(buffer.readByte())));
		}
		System.out.println();
		buffer.resetReaderIndex();
	}
	public static void printBuffer(ByteBuf buffer, String direction) {
		ByteBuf bufferDup = buffer.duplicate();
		bufferDup.resetReaderIndex();
		int length = bufferDup.readableBytes();
		System.out.print(String.format("%s Packet:", direction));
		for (int i=0; i < length; i++) {
			System.out.print(String.format(" %d", Byte.toUnsignedInt(bufferDup.readByte())));
		}
		System.out.println();
	}
}
