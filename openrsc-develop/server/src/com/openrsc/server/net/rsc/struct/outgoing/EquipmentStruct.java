package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;

public class EquipmentStruct extends AbstractStruct<OpcodeOut> {

	public int equipmentCount;
	public int realCount;                         
	public int[] wieldPositions;
	public int[] catalogIDs;
	public int[] amount;                                               
}
