package com.openrsc.server.content.party;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;


public class PartyManager {

	public ArrayList<Party> getParties() {
		return parties;
	}

	private static class PartyRankComparator implements Comparator<Party> {
		public int compare(final Party o1, final Party o2) {
			if (o1.getPartyPoints() == o2.getPartyPoints()) {
				return o1.getPartyName().compareTo(o2.getPartyName());
			}
			return o1.getPartyPoints() > o2.getPartyPoints() ? -1 : 1;
		}
	}

	public final static PartyRankComparator PARTY_COMPERATOR = new PartyRankComparator();
	   
                            
    
	private static final Logger LOGGER = LogManager.getLogger();

	private final ArrayList<Party> parties = new ArrayList<>();

	private final World world;

	public PartyManager (final World world) {
		this.world = world;
	}

	public void createParty(final Party party) {
		getParties().add(party);
		                             
	}

	public void deleteParty(final Party party) {
		                             
		getParties().remove(party);
	}

	public void initialize() {
		                                   
		                
		                                                     
	}

	public void uninitialize() {
		getParties().clear();
	}

	public Party getParty(final String exist) {
		for (final Party t : getParties()) {
			if (t.getPartyName().equalsIgnoreCase(exist))
				return t;
			else if (t.getPartyTag().equalsIgnoreCase(exist))
				return t;
		}
		return null;
	}

	public void checkAndAttachToParty(final Player player) {
		for (final Party p : getParties()) {
			final PartyPlayer partyMember = p.getPlayer(player.getUsername());
			if (partyMember != null) {
				partyMember.setPlayerReference(player);
				player.setParty(p);
				p.updatePartyGUI();
				p.updatePartySettings();
				break;
			}
		}
	}

                                                      
       
                                                                                                             
                                                                                                                                             
                                                    
                                            
                                  
                              
                            
                                                                               
                      
   

    

	public void checkAndUnattachFromParty(final Player player) {
		for (final Party p : getParties()) {
			final PartyPlayer cp = p.getPlayer(player.getUsername());
			if (cp != null) {
				cp.setPlayerReference(null);
				p.updatePartyGUI();
				break;
			}
		}
	}

	public void saveParties() {
		for (final Party t : getParties()) {
			savePartyChanges(t);
		}
	}

	public void savePartyChanges(final Party party) {
		                     

		                           
		                         

		                 
	}

	public World getWorld() {
		return world;
	}

	                                                  
                                                                                                                                                                                                                                                                
                                              
                         
                             
                                         
                                                
                                              
                                                       
                                                           
                                                                
                                                       

                                                                              
                                                                                                                                                                
                                              
                                                         

                                                                      

                                 
                                                                              
                                              
                                                 
                                                   
                                                     
                             
                                                           
                             
     
    
                         

                                  

                     
   
    

	                                                                     
                                                                                                            
                                                                                                                                                     
                                               
                                              
                                                          
                            

                                              
            
                                 
             

                    

                                                        
                                                                                                                                                     
                                                 
                                           
                                                
                                                        
                        
   
                           
    

	                                                                     
                                                                                                                                                                                    
                                                                                   
                                                                                                                         

                                            
                              
                                                   
                                     
    

	                                             
       
                                                                                             
                                                                                                                                                           
                                                  
                                            
                                                 
                                                         
                                           
                                            
                         
    
                            
                            
                                                                                   
                      
   
    

	                                               
       
                                                                           
                                                                                                                          
                                           
                             
                            
                                                                               
                      
   
    

	                                         
       
                                                                           
                                                                                                                                                                                                                        
                                                
                                               
                                                           
                                               
                                                 
                                                   
                                               
                                           
                                               
                                                
                             
                            
                                                                   
                      
   
    
}
