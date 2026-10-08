package com.openrsc.server.model.entity.update;

import com.openrsc.server.model.entity.Mob;

public class Projectile {
	   
                            
    
	private Mob caster;
	   
                                   
    
	private int type;
	   
                                        
    
	private Mob victim;

	public Projectile(Mob caster, Mob victim, int type) {
		this.caster = caster;
		this.victim = victim;
		this.type = type;
	}

	public Mob getCaster() {
		return caster;
	}

	public int getType() {
		return type;
	}

	public Mob getVictim() {
		return victim;
	}

}
