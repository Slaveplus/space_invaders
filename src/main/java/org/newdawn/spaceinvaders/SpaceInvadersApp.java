package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.multiplay.ConnectCanvas;
import org.newdawn.spaceinvaders.multiplay.RoomListCanvas;
import org.newdawn.spaceinvaders.multiplay.LobbyCanvas;
import org.newdawn.spaceinvaders.multiplay.MultiplayerClient;
// CoopGameScreen 제거: Game을 네트 클라이언트 화면으로 공용 사용
import org.newdawn.spaceinvaders.login.LoginScreenCanvas;
import org.newdawn.spaceinvaders.mainmenu.MainMenuCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

/**
 * 애플리케이션 프레임. 창, 메인 루프, 화면 전환을 관리합니다.
 */
public class SpaceInvadersApp extends JFrame implements ScreenNavigator {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private Canvas canvas;              // 현재 화면이 부착되는 캔버스
    private BufferStrategy strategy;    // 더블버퍼

    // 스크린(캔버스)
    private LoginScreenCanvas loginScreenCanvas;
    private MainMenuCanvas mainMenuCanvas;
    // 레거시 싱글 게임 화면 제거: 싱글도 네트워크 파이프라인 사용
    // 멀티플레이 화면들
    private Canvas mpConnectCanvas;
    private Canvas mpRoomListCanvas;
    private Canvas mpLobbyCanvas;
    // 멀티 전투 화면(같은 화면에서 여러 플레이어 렌더)
    // 멀티 전투 화면은 Game을 네트 모드로 재사용
    private final MultiplayerClient multiplayerClient = new MultiplayerClient();
    // 싱글도 동일 파이프라인 사용을 위한 로컬 임베디드 클라이언트
    private MultiplayerClient localClient = new MultiplayerClient();

    private Screen currentScreen; // update/render 가상화
    private volatile boolean running = true;
    private volatile Canvas pendingScreen; // 전환 요청된 다음 화면

    private static final String windowTitle = "Space Invaders";

    // message, waitingForKeyPress, and logicRequiredThisLoop are now managed by GameStateManager

    /** The last time at which we recorded the frame rate */
    private long lastFpsTime;
    /** The current number of frames recorded */
    private int fps;

    public SpaceInvadersApp() {
        super(windowTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIgnoreRepaint(true);
        setResizable(false);

        JPanel panel = (JPanel) getContentPane();
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        panel.setLayout(null);

        // 기본 캔버스 생성 (실제 화면 캔버스로 교체됨)
        canvas = new Canvas();
        canvas.setBounds(0, 0, WIDTH, HEIGHT);
        canvas.setIgnoreRepaint(true);
        panel.add(canvas);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();

    // 스크린 생성
        loginScreenCanvas = new LoginScreenCanvas(this);
        mainMenuCanvas = new MainMenuCanvas(this);
    // 레거시 싱글 화면 생성 제거
        // 멀티플레이 화면 생성
    mpConnectCanvas = new ConnectCanvas(this, multiplayerClient);
    mpRoomListCanvas = new RoomListCanvas(this, multiplayerClient);
    mpLobbyCanvas = new LobbyCanvas(this, multiplayerClient);
    // 멀티플레이 전투(같은 화면에서 여러 플레이어): 서버 스냅샷 기반 캔버스
    // 멀티 전투 화면은 Game을 MultiplayerClient로 감싼 인스턴스로 사용

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
        canvas.setBounds(0, 0, WIDTH, HEIGHT);
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
            // 화면 전환 요청이 있으면 먼저 처리 (EDT 동기)
            if (pendingScreen != null) {
                Canvas next = pendingScreen;
                pendingScreen = null;
                try {
                    javax.swing.SwingUtilities.invokeAndWait(() -> setScreen(next));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 전환 직후 다음 루프에서 버퍼 재생성/렌더 진행
            }

            long delta = SystemTimer.getTime() - lastLoopTime;
            lastLoopTime = SystemTimer.getTime();

            // update the frame counter
            lastFpsTime += delta;
            fps++;

            // update our FPS counter if a second has passed since
            // we last recorded
            if (lastFpsTime >= 1000) {
                this.setTitle(windowTitle+" (FPS: "+fps+")");
                lastFpsTime = 0;
                fps = 0;
            }

            if (currentScreen != null) {
                currentScreen.update(delta);
            }

            // strategy가 null이거나 캔버스가 displayable 상태가 아니면 버퍼 전략 재생성
            if (strategy == null || !canvas.isDisplayable()) {
                if (canvas != null && canvas.isDisplayable()) {
                    canvas.createBufferStrategy(2);
                    strategy = canvas.getBufferStrategy();
                } else {
                    SystemTimer.sleep(10);
                    continue;
                }
            }

            // drawGraphics 얻기 전에 displayable 상태 재확인
            if (!canvas.isDisplayable()) {
                SystemTimer.sleep(10);
                continue;
            }

            Graphics2D g = null;
            try {
                g = (Graphics2D) strategy.getDrawGraphics();
            } catch (IllegalStateException e) {
                // 버퍼 전략이 유효하지 않으면 무효화하고 다음 루프에서 재생성
                strategy = null;
                SystemTimer.sleep(10);
                continue;
            }
            try {
                g.setColor(Color.black);
                g.fillRect(0, 0, WIDTH, HEIGHT);
                if (currentScreen != null) currentScreen.render(g);
            } finally {
                if (g != null) g.dispose();
            }
            if (strategy != null) {
                try {
                    strategy.show();
                } catch (IllegalStateException ise) {
                    // 전환 타이밍 등으로 peer가 무효화된 경우 다음 루프에서 재시도
                    strategy = null;
                }
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
        // 이전 로컬 세션 종료
        if (localClient != null) {
            try { localClient.disconnect(); } catch (Exception ignored) {}
        }
        // 로컬 임베디드 모드로 '서버를 띄운 척' 하고, 같은 네트 파이프라인으로 플레이
        localClient = new MultiplayerClient();
        localClient.enableLocalMode();
        localClient.createRoom("single");
        localClient.joinRoom("local-room");
        localClient.ready();
        localClient.start();
        requestSetScreen(new Game(this, localClient, true));
    }

    @Override
    public void showMultiplayerConnect() { requestSetScreen(mpConnectCanvas); }

    @Override
    public void showMultiplayerRoomList() { requestSetScreen(mpRoomListCanvas); }

    @Override
    public void showMultiplayerLobby() { requestSetScreen(mpLobbyCanvas); }

    @Override
    public void startMultiplayerGame() {
        // 같은 화면에서 여러 플레이어를 렌더링: Game을 네트 모드로 생성해 전환
        Canvas netGame = new Game(this, multiplayerClient, true);
        requestSetScreen(netGame);
    }

    @Override
    public void exitGame() {
        running = false;
    }

    public static void main(String[] args) {
        SpaceInvadersApp app = new SpaceInvadersApp();
        app.runMainLoop();
    }
}
