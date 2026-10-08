package com.openrsc.server.external;

public final class ItemDefinition extends EntityDef {
	   
                                                  
                                               
                                     
    
	private String[] command;

	   
                                                      
                                               
                                     
    
	private String description;

	   
                                               
                                               
                                     
    
	private String name;

	   
                                          
                                    
                                          
                                     
    
	private boolean isFemaleOnly;

	   
                                           
                                    
                                          
                                     
    
	private boolean isMembersOnly;

	   
                                           
                                    
                                          
                                     
    
	private boolean isStackable;

	   
                                            
                                    
                                          
                                     
    
	private boolean isUntradable;

	   
                                          
                                    
                                          
                                     
    
	private boolean isWearable;

	   
                                                        
                                               
                                     
    
	private int appearanceId;

	   
                                                       
                                               
                                     
    
	private long armourBonus;

	   
                                                        
                                               
                                     
    
	private int defaultPrice;

	   
                                             
                                               
                                     
    
	private int id;

	   
                                                      
                                               
                                     
    
	private int magicBonus;

	   
                                                       
                                               
                                     
    
	private int prayerBonus;

	   
                                                         
                                               
                                     
    
	private int requiredLevel;

	   
                                                               
                                               
                                     
    
	private int requiredSkillIndex;

	   
                                                           
                                               
                                     
    
	private int weaponAimBonus;

	   
                                                             
                                               
                                     
    
	private int weaponPowerBonus;

	   
                                                      
                                               
                                     
    
	private int wearableId;

	   
                                                          
                                               
                                     
    
	private int wornItemIndex;

	   
                                          
                                    
                                          
                                     
    
	private boolean isNoteable;


	   
                                                                       
   
                     
                    
                      
                     
                           
                         
                      
                          
                        
                   
                     
                       
                     
                       
                      
                        
                       
                      
               
                  
    
	public ItemDefinition(int id, String name, String description, String[] command, boolean isFemaleOnly, boolean isMembersOnly,
						  boolean isStackable, boolean isUntradable, boolean isWearable, int appearanceID, int wearableID,
						  int wearSlot, int requiredLevel, int requiredSkillID, long armourBonus, int weaponAimBonus,
						  int weaponPowerBonus, int magicBonus, int prayerBonus, int basePrice, boolean isNoteable) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.command = command;
		this.isFemaleOnly = isFemaleOnly;
		this.isMembersOnly = isMembersOnly;
		this.isStackable = isStackable;
		this.isUntradable = isUntradable;
		this.isWearable = isWearable;
		this.appearanceId = appearanceID;
		this.wearableId = wearableID;
		this.wornItemIndex = wearSlot;
		this.requiredLevel = requiredLevel;
		this.requiredSkillIndex = requiredSkillID;
		this.armourBonus = armourBonus;
		this.weaponAimBonus = weaponAimBonus;
		this.weaponPowerBonus = weaponPowerBonus;
		this.magicBonus = magicBonus;
		this.prayerBonus = prayerBonus;
		this.defaultPrice = basePrice;
		this.isNoteable = isNoteable;
	}

	public ItemDefinition(ItemDefinitionBuilder builder) {
		this(builder.id, builder.name, builder.description, builder.command, builder.isFemaleOnly, builder.isMembersOnly,
			builder.isStackable, builder.isUntradable, builder.isWearable, builder.appearanceId, builder.wearableId,
			builder.wornItemIndex, builder.requiredLevel, builder.requiredSkillIndex, builder.armourBonus, builder.weaponAimBonus,
			builder.weaponPowerBonus, builder.magicBonus, builder.prayerBonus, builder.defaultPrice, builder.isNoteable);
	}

	public ItemDefinition() { }


	   
                                        
                                
   
                                
    
	public final String[] getCommand() {
		return command;
	}

	   
                                            
                                
   
                                    
    
	public final String getDescription() {
		return description;
	}

	   
                                     
                                
   
                             
    
	public final String getName() {
		return name;
	}

	   
                                   
                            
                                
   
                                   
                    
    
	public final boolean isFemaleOnly() {
		return isFemaleOnly;
	}

	   
                                    
                            
                                
   
                                    
                    
    
	public final boolean isMembersOnly() {
		return isMembersOnly;
	}

	   
                                    
                            
                                
   
                                    
                    
    
	public final boolean isStackable() {
		return isStackable;
	}

	   
                                     
                            
                                
   
                                     
                    
    
	public final boolean isUntradable() {
		return isUntradable;
	}

	   
                                   
                            
                                
   
                                   
                    
    
	public final boolean isWieldable() {
		return isWearable;
	}
	public void setWieldable(boolean wieldable) {
		this.isWearable = wieldable;
	}

	   
                                     
                                         
   
                                      
    
	public final int getAppearanceId() {
		return appearanceId;
	}
	public void setAppearanceId(int appearanceId) {
		this.appearanceId = appearanceId;
	}

	   
                                    
                                         
   
                                     
    
	public final long getArmourBonus() {
		return armourBonus;
	}

	   
                                     
                                         
   
                                      
    
	public final int getDefaultPrice() {
		return defaultPrice;
	}

	   
                          
                                         
   
                           
    
	public final int getId() {
		return id;
	}

	   
                                   
                                         
   
                                    
    
	public final int getMagicBonus() {
		return magicBonus;
	}

	   
                                    
                                         
   
                                     
    
	public final int getPrayerBonus() {
		return prayerBonus;
	}

	   
                                      
                                         
   
                                       
    
	public final int getRequiredLevel() {
		return requiredLevel;
	}
	public void setRequiredLevel(int requiredLevel) {
		this.requiredLevel = requiredLevel;
	}

	   
                                            
                                         
   
                                             
    
	public final int getRequiredSkillIndex() {
		return requiredSkillIndex;
	}
	public void setRequiredSkillIndex(int index) {
		this.requiredSkillIndex = index;
	}

	   
                                        
                                         
   
                                         
    
	public final int getWeaponAimBonus() {
		return weaponAimBonus;
	}
	public void setWeaponAimBonus(int bonus) {
		this.weaponAimBonus = bonus;
	}

	   
                                          
                                         
   
                                           
    
	public final int getWeaponPowerBonus() {
		return weaponPowerBonus;
	}
	public void setWeaponPowerBonus(int bonus) {
		this.weaponPowerBonus = bonus;
	}

	   
                                 
                                         
   
                                    
    
	public final long getMeleeBonus() {
		return armourBonus + weaponAimBonus + weaponAimBonus;
	}

	   
                                   
                                         
   
                                    
    
	public final int getWearableId() {
		return wearableId;
	}
	public void setWearableId(int wearableId) {
		this.wearableId = wearableId;
	}

	   
                                       
                                         
   
                                        
    
	public final int getWieldPosition() {
		return wornItemIndex;
	}
	public void setWieldPosition(int wieldPosition) {
		this.wornItemIndex = wieldPosition;
	}

	   
                                    
                           
   
                                   
           
    
	public final boolean isNoteable() { return !isStackable && (!isUntradable || isNoteable); }


	@Deprecated
	public int getOriginalItemID() {
		return id;
	}

	@Deprecated
	public int getNoteID() {
		return id;
	}

	public void nullCommand() { this.command = null; }

	public static class ItemDefinitionBuilder
	{
		private String[] command;
		private String description;
		private String name;
		private boolean isFemaleOnly;
		private boolean isMembersOnly;
		private boolean isStackable;
		private boolean isUntradable;
		private boolean isWearable;
		private int appearanceId;
		private long armourBonus;
		private int defaultPrice;
		private int id;
		private int magicBonus;
		private int prayerBonus;
		private int requiredLevel;
		private int requiredSkillIndex;
		private int weaponAimBonus;
		private int weaponPowerBonus;
		private int wearableId;
		private int wornItemIndex;
		private boolean isNoteable;

		public ItemDefinitionBuilder(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public ItemDefinitionBuilder description(String description) {
			this.description = description;
			return this;
		}

		public ItemDefinitionBuilder command(String[] command) {
			this.command = command;
			return this;
		}

		public ItemDefinitionBuilder isStackable(boolean isStackable) {
			this.isStackable = isStackable;
			return this;
		}

		public ItemDefinitionBuilder defaultPrice(int defaultPrice) {
			this.defaultPrice = defaultPrice;
			return this;
		}

		public ItemDefinitionBuilder armourBonus(long armourBonus) {
			this.armourBonus = armourBonus;
			return this;
		}

		public ItemDefinitionBuilder weaponAimBonus(int weaponAimBonus) {
			this.weaponAimBonus = weaponAimBonus;
			return this;
		}

		public ItemDefinitionBuilder weaponPowerBonus(int weaponPowerBonus) {
			this.weaponPowerBonus = weaponPowerBonus;
			return this;
		}

		public ItemDefinitionBuilder magicBonus(int magicBonus) {
			this.magicBonus = magicBonus;
			return this;
		}

		public ItemDefinitionBuilder prayerBonus(int prayerBonus) {
			this.prayerBonus = prayerBonus;
			return this;
		}

		public ItemDefinition build() {
			ItemDefinition definition =  new ItemDefinition(this);
			return definition;
		}
	}
}
