package com.openrsc.client.entityhandling.defs;

import orsc.Config;

public class ItemDef extends EntityDef {
	public String[] command;
	public int basePrice;
	public int spriteID;
	public String spriteLocation;
	public boolean stackable;
	public boolean wieldable;
	public int wearableID;
	private int pictureMask;
	private int blueMask;
	public boolean untradeable;
	public boolean membersItem;
	public boolean noteable;
	public boolean hasNoteType;

	public ItemDef(String name, String description, int spriteID, String spriteLocation,int id) {
		super(name,description,id);
		this.spriteID = spriteID;
		this.spriteLocation = spriteLocation;
	}

	                                                                                                                 
                                                                                                              
                               
                                    
                             
                           
                             
                             
                               
                                 
                    
                                 
                                 
               
                                                        
                       

                                                                                                            
                                       
    

	public ItemDef(String name, String description, String command, int basePrice, int spriteID, String spriteLocation,
				   boolean stackable, boolean wieldable, int wearableID, int pictureMask, boolean membersItem,
				   boolean untradeable, boolean noteable, int id) {
		this(name, description, command, basePrice, spriteID, spriteLocation,
			stackable, wieldable, wearableID, pictureMask, 0, membersItem,
			untradeable, noteable, id);
	}

	public ItemDef(String name, String description, String command, int basePrice, int spriteID, String spriteLocation,
				   boolean stackable, boolean wieldable, int wearableID, int pictureMask, int blueMask, boolean membersItem,
				   boolean untradeable, boolean noteable, int id) {
		this(name, description, command, basePrice, spriteID, spriteLocation,
			stackable, wieldable, wearableID, pictureMask, blueMask, membersItem,
			untradeable, noteable, -1, -1, id);
	}

	ItemDef(String name, String description, String command, int basePrice, int spriteID, String spriteLocation,
				   boolean stackable, boolean wieldable, int wearableID, int pictureMask, int blueMask, boolean membersItem,
				   boolean untradeable, boolean noteable, int notedForm, int notedFormOf, int id) {
		super(name, description, id);
		this.command = command.split(",");                                                                                   
		this.basePrice = basePrice;                                
		this.spriteID = spriteID;                            
		this.stackable = stackable;              
		this.wieldable = wieldable;                      
		this.wearableID = wearableID;                                                                         
		this.pictureMask = pictureMask;                                                      
		this.blueMask = blueMask;                               
		this.membersItem = membersItem;                                          
		this.untradeable = untradeable;                   
		this.noteable = noteable;                                                    
		this.id = id;                              
		this.spriteLocation = spriteLocation;                                                                            

		if (this.command.length == 1 && this.command[0] == "")
			this.command = null;

		this.hasNoteType = calcHasNoteType();
	}

	public static ItemDef asNote(ItemDef item) {
		if (item.hasNoteType) {
			if (Config.S_WANT_CERT_AS_NOTES) {
				return new ItemDef(item.name, "Swap this note at any bank for the equivalent item.", "", item.basePrice, item.spriteID, item.spriteLocation,                                                              
					true, false, 0, item.getPictureMask(), item.getBlueMask(), item.membersItem,
					item.untradeable, false, -1, item.id, item.id);
			} else {
				return new ItemDef(item.name + " Certificate", "Each certificate exchangable at any bank for the equivalent item", "", item.basePrice, item.spriteID, item.spriteLocation,                                                              
					true, false, 0, item.getPictureMask(), item.getBlueMask(), item.membersItem,
					item.untradeable, false, -1, item.id, item.id);
			}
		} else {
			return item;
		}
		                         
                                                                            
                         
                          
                         
   
                
	}

	                                       
                                                                             
                              
                                  
                               
                        
                         
                       
                    
                                      
                                      
                               
              
                                                        
                       
    

	public String[] getCommand() {
		return command;
	}

	public int getSpriteID() {
		return spriteID;
	}

	public String getSpriteLocation() { return this.spriteLocation; }

	public int getBasePrice() {
		return basePrice;
	}

	public boolean isStackable() {
		return stackable;
	}

	public boolean isWieldable() {
		return wieldable;
	}

	public int getPictureMask() { return pictureMask; }

	public int getBlueMask() { return blueMask; }

	public boolean calcHasNoteType() {
		return !stackable && (!untradeable || noteable);
	}

}
