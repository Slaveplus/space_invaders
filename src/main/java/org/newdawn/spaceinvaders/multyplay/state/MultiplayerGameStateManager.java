package org.newdawn.spaceinvaders.multyplay.state;

import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import java.util.ArrayList;

/**
 * 게임 상태 관리 클래스 (게임플레이 전용)
 * - 화면 전환(로그인/메뉴)은 SpaceInvadersApp에서 전담합니다.
 * - 본 클래스는 게임 내 상태(라운드/스탯/오버레이/엔티티/타이밍)만 관리합니다.
 */
public class MultiplayerGameStateManager {
    // ====== 기본 게임플레이 상태 ======
    private boolean gameRunning = true;
    private boolean waitingForKeyPress = true;
    private String message = "";

    // ====== 라운드 정보 ======
    private int currentRound = 1;
    private final int MAX_ROUND = 8;
    private int alienCount;

    // ====== 멀티플레이 지원: 플레이어 상태 집합 ======
    // key: playerId (세션 또는 로컬 식별자)
    private final java.util.Map<String, PlayerState> players = new java.util.LinkedHashMap<>();
    // 현재 로컬 플레이어 (싱글 플레이일 경우 하나만 존재)
    private String localPlayerId = "local"; // 기본값

    // 레거시 호환을 위한 단일 플레이어 접근 (기존 코드 점진 전환 용)
    private PlayerState getLocalPlayerState() {
        return players.computeIfAbsent(localPlayerId, PlayerState::new);
    }

    // ====== 스킬/오버레이 상태 ======
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

    private int earnedCoins = 0;
    private long gameStartTime = 0;
    private long pausedTime = 0;
    private long pauseStartTime = 0;

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
    
    public MultiplayerGameStateManager() {
        System.out.println("MultiplayerGameStateManager 초기화 완료");
    }
    
    
    // ========== 게임플레이 상태 관리 (GameState.java에서 통합) ==========
    
    /**
     * 게임 시작 시 초기화
     */
    public void startNewGame() {
        if (players.isEmpty()) {
            getLocalPlayerState().resetForNewGame();
        } else {
            for (PlayerState state : players.values()) {
                state.resetForNewGame();
            }
            if (localPlayerId != null && !players.containsKey(localPlayerId)) {
                players.put(localPlayerId, new PlayerState(localPlayerId));
                players.get(localPlayerId).resetForNewGame();
            }
        }

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
        isRoundTransition = false;

        earnedCoins = 0;
        gameStartTime = System.currentTimeMillis();
        pausedTime = 0;
        pauseStartTime = 0;
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
    
    // ---- 레거시 단일 플레이어 getter/setter (점진 제거 예정) ----
    public int getAttackPower() { return getLocalPlayerState().getAttackPower(); }
    public void setAttackPower(int v) { getLocalPlayerState().setAttackPower(v); }

    public double getAttackSpeed() { return getLocalPlayerState().getAttackSpeed(); }
    public void setAttackSpeed(double v) { getLocalPlayerState().setAttackSpeed(v); }

    public int getMaxHP() { return getLocalPlayerState().getMaxHP(); }
    public void setMaxHP(int v) { getLocalPlayerState().setMaxHP(v); }

    public int getCurrentHP() { return getLocalPlayerState().getCurrentHP(); }
    public void setCurrentHP(int v) { getLocalPlayerState().setCurrentHP(v); }

    public int getSkillPoints() { return getLocalPlayerState().getSkillPoints(); }
    public void setSkillPoints(int v) { getLocalPlayerState().setSkillPoints(v); }

    // ---- 멀티플레이 전용 API ----
    public java.util.Collection<PlayerState> getPlayerStates() { return players.values(); }
    public PlayerState getPlayerState(String playerId) { return players.get(playerId); }
    public PlayerState ensurePlayer(String playerId) { return players.computeIfAbsent(playerId, PlayerState::new); }
    public String getLocalPlayerId() { return localPlayerId; }
    public void setLocalPlayerId(String localPlayerId) { this.localPlayerId = localPlayerId; }

    public void addCoins(String playerId, int amount) {
        if (amount == 0) {
            return;
        }
        if (playerId == null) {
            if (localPlayerId != null) {
                ensurePlayer(localPlayerId).addCoins(amount);
                earnedCoins += amount;
            }
            return;
        }
        ensurePlayer(playerId).addCoins(amount);
        if (playerId.equals(localPlayerId)) {
            earnedCoins += amount;
        }
    }

    public void addEarnedCoins(int amount) {
        if (localPlayerId != null) {
            getLocalPlayerState().addCoins(amount);
        }
        earnedCoins += amount;
    }

    public int getEarnedCoins() {
        return earnedCoins;
    }

    public int getEarnedCoins(String playerId) {
        PlayerState ps = players.get(playerId);
        return ps != null ? ps.getEarnedCoins() : 0;
    }

    public void setEarnedCoins(int amount) {
        this.earnedCoins = amount;
        if (localPlayerId != null) {
            ensurePlayer(localPlayerId).setEarnedCoins(amount);
        }
    }
    
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

    public long getGameStartTime() { return gameStartTime; }
    public void setGameStartTime(long gameStartTime) { this.gameStartTime = gameStartTime; }

    public String getPlayTime() {
        if (gameStartTime == 0) {
            return "00:00";
        }

        long currentTime = System.currentTimeMillis();
        long playTimeMs = currentTime - gameStartTime;

        if (pauseStartTime > 0) {
            playTimeMs -= (currentTime - pauseStartTime);
        }

        playTimeMs -= pausedTime;

        long totalSeconds = playTimeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public long getPlayTimeMs() {
        if (gameStartTime == 0) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long playTimeMs = currentTime - gameStartTime;

        if (pauseStartTime > 0) {
            playTimeMs -= (currentTime - pauseStartTime);
        }

        playTimeMs -= pausedTime;

        return playTimeMs;
    }
    
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
        takeDamage(localPlayerId);
    }

    public void takeDamage(String playerId) {
        if (playerId == null) {
            return;
        }
        PlayerState ps = ensurePlayer(playerId);
        ps.takeDamage(1);
        if (playerId.equals(localPlayerId) && ps.isDead()) {
            message = "Oh no! They got you, try again?";
            waitingForKeyPress = true;
            isRoundTransition = false;
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
        getLocalPlayerState().addSkillPoints(points);
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
