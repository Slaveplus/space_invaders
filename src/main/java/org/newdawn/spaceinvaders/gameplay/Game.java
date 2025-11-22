package org.newdawn.spaceinvaders.gameplay;

import java.awt.Canvas;
import java.awt.Graphics2D;
// 여기서는 직접적인 AWT 리스너를 사용하지 않음; InputManager를 통해 처리
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.common.entity.boss.BossEnvironment;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.effect.HeatEffectEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEntity;
import org.newdawn.spaceinvaders.common.entity.near.NearEntity;
import org.newdawn.spaceinvaders.common.entity.near.NearEnvironment;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseBossShotEntity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.ShotEntity;
import org.newdawn.spaceinvaders.common.entity.ui.CoinDisplayEntity;
import org.newdawn.spaceinvaders.gameplay.entity.CoinEntity;
import org.newdawn.spaceinvaders.gameplay.core.GameplayContext;
import org.newdawn.spaceinvaders.gameplay.core.SharedGameplayCoordinator;
import org.newdawn.spaceinvaders.gameplay.entity.BossShotEntity;
import org.newdawn.spaceinvaders.gameplay.entity.GameplayAlienEnvironment;
import org.newdawn.spaceinvaders.database.LeaderboardRecord;
import org.newdawn.spaceinvaders.database.LeaderboardRepository;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.net.GameNetworkAdapter;
import org.newdawn.spaceinvaders.gameplay.net.LocalLoopbackNetworkAdapter;
import org.newdawn.spaceinvaders.common.util.Logger;
import org.newdawn.spaceinvaders.common.util.LoggerFactory;

/**
 * 게임의 메인 훅. 이 클래스는 디스플레이 관리자와 게임 로직의 중앙 조정자 역할을 합니다.
 * 
 * 디스플레이 관리는 게임의 모든 엔티티들을 순환하며 이동을 요청하고
 * 적절한 위치에 그리는 루프로 구성됩니다. 내부 클래스의 도움으로
 * 플레이어가 메인 우주선을 제어할 수 있도록 합니다.
 * 
 * 조정자로서 게임 내 엔티티들이 이벤트를 감지할 때 (예: 적 처치, 플레이어 사망)
 * 알림을 받고 적절한 게임 액션을 취합니다.
 * 
 * @author Kevin Glass
 */
public class Game extends Canvas implements Screen, GameplayContext, GameContext, BossEnvironment, NearEnvironment
{
	/** 가속 페이지 플리핑을 사용할 수 있게 해주는 전략 */
	// BufferStrategy는 상위 App에서 관리
	// entities and removeList는 이제 GameStateManager에서 관리됨
	/** 플레이어를 나타내는 엔티티 */
	private ShipEntity ship;
	/** 플레이어 우주선이 이동해야 하는 속도 (픽셀/초) */
	private double moveSpeed = 300;
	/** 장착된 아이템에 접근하기 위한 UserManager */
	private UserManager userManager;
	/** 해상도 스케일링을 처리하는 ResolutionManager */
	private ResolutionManager resolutionManager;
	// lastFire과 firingInterval은 이제 GameStateManager에서 관리됨
	/** 현재 장착된 우주선 스킨 경로 */
	private String currentSpaceshipSkin = "sprites/ship.gif";
	/** 현재 장착된 무기 스킨 경로 */
	private String currentWeaponSkin = "sprites/shot.gif";

	/** 현재까지 기록된 프레임 수 */
	// FPS 표시 기능은 상위에서 처리 가능, 내부적으로는 카운트만 유지하지 않음
	
	/** 게임 상태 관리자 */
	private GameStateManager gameStateManager;
	/** 입력 관리자 */
	private InputManager inputManager;
	/** 스킬 관리자 */
	private SkillManager skillManager;
	/** UI 렌더러 */
	private UIRenderer uiRenderer;
	/** 배경 렌더러 (캐시됨) */
	private BackgroundRenderer backgroundRenderer;
	/** 공유 게임플레이 조정자 */
	private final SharedGameplayCoordinator gameplayCoordinator;
	/** Alien 엔티티 환경 */
	private final GameplayAlienEnvironment alienEnvironment;
	private final IntUnaryOperator alienHpResolver;
	// gameplay는 mainmenu 패키지에 의존하지 않도록, 오버레이는 UIRenderer에서 처리
	
	/** 메인메뉴 전환 요청 플래그 */
	private boolean requestMainMenu = false;
	/** 메뉴 네비게이션을 위한 ScreenNavigator */
	private ScreenNavigator navigator;
	/** 게임 시작 시간 (3초 공격 지연용) */
	private long gameStartTime = 0;

	/** 네트워크 어댑터 (싱글: LocalLoopback 기본) */
	private GameNetworkAdapter networkAdapter;
	
	/** 엔티티 팩토리 */
	private EntityFactory entityFactory;
	/** 게임 렌더러 */
	private GameRenderer gameRenderer;
	/** 게임 업데이터 */
	private GameUpdater gameUpdater;
	/** 아이템 적용자 */
	private ItemApplier itemApplier;
	
	/** 로거 */
	private static final Logger logger = LoggerFactory.getLogger(Game.class);
	
	/**
	 * 게임을 구성하고 실행합니다.
	 */
	public Game(ScreenNavigator navigator) {
		this.navigator = navigator;
		// 게임 시작 시간 기록
		gameStartTime = System.currentTimeMillis();
		setIgnoreRepaint(true);
		setBounds(0,0,800,600);
		setFocusable(true);
		
		// 게임 상태 관리자 초기화
		gameStateManager = new GameStateManager();
		
	// 스킬 관리자 초기화
	skillManager = new SkillManager(this);
	gameplayCoordinator = new SharedGameplayCoordinator(this);
	alienEnvironment = new GameplayAlienEnvironment(this);
	alienHpResolver = GameplayAlienEnvironment.hpResolver();

	// UI 렌더러 초기화
		uiRenderer = new UIRenderer(this);

		// 배경 및 오버레이
		backgroundRenderer = new BackgroundRenderer();
		updateBackgroundForRound(1); // 1라운드부터 시작
		
		// 입력 관리자 초기화
		inputManager = new InputManager(gameStateManager, this);
		
		// 입력 핸들러 추가 (inputManager 초기화 후)
		addKeyListener(inputManager.new KeyInputHandler());
		addMouseListener(inputManager.new MouseInputHandler());
		
		// 게임의 엔티티들을 초기화하여 시작할 때 볼 수 있는 것이 있도록 함
		logger.debug("Game constructor calling initEntities()...");
		initEntities();

		// 기본 로컬 네트워크 어댑터 설정 (멀티 환경에서는 외부에서 교체)
		this.networkAdapter = new LocalLoopbackNetworkAdapter(gameStateManager);
		
		// 책임 분리된 클래스들 초기화
		this.entityFactory = new EntityFactory(this, alienEnvironment, alienHpResolver);
		this.gameRenderer = new GameRenderer(backgroundRenderer, uiRenderer, resolutionManager);
		this.itemApplier = new ItemApplier(userManager);
		// gameUpdater는 ship이 생성된 후 초기화 (startGame 또는 initEntities 후)
	}

	
	/**
	 * ResolutionManager 설정
	 */
	public void setResolutionManager(ResolutionManager resolutionManager) {
		this.resolutionManager = resolutionManager;
	}
	
	/**
	 * 새로운 게임을 시작합니다. 기존 데이터를 모두 지우고 새로운 세트를 생성합니다.
	 */
	public void startGame() {
		logger.debug("startGame() called!");
		
		// ShopManager에 아이템들 추가 및 장착 정보 로드
		if (userManager != null && userManager.isLoggedIn()) {
			logger.debug("Game: 게임 시작 시 ShopManager 초기화");
			// ShopManager의 static 메서드로 아이템 초기화
			org.newdawn.spaceinvaders.shop.ShopManager.initializeDefaultItems(userManager.getShopManager());
			userManager.getShopManager().loadInventoryFromDB();
			userManager.getShopManager().loadEquipmentFromDB();
		}
		
		// 게임플레이 상태 초기화 및 라운드 준비
		String playerId = gameStateManager.getLocalPlayerId();
		gameplayCoordinator.startNewGame(playerId);
		ship = getShip(playerId);
		
		// 장착된 아이템 적용 (ship 생성 후)
		if (itemApplier != null) {
			itemApplier.applyEquippedItems(ship);
			// ItemApplier에서 스킨 경로 가져오기
			currentSpaceshipSkin = itemApplier.getCurrentSpaceshipSkin();
			currentWeaponSkin = itemApplier.getCurrentWeaponSkin();
		}
		
		// ship 생성 후 다시 한번 스킨 적용 (확실하게 하기 위해)
		if (ship != null && currentSpaceshipSkin != null && !currentSpaceshipSkin.equals("sprites/ship.gif")) {
			ship.changeSkin(currentSpaceshipSkin);
			logger.debug("startGame에서 최종 스킨 적용: " + currentSpaceshipSkin);
		}
		
		// GameUpdater 초기화 (ship 생성 후)
		if (gameUpdater == null) {
			gameUpdater = new GameUpdater(this, gameStateManager, skillManager, 
			                             inputManager, networkAdapter, ship, moveSpeed);
		} else {
			// ship이 변경되었으므로 GameUpdater에 업데이트
			gameUpdater.updateShipReference(ship);
		}
		
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
		logger.debug("initEntities() called for Round " + gameStateManager.getCurrentRound());
		String playerId = gameStateManager.getLocalPlayerId();
		gameplayCoordinator.initializeRound(playerId);
		ship = getShip(playerId);
		
		// ship 생성 후 스킨 적용
		if (ship != null && itemApplier != null) {
			itemApplier.applySpaceshipSkin(ship);
			currentSpaceshipSkin = itemApplier.getCurrentSpaceshipSkin();
			currentWeaponSkin = itemApplier.getCurrentWeaponSkin();
		}
		
		// GameUpdater 초기화 (ship 생성 후)
		if (gameUpdater == null && ship != null) {
			gameUpdater = new GameUpdater(this, gameStateManager, skillManager, 
			                             inputManager, networkAdapter, ship, moveSpeed);
		} else if (gameUpdater != null && ship != null) {
			// ship이 변경되었으므로 GameUpdater에 업데이트
			gameUpdater.updateShipReference(ship);
		}
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
	
	@Override
	public void notifyDeath(String ownerId) {
		notifyDeath();
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
	
	@Override
	public void notifyPlayerDamaged(String ownerId, int damage) {
		notifyPlayerDamaged(damage);
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
		if (!gameStateManager.isDamageApplicable()) {
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
	
	@Override
	public void notifyAlienKilled(String ownerId, double x, double y) {
		notifyAlienKilled();
	}

	@Override
	public void addSkillToInventory(String ownerId, int skillType, int skillValue) {
		if (skillManager != null) {
			skillManager.addSkillToInventory(skillType, skillValue);
		}
	}

	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		gameplayCoordinator.handleAlienKilled(gameStateManager.getLocalPlayerId());
	}
	
	/**
	 * Notification that an alien has been killed (with position)
	 */
	public void notifyAlienKilled(int x, int y) {
		gameplayCoordinator.handleAlienKilled(gameStateManager.getLocalPlayerId(), x, y);
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
		
		String weaponSkin = itemApplier != null ? itemApplier.getCurrentWeaponSkin() : currentWeaponSkin;
		if (skillManager.hasTripleShot()) {
			// Fire three shots in a wider spread pattern
			ShotEntity shot1 = entityFactory.createShot(weaponSkin, ship.getX()-5, ship.getY()-30, false);
			ShotEntity shot2 = entityFactory.createShot(weaponSkin, ship.getX()+10, ship.getY()-30, false);
			ShotEntity shot3 = entityFactory.createShot(weaponSkin, ship.getX()+25, ship.getY()-30, false);
			gameStateManager.getEntities().add(shot1);
			gameStateManager.getEntities().add(shot2);
			gameStateManager.getEntities().add(shot3);
		} else {
			// Fire single shot
			ShotEntity shot = entityFactory.createShot(weaponSkin, ship.getX()+10, ship.getY()-30, false);
			gameStateManager.getEntities().add(shot);
			logger.debug("Player fired shot at position: (" + (ship.getX()+10) + ", " + (ship.getY()-30) + ")");
		}
	}
	
	/**
	 * Add an alien shot to the game
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 */
	public void addAlienShot(int x, int y) {
		ShotEntity shot = entityFactory.createShot("sprites/shot.gif", x, y, true);
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
		
		ShotEntity shot = entityFactory.createShot("sprites/shot.gif", x + aimOffset, y, true);
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
		logger.debug("createSkillDrop called: type=" + skillType + ", value=" + skillValue + " at (" + x + ", " + y + ")");
		
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
		
		logger.debug("Using sprite: " + spritePath);
		ShotEntity skillDrop = entityFactory.createSkillDrop(x, y, skillType, skillValue);
		gameStateManager.getEntities().add(skillDrop);
		logger.debug("Skill drop entity added to game. Total entities: " + gameStateManager.getEntities().size());
	}
	
	
	/**
	 * Get the player's current attack power
	 * 
	 * @return The player's attack power
	 */
	@Override
	public int getPlayerAttackPower(String ownerId) {
		return getPlayerAttackPower();
	}

	public int getPlayerAttackPower() {
		return gameStateManager.getAttackPower();
	}

	@Override
	public int getPlayerShipX(String ownerId) {
		return ship != null ? ship.getX() : 0;
	}

	@Override
	public int getPlayerShipY(String ownerId) {
		return ship != null ? ship.getY() : 0;
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
	@Override
	public boolean isPlayerInvincible(String ownerId) {
		return isPlayerInvincible();
	}

	public boolean isPlayerInvincible() {
		return skillManager != null && skillManager.isInvincible();
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
			MissileEntity missile = entityFactory.createMissile(
					ship.getX() + 15,
					ship.getY(),
					targetX,
					targetY);
			gameStateManager.getEntities().add(missile);
		} catch (Exception e) {
			logger.error("Error firing missile: " + e.getMessage(), e);
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
			CoinEntity coin = entityFactory.createCoin(x, y, coinValue);
			gameStateManager.getEntities().add(coin);
			logger.debug("Coin drop created at (" + x + ", " + y + ") with value: " + coinValue);
		} catch (Exception e) {
			logger.error("Error creating coin drop: " + e.getMessage(), e);
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
			CoinDisplayEntity coinDisplay = entityFactory.createCoinDisplay(x, y, coinAmount);
			addEntity(coinDisplay);
			
			logger.debug("Coin earned display created: +" + coinAmount + " at (" + x + ", " + y + ")");
		} catch (Exception e) {
			logger.error("Error creating coin earned display: " + e.getMessage(), e);
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
		String skin = itemApplier != null ? itemApplier.getCurrentSpaceshipSkin() : currentSpaceshipSkin;
		ship = entityFactory.createPlayerShip(playerId, skin);
		return ship;
	}

	@Override
	public Entity createAlienEntity(int x, int y) {
		return entityFactory.createAlien(x, y);
	}

	@Override
	public Entity createNearEntity(int round, int index) {
		return entityFactory.createNear(round, index);
	}

	@Override
	public Entity createBossEntity(int bossRound) {
		return entityFactory.createBoss(bossRound);
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
	public void addCoins(String playerId, int amount) {
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
				logger.info("Game coins saved to DB: " + earnedCoins + " coins");
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

			if (completed) {
				saveSingleLeaderboardEntry(playTimeMs,
						userManager.getCurrentUser().getUsername());
			}

			logger.info("Play time: " + gameStateManager.getPlayTime() + " (" + playTimeMs + "ms)");
			logger.info("Game record saved: " + gameRecord.toString());
		} else {
			logger.warn("Cannot save coins: User not logged in");
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
				logger.info("Game record saved to Firebase DB: " + dbPath);
			} else {
				logger.error("Failed to save game record to Firebase DB");
			}
		} catch (Exception e) {
			logger.error("Error saving game record to DB: " + e.getMessage(), e);
		}
	}

	private void saveSingleLeaderboardEntry(long playTimeMs, String username) {
		try {
			if (userManager == null || userManager.getFirebaseDB() == null) {
				return;
			}
			List<String> names = new ArrayList<>();
			if (username != null && !username.trim().isEmpty()) {
				names.add(username.trim());
			} else if (userManager.getCurrentUser() != null) {
				names.add(userManager.getCurrentUser().getUid());
			}
			LeaderboardRecord record = new LeaderboardRecord(
					LeaderboardRecord.Mode.SINGLE,
					names,
					playTimeMs);
			boolean success = LeaderboardRepository.saveRecord(
					userManager.getFirebaseDB(),
					LeaderboardRecord.Mode.SINGLE,
					record);
			if (success) {
				logger.info("Saved single-player leaderboard entry: " + record);
			} else {
				logger.error("Failed to save single-player leaderboard entry");
			}
		} catch (Exception ex) {
			logger.error("Error saving single-player leaderboard entry: " + ex.getMessage(), ex);
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
		if (gameUpdater != null) {
			gameUpdater.update(delta);
		}
	}

	public void render(Graphics2D g) {
		if (gameRenderer != null) {
			gameRenderer.render(g, gameStateManager, skillManager);
		}
	}
	
	
	/**
	 * Getter methods for InputManager
	 */
	public ShipEntity getShip() {
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
		logger.debug("startNewGame() called!");
		startGame(); 
	}
	
	/**
	 * 엔티티 리스트 반환 (gameplay 패키지용)
	 */
	@Override
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
			ExplosionEntity explosion = entityFactory.createExplosion(x, y, radius);
			gameStateManager.getEntities().add(explosion);
		} catch (Exception e) {
			logger.error("Error creating explosion: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Create heat effect (피튀기는 효과)
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param radius Heat effect radius
	 */
	@Override
	public void createHeatEffect(int x, int y, double radius) {
		try {
			HeatEffectEntity heatEffect = entityFactory.createHeatEffect(x, y);
			gameStateManager.getEntities().add(heatEffect);
		} catch (Exception e) {
			logger.error("Error creating heat effect: " + e.getMessage(), e);
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

	@Override
	public void addScore(String playerId, int points) {
		addScore(points);
	}
	
	/**
	 * Add skill points
	 * 
	 * @param points Skill points to add
	 */
	public void addSkillPoints(int points) {
		int currentPoints = gameStateManager.getSkillPoints();
		gameStateManager.setSkillPoints(currentPoints + points);
		logger.debug("Skill points added: " + points + " (Total: " + (currentPoints + points) + ")");
	}

	@Override
	public void addSkillPoints(String playerId, int points) {
		addSkillPoints(points);
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

	@Override
	public void notifyBossDefeated(String playerId) {
		notifyBossDefeated();
	}

	/**
	 * Check if all near monsters are defeated and advance to boss round
	 */
	public void checkAllNearMonstersDefeated() {
		gameplayCoordinator.handleNearMonstersCleared(gameStateManager.getLocalPlayerId());
	}

	@Override
	public void onNearMonsterDestroyed(NearEntity nearEntity, String killerPlayerId, double killX, double killY) {
		int bossRound = Math.max(1, (nearEntity.getRound() + 1) / 2);
		addScore(100 * bossRound);
		addSkillPoints(2 + bossRound);

		double dropChance = Math.random();
		int currentRound = gameStateManager.getCurrentRound();
		if (dropChance < skillManager.getSkillDropChance(currentRound)) {
			skillManager.dropSkill(currentRound, killX, killY);
		} else if (dropChance < 0.4) {
			int bonusPoints = skillManager.getRandomSkillPoints(currentRound);
			if (bonusPoints > 0) {
				addSkillPoints(bonusPoints);
			}
		}

		showCoinEarned((int) killX, (int) killY, 5 + bossRound);
		checkAllNearMonstersDefeated();
	}

	/**
	 * Add a boss shot to the game
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public void addBossShot(int x, int y) {
		try {
			ShotEntity shot = entityFactory.createShot("sprites/shot.gif", x, y, true); // true = alien shot
			gameStateManager.getEntities().add(shot);
		} catch (Exception e) {
			logger.error("Error adding boss shot: " + e.getMessage(), e);
		}
	}
	
	@Override
	public BaseBossShotEntity createBossShot(int x, int y, double directionX, double directionY, double speed,
			int radius, boolean canSplit, double splitY, int splitCount) {
		return new BossShotEntity(this, x, y, directionX, directionY, speed, radius, canSplit, splitY, splitCount);
	}
	
	/**
	 * Set the UserManager for accessing equipped items
	 */
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
		// ItemApplier 업데이트
		if (itemApplier == null) {
			itemApplier = new ItemApplier(userManager);
		}
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
	 * Get current spaceship skin path
	 */
	public String getCurrentSpaceshipSkin() {
		return itemApplier != null ? itemApplier.getCurrentSpaceshipSkin() : currentSpaceshipSkin;
	}
	
	/**
	 * Get current weapon skin path
	 */
	public String getCurrentWeaponSkin() {
		return itemApplier != null ? itemApplier.getCurrentWeaponSkin() : currentWeaponSkin;
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
		logger.info("라운드 " + round + " 배경 변경: " + backgroundPath);
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
