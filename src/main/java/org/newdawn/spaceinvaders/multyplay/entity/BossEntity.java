package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.*;import org.newdawn.spaceinvaders.multyplay.core.MultiGameRuntime;

public class BossEntity extends Entity {
    private MultiGameRuntime game; private boolean used=false; private int currentHP; private int maxHP; private double moveSpeed=50; private long lastFire=0; private long firingInterval=1500; private boolean movingRight=true; private int round; private int phase=1; private double fanAngle=0;
    private static String getBossSpriteForRound(int round){ return "sprites/Boss/2round_Boss.png"; }
    public BossEntity(MultiGameRuntime game,int x,int y,int round){ super(getBossSpriteForRound(round),x,y); this.game=game; this.round=round; maxHP=50+(round*30); currentHP=maxHP; moveSpeed=50+(round*10); firingInterval=Math.max(2000,3000-(round*200)); dx=0; dy=0; }
    public void move(long delta){ dx=0; dy=0; tryToFire(); }
    public void tryToFire(){ if(System.currentTimeMillis()-lastFire < firingInterval) return; lastFire=System.currentTimeMillis(); double playerX=game.getShip().getX()+15; double playerY=game.getShip().getY(); double fx=x; double fy=y+75; int pattern=(int)(Math.random()*5); switch(pattern){ case 0: fireLinear(fx,fy); break; case 1: fireAimed(fx,fy,playerX,playerY); break; case 2: fireFan(fx,fy); break; case 3: fireCircle(fx,fy); break; case 4: fireSplit(fx,fy); break; } }
    private void fireLinear(double fx,double fy){ int spacing=40; for(int i=0;i<4;i++){ int offset=(i-1)*spacing - spacing/2; createShot(fx+offset,fy,0,1,300); } }
    private void fireAimed(double fx,double fy,double px,double py){ double dx=px-fx; double dy=py-fy; double dist=Math.sqrt(dx*dx+dy*dy); if(dist>0){ double dirX=dx/dist; double dirY=dy/dist; for(int i=-1;i<=1;i++){ double ang=i*0.2; double sx=dirX*Math.cos(ang)-dirY*Math.sin(ang); double sy=dirX*Math.sin(ang)+dirY*Math.cos(ang); createShot(fx,fy,sx,sy,350); } } }
    private void fireFan(double fx,double fy){ int n=5; double start=Math.PI/2 - Math.PI/6; double end=Math.PI/2 + Math.PI/6; for(int i=0;i<n;i++){ double a=start+(end-start)*i/(n-1); createShot(fx,fy,Math.cos(a),Math.sin(a),300); } }
    private void fireCircle(double fx,double fy){ int n=8; for(int i=0;i<n;i++){ double a=2*Math.PI*i/n; createShot(fx,fy,Math.cos(a),Math.sin(a),250); } }
    private void fireSplit(double fx,double fy){ BossShotEntity big=new BossShotEntity(game,(int)fx,(int)fy,0,1,200,16,true,350,12); game.addEntity(big); }
    private void createShot(double sx,double sy,double dirX,double dirY,double speed){ BossShotEntity shot=new BossShotEntity(game,(int)sx,(int)sy,dirX,dirY,speed); game.addEntity(shot); }
    public void takeDamage(int damage){ currentHP-=damage; int newPhase = currentHP>maxHP*0.66?1: (currentHP>maxHP*0.33?2:3); if(newPhase!=phase) phase=newPhase; if(currentHP<=0){ for(int i=0;i<5;i++){ int ex=(int)(x+(Math.random()-0.5)*100); int ey=(int)(y+(Math.random()-0.5)*100); game.createExplosion(ex,ey,80.0); } game.addScore(1000*round); game.addSkillPoints(5*round); game.notifyBossDefeated(); game.removeEntity(this); used=true; } }
    public void draw(Graphics g){ Graphics2D g2d=(Graphics2D)g; if(sprite!=null){ int bw=300,bh=300; int dx=(int)x - bw/2; int dy=(int)y - bh/2; g2d.drawImage(sprite.getImage(),dx,dy,dx+bw,dy+bh,0,0,sprite.getWidth(),sprite.getHeight(),null);} g.setColor(Color.YELLOW); g.setFont(g.getFont().deriveFont(16f)); g.drawString("Phase "+phase,(int)x-25,(int)y-170); g.setColor(Color.RED); g.fillRect((int)x-50,(int)y+170,100,8); g.setColor(Color.GREEN); int w=(int)(100*((double)currentHP/maxHP)); g.fillRect((int)x-50,(int)y+170,w,8); g.setColor(Color.WHITE); g.drawRect((int)x-50,(int)y+170,100,8); }
    public void collidedWith(Entity other){ }
    public int getCurrentHP(){ return currentHP; } public int getMaxHP(){ return maxHP; } public int getPhase(){ return phase; }
    public java.awt.Rectangle getBounds(){ return new java.awt.Rectangle((int)x-150,(int)y-150,300,300); }
}
