package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.util.Logger;
import org.newdawn.spaceinvaders.common.util.LoggerFactory;
import java.util.ArrayList;

/**
 * 게임 상태 관리 클래스 (게임플레이 전용)
 * - 화면 전환(로그인/메뉴)은 SpaceInvadersApp에서 전담합니다.
 * - 본 클래스는 게임 내 상태(라운드/스탯/오버레이/엔티티/타이밍)만 관리합니다.
 */
public class GameStateManager {
    // ====== 기본 게임플레이 상태 ======
    private boolean gameRunning = true;
    private boolean waitingForKeyPress = false; // 게임 시작 시 바로 시작되도록 false로 변경
    private String message = "";

    // ====== 라운드 정보 ======
    private int currentRound = 1;
    private final int MAX_ROUND = 8; // 1-4라운드: Near + Boss, 5라운드: Boss only
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
    
    // 라운드 설명 창 관련
    private boolean showingRoundInfo = false;
    private long roundInfoStartTime = 0; // 라운드 정보 창 시작 시간
    
    // 그만두기 확인 창 관련
    private boolean showingQuitConfirm = false;
    private int selectedQuitOption = 0; // 0: 아니요, 1: 예
    
    // 게임 클리어 상태
    private boolean gameCompleted = false;

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
    
    // 코인 및 시간 추적
    private int earnedCoins = 0; // 게임 중 획득한 코인
    private long gameStartTime = 0; // 게임 시작 시간
    private long pausedTime = 0; // 일시정지된 시간 (누적)
    private long pauseStartTime = 0; // 현재 일시정지 시작 시간
    // 디버그 오버레이 플래그
    private boolean debugDrawHitboxes = false;
    
    /** 로거 */
    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);
    
    public GameStateManager() {
        logger.debug("GameStateManager 초기화 완료");
    }

    public boolean isDebugDrawHitboxes() {
        return debugDrawHitboxes;
    }

    public void toggleDebugDrawHitboxes() {
        debugDrawHitboxes = !debugDrawHitboxes;
    }
    
    
    // ========== 게임플레이 상태 관리 (GameState.java에서 통합) ==========
    
    /**
     * 게임 시작 시 초기화
     */
    public void startNewGame() {
        logger.debug("GameStateManager.startNewGame() called!");

        // 모든 플레이어 상태 초기화 (현재는 로컬 플레이어만 존재)
        players.clear();
        PlayerState local = getLocalPlayerState();
        local.resetForNewGame();
        local.setAttackPower(10);
        local.setAttackSpeed(1.0);
        local.setMaxHP(10);
        local.setCurrentHP(10);
        local.setSkillPoints(0);

        // Reset round
        currentRound = 1;
        logger.debug("GameStateManager: Set currentRound = " + currentRound);
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
        
        // Reset coin and time tracking
        earnedCoins = 0;
        gameStartTime = System.currentTimeMillis();
        pausedTime = 0; // 누적 일시정지 시간 초기화
        pauseStartTime = 0; // 현재 일시정지 시작 시간 초기화
        logger.debug("Game started at: " + gameStartTime);
    }
    
    /**
     * 게임 상태 초기화 (메인메뉴로 돌아갈 때 사용)
     */
    public void resetGame() {
        logger.debug("GameStateManager.resetGame() called!");
        
        players.clear();
        
        // Reset game state
        waitingForKeyPress = false;
        message = "";
        logicRequiredThisLoop = false;
        isRoundTransition = false;
        gameCompleted = false;
        
        // Reset UI states
        showingSkillMenu = false;
        showingPauseMenu = false;
        showingRoundInfo = false;
        showingQuitConfirm = false;
        
        // Reset coin and time tracking
        earnedCoins = 0;
        gameStartTime = 0;
        pausedTime = 0;
        pauseStartTime = 0;
        
        // Clear entities
        entities.clear();
        removeList.clear();
        
        logger.debug("Game state reset completed");
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
        PlayerState ps = getLocalPlayerState();
        ps.takeDamage(1);
        if (ps.isDead()) {
            message = "아쉬워요 .... 다시 시작!!";
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
            
            // 라운드 설명 창 표시
            showingRoundInfo = true;
            roundInfoStartTime = System.currentTimeMillis(); // 타이머 시작
            
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
    
    // ========== 코인 및 시간 추적 관련 메서드 ==========
    
    /**
     * 획득한 코인 수 반환
     */
    public int getEarnedCoins() {
        return earnedCoins;
    }
    
    /**
     * 획득한 코인 수 설정
     */
    public void setEarnedCoins(int earnedCoins) {
        this.earnedCoins = earnedCoins;
    }
    
    /**
     * 코인 추가
     */
    public void addEarnedCoins(int amount) {
        this.earnedCoins += amount;
        logger.debug("Earned coins: " + amount + " (Total: " + this.earnedCoins + ")");
    }
    
    /**
     * 게임 시작 시간 반환
     */
    public long getGameStartTime() {
        return gameStartTime;
    }
    
    /**
     * 게임 시작 시간 설정
     */
    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }
    
    /**
     * 플레이 시간을 분:초 형식으로 반환
     */
    public String getPlayTime() {
        if (gameStartTime == 0) {
            return "00:00";
        }
        
        long currentTime = System.currentTimeMillis();
        long playTimeMs = currentTime - gameStartTime;
        
        // 현재 일시정지 중이면 일시정지 시간도 제외
        if (pauseStartTime > 0) {
            playTimeMs -= (currentTime - pauseStartTime);
        }
        
        // 누적된 일시정지 시간 제외
        playTimeMs -= pausedTime;
        
        long totalSeconds = playTimeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * 플레이 시간을 밀리초로 반환
     */
    public long getPlayTimeMs() {
        if (gameStartTime == 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long playTimeMs = currentTime - gameStartTime;
        
        // 현재 일시정지 중이면 일시정지 시간도 제외
        if (pauseStartTime > 0) {
            playTimeMs -= (currentTime - pauseStartTime);
        }
        
        // 누적된 일시정지 시간 제외
        playTimeMs -= pausedTime;
        
        return playTimeMs;
    }
    
    // ========== 라운드 설명 창 관련 메서드 ==========
    
    /**
     * 라운드 설명 창 표시 여부 확인
     */
    public boolean isShowingRoundInfo() {
        return showingRoundInfo;
    }
    
    /**
     * 라운드 설명 창 표시
     */
    public void showRoundInfo() {
        showingRoundInfo = true;
    }
    
    /**
     * 라운드 설명 창 숨기기
     */
    public void hideRoundInfo() {
        showingRoundInfo = false;
        roundInfoStartTime = 0; // 타이머 리셋
    }
    
    /**
     * 라운드별 설명 텍스트 반환
     */
    public String getRoundDescription(int round) {
        switch (round) {
            case 1:
                return "6마리의 몬스터를 처치하세요!\n적 처치 시 코인과 스킬 획득 가능";
            case 2:
                return "강력한 보스와의 첫 번째 대결!\n다양한 공격 패턴에 주의하세요~~";
            case 3:
                return "더 강해진 몬스터들!\n공격이 더 강해질수있어요~~";
            case 4:
                return "두 번째 보스와의 대결!\n더욱 강력한 공격 패턴 주의";
            case 5:
                return "갑자기 철들은 몬스터들!\n이건 좀 힘들듯";
            case 6:
                return "세 번째 보스와의 대결!\n가장 강력한 공격 패턴";
            case 7:
                return "최강의 몬스터들!\n모든 스킬을 활용하세요";
            case 8:
                return "최종 보스와의 마지막 대결!\n승리하면 게임 클리어!";
            default:
                return "이게 뭐지,, 왜 이렇게 나오지??";
        }
    }
    
    // ========== 그만두기 확인 창 관련 메서드 ==========
    
    /**
     * 그만두기 확인 창 표시 여부 확인
     */
    public boolean isShowingQuitConfirm() {
        return showingQuitConfirm;
    }
    
    /**
     * 그만두기 확인 창 표시
     */
    public void showQuitConfirm() {
        showingQuitConfirm = true;
        selectedQuitOption = 0; // 기본값: 아니요
    }
    
    /**
     * 그만두기 확인 창 숨기기
     */
    public void hideQuitConfirm() {
        showingQuitConfirm = false;
    }
    
    /**
     * 선택된 그만두기 옵션 반환
     */
    public int getSelectedQuitOption() {
        return selectedQuitOption;
    }
    
    /**
     * 선택된 그만두기 옵션 설정
     */
    public void setSelectedQuitOption(int option) {
        selectedQuitOption = option;
    }
    
    /**
     * 일시정지 시작
     */
    public void startPause() {
        if ((showingPauseMenu || showingSkillMenu || showingRoundInfo || showingQuitConfirm) && pauseStartTime == 0) {
            pauseStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 일시정지 종료
     */
    public void endPause() {
        if (pauseStartTime > 0) {
            pausedTime += System.currentTimeMillis() - pauseStartTime;
            pauseStartTime = 0;
        }
    }
    
    /**
     * 일시정지 중인지 확인
     */
    public boolean isPaused() {
        return showingPauseMenu || showingSkillMenu || showingRoundInfo || showingQuitConfirm || gameCompleted;
    }
    
    // ========== 게임 클리어 상태 관련 메서드 ==========
    
    /**
     * 게임 클리어 상태 확인
     */
    public boolean isGameCompleted() {
        return gameCompleted;
    }
    
    /**
     * 게임 클리어 상태 설정
     */
    public void setGameCompleted(boolean completed) {
        this.gameCompleted = completed;
    }
    
    /**
     * 라운드 정보 창이 5초 이상 표시되었는지 확인
     */
    public boolean shouldAutoCloseRoundInfo() {
        if (!showingRoundInfo || roundInfoStartTime == 0) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - roundInfoStartTime) >= 5000; // 5초 이후에 자동으로 닫힘
    }
    
    // ========== 게임플레이 활성 상태 체크 공통 메서드 ==========
    
    /**
     * 게임플레이가 활성화되어 있는지 확인
     * (모든 메뉴가 닫혀있고, 키 입력 대기 상태가 아닐 때)
     * 
     * @return 게임플레이 활성화 여부
     */
    public boolean isGameplayActive() {
        return !waitingForKeyPress &&
               !showingPauseMenu &&
               !showingSkillMenu &&
               !showingRoundInfo &&
               !showingQuitConfirm;
    }
    
    /**
     * 우주선 제어가 가능한지 확인
     * (일시정지 메뉴, 스킬 메뉴, 라운드 정보, 종료 확인 창이 모두 닫혀있을 때)
     * 
     * @return 우주선 제어 가능 여부
     */
    public boolean isShipControlActive() {
        return !showingPauseMenu &&
               !showingSkillMenu &&
               !showingRoundInfo &&
               !showingQuitConfirm;
    }
    
    /**
     * 충돌 체크가 가능한지 확인
     * (일시정지 메뉴, 스킬 메뉴, 라운드 정보, 종료 확인 창이 모두 닫혀있을 때)
     * 
     * @return 충돌 체크 가능 여부
     */
    public boolean isCollisionCheckActive() {
        return !showingPauseMenu &&
               !showingSkillMenu &&
               !showingRoundInfo &&
               !showingQuitConfirm;
    }
    
    /**
     * 데미지 적용이 가능한지 확인
     * (키 입력 대기, 일시정지 메뉴, 스킬 메뉴, 라운드 정보, 종료 확인 창이 모두 닫혀있을 때)
     * 
     * @return 데미지 적용 가능 여부
     */
    public boolean isDamageApplicable() {
        return !waitingForKeyPress &&
               !showingPauseMenu &&
               !showingSkillMenu &&
               !showingRoundInfo &&
               !showingQuitConfirm;
    }
    
    /**
     * 입력 처리가 가능한지 확인
     * (라운드 정보 창이 열려있지 않을 때)
     * 
     * @return 입력 처리 가능 여부
     */
    public boolean isInputProcessingActive() {
        return !showingRoundInfo;
    }
}
