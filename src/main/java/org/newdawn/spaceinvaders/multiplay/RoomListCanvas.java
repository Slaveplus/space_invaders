package org.newdawn.spaceinvaders.multiplay;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.net.RoomInfo;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 방 목록 화면 (임시): 위/아래로 이동, Enter로 로비로 이동, ESC로 뒤로가기
 */
public class RoomListCanvas extends Canvas implements Screen {
    private final ScreenNavigator navigator;
    private int selectedIndex = 0;
    private MultiplayerClient client;
    private MultiplayerClient.Listener listener;
    private java.util.List<RoomInfo> rooms = new java.util.ArrayList<>();
    private String status = "방 목록 요청 중...";

    public RoomListCanvas(ScreenNavigator navigator, MultiplayerClient client) {
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
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    navigator.showMultiplayerConnect();
                    break;
                case KeyEvent.VK_UP:
                    selectedIndex = Math.max(0, selectedIndex - 1);
                    break;
                case KeyEvent.VK_DOWN:
                    selectedIndex = Math.min(Math.max(0, rooms.size()-1), selectedIndex + 1);
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    if (client != null && client.isConnected()) {
                        if (rooms.isEmpty()) {
                            client.createRoom("Room-" + System.currentTimeMillis()%1000);
                            // 생성 후 목록을 다시 받으면 첫 방으로 이동 시도
                        } else {
                            int idx = Math.max(0, Math.min(selectedIndex, rooms.size()-1));
                            client.joinRoom(rooms.get(idx).id);
                        }
                    } else {
                        status = "서버에 먼저 접속하세요 (ESC로 뒤로)";
                    }
                    break;
            }
        }
    };

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        requestFocusInWindow();
        // ConnectCanvas에서 주입된 client를 사용. 자동 연결하지 않음.
        if (client == null) client = new MultiplayerClient();
        if (!client.isConnected()) {
            status = "서버에 먼저 접속하세요 (ESC로 뒤로)";
            return;
        }
        listener = new MultiplayerClient.Listener() {
            @Override public void onRooms(java.util.List<RoomInfo> list) {
                rooms = list;
                if (selectedIndex >= rooms.size()) selectedIndex = Math.max(0, rooms.size()-1);
                status = rooms.isEmpty() ? "방 없음. Enter로 방 생성" : "방 선택 후 Enter";
            }
            @Override public void onJoined(String roomId) { navigator.showMultiplayerLobby(); }
        };
        client.addListener(listener);
        client.listRooms();
    }

    @Override
    public void onHide() {
        removeKeyListener(keyAdapter);
        if (client != null && listener != null) client.removeListener(listener);
    }

    @Override
    public void update(long deltaMillis) { }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.white);
        g.drawString("[멀티플레이] 방 목록 (임시)", 40, 60);
        int y = 100;
        if (rooms.isEmpty()) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("표시할 방이 없습니다.", 60, y);
        } else {
            for (int i = 0; i < rooms.size(); i++) {
                RoomInfo r = rooms.get(i);
                g.setColor(i == selectedIndex ? Color.YELLOW : Color.WHITE);
                g.drawString(r.name + " (" + r.players + "/" + r.capacity + ")", 60, y);
                y += 20;
            }
        }
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("↑/↓: 이동, Enter: 선택/생성, ESC: 뒤로", 40, 230);
        g.drawString("상태: " + status, 40, 260);
    }
}
