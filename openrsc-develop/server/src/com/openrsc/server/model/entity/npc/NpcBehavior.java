package com.openrsc.server.model.entity.npc;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.content.EnchantedCrowns;
import com.openrsc.server.event.rsc.impl.combat.AggroEvent;
import com.openrsc.server.model.PathValidation;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.action.ActionType;
import com.openrsc.server.model.action.WalkToAction;
import com.openrsc.server.model.action.WalkToMobAction;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.states.CombatState;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;

import java.util.Optional;

import static com.openrsc.server.plugins.Functions.*;


public class NpcBehavior {

	private long lastMovement;
	private long lastTackleAttempt;
	private static final int[] TACKLING_XP = {7, 10, 15, 20};

	protected Npc npc;

	protected Mob target;
	private State state = State.ROAM;
	private int aggroRadius;

	private boolean draynorManorSkeleton;
	private boolean blackKnightsFortress;
	private int tickFactor;

	NpcBehavior(final Npc npc) {
		this.npc = npc;
		this.tickFactor = (int)Math.ceil(640.0 / npc.getConfig().GAME_TICK);
		this.blackKnightsFortress = npc.getLoc().startX() > 274 && npc.getLoc().startX() < 283
			&& npc.getLoc().startY() > 432 && npc.getLoc().startY() < 441;
		this.draynorManorSkeleton = npc.getID() == NpcId.SKELETON_LVL21.id()
			&& npc.getLoc().startX() >= 208 && npc.getLoc().startX() <= 211
			&& npc.getLoc().startY() >= 545 && npc.getLoc().startY() <= 546;

		switch (NpcId.getById(npc.getID())) {
			case BANDIT_AGGRESSIVE:
				aggroRadius = 2;
				break;
			case BLACK_KNIGHT:
				aggroRadius = 10;
				break;
			case UNDEADONE:
				aggroRadius = 3;
				break;
			default:
				aggroRadius = npc.getWorld().getServer().getConfig().AGGRO_RANGE;
		}
	}

	public void tick() {
		if (state == State.ROAM) {
			handleRoam();
		} else if (state == State.AGGRO) {
			handleAggro();
		} else if (state == State.COMBAT) {
			handleCombat();
		} else if (state == State.TACKLE) {
			handleTackle();
		} else if (state == State.RETREAT || state == State.TACKLE_RETREAT) {
			                                                                                              
			                                                                                                                                   
			if (npc.finishedPath() && checkCombatTimer(npc.getCombatTimer(), 15 * tickFactor)) {
				npc.setLastCombatState(CombatState.WAITING);
				setRoaming();
			}
		}
	}

	private void handleRoam() {

		                               
		if (npc.getID() == NpcId.FIRST_PLAGUE_SHEEP.id() ||
			npc.getID() == NpcId.SECOND_PLAGUE_SHEEP.id() ||
			npc.getID() == NpcId.THIRD_PLAGUE_SHEEP.id() ||
			npc.getID() == NpcId.FOURTH_PLAGUE_SHEEP.id()) {
			return;
		}

		                                                     
		if (npc.inCombat()) {
			state = State.COMBAT;
			target = npc.getOpponent();
			return;
		} else if (npc.isBusy() || npc.isRespawning()) {
			return;
		}

		                          
		if (checkCombatTimer(npc.getCombatTimer(), 5 * tickFactor)) {
			if ((npc.getDef().isAggressive() && !draynorManorSkeleton) || npc.getLocation().inWilderness() || (blackKnightsFortress)) {

				                                       
				for (Player player : npc.getViewArea().getPlayersInView()) {

					if (!player.withinRange(npc, aggroRadius)) continue;

					                                          
					if (!canAggro(player)) {
						continue;
					}

					                                                                         
					if (npc.getLastOpponent() != null && npc.getLastOpponent().equals(player) && checkCombatTimer(npc.getLastOpponent().getCombatTimer(), 15 * tickFactor)) {
						npc.setLastOpponent(null);
						setRoaming();
					}

					                                                
					else {
						setChasing(player);
						handleAggro();
						new AggroEvent(npc.getWorld(), npc, player);
					}

					                                              
					                                         
					return;
				}
			}
		}

		                   
		if (System.currentTimeMillis() - lastTackleAttempt > npc.getConfig().GAME_TICK * 5 &&
			checkCombatTimer(npc.getCombatTimer(), 5)
			&& npc.getDef().getName().toLowerCase().equals("gnome baller")
			&& !(npc.getID() == NpcId.GNOME_BALLER_TEAMNORTH.id() || npc.getID() == NpcId.GNOME_BALLER_TEAMSOUTH.id())) {
			for (Player player : npc.getViewArea().getPlayersInView()) {
				int range = 1;
				if (!player.withinRange(npc, range) || !player.getCarriedItems().hasCatalogID(ItemId.GNOME_BALL.id(), Optional.of(false))
					|| !inArray(player.getAttribute("gnomeball_npc", -1), -1, 0))
					continue;                                                                                

				            
				state = State.TACKLE;
				target = player;
				return;
			}
		}

		                                            
		                                     
		if (checkCombatTimer(lastMovement, 5 * tickFactor) && checkCombatTimer(npc.getCombatTimer(), 5 * tickFactor) && npc.finishedPath()) {
			lastMovement = System.currentTimeMillis();
			int rand = DataConversions.random(0, 1);

			                                                      
			if (!npc.isBusy() && rand == 1 && !npc.isRemoved() && !npc.isRespawning()) {
				Point point = npc.walkablePoint(Point.location(npc.getLoc().minX(), npc.getLoc().minY()),
					Point.location(npc.getLoc().maxX(), npc.getLoc().maxY()));
				npc.walk(point.getX(), point.getY());
			}

			                                                               
			if (npc.getLastOpponent() != null) {
				npc.setLastOpponent(null);
			}

			                                                       
			if (npc.getInteractingPlayer() != null) {
				npc.setInteractingPlayer(null);
			}
		}
	}

	private void handleAggro() {
		                                                             
		if (target == null || target.isRemoved() || target.inCombat() || npc.isRespawning() || npc.isRemoved()) {
			setRoaming();
			return;
		}

		                          
		if (target.getX() < (npc.getLoc().minX() - 4) || target.getX() > (npc.getLoc().maxX() + 4)
			|| target.getY() < (npc.getLoc().minY() - 4) || target.getY() > (npc.getLoc().maxY() + 4)) {

			                                                 
			if (npc.getConfig().WANT_IMPROVED_PATHFINDING) {
				Point origin = new Point(npc.getLoc().startX(), npc.getLoc().startY());
				npc.walkToEntityAStar(origin.getX(), origin.getY());
				npc.getSkills().normalize();
				npc.cure();
			}
			setRoaming();
			return;
		}

		                                               
		if (npc.inCombat() && npc.getOpponent() != target) {
			npc.setLastOpponent(null);
			setFighting(npc.getOpponent());
			return;
		}

		                                                                   
		lastMovement = System.currentTimeMillis();
		int numTicks = target.getCombatState() == CombatState.RUNNING ? 5 * tickFactor : (int)Math.ceil(640.0 / target.getConfig().GAME_TICK);
		if (checkCombatTimer(target.getCombatTimer(), numTicks)) {
			if (npc.getWorld().getServer().getConfig().WANT_IMPROVED_PATHFINDING)
				npc.walkToEntityAStar(target.getX(), target.getY());
			else
				npc.walkToEntity(target.getX(), target.getY());

			                                                                              
			                                                                                                                                           
			Point checkedPoint = npc.getWalkingQueue().getNextMovement();
			if (checkedPoint.withinRange(target.getLocation(), 1)
				&& PathValidation.checkAdjacentDistance(npc.getWorld(), checkedPoint, target.getLocation(), true, false)
				&& !target.inCombat()) {
				if (target.isPlayer() && EnchantedCrowns.shouldActivate((Player)target, ItemId.CROWN_OF_MIMICRY)
					&& ((Player)target).getBatch() != null && !((Player)target).getBatch().isComplete()) {
					((Player)target).playerServerMessage(MessageType.QUEST, "Your crown shines and you dodge an attack!");
					npc.setLastCombatState(CombatState.RUNNING);
					target.setCombatTimer(target.getConfig().GAME_TICK * 5);
					walk_retreat(-3);
					EnchantedCrowns.useCharge(((Player)target), ItemId.CROWN_OF_MIMICRY);
				} else {
					setFighting(target);
				}
			}
		}
	}

	private void handleCombat() {

		                                
		if (target == null || npc.isRespawning() || npc.isRemoved() || target.isRemoved()) {
			setRoaming();
		}

		                                                                                 
                             
                            
                              

                                                 
                                                                          
                                              
              
    


   

    

		                                                       
		                                                         
		else if (!npc.inCombat() && checkCombatTimer(npc.getCombatTimer(), 5 * tickFactor)) {
			npc.setExecutedAggroScript(false);

			                                                                          
			if (canAggro(target) && target == npc.getLastOpponent()) {
				if (target.isPlayer()) {
					setChasing((Player)target);
				}
				else {
					setChasing((Npc)target);
				}

			                                                                   
			} else {
				if (!npc.isFollowing())	setRoaming();
			}
		}
	}

	private void handleTackle() {
		                                                    
		if (target == null || npc.isRespawning() || npc.isRemoved() || target.isRemoved() || target.inCombat() || target.isBusy()) {
			setRoaming();
		}
		                          
		else if (target.getX() < (npc.getLoc().minX() - 4) || target.getX() > (npc.getLoc().maxX() + 4)
			|| target.getY() < (npc.getLoc().minY() - 4) || target.getY() > (npc.getLoc().maxY() + 4)) {
			setRoaming();
		}
		if (target.isPlayer()) {
			attemptTackle(npc, (Player) target);
			tackle_retreat();
		}
	}

	private synchronized void attemptTackle(final Npc n, final Player player) {
		int otherNpcId = player.getAttribute("gnomeball_npc", -1);
		if ((!inArray(otherNpcId, -1, 0) && npc.getID() != otherNpcId) || player.getAttribute("throwing_ball_game", false)) {
			return;
		}
		                                                      
		                                                
		player.message("the gnome trys to tackle you");
		if (DataConversions.random(0, 1) == 0) {
			                                              
			player.playerServerMessage(MessageType.QUEST, "You manage to push him away");
			npcYell(player, npc, "grrrrr");
			player.incExp(Skill.AGILITY.id(), TACKLING_XP[DataConversions.random(0, 3)], true);
		} else {
			if (!inArray(player.getAttribute("gnomeball_npc", -1), -1, 0) || player.getAttribute("throwing_ball_game", false)) {
				                                                           
				return;
			}
			player.setAttribute("gnomeball_npc", npc.getID());
			player.getCarriedItems().remove(new Item(ItemId.GNOME_BALL.id()));
			player.playerServerMessage(MessageType.QUEST, "he takes the ball...");
			player.playerServerMessage(MessageType.QUEST, "and pushes you to the floor");
			player.damage((int) (Math.ceil(player.getSkills().getLevel(Skill.HITS.id()) * 0.05)));
			say(player, "ouch");
			npcYell(player, npc, "yeah");
		}
		lastTackleAttempt = System.currentTimeMillis();
	}

	public void retreat() {
		retreat(-1);
	}

	public void retreat(int time) {
		state = State.RETREAT;
		Mob opponent = npc.getOpponent();
		if (opponent == null) return;

		opponent.setLastOpponent(npc);
		npc.setLastOpponent(opponent);
		npc.setCombatTimer();
		if (opponent.isPlayer()) {
			Player victimPlayer = ((Player) opponent);
			victimPlayer.resetAll();
			victimPlayer.message("Your opponent is retreating");
			ActionSender.sendSound(victimPlayer, "retreat");
		}
		npc.setLastCombatState(CombatState.RUNNING);
		opponent.setLastCombatState(CombatState.WAITING);
		npc.setRanAwayTimer();

		npc.resetCombatEvent();

		walk_retreat(time);
	}

	private void walk_retreat(int time) {
		Point walkTo;
		if (time == -1) {
			walkTo = Point.location(DataConversions.random(npc.getLoc().minX(), npc.getLoc().maxX()),
				DataConversions.random(npc.getLoc().minY(), npc.getLoc().maxY()));
		} else {
			final int newX = npc.getX() + (DataConversions.random(-1, 1) * time);
			final int newY = npc.getY() + (DataConversions.random(-1, 1) * time);
			walkTo = Point.location(newX, newY);
		}
		npc.walk(walkTo.getX(), walkTo.getY());
	}

	private void tackle_retreat() {
		state = State.TACKLE_RETREAT;
		npc.setLastCombatState(CombatState.RUNNING);
		target.setLastCombatState(CombatState.WAITING);
		npc.resetCombatEvent();

		Point walkTo = Point.location(DataConversions.random(npc.getLoc().minX(), npc.getLoc().maxX()),
			DataConversions.random(npc.getLoc().minY(), npc.getLoc().maxY()));
		npc.walk(walkTo.getX(), walkTo.getY());
	}

	private boolean aggressiveCheck(Mob target) {
		boolean bothInWilderness = (target.getLocation().inWilderness() && npc.getLocation().inWilderness());
		boolean levelMeetsStandard = target.getCombatLevel() < ((npc.getNPCCombatLevel() * 2) + 1);
		return npc.getDef().isAggressive() && (levelMeetsStandard || bothInWilderness || npc.getConfig().NPC_AGGRO_DONT_CHECK_LEVEL);
	}

	                                                   
	private boolean canAggro(final Mob target) {
		boolean outOfBounds = !target.getLocation().inBounds(npc.getLoc().minX - 4, npc.getLoc().minY - 4,
			npc.getLoc().maxX + 4, npc.getLoc().maxY + 4);

		boolean targetInCombat = target.inCombat();

		boolean isPlayer = target instanceof Player;

		boolean lastLogin = isPlayer && checkCombatTimer(((Player)target).getLastLogin(), 5);

		int numTicks = target.getCombatState() == CombatState.RUNNING ? 5 : (int)(Math.ceil(640.0 / target.getConfig().GAME_TICK) - 1);

		boolean targetCombatTimeoutExceeded = checkCombatTimer(target.getCombatTimer(), numTicks);

		boolean isAggressive = aggressiveCheck(target);

		boolean impervious = isPlayer
			&& (((Player) target).isInvulnerableTo(npc) || ((Player) target).isInvisibleTo(npc));

		return isAggressive
			&& !impervious
			&& !outOfBounds
			&& !targetInCombat
			&& lastLogin
			&& targetCombatTimeoutExceeded;
	}

	private boolean grandTreeGnome(final Npc npc) {
		String npcName = npc.getDef().getName();
		return npcName.equalsIgnoreCase("gnome child") || npcName.equalsIgnoreCase("gnome local");
	}

	public State getBehaviorState() {
		return state;
	}

	boolean isChasing() {
		return state == State.AGGRO;
	}

	public void setChasing(final Player player) {
		state = State.AGGRO;
		target = player;
	}

	public void setChasing(final Npc npc) {
		state = State.AGGRO;
		target = npc;
	}

	Player getChasedPlayer() {
		if (target != null && target.isPlayer())
			return (Player) target;
		return null;
	}

	Npc getChasedNpc() {
		if (target != null && target.isNpc())
			return (Npc) target;
		return null;
	}

	                                                     
	private boolean checkCombatTimer(long timer, int ticks) {
		return (System.currentTimeMillis() - timer) >= (npc.getConfig().GAME_TICK * ticks);
	}

	public Mob getChaseTarget() {
		return target;
	}

	public void setRoaming() {
		npc.setExecutedAggroScript(false);
		target = null;
		state = State.ROAM;
	}

	private void setFighting(final Mob target) {
		npc.startCombat(target);
		state = State.COMBAT;
	}

	public boolean shouldRetreat(final Npc npc) {
		if (!npc.getConfig().NPC_DONT_RETREAT) {
			if (npc.getWorld().getServer().getConstants().getRetreats().npcData.containsKey(npc.getID())) {
				return npc.getSkills().getLevel(Skill.HITS.id()) <= npc.getWorld().getServer().getConstants().getRetreats().npcData.get(npc.getID());
			}
		}

		return false;
	}

	enum State {
		ROAM, AGGRO, COMBAT, RETREAT, TACKLE, TACKLE_RETREAT;
	}
}
