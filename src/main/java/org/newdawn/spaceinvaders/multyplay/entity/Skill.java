package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime;import java.io.InputStream;

public class Skill extends Entity {
    private MultiGameRuntime game; private boolean used=false; private int skillType; private int skillValue; private double moveSpeed=140; private BufferedImage icon;
    public Skill(MultiGameRuntime game,String sprite,int x,int y,int skillType,int skillValue){ super(sprite,x,y); this.game=game; this.skillType=skillType; this.skillValue=skillValue; loadIcon(); dx=0; dy=moveSpeed; }
    public void move(long delta){ super.move(delta); if(y>700) game.removeEntity(this); }
    public void collidedWith(Entity other){ if(used) return; if(other instanceof ShipEntity){ game.removeEntity(this); game.addSkillToInventory(skillType,skillValue); used=true; } }
    public int getSkillType(){ return skillType; } public int getSkillValue(){ return skillValue; } public boolean isUsed(){ return used; }
    private void loadIcon(){ String path; switch(skillType){ case 0: path="sprites/Skill/Icon.1_45.png"; break; case 1: path="sprites/Skill/Icon.6_26.png"; break; case 2: path="sprites/Skill/Icon.7_11.png"; break; case 3: path="sprites/Skill/Missile.png"; break; default: path="sprites/Skill/Icon.1_45.png"; } try(InputStream is=getClass().getClassLoader().getResourceAsStream(path)){ if(is!=null) icon=ImageIO.read(is); } catch(Exception e){ icon=null; } }
    @Override public void draw(Graphics g){ Graphics2D g2d=(Graphics2D)g; if(icon!=null){ g2d.drawImage(icon,(int)x,(int)y,null);} else { g2d.setColor(Color.BLACK); g2d.fillRect((int)x,(int)y,32,32); g2d.setColor(Color.GRAY); g2d.drawRect((int)x,(int)y,32,32); g2d.setColor(Color.WHITE); g2d.drawString("S"+skillType,(int)x+8,(int)y+20);} }
}
