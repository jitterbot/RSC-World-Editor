package com.openrsc.server.net;

import com.openrsc.server.constants.AppearanceId;
import com.openrsc.server.util.rsc.CipheredMessage;
import com.openrsc.server.util.rsc.DataConversions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketBuilder {

	   
                   
    
	private static final int[] BIT_MASK_OUT = new int[32];

	  
                              
    
	static {
		for (int i = 0; i < BIT_MASK_OUT.length; i++) {
			BIT_MASK_OUT[i] = (1 << i) - 1;
		}
	}

	   
               
    
	private int opcode;

	   
                
    
	private ByteBuf payload = Unpooled.buffer();

	   
                             
    
	private int bitPosition;

	   
                                 
    
	public PacketBuilder() {
		this(-1);
	}

	   
                                                                
   
                             
    
	public PacketBuilder(int opcode) {
		this.opcode = opcode;
	}

	   
                  
   
                               
                                                     
    
	public PacketBuilder writeByte(int i) {
		payload.writeByte(i);
		return this;
	}

	   
                             
   
                            
                                                     
    
	public PacketBuilder write(byte[] b) {
		payload.writeBytes(b);
		return this;
	}

	   
                   
   
                       
                                                     
    
	public PacketBuilder writeShort(int s) {
		payload.writeShort((short) s);
		return this;
	}


	   
                                                              
   
                           
                                                     
    
	public PacketBuilder writeUnsignedShortInt(int value) {
		value &= Integer.MAX_VALUE;
		if (value <= Short.MAX_VALUE)
			writeShort(value);
		else
			writeInt(Integer.MIN_VALUE + value);
		return this;
	}

	   
                                                             
   
                           
                                                     
    
	public PacketBuilder writeUnsignedByteInt(int value) {
		value &= Integer.MAX_VALUE;
		if (value < 128)
			writeByte(value);
		else
			writeInt(Integer.MIN_VALUE + value);
		return this;
	}

	   
                      
   
                         
                                                     
    
	public PacketBuilder writeInt(int i) {
		payload.writeInt(i);
		return this;
	}

	   
                  
   
                      
                                                     
    
	public PacketBuilder writeLong(long l) {
		payload.writeLong(l);
		return this;
	}

	   
                                            
   
                              
    
	public Packet toPacket() {
		finalizeLength();
		return new Packet(opcode, payload);
	}

	   
                                                                     
                                                                                                       
    
	private void finalizeLength() {
		payload.capacity(payload.writerIndex());
	}

	   
                         
   
                                      
                                                     
    
	public PacketBuilder writeString(String string) {
		payload.writeBytes(string.getBytes());
		payload.writeByte((byte) 10);
		return this;
	}

	   
                                           
   
                                                               
    
	public boolean isEmpty() {
		return payload.writerIndex() == 0;
	}

	   
                      
   
                                                     
    
	public PacketBuilder startBitAccess() {
		bitPosition = payload.writerIndex() * 8;
		return this;
	}

	   
                        
   
                                                     
    
	public PacketBuilder finishBitAccess() {
		payload.writerIndex((bitPosition + 7) / 8);
		return this;
	}

	   
                     
   
                                               
                             
                                                     
    
	public PacketBuilder writeBits(int value, int numBits) {
		if (!payload.hasArray())
			throw new UnsupportedOperationException(
				"The ChannelBuffer implementation must support array() for bit usage.");

		if (numBits < 1 || numBits > 32) {
			throw new IllegalArgumentException("Invalid number of bits");
		}
		int bytePos = bitPosition >> 3;
		int offset = 8 - (bitPosition & 7);
		bitPosition += numBits;
		int pos = (bitPosition + 7) / 8;
		while (pos + 1 > payload.capacity()) {
			payload.writeByte((byte) 0);
		}
		payload.writerIndex(pos);
		byte b;
		for (; numBits > offset; offset = 8) {
			b = payload.getByte(bytePos);
			payload.setByte(bytePos, (byte) (b & ~BIT_MASK_OUT[offset]));
			payload.setByte(bytePos, (byte) (b | (value >> (numBits - offset)) & BIT_MASK_OUT[offset]));
			bytePos++;
			numBits -= offset;
		}
		b = payload.getByte(bytePos);
		if (numBits == offset) {
			payload.setByte(bytePos, (byte) (b & ~BIT_MASK_OUT[offset]));
			payload.setByte(bytePos, (byte) (b | value & BIT_MASK_OUT[offset]));
		} else {
			payload.setByte(bytePos, (byte) (b & ~(BIT_MASK_OUT[numBits] << (offset - numBits))));
			payload.setByte(bytePos, (byte) (b | (value & BIT_MASK_OUT[numBits]) << (offset - numBits)));
		}
		return this;
	}

	   
                                      
   
                          
                                                     
    
	public PacketBuilder write(final ByteBuf buf) {
		payload.writeBytes(buf);
		return this;
	}

	   
                                             
   
                            
                             
                             
                                                     
    
	public PacketBuilder write(byte[] data, int offset, int length) {
		payload.writeBytes(data, offset, length);
		return this;
	}

	public int getOpcode() {
		return opcode;
	}

	public PacketBuilder setID(int i) {
		opcode = i;
		return this;
	}

	public PacketBuilder writeBytes(byte[] message) {
		payload.writeBytes(message);
		return this;
	}

	public PacketBuilder writeBytes(byte[] arg0, int arg1, int arg2) {
		payload.writeBytes(arg0, arg1, arg2);
		return this;
	}

	public void writeString(byte[] message) {
		payload.writeBytes(message);
		payload.writeByte(10);
	}

	public void writeSmart08_16(int value) {
		if (value >= 0 && value < 128) {
			this.writeByte(value);
		} else if (value >= 0 && value < 32768) {
			this.writeShort(value + 32768);
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void writeRSCString(String string) {
		CipheredMessage message = new CipheredMessage();
		DataConversions.encryption.encipher(string, message);

		writeSmart08_16(message.decipheredLength);
		payload.writeBytes(message.messageBuffer, 0, message.encipheredLength);
	}

	public void writeZeroQuotedString(String string) {
		payload.writeByte(0);
		payload.writeBytes(string.getBytes());
		payload.writeByte(0);
	}

	public void writeNonTerminatedString(String string) {
		payload.writeBytes(string.getBytes());
	}

	   
                                                                                                    
   
                               
                                                                                                       
                                                     
    
	public PacketBuilder writeAppearanceByte(int i, int clientVersion) {
		if (i <= AppearanceId.maximumAnimationSprite(clientVersion)) {
			payload.writeByte(i);
		} else {
			payload.writeByte(AppearanceId.NOTHING.id());
		}
		return this;
	}
}
