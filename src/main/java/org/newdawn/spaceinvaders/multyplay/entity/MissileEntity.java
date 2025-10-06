package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.*;import java.awt.RenderingHints;import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime;

public class MissileEntity extends Entity {
    private MultiGameRuntime game; private double targetX; private double targetY; private double speed=400; private boolean used=false;
    public MissileEntity(MultiGameRuntime game,String sprite,int x,int y,double tx,double ty){ super(sprite,x,y); this.game=game; this.targetX=tx; this.targetY=ty; double dxTo=tx-x; double dyTo=ty-y; double dist=Math.sqrt(dxTo*dxTo+dyTo*dyTo); if(dist>0){ dx=(dxTo/dist)*speed; dy=(dyTo/dist)*speed; } else { dx=0; dy=-speed; } }
    public void move(long delta){ super.move(delta); double d=Math.sqrt((targetX-x)*(targetX-x)+(targetY-y)*(targetY-y)); if(d<30 || y<-100 || y>700 || x<-100 || x>800){ ExplosionEntity ex=new ExplosionEntity(game,"sprites/Skill/Explosion.png",(int)targetX-25,(int)targetY-25,100.0); game.addEntity(ex); game.removeEntity(this); } }
    @Override public void draw(Graphics g){ int ow=sprite.getWidth(); int oh=sprite.getHeight(); int sw=ow/2; int sh=oh/2; int drawX=(int)x - (sw - ow)/2; int drawY=(int)y - (sh - oh)/2; Graphics2D g2d=(Graphics2D)g; g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR); g2d.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY); g2d.drawImage(sprite.getImage(),drawX,drawY,sw,sh,null); }
    public void collidedWith(Entity other){ if(used) return; if(other instanceof AlienEntity){ game.removeEntity(this); game.removeEntity(other); game.notifyAlienKilled(); used=true; } if(other instanceof BossEntity){ BossEntity b=(BossEntity)other; b.takeDamage(50); game.removeEntity(this); used=true; } }
}
