package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime; // 멀티 런타임 인터페이스

public class ShipEntity extends Entity {
    private MultiGameRuntime game; // 멀티 런타임
    public ShipEntity(MultiGameRuntime game,String ref,int x,int y) { super(ref,x,y); this.game = game; }
    public void move(long delta) {
        if ((dx < 0) && (x < 10)) { return; }
        if ((dx > 0) && (x > 750)) { return; }
        super.move(delta);
    }
    @Override public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (game.isPlayerInvincible()) { g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); }
        sprite.draw(g2d, (int) x, (int) y);
        if (game.isPlayerInvincible()) { g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); }
    }
    public void collidedWith(Entity other) { if (other instanceof AlienEntity) { game.notifyDeath(); } }
}
