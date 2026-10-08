package com.openrsc.server.plugins.menu;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.rsc.ActionSender;

import java.util.ArrayList;

import static com.openrsc.server.plugins.Functions.delay;
import static com.openrsc.server.plugins.Functions.say;

   
                                                                                
                                                                               
   
public class Menu {

	private ArrayList<Option> options = new ArrayList<Option>();

	   
                                                                           
                                                                           
                   
       
       
   
                 
           
    
	public Menu addOption(final Option option) {
		options.add(option);
		return this;
	}

	   
                                                             
                                                                 
       
                                                                          
       
         
   
               
           
    
	public Menu addOptions(final Option... opts) {
		for (Option i : opts) {
			options.add(i);
		}
		return this;
	}

	   
                                               
   
                 
    
	public void showMenu(final Player player) {
		String[] option = new String[options.size()];
		int i = 0;
		for (Option opt : options) {
			option[i] = opt.getOption();
			i++;
		}
		player.setMenu(this);
		ActionSender.sendMenu(player, option);
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start <= 19500 && player.getMenu() != null && player.getOption() == -1) {
			delay();
		}

		doReply(player);
	}

	public int size() {
		return options.size();
	}

	private void doReply(final Player player) {
		final int i = player.getOption();
		if(i >= 0 && i <= options.size()) {
			Option option = options.get(i);
			if (option != null) {
				say(player, null, option.getOption());
				option.action();
			}
		}
	}

	public final void handleReply(final Player player, final int i) {
		player.setOption(i);
		player.resetMenuHandler();
	}
}
