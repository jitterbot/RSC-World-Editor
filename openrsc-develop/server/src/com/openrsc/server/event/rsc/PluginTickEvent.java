package com.openrsc.server.event.rsc;

import com.openrsc.server.model.action.WalkToAction;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PluginTickEvent extends GameTickEvent {
	   
                            
    
	private static final Logger LOGGER = LogManager.getLogger();

	private final PluginTask pluginTask;
	private final WalkToAction walkToAction;                                                                                                     
	private final String pluginName;

	public PluginTickEvent(final World world, final Mob owner, final String pluginName, final WalkToAction walkToAction, final PluginTask pluginTask) {
		super(world, owner, 0, pluginName, DuplicationStrategy.ONE_PER_MOB);
		this.walkToAction = walkToAction;
		this.pluginTask = pluginTask;
		this.getPluginTask().setPluginTickEvent(this);
		this.pluginName = pluginName;
	}

	public void run() {
		                                                                                                                                            
		if (walkToAction != null && walkToAction != getPlayerOwner().getLastExecutedWalkToAction()) {
			if (!getPluginTask().isInitialized()) {
				stop();
				return;
			}
		}

		                                                         
		synchronized(getPluginTask()) {
			                                                                                                                
			getPluginTask().doRun();
		}

		                                                                                                                                                                          
		while((!getPluginTask().isInitialized() || getPluginTask().isThreadRunning()) && !getPluginTask().isComplete()) {
			try {
				                                                                                                                                                                                                                                                         
				Thread.sleep(1);
			} catch (final InterruptedException ex) {
				LOGGER.error("Interrupted while waiting for plugin task to complete in run()", ex);
			}
		}

		                                                                                                           

		                                                      
		if (getPluginTask().isComplete()) {
			stop();
			return;
		}
	}

	public void stop() {
		super.stop();
		getPluginTask().stop();
	}

	public final PluginTask getPluginTask() {
		return pluginTask;
	}

	public String getPluginName() {
		return pluginName;
	}
}
