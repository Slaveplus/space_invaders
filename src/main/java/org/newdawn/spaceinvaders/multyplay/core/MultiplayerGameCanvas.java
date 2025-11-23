package org.newdawn.spaceinvaders.multyplay.core;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.MediaTracker;
import java.awt.Toolkit;
// no direct AWT listeners here; handled via MultiplayerInputManager
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEntity;
import org.newdawn.spaceinvaders.common.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.EntitySnapshot;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.ShotEntity;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.effect.HeatEffectEntity;
import org.newdawn.spaceinvaders.common.entity.near.NearEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEntity;
import org.newdawn.spaceinvaders.common.entity.ui.CoinDisplayEntity;
import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import org.newdawn.spaceinvaders.multyplay.entity.MultiplayerMissileEnvironment;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.database.GameRecord;
import org.newdawn.spaceinvaders.shop.ShopCategory;
import org.newdawn.spaceinvaders.shop.ShopItem;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.multyplay.input.MultiplayerInputManager;
import org.newdawn.spaceinvaders.multyplay.net.GameEvent;
import org.newdawn.spaceinvaders.multyplay.net.GameNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;
import org.newdawn.spaceinvaders.multyplay.net.LocalLoopbackNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.state.PlayerState;
import org.newdawn.spaceinvaders.multyplay.ui.MultiplayerUIRenderer;
import org.newdawn.spaceinvaders.room.GameInitInfo;
import org.newdawn.spaceinvaders.multyplay.net.client.RoomGameNetworkAdapter;

/**
 * 게임의 메인 훅입니다. 이 클래스는 디스플레이 관리자와
 * 게임 로직의 중앙 중재자 역할을 모두 수행합니다.
 * 
 * 디스플레이 관리는 게임의 모든 엔티티를 순환하면서 이동을 요청하고
 * 적절한 위치에 그리는 루프로 구성됩니다. 내부 클래스의 도움으로
 * 플레이어가 메인 우주선을 제어할 수 있습니다.
 * 
 * 중재자로서 게임 내 엔티티가 이벤트를 감지하면
 * (예: 외계인 처치, 플레이어 사망) 알림을 받고 적절한 게임 액션을 취합니다.
 * 
 * @author Kevin Glass
 */
public class MultiplayerGameCanvas extends Canvas implements Screen, MultiplayerGameContext
{
	private static final String REMOTE_PLAYER_LABEL = "클라이언트: 원격 플레이어 ";
	private static final String FONT_ARIAL = "Arial";
	private static final String RADIUS_KEY = "radius";
	private static final String GAME_COMPLETED_MESSAGE = "🏆 GAME COMPLETED! 🏆 Congratulations!";

	/** 가속 페이지 플리핑을 사용할 수 있게 해주는 전략 */
	// BufferStrategy는 상위 App에서 관리
	// entities and removeList are now managed by MultiplayerGameStateManager
	/** 플레이어를 나타내는 엔티티 */
	private transient ShipEntity ship;
	/** 플레이어 우주선이 이동해야 하는 속도 (픽셀/초) */
	private double moveSpeed = 300;
	/** 장착된 아이템에 접근하기 위한 UserManager */
	private transient UserManager userManager;
	/** 해상도 스케일링을 처리하기 위한 ResolutionManager */
	private transient ResolutionManager resolutionManager;
	// lastFire and firingInterval are now managed by MultiplayerGameStateManager
	/** 현재 장착된 우주선 스킨 경로 */
	private String currentSpaceshipSkin = SpriteConstants.SHIP_GIF;
	/** 현재 장착된 무기 스킨 경로 */
	private String currentWeaponSkin = SpriteConstants.SHOT_GIF;
	
	// 플레이어별 스킨 정보 저장 (클라이언트)
	private final Map<String, String> remotePlayerSkins = new HashMap<>();

	/** The current number of frames recorded */
	// FPS 표시 기능은 상위에서 처리 가능, 내부적으로는 카운트만 유지하지 않음
	
	/** The game state manager */
	private transient MultiplayerGameStateManager gameStateManager;
	/** The input manager */
	private transient MultiplayerInputManager inputManager;
	/** The skill manager */
	private transient MultiplayerSkillManager skillManager;
	/** The UI renderer */
	private transient MultiplayerUIRenderer uiRenderer;
	/** Background renderer (cached) */
	private transient BackgroundRenderer backgroundRenderer;
	// gameplay는 mainmenu 패키지에 의존하지 않도록, 오버레이는 MultiplayerUIRenderer에서 처리
	
	/** 메인메뉴 전환 요청 플래그 */
	private boolean requestMainMenu = false;

	/** 네트워크 어댑터 (싱글: LocalLoopback 기본) */
	private transient GameNetworkAdapter networkAdapter;
	private boolean remoteMode = false;
	private String localPlayerId;
	private long inputSequence;
	private long lastInputSendTime;
	private boolean lastSentLeft;
	private boolean lastSentRight;
	private boolean lastSentFire;
	private final Map<Long, Entity> remoteEntities = new HashMap<>();
	private final ArrayList<Entity> snapshotEntitiesBuffer = new ArrayList<>();
	private GameSnapshot.Phase remotePhase = GameSnapshot.Phase.ACTIVE;
	private boolean remoteWaitingForPlayers;
	private final Map<String, Boolean> remoteReadyStates = new LinkedHashMap<>();
	private boolean resultsPersisted = false;
	private final Map<String, String> playerDisplayNames = new LinkedHashMap<>();
	private final Deque<String> intermissionChatLines = new ArrayDeque<>();
	private final List<CoinDisplayEntity> remoteCoinPopups = new ArrayList<>();
	private String intermissionChatInput = "";
	private boolean intermissionChatFocus = false;
	private boolean localReady = false;
	private String intermissionMessage = "";
	private boolean localSpectating = false;
	private boolean requestLobbyReturn = false;
	
	/**
	 * Construct our game and set it running.
	 */
	public MultiplayerGameCanvas() {
		initCanvas();
		initComponents();
		initInputHandlers();
		initializeEntityFactories();
		initEntities();
	}

	private void initCanvas() {
		setIgnoreRepaint(true);
		setBounds(0,0,800,600);
		setFocusable(true);
	}

	private void initComponents() {
		// initialize the game state manager
		gameStateManager = new MultiplayerGameStateManager();
		localPlayerId = gameStateManager.getLocalPlayerId();
		gameStateManager.ensurePlayer(localPlayerId);
		gameStateManager.setLocalPlayerId(localPlayerId);
		
		// initialize the skill manager
		skillManager = new MultiplayerSkillManager(this, localPlayerId);
			
		// initialize the UI renderer
		uiRenderer = new MultiplayerUIRenderer(this);

		// background & overlays
		backgroundRenderer = new BackgroundRenderer(SpriteConstants.BACKGROUND_2_JPG);
		
		// initialize the input manager
		inputManager = new MultiplayerInputManager(gameStateManager, this);
		
		// 기본 로컬 네트워크 어댑터 설정 (멀티 환경에서는 외부에서 교체)
		this.networkAdapter = new LocalLoopbackNetworkAdapter(gameStateManager);
	}

	private void initInputHandlers() {
		// add input handlers (after inputManager is initialized)
		addKeyListener(inputManager.new KeyInputHandler());
		addMouseListener(inputManager.new MouseInputHandler());
	}

	public void setNetworkAdapter(GameNetworkAdapter adapter) {
		this.networkAdapter = adapter;
	}

	public void configureForRemote(GameNetworkAdapter adapter, String localPlayerId, GameInitInfo initInfo) {
		setNetworkAdapter(adapter);
		this.remoteMode = adapter != null && adapter.getMode() != GameNetworkAdapter.Mode.LOCAL;
		this.localPlayerId = localPlayerId;
		this.inputSequence = 0;
		this.lastInputSendTime = 0;
		this.lastSentLeft = false;
		this.lastSentRight = false;
		this.lastSentFire = false;
		this.remoteEntities.clear();
		this.snapshotEntitiesBuffer.clear();
		this.remoteReadyStates.clear();
		this.playerDisplayNames.clear();
		this.intermissionChatLines.clear();
		this.intermissionChatInput = "";
		this.localSpectating = false;
		this.intermissionChatFocus = false;
		this.localReady = false;
		this.intermissionMessage = "";
		this.remotePhase = GameSnapshot.Phase.ACTIVE;
		this.remoteWaitingForPlayers = false;
		this.resultsPersisted = false;
		gameStateManager.getEntities().clear();
		gameStateManager.getRemoveList().clear();
		remoteCoinPopups.clear();
		gameStateManager.setWaitingForKeyPress(false);
		if (initInfo != null && initInfo.players != null) {
			for (GameInitInfo.Player p : initInfo.players) {
				gameStateManager.ensurePlayer(p.id);
				playerDisplayNames.put(p.id, p.username != null && !p.username.isEmpty() ? p.username : p.id);
				if (this.localPlayerId == null) {
					this.localPlayerId = p.id;
				}
			}
		} else if (this.localPlayerId == null) {
			this.localPlayerId = "local";
			playerDisplayNames.put(this.localPlayerId, this.localPlayerId);
			gameStateManager.ensurePlayer(this.localPlayerId);
		}
		if (this.localPlayerId != null && !playerDisplayNames.containsKey(this.localPlayerId)) {
			playerDisplayNames.put(this.localPlayerId, this.localPlayerId);
		}
		gameStateManager.setLocalPlayerId(this.localPlayerId);
		skillManager.setOwnerId(this.localPlayerId);
		ship = null;
	}

	public void shutdownNetwork() {
		if (networkAdapter != null) {
			networkAdapter.shutdown();
		}
	}

	public boolean isRemoteSession() {
		return remoteMode;
	}

	public void sendSkillActivationRequest(int skillType) {
		if (!remoteMode || networkAdapter == null) {
			return;
		}
		GameEvent event = new GameEvent(GameEvent.Type.SKILL_REQUEST,
				localPlayerId,
				Integer.toString(skillType),
				System.currentTimeMillis());
		networkAdapter.sendEvent(event);
	}

	public void sendSkillUpgradeRequest(int upgradeType) {
		if (!remoteMode || networkAdapter == null) {
			return;
		}
		GameEvent event = new GameEvent(GameEvent.Type.SKILL_UPGRADE,
				localPlayerId,
				Integer.toString(upgradeType),
				System.currentTimeMillis());
		networkAdapter.sendEvent(event);
	}

	public void requestReturnToLobby() {
		requestLobbyReturn = true;
	}

	public boolean isRequestingLobbyReturn() {
		return requestLobbyReturn;
	}

	public void resetLobbyReturnRequest() {
		requestLobbyReturn = false;
	}

	@Override
	public MultiplayerGameStateManager getGameStateManager() {
		return gameStateManager;
	}

	@Override
	public MultiplayerSkillManager getSkillManager(String playerId) {
		return skillManager;
	}
	
	/**
	 * ResolutionManager 설정
	 */
	public void setResolutionManager(ResolutionManager resolutionManager) {
		this.resolutionManager = resolutionManager;
	}

	/**
	 * Overload: single-player 패키지의 ResolutionManager도 허용
	 */
	public void setResolutionManager(org.newdawn.spaceinvaders.gameplay.ResolutionManager resolutionManager) {
		if (resolutionManager == null) {
			this.resolutionManager = null;
			return;
		}
		// 서로 동일한 기능을 가지므로 값을 복사하여 새 멀티 전용 매니저 생성
		ResolutionManager rm = new ResolutionManager();
		rm.setResolution(resolutionManager.getCurrentWidth(), resolutionManager.getCurrentHeight());
		this.resolutionManager = rm;
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	public void startGame() {
		// 게임플레이 상태 초기화 (entities.clear() 포함)
		gameStateManager.startNewGame();
		
		// ShopManager에 아이템들 추가 및 장착 정보 로드
		if (userManager != null && userManager.isLoggedIn()) {
			// ShopManager의 static 메서드로 아이템 초기화
			org.newdawn.spaceinvaders.shop.ShopManager.initializeDefaultItems(userManager.getShopManager());
			userManager.getShopManager().loadInventoryFromDB();
			userManager.getShopManager().loadEquipmentFromDB();
		}
		
		// 장착된 아이템 적용 (ShopManager 초기화 후, ship 생성 전)
		applyEquippedItems();
		
		// 엔티티 초기화 (스킨 적용 후)
		initEntities();
		
		// ship 생성 후 다시 한번 스킨 적용 (확실하게 하기 위해)
		if (ship != null && currentSpaceshipSkin != null && !currentSpaceshipSkin.equals(SpriteConstants.SHIP_GIF)) {
			ship.changeSkin(currentSpaceshipSkin);
			System.out.println("멀티플레이어 startGame에서 최종 스킨 적용: " + currentSpaceshipSkin);
		}
		
		// 서버에 스킨 정보 전송 (지연 후 전송)
		sendSkinToServerDelayed();
		
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
		if (remoteMode) {
			return;
		}

		ArrayList<Entity> entities = gameStateManager.getEntities();
		entities.clear();
		gameStateManager.getRemoveList().clear();

		ship = createPlayerShip(localPlayerId);
		if (ship != null) {
			ship.setOwnerId(localPlayerId);
			entities.add(ship);
			
			// ship 생성 후 스킨 적용
			if (currentSpaceshipSkin != null && !currentSpaceshipSkin.equals(SpriteConstants.SHIP_GIF)) {
				ship.changeSkin(currentSpaceshipSkin);
				System.out.println("멀티플레이어 initEntities에서 스킨 적용: " + currentSpaceshipSkin);
			}
		}

		SharedMultiplayerRoundCoordinator.setupRound(this, gameStateManager.getCurrentRound());
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
	@Override
	public void notifyDeath(String playerId) {
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();

		if (targetId != null && targetId.equals(gameStateManager.getLocalPlayerId()) && skillManager.isInvincible()) {
			return;
		}

		gameStateManager.takeDamage(targetId);
		PlayerState targetState = gameStateManager.ensurePlayer(targetId);
		if (targetId != null && targetId.equals(gameStateManager.getLocalPlayerId()) && targetState.isDead()) {
			gameStateManager.setMessage("Oh no! They got you, try again?");
			gameStateManager.setWaitingForKeyPress(true);
		}
	}

	@Override
	public boolean canEnemiesAttack() {
		long startTime = gameStateManager.getGameStartTime();
		if (startTime <= 0) {
			return false;
		}
		return System.currentTimeMillis() - startTime >= 3000;
	}

	@Override
	public void notifyPlayerDamaged(String playerId, int damage) {
		if (damage <= 0) {
			return;
		}
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
		if (targetId == null) {
			return;
		}
		if (targetId.equals(gameStateManager.getLocalPlayerId()) && skillManager.isInvincible()) {
			return;
		}
		if (remoteMode) {
			return;
		}

		PlayerState targetState = gameStateManager.ensurePlayer(targetId);
		for (int i = 0; i < damage; i++) {
			gameStateManager.takeDamage(targetId);
			if (targetState.isDead()) {
				break;
			}
		}
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		if (remoteMode) {
			return;
		}

		boolean roundAdvanced = gameStateManager.advanceRound();
		if (roundAdvanced) {
			SharedMultiplayerRoundCoordinator.setupRound(this, gameStateManager.getCurrentRound());
		} else {
			gameStateManager.setMessage(GAME_COMPLETED_MESSAGE);
			gameStateManager.setWaitingForKeyPress(true);
			persistMultiplayerResults(true);
		}
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	@Override
	public void notifyAlienKilled(String killerPlayerId, double killX, double killY) {
		if (remoteMode) {
			return;
		}

		String targetId = killerPlayerId != null ? killerPlayerId : gameStateManager.getLocalPlayerId();

		// Give random skill points for killing aliens
		int earnedPoints = skillManager.getRandomSkillPoints(gameStateManager.getCurrentRound());
		PlayerState ps = gameStateManager.ensurePlayer(targetId);
		ps.addSkillPoints(earnedPoints);

		// Random chance to drop a skill (world drop, not player-specific)
		double dropChance = skillManager.getSkillDropChance(gameStateManager.getCurrentRound());
		if (Math.random() < dropChance) {
			skillManager.dropSkill(gameStateManager.getCurrentRound(), killX, killY);
		}
		
		// Count remaining aliens dynamically (excluding those marked for removal)
		int remainingAliens = 0;
		ArrayList<Entity> entities = gameStateManager.getEntities();
		ArrayList<Entity> removeList = gameStateManager.getRemoveList();
		
		for (Entity entity : entities) {
			if (entity instanceof AlienEntity && !removeList.contains(entity)) {
				remainingAliens++;
			}
		}
		
		BossEntity boss = getBoss();
		boolean bossAlive = boss != null && !removeList.contains(boss);
		if (remainingAliens == 0 && !bossAlive) {
			notifyWin();
		}
		
		// Speed up remaining aliens
		for (Entity entity : entities) {
			if (entity instanceof AlienEntity) {
				// speed up by 1.5% + round-based bonus (more gradual increase)
				double speedMultiplier = 1.015 + (gameStateManager.getCurrentRound() * 0.01);
				entity.setHorizontalMovement(entity.getHorizontalMovement() * speedMultiplier);
				entity.setVerticalMovement(entity.getVerticalMovement() * speedMultiplier);
			}
		}
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		if (ship == null) {
			return;
		}
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
			String ownerId = localPlayerId != null ? localPlayerId : gameStateManager.getLocalPlayerId();
			shot1.setOwnerId(ownerId);
			shot2.setOwnerId(ownerId);
			shot3.setOwnerId(ownerId);
			gameStateManager.getEntities().add(shot1);
			gameStateManager.getEntities().add(shot2);
			gameStateManager.getEntities().add(shot3);
		} else {
			// Fire single shot
			ShotEntity shot = new ShotEntity(this, currentWeaponSkin, ship.getX()+10, ship.getY()-30);
			String ownerId = localPlayerId != null ? localPlayerId : gameStateManager.getLocalPlayerId();
			shot.setOwnerId(ownerId);
			gameStateManager.getEntities().add(shot);
		}
	}
	
	/**
	 * Add an alien shot to the game
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 */
	public void addAlienShot(int x, int y) {
		ShotEntity shot = new ShotEntity(this, SpriteConstants.SHOT_GIF, x, y, true);
		gameStateManager.getEntities().add(shot);
	}

	@Override
	public void onNearMonsterDestroyed(NearEntity nearEntity, String killerPlayerId, double killX, double killY) {
		if (remoteMode) {
			return;
		}

		boolean cleared = SharedMultiplayerRoundCoordinator.handleNearMonsterDestroyed(
				this, nearEntity, killerPlayerId, killX, killY);
		if (cleared) {
			checkAllNearMonstersDefeated();
		}
	}
	
	/**
	 * Add an alien shot with slight aim adjustment towards player
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 * @param alienX The x location of the alien firing
	 */
	@Override
	public void addAimedAlienShot(int x, int y, int alienX) {
		// Add more accurate horizontal adjustment towards player (within 15 pixel range)
		int playerX = ship.getX() + 10; // Player center
		int aimOffset = (int)((playerX - alienX) * 0.15); // 15% of distance towards player
		aimOffset = Math.max(-15, Math.min(15, aimOffset)); // Clamp to player-sized range
		
		ShotEntity shot = new ShotEntity(this, SpriteConstants.SHOT_GIF, x + aimOffset, y, true);
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
		// Create skill drop using ShotEntity with skill drop functionality
		ShotEntity skillDrop = new ShotEntity(this, SpriteConstants.SHOT_GIF, x, y, skillType, skillValue);
		gameStateManager.getEntities().add(skillDrop);
	}

	@Override
	public void addCoins(String playerId, int amount) {
		if (amount == 0) {
			return;
		}
		if (playerId == null) {
			playerId = gameStateManager.getLocalPlayerId();
		}
		if (playerId == null) {
			return;
		}
		gameStateManager.addCoins(playerId, amount);
	}

	@Override
	public void showCoinEarned(String playerId, int x, int y, int coinAmount) {
		String localId = gameStateManager.getLocalPlayerId();
		if (coinAmount <= 0) {
			return;
		}
		if (playerId != null && localId != null && !playerId.equals(localId)) {
			return;
		}
		if (remoteMode) {
			if (localId == null) {
				return;
			}
			CoinDisplayEntity display = new CoinDisplayEntity(this, x, y, coinAmount);
			remoteCoinPopups.add(display);
			return;
		}
		spawnCoinPopup(x, y, coinAmount);
	}

	private void spawnCoinPopup(int x, int y, int coinAmount) {
		CoinDisplayEntity display = new CoinDisplayEntity(this, x, y, coinAmount);
		addEntity(display);
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
	@Override
	public int getPlayerAttackPower(String playerId) {
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
		PlayerState ps = gameStateManager.ensurePlayer(targetId);
		return ps.getAttackPower();
	}
	
	/**
	 * Get the current round number
	 * 
	 * @return The current round
	 */
	@Override
	public int getCurrentRound() {
		return gameStateManager.getCurrentRound();
	}
	
	/**
	 * Check if player is currently invincible
	 */
	@Override
	public boolean isPlayerInvincible(String playerId) {
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
		if (targetId != null && targetId.equals(gameStateManager.getLocalPlayerId())) {
			return skillManager.isInvincible();
		}
		return false;
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
	@Override
	public void addSkillToInventory(String playerId, int skillType, int skillValue) {
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
		if (targetId != null && targetId.equals(gameStateManager.getLocalPlayerId())) {
			skillManager.addSkillToInventory(skillType, skillValue);
		}
	}
	
	/**
	 * Fire missile at a target location
	 * 
	 * @param targetX The target x location
	 * @param targetY The target y location
	 */
	@Override
	public void fireMissile(String playerId, double targetX, double targetY) {
		try {
			ShipEntity source = getShip(playerId);
			if (source == null) {
				return;
			}
			MissileEntity missile = new MissileEntity(
					new MultiplayerMissileEnvironment(this),
					SpriteConstants.SKILL_MISSILE_PNG,
					(int) source.getX() + 15,
					(int) source.getY(),
					targetX,
					targetY);
			if (playerId != null) {
				missile.setOwnerId(playerId);
			}
			gameStateManager.getEntities().add(missile);
		} catch (Exception e) {
			System.err.println("Error firing missile: " + e.getMessage());
			e.printStackTrace();
		}
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
		long now = System.currentTimeMillis();
		if (networkAdapter != null) {
			networkAdapter.tick(now);
		}

		if (remoteMode) {
			updateRemote(now, delta);
		} else {
			updateLocal(delta);
		}
	}

	private void updateRemote(long now, long delta) {
		handleRemoteInput(now);
		GameSnapshot snapshot = networkAdapter != null ? networkAdapter.pollLatestSnapshot() : null;
		if (snapshot != null) {
			applyRemoteSnapshot(snapshot);
		}
		if (networkAdapter != null) {
			List<GameEvent> events = networkAdapter.drainEvents();
			if (events != null && !events.isEmpty()) {
				handleRemoteEvents(events);
			}
		}
		updateRemoteCoinPopups(delta);
		skillManager.updateSkillEffects();
	}

	private void updateLocal(long delta) {
		if (gameStateManager.isGamePaused()) {
			return;
		}

		moveEntities(delta);
		updateShipReference();
		updateShipMovement();
		checkCollisions();
		cleanupEntities();

		if (gameStateManager.isLogicRequiredThisLoop()) {
			for (Entity e : gameStateManager.getEntities()) {
				e.doLogic();
			}
			gameStateManager.setLogicRequiredThisLoop(false);
		}
	}

	private void moveEntities(long delta) {
		skillManager.updateSkillEffects();
		for (Entity entity : new ArrayList<>(gameStateManager.getEntities())) {
			entity.move(delta);
		}
		tryAlienFire();
	}

	private void updateShipReference() {
		if (localPlayerId == null) {
			return;
		}
		if (ship != null && gameStateManager.getEntities().contains(ship)) {
			return;
		}
		
		ShipEntity candidate = getShip(localPlayerId);
		if (candidate != null) {
			ship = candidate;
		}
	}

	private void updateShipMovement() {
		if (ship != null) {
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
	}

	private void checkCollisions() {
		ArrayList<Entity> entities = gameStateManager.getEntities();
		for (int i = 0; i < entities.size(); i++) {
			for (int j = i + 1; j < entities.size(); j++) {
				Entity e1 = entities.get(i);
				Entity e2 = entities.get(j);
				if (e1.collidesWith(e2)) {
					e1.collidedWith(e2);
					e2.collidedWith(e1);
				}
			}
		}
	}

	private void cleanupEntities() {
		gameStateManager.getEntities().removeAll(gameStateManager.getRemoveList());
		gameStateManager.getRemoveList().clear();
	}


	private void handleRemoteInput(long now) {
		if (networkAdapter == null || localPlayerId == null) {
			return;
		}
		if (remotePhase != GameSnapshot.Phase.ACTIVE) {
			lastSentLeft = false;
			lastSentRight = false;
			lastSentFire = false;
			return;
		}
		if (isLocalSpectatorActive()) {
			lastSentLeft = false;
			lastSentRight = false;
			lastSentFire = false;
			return;
		}
		boolean left = inputManager.isLeftPressed();
		boolean right = inputManager.isRightPressed();
		boolean fire = inputManager.isFirePressed();
		boolean changed = left != lastSentLeft || right != lastSentRight || fire != lastSentFire;
		if (changed || now - lastInputSendTime >= 100) {
			PlayerInput input = new PlayerInput(localPlayerId, left, right, fire, now, (int) (++inputSequence));
			networkAdapter.sendInput(input);
			lastInputSendTime = now;
			lastSentLeft = left;
			lastSentRight = right;
			lastSentFire = fire;
		}
	}

	private void applyRemoteSnapshot(GameSnapshot snapshot) {
		if (snapshot == null) {
			return;
		}
		if (gameStateManager.getGameStartTime() == 0) {
			gameStateManager.setGameStartTime(System.currentTimeMillis());
		}

		boolean localShipVisible = applyEntitySnapshots(snapshot);
		applyPlayerStateSnapshots(snapshot);
		applyGamePhaseAndState(snapshot);
		updateLocalPlayerStatus(localShipVisible);

		if (!resultsPersisted && remotePhase == GameSnapshot.Phase.COMPLETED) {
			persistMultiplayerResults(false);
		}
	}

	private boolean applyEntitySnapshots(GameSnapshot snapshot) {
		Map<Long, Entity> next = new HashMap<>();
		snapshotEntitiesBuffer.clear();
		Entity localShipCandidate = null;
		boolean localShipVisible = false;

		for (EntitySnapshot snap : snapshot.entities) {
			Entity entity = processEntitySnapshot(snap);
			next.put(snap.id, entity);
			snapshotEntitiesBuffer.add(entity);
			if (localPlayerId != null && localPlayerId.equals(snap.ownerId) && "ShipEntity".equals(snap.type)) {
				localShipCandidate = entity;
				localShipVisible = true;
			}
		}

		remoteEntities.clear();
		remoteEntities.putAll(next);
		gameStateManager.getEntities().clear();
		gameStateManager.getEntities().addAll(snapshotEntitiesBuffer);
		gameStateManager.getRemoveList().clear();
		onRoundBackgroundChanged(snapshot.round);

		if (localShipCandidate instanceof ShipEntity) {
			ship = (ShipEntity) localShipCandidate;
		} else if (ship != null && !remoteEntities.containsValue(ship)) {
			ship = null;
		}

		return localShipVisible;
	}

	private Entity processEntitySnapshot(EntitySnapshot snap) {
		Entity entity = remoteEntities.get(snap.id);
		if (entity == null) {
			entity = createRemoteEntity(snap);
		}
		entity.applySnapshot(snap);

		if (entity instanceof ShipEntity && snap.ownerId != null && !snap.ownerId.equals(localPlayerId)) {
			String savedSkin = remotePlayerSkins.get(snap.ownerId);
			if (savedSkin != null) {
				((ShipEntity) entity).changeSkin(savedSkin);
			} else {
				applyRemotePlayerSkin((ShipEntity) entity, snap.ownerId);
			}
		}
		return entity;
	}

	private void applyPlayerStateSnapshots(GameSnapshot snapshot) {
		if (snapshot.players == null) {
			return;
		}
		for (Map.Entry<String, GameSnapshot.PlayerScalarState> entry : snapshot.players.entrySet()) {
			PlayerState ps = gameStateManager.ensurePlayer(entry.getKey());
			GameSnapshot.PlayerScalarState state = entry.getValue();
			ps.setCurrentHP(state.hp);
			ps.setMaxHP(state.maxHp);
			ps.setAttackPower(state.atk);
			ps.setAttackSpeed(state.aspd);
			ps.setSkillPoints(state.skillPts);
			ps.setEarnedCoins(state.coins);

			if (entry.getKey() != null && entry.getKey().equals(localPlayerId)) {
				updateLocalPlayerSkills(state);
			}
			playerDisplayNames.putIfAbsent(entry.getKey(), entry.getKey());
		}
	}

	private void updateLocalPlayerSkills(GameSnapshot.PlayerScalarState state) {
		gameStateManager.setEarnedCoins(state.coins);
		skillManager.setInvincibleSkills(state.invincibleCharges);
		skillManager.setTripleShotSkills(state.tripleShotCharges);
		skillManager.setMissileSkills(state.missileCharges);
		skillManager.setAttackPowerLevel(state.attackPowerLevel);
		skillManager.setAttackSpeedLevel(state.attackSpeedLevel);
		skillManager.setHpUpLevel(state.hpUpLevel);
		long now = System.currentTimeMillis();
		skillManager.setInvincible(state.invincibleRemainingMs > 0);
		skillManager.setInvincibleEndTime(state.invincibleRemainingMs > 0 ? now + state.invincibleRemainingMs : 0);
		skillManager.setHasTripleShot(state.tripleShotRemainingMs > 0);
		skillManager.setTripleShotEndTime(state.tripleShotRemainingMs > 0 ? now + state.tripleShotRemainingMs : 0);
	}

	private void applyGamePhaseAndState(GameSnapshot snapshot) {
		gameStateManager.setCurrentRound(snapshot.round);
		remotePhase = snapshot.phase != null ? snapshot.phase : GameSnapshot.Phase.ACTIVE;
		remoteWaitingForPlayers = snapshot.waitingForPlayers;
		intermissionMessage = snapshot.message != null ? snapshot.message : "";
		remoteReadyStates.clear();
		remoteReadyStates.putAll(snapshot.readyStates != null ? snapshot.readyStates : Collections.emptyMap());

		if (localPlayerId != null && !remoteReadyStates.containsKey(localPlayerId)) {
			remoteReadyStates.put(localPlayerId, localReady);
		}
		if (localPlayerId != null) {
			Boolean serverReady = remoteReadyStates.get(localPlayerId);
			if (serverReady != null) {
				localReady = serverReady;
			}
		}

		if (remotePhase != GameSnapshot.Phase.INTERMISSION) {
			intermissionChatFocus = false;
			intermissionChatInput = "";
			if (remotePhase == GameSnapshot.Phase.ACTIVE) {
				localReady = false;
			}
		}

		boolean intermission = remotePhase == GameSnapshot.Phase.INTERMISSION;
		boolean waiting = intermission || remoteWaitingForPlayers;
		gameStateManager.setRoundTransition(intermission);
		gameStateManager.setWaitingForKeyPress(waiting);
	}

	private void updateLocalPlayerStatus(boolean localShipVisible) {
		if (localPlayerId != null && remotePhase == GameSnapshot.Phase.ACTIVE) {
			PlayerState localState = gameStateManager.getPlayerState(localPlayerId);
			boolean reportedDead = localState != null && localState.isDead();
			localSpectating = reportedDead && !localShipVisible;
		} else {
			localSpectating = false;
		}
	}

	private final Map<String, java.util.function.Function<EntitySnapshot, Entity>> entityFactories = new HashMap<>();

	private void initializeEntityFactories() {
		entityFactories.put("ShotEntity", snapshot -> new RemoteShotEntity(snapshot, MetadataCodec.decode(snapshot.metadata)));
		entityFactories.put("ExplosionEntity", snapshot -> new RemoteExplosionEntity(snapshot, MetadataCodec.decode(snapshot.metadata)));
		entityFactories.put("IceAttack", RemoteIceAttack::new);
		entityFactories.put("IceBallAttack", RemoteIceBallAttack::new);
		entityFactories.put("MagneticFieldEntity", snapshot -> new RemoteMagneticField(snapshot, MetadataCodec.decode(snapshot.metadata)));
		entityFactories.put("Round2LaserAttack", RemoteRound2Laser::new);
		entityFactories.put("Round2Phase1Attack", RemoteRound2Phase1::new);
		entityFactories.put("Round2Phase2Attack", RemoteRound2Phase2::new);
		entityFactories.put("Round2RandomAttack", RemoteRound2Random::new);
		entityFactories.put("Round2QuadAttack", RemoteRound2Quad::new);
		entityFactories.put("Round2MachineGunAttack", RemoteRound2MachineGun::new);
		entityFactories.put("Round3StraightAttack", RemoteRound3Straight::new);
		entityFactories.put("Round3RandomAttack", RemoteRound3Random::new);
		entityFactories.put("Round3PullAttack", RemoteRound3Pull::new);
		entityFactories.put("Round3BlackHoleAttack", RemoteRound3BlackHole::new);
		entityFactories.put("Round4HealAttack", RemoteRound4Heal::new);
		entityFactories.put("Round4GreenSphereAttack", RemoteRound4GreenSphere::new);
		entityFactories.put("Round4PlayerLineAttack", RemoteRound4PlayerLine::new);
		entityFactories.put("BossEntity", snapshot -> new RemoteBossEntity(snapshot, MetadataCodec.decode(snapshot.metadata)));
		entityFactories.put("BossShotEntity", snapshot -> new RemoteBossShotEntity(snapshot, MetadataCodec.decode(snapshot.metadata)));
		entityFactories.put("HeatEffectEntity", snapshot -> new RemoteHeatEffectEntity(snapshot.x, snapshot.y));
		entityFactories.put("NearEntity", this::createNearEntityFromSnapshot);
		entityFactories.put("AlienEntity", this::createAlienEntityFromSnapshot);
	}

	private Entity createNearEntityFromSnapshot(EntitySnapshot snapshot) {
		Map<String, String> meta = MetadataCodec.decode(snapshot.metadata);
		int round = safeParseInt(meta.getOrDefault("round", "1"), 1);
		int monster = safeParseInt(meta.getOrDefault("monster", "0"), 0);
		NearEntity near = new NearEntity(this, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y), round, monster);
		near.setOwnerId(snapshot.ownerId);
		return near;
	}

	private Entity createAlienEntityFromSnapshot(EntitySnapshot snapshot) {
		String spritePath = snapshot.sprite != null && !snapshot.sprite.isEmpty()
				? snapshot.sprite
				: SpriteConstants.BOSS_1_NEAR_PNG;
		return new RemoteAlienEntity(spritePath, snapshot.x, snapshot.y);
	}

	private Entity createDefaultEntity(EntitySnapshot snapshot) {
		String type = snapshot.type != null ? snapshot.type : "";
		String spritePath = snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : null;
		if (spritePath == null || spritePath.isEmpty()) {
			if ("ShipEntity".equals(type)) {
				spritePath = currentSpaceshipSkin;
			} else if ("AlienEntity".equals(type)) {
				spritePath = SpriteConstants.BOSS_1_NEAR_PNG;
			} else {
				spritePath = currentWeaponSkin;
			}
		}
		return new RemoteSpriteEntity(spritePath, snapshot.x, snapshot.y);
	}

	private Entity createRemoteEntity(EntitySnapshot snapshot) {
		String type = snapshot.type != null ? snapshot.type : "";
		java.util.function.Function<EntitySnapshot, Entity> factory = entityFactories.get(type);
		if (factory != null) {
			return factory.apply(snapshot);
		}
		return createDefaultEntity(snapshot);
	}

	private static Image loadAnimatedImage(String path) {
		try {
			URL url = MultiplayerGameCanvas.class.getClassLoader().getResource(path);
			if (url == null) {
				return null;
			}
			Image image = Toolkit.getDefaultToolkit().createImage(url);
			if (image == null) {
				return null;
			}
			MediaTracker tracker = new MediaTracker(new Canvas());
			tracker.addImage(image, 0);
			try {
				tracker.waitForAll();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
			if (tracker.isErrorAny()) {
				return null;
			}
			return image;
		} catch (Exception e) {
			return null;
		}
	}

	private class RemoteAlienEntity extends Entity {
		RemoteAlienEntity(String spritePath, double centerX, double centerY) {
			super(spritePath, (int) Math.round(centerX), (int) Math.round(centerY));
			this.x = centerX;
			this.y = centerY;
		}

		private double currentScale() {
			int round = gameStateManager.getCurrentRound();
			return (round == 1) ? 0.18 : 0.75;
		}

		@Override
		public java.awt.Rectangle getBounds() {
			double scale = currentScale();
			int scaledWidth = (int) Math.round(sprite.getWidth() * scale);
			int scaledHeight = (int) Math.round(sprite.getHeight() * scale);
			int topLeftX = (int) Math.round(x) - (scaledWidth / 2);
			int topLeftY = (int) Math.round(y) - (scaledHeight / 2);
			return new java.awt.Rectangle(topLeftX, topLeftY, scaledWidth, scaledHeight);
		}

		@Override
		public void draw(Graphics g) {
			if (sprite == null) {
				return;
			}
			Graphics2D g2d = (Graphics2D) g;
			double scale = currentScale();
			int scaledWidth = (int) Math.round(sprite.getWidth() * scale);
			int scaledHeight = (int) Math.round(sprite.getHeight() * scale);
			int drawX = (int) Math.round(x) - (scaledWidth / 2);
			int drawY = (int) Math.round(y) - (scaledHeight / 2);
			g2d.drawImage(sprite.getImage(), drawX, drawY,
					drawX + scaledWidth, drawY + scaledHeight,
					0, 0, sprite.getWidth(), sprite.getHeight(), null);
		}

		@Override
		public void move(long delta) {
			// remote entities are driven by snapshots only
		}

		@Override
		public void collidedWith(Entity other) {
			// remote visuals do not process collisions locally
		}
	}

	private static class RemoteBossEntity extends Entity {
		private int currentHP = 1;
		private int maxHP = 1;
		private int phase = 1;

		RemoteBossEntity(EntitySnapshot snapshot, Map<String, String> meta) {
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : SpriteConstants.BOSS_1_BOSS_PNG,
				(int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			apply(meta);
		}

		private void apply(Map<String, String> meta) {
			if (meta == null || meta.isEmpty()) {
				return;
			}
			try {
				currentHP = Integer.parseInt(meta.getOrDefault("hp", Integer.toString(currentHP)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			try {
				maxHP = Integer.parseInt(meta.getOrDefault("max", Integer.toString(maxHP)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			try {
				phase = Integer.parseInt(meta.getOrDefault("phase", Integer.toString(phase)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
		}

		@Override
		protected void applySnapshotMetadata(String metadata) {
			apply(MetadataCodec.decode(metadata));
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - 150, (int) Math.round(y) - 150, 300, 300);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			if (sprite != null) {
				int bossWidth = 300;
				int bossHeight = 300;
				int drawX = (int) Math.round(x) - bossWidth / 2;
				int drawY = (int) Math.round(y) - bossHeight / 2;
				g2d.drawImage(sprite.getImage(), drawX, drawY,
						drawX + bossWidth, drawY + bossHeight,
						0, 0, sprite.getWidth(), sprite.getHeight(), null);
			}
			g2d.setColor(Color.YELLOW);
			g2d.setFont(g2d.getFont().deriveFont(16f));
			g2d.drawString("Phase " + Math.max(1, phase), (int) Math.round(x) - 25, (int) Math.round(y) - 170);
			g2d.setColor(Color.RED);
			g2d.fillRect((int) Math.round(x) - 50, (int) Math.round(y) + 170, 100, 8);
			g2d.setColor(Color.GREEN);
			int healthWidth = 0;
			if (maxHP > 0) {
				double ratio = Math.max(0.0, Math.min(1.0, (double) currentHP / maxHP));
				healthWidth = (int) Math.round(100 * ratio);
			}
			g2d.fillRect((int) Math.round(x) - 50, (int) Math.round(y) + 170, healthWidth, 8);
			g2d.setColor(Color.WHITE);
			g2d.drawRect((int) Math.round(x) - 50, (int) Math.round(y) + 170, 100, 8);
		}

		@Override
		public void move(long delta) {
			// remote boss driven by snapshots only
		}

		@Override
		public void collidedWith(Entity other) {
			// remote visuals do not process collisions locally
		}
	}

	private static class RemoteBossShotEntity extends Entity {
		private double directionX;
		private double directionY;
		private double speed;
		private int radius = 8;
		private boolean canSplit;
		private double splitY;
		private int splitCount;

		RemoteBossShotEntity(EntitySnapshot snapshot, Map<String, String> meta) {
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : SpriteConstants.SHOT_GIF,
				(int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			apply(meta);
		}

		private void apply(Map<String, String> meta) {
			if (meta == null || meta.isEmpty()) {
				return;
			}
			try {
				directionX = Double.parseDouble(meta.getOrDefault("dirX", Double.toString(directionX)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			try {
				directionY = Double.parseDouble(meta.getOrDefault("dirY", Double.toString(directionY)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			try {
				speed = Double.parseDouble(meta.getOrDefault("speed", Double.toString(speed)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			try {
				radius = Integer.parseInt(meta.getOrDefault(RADIUS_KEY, Integer.toString(radius)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			canSplit = "1".equals(meta.get("split"));
			try {
				splitY = Double.parseDouble(meta.getOrDefault("splitY", Double.toString(splitY)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
			try {
				splitCount = Integer.parseInt(meta.getOrDefault("splitCount", Integer.toString(splitCount)));
			} catch (NumberFormatException ignore) {
				// keep previous value
			}
		}

		@Override
		protected void applySnapshotMetadata(String metadata) {
			apply(MetadataCodec.decode(metadata));
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			int spriteWidth = sprite != null ? sprite.getWidth() : 16;
			int spriteHeight = sprite != null ? sprite.getHeight() : 16;
			int centerX = (int) Math.round(x) + spriteWidth / 2;
			int centerY = (int) Math.round(y) + spriteHeight / 2;
			int glowAlpha = canSplit ? 100 : 60;
			int coreAlpha = canSplit ? 240 : 220;
			g2d.setColor(new Color(255, 100, 100, glowAlpha));
			g2d.fillOval(centerX - radius - 3, centerY - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
			g2d.setColor(new Color(255, 50, 50, coreAlpha));
			g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
			if (radius > 4) {
				g2d.setColor(new Color(255, 200, 200, 180));
				g2d.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);
			}
			if (radius > 6) {
				g2d.setColor(new Color(255, 255, 255, 200));
				g2d.fillOval(centerX - radius + 4, centerY - radius + 4, (radius - 4) * 2, (radius - 4) * 2);
			}
			if (speed > 0 && radius > 4 && (directionX != 0 || directionY != 0)) {
				int trailDistance = Math.max(8, radius);
				int trailX = centerX - (int) (directionX * trailDistance);
				int trailY = centerY - (int) (directionY * trailDistance);
				g2d.setColor(new Color(255, 100, 100, 80));
				int trailRadius = Math.max(3, radius - 1);
				g2d.fillOval(trailX - trailRadius, trailY - trailRadius, trailRadius * 2, trailRadius * 2);
				trailX = centerX - (int) (directionX * trailDistance * 1.5);
				trailY = centerY - (int) (directionY * trailDistance * 1.5);
				g2d.setColor(new Color(255, 100, 100, 40));
				trailRadius = Math.max(2, radius - 2);
				g2d.fillOval(trailX - trailRadius, trailY - trailRadius, trailRadius * 2, trailRadius * 2);
			}
		}

		@Override
		public void move(long delta) {
			// snapshots drive movement
		}

		@Override
		public void collidedWith(Entity other) {
			// visuals only
		}

		@Override
		public java.awt.Rectangle getBounds() {
			int r = (int) Math.round(radius);
			return new java.awt.Rectangle((int) Math.round(x) - r, (int) Math.round(y) - r, r * 2, r * 2);
		}
	}

	private static class RemoteHeatEffectEntity extends Entity {
		private static BufferedImage cachedHeat;
		private final long startTime = System.currentTimeMillis();
		private final long duration = 800;

		RemoteHeatEffectEntity(double x, double y) {
			super(SpriteConstants.HEAT_GIF, (int) Math.round(x), (int) Math.round(y));
			this.x = x;
			this.y = y;
			ensureHeatImage();
		}

		private static synchronized void ensureHeatImage() {
			if (cachedHeat != null) {
				return;
			}
			try (java.io.InputStream is = MultiplayerGameCanvas.class.getClassLoader().getResourceAsStream(SpriteConstants.HEAT_GIF)) {
				if (is != null) {
					cachedHeat = ImageIO.read(is);
				}
			} catch (Exception ignore) {
				cachedHeat = null;
			}
		}

		@Override
		public void move(long delta) {
			// remote visuals rely on draw timing
		}

		@Override
		public void collidedWith(Entity other) {
			// no collisions for visuals
		}

		@Override
		public void draw(Graphics g) {
			long elapsed = System.currentTimeMillis() - startTime;
			double progress = Math.min(1.0, Math.max(0.0, elapsed / (double) duration));
			if (progress >= 1.0) {
				return;
			}
			float alpha = (float) (1.0 - progress);
			float scale;
			if (progress < 0.3) {
				scale = 1.5f + (float) (progress * 1.0);
			} else {
				float shrink = (float) ((progress - 0.3) / 0.7);
				scale = 2.5f - shrink;
			}
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
					java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
					Math.max(0f, Math.min(1f, alpha))));
			if (cachedHeat != null) {
				int originalWidth = cachedHeat.getWidth();
				int originalHeight = cachedHeat.getHeight();
				int scaledWidth = (int) (originalWidth * scale);
				int scaledHeight = (int) (originalHeight * scale);
				int drawX = (int) Math.round(x) - scaledWidth / 2;
				int drawY = (int) Math.round(y) - scaledHeight / 2;
				g2d.drawImage(cachedHeat, drawX, drawY, scaledWidth, scaledHeight, null);
			} else {
				int fallbackSize = (int) (20 * scale);
				g2d.setColor(new Color(255, 100, 0, (int) (alpha * 200)));
				g2d.fillOval((int) Math.round(x) - fallbackSize / 2, (int) Math.round(y) - fallbackSize / 2,
					fallbackSize, fallbackSize);
				int innerSize = (int) (12 * scale);
				g2d.setColor(new Color(255, 200, 0, (int) (alpha * 150)));
				g2d.fillOval((int) Math.round(x) - innerSize / 2, (int) Math.round(y) - innerSize / 2,
					innerSize, innerSize);
			}
			g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
		}
	}

	private void handleRemoteEvents(List<GameEvent> events) {
		for (GameEvent event : events) {
			if (event == null) {
				continue;
			}
			switch (event.type) {
				case CHAT:
					handleChatEvent(event);
					break;
				case SYSTEM:
					handleSystemEvent(event);
					break;
				case COIN_POPUP:
					handleCoinPopupEvent(event);
					break;
				case SKILL_UPDATE:
					// Reserved for future skill synchronization events
					break;
				default:
					break;
			}
		}
	}

	private void handleChatEvent(GameEvent event) {
		String senderId = event.fromPlayerId != null ? event.fromPlayerId : "";
		String display = playerDisplayNames.getOrDefault(senderId, senderId.isEmpty() ? "" : senderId);
		String line = (display == null || display.isEmpty() ? "" : display + ": ") + (event.message != null ? event.message : "");
		appendIntermissionChat(line);
	}

	private void handleSystemEvent(GameEvent event) {
		String message = event.message != null ? event.message : "";
		if (message.isEmpty()) {
			return;
		}

		if (message.startsWith("SKIN_CHANGED:")) {
			handleSkinChangedEvent(message);
		} else if (event.fromPlayerId != null && event.fromPlayerId.equals(localPlayerId)) {
			gameStateManager.setMessage(message);
			gameStateManager.setWaitingForKeyPress(true);
			gameStateManager.setRoundTransition(false);
		}
	}

	private void handleSkinChangedEvent(String message) {
		String[] parts = message.split(":", 3);
		if (parts.length == 3) {
			String playerId = parts[1];
			String skinPath = parts[2];
			remotePlayerSkins.put(playerId, skinPath);
			updateRemotePlayerSkin(playerId, skinPath);
			System.out.println(REMOTE_PLAYER_LABEL + playerId + " 스킨 변경됨: " + skinPath);
		}
	}

	private void handleCoinPopupEvent(GameEvent event) {
		if (!remoteMode) {
			return;
		}
		String localId = localPlayerId != null ? localPlayerId : gameStateManager.getLocalPlayerId();
		if (localId == null || event.fromPlayerId == null || !event.fromPlayerId.equals(localId)) {
			return;
		}

		int popupX = 400;
		int popupY = 200;
		int reward = 0;
		if (event.message != null && !event.message.isEmpty()) {
			String[] parts = event.message.split("\\|", -1);
			if (parts.length > 0) {
				popupX = safeParseInt(parts[0], popupX);
			}
			if (parts.length > 1) {
				popupY = safeParseInt(parts[1], popupY);
			}
			if (parts.length > 2) {
				reward = safeParseInt(parts[2], reward);
			}
		}
		if (reward > 0) {
			showCoinEarned(localId, popupX, popupY, reward);
		}
	}

	private void updateRemoteCoinPopups(long delta) {
		if (!remoteMode || remoteCoinPopups.isEmpty()) {
			return;
		}
		Iterator<CoinDisplayEntity> iterator = remoteCoinPopups.iterator();
		while (iterator.hasNext()) {
			CoinDisplayEntity popup = iterator.next();
			popup.move(delta);
			if (popup.isExpired()) {
				iterator.remove();
			}
		}
	}

	private void appendIntermissionChat(String line) {
		if (line == null) {
			return;
		}
		String trimmed = line.trim();
		if (trimmed.isEmpty()) {
			return;
		}
		intermissionChatLines.addLast(trimmed);
		while (intermissionChatLines.size() > 50) {
			intermissionChatLines.pollFirst();
		}
	}

	public boolean handleIntermissionKeyPressed(KeyEvent e) {
		if (!isIntermissionOverlayVisible()) {
			return false;
		}
		switch (e.getKeyCode()) {
			case KeyEvent.VK_R:
				if (remotePhase == GameSnapshot.Phase.INTERMISSION) {
					toggleReadyStatus();
				}
				return true;
			case KeyEvent.VK_ENTER:
				if (intermissionChatFocus) {
					sendIntermissionChatMessage();
				} else {
					intermissionChatFocus = true;
				}
				return true;
			case KeyEvent.VK_ESCAPE:
				if (intermissionChatFocus) {
					intermissionChatFocus = false;
					return true;
				}
				if (remotePhase == GameSnapshot.Phase.COMPLETED) {
					requestReturnToLobby();
					return true;
				}
				break;
			case KeyEvent.VK_BACK_SPACE:
				if (intermissionChatFocus && !intermissionChatInput.isEmpty()) {
					intermissionChatInput = intermissionChatInput.substring(0, intermissionChatInput.length() - 1);
					return true;
				}
				break;
			default:
				break;
		}
		return false;
	}

	private static int safeParseInt(String value, int defaultValue) {
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignore) {
			return defaultValue;
		}
	}

	public boolean handleIntermissionKeyTyped(KeyEvent e) {
		if (!intermissionChatFocus || !isIntermissionOverlayVisible()) {
			return false;
		}
		char ch = e.getKeyChar();
		if (Character.isISOControl(ch)) {
			return true;
		}
		if (intermissionChatInput.length() >= 200) {
			return true;
		}
		intermissionChatInput += ch;
		return true;
	}

	public boolean isIntermissionOverlayVisible() {
		return remoteMode && (remotePhase == GameSnapshot.Phase.INTERMISSION || remotePhase == GameSnapshot.Phase.COMPLETED);
	}

	public boolean isSpectatorOverlayVisible() {
		return isLocalSpectatorActive();
	}

	private boolean isLocalSpectatorActive() {
		return remoteMode && remotePhase == GameSnapshot.Phase.ACTIVE && localSpectating;
	}

	public boolean isIntermissionChatFocused() {
		return intermissionChatFocus;
	}

	public void toggleReadyStatus() {
		if (!remoteMode || localPlayerId == null || networkAdapter == null || remotePhase != GameSnapshot.Phase.INTERMISSION) {
			return;
		}
		localReady = !localReady;
		remoteReadyStates.put(localPlayerId, localReady);
		if (networkAdapter instanceof RoomGameNetworkAdapter) {
			((RoomGameNetworkAdapter) networkAdapter).sendRoundReady(localReady);
		}
	}

	private void sendIntermissionChatMessage() {
		String message = intermissionChatInput.trim();
		if (message.isEmpty()) {
			intermissionChatInput = "";
			intermissionChatFocus = false;
			return;
		}
		boolean shouldEchoLocally = true;
		if (remoteMode && networkAdapter != null) {
			GameEvent chat = new GameEvent(GameEvent.Type.CHAT, localPlayerId, message, System.currentTimeMillis());
			networkAdapter.sendEvent(chat);
			shouldEchoLocally = false;
		}
		if (shouldEchoLocally) {
			String display = playerDisplayNames.getOrDefault(localPlayerId, localPlayerId != null ? localPlayerId : "나");
			appendIntermissionChat((display == null || display.isEmpty() ? "나" : display) + ": " + message);
		}
		intermissionChatInput = "";
		intermissionChatFocus = false;
	}

	private static class RemoteSpriteEntity extends Entity {
		RemoteSpriteEntity(String spritePath, double x, double y) {
			super(spritePath, (int) x, (int) y);
		}

		@Override
		public void move(long delta) {
			// remote entities are driven by snapshots only
		}

		@Override
		public void collidedWith(Entity other) {
			// no-op for remote entities
		}
	}

	private static class RemoteShotEntity extends Entity {
		private static final Map<Integer, BufferedImage> SKILL_ICON_CACHE = new HashMap<>();
		private boolean isAlienShot;
		private boolean isSkillDrop;
		private int skillType;
		private int skillValue;
		private boolean hasPiercing;
		private boolean nearMonsterShot;

		RemoteShotEntity(EntitySnapshot snapshot, Map<String, String> meta) {
			super(resolveShotSprite(snapshot), (int) snapshot.x, (int) snapshot.y);
			apply(meta);
		}

		private static String resolveShotSprite(EntitySnapshot snapshot) {
			return snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : SpriteConstants.SHOT_GIF;
		}

		private void apply(Map<String, String> meta) {
			if (meta == null) return;
			isAlienShot = "1".equals(meta.get("alien"));
			isSkillDrop = "1".equals(meta.get("skill"));
			try { skillType = Integer.parseInt(meta.getOrDefault("skillType", "-1")); }
			catch (NumberFormatException ignore) { skillType = -1; }
			try { skillValue = Integer.parseInt(meta.getOrDefault("skillValue", "0")); }
			catch (NumberFormatException ignore) { skillValue = 0; }
			hasPiercing = "1".equals(meta.get("pierce"));
			nearMonsterShot = "1".equals(meta.get("near"));
			String spriteOverride = meta.get("sprite");
			if (spriteOverride != null && !spriteOverride.isEmpty()) {
				changeSkin(spriteOverride);
			}
		}

		@Override
		protected void applySnapshotMetadata(String metadata) {
			apply(MetadataCodec.decode(metadata));
		}

		@Override
		public void move(long delta) {
			// remote entities are driven by snapshots only
		}

		@Override
		public void collidedWith(Entity other) {
			// no-op
		}

		@Override
		public void draw(Graphics g) {
			if (isSkillDrop) {
				drawSkillDrop(g);
			} else if (hasPiercing) {
				g.setColor(Color.YELLOW);
				g.fillRect((int) x - 2, (int) y - 10, 4, 20);
			} else if (nearMonsterShot) {
				drawNearMonsterShot(g);
			} else {
				super.draw(g);
			}
		}

		private BufferedImage loadSkillImage(int type) {
			BufferedImage cached = SKILL_ICON_CACHE.get(type);
			if (cached != null) {
				return cached;
			}
			String imagePath;
			switch (type) {
				case 0: imagePath = SpriteConstants.SKILL_1_PNG; break;
				case 1: imagePath = SpriteConstants.SKILL_2_PNG; break;
				case 2: imagePath = SpriteConstants.SKILL_3_PNG; break;
				case 3: imagePath = SpriteConstants.SKILL_4_PNG; break;
				default: imagePath = SpriteConstants.SKILL_1_PNG; break;
			}
			try (java.io.InputStream is = MultiplayerGameCanvas.class.getClassLoader().getResourceAsStream(imagePath)) {
				if (is != null) {
					BufferedImage img = ImageIO.read(is);
					SKILL_ICON_CACHE.put(type, img);
					return img;
				}
			} catch (Exception ignored) {}
			return null;
		}

		private void drawSkillDrop(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			int itemSize = 24;
			int drawX = (int) x - itemSize / 2;
			int drawY = (int) y - itemSize / 2;
			BufferedImage img = loadSkillImage(skillType);
			if (img != null) {
				g2d.setColor(new Color(0, 0, 0, 200));
				g2d.fillRect(drawX + 2, drawY + 2, itemSize, itemSize);
				g2d.setColor(new Color(0, 0, 0, 150));
				g2d.fillRect(drawX + 1, drawY + 1, itemSize, itemSize);
				g2d.drawImage(img, drawX, drawY, itemSize, itemSize, null);
				g2d.setColor(new Color(255, 255, 255, 200));
				g2d.setStroke(new BasicStroke(2));
				g2d.drawRect(drawX, drawY, itemSize, itemSize);
				int circleX = drawX + itemSize - 4;
				int circleY = drawY + itemSize - 4;
				int radius = 6;
				g2d.setColor(new Color(0, 0, 0, 180));
				g2d.fillOval(circleX - radius, circleY - radius, radius * 2, radius * 2);
				g2d.setColor(Color.YELLOW);
				g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 8));
				String text = "1";
				int textWidth = g2d.getFontMetrics().stringWidth(text);
				int textHeight = g2d.getFontMetrics().getHeight();
				g2d.drawString(text, circleX - textWidth / 2, circleY + textHeight / 4);
			} else {
				g2d.setColor(Color.CYAN);
				g2d.fillOval(drawX, drawY, itemSize, itemSize);
				g2d.setColor(Color.WHITE);
				g2d.drawOval(drawX, drawY, itemSize, itemSize);
			}
		}

		private void drawNearMonsterShot(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			if (SpriteConstants.HEAT_GIF.equals(spritePath)) {
				int sphereSize = 20;
				g2d.setColor(Color.GREEN);
				g2d.fillOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
				g2d.setColor(Color.WHITE);
				g2d.drawOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
				g2d.setColor(new Color(0, 255, 0, 100));
				g2d.fillOval((int) x - sphereSize / 4, (int) y - sphereSize / 4, sphereSize / 2, sphereSize / 2);
				return;
			}
			if (SpriteConstants.SHOT_GIF.equals(spritePath) || (spritePath != null && spritePath.endsWith("/shot.gif"))) {
				int sphereSize = 20;
				g2d.setColor(Color.BLACK);
				g2d.fillOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
				g2d.setColor(Color.DARK_GRAY);
				g2d.drawOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
				return;
			}
			if (SpriteConstants.ROUND_2_ATTACK_1_GIF.equals(spritePath)) {
				sprite.draw(g, (int) x - 75, (int) y - 75, 150, 150);
				return;
			}
			if (SpriteConstants.ICE_BALL_GIF.equals(spritePath)) {
				sprite.draw(g, (int) x - 50, (int) y - 50, 100, 100);
				return;
			}
			sprite.draw(g, (int) x - 10, (int) y - 10, 20, 20);
		}

		@Override
		public java.awt.Rectangle getBounds() {
			java.awt.Rectangle base = super.getBounds();
			if (base == null) {
				return new java.awt.Rectangle();
			}
			if (isSkillDrop) {
				return centeredSquare(24);
			}
			if (nearMonsterShot) {
				java.awt.Rectangle shrunk = shrinkRect(base);
				int centerX = (int) Math.round(x);
				int centerY = (int) Math.round(y);
				return new java.awt.Rectangle(centerX - shrunk.width / 2, centerY - shrunk.height / 2, shrunk.width, shrunk.height);
			}
			if (isAlienShot || hasPiercing) {
				return shrinkRect(base);
			}
			return base;
		}

		private java.awt.Rectangle shrinkRect(java.awt.Rectangle base) {
			int width = Math.max(4, (int) (base.width * 0.175));
			int height = Math.max(4, (int) (base.height * 0.175));
			int centerX = base.x + base.width / 2;
			int centerY = base.y + base.height / 2;
			return new java.awt.Rectangle(centerX - width / 2, centerY - height / 2, width, height);
		}

		private java.awt.Rectangle centeredSquare(int size) {
			int centerX = (int) Math.round(x);
			int centerY = (int) Math.round(y);
			return new java.awt.Rectangle(centerX - size / 2, centerY - size / 2, size, size);
		}
	}

	private static class RemoteIceAttack extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteIceAttack(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_ICE_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_ICE_GIF);
			this.width = snapshot.w > 0 ? snapshot.w : 48;
			this.height = snapshot.h > 0 ? snapshot.h : 48;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			int drawX = (int) Math.round(x) - width / 2;
			int drawY = (int) Math.round(y) - height / 2;
			return new java.awt.Rectangle(drawX, drawY, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.drawImage(image, drawX, drawY, width, height, null);
			} else {
				g2d.setColor(new Color(0, 255, 255, 180));
				g2d.fillOval((int) Math.round(x) - 16, (int) Math.round(y) - 16, 32, 32);
				g2d.setColor(new Color(0, 200, 200, 120));
				g2d.drawOval((int) Math.round(x) - 16, (int) Math.round(y) - 16, 32, 32);
			}
		}
	}

	private static class RemoteIceBallAttack extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteIceBallAttack(EntitySnapshot snapshot) {
			super(SpriteConstants.ICE_BALL_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
 			this.animated = loadAnimatedImage(SpriteConstants.ICE_BALL_GIF);
			int fallback = 20;
			this.width = snapshot.w > 0 ? snapshot.w : fallback;
			this.height = snapshot.h > 0 ? snapshot.h : fallback;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			int drawX = (int) Math.round(x) - width / 2;
			int drawY = (int) Math.round(y) - height / 2;
			return new java.awt.Rectangle(drawX, drawY, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.drawImage(image, drawX, drawY, width, height, null);
			} else {
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.setColor(new Color(120, 255, 255, 180));
				g2d.fillOval(drawX, drawY, width, height);
				g2d.setColor(new Color(210, 255, 255, 220));
				g2d.fillOval(drawX + 3, drawY + 3, Math.max(0, width - 6), Math.max(0, height - 6));
			}
		}
	}

	private static class RemoteMagneticField extends Entity {
		private final double radius;
		private final double strength;

		RemoteMagneticField(EntitySnapshot snapshot, Map<String, String> meta) {
			super(SpriteConstants.SHOT_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.radius = parseDouble(meta, RADIUS_KEY, 220.0);
			this.strength = parseDouble(meta, "strength", 0.8);
		}

		private static double parseDouble(Map<String, String> meta, String key, double def) {
			if (meta == null) {
				return def;
			}
			try {
				return Double.parseDouble(meta.getOrDefault(key, Double.toString(def)));
			} catch (NumberFormatException ignore) {
				return def;
			}
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			int r = (int) Math.round(radius);
			return new java.awt.Rectangle((int) Math.round(x) - r, (int) Math.round(y) - r, r * 2, r * 2);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int r = (int) Math.round(radius);
			int centerX = (int) Math.round(x);
			int centerY = (int) Math.round(y);
			float alpha = 0.4f;
			g2d.setColor(strength > 0 ? new Color(1.0f, 0.3f, 0.3f, alpha)
					: new Color(0.3f, 0.3f, 1.0f, alpha));
			g2d.fillOval(centerX - r, centerY - r, r * 2, r * 2);
			g2d.setStroke(new BasicStroke(2.0f));
			g2d.setColor(new Color(1.0f, 1.0f, 1.0f, alpha * 0.8f));
			g2d.drawOval(centerX - r, centerY - r, r * 2, r * 2);
			g2d.setColor(Color.WHITE);
			g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
			g2d.dispose();
		}
	}

	private static class RemoteRound2Laser extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteRound2Laser(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_2ROUND_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_2ROUND_GIF);
			this.width = snapshot.w > 0 ? snapshot.w : 700;
			this.height = snapshot.h > 0 ? snapshot.h : 300;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x),
					(int) Math.round(y), width, height);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			AffineTransform original = g2d.getTransform();
			g2d.translate(x + width / 2.0, y + height / 2.0);
			g2d.rotate(Math.PI / 2);
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g2d.drawImage(image, (int) -(width / 2), (int) -(height / 2),
						width, height, null);
			} else {
				g2d.setColor(new Color(120, 200, 255, 180));
				g2d.fillRect((int) -(width / 2), (int) -(height / 2), width, height);
			}
			g2d.setTransform(original);
		}
	}

	private static class RemoteRound2Phase1 extends Entity {
		private final Image animated;
		private final int size;

		RemoteRound2Phase1(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_2ROUND3_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_2ROUND3_GIF);
			this.size = snapshot.w > 0 ? snapshot.w : 80;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2,
						size, size, null);
			} else {
				g.setColor(new Color(255, 150, 150, 200));
				g.fillOval((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
			}
		}
	}

	private static class RemoteRound2Phase2 extends Entity {
		private final Image animated;
		private final int size;

		RemoteRound2Phase2(EntitySnapshot snapshot) {
			super(SpriteConstants.ROUND_2_ATTACK_1_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.ROUND_2_ATTACK_1_GIF);
			this.size = snapshot.w > 0 ? snapshot.w : 50;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2,
						size, size, null);
			} else {
				g.setColor(new Color(255, 200, 120, 220));
				g.fillOval((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
			}
		}
	}

	private static class RemoteRound2Random extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteRound2Random(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_2ROUND2_GIF, (int) Math.round(snapshot.x), 150);
			this.x = snapshot.x;
			this.y = 150;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_2ROUND2_GIF);
			this.width = snapshot.w > 0 ? snapshot.w : 300;
			this.height = snapshot.h > 0 ? snapshot.h : 400;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - width / 2, (int) Math.round(y), width, height);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - width / 2, 150, width, height, null);
			} else {
				g.setColor(new Color(255, 200, 0, 128));
				g.fillRect((int) Math.round(x) - width / 2, 150, width, height);
			}
		}
	}

	private static class RemoteRound2Quad extends Entity {
		private final Image animated;
		private final int size;

		RemoteRound2Quad(EntitySnapshot snapshot) {
			super(SpriteConstants.ROUND_2_ATTACK_1_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.ROUND_2_ATTACK_1_GIF);
			this.size = snapshot.w > 0 ? snapshot.w : 50;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size, null);
			} else {
				g.setColor(new Color(255, 180, 120, 200));
				g.fillOval((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
			}
		}
	}

	private static class RemoteRound2MachineGun extends Entity {
		private final Image animated;
		private final int size;

		RemoteRound2MachineGun(EntitySnapshot snapshot) {
			super(SpriteConstants.ROUND_2_ATTACK_1_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.ROUND_2_ATTACK_1_GIF);
			this.size = snapshot.w > 0 ? snapshot.w : 30;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size, null);
			} else {
				g.setColor(new Color(255, 220, 120, 200));
				g.fillOval((int) Math.round(x) - size / 2, (int) Math.round(y) - size / 2, size, size);
			}
		}
	}

	private static class RemoteRound3Straight extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteRound3Straight(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_3ROUND4_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_3ROUND4_GIF);
			this.width = snapshot.w > 0 ? snapshot.w : 600;
			this.height = snapshot.h > 0 ? snapshot.h : 300;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2,
						width, height, null);
			} else {
				g.setColor(new Color(200, 30, 30, 140));
				g.fillRect((int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2, width, height);
			}
		}
	}

	private static class RemoteRound3Random extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteRound3Random(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_3ROUND2_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_3ROUND2_GIF);
			this.width = snapshot.w > 0 ? snapshot.w : 400;
			this.height = snapshot.h > 0 ? snapshot.h : 1400;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2,
						width, height, null);
			} else {
				g.setColor(new Color(255, 80, 80, 120));
				g.fillRect((int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2, width, height);
			}
		}
	}

	private static class RemoteRound3Pull extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteRound3Pull(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_3ROUND3_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_3ROUND3_GIF);
			this.width = snapshot.w > 0 ? snapshot.w : 300;
			this.height = snapshot.h > 0 ? snapshot.h : 400;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2,
						width, height, null);
			} else {
				g.setColor(new Color(80, 120, 255, 120));
				g.fillRect((int) Math.round(x) - width / 2, (int) Math.round(y) - height / 2, width, height);
			}
		}
	}

	private static class RemoteRound3BlackHole extends Entity {
		private static final int SIZE = 150;
		private final Image animated;

		RemoteRound3BlackHole(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_3ROUND_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_3ROUND_GIF);
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2,
						SIZE, SIZE, null);
			} else {
				g.setColor(new Color(40, 40, 80, 200));
				g.fillOval((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
			}
		}
	}

	private static class RemoteRound4Heal extends Entity {
		private static final int SIZE = 300;
		private final Image animated;

		RemoteRound4Heal(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_4ROUND_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_4ROUND_GIF);
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2,
						SIZE, SIZE, null);
			} else {
				g.setColor(new Color(120, 255, 120, 140));
				g.fillOval((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
			}
		}
	}

	private static class RemoteRound4GreenSphere extends Entity {
		private final Image animated;
		private final int width;
		private final int height;

		RemoteRound4GreenSphere(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_5ROUND1_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_5ROUND1_GIF);
			int fallback = 24;
			this.width = snapshot.w > 0 ? snapshot.w : fallback;
			this.height = snapshot.h > 0 ? snapshot.h : fallback;
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			int drawX = (int) Math.round(x) - width / 2;
			int drawY = (int) Math.round(y) - height / 2;
			return new java.awt.Rectangle(drawX, drawY, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.drawImage(image, drawX, drawY, width, height, null);
			} else {
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.setColor(new Color(120, 255, 120, 220));
				g2d.fillOval(drawX, drawY, width, height);
				g2d.setColor(new Color(200, 255, 200, 160));
				g2d.drawOval(drawX, drawY, width, height);
			}
		}
	}


	private static class RemoteRound4PlayerLine extends Entity {
		private static final int SIZE = 160;
		private final Image animated;

		RemoteRound4PlayerLine(EntitySnapshot snapshot) {
			super(SpriteConstants.BOSS_ATTACK_4ROUND3_GIF, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage(SpriteConstants.BOSS_ATTACK_4ROUND3_GIF);
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2,
						SIZE, SIZE, null);
			} else {
				g.setColor(new Color(255, 120, 120, 200));
				g.fillRect((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
			}
		}
	}

	private static class RemoteExplosionEntity extends Entity {
		private static BufferedImage cachedImage;
		private double currentRadius;

		RemoteExplosionEntity(EntitySnapshot snapshot, Map<String, String> meta) {
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : SpriteConstants.EXPLOSION_PNG, (int) snapshot.x, (int) snapshot.y);
			loadImage();
			apply(meta);
		}

		private void apply(Map<String, String> meta) {
			if (meta == null) return;
			try { currentRadius = Double.parseDouble(meta.getOrDefault(RADIUS_KEY, "0")); }
			catch (NumberFormatException ignore) { currentRadius = 0; }
		}

		@Override
		protected void applySnapshotMetadata(String metadata) {
			apply(MetadataCodec.decode(metadata));
		}

		@Override
		public void move(long delta) {
			// Method is intentionally empty.
		}

		@Override
		public void collidedWith(Entity other) {
			// Method is intentionally empty.
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
					java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			if (cachedImage != null) {
				int drawSize = (int) (currentRadius * 2);
				int drawX = (int) x - drawSize / 2;
				int drawY = (int) y - drawSize / 2;
				g2d.drawImage(cachedImage, drawX, drawY, drawSize, drawSize, null);
			}
			g2d.setColor(Color.YELLOW);
			g2d.setStroke(new java.awt.BasicStroke(3.0f));
			g2d.drawOval((int)(x - currentRadius), (int)(y - currentRadius),
					(int)(currentRadius * 2), (int)(currentRadius * 2));
			g2d.setColor(Color.ORANGE);
			g2d.setStroke(new java.awt.BasicStroke(2.0f));
			double innerRadius = currentRadius * 0.7;
			g2d.drawOval((int)(x - innerRadius), (int)(y - innerRadius),
					(int)(innerRadius * 2), (int)(innerRadius * 2));
		}

		private static void loadImage() {
			if (cachedImage != null) return;
			try (java.io.InputStream is = MultiplayerGameCanvas.class.getClassLoader().getResourceAsStream(SpriteConstants.EXPLOSION_PNG)) {
				if (is != null) {
					cachedImage = ImageIO.read(is);
				}
			} catch (Exception ignored) {}
		}
	}

	public void render(Graphics2D g) {
		applyResolutionScaling(g);
		drawGameElements(g);
		drawOverlays(g);
	}

	private void applyResolutionScaling(Graphics2D g) {
		if (resolutionManager != null) {
			double scaleX = resolutionManager.getScaleX();
			double scaleY = resolutionManager.getScaleY();
			g.scale(scaleX, scaleY);
		}
	}

	private void drawGameElements(Graphics2D g) {
		backgroundRenderer.draw(g);
		for (Entity entity : gameStateManager.getEntities()) {
			entity.draw(g);
		}
		drawRemoteCoinPopups(g);
		uiRenderer.drawGameUI(g, gameStateManager, skillManager);
	}

	private void drawOverlays(Graphics2D g) {
		if (isLocalSpectatorActive()) {
			drawSpectatorOverlay(g);
		}
		if (gameStateManager.isShowingPauseMenu()) {
			drawPauseMenu(g);
		}
		if (isIntermissionOverlayVisible()) {
			drawIntermissionOverlay(g);
		}
		if (gameStateManager.isWaitingForKeyPress()) {
			uiRenderer.drawMessage(g, gameStateManager.getMessage());
		}
		if (gameStateManager.isShowingSkillMenu()) {
			drawSkillMenu(g);
		}
		if (gameStateManager.isDebugDrawHitboxes()) {
			drawHitboxOverlay(g);
		}
	}
	
	private void drawRemoteCoinPopups(Graphics2D g) {
		if (!remoteMode || remoteCoinPopups.isEmpty()) {
			return;
		}
		for (CoinDisplayEntity popup : remoteCoinPopups) {
			popup.draw(g);
		}
	}
	
	private void drawHitboxOverlay(Graphics2D g) {
		java.awt.Stroke previous = g.getStroke();
		g.setStroke(new BasicStroke(1f));
		for (Entity entity : gameStateManager.getEntities()) {
			if (entity == null) {
				continue;
			}
			java.awt.Rectangle rect = entity.getBounds();
			if (rect == null) {
				continue;
			}
			g.setColor(getHitboxColor(entity));
			g.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
		g.setStroke(previous);
	}

	private Color getHitboxColor(Entity entity) {
		if (entity instanceof ShipEntity) {
			return new Color(0, 200, 0, 160);
		} else if (entity instanceof ShotEntity) {
			return ((ShotEntity) entity).isAlienShot() ? new Color(255, 0, 0, 160) : new Color(0, 200, 255, 160);
		} else if (entity instanceof RemoteShotEntity) {
			return ((RemoteShotEntity) entity).isAlienShot ? new Color(255, 0, 0, 160) : new Color(0, 200, 255, 160);
		} else {
			return new Color(255, 255, 0, 120);
		}
	}

	
	/**
	 * Getter methods for MultiplayerInputManager
	 */
	@Override
	public ShipEntity getShip(String playerId) {
		if (playerId == null) {
			return ship;
		}
		for (Entity entity : gameStateManager.getEntities()) {
			if (entity instanceof ShipEntity && playerId.equals(entity.getOwnerId())) {
				return (ShipEntity) entity;
			}
		}
		return null;
	}
	
	@Override
	public int getShipX(String playerId) {
		Entity target = getShip(playerId);
		return target != null ? target.getX() : 370;
	}
	
	@Override
	public int getShipY(String playerId) {
		Entity target = getShip(playerId);
		return target != null ? target.getY() : 550;
	}
	
	public double getMoveSpeed() {
		return moveSpeed;
	}
	
	public boolean isWaitingForKeyPress() {
		return gameStateManager.isWaitingForKeyPress();
	}
	
	public void setWaitingForKeyPress(boolean waiting) {
		gameStateManager.setWaitingForKeyPress(waiting);
	}

	private void drawSpectatorOverlay(Graphics2D g) {
		int panelWidth = 380;
		int panelHeight = 240;
		int panelX = (getWidth() - panelWidth) / 2;
		int panelY = (getHeight() - panelHeight) / 2;

		drawSpectatorPanelBackground(g, panelX, panelY, panelWidth, panelHeight);
		drawPlayerLists(g, panelX, panelY);

		g.setFont(new Font(FONT_ARIAL, Font.PLAIN, 12));
		g.setColor(new Color(200, 200, 200));
		g.drawString("Enter: 채팅  ESC: 로비로 돌아가기", panelX + 24, panelY + panelHeight - 28);
	}

	private void drawSpectatorPanelBackground(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight) {
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
		g.setColor(new Color(255, 255, 255, 90));
		g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
		drawSpectatorText(g, panelX, panelY);
	}

	private void drawSpectatorText(Graphics2D g, int panelX, int panelY) {
		g.setFont(new Font(FONT_ARIAL, Font.BOLD, 22));
		g.setColor(Color.WHITE);
		g.drawString("관전 모드", panelX + 24, panelY + 36);

		g.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));
		g.setColor(new Color(225, 225, 225));
		g.drawString("당신의 함선이 파괴되었습니다.", panelX + 24, panelY + 64);
		g.drawString("라운드 종료까지 관전을 계속합니다.", panelX + 24, panelY + 82);
	}

	private void drawPlayerLists(Graphics2D g, int panelX, int panelY) {
		PlayerCategorizationResult categorization = categorizePlayers();

		int listY = panelY + 110;
		drawPlayerList(g, "생존 플레이어", categorization.alivePlayers, panelX, listY, new Color(120, 255, 160), new Color(200, 255, 200));
		listY += (categorization.alivePlayers.isEmpty() ? 1 : categorization.alivePlayers.size()) * 18 + 4;
		drawPlayerList(g, "전투 불능", categorization.defeatedPlayers, panelX, listY, new Color(255, 200, 160), new Color(240, 200, 200));
	}

	private PlayerCategorizationResult categorizePlayers() {
		List<String> alive = new ArrayList<>();
		List<String> defeated = new ArrayList<>();
		for (PlayerState state : gameStateManager.getPlayerStates()) {
			if (state == null) {
				continue;
			}
			String pid = state.getPlayerId();
			if (!shouldDisplayInSpectatorList(pid)) {
				continue;
			}
			String display = playerDisplayNames.getOrDefault(pid, pid);
			if (pid.equals(localPlayerId)) {
				display = display + " (You)";
			}
			if (state.isDead()) {
				defeated.add(display);
			} else {
				alive.add(display);
			}
		}
		return new PlayerCategorizationResult(alive, defeated);
	}

	private static class PlayerCategorizationResult {
		public final List<String> alivePlayers;
		public final List<String> defeatedPlayers;

		public PlayerCategorizationResult(List<String> alivePlayers, List<String> defeatedPlayers) {
			this.alivePlayers = alivePlayers;
			this.defeatedPlayers = defeatedPlayers;
		}
	}

	private void drawPlayerList(Graphics2D g, String title, List<String> players, int x, int y, Color titleColor, Color playerColor) {
		g.setFont(new Font(FONT_ARIAL, Font.BOLD, 14));
		g.setColor(titleColor);
		g.drawString(title, x + 24, y);
		y += 18;
		g.setFont(new Font(FONT_ARIAL, Font.PLAIN, 13));
		if (players.isEmpty()) {
			g.setColor(new Color(200, 200, 200));
			g.drawString("• 없음", x + 24, y);
		} else {
			g.setColor(playerColor);
			for (String label : players) {
				g.drawString("• " + label, x + 24, y);
				y += 18;
			}
		}
	}

	private boolean shouldDisplayInSpectatorList(String playerId) {
		if (playerId == null) {
			return false;
		}
		String trimmed = playerId.trim();
		if (trimmed.isEmpty()) {
			return false;
		}
		return !"local".equalsIgnoreCase(trimmed);
	}

	private void drawIntermissionOverlay(Graphics2D g) {
		int panelWidth = 620;
		int panelHeight = 360;
		int panelX = (getWidth() - panelWidth) / 2;
		int panelY = (getHeight() - panelHeight) / 2;

		drawIntermissionPanel(g, panelX, panelY, panelWidth, panelHeight);
		drawPlayerReadyStatus(g, panelX, panelY);
		drawIntermissionChat(g, panelX, panelY, panelWidth, panelHeight);
		drawIntermissionInfo(g, panelX, panelY, panelHeight);
	}

	private void drawIntermissionPanel(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight) {
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 18, 18);
		g.setColor(new Color(255, 255, 255, 90));
		g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 18, 18);

		drawIntermissionTitle(g, panelX, panelY);
		drawIntermissionMessage(g, panelX, panelY);
	}

	private void drawIntermissionTitle(Graphics2D g, int panelX, int panelY) {
		String title = remotePhase == GameSnapshot.Phase.COMPLETED ? "게임 종료" : "라운드 준비";
		Font titleFont = new Font(FONT_ARIAL, Font.BOLD, 24);
		g.setFont(titleFont);
		g.setColor(Color.WHITE);
		g.drawString(title, panelX + 24, panelY + 40);
	}

	private void drawIntermissionMessage(Graphics2D g, int panelX, int panelY) {
		if (intermissionMessage != null && !intermissionMessage.isEmpty()) {
			g.setFont(new Font(FONT_ARIAL, Font.PLAIN, 16));
			g.setColor(new Color(220, 220, 220));
			g.drawString(intermissionMessage, panelX + 24, panelY + 70);
		}
	}

	private void drawPlayerReadyStatus(Graphics2D g, int panelX, int panelY) {
		int listX = panelX + 24;
		int listY = panelY + 100;
		g.setFont(new Font(FONT_ARIAL, Font.BOLD, 16));
		int lineHeight = 24;
		Map<String, String> displayMap = new LinkedHashMap<>(playerDisplayNames);
		for (String id : remoteReadyStates.keySet()) {
			displayMap.putIfAbsent(id, id);
		}
		int i = 0;
		for (Map.Entry<String, String> entry : displayMap.entrySet()) {
			String playerId = entry.getKey();
			String name = entry.getValue();
			boolean ready = Boolean.TRUE.equals(remoteReadyStates.get(playerId));
			boolean isLocal = playerId != null && playerId.equals(localPlayerId);
			g.setColor(ready ? new Color(120, 255, 140) : new Color(200, 200, 200));
			String status = ready ? "READY" : ".....";
			String label = String.format("%s %s", name != null && !name.isEmpty() ? name : playerId, status);
			g.drawString(label, listX, listY + i * lineHeight);
			if (isLocal) {
				g.setColor(new Color(255, 215, 0));
				g.drawString("← You", listX + g.getFontMetrics().stringWidth(label) + 8, listY + i * lineHeight);
			}
			i++;
		}
	}

	private void drawIntermissionChat(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight) {
		int chatX = panelX + panelWidth / 2 + 10;
		int chatY = panelY + 100;
		int chatWidth = panelWidth / 2 - 34;
		int chatHeight = panelHeight - 160;
		g.setColor(new Color(20, 20, 20, 200));
		g.fillRoundRect(chatX, chatY, chatWidth, chatHeight, 12, 12);
		g.setColor(new Color(80, 80, 80, 180));
		g.drawRoundRect(chatX, chatY, chatWidth, chatHeight, 12, 12);
		g.setFont(new Font("Monospaced", Font.PLAIN, 13));
		int availableLines = Math.max(1, (chatHeight - 16) / 16);
		Object[] lines = intermissionChatLines.toArray();
		int start = Math.max(0, lines.length - availableLines);
		for (int idx = start; idx < lines.length; idx++) {
			String text = (String) lines[idx];
			g.setColor(text.startsWith("* ") ? new Color(160, 220, 255) : Color.WHITE);
			g.drawString(text, chatX + 10, chatY + 18 + (idx - start) * 16);
		}

		int inputY = chatY + chatHeight + 12;
		g.setColor(new Color(0, 0, 0, 210));
		g.fillRoundRect(chatX, inputY, chatWidth, 32, 10, 10);
		g.setColor(intermissionChatFocus ? Color.YELLOW : Color.GRAY);
		g.drawRoundRect(chatX, inputY, chatWidth, 32, 10, 10);
		g.setFont(new Font("Monospaced", Font.PLAIN, 14));
		String inputDisplay = intermissionChatInput;
		if (intermissionChatFocus && (System.currentTimeMillis() / 400) % 2 == 0) {
			inputDisplay += "_";
		}
		g.setColor(Color.WHITE);
		g.drawString(inputDisplay, chatX + 12, inputY + 21);
	}

	private void drawIntermissionInfo(Graphics2D g, int panelX, int panelY, int panelHeight) {
		g.setFont(new Font(FONT_ARIAL, Font.PLAIN, 13));
		g.setColor(new Color(200, 200, 200));
		int infoY = panelY + panelHeight - 40;
		if (remotePhase == GameSnapshot.Phase.INTERMISSION) {
			String info = "R: 준비 토글   Enter: 채팅" + (intermissionChatFocus ? " (입력중 ESC 취소)" : "");
			g.drawString(info, panelX + 24, infoY);
			if (remoteWaitingForPlayers) {
				g.drawString("모든 플레이어가 READY가 되면 다음 라운드가 시작됩니다.", panelX + 24, infoY + 18);
			}
		} else {
			g.drawString("ESC: 로비로 돌아가기", panelX + 24, infoY);
		}
	}
	
	/**
	 * 게임 상태 반환 (gameplay 패키지용)
	 */
	// 게임 상태 텍스트 반환은 더 이상 필요하지 않음 (화면 전환은 App에서 관리)
	
	/**
	 * 새 게임 시작 (gameplay 패키지용)
	 */
	public void startNewGame() { startGame(); }
	
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
	public MultiplayerGameStateManager getGameplayState() {
		return gameStateManager;
	}
	
	/**
	 * 스킬 매니저 반환
	 */
	@Deprecated
	public MultiplayerSkillManager getMultiplayerSkillManager() {
		return getSkillManager(localPlayerId);
	}
	
	/**
	 * UI 렌더러 반환
	 */
	public MultiplayerUIRenderer getMultiplayerUIRenderer() {
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
	 * Create explosion effect
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param radius Explosion radius
	 */
	@Override
	public void createExplosion(int x, int y, double radius) {
		try {
			ExplosionEntity explosion = new ExplosionEntity(this, SpriteConstants.EXPLOSION_PNG, x, y, radius);
			explosion.setOwnerId(localPlayerId);
			gameStateManager.getEntities().add(explosion);
		} catch (Exception e) {
			System.err.println("Error creating explosion: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void createHeatEffect(int x, int y, double radius) {
		try {
			HeatEffectEntity heat = new HeatEffectEntity(this, x, y);
			gameStateManager.getEntities().add(heat);
		} catch (Exception e) {
			System.err.println("Error creating heat effect: " + e.getMessage());
		}
	}
	
	/**
	 * Add score points
	 * 
	 * @param points Points to add
	 */
	@Override
	public void addScore(String playerId, int points) {
		// Method is intentionally empty.
	}
	
	/**
	 * Add skill points
	 * 
	 * @param points Skill points to add
	 */
	@Override
	public void addSkillPoints(String playerId, int points) {
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
		PlayerState ps = gameStateManager.ensurePlayer(targetId);
		ps.addSkillPoints(points);
	}

	@Override
	public ShipEntity createPlayerShip(String playerId) {
		return new ShipEntity(this, currentSpaceshipSkin, 370, 550);
	}

	@Override
	public Entity createNearEntity(int round, int index) {
		int posX = 150 + (index * 100);
		return new NearEntity(this, posX, 120, round, index);
	}

	@Override
	public Entity createBossEntity(int bossRound) {
		return new BossEntity(this, 400, 160, bossRound);
	}
	
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

	@Override
	public void onRoundBackgroundChanged(int round) {
		if (backgroundRenderer == null) {
			backgroundRenderer = new BackgroundRenderer();
		}
		String backgroundFileName;
		if (round == 1 || round == 2) {
			backgroundFileName = "1.png";
		} else if (round == 3 || round == 4) {
			backgroundFileName = "2.png";
		} else if (round == 5 || round == 6) {
			backgroundFileName = "3.png";
		} else if (round == 7 || round == 8) {
			backgroundFileName = "4.png";
		} else {
			backgroundFileName = "1.png";
		}
		String backgroundPath = "sprites/stage_background/" + backgroundFileName;
		backgroundRenderer.setResourcePath(backgroundPath);
	}

	public void checkAllNearMonstersDefeated() {
		if (remoteMode) {
			return;
		}
		boolean advanced = gameStateManager.advanceRound();
		if (advanced) {
			SharedMultiplayerRoundCoordinator.setupRound(this, gameStateManager.getCurrentRound());
			gameStateManager.setWaitingForKeyPress(false);
		} else {
			gameStateManager.setMessage(GAME_COMPLETED_MESSAGE);
			gameStateManager.setWaitingForKeyPress(true);
			persistMultiplayerResults(true);
		}
	}
	
	/**
	 * Handle boss defeat
	 */
	@Override
	public void notifyBossDefeated(String playerId) {
		if (remoteMode) {
			return;
		}

		SharedMultiplayerRoundCoordinator.handleBossDefeated(this, playerId);
		boolean roundAdvanced = gameStateManager.advanceRound();
		if (roundAdvanced) {
			SharedMultiplayerRoundCoordinator.setupRound(this, gameStateManager.getCurrentRound());
		} else {
			gameStateManager.setMessage(GAME_COMPLETED_MESSAGE);
			gameStateManager.setWaitingForKeyPress(true);
			persistMultiplayerResults(true);
		}
	}

	private void persistMultiplayerResults(boolean forcePersist) {
		if (resultsPersisted) {
			return;
		}
		if (!forcePersist && remoteMode && remotePhase != GameSnapshot.Phase.COMPLETED) {
			return;
		}
		if (userManager == null || !userManager.isLoggedIn() || userManager.getCurrentUser() == null) {
			resultsPersisted = true;
			return;
		}

		int earnedCoins = gameStateManager.getEarnedCoins();
		if (earnedCoins > 0) {
			userManager.addCoins(earnedCoins);
		}

		long playTimeMs = gameStateManager.getPlayTimeMs();
		boolean completed = gameStateManager.getCurrentRound() >= gameStateManager.getMaxRound();
		int finalRound = gameStateManager.getCurrentRound();
		List<String> teammates = collectTeammateNames();

		GameRecord record = new GameRecord(
				userManager.getCurrentUser().getUid(),
				userManager.getCurrentUser().getUsername(),
				playTimeMs,
				earnedCoins,
				completed,
				finalRound,
				GameRecord.GameMode.MULTI,
				teammates);

		saveMultiplayerRecord(record);
		resultsPersisted = true;
	}

	private List<String> collectTeammateNames() {
		LinkedHashSet<String> names = new LinkedHashSet<>();
		addTeammateNamesFromDisplayNames(names);

		if (names.isEmpty()) {
			addTeammateNamesFromPlayerStates(names);
		}

		return new ArrayList<>(names);
	}

	private void addTeammateNamesFromDisplayNames(Set<String> names) {
		String localId = localPlayerId;
		for (Map.Entry<String, String> entry : playerDisplayNames.entrySet()) {
			String playerId = entry.getKey();
			String display = entry.getValue();
			if (playerId == null || (localId != null && localId.equals(playerId))) {
				continue;
			}
			String trimmed = display != null ? display.trim() : "";
			if (!trimmed.isEmpty()) {
				names.add(trimmed);
			}
		}
	}

	private void addTeammateNamesFromPlayerStates(Set<String> names) {
		String localId = localPlayerId;
		for (PlayerState ps : gameStateManager.getPlayerStates()) {
			String playerId = ps.getPlayerId();
			if (playerId == null || (localId != null && localId.equals(playerId))) {
				continue;
			}
			names.add(playerId);
		}
	}

	private void saveMultiplayerRecord(GameRecord record) {
		try {
			String dbPath = "users/" + record.getUserId() + "/gameRecords/multi/" + record.getRecordId();
			Map<String, Object> recordData = new HashMap<>();
			recordData.put("recordId", record.getRecordId());
			recordData.put("userId", record.getUserId());
			recordData.put("username", record.getUsername());
			recordData.put("playTimeMs", record.getPlayTimeMs());
			recordData.put("playTime", record.getPlayTime());
			recordData.put("earnedCoins", record.getEarnedCoins());
			recordData.put("completed", record.isCompleted());
			recordData.put("finalRound", record.getFinalRound());
			recordData.put("playDate", record.getPlayDateString());
			recordData.put("mode", record.getMode().name());
			recordData.put("coPlayers", new ArrayList<>(record.getCoPlayers()));

			boolean success = userManager.getFirebaseDB().putData(dbPath, recordData);
			if (success) {
				System.out.println("✅ Multiplayer record saved: " + dbPath);
			} else {
				System.err.println("❌ Failed to save multiplayer record: " + dbPath);
			}
		} catch (Exception ex) {
			System.err.println("Error saving multiplayer record: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	/**
	 * Add a boss shot to the game
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public void addBossShot(int x, int y) {
		try {
			ShotEntity shot = new ShotEntity(this, SpriteConstants.SHOT_GIF, x, y, true); // true = alien shot
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
	 * Get the MultiplayerGameStateManager
	 */
	public MultiplayerGameStateManager getMultiplayerGameStateManager() {
		return gameStateManager;
	}
	
	/**
	 * Apply equipped items from UserManager
	 */
	private void applyEquippedItems() {
		if (userManager == null) {
			return;
		}
		
		applySpaceshipSkin();
		applyWeaponSkin();
		applyPowerupEffects();
	}
	
	/**
	 * Apply equipped spaceship skin
	 */
	private void applySpaceshipSkin() {
		if (userManager == null) return;
		
		ShopItem equippedSpaceship = userManager.getShopManager().getEquippedItem(ShopCategory.SPACESHIPS);
		if (equippedSpaceship != null) {
			// 아이템 ID를 실제 파일명으로 매핑
			String skinFileName = mapItemIdToSkinFile(equippedSpaceship.getId());
			currentSpaceshipSkin = "sprites/ships/" + skinFileName;
			System.out.println("멀티플레이어 스킨 적용: " + equippedSpaceship.getName() + " -> " + currentSpaceshipSkin);
			
			// 기존 ShipEntity가 있으면 스킨 변경
			if (ship != null) {
				ship.changeSkin(currentSpaceshipSkin);
			}
		} else {
			// 장착된 스킨이 없으면 기본 스킨 사용
			currentSpaceshipSkin = SpriteConstants.SHIP_GIF;
			System.out.println("멀티플레이어 기본 스킨 사용: " + currentSpaceshipSkin);
		}
	}
	
	/**
	 * 아이템 ID를 실제 스킨 파일명으로 매핑
	 */
	private String mapItemIdToSkinFile(String itemId) {
		switch (itemId) {
			case "fighter_ship":
				return "spaceship_green.png";
			case "battleship":
				return "spaceship_blue.png";
			case "professor":
				return "professor.png";
			case "king":
				return "king.png";
			case "software_king":
				return "software_king.png";
			default:
				// 기본값으로 아이템 ID + .png 사용
				return itemId + ".png";
		}
	}
	
	/**
	 * 서버에 스킨 정보 전송
	 */
	private void sendSkinToServer() {
		if (networkAdapter != null && localPlayerId != null && currentSpaceshipSkin != null) {
			try {
				// 스킨 정보를 서버로 전송하는 이벤트 생성
				GameEvent skinEvent = new GameEvent(GameEvent.Type.SYSTEM, 
					localPlayerId, "SKIN:" + localPlayerId + ":" + currentSpaceshipSkin, System.currentTimeMillis());
				networkAdapter.sendEvent(skinEvent);
				System.out.println("클라이언트: 서버에 스킨 정보 전송: " + currentSpaceshipSkin);
			} catch (Exception e) {
				System.err.println("스킨 정보 전송 실패: " + e.getMessage());
			}
		}
	}
	
	/**
	 * 지연된 스킨 정보 전송 (게임 시작 후 네트워크가 안정화된 후 전송)
	 */
	private void sendSkinToServerDelayed() {
		// 1초 후에 스킨 정보 전송
		new Thread(() -> {
			try {
				Thread.sleep(1000);
				sendSkinToServer();
				// 추가로 2초 후에도 한 번 더 전송 (확실하게 하기 위해)
				Thread.sleep(2000);
				sendSkinToServer();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "SkinSender").start();
	}
	
	/**
	 * 원격 플레이어의 스킨 적용
	 */
	private void applyRemotePlayerSkin(ShipEntity shipEntity, String playerId) {
		System.out.println("applyRemotePlayerSkin 호출: playerId=" + playerId + ", shipEntity=" + (shipEntity != null ? "존재" : "null"));
		if (shipEntity != null && playerId != null) {
			// 원격 플레이어의 스킨을 추정하여 적용
			// 실제로는 서버에서 스킨 정보를 받아야 하지만, 
			// 현재는 기본 스킨을 사용하거나 플레이어 ID 기반으로 추정
			String remoteSkin = SpriteConstants.SHIP_GIF; // 기본값
			
			// 플레이어 ID 기반으로 스킨 추정 (임시 로직)
			if (playerId.contains("player1") || playerId.contains("1")) {
				remoteSkin = "sprites/ships/spaceship_green.png";
			} else if (playerId.contains("player2") || playerId.contains("2")) {
				remoteSkin = "sprites/ships/spaceship_blue.png";
			} else if (playerId.contains("player3") || playerId.contains("3")) {
				remoteSkin = "sprites/ships/professor.png";
			}
			
			System.out.println("  - 추정된 스킨: " + remoteSkin);
			shipEntity.changeSkin(remoteSkin);
			System.out.println(REMOTE_PLAYER_LABEL + playerId + " 스킨 적용: " + remoteSkin);
		}
	}
	
	/**
	 * 원격 플레이어의 스킨 업데이트 (서버에서 받은 스킨 정보로)
	 */
	private void updateRemotePlayerSkin(String playerId, String skinPath) {
		System.out.println("updateRemotePlayerSkin 호출: playerId=" + playerId + ", skinPath=" + skinPath);
		if (playerId != null && skinPath != null) {
			// 해당 플레이어의 모든 엔티티를 찾아서 스킨 업데이트
			int foundEntities = 0;
			for (Entity entity : gameStateManager.getEntities()) {
				if (entity instanceof ShipEntity) {
					ShipEntity shipEntity = (ShipEntity) entity;
					System.out.println("  - ShipEntity 발견: ownerId=" + shipEntity.getOwnerId() + ", 찾는 playerId=" + playerId);
					// ownerId를 확인하여 해당 플레이어의 배인지 확인
					if (playerId.equals(shipEntity.getOwnerId())) {
						foundEntities++;
						System.out.println("  - 매칭되는 플레이어 배 발견! 스킨 변경 시도...");
						shipEntity.changeSkin(skinPath);
						System.out.println(REMOTE_PLAYER_LABEL + playerId + " 스킨 업데이트: " + skinPath);
					}
				}
			}
			System.out.println("  - 총 " + foundEntities + "개의 매칭되는 엔티티에 스킨 적용");
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
