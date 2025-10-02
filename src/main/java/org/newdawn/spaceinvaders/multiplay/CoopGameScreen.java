package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.UIRenderer;
import org.newdawn.spaceinvaders.net.Snapshot;
import org.newdawn.spaceinvaders.gameplay.render.CommonRenderer;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 서버 스냅샷을 사용해 같은 화면에서 여러 플레이어를 렌더링하는 협동 모드 화면.
 * 싱글플레이와 똑같은 스프라이트/배경/오버레이를 재사용합니다.
 */
public class CoopGameScreen extends Canvas implements Screen {
    private final MultiplayerClient client;
    private int dx = 0; // -1/0/1

    // 공통 렌더 파사드
    private CommonRenderer commonRenderer;

    // Pause & Skill 메뉴(싱글과 동일 UX)
    private boolean showingPauseMenu = false;
    private int selectedPauseIndex = 0; // 0: 계속하기, 1: 메인메뉴
    private final UIRenderer ui = new UIRenderer(null);
    private ScreenNavigator navigator;

    private boolean showingSkillMenu = false;
    private int selectedSkill = 0; // 0: 공격력, 1: 공속, 2: HP

    public CoopGameScreen(MultiplayerClient client) {
        this.client = client;
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.WIDTH, SpaceInvadersApp.HEIGHT);
        setFocusable(true);

        // 공통 렌더러 초기화
        commonRenderer = new CommonRenderer();
    }

    // 동시 키 입력 처리
    private boolean leftDown = false;
    private boolean rightDown = false;

    private void recomputeDxAndSend() {
        int ndx;
        if (leftDown && !rightDown) ndx = -1;
        else if (rightDown && !leftDown) ndx = 1;
        else ndx = 0;
        if (ndx != dx) { dx = ndx; client.inputDx(dx); }
    }

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
            if (showingPauseMenu) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { showingPauseMenu = false; return; }
                if (e.getKeyCode() == KeyEvent.VK_UP) { selectedPauseIndex = (selectedPauseIndex + 3 - 1) % 3; }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) { selectedPauseIndex = (selectedPauseIndex + 1) % 3; }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (selectedPauseIndex == 0) { showingPauseMenu = false; }
                    else if (selectedPauseIndex == 1) { if (navigator != null) navigator.showMainMenu(); }
                    else { showingPauseMenu = false; }
                }
                return;
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (showingSkillMenu) { selectedSkill = (selectedSkill + 2) % 3; break; }
                    leftDown = true; recomputeDxAndSend(); client.keyDownLeft();
                    break;
                case KeyEvent.VK_RIGHT:
                    if (showingSkillMenu) { selectedSkill = (selectedSkill + 1) % 3; break; }
                    rightDown = true; recomputeDxAndSend(); client.keyDownRight();
                    break;
                case KeyEvent.VK_SPACE:
                    if (showingSkillMenu) { client.upgrade(selectedSkill); }
                    else { client.inputShoot(); client.keyDownFire(); }
                    break;
                case KeyEvent.VK_ESCAPE: showingPauseMenu = true; selectedPauseIndex = 0; break;
                case KeyEvent.VK_Q: showingSkillMenu = !showingSkillMenu; break;
                case KeyEvent.VK_ENTER: if (showingSkillMenu) { client.upgrade(selectedSkill); } break;
            }
        }
        @Override public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT: leftDown = false; recomputeDxAndSend(); client.keyUpLeft(); break;
                case KeyEvent.VK_RIGHT: rightDown = false; recomputeDxAndSend(); client.keyUpRight(); break;
                case KeyEvent.VK_SPACE: client.keyUpFire(); break;
            }
        }
    };

    @Override public void onShow() {
        addKeyListener(keyAdapter);
        requestFocusInWindow();
        leftDown = rightDown = false; dx = 0; client.inputDx(0);
    }

    @Override public void onHide() { removeKeyListener(keyAdapter); }

    @Override public void init() { }

    @Override public void update(long deltaMillis) { /* 서버 스냅샷 기반 */ }

    @Override public void render(Graphics2D g) {
        Snapshot snap = client.getLatestSnapshot();
        commonRenderer.renderSnapshot(g, snap, () -> {
            // 간단 HUD
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("players=" + (snap != null ? snap.players.size() : 0) + " wave=" + (snap != null ? snap.wave : 0), 10, 15);
        });

        // 안내: 업그레이드 사용법
        g.setColor(Color.GRAY);
        g.drawString("Q: 스킬 업그레이드 열기/닫기  ←/→: 항목 선택  Enter/Space: 구매 (공격력/공속/HP)", 10, SpaceInvadersApp.HEIGHT - 10);

        if (showingPauseMenu) {
            g.setColor(new Color(0,0,0,120));
            g.fillRect(0,0,SpaceInvadersApp.WIDTH, SpaceInvadersApp.HEIGHT);
            ui.drawPauseOverlay(g, selectedPauseIndex);
        }

        if (showingSkillMenu) {
            Snapshot.PlayerState me = null;
            String myId = client.getPlayerId();
            if (myId != null) {
                for (Snapshot.PlayerState ps : snap.players) if (myId.equals(ps.id)) { me = ps; break; }
            }
            int skillPoints = me != null ? me.skillPoints : 0;
            int atk = me != null ? me.attackPower : 1;
            double aspd = me != null ? me.attackSpeed : 1.0;
            int hpmax = me != null ? me.maxHp : 3;
            int cAtk = me != null ? me.costAtk : 2;
            int cAspd = me != null ? me.costAspd : 2;
            int cHp = me != null ? me.costHp : 8;
            ui.drawSkillOverlay(g, skillPoints, atk, aspd, hpmax, cAtk, cAspd, cHp, selectedSkill);
        }
    }

    public void setNavigator(ScreenNavigator navigator) { this.navigator = navigator; }
}
