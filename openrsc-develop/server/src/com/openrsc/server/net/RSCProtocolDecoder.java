package com.openrsc.server.net;

import com.openrsc.server.net.rsc.ISAACContainer;
import com.openrsc.server.net.rsc.ReverseOpcodeLookup;
import com.openrsc.server.net.rsc.enums.OpcodeIn;
import com.openrsc.server.net.rsc.parsers.impl.Payload196Parser;
import com.openrsc.server.net.rsc.parsers.impl.Payload198Parser;
import com.openrsc.server.net.rsc.parsers.impl.Payload199Parser;
import com.openrsc.server.net.rsc.parsers.impl.Payload201Parser;
import com.openrsc.server.net.rsc.parsers.impl.Payload202Parser;
import com.openrsc.server.net.rsc.parsers.impl.Payload203Parser;
import com.openrsc.server.net.rsc.parsers.impl.Payload235Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

import java.util.List;

public final class RSCProtocolDecoder extends ByteToMessageDecoder implements AttributeMap {
	public static final AttributeKey<ConnectionAttachment> attachment = AttributeKey.valueOf("conn-attachment");

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
		final Channel channel = ctx.channel();
		ConnectionAttachment att = channel.attr(attachment).get();

		if (att != null && att.authenticClient != null) {
			Short authenticClient = att.authenticClient.get();

			if (authenticClient == null) {
				                                                                 
				                                                                                                                                         
				if (buffer.readableBytes() > 2) {
					buffer.markReaderIndex();
					int length = buffer.readUnsignedByte();
					int lengthLength;
					if (length >= 160) {
						length = 256 * length - (40960 - buffer.readUnsignedByte());
						lengthLength = 2;
					} else {
						lengthLength = 1;
					}

					System.out.println("Buffer readable bytes: " + buffer.readableBytes() + " len: " + length);
					if (buffer.readableBytes() >= length && length > 0) {
						int opcode;

						if (att != null && att.ISAAC != null) {
							ISAACContainer isaacContainer = att.ISAAC.get();
							if (isaacContainer != null) {
								if (lengthLength == 1) {
									ByteBuf bufferOrdered = Unpooled.buffer(length);
									byte lastByte = buffer.readByte();
									buffer.readBytes(bufferOrdered, length - 1);
									bufferOrdered.writeByte(lastByte);

									int encodedOpcode = bufferOrdered.readByte() & 0xFF;

									opcode = (isaacContainer.decodeOpcode(encodedOpcode) & 0xFF);

									Packet packet = new Packet(opcode, bufferOrdered);
									addPacketToIncoming(out, att, packet);
									return;

								} else {
									int encodedOpcode = buffer.readByte() & 0xFF;
									opcode = (isaacContainer.decodeOpcode(encodedOpcode) & 0xFF);
								}
							} else {
								if (lengthLength == 1) {
									ByteBuf bufferOrdered = Unpooled.buffer(length);
									byte lastByte = buffer.readByte();
									buffer.readBytes(bufferOrdered, length - 1);
									bufferOrdered.writeByte(lastByte);

									opcode = bufferOrdered.readByte() & 0xFF;

									Packet packet = new Packet(opcode, bufferOrdered);
									                                          
									addPacketToIncoming(out, att, packet);
									return;
								} else {
									opcode = (buffer.readByte()) & 0xFF;

								}
							}
						} else {
							opcode = (buffer.readByte()) & 0xFF;
						}
						length -= 1;

						ByteBuf data = Unpooled.buffer(length);
						buffer.readBytes(data, length);
						Packet packet = new Packet(opcode, data);
						addPacketToIncoming(out, att, packet);
						                                          

					} else {
						if (buffer.readableBytes() > 0) {
							byte bLength = buffer.readByte();
							if (bLength == 1) {
								att.authenticClient.set((short)-1);
								byte theOnlyByte = buffer.readByte();
								if (theOnlyByte == (byte) 19) {
									Packet packet = new Packet(19, Unpooled.buffer(1));
									addPacketToIncoming(out, att, packet);
								} else {
									buffer.resetReaderIndex();
								}
							} else {
								                                  
								buffer.resetReaderIndex();

								int loginLength = buffer.readUnsignedShort();
								if (buffer.readableBytes() >= loginLength && loginLength > 0) {
									int opcode = (buffer.readByte()) & 0xFF;
									if (buffer.readableBytes() >= 38
										&& (ReverseOpcodeLookup.getOpcode(opcode) == OpcodeIn.LOGIN)) {
										att.authenticClient.set((short)-1);
									}
									else if (buffer.readableBytes() < 80
										&& ReverseOpcodeLookup.getOpcode(opcode) == OpcodeIn.REGISTER_ACCOUNT) {
										att.authenticClient.set((short)-1);
									} else if (opcode > 2 && ReverseOpcodeLookup.getOpcode(opcode) != OpcodeIn.RELOGIN) {
										att.authenticClient.set((short)-1);
									}
									loginLength -= 1;
									ByteBuf data = Unpooled.buffer(loginLength);
									buffer.readBytes(data, loginLength);
									Packet packet = new Packet(opcode, data);
									                                          
									addPacketToIncoming(out, att, packet);
								} else {
									buffer.resetReaderIndex();
								}
							}
						} else {
							buffer.resetReaderIndex();
						}
					}
				}
			} else if (authenticClient >= 183) {
				                        
				if (buffer.readableBytes() >= 2) {
					buffer.markReaderIndex();
					int length = buffer.readUnsignedByte();
					int lengthLength;
					if (length >= 160) {
						length = 256 * length - (40960 - buffer.readUnsignedByte());
						lengthLength = 2;
					} else {
						lengthLength = 1;
					}

					if (buffer.readableBytes() >= length && length > 0) {
						int opcode;

						if (att != null && att.ISAAC != null) {
							ISAACContainer isaacContainer = att.ISAAC.get();
							if (isaacContainer != null) {
								if (lengthLength == 1) {
									ByteBuf bufferOrdered = Unpooled.buffer(length);
									byte lastByte = buffer.readByte();
									buffer.readBytes(bufferOrdered, length - 1);
									bufferOrdered.writeByte(lastByte);

									int encodedOpcode = bufferOrdered.readByte() & 0xFF;

									                                                                                                               
									                                                                                    
									                                                                                                                           
									  
									                                                                                                                
									                                                                                        
									int opcodeTries = 0;
									while (opcodeTries < 256) {                                                                                                           
										opcode = (isaacContainer.decodeOpcode(encodedOpcode) & 0xFF);
										opcodeTries++;

										                                                                                                               
										                                                                     

										boolean isPossiblyValid;
										if (authenticClient > 204) {
											isPossiblyValid = Payload235Parser.isPossiblyValid(opcode, length, 235)
												|| Payload235Parser.isPossiblyValid(opcode, length, 175);
										} else if (authenticClient > 202) {
											isPossiblyValid = Payload203Parser.isPossiblyValid(opcode, length);
										} else if (authenticClient > 201) {
											isPossiblyValid = Payload202Parser.isPossiblyValid(opcode, length);
										} else if (authenticClient > 199) {
											isPossiblyValid = Payload201Parser.isPossiblyValid(opcode, length);
										} else if (authenticClient > 198) {
											isPossiblyValid = Payload199Parser.isPossiblyValid(opcode, length);
										} else if (authenticClient > 196) {
											isPossiblyValid = Payload198Parser.isPossiblyValid(opcode, length);
										} else {
											isPossiblyValid = Payload196Parser.isPossiblyValid(opcode, length);
										}
										if (isPossiblyValid) {
											Packet packet = new Packet(opcode, bufferOrdered);
											addPacketToIncoming(out, att, packet);
											return;
										} else {
											System.out.println(String.format("Caught invalid incoming opcode;; enc: %d; dec: %d; len: %d; isPossiblyValid: %b; opcodeTries: %d", encodedOpcode, opcode, length, isPossiblyValid, opcodeTries));
										}
									}
									                                         
									return;

								} else {
									int encodedOpcode = buffer.readByte() & 0xFF;
									opcode = (isaacContainer.decodeOpcode(encodedOpcode) & 0xFF);
								}
							} else {
								opcode = (buffer.readByte()) & 0xFF;
							}
						} else {
							opcode = (buffer.readByte()) & 0xFF;
						}
						length -= 1;

						ByteBuf data = Unpooled.buffer(length);
						buffer.readBytes(data, length);
						Packet packet = new Packet(opcode, data);
						addPacketToIncoming(out, att, packet);
						                                          


					} else {
						buffer.resetReaderIndex();
					}
				}
			} else if (authenticClient >= 175) {
				                                                                                 
				if (buffer.readableBytes() >= 2) {
					buffer.markReaderIndex();
					int length = buffer.readUnsignedByte();
					int lengthLength;
					if (length >= 160) {
						length = 256 * length - (40960 - buffer.readUnsignedByte());
						lengthLength = 2;
					} else {
						lengthLength = 1;
					}

					if (buffer.readableBytes() >= length && length > 0) {
						ByteBuf data;
						if (lengthLength == 1) {
							data = Unpooled.buffer(length);
							byte lastByte = buffer.readByte();
							buffer.readBytes(data, length - 1);
							data.writeByte(lastByte);
						} else {
							data = Unpooled.buffer(length);
							buffer.readBytes(data, length);
							length -= 1;
						}
						int opcode = (data.readByte()) & 0xFF;

						Packet packet = new Packet(opcode, data);
						addPacketToIncoming(out, att, packet);
						                                          
					} else {
						buffer.resetReaderIndex();
					}
				}

			} else if (authenticClient >= 93) {
				if (buffer.readableBytes() >= 2) {
					buffer.markReaderIndex();
					int length = buffer.readUnsignedByte();
					int lengthLength;
					if (length >= 160) {
						length = 256 * length - (40960 - buffer.readUnsignedByte());
						lengthLength = 2;
					} else {
						lengthLength = 1;
					}

					if (buffer.readableBytes() >= length && length > 0) {
						ByteBuf data;
						if (lengthLength == 1) {
							data = Unpooled.buffer(length);
							byte lastByte = buffer.readByte();
							buffer.readBytes(data, length - 1);
							data.writeByte(lastByte);
						} else {
							data = Unpooled.buffer(length);
							buffer.readBytes(data, length);
							length -= 1;
						}
						int opcode = (data.readByte()) & 0xFF;

						Packet packet = new Packet(opcode, data);
						addPacketToIncoming(out, att, packet);
						                                          
					} else {
						buffer.resetReaderIndex();
					}
				}
			} else if (authenticClient >= 14) {
				                                                                                          
				                                                            
				                    
				if (buffer.readableBytes() >= 2) {
					buffer.markReaderIndex();
					int length = buffer.readUnsignedShort();
					if (buffer.readableBytes() >= length && length > 0) {
						int opcode = (buffer.readByte()) & 0xFF;
						length -= 1;
						ByteBuf data = Unpooled.buffer(length);
						buffer.readBytes(data, length);
						Packet packet = new Packet(opcode, data);
						addPacketToIncoming(out, att, packet);
					} else {
						buffer.resetReaderIndex();
					}
				}
			} else if (authenticClient == -1) {
				                              
				if (buffer.readableBytes() > 2) {
					buffer.markReaderIndex();
					int length = buffer.readUnsignedShort();
					if (buffer.readableBytes() >= length && length > 0) {
						int opcode = (buffer.readByte()) & 0xFF;
						length -= 1;
						ByteBuf data = Unpooled.buffer(length);
						buffer.readBytes(data, length);
						Packet packet = new Packet(opcode, data);
						addPacketToIncoming(out, att, packet);
					} else {
						buffer.resetReaderIndex();
					}
				}
			}
		}
	}

	private void addPacketToIncoming(List<Object> out, ConnectionAttachment att, Packet packet) {
		if (att.player != null && att.player.get() != null) {
			if (att.player.get().getWorld().getServer().getConfig().WANT_PCAP_LOGGING) {
				Packet copy = new Packet(packet.getID(), packet.getBuffer().copy());
				att.pcapLogger.get().addPacket(copy, false);                                      
			}
		}
		out.add(packet);
	}

	@Override
	public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
		return null;
	}

	@Override
	public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
		return false;
	}
}
