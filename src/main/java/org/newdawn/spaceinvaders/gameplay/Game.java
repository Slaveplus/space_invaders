package org.newdawn.spaceinvaders.gameplay;

import java.awt.Canvas;
import java.awt.Graphics2D;
// no direct AWT listeners here; handled via InputManager
import java.util.ArrayList;
import java.util.List;

import org.newdawn.spaceinvaders.gameplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.gameplay.entity.BossEntity;
import org.newdawn.spaceinvaders.gameplay.entity.CoinDisplayEntity;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.entity.NearEntity;
import org.newdawn.spaceinvaders.gameplay.entity.ExplosionEntity;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceAttack;
import org.newdawn.spaceinvaders.gameplay.entity.MissileEntity;
import org.newdawn.spaceinvaders.gameplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.gameplay.entity.ShotEntity;
import org.newdawn.spaceinvaders.gameplay.core.GameplayContext;
import org.newdawn.spaceinvaders.gameplay.core.SharedGameplayCoordinator;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.shop.ShopCategory;
import org.newdawn.spaceinvaders.shop.ShopItem;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.net.GameNetworkAdapter;
import org.newdawn.spaceinvaders.gameplay.net.LocalLoopbackNetworkAdapter;

/**
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic. 
 * 
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 * 
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 * 
 * @author Kevin Glass
 */
public class Game extends Canvas implements Screen, GameplayContext
{
	/** The stragey that allows us to use accelerate page flipping */
	// BufferStrategy는 상위 App에서 관리
	// entities and removeList are now managed by GameStateManager
	/** The entity representing the player */
	private ShipEntity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** UserManager for accessing equipped items */
	private UserManager userManager;
	/** ResolutionManager for handling resolution scaling */
	private ResolutionManager resolutionManager;
	// lastFire and firingInterval are now managed by GameStateManager
	/** 현재 장착된 우주선 스킨 경로 */
	private String currentSpaceshipSkin = "sprites/ship.gif";
	/** 현재 장착된 무기 스킨 경로 */
	private String currentWeaponSkin = "sprites/shot.gif";
	
	/** 이전 프레임의 일시정지 상태 (상태 변화 감지용) */
	private boolean wasPaused = false;

	/** The current number of frames recorded */
	// FPS 표시 기능은 상위에서 처리 가능, 내부적으로는 카운트만 유지하지 않음
	
	/** The game state manager */
	private GameStateManager gameStateManager;
	/** The input manager */
	private InputManager inputManager;
	/** The skill manager */
	private SkillManager skillManager;
	/** The UI renderer */
	private UIRenderer uiRenderer;
	/** Background renderer (cached) */
	private BackgroundRenderer backgroundRenderer;
	/** Shared gameplay coordinator */
	private final SharedGameplayCoordinator gameplayCoordinator;
	// gameplay는 mainmenu 패키지에 의존하지 않도록, 오버레이는 UIRenderer에서 처리
	
	/** 메인메뉴 전환 요청 플래그 */
	private boolean requestMainMenu = false;
	/** ScreenNavigator for menu navigation */
	private ScreenNavigator navigator;
	/** 게임 시작 시간 (3초 공격 지연용) */
	private long gameStartTime = 0;

	/** 네트워크 어댑터 (싱글: LocalLoopback 기본) */
	private GameNetworkAdapter networkAdapter;
	
	/**
	 * Construct our game and set it running.
	 */
	public Game(ScreenNavigator navigator) {
		this.navigator = navigator;
		// 게임 시작 시간 기록
		gameStartTime = System.currentTimeMillis();
		setIgnoreRepaint(true);
		setBounds(0,0,800,600);
		setFocusable(true);
		
		// initialize the game state manager
		gameStateManager = new GameStateManager();
		
	// initialize the skill manager
	skillManager = new SkillManager(this);
	gameplayCoordinator = new SharedGameplayCoordinator(this);

	// initialize the UI renderer
		uiRenderer = new UIRenderer(this);

		// background & overlays
		backgroundRenderer = new BackgroundRenderer();
		updateBackgroundForRound(1); // 1라운드부터 시작
		
		// initialize the input manager
		inputManager = new InputManager(gameStateManager, this);
		
		// add input handlers (after inputManager is initialized)
		addKeyListener(inputManager.new KeyInputHandler());
		addMouseListener(inputManager.new MouseInputHandler());
		
		// initialise the entities in our game so there's something
		// to see at startup
		System.out.println("🎮 Game constructor calling initEntities()...");
		initEntities();

		// 기본 로컬 네트워크 어댑터 설정 (멀티 환경에서는 외부에서 교체)
		this.networkAdapter = new LocalLoopbackNetworkAdapter(gameStateManager);
	}

	
	/**
	 * ResolutionManager 설정
	 */
	public void setResolutionManager(ResolutionManager resolutionManager) {
		this.resolutionManager = resolutionManager;
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	public void startGame() {
		System.out.println("🎮 startGame() called!");
		
		// ShopManager에 아이템들 추가 및 장착 정보 로드
		if (userManager != null && userManager.isLoggedIn()) {
			System.out.println("Game: 게임 시작 시 ShopManager 초기화");
			// ShopManager의 static 메서드로 아이템 초기화
			org.newdawn.spaceinvaders.shop.ShopManager.initializeDefaultItems(userManager.getShopManager());
			userManager.getShopManager().loadInventoryFromDB();
			userManager.getShopManager().loadEquipmentFromDB();
		}
		
		// 장착된 아이템 적용 (ShopManager 초기화 후)
		applyEquippedItems();
		
		// 게임플레이 상태 초기화 및 라운드 준비
		String playerId = gameStateManager.getLocalPlayerId();
		gameplayCoordinator.startNewGame(playerId);
		ship = getShip(playerId);
		applyEquippedItems();
		
		// 입력 상태 초기화
		inputManager.reset();
		
		// 스킬 매니저 초기화
		skillManager.reset();
	}
	
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		System.out.println("🎮 initEntities() called for Round " + gameStateManager.getCurrentRound());
		String playerId = gameStateManager.getLocalPlayerId();
		gameplayCoordinator.initializeRound(playerId);
		ship = getShip(playerId);
	}
	
	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() {
		gameStateManager.setLogicRequiredThisLoop(true);
	}
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 * 
	 * @param entity The entity that should be removed
	 */
	@Override
	public void removeEntity(Entity entity) {
		gameStateManager.getRemoveList().add(entity);
	}
	
	/**
	 * Notification that the player has died. 
	 */
	public void notifyDeath() {
		// Check if player is invincible
		if (skillManager.isInvincible()) {
			return; // No damage taken when invincible
		}
		
		gameStateManager.takeDamage();
		if (gameStateManager.getCurrentHP() <= 0) {
			gameStateManager.setMessage("아쉬워요 .... 다시 시작!!");
			gameStateManager.setWaitingForKeyPress(true);
		}
	}
	
	/**
	 * Notification that the player has been damaged by boss attack
	 */
	public void notifyPlayerDamaged(int damage) {
		// Check if player is invincible
		if (skillManager.isInvincible()) {
			return; // No damage taken when invincible
		}
		
		// Check if game is in a state where damage should be applied
		if (gameStateManager.isWaitingForKeyPress() || 
			gameStateManager.isShowingPauseMenu() || 
			gameStateManager.isShowingSkillMenu() ||
			gameStateManager.isShowingRoundInfo() ||
			gameStateManager.isShowingQuitConfirm()) {
			return;
		}
		
		// Apply damage only once (not multiple times)
		gameStateManager.takeDamage();
		if (gameStateManager.getCurrentHP() <= 0) {
			gameStateManager.setMessage("아쉬워요 .... 다시 시작!!");
			gameStateManager.setWaitingForKeyPress(true);
		}
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		gameplayCoordinator.handleRoundClear(gameStateManager.getLocalPlayerId());
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		gameplayCoordinator.handleAlienKilled(gameStateManager.getLocalPlayerId());
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// Calculate firing interval based on attack speed skill
		long currentFiringInterval = (long) (gameStateManager.getFiringInterval() / gameStateManager.getAttackSpeed());
		
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - gameStateManager.getLastFire() < currentFiringInterval) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		gameStateManager.setLastFire(System.currentTimeMillis());
		
		if (skillManager.hasTripleShot()) {
			// Fire three shots in a wider spread pattern
			ShotEntity shot1 = new ShotEntity(this, currentWeaponSkin, ship.getX()-5, ship.getY()-30);
			ShotEntity shot2 = new ShotEntity(this, currentWeaponSkin, ship.getX()+10, ship.getY()-30);
			ShotEntity shot3 = new ShotEntity(this, currentWeaponSkin, ship.getX()+25, ship.getY()-30);
			gameStateManager.getEntities().add(shot1);
			gameStateManager.getEntities().add(shot2);
			gameStateManager.getEntities().add(shot3);
		} else {
			// Fire single shot
			ShotEntity shot = new ShotEntity(this, currentWeaponSkin, ship.getX()+10, ship.getY()-30);
			gameStateManager.getEntities().add(shot);
			System.out.println("Player fired shot at position: (" + (ship.getX()+10) + ", " + (ship.getY()-30) + ")");
		}
	}
	
	/**
	 * Add an alien shot to the game
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 */
	public void addAlienShot(int x, int y) {
		ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", x, y, true);
		gameStateManager.getEntities().add(shot);
	}
	
	/**
	 * Add an alien shot with slight aim adjustment towards player
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 * @param alienX The x location of the alien firing
	 */
	public void addAimedAlienShot(int x, int y, int alienX) {
		// Add more accurate horizontal adjustment towards player (within 15 pixel range)
		int playerX = ship.getX() + 10; // Player center
		int aimOffset = (int)((playerX - alienX) * 0.15); // 15% of distance towards player
		aimOffset = Math.max(-15, Math.min(15, aimOffset)); // Clamp to player-sized range
		
		ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", x + aimOffset, y, true);
		gameStateManager.getEntities().add(shot);
	}
	
	/**
	 * Create a skill drop that moves downward in a straight line
	 * 
	 * @param x The x location where the skill drop is created
	 * @param y The y location where the skill drop is created
	 * @param skillType The type of skill (0: Invincible, 2: Triple Shot, 3: Missile)
	 * @param skillValue The value/duration of the skill
	 */
	@Override
	public void createSkillDrop(int x, int y, int skillType, int skillValue) {
		System.out.println("🎁 createSkillDrop called: type=" + skillType + ", value=" + skillValue + " at (" + x + ", " + y + ")");
		
		// Create skill drop using ShotEntity with skill drop functionality
		// 스킬 타입에 따라 다른 이미지 사용
		String spritePath;
		switch (skillType) {
			case 0: // Invincible (무적)
				spritePath = "sprites/Skill/1.png";
				break;
			case 2: // Triple Shot (3연발)
				spritePath = "sprites/Skill/3.png";
				break;
			case 3: // Missile (미사일)
				spritePath = "sprites/Skill/4.png";
				break;
			default:
				spritePath = "sprites/shot.gif";
				break;
		}
		
		System.out.println("🎁 Using sprite: " + spritePath);
		ShotEntity skillDrop = new ShotEntity(this, spritePath, x, y, skillType, skillValue);
		gameStateManager.getEntities().add(skillDrop);
		System.out.println("🎁 Skill drop entity added to game. Total entities: " + gameStateManager.getEntities().size());
	}
	
	/**
	 * Try to fire shots from aliens (only those close to player)
	 */
	private void tryAlienFire() {
		// check that we have waited long enough to fire
		if (System.currentTimeMillis() - gameStateManager.getLastAlienFire() < gameStateManager.getAlienFiringInterval()) {
			return;
		}
		
		// find all aliens (no distance restriction)
		ArrayList<AlienEntity> aliens = new ArrayList<>();
		ArrayList<Entity> entities = gameStateManager.getEntities();
		
		for (Entity entity : entities) {
			if (entity instanceof AlienEntity) {
				// 모든 적이 공격 가능 (거리 제한 없음)
				aliens.add((AlienEntity) entity);
			}
		}
		
		if (aliens.size() > 0) {
			// pick a random alien from those close enough
			int randomIndex = (int) (Math.random() * aliens.size());
			AlienEntity alien = aliens.get(randomIndex);
			
			// fire from this alien
			alien.tryToFire();
			gameStateManager.setLastAlienFire(System.currentTimeMillis());
		}
	}
	
	/**
	 * Get the player's current attack power
	 * 
	 * @return The player's attack power
	 */
	public int getPlayerAttackPower() {
		return gameStateManager.getAttackPower();
	}
	
	/**
	 * Get the current round number
	 * 
	 * @return The current round
	 */
	public int getCurrentRound() {
		return gameStateManager.getCurrentRound();
	}
	
	/**
	 * Check if player is currently invincible
	 */
	public boolean isPlayerInvincible() {
		return skillManager.isInvincible();
	}
	
	/**
	 * Check if player has piercing shots (관통 스킬 제거됨)
	 */
	public boolean hasPiercingShots() {
		return false; // 관통 스킬 제거됨
	}
	
	/**
	 * Check if player has triple shot
	 */
	public boolean hasTripleShot() {
		return skillManager.hasTripleShot();
	}
	
	/**
	 * Add skill to inventory
	 */
	public void addSkillToInventory(int skillType, int skillValue) {
		skillManager.addSkillToInventory(skillType, skillValue);
	}
	
	@Override
	public void fireMissile(String playerId, double targetX, double targetY) {
		try {
			// Fire missile from player position
			MissileEntity missile = new MissileEntity(this, "sprites/Skill/Missile.png", 
					(int)ship.getX() + 15, (int)ship.getY(), targetX, targetY);
			gameStateManager.getEntities().add(missile);
		} catch (Exception e) {
			System.err.println("Error firing missile: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Convenience overload used by legacy callers that do not track player ids.
	 */
	public void fireMissile(double targetX, double targetY) {
		fireMissile(null, targetX, targetY);
	}
	
	/**
	 * Add entity to the game
	 * 
	 * @param entity The entity to add
	 */
	@Override
	public void addEntity(Entity entity) {
		gameStateManager.getEntities().add(entity);
	}
	
	/**
	 * 코인 드롭 생성
	 * 
	 * @param x X 좌표
	 * @param y Y 좌표
	 * @param coinValue 코인 가치
	 */
	public void createCoinDrop(int x, int y, int coinValue) {
		try {
			org.newdawn.spaceinvaders.gameplay.entity.CoinEntity coin = 
				new org.newdawn.spaceinvaders.gameplay.entity.CoinEntity(this, x, y, coinValue);
			gameStateManager.getEntities().add(coin);
			System.out.println("💰 Coin drop created at (" + x + ", " + y + ") with value: " + coinValue);
		} catch (Exception e) {
			System.err.println("Error creating coin drop: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 획득한 코인 추가
	 * 
	 * @param amount 추가할 코인 수
	 */
	public void addEarnedCoins(int amount) {
		gameStateManager.addEarnedCoins(amount);
	}
	
	/**
	 * 코인 획득 표시
	 * 
	 * @param x X 좌표
	 * @param y Y 좌표
	 * @param coinAmount 획득한 코인 수
	 */
	public void showCoinEarned(int x, int y, int coinAmount) {
		try {
			// 코인 획득량을 게임 상태에 추가
			addEarnedCoins(coinAmount);
			
			// 코인 표시 엔티티 생성
			CoinDisplayEntity coinDisplay = new CoinDisplayEntity(this, x, y, coinAmount);
			addEntity(coinDisplay);
			
			System.out.println("💰 Coin earned display created: +" + coinAmount + " at (" + x + ", " + y + ")");
		} catch (Exception e) {
			System.err.println("Error creating coin earned display: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public SkillManager getSkillManager(String playerId) {
		return skillManager;
	}

	@Override
	public ShipEntity getShip(String playerId) {
		return ship;
	}

	@Override
	public ShipEntity createPlayerShip(String playerId) {
		ship = new ShipEntity(this, currentSpaceshipSkin, 370, 550);
		return ship;
	}

	@Override
	public Entity createAlienEntity(int x, int y) {
		return new AlienEntity(this, x, y);
	}

	@Override
	public Entity createNearEntity(int round, int index) {
		int posX = 150 + (index * 100);
		return new NearEntity(this, posX, 120, round, index);
	}

	@Override
	public Entity createBossEntity(int bossRound) {
		return new BossEntity(this, 400, 120, bossRound);
	}

	@Override
	public List<Entity> getActiveEntities() {
		return gameStateManager.getEntities();
	}

    @Override
    public List<Entity> getPendingRemovals() {
        return gameStateManager.getRemoveList();
    }

	@Override
	public void onRoundBackgroundChanged(int round) {
		updateBackgroundForRound(round);
	}

	@Override
	public void addEarnedCoins(String playerId, int amount) {
		addEarnedCoins(amount);
	}

	@Override
	public void showCoinEarned(String playerId, int x, int y, int coinAmount) {
		showCoinEarned(x, y, coinAmount);
	}

	@Override
	public void onGameCompleted(String message) {
		saveCoinsAndPlayTime();
		gameStateManager.setMessage(message);
		gameStateManager.setWaitingForKeyPress(true);
		gameStateManager.setGameCompleted(true);
	}
	
	/**
	 * 코인과 플레이 시간을 DB에 저장
	 */
	public void saveCoinsAndPlayTime() {
		if (userManager != null && userManager.isLoggedIn()) {
			int earnedCoins = gameStateManager.getEarnedCoins();
			if (earnedCoins > 0) {
				// 획득한 코인을 유저의 총 코인에 추가
				userManager.addCoins(earnedCoins);
				System.out.println("💰 Game coins saved to DB: " + earnedCoins + " coins");
			}
			
			// 플레이 기록 저장
			long playTimeMs = gameStateManager.getPlayTimeMs();
			boolean completed = gameStateManager.getCurrentRound() >= gameStateManager.getMaxRound() || gameStateManager.isGameCompleted();
			int finalRound = gameStateManager.getCurrentRound();
			
			// GameRecord 생성
			org.newdawn.spaceinvaders.database.GameRecord gameRecord = 
				new org.newdawn.spaceinvaders.database.GameRecord(
					userManager.getCurrentUser().getUid(),
					userManager.getCurrentUser().getUsername(),
					playTimeMs,
					earnedCoins,
					completed,
					finalRound,
					org.newdawn.spaceinvaders.database.GameRecord.GameMode.SINGLE,
					java.util.Collections.emptyList()
				);
			
			// Firebase DB에 플레이 기록 저장
			saveGameRecordToDB(gameRecord);
			
			System.out.println("⏱️ Play time: " + gameStateManager.getPlayTime() + " (" + playTimeMs + "ms)");
			System.out.println("📊 Game record saved: " + gameRecord.toString());
		} else {
			System.out.println("⚠️ Cannot save coins: User not logged in");
		}
	}
	
	/**
	 * 게임 기록을 Firebase DB에 저장
	 * 
	 * @param gameRecord 저장할 게임 기록
	 */
	private void saveGameRecordToDB(org.newdawn.spaceinvaders.database.GameRecord gameRecord) {
		try {
			// Firebase DB 경로: users/{uid}/gameRecords/single/{recordId}
			String dbPath = "users/" + gameRecord.getUserId() + "/gameRecords/single/" + gameRecord.getRecordId();
			
			// GameRecord를 Map으로 변환
			java.util.Map<String, Object> recordData = new java.util.HashMap<>();
			recordData.put("recordId", gameRecord.getRecordId());
			recordData.put("userId", gameRecord.getUserId());
			recordData.put("username", gameRecord.getUsername());
			recordData.put("playTimeMs", gameRecord.getPlayTimeMs());
			recordData.put("playTime", gameRecord.getPlayTime());
			recordData.put("earnedCoins", gameRecord.getEarnedCoins());
			recordData.put("completed", gameRecord.isCompleted());
			recordData.put("finalRound", gameRecord.getFinalRound());
			recordData.put("playDate", gameRecord.getPlayDateString());
			recordData.put("mode", gameRecord.getMode().name());
			recordData.put("coPlayers", new java.util.ArrayList<>(gameRecord.getCoPlayers()));
			
			// Firebase DB에 저장
			boolean success = userManager.getFirebaseDB().putData(dbPath, recordData);
			
			if (success) {
				System.out.println("✅ Game record saved to Firebase DB: " + dbPath);
			} else {
				System.err.println("❌ Failed to save game record to Firebase DB");
			}
		} catch (Exception e) {
			System.err.println("Error saving game record to DB: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * The main game loop. This loop is running during all game
	 * play as is responsible for the following activities:
	 * <p>
	 * - Working out the speed of the game loop to update moves
	 * - Moving the game entities
	 * - Drawing the screen contents (entities, text)
	 * - Updating game events
	 * - Checking Input
	 * <p>
	 */
	public void update(long delta) {
		// 1) 네트워크 틱 (authoritative 스냅샷 생성 또는 수신)
		if (networkAdapter != null) {
			networkAdapter.tick(System.currentTimeMillis());
			// 향후: 클라이언트 모드에서 snapshot 적용/보간 로직 위치
		}

		// 라운드 정보 창 자동 닫기 (7초 후)
		if (gameStateManager.shouldAutoCloseRoundInfo()) {
			gameStateManager.hideRoundInfo();
			gameStateManager.setWaitingForKeyPress(false);
		}
		
		// 일시정지 상태 변화 감지
		boolean currentlyPaused = gameStateManager.isPaused();
		
		if (currentlyPaused && !wasPaused) {
			// 일시정지 시작
			gameStateManager.startPause();
		} else if (!currentlyPaused && wasPaused) {
			// 일시정지 종료
			gameStateManager.endPause();
		}
		
		wasPaused = currentlyPaused;
		
		// 게임플레이 업데이트 (일시정지 상태가 아닐 때만)
		if (!gameStateManager.isWaitingForKeyPress() &&
			!gameStateManager.isShowingPauseMenu() &&
			!gameStateManager.isShowingSkillMenu() &&
			!gameStateManager.isShowingRoundInfo() &&
			!gameStateManager.isShowingQuitConfirm()) {
			// Update skill effects
			skillManager.updateSkillEffects();
			
			// Use direct iteration to avoid ArrayList copy overhead
			ArrayList<Entity> entities = gameStateManager.getEntities();
			for (int i = 0; i < entities.size(); i++) {
				Entity entity = entities.get(i);
				if (entity != null) {
					entity.move(delta);
				}
			}
			tryAlienFire();
		}

		// Ship movement & fire (일시정지 상태가 아닐 때만)
		if (ship != null && !gameStateManager.isShowingPauseMenu() && 
			!gameStateManager.isShowingSkillMenu() && !gameStateManager.isShowingRoundInfo() &&
			!gameStateManager.isShowingQuitConfirm()) {
			ship.setHorizontalMovement(0);
			if (inputManager.isLeftPressed() && !inputManager.isRightPressed()) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if (inputManager.isRightPressed() && !inputManager.isLeftPressed()) {
				ship.setHorizontalMovement(moveSpeed);
			}
			if (inputManager.isFirePressed()) {
				tryToFire();
			}
		}

		// collisions (일시정지 상태가 아닐 때만)
		ArrayList<Entity> entities = gameStateManager.getEntities();
		if (!gameStateManager.isShowingPauseMenu() && !gameStateManager.isShowingSkillMenu() &&
			!gameStateManager.isShowingRoundInfo() && !gameStateManager.isShowingQuitConfirm()) {
			for (int i=0;i<entities.size();i++) {
				Entity e1 = entities.get(i);
				for (int j=i+1;j<entities.size();j++) {
					Entity e2 = entities.get(j);
					if (e1.collidesWith(e2)) {
						e1.collidedWith(e2);
						e2.collidedWith(e1);
					}
				}
			}
			entities.removeAll(gameStateManager.getRemoveList());
			gameStateManager.getRemoveList().clear();
			if (gameStateManager.isLogicRequiredThisLoop()) {
				for (Entity e : entities) e.doLogic();
				gameStateManager.setLogicRequiredThisLoop(false);
			}
		}
	}

	public void render(Graphics2D g) {
		// 해상도 스케일링 적용
		if (resolutionManager != null) {
			double scaleX = resolutionManager.getScaleX();
			double scaleY = resolutionManager.getScaleY();
			g.scale(scaleX, scaleY);
		}
		
		// 배경 (cached)
		backgroundRenderer.draw(g);
		// entities
		ArrayList<Entity> entities = gameStateManager.getEntities();
		for (Entity entity : entities) entity.draw(g);
		// UI & overlays
		uiRenderer.drawGameUI(g, gameStateManager, skillManager);
		if (gameStateManager.isShowingPauseMenu()) { drawPauseMenu(g); }
		if (gameStateManager.isShowingSkillMenu()) { drawSkillMenu(g); }
		if (gameStateManager.isShowingRoundInfo()) { drawRoundInfoOverlay(g, gameStateManager); }
		if (gameStateManager.isShowingQuitConfirm()) { drawQuitConfirmOverlay(g, gameStateManager); }
		if (gameStateManager.isWaitingForKeyPress()) {
			uiRenderer.drawMessage(g, gameStateManager.getMessage());
		}
	}
	
	
	/**
	 * Getter methods for InputManager
	 */
	public Entity getShip() {
		return ship;
	}
	
	public int getShipX() {
		return ship != null ? (int)ship.getX() : 370;
	}
	
	public int getShipY() {
		return ship != null ? (int)ship.getY() : 550;
	}
	
	public double getMoveSpeed() {
		return moveSpeed;
	}
	
	public boolean isWaitingForKeyPress() {
		return gameStateManager.isWaitingForKeyPress();
	}
	
	/**
	 * 메인메뉴로 돌아가기
	 */
	public void returnToMainMenu() {
		// 게임 상태 초기화
		gameStateManager.resetGame();
		// 메인메뉴로 이동
		if (navigator != null) {
			navigator.showMainMenu();
		}
	}
	
	public void setWaitingForKeyPress(boolean waiting) {
		gameStateManager.setWaitingForKeyPress(waiting);
	}
	
	/**
	 * 게임 상태 반환 (gameplay 패키지용)
	 */
	// 게임 상태 텍스트 반환은 더 이상 필요하지 않음 (화면 전환은 App에서 관리)
	
	/**
	 * 새 게임 시작 (gameplay 패키지용)
	 */
	public void startNewGame() { 
		System.out.println("🎮 startNewGame() called!");
		startGame(); 
	}
	
	/**
	 * 엔티티 리스트 반환 (gameplay 패키지용)
	 */
	public ArrayList<Entity> getEntities() {
		return gameStateManager.getEntities();
	}
	
	/**
	 * 게임플레이 상태 반환
	 */
	public GameStateManager getGameplayState() {
		return gameStateManager;
	}
	
	/**
	 * 스킬 매니저 반환
	 */
	public SkillManager getSkillManager() {
		return skillManager;
	}
	
	/**
	 * UI 렌더러 반환
	 */
	public UIRenderer getUIRenderer() {
		return uiRenderer;
	}
	
	/**
	 * 스킬 메뉴 그리기
	 */
	public void drawSkillMenu(java.awt.Graphics2D g2d) {
		uiRenderer.drawSkillOverlay(
			g2d,
			gameStateManager.getSkillPoints(),
			gameStateManager.getAttackPower(),
			gameStateManager.getAttackSpeed(),
			gameStateManager.getMaxHP(),
			skillManager.getAttackPowerCost(),
			skillManager.getAttackSpeedCost(),
			skillManager.getHpUpCost(),
			gameStateManager.getSelectedSkill(),
			skillManager
		);
	}
	
	/**
	 * 일시정지 메뉴 그리기
	 */
	public void drawPauseMenu(java.awt.Graphics2D g2d) {
		uiRenderer.drawPauseOverlay(g2d, gameStateManager.getSelectedPauseMenuItem());
	}
	
	/**
	 * 라운드 설명 창 그리기
	 */
	public void drawRoundInfoOverlay(java.awt.Graphics2D g2d, GameStateManager gameStateManager) {
		uiRenderer.drawRoundInfoOverlay(g2d, gameStateManager);
	}
	
	/**
	 * 그만두기 확인 창 그리기
	 */
	public void drawQuitConfirmOverlay(java.awt.Graphics2D g2d, GameStateManager gameStateManager) {
		uiRenderer.drawQuitConfirmOverlay(g2d, gameStateManager);
	}

	
	/**
	 * Create explosion effect
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param radius Explosion radius
	 */
	public void createExplosion(int x, int y, double radius) {
		try {
			ExplosionEntity explosion = new ExplosionEntity(this, "sprites/Skill/Explosion.png", x, y, radius);
			gameStateManager.getEntities().add(explosion);
		} catch (Exception e) {
			System.err.println("Error creating explosion: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Create heat effect (피튀기는 효과)
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param radius Heat effect radius
	 */
	public void createHeatEffect(int x, int y, double radius) {
		try {
			org.newdawn.spaceinvaders.gameplay.entity.HeatEffectEntity heatEffect = 
				new org.newdawn.spaceinvaders.gameplay.entity.HeatEffectEntity(this, x, y);
			gameStateManager.getEntities().add(heatEffect);
		} catch (Exception e) {
			System.err.println("Error creating heat effect: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Add score points
	 * 
	 * @param points Points to add
	 */
	public void addScore(int points) {
		// 점수 추가
	}
	
	/**
	 * Add skill points
	 * 
	 * @param points Skill points to add
	 */
	public void addSkillPoints(int points) {
		int currentPoints = gameStateManager.getSkillPoints();
		gameStateManager.setSkillPoints(currentPoints + points);
		System.out.println("Skill points added: " + points + " (Total: " + (currentPoints + points) + ")");
	}

	/**
	 * Drop random skill at specified location
	 */
	public void dropRandomSkill(int x, int y) {
		try {
			System.out.println("🎁 dropRandomSkill called at (" + x + ", " + y + ")");
			
			// Random skill type (0: Invincible, 1: Triple Shot, 2: Missile) - 관통 스킬 제거
			double random = Math.random();
			int skillType;
			int skillValue;
			
			if (random < 0.33) {
				skillType = 0; // Invincible (무적)
				skillValue = 5; // 5 seconds
				System.out.println("🎁 Dropping Invincible skill");
			} else if (random < 0.66) {
				skillType = 2; // Triple Shot (3연발)
				skillValue = 8; // 8 seconds
				System.out.println("🎁 Dropping Triple Shot skill");
			} else {
				skillType = 3; // Missile (미사일)
				skillValue = 1; // 1 missile
				System.out.println("🎁 Dropping Missile skill");
			}
			
			// Create skill drop entity
			createSkillDrop(x, y, skillType, skillValue);
			System.out.println("🎁 Skill drop created successfully");
			
		} catch (Exception e) {
			System.err.println("Error dropping random skill: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Drop random skill points at specified location
	 */
	public void dropRandomSkillPoints(int x, int y) {
		try {
			// Random skill points amount (1-5)
			int skillPoints = 1 + (int)(Math.random() * 5);
			
			// Add skill points directly
			addSkillPoints(skillPoints);
			
			System.out.println("Skill points dropped: " + skillPoints);
			
		} catch (Exception e) {
			System.err.println("Error dropping skill points: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Check if there's currently a boss in the game
	 * 
	 * @return True if boss exists
	 */
	public boolean hasBoss() {
		for (Object obj : gameStateManager.getEntities()) {
			if (obj instanceof BossEntity) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get the current boss entity
	 * 
	 * @return BossEntity or null if no boss exists
	 */
	public BossEntity getBoss() {
		for (Object obj : gameStateManager.getEntities()) {
			if (obj instanceof BossEntity) {
				return (BossEntity) obj;
			}
		}
		return null;
	}
	

	/**
	 * Handle boss defeat
	 */
	public void notifyBossDefeated() {
		String playerId = gameStateManager.getLocalPlayerId();
		gameplayCoordinator.handleRoundClear(playerId);
	}

	/**
	 * Check if all near monsters are defeated and advance to boss round
	 */
	public void checkAllNearMonstersDefeated() {
		gameplayCoordinator.handleNearMonstersCleared(gameStateManager.getLocalPlayerId());
	}

	/**
	 * Add a boss shot to the game
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public void addBossShot(int x, int y) {
		try {
			ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", x, y, true); // true = alien shot
			gameStateManager.getEntities().add(shot);
		} catch (Exception e) {
			System.err.println("Error adding boss shot: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the UserManager for accessing equipped items
	 */
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
		// ShopManager의 장착 정보 동기화
		if (userManager != null && userManager.isLoggedIn()) {
			// ShopManager에 아이템들 추가 (MainMenu와 동일한 방식)
			org.newdawn.spaceinvaders.shop.ShopManager.initializeDefaultItems(userManager.getShopManager());
			userManager.getShopManager().loadInventoryFromDB();
			userManager.getShopManager().loadEquipmentFromDB();
		}
	}
	
	/**
	 * Get the current UserManager
	 */
	public UserManager getUserManager() {
		return userManager;
	}
	
	/**
	 * Get the GameStateManager
	 */
	@Override
	public GameStateManager getGameStateManager() {
		return gameStateManager;
	}
	
	/**
	 * Apply equipped items from UserManager
	 */
	private void applyEquippedItems() {
		if (userManager == null) {
			return;
		}
		
		// Apply spaceship skin
		applySpaceshipSkin();
		
		// Apply weapon skin
		applyWeaponSkin();
		
		// Apply powerup effects
		applyPowerupEffects();
	}
	
	/**
	 * Apply equipped spaceship skin
	 */
	private void applySpaceshipSkin() {
		if (userManager == null) return;
		
		ShopItem equippedSpaceship = userManager.getShopManager().getEquippedItem(ShopCategory.SPACESHIPS);
		if (equippedSpaceship != null) {
			currentSpaceshipSkin = "sprites/ships/" + equippedSpaceship.getId() + ".png";
			// 기존 ShipEntity가 있으면 스킨 변경
			if (ship != null) {
				ship.changeSkin(currentSpaceshipSkin);
			}
		}
	}
	
	/**
	 * Apply equipped weapon skin
	 */
	private void applyWeaponSkin() {
		if (userManager == null) return;
		
		ShopItem equippedWeapon = userManager.getShopManager().getEquippedItem(ShopCategory.WEAPONS);
		if (equippedWeapon != null) {
			currentWeaponSkin = "sprites/weapons/" + equippedWeapon.getId() + ".png";
		}
	}
	
	/**
	 * Apply powerup effects
	 */
	private void applyPowerupEffects() {
		if (userManager == null) return;
		
		// Apply powerup effects if any
		// This can be extended based on your powerup system
	}
	
	/**
	 * Get current spaceship skin path
	 */
	public String getCurrentSpaceshipSkin() {
		return currentSpaceshipSkin;
	}
	
	/**
	 * Get current weapon skin path
	 */
	public String getCurrentWeaponSkin() {
		return currentWeaponSkin;
	}
	
	/**
	 * Go to main menu (called from pause menu)
	 */
	public void goToMainMenu() {
		// 메인메뉴 전환 요청 플래그 설정
		requestMainMenu = true;
	}
	
	/**
	 * 메인메뉴 전환 요청 여부 확인
	 */
	public boolean isRequestingMainMenu() {
		return requestMainMenu;
	}
	
	/**
	 * 메인메뉴 전환 요청 플래그 리셋
	 */
	public void resetMainMenuRequest() {
		requestMainMenu = false;
	}
	
	/**
	 * 라운드별 배경 업데이트
	 */
	public void updateBackgroundForRound(int round) {
		String backgroundFileName;
		
		// 라운드별 배경 파일 매핑 (수정된 버전)
		if (round == 1 || round == 2) {
			backgroundFileName = "1.png";
		} else if (round == 3 || round == 4) {
			backgroundFileName = "2.png";
		} else if (round == 5 || round == 6) {
			backgroundFileName = "3.png";
		} else if (round == 7 || round == 8) {
			backgroundFileName = "4.png";  // 5.png에서 4.png로 변경
		} else {
			backgroundFileName = "1.png"; // 기본값
		}
		
		String backgroundPath = "sprites/stage_background/" + backgroundFileName;
		backgroundRenderer.setResourcePath(backgroundPath);
		System.out.println("라운드 " + round + " 배경 변경: " + backgroundPath);
	}
	
	/**
	 * Check if 3 seconds have passed since game start
	 */
	public boolean canEnemiesAttack() {
		return (System.currentTimeMillis() - gameStartTime) >= 3000; // 3초 = 3000ms
	}
	
	/**
	 * Get the player ship entity
	 */
	public ShipEntity getPlayerShip() {
		return (ShipEntity) ship;
	}
	
	/**
	 * The entry point into the game. We'll simply create an
	 * instance of class which will start the display and game
	 * loop.
	 * 
	 * @param argv The arguments that are passed into our game
	 */
	public static void main(String argv[]) {
		// Game is now managed by SpaceInvadersApp
		// This main method is kept for compatibility but should not be used directly
	}
	
}
