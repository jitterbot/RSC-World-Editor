package com.openrsc.server.plugins.authentic.minigames.gnomeball;

import com.openrsc.server.model.Point;
import com.openrsc.server.model.entity.player.Player;

public class GnomeField {
	
	private final Point GNOME_GOAL = new Point(729,450);
	private final Point LOW_FIELD = new Point(742,450);
	private static GnomeField instance;
	
	private GnomeField() {
		
	}
	
	public static GnomeField getInstance() {
		if (instance == null) {
			instance = new GnomeField();
		}
		return instance;
	}
	
	                     
	                                                                                     
	public Zone resolvePositionToZone(Player player) {
		int distanceXToGoal = player.getX() - GNOME_GOAL.getX();
		int distanceYToGoal = player.getY() - GNOME_GOAL.getY();
		double skewedYdistToGoal = distanceYToGoal + 0.5;
		                                                              
		                                                                
		int distanceXToLowField = player.getX() - LOW_FIELD.getX();
		                     
		if (Math.abs(distanceYToGoal) <= 2 && distanceXToGoal >= 0 && distanceXToGoal <= 2) {
			return Zone.ZONE_2XP_INNER;
		} 
		                     
		else if (Math.abs(skewedYdistToGoal) < 4 && distanceXToGoal >= 5 && distanceXToGoal <= 8) {
			return Zone.ZONE_2XP_OUTER;
		}
		                                                   
		                              
		         
		else if (Math.abs(skewedYdistToGoal) < 6 && distanceXToGoal >= 1 && distanceXToGoal <= 4) {
			return Zone.ZONE_1XP_INNER;
		} 
		         
		else if ( Math.abs(skewedYdistToGoal) < 6 && distanceXToGoal >= 9 && distanceXToGoal <= 12) {
			return Zone.ZONE_1XP_OUTER;
		}
		                          
		                             
		else if ((player.getX() == LOW_FIELD.getX() && distanceYToGoal >= -5 && distanceYToGoal <= -1)
				|| (player.getX() > LOW_FIELD.getX() && discreteDistance(new Point(LOW_FIELD.getX()+1, LOW_FIELD.getY()), new Point(player.getX(), player.getY()), true, true) <= 3)) {
			return Zone.ZONE_PASS;
		}
		                                                                
		else if ( (Math.abs(skewedYdistToGoal) < 6 && distanceXToLowField >= 1 && distanceXToLowField <= 5)
				|| (distanceXToLowField == 6 && Math.abs(skewedYdistToGoal) < 5)
				) {
			return Zone.ZONE_NO_PASS;
		}
		                    
		else if ( (player.getX() == LOW_FIELD.getX() && distanceYToGoal >= 0 && distanceYToGoal <= 4) ||
				  (distanceXToLowField >= -2 && distanceXToLowField <= -1 && (int)Math.abs(skewedYdistToGoal) == 6) ||
				  (Math.abs(skewedYdistToGoal) < 5 && distanceXToGoal >= -1 && distanceXToLowField <= 0) ||
				  (distanceXToLowField == 2 && Math.abs(skewedYdistToGoal) < 4)
				) {
			return Zone.ZONE_NOT_VISIBLE;
		} 
		                       
		else if ((player.getX() >= 720 && player.getX() <= 743) && (player.getY() >= 440 && player.getY() <= 463)) {
			return Zone.ZONE_OUTSIDE_THROWABLE;
		}
		                                            
		return Zone.ZONE_OUTSIDE_KEEP;
	}
	
	private int discreteDistance(Point base, Point point, boolean skewX, boolean skewY) {
		double offsetX = skewX ? 0.5 : 0.0;
		double offsetY = skewY ? 0.5 : 0.0;
		int distanceX = (int) Math.abs(point.getX() - base.getX() + offsetX);                
		int distanceY = (int) Math.abs(point.getY() - base.getY() + offsetY);             
		return distanceX + distanceY;
	}
	
	enum Zone {
		ZONE_1XP_INNER,
		ZONE_1XP_OUTER,
		ZONE_2XP_INNER,
		ZONE_2XP_OUTER,
		ZONE_PASS,
		ZONE_NO_PASS,
		ZONE_NOT_VISIBLE,
		ZONE_OUTSIDE_THROWABLE,
		ZONE_OUTSIDE_KEEP
	}
	
}
