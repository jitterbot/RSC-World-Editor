package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;

public class IronManStruct extends AbstractStruct<OpcodeOut> {

	public int interfaceId = 2;                               
	public int actionId;                                         
	public int ironmanType;
	public int ironmanRestriction;
}
