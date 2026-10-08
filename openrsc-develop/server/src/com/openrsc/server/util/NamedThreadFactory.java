package com.openrsc.server.util;

import com.openrsc.server.ServerConfiguration;

import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory extends ServerAwareThreadFactory {

	   
                    
    
	private final String name;

	   
                                     
   
                                
    
	public NamedThreadFactory(String name, ServerConfiguration configuration) {
		super(name + "-%d", configuration);
		this.name = name;
	}

}
