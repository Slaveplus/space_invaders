package org.newdawn.spaceinvaders.mainmenu;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;

/**
 * 메인 메뉴 화면용 캔버스 래퍼
 */
public class MainMenuCanvas extends Canvas implements Screen {
    private final MainMenu mainMenu;
    private final ScreenNavigator navigator;

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                navigator.exitGame();
                return;
            }
            mainMenu.handleKeyInput(e.getKeyCode());
            if (mainMenu.shouldStartGame()) {
                mainMenu.reset();
                navigator.startNewGame();
            }
            if (mainMenu.isLogoutRequested()) {
                mainMenu.resetLogoutRequest();
                navigator.showLogin();
            }
        }
    };

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            mainMenu.handleMouseClick(e.getX(), e.getY());
            if (mainMenu.shouldStartGame()) {
                mainMenu.reset();
                navigator.startNewGame();
            }
            if (mainMenu.isLogoutRequested()) {
                mainMenu.resetLogoutRequest();
                navigator.showLogin();
            }
        }
    };

    public MainMenuCanvas(ScreenNavigator navigator) {
        this.navigator = navigator;
        this.mainMenu = new MainMenu();
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.WIDTH, SpaceInvadersApp.HEIGHT);
    }

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
        requestFocus();
    }

    @Override
    public void onHide() {
        removeKeyListener(keyAdapter);
        removeMouseListener(mouseAdapter);
    }

    @Override
    public void update(long deltaMillis) {
        mainMenu.update();
    }

    @Override
    public void render(Graphics2D g) {
        mainMenu.draw(g);
    }
}
