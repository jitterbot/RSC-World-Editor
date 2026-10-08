package com.openrsc.server.external;

   
                                  
   
public class NPCDef extends EntityDef {
	   
                                 
    
	public Boolean aggressive;
	   
                  
    
	public int attack;
	   
                                 
    
	public Boolean attackable;
	public Boolean members;
	   
                      
    
	public int bottomColour;
	   
                                   
    
	public int camera1, camera2;
	   
                       
    
	public String command1, command2;
	   
               
    
	public int defense;
	public int ranged;
	   
                  
    
	public ItemDropDef[] drops;
	   
                      
    
	public int hairColour;
	   
                  
    
	public int hits;
	   
                                     
    
	public int respawnTime;
	   
               
    
	public int skinColour;
	   
                                    
    
	public int[] sprites = new int[12];
	   
                    
    
	public int strength;

	   
                                 
                                                   
                                                                                          
    
	public int combatLevel;
	   
                     
    
	public int topColour;
	   
                               
    
	public int walkModel, combatModel, combatSprite;

	   
                                                                     
             
                    
           
                                                  
    
	public int roundMode;

	private int id;

	public NPCDef(NPCDef.NPCDefinitionBuilder builder) {
		this.id = builder.id;
		super.name = builder.name;
		super.description = builder.description;
		this.command1 = builder.command1;
		this.attack = builder.attack;
		this.strength = builder.strength;
		this.hits = builder.hits;
		this.defense = builder.defense;
		this.ranged = builder.ranged;
		this.combatLevel = builder.combatLevel;
		this.members = builder.members;
		this.attackable = builder.attackable;
		this.aggressive = builder.aggressive;
	}

	public NPCDef() { }

	public int getAtt() {
		return attack;
	}

	public int getBottomColour() {
		return bottomColour;
	}

	public int getCamera1() {
		return camera1;
	}

	public int getCamera2() {
		return camera2;
	}

	public int getCombatModel() {
		return combatModel;
	}

	public int getCombatSprite() {
		return combatSprite;
	}

	public String getCommand1() {
		return command1;
	}
	public void setCommand1(String command) {
		command1 = command;
	}

	public String getCommand2() {
		return command2;
	}
	public void setCommand2(String command) {
		command2 = command;
	}

	public int getDef() {
		return defense;
	}
	public int getRanged() {
		return ranged;
	}

	public ItemDropDef[] getDrops() {
		return drops;
	}

	public int getHairColour() {
		return hairColour;
	}

	public int getHits() {
		return hits;
	}

	public int getSkinColour() {
		return skinColour;
	}

	public int getSprite(int index) {
		return sprites[index];
	}

	public int[] getStats() {
		return new int[]{attack, defense, strength};
	}

	public int getStr() {
		return strength;
	}

	public int getTopColour() {
		return topColour;
	}

	public int getWalkModel() {
		return walkModel;
	}

	public boolean isAggressive() {
		return attackable && aggressive;
	}

	public boolean isAttackable() {
		return attackable;
	}

	public int respawnTime() {
		return respawnTime;
	}

	public boolean isMembers() {
		return members;
	}

	public int roundMode() { return roundMode; }

	public static class NPCDefinitionBuilder
	{
		private String command1;
		private String description;
		private String name;
		private int attack;
		private int strength;
		private int hits;
		private int defense;
		private int ranged;
		private int combatLevel;
		private Boolean members;
		private Boolean attackable;
		private Boolean aggressive;
		private int id;

		public NPCDefinitionBuilder(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public NPCDef.NPCDefinitionBuilder description(String description) {
			this.description = description;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder command(String command) {
			this.command1 = command;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder attack(int attack) {
			this.attack = attack;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder strength(int strength) {
			this.strength = strength;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder hits(int hits) {
			this.hits = hits;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder defense(int defense) {
			this.defense = defense;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder ranged(int ranged) {
			this.ranged = ranged;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder combatLevel(int combatLevel) {
			this.combatLevel = combatLevel;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder members(Boolean members) {
			this.members = members;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder attackable(Boolean attackable) {
			this.attackable = attackable;
			return this;
		}

		public NPCDef.NPCDefinitionBuilder aggressive(Boolean aggressive) {
			this.aggressive = aggressive;
			return this;
		}

		public NPCDef build() {
			NPCDef definition =  new NPCDef(this);
			return definition;
		}
	}
}
