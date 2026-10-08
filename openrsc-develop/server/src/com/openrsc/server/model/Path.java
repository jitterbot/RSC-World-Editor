package com.openrsc.server.model;

import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class Path {

	private static final int MAXIMUM_SIZE = 50;
	private Deque<Point> waypoints = new LinkedList<Point>();
	private PathType pathType;
	private Mob mob;
	public Path(Mob mob, PathType type) {
		setPathType(type);
		this.mob = mob;
	}

	public static int direction(int dx, int dy) {
		if (dx < 0) {
			if (dy < 0) {
				return 7;
			} else if (dy > 0) {
				return 5;
			} else {
				return 6;
			}
		} else if (dx > 0) {
			if (dy < 0) {
				return 1;
			} else if (dy > 0) {
				return 3;
			} else {
				return 2;
			}
		} else {
			if (dy < 0) {
				return 0;
			} else if (dy > 0) {
				return 4;
			} else {
				return -1;
			}
		}
	}

	public void addStep(int x, int y) {

		if (waypoints.size() == 0) {
			waypoints.add(new Point(mob.getX(), mob.getY()));
		}

		  
                                         
     
		Point last = waypoints.peekLast();
		  
                                                       
     
		int diffX = x - last.getX();
		int diffY = y - last.getY();

		                                                             
		int maxTiles = 1;
		if(mob instanceof Player && mob.getConfig().MAX_WALKING_SPEED >= maxTiles) {
			Player player = (Player)mob;

			                                                                                                                                         
			if(player.canLogout() && waypoints.size() >= mob.getConfig().MAX_TICKS_UNTIL_FULL_WALKING_SPEED) {
				maxTiles = mob.getConfig().MAX_WALKING_SPEED;
			}
		}

		  
                                                                   
     
		while (Math.abs(diffX) > 0 || Math.abs(diffY) > 0) {
			  
                                                                       
                       
      
			int moveX = Math.max(-maxTiles, Math.min(maxTiles, diffX));
			int moveY = Math.max(-maxTiles, Math.min(maxTiles, diffY));

			boolean canWalkX = PathValidation.checkAdjacent(mob, last, new Point(x - (diffX - moveX), y - diffY));
			boolean canWalkY = PathValidation.checkAdjacent(mob, last, new Point(x - diffX, y - (diffY - moveY)));
			boolean canWalkXY = PathValidation.checkAdjacent(mob, last, new Point(x - (diffX - moveX), y - (diffY - moveY)));

			                                                           
			if (mob.getConfig().PLAYER_BLOCKING == 1) {
				if (mob instanceof Player && !mob.isFollowing() && pathType == PathType.WALK_TO_POINT) {
					if (Math.abs(diffX) == 1 || Math.abs(diffY) == 1) {
						if (PathValidation.isPlayerBlocking((Player) mob, x, y)) {
							return;
						}
					}
				}
			}

			if (Math.abs(diffX) > 0 && Math.abs(diffY) > 0 && canWalkX && canWalkY) {

				                                
				if (canWalkXY) {
					diffX -= moveX;
					diffY -= moveY;
				}

				                                
				else {
					boolean canWalkX2 = PathValidation.checkAdjacent(mob,
						new Point(x - (diffX - moveX), y - diffY),
						new Point(x - (diffX - moveX), y - (diffY - moveY)));
					boolean canWalkY2 = PathValidation.checkAdjacent(mob,
						new Point(x - diffX, y - (diffY - moveY)),
						new Point(x - (diffX - moveX), y - (diffY - moveY)));
					if (canWalkX2)
						diffX -= moveX;
					else if (canWalkY2)
						diffY -= moveY;
					else
						return;
				}
			}

			else if (Math.abs(diffX) > 0 && canWalkX)
				diffX -= moveX;

			else if (Math.abs(diffY) > 0 && canWalkY)
				diffY -= moveY;

			else {
				diffX -= moveX;
				diffY -= moveY;
			}
  
                                      
                                          
                   
    

                                        
                                               
                   
    

              
         
                   
                   
    
  

			addStepInternal(x - diffX, y - diffY);

			last = waypoints.peekLast();
  
                  
                  
                                         
  
		}
	}

	private void addStepInternal(int x, int y) {
		if (waypoints.size() >= MAXIMUM_SIZE) {
			return;
		}


		  
                                                                          
              
     
		Point last = waypoints.peekLast();

		  
                                                        
     
		int diffX = x - last.getX();
		int diffY = y - last.getY();

		  
                                              
     
		int dir = direction(diffX, diffY);

		  
                                        
     
		if (dir > -1) {
			  
                                                                 
                                                
      
			waypoints.add(new Point(x, y));
		}
	}

	public void addDirect(int x, int y) {
		if (waypoints.size() > MAXIMUM_SIZE)
			return;
		waypoints.addFirst(new Point(x,y));
	}
	public void finish() {
		waypoints.removeFirst();
	}

	public boolean isEmpty() {
		return waypoints.isEmpty();
	}

	Point poll() {
		return waypoints.poll();
	}

	Point getNextPoint() {
		return waypoints.getFirst();
	}

	public Deque<Point> getWaypoints() {
		return waypoints;
	}

	public Point element() {
		return waypoints.element();
	}

	public Iterator<Point> iterator() {return waypoints.iterator(); }
	Point getLastPoint() {
		return waypoints.getLast();
	}

	public int size() {
		return waypoints.size();
	}

	public PathType getPathType() {
		return pathType;
	}

	private void setPathType(PathType pathType) {
		this.pathType = pathType;
	}

	@Override
	public String toString() {

		return "Path: " + pathType.toString() + ", " + waypoints.toString() + "";
	}

	public enum PathType {
		WALK_TO_POINT,
		WALK_TO_ENTITY
	}
}
