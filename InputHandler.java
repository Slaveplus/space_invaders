package org.newdawn.spaceinvaders;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 입력 처리를 담당하는 클래스
 * 키보드 입력을 받아서 적절한 액션으로 변환
 */
public class InputHandler extends KeyAdapter {
    private Game game;
    private MenuManager menuManager;
    private SkillManager skillManager;
    private GameState gameState;
    
    // 키 입력 상태
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean firePressed = false;
    
    // 키 입력 카운터
    private int pressCount = 1;
    
    public InputHandler(Game game, MenuManager menuManager, SkillManager skillManager, GameState gameState) {
        this.game = game;
        this.menuManager = menuManager;
        this.skillManager = skillManager;
        this.gameState = gameState;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        // Handle pause menu navigation
        if (menuManager.isShowPauseMenu()) {
            handlePauseMenuInput(e);
            return;
        }
        
        // Handle skill menu navigation
        if (menuManager.isShowSkillMenu()) {
            handleSkillMenuInput(e);
            return;
        }
        
        // Handle main menu navigation
        if (menuManager.isShowMenu()) {
            handleMainMenuInput(e);
            return;
        }
        
        // Handle ESC key for pause menu (only during gameplay)
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            menuManager.setShowPauseMenu(true);
            return;
        }
        
        // Handle Q key for skill menu
        if (e.getKeyCode() == KeyEvent.VK_Q) {
            menuManager.setShowSkillMenu(true);
            return;
        }
        
        // Handle skill activation keys
        if (e.getKeyCode() == KeyEvent.VK_1) {
            skillManager.extendSkill(0, 5); // 5 seconds invincible
            return;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_2) {
            skillManager.extendSkill(1, 10); // 10 seconds piercing
            return;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_3) {
            skillManager.extendSkill(2, 8); // 8 seconds triple shot
            return;
        }
        
        // if we're waiting for an "any key" typed then we don't 
        // want to do anything with just a "press"
        if (gameState.isWaitingForKeyPress()) {
            return;
        }
        
        // Handle movement and firing
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            firePressed = true;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // if we're waiting for an "any key" typed then we don't 
        // want to do anything with just a "released"
        if (gameState.isWaitingForKeyPress()) {
            return;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            firePressed = false;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // if we're waiting for a "any key" type then
        // check if we've received any recently
        if (gameState.isWaitingForKeyPress()) {
            if (pressCount == 1) {
                // since we've now received our key typed
                // event we can mark it as such and continue
                gameState.setWaitingForKeyPress(false);
                pressCount = 0;
            } else {
                pressCount++;
            }
        }
    }
    
    /**
     * 일시정지 메뉴 입력 처리
     */
    private void handlePauseMenuInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            int current = menuManager.getSelectedPauseMenuItem();
            menuManager.setSelectedPauseMenuItem((current - 1 + menuManager.getPauseMenuItems()) % menuManager.getPauseMenuItems());
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            int current = menuManager.getSelectedPauseMenuItem();
            menuManager.setSelectedPauseMenuItem((current + 1) % menuManager.getPauseMenuItems());
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
            handlePauseMenuSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            menuManager.setShowPauseMenu(false);
        }
    }
    
    /**
     * 스킬 메뉴 입력 처리
     */
    private void handleSkillMenuInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            int current = menuManager.getSelectedSkill();
            menuManager.setSelectedSkill((current - 1 + menuManager.getSkillCount()) % menuManager.getSkillCount());
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            int current = menuManager.getSelectedSkill();
            menuManager.setSelectedSkill((current + 1) % menuManager.getSkillCount());
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
            handleSkillSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_Q) {
            menuManager.setShowSkillMenu(false);
        }
    }
    
    /**
     * 메인 메뉴 입력 처리
     */
    private void handleMainMenuInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            int current = menuManager.getSelectedMenuItem();
            menuManager.setSelectedMenuItem((current - 1 + menuManager.getMenuItems()) % menuManager.getMenuItems());
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            int current = menuManager.getSelectedMenuItem();
            menuManager.setSelectedMenuItem((current + 1) % menuManager.getMenuItems());
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
            handleMenuSelection();
        }
    }
    
    /**
     * 일시정지 메뉴 선택 처리
     */
    private void handlePauseMenuSelection() {
        switch (menuManager.getSelectedPauseMenuItem()) {
            case 0: // CONTINUE
                menuManager.setShowPauseMenu(false);
                break;
            case 1: // MAIN MENU
                menuManager.setShowPauseMenu(false);
                menuManager.setShowMenu(true);
                break;
            case 2: // SETTINGS
                gameState.setMessage("Settings feature coming soon!");
                menuManager.setShowPauseMenu(false);
                gameState.setWaitingForKeyPress(true);
                break;
        }
    }
    
    /**
     * 스킬 선택 처리
     */
    private void handleSkillSelection() {
        String result = skillManager.handleSkillSelection(
            menuManager.getSelectedSkill(),
            gameState.getSkillPoints(),
            gameState.getAttackPower(),
            gameState.getAttackSpeed(),
            gameState.getMaxHP()
        );
        
        if (result.startsWith("스킬 포인트가 부족")) {
            gameState.setMessage(result);
            gameState.setWaitingForKeyPress(true);
            return;
        }
        
        // Apply skill upgrade
        switch (menuManager.getSelectedSkill()) {
            case 0: // Attack Power
                gameState.setAttackPower(gameState.getAttackPower() + 1);
                gameState.setSkillPoints(gameState.getSkillPoints() - skillManager.getAttackPowerCost());
                break;
            case 1: // Attack Speed
                gameState.setAttackSpeed(gameState.getAttackSpeed() + 0.2);
                gameState.setSkillPoints(gameState.getSkillPoints() - skillManager.getAttackSpeedCost());
                break;
            case 2: // HP Up & Heal
                gameState.setMaxHP(gameState.getMaxHP() + 3);
                gameState.setCurrentHP(gameState.getMaxHP()); // Also heal to full
                gameState.setSkillPoints(gameState.getSkillPoints() - skillManager.getHpUpCost());
                break;
        }
        
        gameState.setMessage(result);
        gameState.setWaitingForKeyPress(true);
    }
    
    /**
     * 메인 메뉴 선택 처리
     */
    private void handleMenuSelection() {
        switch (menuManager.getSelectedMenuItem()) {
            case 0: // NEW GAME
                game.startNewGame();
                menuManager.setShowMenu(false);
                gameState.setWaitingForKeyPress(false);
                break;
            case 1: // LEADERBOARD
                gameState.setMessage("Leaderboard feature coming soon!");
                gameState.setWaitingForKeyPress(true);
                break;
            case 2: // SETTINGS
                gameState.setMessage("Settings feature coming soon!");
                gameState.setWaitingForKeyPress(true);
                break;
        }
    }
    
    // Getters for movement state
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }
    public boolean isFirePressed() { return firePressed; }
}