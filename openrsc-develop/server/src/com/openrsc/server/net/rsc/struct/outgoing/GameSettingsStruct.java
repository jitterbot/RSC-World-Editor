package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;

import java.util.List;

public class GameSettingsStruct extends AbstractStruct<OpcodeOut> {

	public int cameraModeAuto;
	public int mouseButtonOne;
	public int soundDisabled;
	public int playerKiller;             
	public int pkChangesLeft;                          
	public List<Integer> customOptions;                        
}
