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

import org.newdawn.spaceinvaders.multyplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.multyplay.entity.BossEntity;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ExplosionEntity;
import org.newdawn.spaceinvaders.multyplay.entity.MissileEntity;
import org.newdawn.spaceinvaders.multyplay.entity.NearEntity;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;
import org.newdawn.spaceinvaders.multyplay.entity.CoinDisplayEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShotEntity;
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
public class MultiplayerGameCanvas extends Canvas implements Screen, MultiplayerGameContext
{
	/** The stragey that allows us to use accelerate page flipping */
	// BufferStrategy는 상위 App에서 관리
	// entities and removeList are now managed by MultiplayerGameStateManager
	/** The entity representing the player */
	private ShipEntity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** UserManager for accessing equipped items */
	private UserManager userManager;
	/** ResolutionManager for handling resolution scaling */
	private ResolutionManager resolutionManager;
	// lastFire and firingInterval are now managed by MultiplayerGameStateManager
	/** 현재 장착된 우주선 스킨 경로 */
	private String currentSpaceshipSkin = "sprites/ship.gif";
	/** 현재 장착된 무기 스킨 경로 */
	private String currentWeaponSkin = "sprites/shot.gif";

	/** The current number of frames recorded */
	// FPS 표시 기능은 상위에서 처리 가능, 내부적으로는 카운트만 유지하지 않음
	
	/** The game state manager */
	private MultiplayerGameStateManager gameStateManager;
	/** The input manager */
	private MultiplayerInputManager inputManager;
	/** The skill manager */
	private MultiplayerSkillManager skillManager;
	/** The UI renderer */
	private MultiplayerUIRenderer uiRenderer;
	/** Background renderer (cached) */
	private BackgroundRenderer backgroundRenderer;
	// gameplay는 mainmenu 패키지에 의존하지 않도록, 오버레이는 MultiplayerUIRenderer에서 처리
	
	/** 메인메뉴 전환 요청 플래그 */
	private boolean requestMainMenu = false;

	/** 네트워크 어댑터 (싱글: LocalLoopback 기본) */
	private GameNetworkAdapter networkAdapter;
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
		setIgnoreRepaint(true);
		setBounds(0,0,800,600);
		setFocusable(true);
		
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
		backgroundRenderer = new BackgroundRenderer("sprites/backgrounds/Background-2.jpg");
		
		// initialize the input manager
		inputManager = new MultiplayerInputManager(gameStateManager, this);
		
		// add input handlers (after inputManager is initialized)
		addKeyListener(inputManager.new KeyInputHandler());
		addMouseListener(inputManager.new MouseInputHandler());
		
		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();

		// 기본 로컬 네트워크 어댑터 설정 (멀티 환경에서는 외부에서 교체)
		this.networkAdapter = new LocalLoopbackNetworkAdapter(gameStateManager);
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
		
		// 장착된 아이템 적용 (ShopManager 초기화 후)
		applyEquippedItems();
		
		// 엔티티 초기화 (스킨 적용 후)
		initEntities();
		
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
			gameStateManager.setMessage("🏆 GAME COMPLETED! 🏆 Congratulations!");
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
		ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", x, y, true);
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
		// Create skill drop using ShotEntity with skill drop functionality
		ShotEntity skillDrop = new ShotEntity(this, "sprites/shot.gif", x, y, false, skillType, skillValue);
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
			MissileEntity missile = new MissileEntity(this, "sprites/Skill/Missile.png",
					source.getX() + 15, source.getY(), targetX, targetY);
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
			return;
		}

		if (!gameStateManager.isWaitingForKeyPress() &&
			!gameStateManager.isShowingPauseMenu() &&
			!gameStateManager.isShowingSkillMenu()) {
			skillManager.updateSkillEffects();
			ArrayList<Entity> entities = new ArrayList<>(gameStateManager.getEntities());
			for (Entity entity : entities) {
				entity.move(delta);
			}
			tryAlienFire();
		}

		if (!remoteMode) {
			if ((ship == null || !gameStateManager.getEntities().contains(ship)) && localPlayerId != null) {
				ShipEntity candidate = getShip(localPlayerId);
				if (candidate != null) {
					ship = candidate;
				}
			}
		}

		if (ship != null && !gameStateManager.isShowingPauseMenu() && !gameStateManager.isShowingSkillMenu()) {
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

		ArrayList<Entity> entities = gameStateManager.getEntities();
		if (!gameStateManager.isShowingPauseMenu() && !gameStateManager.isShowingSkillMenu()) {
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
		Map<Long, Entity> next = new HashMap<>();
		snapshotEntitiesBuffer.clear();
		Entity localShipCandidate = null;
		boolean localShipVisible = false;
		for (EntitySnapshot snap : snapshot.entities) {
			Entity entity = remoteEntities.get(snap.id);
			if (entity == null) {
				entity = createRemoteEntity(snap);
			}
			entity.applySnapshot(snap);
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
		if (snapshot.players != null) {
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
                    gameStateManager.setEarnedCoins(state.coins);
					skillManager.setInvincibleSkills(state.invincibleCharges);
					skillManager.setTripleShotSkills(state.tripleShotCharges);
					skillManager.setMissileSkills(state.missileCharges);
					skillManager.setAttackPowerLevel(state.attackPowerLevel);
					skillManager.setAttackSpeedLevel(state.attackSpeedLevel);
					skillManager.setHpUpLevel(state.hpUpLevel);
					long now = System.currentTimeMillis();
					if (state.invincibleRemainingMs > 0) {
						skillManager.setInvincible(true);
						skillManager.setInvincibleEndTime(now + state.invincibleRemainingMs);
					} else {
						skillManager.setInvincible(false);
						skillManager.setInvincibleEndTime(0);
					}
					if (state.tripleShotRemainingMs > 0) {
						skillManager.setHasTripleShot(true);
						skillManager.setTripleShotEndTime(now + state.tripleShotRemainingMs);
					} else {
						skillManager.setHasTripleShot(false);
						skillManager.setTripleShotEndTime(0);
					}
				}
				playerDisplayNames.putIfAbsent(entry.getKey(), entry.getKey());
			}
		}
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
		if (localPlayerId != null && remotePhase == GameSnapshot.Phase.ACTIVE) {
			PlayerState localState = gameStateManager.getPlayerState(localPlayerId);
			boolean reportedDead = localState != null && localState.isDead();
			localSpectating = reportedDead && !localShipVisible;
		} else {
			localSpectating = false;
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

		if (!resultsPersisted && remotePhase == GameSnapshot.Phase.COMPLETED) {
			persistMultiplayerResults(false);
		}
	}

	private Entity createRemoteEntity(EntitySnapshot snapshot) {
		Map<String, String> meta = MetadataCodec.decode(snapshot.metadata);
		String type = snapshot.type != null ? snapshot.type : "";
		switch (type) {
			case "ShotEntity":
				return new RemoteShotEntity(snapshot, meta);
			case "ExplosionEntity":
				return new RemoteExplosionEntity(snapshot, meta);
			case "IceAttack":
				return new RemoteIceAttack(snapshot);
			case "IceBallAttack":
				return new RemoteIceBallAttack(snapshot);
			case "MagneticFieldEntity":
				return new RemoteMagneticField(snapshot, meta);
			case "Round2LaserAttack":
				return new RemoteRound2Laser(snapshot);
			case "Round2Phase1Attack":
				return new RemoteRound2Phase1(snapshot);
			case "Round2Phase2Attack":
				return new RemoteRound2Phase2(snapshot);
			case "Round2RandomAttack":
				return new RemoteRound2Random(snapshot);
		case "Round2QuadAttack":
			return new RemoteRound2Quad(snapshot);
			case "Round2MachineGunAttack":
				return new RemoteRound2MachineGun(snapshot);
			case "Round3StraightAttack":
				return new RemoteRound3Straight(snapshot);
			case "Round3RandomAttack":
				return new RemoteRound3Random(snapshot);
			case "Round3PullAttack":
				return new RemoteRound3Pull(snapshot);
			case "Round3BlackHoleAttack":
				return new RemoteRound3BlackHole(snapshot);
			case "Round4HealAttack":
				return new RemoteRound4Heal(snapshot);
			case "Round4GreenSphereAttack":
				return new RemoteRound4GreenSphere(snapshot, meta);
			case "Round4PlayerLineAttack":
				return new RemoteRound4PlayerLine(snapshot);
		case "BossEntity":
			return new RemoteBossEntity(snapshot, meta);
			case "BossShotEntity":
				return new RemoteBossShotEntity(snapshot, meta);
			case "HeatEffectEntity":
				return new RemoteHeatEffectEntity(snapshot.x, snapshot.y);
			case "NearEntity": {
				int round = 1;
				int monster = 0;
				try { round = Integer.parseInt(meta.getOrDefault("round", "1")); } catch (NumberFormatException ignore) {}
				try { monster = Integer.parseInt(meta.getOrDefault("monster", "0")); } catch (NumberFormatException ignore) {}
				NearEntity near = new NearEntity(this, (int) Math.round(snapshot.x), (int) Math.round(snapshot.y), round, monster);
				near.setOwnerId(snapshot.ownerId);
				return near;
			}
		case "AlienEntity": {
			String spritePath = snapshot.sprite != null && !snapshot.sprite.isEmpty()
					? snapshot.sprite
					: "sprites/Boss/1near.png";
			return new RemoteAlienEntity(spritePath, snapshot.x, snapshot.y);
		}
			default:
				String spritePath = snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : null;
				if (spritePath == null || spritePath.isEmpty()) {
					if ("ShipEntity".equals(type)) {
						spritePath = currentSpaceshipSkin;
					} else if ("AlienEntity".equals(type)) {
						spritePath = "sprites/Boss/1near.png";
					} else {
						spritePath = currentWeaponSkin;
					}
				}
				return new RemoteSpriteEntity(spritePath, snapshot.x, snapshot.y);
		}
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
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : "sprites/Boss/1Boss.png",
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
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : "sprites/shot.gif",
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
				radius = Integer.parseInt(meta.getOrDefault("radius", Integer.toString(radius)));
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
	}

	private static class RemoteHeatEffectEntity extends Entity {
		private static BufferedImage cachedHeat;
		private final long startTime = System.currentTimeMillis();
		private final long duration = 800;

		RemoteHeatEffectEntity(double x, double y) {
			super("sprites/Skill/Heat.gif", (int) Math.round(x), (int) Math.round(y));
			this.x = x;
			this.y = y;
			ensureHeatImage();
		}

		private static synchronized void ensureHeatImage() {
			if (cachedHeat != null) {
				return;
			}
			try (java.io.InputStream is = MultiplayerGameCanvas.class.getClassLoader().getResourceAsStream("sprites/Skill/Heat.gif")) {
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
				case CHAT: {
					String senderId = event.fromPlayerId != null ? event.fromPlayerId : "";
					String display = playerDisplayNames.getOrDefault(senderId, senderId.isEmpty() ? "" : senderId);
					String line = (display == null || display.isEmpty() ? "" : display + ": ") + (event.message != null ? event.message : "");
					appendIntermissionChat(line);
					break;
				}
				case SYSTEM: {
					String message = event.message != null ? event.message : "";
					if (!message.isEmpty()) {
						if (event.fromPlayerId != null && event.fromPlayerId.equals(localPlayerId)) {
							gameStateManager.setMessage(message);
							gameStateManager.setWaitingForKeyPress(true);
							gameStateManager.setRoundTransition(false);
						}
					}
					break;
				}
				case COIN_POPUP: {
					if (!remoteMode) {
						break;
					}
					String localId = localPlayerId != null ? localPlayerId : gameStateManager.getLocalPlayerId();
					if (localId == null || event.fromPlayerId == null || !event.fromPlayerId.equals(localId)) {
						break;
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
					break;
				}
				case SKILL_UPDATE:
					// Reserved for future skill synchronization events
					break;
				default:
					break;
			}
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
		private boolean hasPiercing;
		private boolean nearMonsterShot;

		RemoteShotEntity(EntitySnapshot snapshot, Map<String, String> meta) {
			super(resolveShotSprite(snapshot), (int) snapshot.x, (int) snapshot.y);
			apply(meta);
		}

		private static String resolveShotSprite(EntitySnapshot snapshot) {
			return snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : "sprites/shot.gif";
		}

		private void apply(Map<String, String> meta) {
			if (meta == null) return;
			isAlienShot = "1".equals(meta.get("alien"));
			isSkillDrop = "1".equals(meta.get("skill"));
			try { skillType = Integer.parseInt(meta.getOrDefault("skillType", "-1")); }
			catch (NumberFormatException ignore) { skillType = -1; }
			hasPiercing = "1".equals(meta.get("pierce"));
			nearMonsterShot = "1".equals(meta.get("near"));
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
				drawScaledSprite(g, 50);
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
				case 0: imagePath = "sprites/Skill/1.png"; break;
				case 1: imagePath = "sprites/Skill/2.png"; break;
				case 2: imagePath = "sprites/Skill/3.png"; break;
				case 3: imagePath = "sprites/Skill/4.png"; break;
				default: imagePath = "sprites/Skill/1.png"; break;
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
				g2d.setFont(new Font("Arial", Font.BOLD, 8));
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

		private void drawScaledSprite(Graphics g, int size) {
			if (sprite == null) {
				super.draw(g);
				return;
			}
			Graphics2D g2d = (Graphics2D) g;
			int drawX = (int) Math.round(x) - size / 2;
			int drawY = (int) Math.round(y) - size / 2;
			g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + size, drawY + size,
					0, 0, sprite.getWidth(), sprite.getHeight(), null);
		}

		@Override
		public java.awt.Rectangle getBounds() {
			if (nearMonsterShot) {
				int hitboxSize = 20;
				return new java.awt.Rectangle((int) x - hitboxSize / 2, (int) y - hitboxSize / 2, hitboxSize, hitboxSize);
			}
			return super.getBounds();
		}
	}

	private static class RemoteIceAttack extends Entity {
		private static final double SCALE = 0.8;
		private final Image animated;

		RemoteIceAttack(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/ice.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/ice.gif");
		}

		@Override
		public void move(long delta) {
			// remote entities are snapshot-driven
		}

		@Override
		public void collidedWith(Entity other) {
			// visuals only
		}

		@Override
		public java.awt.Rectangle getBounds() {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			int width = image != null ? (int) Math.round(image.getWidth(null) * SCALE) : 48;
			int height = image != null ? (int) Math.round(image.getHeight(null) * SCALE) : 48;
			int drawX = (int) Math.round(x) - width / 2;
			int drawY = (int) Math.round(y) - height / 2;
			return new java.awt.Rectangle(drawX, drawY, width, height);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int width = (int) Math.round(image.getWidth(null) * SCALE);
				int height = (int) Math.round(image.getHeight(null) * SCALE);
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
		private static final double SCALE = 0.6;
		private static final int FALLBACK_RADIUS = 10;
 		private final Image animated;

		RemoteIceBallAttack(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/ice ball.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
 			this.animated = loadAnimatedImage("sprites/Boss_Attack/ice ball.gif");
		}

		@Override
		public void move(long delta) {
			// snapshot-driven visuals only
		}

		@Override
		public void collidedWith(Entity other) {
			// visuals only
		}

		@Override
		public java.awt.Rectangle getBounds() {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int width = (int) Math.round(image.getWidth(null) * SCALE);
				int height = (int) Math.round(image.getHeight(null) * SCALE);
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				return new java.awt.Rectangle(drawX, drawY, width, height);
			}
			int drawX = (int) Math.round(x) - FALLBACK_RADIUS;
			int drawY = (int) Math.round(y) - FALLBACK_RADIUS;
			return new java.awt.Rectangle(drawX, drawY, FALLBACK_RADIUS * 2, FALLBACK_RADIUS * 2);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int width = (int) Math.round(image.getWidth(null) * SCALE);
				int height = (int) Math.round(image.getHeight(null) * SCALE);
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.drawImage(image, drawX, drawY, width, height, null);
			} else {
				int drawX = (int) Math.round(x) - FALLBACK_RADIUS;
				int drawY = (int) Math.round(y) - FALLBACK_RADIUS;
				g2d.setColor(new Color(120, 255, 255, 180));
				g2d.fillOval(drawX, drawY, FALLBACK_RADIUS * 2, FALLBACK_RADIUS * 2);
				g2d.setColor(new Color(210, 255, 255, 220));
				g2d.fillOval(drawX + 3, drawY + 3, (FALLBACK_RADIUS - 3) * 2, (FALLBACK_RADIUS - 3) * 2);
			}
		}
	}

	private static class RemoteMagneticField extends Entity {
		private final double radius;
		private final double strength;

		RemoteMagneticField(EntitySnapshot snapshot, Map<String, String> meta) {
			super("sprites/shot.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.radius = parseDouble(meta, "radius", 220.0);
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
			// visuals only
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
		private static final double LENGTH = 700;
		private static final double WIDTH = 300;
		private final Image animated;

		RemoteRound2Laser(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/2round.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/2round.gif");
		}

		@Override
		public void move(long delta) {
			// visuals only
		}

		@Override
		public void collidedWith(Entity other) {
			// visuals only
		}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x),
					(int) Math.round(y), (int) LENGTH, (int) WIDTH);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			AffineTransform original = g2d.getTransform();
			g2d.translate(x + LENGTH / 2.0, y + WIDTH / 2.0);
			g2d.rotate(Math.PI / 2);
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g2d.drawImage(image, (int) -(LENGTH / 2), (int) -(WIDTH / 2),
						(int) LENGTH, (int) WIDTH, null);
			} else {
				g2d.setColor(new Color(120, 200, 255, 180));
				g2d.fillRect((int) -(LENGTH / 2), (int) -(WIDTH / 2), (int) LENGTH, (int) WIDTH);
			}
			g2d.setTransform(original);
		}
	}

	private static class RemoteRound2Phase1 extends Entity {
		private static final int SIZE = 80;
		private final Image animated;

		RemoteRound2Phase1(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/2round3.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/2round3.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

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
				g.setColor(new Color(255, 150, 150, 200));
				g.fillOval((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
			}
		}
	}

	private static class RemoteRound2Phase2 extends Entity {
		private static final int SIZE = 50;
		private final Image animated;

		RemoteRound2Phase2(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/2round1.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/2round1.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

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
				g.setColor(new Color(255, 200, 120, 220));
				g.fillOval((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
			}
		}
	}

	private static class RemoteRound2Random extends Entity {
		private static final int WIDTH = 300;
		private static final int HEIGHT = 400;
		private final Image animated;

		RemoteRound2Random(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/2round2.gif", (int) Math.round(snapshot.x), 150);
			this.x = snapshot.x;
			this.y = 150;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/2round2.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - WIDTH / 2, (int) Math.round(y), WIDTH, HEIGHT);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - WIDTH / 2, 150, WIDTH, HEIGHT, null);
			} else {
				g.setColor(new Color(255, 200, 0, 128));
				g.fillRect((int) Math.round(x) - WIDTH / 2, 150, WIDTH, HEIGHT);
			}
		}
	}

	private static class RemoteRound2Quad extends Entity {
		private final Image animated;

		RemoteRound2Quad(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/2round1.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/2round1.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - 25, (int) Math.round(y) - 25, 50, 50);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - 25, (int) Math.round(y) - 25, 50, 50, null);
			} else {
				g.setColor(new Color(255, 180, 120, 200));
				g.fillOval((int) Math.round(x) - 25, (int) Math.round(y) - 25, 50, 50);
			}
		}
	}

	private static class RemoteRound2MachineGun extends Entity {
		private static final int SIZE = 30;
		private final Image animated;

		RemoteRound2MachineGun(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/2round1.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/2round1.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE, null);
			} else {
				g.setColor(new Color(255, 220, 120, 200));
				g.fillOval((int) Math.round(x) - SIZE / 2, (int) Math.round(y) - SIZE / 2, SIZE, SIZE);
			}
		}
	}

	private static class RemoteRound3Straight extends Entity {
		private static final int WIDTH = 600;
		private static final int HEIGHT = 300;
		private final Image animated;

		RemoteRound3Straight(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/3round4.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/3round4.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2, WIDTH, HEIGHT);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2,
						WIDTH, HEIGHT, null);
			} else {
				g.setColor(new Color(200, 30, 30, 140));
				g.fillRect((int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2, WIDTH, HEIGHT);
			}
		}
	}

	private static class RemoteRound3Random extends Entity {
		private static final int WIDTH = 400;
		private static final int HEIGHT = 1400;
		private final Image animated;

		RemoteRound3Random(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/3round2.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/3round2.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2, WIDTH, HEIGHT);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2,
						WIDTH, HEIGHT, null);
			} else {
				g.setColor(new Color(255, 80, 80, 120));
				g.fillRect((int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2, WIDTH, HEIGHT);
			}
		}
	}

	private static class RemoteRound3Pull extends Entity {
		private static final int WIDTH = 300;
		private static final int HEIGHT = 400;
		private final Image animated;

		RemoteRound3Pull(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/3round3.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/3round3.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			return new java.awt.Rectangle((int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2, WIDTH, HEIGHT);
		}

		@Override
		public void draw(Graphics g) {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				g.drawImage(image, (int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2,
						WIDTH, HEIGHT, null);
			} else {
				g.setColor(new Color(80, 120, 255, 120));
				g.fillRect((int) Math.round(x) - WIDTH / 2, (int) Math.round(y) - HEIGHT / 2, WIDTH, HEIGHT);
			}
		}
	}

	private static class RemoteRound3BlackHole extends Entity {
		private static final int SIZE = 150;
		private final Image animated;

		RemoteRound3BlackHole(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/3round.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/3round.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

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
			super("sprites/Boss_Attack/4round.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/4round.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

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
		private static final double SCALE = 0.6;
		private static final int FALLBACK_SIZE = 24;
		private final Image animated;

		RemoteRound4GreenSphere(EntitySnapshot snapshot, Map<String, String> meta) {
			super("sprites/Boss_Attack/5round1.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/5round1.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

		@Override
		public java.awt.Rectangle getBounds() {
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int width = (int) Math.round(image.getWidth(null) * SCALE);
				int height = (int) Math.round(image.getHeight(null) * SCALE);
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				return new java.awt.Rectangle(drawX, drawY, width, height);
			}
			return new java.awt.Rectangle((int) Math.round(x) - FALLBACK_SIZE / 2,
					(int) Math.round(y) - FALLBACK_SIZE / 2, FALLBACK_SIZE, FALLBACK_SIZE);
		}

		@Override
		public void draw(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Image image = animated != null ? animated : (sprite != null ? sprite.getImage() : null);
			if (image != null) {
				int width = (int) Math.round(image.getWidth(null) * SCALE);
				int height = (int) Math.round(image.getHeight(null) * SCALE);
				int drawX = (int) Math.round(x) - width / 2;
				int drawY = (int) Math.round(y) - height / 2;
				g2d.drawImage(image, drawX, drawY, width, height, null);
			} else {
				int drawX = (int) Math.round(x) - FALLBACK_SIZE / 2;
				int drawY = (int) Math.round(y) - FALLBACK_SIZE / 2;
				g2d.setColor(new Color(120, 255, 120, 220));
				g2d.fillOval(drawX, drawY, FALLBACK_SIZE, FALLBACK_SIZE);
				g2d.setColor(new Color(200, 255, 200, 160));
				g2d.fillOval(drawX + 2, drawY + 2, FALLBACK_SIZE - 4, FALLBACK_SIZE - 4);
			}
		}
	}

	private static class RemoteRound4PlayerLine extends Entity {
		private static final int SIZE = 160;
		private final Image animated;

		RemoteRound4PlayerLine(EntitySnapshot snapshot) {
			super("sprites/Boss_Attack/4round3.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
			this.x = snapshot.x;
			this.y = snapshot.y;
			this.animated = loadAnimatedImage("sprites/Boss_Attack/4round3.gif");
		}

		@Override
		public void move(long delta) {}

		@Override
		public void collidedWith(Entity other) {}

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
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : "sprites/Skill/Explosion.png", (int) snapshot.x, (int) snapshot.y);
			loadImage();
			apply(meta);
		}

		private void apply(Map<String, String> meta) {
			if (meta == null) return;
			try { currentRadius = Double.parseDouble(meta.getOrDefault("radius", "0")); }
			catch (NumberFormatException ignore) { currentRadius = 0; }
		}

		@Override
		protected void applySnapshotMetadata(String metadata) {
			apply(MetadataCodec.decode(metadata));
		}

		@Override
		public void move(long delta) {
			// no-op
		}

		@Override
		public void collidedWith(Entity other) {
			// no-op
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

		private void loadImage() {
			if (cachedImage != null) return;
			try (java.io.InputStream is = MultiplayerGameCanvas.class.getClassLoader().getResourceAsStream("sprites/Skill/Explosion.png")) {
				if (is != null) {
					cachedImage = ImageIO.read(is);
				}
			} catch (Exception ignored) {}
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
		drawRemoteCoinPopups(g);
		// UI & overlays
	uiRenderer.drawGameUI(g, gameStateManager, skillManager);
	if (isLocalSpectatorActive()) {
		drawSpectatorOverlay(g);
	}
	if (gameStateManager.isShowingPauseMenu()) { drawPauseMenu(g); }
	if (isIntermissionOverlayVisible()) {
		drawIntermissionOverlay(g);
	}
	if (gameStateManager.isWaitingForKeyPress()) {
		uiRenderer.drawMessage(g, gameStateManager.getMessage());
	}
	if (gameStateManager.isShowingSkillMenu()) { drawSkillMenu(g); }
	}
	
	private void drawRemoteCoinPopups(Graphics2D g) {
		if (!remoteMode || remoteCoinPopups.isEmpty()) {
			return;
		}
		for (CoinDisplayEntity popup : remoteCoinPopups) {
			popup.draw(g);
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

		g.setColor(new Color(0, 0, 0, 180));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
		g.setColor(new Color(255, 255, 255, 90));
		g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

		g.setFont(new Font("Arial", Font.BOLD, 22));
		g.setColor(Color.WHITE);
		g.drawString("관전 모드", panelX + 24, panelY + 36);

		g.setFont(new Font("Arial", Font.PLAIN, 14));
		g.setColor(new Color(225, 225, 225));
		g.drawString("당신의 함선이 파괴되었습니다.", panelX + 24, panelY + 64);
		g.drawString("라운드 종료까지 관전을 계속합니다.", panelX + 24, panelY + 82);

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

		int listY = panelY + 110;
		g.setFont(new Font("Arial", Font.BOLD, 14));
		g.setColor(new Color(120, 255, 160));
		g.drawString("생존 플레이어", panelX + 24, listY);
		listY += 18;
		g.setFont(new Font("Arial", Font.PLAIN, 13));
		if (alive.isEmpty()) {
			g.setColor(new Color(200, 200, 200));
			g.drawString("• 없음", panelX + 24, listY);
			listY += 18;
		} else {
			g.setColor(new Color(200, 255, 200));
			for (String label : alive) {
				g.drawString("• " + label, panelX + 24, listY);
				listY += 18;
			}
		}

		listY += 4;
		g.setFont(new Font("Arial", Font.BOLD, 14));
		g.setColor(new Color(255, 200, 160));
		g.drawString("전투 불능", panelX + 24, listY);
		listY += 18;
		g.setFont(new Font("Arial", Font.PLAIN, 13));
		if (defeated.isEmpty()) {
			g.setColor(new Color(200, 200, 200));
			g.drawString("• 없음", panelX + 24, listY);
			listY += 18;
		} else {
			g.setColor(new Color(240, 200, 200));
			for (String label : defeated) {
				g.drawString("• " + label, panelX + 24, listY);
				listY += 18;
			}
		}

		g.setFont(new Font("Arial", Font.PLAIN, 12));
		g.setColor(new Color(200, 200, 200));
		g.drawString("Enter: 채팅  ESC: 로비로 돌아가기", panelX + 24, panelY + panelHeight - 28);
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

		g.setColor(new Color(0, 0, 0, 180));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 18, 18);
		g.setColor(new Color(255, 255, 255, 90));
		g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 18, 18);

		String title = remotePhase == GameSnapshot.Phase.COMPLETED ? "게임 종료" : "라운드 준비";
		Font titleFont = new Font("Arial", Font.BOLD, 24);
		g.setFont(titleFont);
		g.setColor(Color.WHITE);
		g.drawString(title, panelX + 24, panelY + 40);

		if (intermissionMessage != null && !intermissionMessage.isEmpty()) {
			g.setFont(new Font("Arial", Font.PLAIN, 16));
			g.setColor(new Color(220, 220, 220));
			g.drawString(intermissionMessage, panelX + 24, panelY + 70);
		}

		int listX = panelX + 24;
		int listY = panelY + 100;
		g.setFont(new Font("Arial", Font.BOLD, 16));
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

		g.setFont(new Font("Arial", Font.PLAIN, 13));
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
			ExplosionEntity explosion = new ExplosionEntity(this, "sprites/Skill/Explosion.png", x, y, radius);
			explosion.setOwnerId(localPlayerId);
			gameStateManager.getEntities().add(explosion);
		} catch (Exception e) {
			System.err.println("Error creating explosion: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Add score points
	 * 
	 * @param points Points to add
	 */
	@Override
	public void addScore(String playerId, int points) {
		// 점수 추가
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
			gameStateManager.setMessage("🏆 GAME COMPLETED! 🏆 Congratulations!");
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
			gameStateManager.setMessage("🏆 GAME COMPLETED! 🏆 Congratulations!");
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

		if (names.isEmpty()) {
			for (PlayerState ps : gameStateManager.getPlayerStates()) {
				String playerId = ps.getPlayerId();
				if (playerId == null || (localId != null && localId.equals(playerId))) {
					continue;
				}
				names.add(playerId);
			}
		}

		return new ArrayList<>(names);
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
