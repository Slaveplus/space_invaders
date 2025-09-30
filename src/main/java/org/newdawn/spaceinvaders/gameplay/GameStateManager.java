package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.login.LoginScreen;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.mainmenu.MainMenu;

import java.util.ArrayList;

/**
 * 게임 상태 관리 클래스
 * 로그인, 메뉴, 게임플레이 간의 전환을 관리합니다
 */
public class GameStateManager {
    /** 현재 게임 상태 */
    public enum GameState {
        LOGIN,      // 로그인 화면
        MENU,       // 메인 메뉴
        GAMEPLAY,   // 게임플레이
        SKILL_MENU  // 스킬 메뉴
    }
    
    private GameState currentState = GameState.LOGIN;
    private LoginScreen loginScreen;
    private MainMenu mainMenu;
    private UserManager userManager;
    
    // 게임플레이 상태 (GameState.java에서 통합)
    private boolean gameRunning = true;
    private boolean waitingForKeyPress = true;
    private String message = "";
    
    // 라운드 정보
    private int currentRound = 1;
    private final int MAX_ROUND = 5;
    private int alienCount;
    
    // 플레이어 스탯
    private int attackPower = 1;
    private double attackSpeed = 1.0;
    private int maxHP = 3;
    private int currentHP = 3;
    private int skillPoints = 0;
    
    // 스킬 메뉴 관련
    private boolean showingSkillMenu = false;
    private int selectedSkill = 0; // 0: Attack Power, 1: Attack Speed, 2: HP Up
    
    // 일시정지 메뉴 관련
    private boolean showingPauseMenu = false;
    private int selectedPauseMenuItem = 0; // 0: 계속하기, 1: 메인메뉴, 2: 설정
    
    // 게임 타이밍
    private long lastFire = 0;
    private long lastAlienFire = 0;
    private long alienFiringInterval = 1500;
    private long baseAlienFiringInterval = 1500;
    private long firingInterval = 500;
    
    // FPS 카운터
    private long lastFpsTime;
    private int fps;
    
    // 엔티티 관리
    private ArrayList<Entity> entities = new ArrayList<>();
    private ArrayList<Entity> removeList = new ArrayList<>();
    
    // 게임 로직
    private boolean logicRequiredThisLoop = false;
    
    public GameStateManager() {
        // 로그인 화면 초기화
        loginScreen = new LoginScreen();
        
        // 메인 메뉴 초기화
        mainMenu = new MainMenu();
        
        // UserManager 동기화
        mainMenu.setUserManager(loginScreen.getUserManager());
        
        System.out.println("GameStateManager 초기화 완료");
    }
    
    /**
     * 현재 상태 반환
     */
    public GameState getCurrentState() {
        return currentState;
    }
    
    /**
     * 로그인 화면으로 전환
     */
    public void showLogin() {
        currentState = GameState.LOGIN;
        loginScreen.reset();
        System.out.println("로그인 화면으로 전환");
    }
    
    /**
     * 메인 메뉴로 전환
     */
    public void showMenu() {
        currentState = GameState.MENU;
        mainMenu.reset();
        System.out.println("메인 메뉴로 전환");
    }
    
    /**
     * 게임플레이로 전환
     */
    public void showGameplay() {
        currentState = GameState.GAMEPLAY;
        System.out.println("게임플레이로 전환");
    }
    
    /**
     * 로그인 성공 처리
     */
    public void handleLoginSuccess() {
        // MainMenu의 UserManager를 LoginScreen의 UserManager로 완전히 교체
        mainMenu.setUserManager(loginScreen.getUserManager());
        showMenu();
        System.out.println("로그인 성공, 메인 메뉴로 이동");
    }
    
    /**
     * 로그아웃 처리
     */
    public void handleLogout() {
        // LoginScreen의 UserManager도 로그아웃 처리 (사용자 데이터는 DB에 저장됨)
        loginScreen.getUserManager().logoutUser();
        // LoginScreen 입력 필드만 초기화 (사용자 데이터는 유지)
        loginScreen.reset();
        // 메뉴 상태 완전 초기화
        mainMenu.reset();
        showLogin();
        System.out.println("로그아웃 요청, 로그인 화면으로 이동 - 사용자 데이터는 DB에 보존됨");
    }
    
    /**
     * 게임 시작 처리
     */
    public void handleGameStart() {
        showGameplay();
        System.out.println("게임 시작");
    }
    
    /**
     * 게임 종료 처리 (메뉴로 돌아가기)
     */
    public void handleGameEnd() {
        showMenu();
        System.out.println("게임 종료, 메뉴로 돌아가기");
    }
    
    /**
     * 현재 화면 업데이트
     */
    public void update() {
        switch (currentState) {
            case LOGIN:
                loginScreen.update();
                break;
            case MENU:
                mainMenu.update();
                break;
            case GAMEPLAY:
                // 게임플레이는 Game 클래스에서 처리
                break;
            case SKILL_MENU:
                // 스킬 메뉴는 입력으로만 처리
                break;
        }
    }
    
    /**
     * 현재 화면 그리기
     */
    public void draw(java.awt.Graphics2D g2d) {
        switch (currentState) {
            case LOGIN:
                loginScreen.draw(g2d);
                break;
            case MENU:
                mainMenu.draw(g2d);
                break;
            case GAMEPLAY:
                // 게임플레이는 Game 클래스에서 처리
                break;
            case SKILL_MENU:
                // 스킬 메뉴는 Game 클래스에서 처리
                break;
        }
    }
    
    /**
     * 키 입력 처리
     */
    public void handleKeyInput(int keyCode, char keyChar) {
        switch (currentState) {
            case LOGIN:
                loginScreen.handleKeyInput(keyCode, keyChar);
                // 로그인 성공 시 메인 메뉴로 이동
                if (loginScreen.getUserManager().isLoggedIn()) {
                    handleLoginSuccess();
                }
                break;
            case MENU:
                mainMenu.handleKeyInput(keyCode);
                // 새게임 시작 요청이 있으면 게임 시작
                if (mainMenu.shouldStartGame()) {
                    handleGameStart();
                }
                // 로그아웃 요청이 있으면 로그인 화면으로 돌아가기
                if (mainMenu.isLogoutRequested()) {
                    handleLogout();
                }
                break;
            case GAMEPLAY:
                // 게임플레이 입력은 Game 클래스에서 처리
                break;
            case SKILL_MENU:
                // 스킬 메뉴 입력은 Game 클래스에서 처리
                break;
        }
    }
    
    /**
     * 마우스 클릭 처리
     */
    public void handleMouseClick(int x, int y) {
        switch (currentState) {
            case LOGIN:
                loginScreen.handleMouseClick(x, y);
                // 로그인 성공 시 메인 메뉴로 이동
                if (loginScreen.getUserManager().isLoggedIn()) {
                    handleLoginSuccess();
                }
                break;
            case MENU:
                mainMenu.handleMouseClick(x, y);
                // 새게임 시작 요청이 있으면 게임 시작
                if (mainMenu.shouldStartGame()) {
                    handleGameStart();
                }
                // 로그아웃 요청이 있으면 로그인 화면으로 돌아가기
                if (mainMenu.isLogoutRequested()) {
                    handleLogout();
                }
                break;
            case GAMEPLAY:
                // 게임플레이 마우스는 Game 클래스에서 처리
                System.out.println("게임 중 마우스 클릭: (" + x + ", " + y + ")");
                break;
            case SKILL_MENU:
                // 스킬 메뉴 마우스는 Game 클래스에서 처리
                break;
        }
    }
    
    /**
     * 로그인 화면 표시 여부
     */
    public boolean isShowingLogin() {
        return currentState == GameState.LOGIN;
    }
    
    /**
     * 메뉴 표시 여부
     */
    public boolean isShowingMenu() {
        return currentState == GameState.MENU;
    }
    
    /**
     * 게임플레이 중 여부
     */
    public boolean isGameplay() {
        return currentState == GameState.GAMEPLAY;
    }
    
    /**
     * UserManager 반환
     */
    public UserManager getUserManager() {
        return loginScreen.getUserManager();
    }
    
    // ========== 게임플레이 상태 관리 (GameState.java에서 통합) ==========
    
    /**
     * 게임 시작 시 초기화
     */
    public void startNewGame() {
        // Reset player stats
        attackPower = 1;
        attackSpeed = 1.0;
        maxHP = 3;
        currentHP = 3;
        skillPoints = 0;
        
        // Reset round
        currentRound = 1;
        
        // Reset alien firing interval
        alienFiringInterval = baseAlienFiringInterval;
        
        // Clear entities
        entities.clear();
        removeList.clear();
        
        // Reset game state
        waitingForKeyPress = false;
        message = "";
        logicRequiredThisLoop = false;
    }
    
    // Getters and Setters for gameplay state
    public boolean isGameRunning() { return gameRunning; }
    public void setGameRunning(boolean gameRunning) { this.gameRunning = gameRunning; }
    
    public boolean isWaitingForKeyPress() { return waitingForKeyPress; }
    public void setWaitingForKeyPress(boolean waitingForKeyPress) { this.waitingForKeyPress = waitingForKeyPress; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    
    public int getMaxRound() { return MAX_ROUND; }
    
    public int getAlienCount() { return alienCount; }
    public void setAlienCount(int alienCount) { this.alienCount = alienCount; }
    
    public int getAttackPower() { return attackPower; }
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }
    
    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }
    
    public int getMaxHP() { return maxHP; }
    public void setMaxHP(int maxHP) { this.maxHP = maxHP; }
    
    public int getCurrentHP() { return currentHP; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }
    
    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }
    
    public long getLastFire() { return lastFire; }
    public void setLastFire(long lastFire) { this.lastFire = lastFire; }
    
    public long getLastAlienFire() { return lastAlienFire; }
    public void setLastAlienFire(long lastAlienFire) { this.lastAlienFire = lastAlienFire; }
    
    public long getAlienFiringInterval() { return alienFiringInterval; }
    public void setAlienFiringInterval(long alienFiringInterval) { this.alienFiringInterval = alienFiringInterval; }
    
    public long getBaseAlienFiringInterval() { return baseAlienFiringInterval; }
    
    public long getFiringInterval() { return firingInterval; }
    
    public long getLastFpsTime() { return lastFpsTime; }
    public void setLastFpsTime(long lastFpsTime) { this.lastFpsTime = lastFpsTime; }
    
    public int getFps() { return fps; }
    public void setFps(int fps) { this.fps = fps; }
    
    public ArrayList<Entity> getEntities() { return entities; }
    public ArrayList<Entity> getRemoveList() { return removeList; }
    
    public boolean isLogicRequiredThisLoop() { return logicRequiredThisLoop; }
    public void setLogicRequiredThisLoop(boolean logicRequiredThisLoop) { this.logicRequiredThisLoop = logicRequiredThisLoop; }
    
    /**
     * 플레이어 데미지 처리
     */
    public void takeDamage() {
        currentHP--;
        if (currentHP <= 0) {
            message = "Oh no! They got you, try again?";
            waitingForKeyPress = true;
        }
    }
    
    /**
     * 라운드 클리어 처리
     */
    public boolean advanceRound() {
        if (currentRound < MAX_ROUND) {
            currentRound++;
            message = "라운드 " + currentRound + " 시작! 준비하세요!";
            waitingForKeyPress = true;
            
            // Update alien firing interval for new round
            alienFiringInterval = Math.max(300, baseAlienFiringInterval - (currentRound * 300));
            
            return true; // Round advanced
        } else {
            // Game completed
            message = "축하합니다! 모든 라운드를 클리어했습니다!";
            waitingForKeyPress = true;
            return false; // Game completed
        }
    }
    
    /**
     * 스킬 포인트 추가
     */
    public void addSkillPoints(int points) {
        skillPoints += points;
    }
    
    // ========== 스킬 메뉴 관련 메서드 ==========
    
    /**
     * 스킬 메뉴 표시 여부 확인
     */
    public boolean isShowingSkillMenu() {
        return showingSkillMenu;
    }
    
    /**
     * 스킬 메뉴 표시
     */
    public void showSkillMenu() {
        showingSkillMenu = true;
        // 게임플레이 상태는 유지하고 오버레이로 표시
    }
    
    /**
     * 스킬 메뉴 숨기기
     */
    public void hideSkillMenu() {
        showingSkillMenu = false;
        // 게임플레이 상태 유지
    }
    
    /**
     * 선택된 스킬 인덱스 가져오기
     */
    public int getSelectedSkill() {
        return selectedSkill;
    }
    
    /**
     * 선택된 스킬 인덱스 설정
     */
    public void setSelectedSkill(int skill) {
        selectedSkill = skill;
    }
    
    // ========== 일시정지 메뉴 관련 메서드 ==========
    
    /**
     * 일시정지 메뉴 표시 여부 확인
     */
    public boolean isShowingPauseMenu() {
        return showingPauseMenu;
    }
    
    /**
     * 일시정지 메뉴 표시
     */
    public void showPauseMenu() {
        showingPauseMenu = true;
    }
    
    /**
     * 일시정지 메뉴 숨기기
     */
    public void hidePauseMenu() {
        showingPauseMenu = false;
    }
    
    /**
     * 선택된 일시정지 메뉴 아이템 가져오기
     */
    public int getSelectedPauseMenuItem() {
        return selectedPauseMenuItem;
    }
    
    /**
     * 선택된 일시정지 메뉴 아이템 설정
     */
    public void setSelectedPauseMenuItem(int item) {
        selectedPauseMenuItem = item;
    }
}
