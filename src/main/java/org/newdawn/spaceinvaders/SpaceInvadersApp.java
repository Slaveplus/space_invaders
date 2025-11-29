package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.ResolutionManager;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameCanvas;
import org.newdawn.spaceinvaders.multyplay.net.client.RoomGameNetworkAdapter;
import org.newdawn.spaceinvaders.room.GameInitInfo;
import org.newdawn.spaceinvaders.login.LoginScreenCanvas;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.mainmenu.MainMenuCanvas;
import org.newdawn.spaceinvaders.room.GameClient;
import org.newdawn.spaceinvaders.room.RoomListCanvas;
import org.newdawn.spaceinvaders.room.RoomLobbyCanvas;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.io.InputStream;

/**
 * 애플리케이션 프레임. 창, 메인 루프, 화면 전환을 관리합니다.
 */
public class SpaceInvadersApp extends JFrame implements ScreenNavigator {
    // 기본 해상도 설정
    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;
    
    // 현재 해상도 (동적으로 변경 가능)
    private int currentWidth = DEFAULT_WIDTH;
    private int currentHeight = DEFAULT_HEIGHT;

    private Canvas canvas;              // 현재 화면이 부착되는 캔버스
    private transient BufferStrategy strategy;    // 더블버퍼

    // 공유 UserManager
    private transient UserManager userManager;
    
    // 해상도 관리자
    private transient ResolutionManager resolutionManager;
    
    // 스크린(캔버스)
    private LoginScreenCanvas loginScreenCanvas;
    private MainMenuCanvas mainMenuCanvas;
    private RoomListCanvas roomListCanvas; // 동적 생성 (접속 후)
    private RoomLobbyCanvas roomLobbyCanvas; // 현재 로비
    private transient GameClient currentClient; // 현재 GameClient 참조
    private Game gameScreen; // Game 자체를 캔버스로 이용
    private MultiplayerGameCanvas multiplayerGameCanvas;
    private transient RoomGameNetworkAdapter multiplayerNetworkAdapter;

    private transient Screen currentScreen; // update/render 가상화
    private volatile boolean running = true;
    private volatile Canvas pendingScreen; // 전환 요청된 다음 화면

    private static final String WINDOW_TITLE = "Space Invaders";

    private void handlePendingScreenTransition() {
        if (pendingScreen != null) {
            Canvas next = pendingScreen;
            pendingScreen = null;
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> setScreen(next));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFpsCounter(long delta) {
        lastFpsTime += delta;
        fps++;

        if (lastFpsTime >= 1000) {
            this.setTitle(WINDOW_TITLE+" (FPS: "+fps+")");
            lastFpsTime = 0;
            fps = 0;
        }
    }

    private void updateCurrentScreen(long delta) {
        if (currentScreen != null) {
            currentScreen.update(delta);
            handleGameScreenTransitions();
            handleMultiplayerGameCanvasTransitions();
        }
    }

    private void handleGameScreenTransitions() {
        if (currentScreen == gameScreen && gameScreen.isRequestingMainMenu()) {
            setScreen(mainMenuCanvas);
            gameScreen.resetMainMenuRequest();
        }
    }

    private void handleMultiplayerGameCanvasTransitions() {
        if (currentScreen == multiplayerGameCanvas && multiplayerGameCanvas != null
                && multiplayerGameCanvas.isRequestingLobbyReturn()) {
            multiplayerGameCanvas.resetLobbyReturnRequest();
            if (multiplayerNetworkAdapter != null && currentClient != null) {
                currentClient.removeListener(multiplayerNetworkAdapter);
            }
            if (multiplayerNetworkAdapter != null) {
                multiplayerNetworkAdapter.shutdown();
                multiplayerNetworkAdapter = null;
            }
            if (currentClient != null) {
                currentClient.leaveRoom();
                showRoomList(currentClient);
            } else {
                showMainMenu();
            }
            multiplayerGameCanvas.shutdownNetwork();
            multiplayerGameCanvas = null;
        }
    }

    private boolean ensureBufferStrategy() {
        if (strategy == null || !canvas.isDisplayable()) {
            if (canvas != null && canvas.isDisplayable()) {
                canvas.createBufferStrategy(2);
                strategy = canvas.getBufferStrategy();
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean renderFrame() {
        if (!canvas.isDisplayable()) {
            SystemTimer.sleep(10);
            return false;
        }

        Graphics2D g = null;
        try {
            g = (Graphics2D) strategy.getDrawGraphics();
        } catch (IllegalStateException e) {
            strategy = null;
            SystemTimer.sleep(10);
            return false;
        }
        try {
            g.setColor(Color.black);
            g.fillRect(0, 0, currentWidth, currentHeight);
            if (currentScreen != null) currentScreen.render(g);
        } finally {
            if (g != null) g.dispose();
        }
        if (strategy != null) {
            try {
                strategy.show();
            } catch (IllegalStateException ise) {
                strategy = null;
                return false;
            }
        }
        return true;
    }

    /** 프레임 레이트를 마지막으로 기록한 시간 */
    private long lastFpsTime;
    /** 현재까지 기록된 프레임 수 */
    private int fps;

    public SpaceInvadersApp() {
        super(WINDOW_TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIgnoreRepaint(true);
        setResizable(true); // 창 크기 조절 가능하도록 변경

        JPanel panel = (JPanel) getContentPane();
        panel.setPreferredSize(new Dimension(currentWidth, currentHeight));
        panel.setLayout(null);

        // 기본 캔버스 생성 (실제 화면 캔버스로 교체됨)
        canvas = new Canvas();
        canvas.setBounds(0, 0, currentWidth, currentHeight);
        canvas.setIgnoreRepaint(true);
        panel.add(canvas);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();

        // 해상도 관리자 초기화
        resolutionManager = new ResolutionManager();
        resolutionManager.setResolution(currentWidth, currentHeight);

        // 공유 UserManager 생성
        userManager = new UserManager();

        // 스크린 생성 (UserManager 공유)
        loginScreenCanvas = new LoginScreenCanvas(this, userManager);
        mainMenuCanvas = new MainMenuCanvas(this, userManager);
        gameScreen = new Game(this); // Game을 스크린(캔버스)으로 사용, ScreenNavigator 전달
        gameScreen.setUserManager(userManager); // Game에 UserManager 전달
        gameScreen.setResolutionManager(resolutionManager); // Game에 ResolutionManager 전달

        // 초기 화면
        setScreen(loginScreenCanvas);
    }

    /** 다음 화면 전환을 요청 (렌더 루프가 EDT에서 안전하게 처리) */
    private void requestSetScreen(Canvas next) {
        pendingScreen = next;
    }

    private void setScreen(Canvas newCanvas) {
        // 현재 화면 제거
        if (canvas != null) {
            getContentPane().remove(canvas);
            if (currentScreen != null) currentScreen.onHide();
        }

        // 새 화면 추가
        canvas = newCanvas;
        canvas.setBounds(0, 0, currentWidth, currentHeight);
        canvas.setIgnoreRepaint(true);
        getContentPane().add(canvas);
        canvas.setVisible(true);
        canvas.setFocusable(true);
        getContentPane().revalidate();
        getContentPane().repaint();

        // 화면 전환 시 버퍼 전략 무효화
        strategy = null;

        if (newCanvas instanceof Screen) {
            currentScreen = (Screen) newCanvas;
            currentScreen.init();
            currentScreen.onShow();
            // 포커스는 리스너 등록 이후 안정적으로 요청
            javax.swing.SwingUtilities.invokeLater(() -> canvas.requestFocusInWindow());
        } else {
            currentScreen = null;
        }
    }

    public void runMainLoop() {
        long lastLoopTime = SystemTimer.getTime();
        while (running) {
            handlePendingScreenTransition();

            long delta = SystemTimer.getTime() - lastLoopTime;
            lastLoopTime = SystemTimer.getTime();

            updateFpsCounter(delta);
            updateCurrentScreen(delta);

            if (!ensureBufferStrategy()) {
                SystemTimer.sleep(10);
                continue;
            }

            if (!renderFrame()) {
                continue;
            }

            SystemTimer.sleep(1);
        }
        dispose();
    }

    // ============== ScreenNavigator 구현 ==============
    @Override
    public void showLogin() { requestSetScreen(loginScreenCanvas); }

    @Override
    public void showMainMenu() { requestSetScreen(mainMenuCanvas); }

    @Override
    public void startNewGame() {
        // 게임 시작 전에 멀티플레이 클라이언트가 살아있다면 정리 (싱글게임 전환시 세션 종료)
        cleanupClient();
        if (gameScreen != null) gameScreen.startNewGame();
        requestSetScreen(gameScreen);
    }

    @Override
    public void exitGame() {
        running = false;
    }

    /** 현재 GameClient 및 관련 화면 정리 */
    private void cleanupClient() {
        if (roomListCanvas != null && currentClient != null) {
            currentClient.removeListener(roomListCanvas);
        }
        if (roomLobbyCanvas != null && currentClient != null) {
            currentClient.removeListener(roomLobbyCanvas);
        }
        if (multiplayerNetworkAdapter != null && currentClient != null) {
            currentClient.removeListener(multiplayerNetworkAdapter);
        }
        if (multiplayerGameCanvas != null) {
            multiplayerGameCanvas.shutdownNetwork();
            multiplayerGameCanvas = null;
        }
        if (multiplayerNetworkAdapter != null) {
            multiplayerNetworkAdapter.shutdown();
            multiplayerNetworkAdapter = null;
        }
        if (currentClient != null) {
            currentClient.shutdown();
            currentClient = null;
        }
        roomListCanvas = null;
        roomLobbyCanvas = null;
    }
    
    @Override
    public void setResolution(int width, int height) {
        changeResolution(width, height);
    }

    // ===== 멀티플레이 네비게이션 구현 =====
    @Override
    public void showRoomList(GameClient client) {
        // 기존 클라이언트 리스너 정리 필요시 처리
        if (client != currentClient) {
            // 다른 세션으로 전환 시 이전 세션 종료
            cleanupClient();
            this.currentClient = client;
        }
        if (roomListCanvas == null || roomListCanvas.getWidth() != currentWidth) {
            roomListCanvas = new RoomListCanvas(this, client);
            roomListCanvas.init();
        }
        requestSetScreen(roomListCanvas);
    }

	@Override
	public void showRoomLobby(String roomId) {
		if (currentClient == null) {
			// 예외 상황: 클라이언트 없으면 목록으로
			showRoomList(null);
			return;
		}
		roomLobbyCanvas = new RoomLobbyCanvas(this, currentClient, roomId);
		roomLobbyCanvas.init();
		requestSetScreen(roomLobbyCanvas);
	}
	
    private String resolveSelfId(GameClient client, GameInitInfo initInfo) {
        String resolvedSelfId = client.getSelfId();
        if ((resolvedSelfId == null || resolvedSelfId.isEmpty()) && initInfo.players != null) {
            String username = client.getUsername();
            if (username != null) {
                for (GameInitInfo.Player p : initInfo.players) {
                    if (p != null && username.equals(p.username)) {
                        resolvedSelfId = p.id;
                        break;
                    }
                }
            }
            if ((resolvedSelfId == null || resolvedSelfId.isEmpty()) && initInfo.players.size() == 1) {
                GameInitInfo.Player only = initInfo.players.get(0);
                resolvedSelfId = only != null ? only.id : null;
            }
        }
        return resolvedSelfId;
    }

	@Override
	public void startMultiplayerGame(GameClient client, GameInitInfo initInfo) {
		if (client == null || initInfo == null) {
			return;
		}
		currentClient = client;
		if (roomLobbyCanvas != null) {
			client.removeListener(roomLobbyCanvas);
			roomLobbyCanvas = null;
		}
		if (multiplayerNetworkAdapter != null) {
			multiplayerNetworkAdapter.shutdown();
		}
        multiplayerNetworkAdapter = new RoomGameNetworkAdapter(client);
        multiplayerNetworkAdapter.onGameInit(initInfo);
		
        multiplayerGameCanvas = new MultiplayerGameCanvas();
        multiplayerGameCanvas.setUserManager(userManager);
        multiplayerGameCanvas.setResolutionManager(resolutionManager);
        String resolvedSelfId = resolveSelfId(client, initInfo);
        multiplayerGameCanvas.configureForRemote(multiplayerNetworkAdapter,
                resolvedSelfId, initInfo);
		requestSetScreen(multiplayerGameCanvas);
	}
    
    // 해상도 변경 메서드들
    public void changeResolution(int width, int height) {
        this.currentWidth = width;
        this.currentHeight = height;
        
        // 해상도 관리자 업데이트
        if (resolutionManager != null) {
            resolutionManager.setResolution(width, height);
        }
        
        // 창 크기 업데이트
        JPanel panel = (JPanel) getContentPane();
        panel.setPreferredSize(new Dimension(width, height));
        
        // 캔버스 크기 업데이트
        canvas.setBounds(0, 0, width, height);
        
        // Game 화면 크기도 업데이트
        if (gameScreen != null) {
            gameScreen.setBounds(0, 0, width, height);  
        }
        
        // MultiplayerGameCanvas 해상도도 업데이트
        if (multiplayerGameCanvas != null) {
            multiplayerGameCanvas.setBounds(0, 0, width, height);
            multiplayerGameCanvas.setResolutionManager(resolutionManager);
        }
        
        // 창 크기 재조정
        pack();
        setLocationRelativeTo(null);
        
        // 버퍼 전략 재생성
        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();
    }
    
    public int getCurrentWidth() {
        return currentWidth;
    }
    
    public int getCurrentHeight() {
        return currentHeight;
    }
    
    @Override
    public ResolutionManager getResolutionManager() {
        return resolutionManager;
    }
    
    // 미리 정의된 해상도 옵션들
    public static final int[][] RESOLUTION_OPTIONS = {
        {800, 600},   // 기본
        {1024, 768},  // 일반
        {1280, 720},  // HD
        {1366, 768},  // 노트북
        {1920, 1080}  // Full HD
    };
    
    public static String[] getResolutionNames() {
        return new String[]{
            "800x600 (기본)",
            "1024x768 (일반)",
            "1280x720 (HD)",
            "1366x768 (노트북)",
            "1920x1080 (Full HD)"
        };
    }

    public static void main(String[] args) {
        registerFont();
        SpaceInvadersApp app = new SpaceInvadersApp();
        app.runMainLoop();
    }

    private static void registerFont() {
        try {
            InputStream fontStream = SpaceInvadersApp.class.getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                Font kostarFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(kostarFont);
                fontStream.close();
            } else {
                System.err.println("Kostar 폰트를 로드할 수 없습니다.");
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 중 오류 발생: " + e.getMessage());
        }
    }
}
