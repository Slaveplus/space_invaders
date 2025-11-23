package org.newdawn.spaceinvaders.room;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;

/** 단순 방 로비 화면: 참가자 목록, 준비, 채팅 */
public class RoomLobbyCanvas extends Canvas implements Screen, GameClientListener {
    private transient final ScreenNavigator navigator;
    private transient final GameClient client;
    private final String roomId; // 표시용 (client 내 상태와 동일)
    private static final String FONT_NAME = "Arial";
    private Font titleFont = new Font(FONT_NAME, Font.BOLD, 32);
    private Font listFont = new Font(FONT_NAME, Font.BOLD, 18);
    private Font chatFont = new Font("Monospaced", Font.PLAIN, 14);

    private transient List<PlayerInfo> players = new ArrayList<>();
    private boolean isHost = false; // hostId는 isHost 계산에 직접 필요없어 제거

    // 채팅
    private final Deque<String> chatLines = new ArrayDeque<>();
    private String chatInput = "";
    private boolean chatFocus = false; // Enter 로 포커스 토글
    private int infoTimer = 0;
    private String infoMessage = "";
    private long statePollAccumulator = 0; // ms 누적
    private boolean launchedMultiplayerGame = false;

    private transient BufferedImage backgroundImage;
    private String lastChatComposite = null; // 중복 방지 키(from+msg)

    public RoomLobbyCanvas(ScreenNavigator navigator, GameClient client, String roomId) {
        this.navigator = navigator;
        this.client = client;
        this.roomId = roomId;
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.DEFAULT_WIDTH, SpaceInvadersApp.DEFAULT_HEIGHT);
        loadBackground();
    }

    private void loadBackground() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/backgrounds/Background-1.jpg")) {
            if (is != null) backgroundImage = ImageIO.read(is);
        } catch (IOException e) {
            backgroundImage = null;
        }
    }

    @Override
    public void init() {
        if (client != null) client.addListener(this);
    }

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        requestFocusInWindow();
        launchedMultiplayerGame = false;
        appendChatInfo("방에 입장했습니다.");
    }

    @Override
    public void onHide() {
        removeKeyListener(keyAdapter);
        if (client != null) client.removeListener(this);
    }

	@Override
	public void update(long deltaMillis) {
		if (launchedMultiplayerGame) {
			return;
		}
		if (infoTimer > 0) { infoTimer--; if (infoTimer==0) infoMessage=""; }
		statePollAccumulator += deltaMillis;
		if (statePollAccumulator >= 1500) { // 1.5초 폴링
			statePollAccumulator = 0;
			if (client != null) client.requestRoomState();
        }
    }

    private void drawBackground(Graphics2D g) {
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            g.setColor(new Color(0,0,0,140));
            g.fillRect(0,0,getWidth(),getHeight());
        } else {
            g.setColor(Color.BLACK); g.fillRect(0,0,getWidth(),getHeight());
        }
    }

    private void drawTitle(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        String title = "방 로비";
        FontMetrics tm = g.getFontMetrics();
        g.drawString(title, (getWidth()-tm.stringWidth(title))/2, 60);
        g.setFont(listFont);
        String rid = "Room ID: "+roomId;
        g.drawString(rid, 40, 100);
    }

    private void drawSinglePlayerUI(Graphics2D g) {
        String btn = "게임 시작 (Enter)";
        g.setFont(listFont.deriveFont(24f));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(btn)+40;
        int h = 60;
        int x = (getWidth()-w)/2;
        int y = (getHeight()-h)/2;
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(x, y, w, h, 20,20);
        g.setColor(Color.YELLOW);
        g.drawRoundRect(x, y, w, h, 20,20);
        g.drawString(btn, x + (w - fm.stringWidth(btn))/2, y + (h + fm.getAscent())/2 - 8);
        if (infoMessage!=null && !infoMessage.isEmpty()) {
            g.setColor(Color.ORANGE);
            g.drawString(infoMessage, 40, getHeight()-40);
        }
    }

    private void drawPlayerList(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(x, y, w, h, 16,16);
        g.setColor(Color.DARK_GRAY);
        g.drawRoundRect(x, y, w, h, 16,16);
        g.setColor(Color.WHITE);
        // 플레이어 목록
        int listY = y + 40; int line=30;
        g.drawString("플레이어 목록", x + 20, y + 25);
        String mySid = client != null ? client.getSessionId() : null;
        for (int i=0;i<players.size();i++) {
            PlayerInfo p = players.get(i);
            String mark = p.host?"[HOST] ":"";
            String ready = p.ready?"(READY)":"(.....)";
            // 세션ID가 내 것과 같으면 * 표시
            String self = (mySid != null && p.id.equals(mySid)) ? " ← You" : "";
            g.setColor(p.ready?Color.GREEN:Color.LIGHT_GRAY);
            g.drawString(mark+p.username+" "+ready+self, x + 30, listY + i*line);
            g.setColor(new Color(255,255,255,24));
            g.drawLine(x + 25, listY + i*line + 6, x + w - 30, listY + i*line + 6);
        }
    }

    private void drawHelp(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 13));
        int helpY = getHeight()-110;
        g.drawString("Enter/T: 채팅 포커스/전송  R: 준비 토글  S: 시작(호스트)", 40, helpY);
        g.drawString("ESC: 나가기  메시지 입력시 ESC로 취소", 40, helpY + 18);
    }

    private void drawChat(Graphics2D g, int x, int y, int w, int h) {
        // 채팅 영역
    g.setColor(new Color(30,30,30,180));
    g.fillRoundRect(x, y, w, h, 16,16);
    g.setColor(Color.DARK_GRAY);
    g.drawRoundRect(x, y, w, h, 16,16);
        g.setFont(chatFont);
        int lineH = 18;
        int maxLines = (h-20)/lineH;
        Object[] arr = chatLines.toArray();
        int start = Math.max(0, arr.length - maxLines);
        for (int i=start;i<arr.length;i++) {
            String lineText = (String)arr[i];
            g.setColor(lineText.startsWith("* ")?Color.CYAN:Color.WHITE);
            g.drawString(lineText, x+10, y+20 + (i-start)*lineH);
        }

        // 채팅 입력 박스
    int inputY = y + h + 20;
    g.setColor(new Color(0,0,0,180));
    g.fillRoundRect(x, inputY, w, 34, 12,12);
    g.setColor(chatFocus?Color.YELLOW:Color.GRAY);
    g.drawRoundRect(x, inputY, w, 34, 12,12);
        g.setColor(Color.WHITE);
        String disp = chatInput.isEmpty() && chatFocus ? "메시지 입력..." : chatInput;
        g.drawString(disp, x+10, inputY+20);
    }

    private void drawMultiplayerUI(Graphics2D g) {
        // 패널 레이아웃 (반응형)
    int margin = 30;
    int availableW = getWidth() - margin*2;
    int leftPanelW = (int)(availableW * 0.52); // 목록 패널 52%
    leftPanelW = Math.min(leftPanelW, 520);
    int rightPanelW = Math.min(400, getWidth() - margin*3 - leftPanelW);
    int leftPanelX = margin;
    int leftPanelY = 110;
    int rightPanelX = leftPanelX + leftPanelW + margin;
    int rightPanelY = 110;
    int leftPanelH = getHeight() - 260;
    int rightPanelH = Math.min(getHeight() - 260, 420);
        
        drawPlayerList(g, leftPanelX, leftPanelY, leftPanelW, leftPanelH);
        drawHelp(g);
        drawChat(g, rightPanelX, rightPanelY, rightPanelW, rightPanelH);

        if (infoMessage!=null && !infoMessage.isEmpty()) {
            g.setColor(Color.ORANGE);
            g.drawString(infoMessage, 40, getHeight()-40);
        }
    }

    @Override
    public void render(Graphics2D g) {
        drawBackground(g);
        drawTitle(g);

        if (client != null && client.isCurrentRoomSingle()) {
            drawSinglePlayerUI(g);
            return;
        }

        drawMultiplayerUI(g);
    }


    private void appendChatLine(String line) {
        if (line==null) return;
        if (chatLines.size()>200) chatLines.pollFirst();
        chatLines.addLast(line);
    }
    private void appendChatInfo(String msg) { appendChatLine("* "+msg); }

    private void sendChat() {
        if (client==null) return;
        String msg = chatInput.trim();
        if (!msg.isEmpty()) client.chat(msg);
        chatInput = "";
    }

    private void leaveRoom() {
        if (client!=null) client.leaveRoom();
        navigator.showRoomList(client); // 방 나가면 목록 복귀
    }

    private void toggleReady() { if (client!=null) client.toggleReady(); }
    private void startGame() { if (client!=null && isHost) client.startGame(); }

    private transient final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
            // 싱글방 최소 UI 모드
            if (client != null && client.isCurrentRoomSingle()) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_S:
                        startGame();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        leaveRoom();
                        break;
                    default: // do nothing
                        break;
                }
                return;
            }
            if (chatFocus) {
                if (e.getKeyCode()==KeyEvent.VK_ESCAPE) { chatFocus=false; return; }
                if (e.getKeyCode()==KeyEvent.VK_ENTER) { sendChat(); chatFocus=false; return; }
                if (e.getKeyCode()==KeyEvent.VK_BACK_SPACE) { if (!chatInput.isEmpty()) chatInput=chatInput.substring(0,chatInput.length()-1); return; }
                // 제한된 문자만
                char c = e.getKeyChar();
                if (c>=32 && c<127) { chatInput += c; }
                return;
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_T:
                    chatFocus = true; break;
                case KeyEvent.VK_R: toggleReady(); break;
                case KeyEvent.VK_S: if (isHost) startGame(); break;
                case KeyEvent.VK_ESCAPE: leaveRoom(); break;
                default: // do nothing
                    break;
            }
        }
    };

    // GameClientListener 구현
    @Override public void onRoomState(String roomId, String hostId, List<PlayerInfo> players) {
        this.players = new ArrayList<>(players);
        this.isHost = hostId != null && client!=null && hostId.equals(client.getSelfId());
    }
    @Override public void onChatMessage(String from, String msg) {
        String key = from+"\u0000"+msg;
        if (key.equals(lastChatComposite)) return; // 중복 방지
        lastChatComposite = key;
        appendChatLine(from+": "+msg);
    }
    @Override public void onHostLeft(String roomId) { appendChatInfo("호스트가 방을 제거했습니다."); infoMessage="호스트 종료 - 목록으로"; infoTimer=180; navigator.showRoomList(client); }
    @Override public void onGameInit(GameInitInfo info) {
        if (!launchedMultiplayerGame) {
            launchedMultiplayerGame = true;
            navigator.startMultiplayerGame(client, info);
        }
    }

    @Override public void onGameStart(String roomId) { appendChatInfo("게임 시작!"); }
    @Override public void onInfo(String msg) { if ("NO_ROOM".equals(msg)) return; infoMessage=msg; infoTimer=180; }
    @Override public void onError(String msg) { infoMessage="ERROR:"+msg; infoTimer=240; }
}
