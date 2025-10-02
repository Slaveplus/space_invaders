package org.newdawn.spaceinvaders.mainmenu;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.login.UserManager;

/**
 * 메인 메뉴 화면용 캔버스 래퍼
 */
public class MainMenuCanvas extends Canvas implements Screen {
    private final MainMenu mainMenu;
    private final ScreenNavigator navigator;

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            // ESC 키는 메인 메뉴에서 뒤로가기 용도로만 사용 (게임 종료 안함)
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

    public MainMenuCanvas(ScreenNavigator navigator, UserManager userManager) {
        this.navigator = navigator;
        this.mainMenu = new MainMenu(userManager, navigator);
        setIgnoreRepaint(true);
        setBackground(Color.black);
        setSize(SpaceInvadersApp.DEFAULT_WIDTH, SpaceInvadersApp.DEFAULT_HEIGHT);
        setFocusable(true);
    }

    @Override
    public void onShow() {
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
        requestFocusInWindow();
        
        // 메인 메뉴 진입 시 인벤토리 새로고침 (로그인 후 데이터 보존)
        if (mainMenu != null) {
            mainMenu.refreshInventory();
        }
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
