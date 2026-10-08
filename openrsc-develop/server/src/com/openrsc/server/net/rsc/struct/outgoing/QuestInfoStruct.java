package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;

public class QuestInfoStruct extends AbstractStruct<OpcodeOut> {

	public int[] questCompleted;                                     
	               
	public int isUpdate;                                        
	public int numberOfQuests;
	public int[] questId;
	public int[] questStage;
	public String[] questName;
}
