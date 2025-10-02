package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
// no embedded server; user provides host/port

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 서버 주소/포트 입력용 간단한 프레임 UI (임시)
 * 실제 구현 전까지는 안내 텍스트와 뒤로가기/다음 이동만 처리
 */
public class ConnectCanvas extends Canvas implements Screen {
    private final ScreenNavigator navigator;
    private MultiplayerClient client;
    private String status = "서버 주소와 포트를 입력하세요.";
    private String host = "127.0.0.1";
    private String port = "5057";
    private boolean editingHost = true; // true=host, false=port

    public ConnectCanvas(ScreenNavigator navigator, MultiplayerClient client) {
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
                navigator.showMainMenu();
            } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                editingHost = !editingHost;
                repaint();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                try {
                    int p = Integer.parseInt(port.trim());
                    if (client == null) client = new MultiplayerClient();
                    status = "연결 중...";
                    repaint();
                    if (client.connect(host.trim(), p)) {
                        status = "연결됨: " + host + ":" + p;
                        navigator.showMultiplayerRoomList();
                    } else {
                        status = "연결 실패: " + host + ":" + p;
                    }
                } catch (NumberFormatException ex) {
                    status = "포트는 숫자여야 합니다.";
                }
            } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyChar() == '\b') {
                if (editingHost) {
                    if (!host.isEmpty()) host = host.substring(0, host.length()-1);
                } else {
                    if (!port.isEmpty()) port = port.substring(0, port.length()-1);
                }
                repaint();
            } else {
                char ch = e.getKeyChar();
                if (editingHost) {
                    // 허용: 영문/숫자/점/하이픈
                    if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == ':') {
                        host += ch;
                        repaint();
                    }
                } else {
                    if (Character.isDigit(ch)) {
                        port += ch;
                        repaint();
                    }
                }
            }
        }
    };

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        requestFocusInWindow();
        // manual connect; no auto attempt
    }

    @Override
    public void onHide() {
        removeKeyListener(keyAdapter);
    }

    @Override
    public void update(long deltaMillis) {
        // no-op
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.white);
        g.drawString("[멀티플레이] 서버 접속", 40, 60);
        g.drawString("Host: " + host + (editingHost ? "  <" : ""), 40, 90);
        g.drawString("Port: " + port + (!editingHost ? "  <" : ""), 40, 110);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("입력: Host/Port, Tab: 필드 전환, Enter: 접속, ESC: 메인 메뉴", 40, 140);
        g.setColor(Color.WHITE);
        g.drawString("상태: " + status, 40, 170);
    }
    
    public MultiplayerClient getClient() { return client; }
}
