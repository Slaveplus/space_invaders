package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 로비 화면 (임시): Enter로 게임 시작, ESC로 방 목록으로
 */
public class LobbyCanvas extends Canvas implements Screen {
    private final ScreenNavigator navigator;
    private MultiplayerClient client;
    private boolean started = false;
    private boolean switchedToGame = false;
    private String status = "준비하려면 Enter";
    private MultiplayerClient.Listener listener;

    public LobbyCanvas(ScreenNavigator navigator, MultiplayerClient client) {
        this.navigator = navigator;
        this.client = client;
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.WIDTH, SpaceInvadersApp.HEIGHT);
        setFocusable(true);
    }

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                navigator.showMultiplayerRoomList();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (client != null) {
                    client.ready();
                    client.start();
                }
            }
        }
    };

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        requestFocusInWindow();
        if (client == null) client = new MultiplayerClient();
        if (!client.isConnected()) client.connect("127.0.0.1", 5057);
        listener = new MultiplayerClient.Listener() {
            @Override public void onStart() {
                started = true;
                // 첫 스냅샷 수신 후 화면 전환하여 '스냅샷 대기중' 표시를 피한다
            }
            @Override public void onSnapshot(org.newdawn.spaceinvaders.net.Snapshot snapshot) {
                if (started && !switchedToGame) {
                    switchedToGame = true;
                    navigator.startMultiplayerGame();
                }
            }
        };
        client.addListener(listener);
    }

    @Override
    public void onHide() {
    removeKeyListener(keyAdapter);
    if (client != null && listener != null) client.removeListener(listener);
    started = false;
    switchedToGame = false;
    }

    @Override
    public void update(long deltaMillis) { }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.white);
        g.drawString("[멀티플레이] 로비 (임시)", 40, 60);
        g.drawString("Enter: 준비/시작, ESC: 방 목록", 40, 80);
        String s = started ? (switchedToGame ? "전환 중..." : "게임 시작 신호 수신, 동기화 중...") : status;
        g.drawString("상태: " + s, 40, 110);
    }
}
