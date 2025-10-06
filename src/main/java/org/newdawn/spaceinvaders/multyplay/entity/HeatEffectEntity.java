package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime;import java.net.URL;

public class HeatEffectEntity extends Entity {
    private MultiGameRuntime game; private long heatDuration=800; private long startTime; private BufferedImage heatImage; private float alpha=1.0f; private float scale=1.0f;
    public HeatEffectEntity(MultiGameRuntime game,int x,int y){ super("sprites/Skill/Heat.gif",x,y); this.game=game; startTime=System.currentTimeMillis(); dx=0; dy=0; loadImage(); }
    public void move(long delta){ long now=System.currentTimeMillis(); long elapsed=now-startTime; double progress=(double)elapsed/heatDuration; if(progress>=1.0){ game.removeEntity(this); return; } alpha=(float)(1.0-progress); if(progress<0.3){ scale=1.5f+(float)(progress*1.0); } else { float sp=(float)((progress-0.3)/0.7); scale=2.5f - (sp*1.0f); } }
    public void draw(Graphics g){ Graphics2D g2d=(Graphics2D)g; g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha)); if(heatImage!=null){ int ow=heatImage.getWidth(); int oh=heatImage.getHeight(); int sw=(int)(ow*scale); int sh=(int)(oh*scale); int dx=(int)x - sw/2; int dy=(int)y - sh/2; g2d.drawImage(heatImage,dx,dy,sw,sh,null);} else { int size=(int)(20*scale); g2d.setColor(new Color(255,100,0,(int)(alpha*200))); g2d.fillOval((int)x - size/2,(int)y - size/2,size,size); int inner=(int)(12*scale); g2d.setColor(new Color(255,200,0,(int)(alpha*150))); g2d.fillOval((int)x - inner/2,(int)y - inner/2,inner,inner);} g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f)); }
    private void loadImage(){ try{ URL url=getClass().getClassLoader().getResource("sprites/Skill/Heat.gif"); if(url!=null) heatImage=ImageIO.read(url); }catch(Exception e){ heatImage=null; } }
    public boolean isFinished(){ long now=System.currentTimeMillis(); return (now-startTime)>=heatDuration; }
    public void collidedWith(Entity other){ }
}
