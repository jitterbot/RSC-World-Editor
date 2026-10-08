package com.openrsc.server.model.entity.update;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

   
                                              
   
public class UpdateFlags {

	   
                                                          
    
	private AtomicReference<Bubble> actionBubble = new AtomicReference<Bubble>();
	private AtomicReference<BubbleNpc> actionBubbleNpc = new AtomicReference<BubbleNpc>();
	   
                                                       
    
	private AtomicBoolean appearanceChanged = new AtomicBoolean(true);
	   
                                                          
    
	private AtomicReference<ChatMessage> chatMessage = new AtomicReference<ChatMessage>();

	   
                                       
    
	private AtomicBoolean pluginChatMessage = new AtomicBoolean(false);

	   
                                                       
    
	private AtomicReference<Wield> wield = new AtomicReference<Wield>();
	private AtomicReference<Wield> wield2 = new AtomicReference<Wield>();
	   
                                                       
    
	private AtomicReference<Skull> skull = new AtomicReference<Skull>();
	   
                                                       
    
	private AtomicReference<Damage> damage = new AtomicReference<Damage>();
	   
                                                   
    
	private AtomicReference<HpUpdate> hpUpdate = new AtomicReference<HpUpdate>();
	   
                                       
    
	private AtomicReference<Projectile> projectile = new AtomicReference<Projectile>();

	public AtomicReference<Bubble> getActionBubble() {
		return actionBubble;
	}

	public AtomicReference<BubbleNpc> getActionBubbleNpc() {
		return actionBubbleNpc;
	}

	public void setActionBubble(Bubble bubble) {
		this.actionBubble.set(bubble);
	}

	public void setActionBubbleNpc(BubbleNpc bubble) {
		this.actionBubbleNpc.set(bubble);
	}

	private AtomicBoolean getAppearanceChanged() {
		return appearanceChanged;
	}
	public void setAppearanceChanged(boolean b) {
		this.appearanceChanged.set(b);
	}
	public ChatMessage getChatMessage() {
		return chatMessage.get();
	}
	public void setChatMessage(ChatMessage message) {
		this.chatMessage.set(message);
	}

	public void setPluginChatMessage(boolean pcm){
		this.pluginChatMessage.set(pcm);
	}

	public AtomicBoolean isPluginChatMessage(){
		return this.pluginChatMessage;
	}

	public AtomicReference<Damage> getDamage() {
		return damage;
	}
	public AtomicReference<Skull> getSkull() {
		return skull;
	}
	public AtomicReference<Wield> getWield() {
		return wield;
	}
	public AtomicReference<Wield> getWield2() {
		return wield2;
	}
	public AtomicReference<HpUpdate> getHpUpdate() {
		return hpUpdate;
	}

	public void setDamage(Damage damage) {
		this.damage.set(damage);
	}
	public void setSkull(Skull skull) {
		this.skull.set(skull);
	}
	public void setWield(Wield wield) {
		this.wield.set(wield);
	}
	public void setWield2(Wield wield2) {
		this.wield2.set(wield2);
	}

	public void setHpUpdate(HpUpdate hpUpdate) {
		this.hpUpdate.set(hpUpdate);
	}

	public AtomicReference<Projectile> getProjectile() {
		return projectile;
	}

	public void setProjectile(Projectile projectile) {
		this.projectile.set(projectile);
	}

	public boolean hasAppearanceChanged() {
		return getAppearanceChanged().get();
	}

	public boolean hasBubble() {
		return getActionBubble().get() != null;
	}
	public boolean hasBubbleNpc() {
		return getActionBubbleNpc().get() != null;
	}

	public boolean hasChatMessage() {
		return getChatMessage() != null;
	}

	public boolean hasFiredProjectile() {
		return getProjectile().get() != null;
	}

	public boolean hasTakenDamage() {
		return getDamage().get() != null;
	}
	public boolean hasSkulled() {
		return getSkull().get() != null;
	}
	public boolean changedWield() {
		return getWield().get() != null;
	}
	public boolean changedWield2() {
		return getWield2().get() != null;
	}

	public boolean hasTakenHpUpdate() {
		return getHpUpdate().get() != null;
	}

	   
                           
    
	public void reset() {
		projectile.set(null);
		actionBubble.set(null);
		damage.set(null);
		skull.set(null);
		wield.set(null);
		hpUpdate.set(null);
		projectile.set(null);
		chatMessage.set(null);
		pluginChatMessage.set(false);

		appearanceChanged.set(false);
	}

	@Override
	public String toString() {
		return "UpdateFlags{" +
			"actionBubble=" + actionBubble +
			", actionBubbleNpc=" + actionBubbleNpc +
			", appearanceChanged=" + appearanceChanged +
			", chatMessage=" + chatMessage +
			", pluginChatMessage=" + pluginChatMessage +
			", damage=" + damage +
			", skull=" + skull +
			", wield=" + wield +
			", hpUpdate=" + hpUpdate +
			", projectile=" + projectile +
			'}';
	}
}
