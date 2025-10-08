package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import java.util.ArrayList;

/**
 * 게임 상태 관리 클래스 (게임플레이 전용)
 * - 화면 전환(로그인/메뉴)은 SpaceInvadersApp에서 전담합니다.
 * - 본 클래스는 게임 내 상태(라운드/스탯/오버레이/엔티티/타이밍)만 관리합니다.
 */
public class GameStateManager {
    // 게임플레이 상태
    private boolean gameRunning = true;
    private boolean waitingForKeyPress = false; // 게임 시작 시 바로 시작되도록 false로 변경
    private String message = "";

    // 라운드 정보
    private int currentRound = 1;
    private final int MAX_ROUND = 8; // 1-4라운드: Near + Boss, 5라운드: Boss only
    private int alienCount;

    // 플레이어 스탯
    private int attackPower = 10;
    private double attackSpeed = 1.0;
    private int maxHP = 10;
    private int currentHP = 10;
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
    private final long baseAlienFiringInterval = 1500;
    private final long firingInterval = 500;

    // FPS 카운터 (필요 시 외부에서 사용 가능하도록 유지)
    private long lastFpsTime;
    private int fps;

    // 엔티티 관리
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final ArrayList<Entity> removeList = new ArrayList<>();

    // 게임 로직
    private boolean logicRequiredThisLoop = false;
    
    // 라운드 전환 상태 구분
    private boolean isRoundTransition = false;
    
    public GameStateManager() {
        System.out.println("GameStateManager 초기화 완료");
    }
    
    
    // ========== 게임플레이 상태 관리 (GameState.java에서 통합) ==========
    
    /**
     * 게임 시작 시 초기화
     */
    public void startNewGame() {
        System.out.println("🎮 GameStateManager.startNewGame() called!");
        
        // Reset player stats
        attackPower = 10;
        attackSpeed = 1.0;
        maxHP = 10;
        currentHP = 10;
        skillPoints = 0;
        
        // Reset round
        currentRound = 1;
        System.out.println("🎮 GameStateManager: Set currentRound = " + currentRound);
        
        // Reset alien firing interval
        alienFiringInterval = baseAlienFiringInterval;
        
        // Clear entities
        entities.clear();
        removeList.clear();
        
        // Reset game state
        waitingForKeyPress = false;
        message = "";
        logicRequiredThisLoop = false;
        isRoundTransition = false;
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
    
    public boolean isRoundTransition() { return isRoundTransition; }
    public void setRoundTransition(boolean roundTransition) { this.isRoundTransition = roundTransition; }
    
    /**
     * 플레이어 데미지 처리
     */
    public void takeDamage() {
        currentHP--;
        if (currentHP <= 0) {
            message = "Oh no! They got you, try again?";
            waitingForKeyPress = true;
            isRoundTransition = false; // 게임 오버 시 라운드 전환 플래그 해제
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
            isRoundTransition = true; // 라운드 전환 플래그 설정
            
            // Update alien firing interval for new round
            alienFiringInterval = Math.max(300, baseAlienFiringInterval - (currentRound * 300));
            
            return true; // Round advanced
        } else {
            // Game completed
            message = "축하합니다! 모든 라운드를 클리어했습니다!";
            waitingForKeyPress = true;
            isRoundTransition = false; // 게임 완료
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
