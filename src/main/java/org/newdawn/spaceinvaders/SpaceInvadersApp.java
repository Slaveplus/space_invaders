package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.login.LoginScreenCanvas;
import org.newdawn.spaceinvaders.login.UserManager;
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

    // 공유 UserManager
    private UserManager userManager;
    
    // 스크린(캔버스)
    private LoginScreenCanvas loginScreenCanvas;
    private MainMenuCanvas mainMenuCanvas;
    private Game gameScreen; // Game 자체를 캔버스로 이용

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

        // 공유 UserManager 생성
        userManager = new UserManager();

        // 스크린 생성 (UserManager 공유)
        loginScreenCanvas = new LoginScreenCanvas(this, userManager);
        mainMenuCanvas = new MainMenuCanvas(this, userManager);
        gameScreen = new Game(this); // Game을 스크린(캔버스)으로 사용
        gameScreen.setUserManager(userManager); // Game에 UserManager 전달

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
        if (gameScreen != null) gameScreen.startNewGame();
        requestSetScreen(gameScreen);
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
