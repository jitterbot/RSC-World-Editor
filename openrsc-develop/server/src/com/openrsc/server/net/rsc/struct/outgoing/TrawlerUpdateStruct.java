package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;

public class TrawlerUpdateStruct extends AbstractStruct<OpcodeOut> {

	public int interfaceId = 6;                                       
	public int actionId;                                         
	public int waterLevel;
	public int fishCaught;
	public int minutesLeft;
	public int isNetBroken;
}
