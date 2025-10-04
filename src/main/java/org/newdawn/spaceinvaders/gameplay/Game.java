package org.newdawn.spaceinvaders.gameplay;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.render.CommonRenderer;
import org.newdawn.spaceinvaders.multiplay.MultiplayerClient;
import org.newdawn.spaceinvaders.net.Snapshot;
import org.newdawn.spaceinvaders.net.Snapshot.PlayerState;

public class Game extends Canvas implements Screen {
    private final ScreenNavigator navigator;
    private final MultiplayerClient client;
    private final CommonRenderer commonRenderer = new CommonRenderer();
    private final UIRenderer uiRenderer = new UIRenderer(this);

    private boolean leftDown, rightDown;
    private int dx; // -1,0,1
    private boolean showingSkillMenu;
    private int selectedSkill; // 0~2

    public Game(ScreenNavigator navigator, MultiplayerClient client, boolean network) {
        this.navigator = navigator;
        this.client = client;
        setIgnoreRepaint(true);
        setFocusable(true);
        addKeyListener(keyAdapter);
    }

    @Override public void init() { }
    @Override public void onShow() { requestFocusInWindow(); }
    @Override public void onHide() { }
    @Override public void update(long deltaMillis) { }

    @Override public void render(Graphics2D g) {
        Snapshot snap = client != null ? client.getLatestSnapshot() : null;
        commonRenderer.renderSnapshot(g, snap, () -> drawHud(g, snap));
        if (showingSkillMenu) drawSkillOverlay(g, snap);
    }

    private void drawHud(Graphics2D g, Snapshot snap) {
        g.setColor(java.awt.Color.LIGHT_GRAY);
        int wave = snap != null ? snap.wave : 0;
        g.drawString("라운드: " + wave, 20, 25);
        PlayerState me = findMe(snap);
        if (me != null) {
            g.setColor(java.awt.Color.WHITE);
            g.drawString("HP: " + me.hp + "/" + me.maxHp, 20, 50);
            int barX=20, barY=60, barW=200, barH=20;
            g.setColor(new java.awt.Color(50,50,50)); g.fillRect(barX, barY, barW, barH);
            int hpw = (int)(Math.max(0, Math.min(1.0, me.maxHp>0? (double)me.hp/me.maxHp:0))*barW);
            g.setColor(new java.awt.Color(255,0,0)); g.fillRect(barX, barY, hpw, barH);
            g.setColor(java.awt.Color.WHITE); g.drawRect(barX, barY, barW, barH);
            g.setColor(java.awt.Color.YELLOW); g.drawString("스킬 포인트: " + me.skillPoints, 20, 100);
            g.setColor(java.awt.Color.WHITE); g.drawString("공격력: " + me.attackPower, 20, 120);
            g.drawString("공격속도: " + String.format("%.1f", me.attackSpeed) + "x", 20, 135);
            g.setColor(java.awt.Color.CYAN); g.drawString("Q: 스킬 메뉴", 20, 155);
        }
    }

    private void drawSkillOverlay(Graphics2D g, Snapshot snap) {
        PlayerState me = findMe(snap);
        int sp = me != null ? me.skillPoints : 0;
        int atk = me != null ? me.attackPower : 1;
        double aspd = me != null ? me.attackSpeed : 1.0;
        int hpmax = me != null ? me.maxHp : 3;
        int cAtk = me != null ? me.costAtk : 2;
        int cAspd = me != null ? me.costAspd : 2;
        int cHp = me != null ? me.costHp : 8;
        uiRenderer.drawSkillOverlay(g, sp, atk, aspd, hpmax, cAtk, cAspd, cHp, selectedSkill);
    }

    private PlayerState findMe(Snapshot snap) {
        if (snap == null || snap.players == null) return null;
        String myId = client != null ? client.getPlayerId() : null;
        if (myId == null) return null;
        for (PlayerState ps : snap.players) if (myId.equals(ps.id)) return ps;
        return null;
    }

    private void recomputeDxAndSend() {
        int ndx = (leftDown && !rightDown) ? -1 : (rightDown && !leftDown) ? 1 : 0;
        if (ndx != dx) { dx = ndx; if (client != null) client.inputDx(dx); }
    }

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT: if (showingSkillMenu) { selectedSkill=(selectedSkill+2)%3; break; } leftDown=true; recomputeDxAndSend(); if (client!=null) client.keyDownLeft(); break;
                case KeyEvent.VK_RIGHT: if (showingSkillMenu) { selectedSkill=(selectedSkill+1)%3; break; } rightDown=true; recomputeDxAndSend(); if (client!=null) client.keyDownRight(); break;
                case KeyEvent.VK_SPACE: if (client!=null) { if (showingSkillMenu) client.upgrade(selectedSkill); else { client.inputShoot(); client.keyDownFire(); } } break;
                case KeyEvent.VK_ENTER: if (client!=null && showingSkillMenu) client.upgrade(selectedSkill); break;
                case KeyEvent.VK_Q: showingSkillMenu = !showingSkillMenu; break;
            }
        }
        @Override public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT: leftDown=false; recomputeDxAndSend(); if (client!=null) client.keyUpLeft(); break;
                case KeyEvent.VK_RIGHT: rightDown=false; recomputeDxAndSend(); if (client!=null) client.keyUpRight(); break;
                case KeyEvent.VK_SPACE: if (client!=null) client.keyUpFire(); break;
            }
        }
    };

    public void startGame() { leftDown = rightDown = false; dx = 0; if (client != null) client.inputDx(0); }
    public void startNewGame() { startGame(); }
    public void tryToFire() { if (client != null) client.inputShoot(); }
    public void goToMainMenu() { if (navigator != null) navigator.showMainMenu(); }

    // Legacy stubs
    public org.newdawn.spaceinvaders.gameplay.entity.Entity getShip() { return null; }
    public int getShipX() { return 0; }
    public int getShipY() { return 0; }
    public double getMoveSpeed() { return org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_MOVE_SPEED; }
    public java.util.ArrayList<org.newdawn.spaceinvaders.gameplay.entity.Entity> getEntities() { return new java.util.ArrayList<>(); }
    public void removeEntity(org.newdawn.spaceinvaders.gameplay.entity.Entity e) { }
    public void addEntity(org.newdawn.spaceinvaders.gameplay.entity.Entity e) { }
    public void addAlienShot(int x,int y) { }
    public void addAimedAlienShot(int x,int y,int ax) { }
    public void createSkillDrop(int x,int y,int type,int val) { }
    public void fireMissile(double tx,double ty) { }
    public int getPlayerAttackPower() { return 0; }
    public int getCurrentRound() { return 1; }
    public boolean isPlayerInvincible() { return false; }
    public boolean hasPiercingShots() { return false; }
    public boolean hasTripleShot() { return false; }
    public void addSkillToInventory(int t,int v) { }
    public void addScore(int p) { }
    public void addSkillPoints(int p) { }
    public void notifyAlienKilled() { }
    public void notifyDeath() { }
    public void notifyWin() { }
    public void spawnBoss() { }
    public boolean hasBoss() { return false; }
    public org.newdawn.spaceinvaders.gameplay.entity.BossEntity getBoss() { return null; }
    public void notifyBossDefeated() { }
    public void addBossShot(int x,int y) { }
    public void setUserManager(org.newdawn.spaceinvaders.login.UserManager um) { }
    public org.newdawn.spaceinvaders.login.UserManager getUserManager() { return null; }
    public GameStateManager getGameStateManager() { return null; }
    public GameStateManager getGameplayState() { return null; }
    public SkillManager getSkillManager() { return null; }
    public UIRenderer getUIRenderer() { return uiRenderer; }
    public void drawSkillMenu(Graphics2D g) { }
    public void drawPauseMenu(Graphics2D g) { }
    public void createExplosion(int x,int y,double r) { }
    public boolean isRequestingMainMenu() { return false; }
    public void resetMainMenuRequest() { }
}
