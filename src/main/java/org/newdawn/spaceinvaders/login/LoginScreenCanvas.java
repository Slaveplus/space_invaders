package org.newdawn.spaceinvaders.login;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 로그인 화면용 캔버스 래퍼
 */
public class LoginScreenCanvas extends Canvas implements Screen {
    private final LoginScreen loginScreen;
    private final ScreenNavigator navigator;

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            loginScreen.handleKeyInput(e.getKeyCode(), '\0');
            if (loginScreen.consumeLoginSuccess()) {
                navigator.showMainMenu();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            loginScreen.handleKeyInput(0, e.getKeyChar());
            if (loginScreen.consumeLoginSuccess()) {
                navigator.showMainMenu();
            }
        }
    };

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            loginScreen.handleMouseClick(e.getX(), e.getY());
            if (loginScreen.consumeLoginSuccess()) {
                navigator.showMainMenu();
            }
        }
    };

    public LoginScreenCanvas(ScreenNavigator navigator) {
        this.navigator = navigator;
        this.loginScreen = new LoginScreen();
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.WIDTH, SpaceInvadersApp.HEIGHT);
        setFocusable(true);
    }

    @Override
    public void init() {
        // 로그인 화면 진입 시 이전 상태/세션 초기화
        if (loginScreen != null) {
            loginScreen.reset();
            if (loginScreen.getUserManager() != null) {
                // 이전 로그인 세션이 남아 자동으로 메인메뉴로 리다이렉트되는 문제 방지
                loginScreen.getUserManager().logoutUser();
            }
        }
    }

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
        requestFocusInWindow();
    }

    @Override
    public void onHide() {
        removeKeyListener(keyAdapter);
        removeMouseListener(mouseAdapter);
    }

    @Override
    public void update(long deltaMillis) {
        loginScreen.update();
    }

    @Override
    public void render(Graphics2D g) {
        loginScreen.draw(g);
    }
}
