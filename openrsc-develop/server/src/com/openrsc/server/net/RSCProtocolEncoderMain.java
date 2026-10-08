package com.openrsc.server.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public final class RSCProtocolEncoderMain {
	public static final AttributeKey<ConnectionAttachment> attachment = AttributeKey.valueOf("conn-attachment");

	private boolean isInauthenticPacket(int opcode) {
		switch (opcode) {
			case 19:                                         
				return true;
			default:
				return false;
		}
	}
	public ByteBuf encode(ChannelHandlerContext ctx, Packet message) throws Exception {
		final Channel channel = ctx.channel();
		ConnectionAttachment att = channel.attr(attachment).get();
		ByteBuf outBuffer = null;

		if (att.player != null && att.player.get() != null) {
			if (att.player.get().getWorld().getServer().getConfig().WANT_PCAP_LOGGING) {
				att.pcapLogger.get().addPacket(message, true);                                       
			}
		}

		if (!message.isRaw()) {
			Short authenticClient = null;
			if (att.authenticClient.get() != null) {
				authenticClient = att.authenticClient.get();
			}

			if (authenticClient == null || isInauthenticPacket(message.getID()) || authenticClient == -1) {
				                                                                                        
				int packetLength = message.getBuffer().readableBytes();
				ByteBuf buffer = Unpooled.buffer(packetLength + 3);

				buffer.writeShort(buffer.capacity());
				buffer.writeByte(message.getID());

				buffer.writeBytes(message.getBuffer());
				outBuffer = buffer;
			} else if (authenticClient >= 183) {
				                                                
				                                                                                                              
				int packetLength = message.getBuffer().readableBytes() + 1;                  

				             
                                                                                   
                                                                                        
     
      

				ByteBuf buffer;
				int encodedOpcode;
				if (packetLength >= 160) {
					buffer = Unpooled.buffer(packetLength + 2);                      
					buffer.writeByte((byte) (packetLength / 256 + 160));
					buffer.writeByte((byte) (packetLength & 0xFF));

					encodedOpcode = att.ISAAC.get().encodeOpcode(message.getID());
					buffer.writeByte(encodedOpcode);

					buffer.writeBytes(message.getBuffer());

				} else {
					buffer = Unpooled.buffer(packetLength + 1);                      
					buffer.writeByte((byte) packetLength);
					int bufferLen = message.getBuffer().readableBytes();

					if (packetLength != 1) {
						                                                                                 
						try {
							buffer.writeByte(message.getBuffer().slice(bufferLen - 1, 1).readByte());
						} catch (IndexOutOfBoundsException e) {
							                                                                                                                
							System.out.println(String.format("Warning: index out of bounds on sending last byte of opcode %d", message.getID()));
							System.out.println(e.toString());
							if (message.getBuffer().hasArray()) {
								byte[] bArr = message.getBuffer().array();
								buffer.writeByte(bArr[bArr.length - 1]);
							}
						}

						encodedOpcode = att.ISAAC.get().encodeOpcode(message.getID());
						buffer.writeByte(encodedOpcode);

						buffer.writeBytes(message.getBuffer().slice(0, bufferLen - 1));
					} else {
						                        
						encodedOpcode = att.ISAAC.get().encodeOpcode(message.getID());
						buffer.writeByte(encodedOpcode);
					}
				}

				outBuffer = buffer;
			} else if (authenticClient >= 93) {
				int packetLength = message.getBuffer().readableBytes() + 1;                  

				ByteBuf buffer;
				if (packetLength >= 160) {
					buffer = Unpooled.buffer(packetLength + 2);                      
					buffer.writeByte((byte) (packetLength / 256 + 160));
					buffer.writeByte((byte) (packetLength & 0xFF));

					buffer.writeByte(message.getID());
					buffer.writeBytes(message.getBuffer());

				} else {
					buffer = Unpooled.buffer(packetLength + 1);                      
					buffer.writeByte((byte) packetLength);
					int bufferLen = message.getBuffer().readableBytes();

					if (packetLength != 1) {
						                                                                                 
						try {
							buffer.writeByte(message.getBuffer().slice(bufferLen - 1, 1).readByte());
						} catch (IndexOutOfBoundsException e) {
							                                                                                                                
							System.out.println(String.format("Warning: index out of bounds on sending last byte of opcode %d", message.getID()));
							System.out.println(e.toString());
							if (message.getBuffer().hasArray()) {
								byte[] bArr = message.getBuffer().array();
								buffer.writeByte(bArr[bArr.length - 1]);
							}
						}

						buffer.writeByte(message.getID());
						buffer.writeBytes(message.getBuffer().slice(0, bufferLen - 1));
					} else {
						                        
						buffer.writeByte(message.getID());
					}
				}

				outBuffer = buffer;
			} else if (authenticClient >= 14) {
				                                        
				int packetLength = message.getBuffer().readableBytes();
				ByteBuf buffer = Unpooled.buffer(packetLength + 3);

				buffer.writeShort(packetLength + 1);
				buffer.writeByte(message.getID());

				buffer.writeBytes(message.getBuffer());
				outBuffer = buffer;
			}
		} else {
			outBuffer = message.getBuffer();
		}

		return outBuffer;
	}
}
