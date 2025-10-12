package org.newdawn.spaceinvaders.room;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;

/** 방 목록 화면 */
public class RoomListCanvas extends Canvas implements Screen, GameClientListener {
    private final ScreenNavigator navigator;
    private final GameClient client;
    private final List<RoomInfo> rooms = new CopyOnWriteArrayList<>();
    private int selectedIndex = 0;
    private boolean creatingOverlay = false; // 방 생성 선택(싱글/멀티)
    private int createSelect = 0; // 0 싱글 1 멀티
    private Font titleFont = new Font("Arial", Font.BOLD, 36);
    private Font listFont = new Font("Arial", Font.BOLD, 20);
    private String infoMessage = "";
    private int infoTimer = 0;
    // 배경
    private BufferedImage backgroundImage;

    // 하단 버튼 포커스/선택 상태
    private boolean bottomFocus = false; // TAB 으로 전환
    private int bottomSelected = 0; // 0: 방 생성하기, 1: 뒤로가기
    // 자동 새로고침 타이머 (ms 누적식)
    private long refreshAccumulator = 0; // update에서 누적 후 2초마다 LIST_ROOMS

    public interface RoomJoinHandler { void joinRoom(String roomId); }
    public interface RoomCreateHandler { void createRoom(boolean single); }

    public RoomListCanvas(ScreenNavigator navigator, GameClient client) {
        this.navigator = navigator;
        this.client = client;
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.DEFAULT_WIDTH, SpaceInvadersApp.DEFAULT_HEIGHT);
        loadBackground();
        // 기본 포커스 전이 키 해제(TAB 처리 직접)
        setFocusTraversalKeysEnabled(false);
    }

    @Override
    public void init() {
        if (client != null) client.addListener(this);
    }

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
        requestFocusInWindow();
        if (client != null) client.requestRoomList();
    }

    @Override
    public void onHide() {
        removeKeyListener(keyAdapter);
        removeMouseListener(mouseAdapter);
        if (client != null) client.removeListener(this);
    }

    @Override
    public void update(long deltaMillis) {
        if (infoTimer > 0) { infoTimer--; if (infoTimer==0) infoMessage=""; }
        // 주기적 방 목록 새로고침 (2초)
        refreshAccumulator += deltaMillis;
        if (refreshAccumulator >= 2000) {
            refreshAccumulator = 0;
            if (client != null) client.requestRoomList();
        }
    }

    @Override
    public void render(Graphics2D g) {
        // 배경
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            g.setColor(new Color(0,0,0,120));
            g.fillRect(0,0,getWidth(),getHeight());
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0,0,getWidth(),getHeight());
        }
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        String title = "방 목록";
        FontMetrics tm = g.getFontMetrics();
        g.drawString(title, (getWidth()-tm.stringWidth(title))/2, 80);

        // 목록
        g.setFont(listFont);
        int startY = 140; int line = 40;
        // 목록 패널 영역
        int panelWidth = getWidth() - 160;
        int panelX = 80;
        int panelY = startY - 30;
    int dynamicHeight = rooms.size()*line + 60;
    int maxHeight = getHeight() - 220; // 상단/하단 여백 고려
    int panelHeight = Math.min(Math.max(200, dynamicHeight), maxHeight);
        g.setColor(new Color(0,0,0,140));
        g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16,16);
        g.setColor(Color.DARK_GRAY);
        g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 16,16);
        for (int i=0;i<rooms.size();i++) {
            RoomInfo r = rooms.get(i);
            String shortId = r.id.length()>8? r.id.substring(0,8): r.id;
            String lineText = String.format("[%s] (%s) (%d/%d)%s", shortId, r.name, r.current, r.max, r.single?" [S]":"");
            FontMetrics fm = g.getFontMetrics();
            int x = panelX + 30;
            int y = startY + i*line;
            if (i==selectedIndex && !creatingOverlay && !bottomFocus) {
                g.setColor(new Color(255,255,0,60));
                g.fillRect(panelX+10, y - fm.getAscent(), panelWidth-20, fm.getHeight());
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString(lineText, x, y);
            // 구분선
            g.setColor(new Color(255,255,255,25));
            g.drawLine(panelX+12, y+8, panelX+panelWidth-12, y+8);
        }

    // 하단 버튼 (방 생성 / 뒤로가기)
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        String b1 = "방 생성하기";
        String b2 = "뒤로가기";
        FontMetrics bm = g.getFontMetrics();
        int by = getHeight()-80;
        int b1x = getWidth()/2 - 150 - bm.stringWidth(b1)/2;
        int b2x = getWidth()/2 + 150 - bm.stringWidth(b2)/2;
    // 포커스/선택 표시
    g.setColor(bottomFocus && bottomSelected==0?Color.YELLOW:Color.WHITE);
    g.drawRect(b1x-10, by-30, bm.stringWidth(b1)+20, 40);
    g.drawString(b1, b1x, by);
    g.setColor(bottomFocus && bottomSelected==1?Color.YELLOW:Color.WHITE);
    g.drawRect(b2x-10, by-30, bm.stringWidth(b2)+20, 40);
    g.drawString(b2, b2x, by);

    // 버튼 힌트 / 조작법
    g.setFont(new Font("Arial", Font.PLAIN, 13));
    g.setColor(Color.LIGHT_GRAY);
    String hint = "방이 없다면 'C' 또는 TAB→Enter 로 생성 | ↑↓: 방 선택  Enter: 입장  R: 새로고침  C: 생성  ESC: 뒤로가기";
    FontMetrics hm = g.getFontMetrics();
    g.drawString(hint, (getWidth()-hm.stringWidth(hint))/2, getHeight()-40);

        if (infoMessage!=null && !infoMessage.isEmpty()) {
            g.setColor(Color.CYAN);
            g.drawString(infoMessage, 20, getHeight()-20);
        }

        if (creatingOverlay) {
            drawCreateOverlay(g);
        }
    }

    private void drawCreateOverlay(Graphics2D g) {
        g.setColor(new Color(0,0,0,180));
        g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(Color.WHITE);
        g.setFont(titleFont.deriveFont(28f));
        String t = "방 생성";
        FontMetrics tm = g.getFontMetrics();
        g.drawString(t, (getWidth()-tm.stringWidth(t))/2, 180);
        g.setFont(listFont);
        String[] opts = {"싱글 방", "멀티 방"};
        int baseY = 250; int line=60;
        for (int i=0;i<opts.length;i++) {
            String txt = opts[i];
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth()-fm.stringWidth(txt))/2;
            int y = baseY + i*line;
            if (i==createSelect) {
                g.setColor(new Color(255,255,0,80));
                g.fillRect(x-15, y-fm.getAscent(), fm.stringWidth(txt)+30, fm.getHeight());
                g.setColor(Color.YELLOW);
            } else g.setColor(Color.WHITE);
            g.drawString(txt, x, y);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Enter: 선택  ESC: 취소", (getWidth()-200)/2, baseY + line*2);
    }

    private void joinSelectedRoom() {
        if (rooms.isEmpty()) return;
        if (selectedIndex < 0) selectedIndex = 0;
        if (selectedIndex >= rooms.size()) selectedIndex = rooms.size()-1;
        RoomInfo r = rooms.get(selectedIndex);
        if (client != null) client.joinRoom(r.id);
    }

    private void createRoom(boolean single) {
        if (client != null) client.createRoom(single?"싱글방":"멀티방", single, single?1:4);
        creatingOverlay = false;
        infoMessage = single?"싱글 방 생성 완료":"멀티 방 생성 완료";
        infoTimer = 150;
    }

    private void backToMenu() {
        navigator.showMainMenu();
    }

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
            if (creatingOverlay) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: createSelect = Math.max(0, createSelect-1); break;
                    case KeyEvent.VK_DOWN: createSelect = Math.min(1, createSelect+1); break;
                    case KeyEvent.VK_ESCAPE: creatingOverlay = false; break;
                    case KeyEvent.VK_ENTER: createRoom(createSelect==0); break;
                }
                return;
            }
            // 하단 버튼 포커스 상태
            if (bottomFocus) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: bottomSelected = Math.max(0, bottomSelected-1); break;
                    case KeyEvent.VK_RIGHT: bottomSelected = Math.min(1, bottomSelected+1); break;
                    case KeyEvent.VK_TAB: bottomFocus = false; break;
                    case KeyEvent.VK_ESCAPE: backToMenu(); break;
                    case KeyEvent.VK_ENTER:
                        if (bottomSelected==0) { 
                            // 멀티 방만 생성
                            createRoom(false);
                            bottomFocus = false;
                        } else {
                            backToMenu();
                        }
                        break;
                }
                return;
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: selectedIndex = Math.max(0, selectedIndex-1); break;
                case KeyEvent.VK_DOWN: selectedIndex = Math.min(Math.max(0, rooms.size()-1), selectedIndex+1); break;
                case KeyEvent.VK_ENTER: joinSelectedRoom(); break;
                case KeyEvent.VK_R: if (client!=null) client.requestRoomList(); break;
                case KeyEvent.VK_C: 
                    // 멀티 방만 생성
                    createRoom(false);
                    break;
                case KeyEvent.VK_TAB: bottomFocus = true; bottomSelected = 0; selectedIndex = Math.max(0, Math.min(selectedIndex, rooms.size()-1)); break;
                case KeyEvent.VK_ESCAPE: backToMenu(); break;
            }
        }
    };

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
            if (creatingOverlay) return; // 단순화
            handleButtonClick(e.getX(), e.getY());
        }
    };

    // GameClientListener 구현
    @Override public void onRoomsUpdated(List<RoomInfo> rooms) { this.rooms.clear(); this.rooms.addAll(rooms); if (selectedIndex>=this.rooms.size()) selectedIndex = this.rooms.size()-1; if (this.rooms.isEmpty()) infoMessage="표시할 방이 없습니다."; }
    @Override public void onJoinedRoom(String roomId, String hostId) { navigator.showRoomLobby(roomId); }
    @Override public void onInfo(String msg) { infoMessage = msg; infoTimer = 180; }
    @Override public void onError(String msg) { infoMessage = "ERROR:"+msg; infoTimer = 240; }

    // ===== Helper Methods =====
    private void loadBackground() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/backgrounds/Background-0.jpg")) {
            if (is != null) backgroundImage = ImageIO.read(is);
        } catch (IOException e) {
            backgroundImage = null; // 실패 시 기본 검정
        }
    }

    private void handleButtonClick(int x, int y) {
        String b1 = "방 생성하기"; String b2 = "뒤로가기";
        FontMetrics bm = getGraphics() != null? getGraphics().getFontMetrics(new Font("Arial", Font.PLAIN, 16)) : null;
        // 안전 장치
        if (bm == null) return;
        int by = getHeight()-80;
        int b1x = getWidth()/2 - 150 - bm.stringWidth(b1)/2;
        int b2x = getWidth()/2 + 150 - bm.stringWidth(b2)/2;
        Rectangle r1 = new Rectangle(b1x-10, by-30, bm.stringWidth(b1)+20, 40);
        Rectangle r2 = new Rectangle(b2x-10, by-30, bm.stringWidth(b2)+20, 40);
        if (r1.contains(x,y)) { 
            // 멀티 방만 생성
            createRoom(false); 
            bottomFocus=false; 
        }
        else if (r2.contains(x,y)) { backToMenu(); }
    }
}
