package org.newdawn.spaceinvaders.multyplay.core;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
// no direct AWT listeners here; handled via MultiplayerInputManager
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.multyplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.multyplay.entity.BossEntity;
import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.ExplosionEntity;
import org.newdawn.spaceinvaders.multyplay.entity.MissileEntity;
import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;
import org.newdawn.spaceinvaders.multyplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.multyplay.entity.ShotEntity;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.shop.ShopCategory;
import org.newdawn.spaceinvaders.shop.ShopItem;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.multyplay.input.MultiplayerInputManager;
import org.newdawn.spaceinvaders.multyplay.net.GameNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.GameSnapshot;
import org.newdawn.spaceinvaders.multyplay.net.PlayerInput;
import org.newdawn.spaceinvaders.multyplay.net.LocalLoopbackNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;
import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.state.PlayerState;
import org.newdawn.spaceinvaders.multyplay.ui.MultiplayerUIRenderer;
import org.newdawn.spaceinvaders.room.GameInitInfo;

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
	private Entity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** UserManager for accessing equipped items */
	private UserManager userManager;
	/** ResolutionManager for handling resolution scaling */
	private ResolutionManager resolutionManager;
	// lastFire and firingInterval are now managed by MultiplayerGameStateManager
	/** The number of aliens left on the screen */
	private int alienCount;
	
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
	private String remoteRoomId;
	private String localPlayerId;
	private long inputSequence;
	private long lastInputSendTime;
	private boolean lastSentLeft;
	private boolean lastSentRight;
	private boolean lastSentFire;
	private final Map<Long, Entity> remoteEntities = new HashMap<>();
	private final ArrayList<Entity> snapshotEntitiesBuffer = new ArrayList<>();
	
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
		this.remoteRoomId = initInfo != null ? initInfo.roomId : null;
		this.inputSequence = 0;
		this.lastInputSendTime = 0;
		this.lastSentLeft = false;
		this.lastSentRight = false;
		this.lastSentFire = false;
		this.remoteEntities.clear();
		this.snapshotEntitiesBuffer.clear();
		gameStateManager.getEntities().clear();
		gameStateManager.getRemoveList().clear();
		gameStateManager.setWaitingForKeyPress(false);
		if (initInfo != null && initInfo.players != null) {
			for (GameInitInfo.Player p : initInfo.players) {
				gameStateManager.ensurePlayer(p.id);
				if (this.localPlayerId == null) {
					this.localPlayerId = p.id;
				}
			}
		} else if (this.localPlayerId == null) {
			this.localPlayerId = "local";
			gameStateManager.ensurePlayer(this.localPlayerId);
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
		// create the player ship and place it roughly in the center of the screen
		ship = new ShipEntity(this, currentSpaceshipSkin, 370, 550);
		ship.setOwnerId(localPlayerId);
		gameStateManager.getEntities().add(ship);
		
		// Create aliens based on current round with balanced progression
		alienCount = 0;
		int rows, cols;
		
		switch (gameStateManager.getCurrentRound()) {
			case 1: rows = 2; cols = 5; break;  // 10 aliens (이전: 18)
			case 2: rows = 3; cols = 5; break;  // 15 aliens (이전: 21)
			case 3: rows = 3; cols = 6; break;  // 18 aliens (이전: 28)
			case 4: rows = 3; cols = 7; break;  // 21 aliens (이전: 32)
			case 5: rows = 4; cols = 7; break;  // 28 aliens (이전: 40)
			default: rows = 2; cols = 5; break;
		}
		
		// 화면 너비에 맞춰서 적들을 균등하게 배치
		int screenWidth = 800;
		int margin = 50; // 양쪽 여백
		int usableWidth = screenWidth - (2 * margin);
		int spacingX = usableWidth / (cols + 1); // 적들 사이 간격
		int spacingY = 80; // 세로 간격
		
		for (int row=0; row<rows; row++) {
			for (int x=0; x<cols; x++) {
				// 적들을 화면에 균등하게 배치
				int posX = margin + spacingX * (x + 1);
				int posY = 80 + (row * spacingY);
				Entity alien = new AlienEntity(this, posX, posY);
				gameStateManager.getEntities().add(alien);
				alienCount++;
			}
		}
		
		gameStateManager.setAlienCount(alienCount);
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
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		boolean roundAdvanced = gameStateManager.advanceRound();
		
		if (roundAdvanced) {
			// Check if this is a boss round (after round 1, 3, 5, etc.)
			if (gameStateManager.getCurrentRound() == 2 || 
				gameStateManager.getCurrentRound() == 4 || 
				gameStateManager.getCurrentRound() == 6) {
				// Boss round - spawn boss instead of regular aliens
				spawnBoss();
			} else {
				// Regular round - clear current entities and initialize next round
				gameStateManager.getEntities().clear();
				initEntities();
			}
		} else {
			// Game completed
			gameStateManager.setMessage("Well done! You Win!");
			gameStateManager.setWaitingForKeyPress(true);
		}
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	@Override
	public void notifyAlienKilled(String killerPlayerId) {
		String targetId = killerPlayerId != null ? killerPlayerId : gameStateManager.getLocalPlayerId();

		// Give random skill points for killing aliens
		int earnedPoints = skillManager.getRandomSkillPoints(gameStateManager.getCurrentRound());
		PlayerState ps = gameStateManager.ensurePlayer(targetId);
		ps.addSkillPoints(earnedPoints);

		// Random chance to drop a skill (world drop, not player-specific)
		double dropChance = skillManager.getSkillDropChance(gameStateManager.getCurrentRound());
		if (Math.random() < dropChance) {
			skillManager.dropSkill(gameStateManager.getCurrentRound());
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
		
		if (remainingAliens == 0) {
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
	 * @param skillType The type of skill (0: Invincible, 1: Piercing, 2: Triple Shot)
	 * @param skillValue The value/duration of the skill
	 */
	@Override
	public void createSkillDrop(int x, int y, int skillType, int skillValue) {
		// Create skill drop using ShotEntity with skill drop functionality
		ShotEntity skillDrop = new ShotEntity(this, "sprites/shot.gif", x, y, false, skillType, skillValue);
		gameStateManager.getEntities().add(skillDrop);
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
	 * Check if player has piercing shots
	 */
	@Override
	public boolean hasPiercingShots(String playerId) {
		String targetId = playerId != null ? playerId : gameStateManager.getLocalPlayerId();
		if (targetId != null && targetId.equals(gameStateManager.getLocalPlayerId())) {
			return skillManager.hasPiercing();
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
			Entity source = getShip(playerId);
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
				networkAdapter.drainEvents();
			}
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
		Map<Long, Entity> next = new HashMap<>();
		snapshotEntitiesBuffer.clear();
		Entity localShipCandidate = null;
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
			}
		}
		remoteEntities.clear();
		remoteEntities.putAll(next);
		gameStateManager.getEntities().clear();
		gameStateManager.getEntities().addAll(snapshotEntitiesBuffer);
		gameStateManager.getRemoveList().clear();
		if (localShipCandidate != null) {
			ship = localShipCandidate;
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
			}
		}
		gameStateManager.setCurrentRound(snapshot.round);
		gameStateManager.setWaitingForKeyPress(false);
	}

	private Entity createRemoteEntity(EntitySnapshot snapshot) {
		Map<String, String> meta = MetadataCodec.decode(snapshot.metadata);
		String type = snapshot.type != null ? snapshot.type : "";
		switch (type) {
			case "ShotEntity":
				return new RemoteShotEntity(snapshot, meta);
			case "ExplosionEntity":
				return new RemoteExplosionEntity(snapshot, meta);
			default:
				String spritePath = snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : null;
				if (spritePath == null || spritePath.isEmpty()) {
					if ("ShipEntity".equals(type)) {
						spritePath = currentSpaceshipSkin;
					} else if ("AlienEntity".equals(type)) {
						spritePath = "sprites/Boss/1round_small.png";
					} else {
						spritePath = currentWeaponSkin;
					}
				}
				return new RemoteSpriteEntity(spritePath, snapshot.x, snapshot.y);
		}
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
			try { skillValue = Integer.parseInt(meta.getOrDefault("skillValue", "0")); }
			catch (NumberFormatException ignore) { skillValue = 0; }
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
				Graphics2D g2d = (Graphics2D) g;
				BufferedImage img = loadSkillImage(skillType);
				if (img != null) {
					int imageSize = 32;
					int drawX = (int) x - imageSize / 2;
					int drawY = (int) y - imageSize / 2;
					g2d.drawImage(img, drawX, drawY, imageSize, imageSize, null);
				} else {
					g2d.setColor(Color.BLACK);
					g2d.fillRect((int) x - 16, (int) y - 16, 32, 32);
					g2d.setColor(Color.GRAY);
					g2d.drawRect((int) x - 16, (int) y - 16, 32, 32);
					g2d.setColor(Color.WHITE);
					g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
					g2d.drawString("S" + skillType, (int) x - 8, (int) y + 4);
				}
				g2d.setColor(new Color(255, 255, 255, 50));
				g2d.fillOval((int) x - 18, (int) y - 18, 36, 36);
			} else if (isAlienShot) {
				Graphics2D g2d = (Graphics2D) g;
				int w = sprite != null ? sprite.getWidth() : 16;
				int h = sprite != null ? sprite.getHeight() : 16;
				int centerX = (int) x + w / 2;
				int centerY = (int) y + h / 2;
				int radius = 8;
				g2d.setColor(new Color(255, 100, 100, 60));
				g2d.fillOval(centerX - radius - 3, centerY - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
				g2d.setColor(new Color(255, 50, 50, 220));
				g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
				g2d.setColor(new Color(255, 200, 200, 180));
				g2d.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);
				g2d.setColor(new Color(255, 255, 255, 200));
				g2d.fillOval(centerX - radius + 4, centerY - radius + 4, (radius - 4) * 2, (radius - 4) * 2);
				g2d.setColor(new Color(255, 100, 100, 80));
				g2d.fillOval(centerX - radius + 1, centerY - radius + 6, (radius - 1) * 2, (radius - 1) * 2);
				g2d.setColor(new Color(255, 100, 100, 40));
				g2d.fillOval(centerX - radius + 2, centerY - radius + 10, (radius - 2) * 2, (radius - 2) * 2);
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
	}

	private static class RemoteExplosionEntity extends Entity {
		private static BufferedImage cachedImage;
		private double currentRadius;
		private double maxRadius;

		RemoteExplosionEntity(EntitySnapshot snapshot, Map<String, String> meta) {
			super(snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : "sprites/Skill/Explosion.png", (int) snapshot.x, (int) snapshot.y);
			loadImage();
			apply(meta);
		}

		private void apply(Map<String, String> meta) {
			if (meta == null) return;
			try { currentRadius = Double.parseDouble(meta.getOrDefault("radius", "0")); }
			catch (NumberFormatException ignore) { currentRadius = 0; }
			try { maxRadius = Double.parseDouble(meta.getOrDefault("maxRadius", "0")); }
			catch (NumberFormatException ignore) { maxRadius = 0; }
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
		// UI & overlays
		uiRenderer.drawGameUI(g, gameStateManager, skillManager);
		if (gameStateManager.isShowingPauseMenu()) { drawPauseMenu(g); }
		if (gameStateManager.isShowingSkillMenu()) { drawSkillMenu(g); }
		if (gameStateManager.isWaitingForKeyPress()) {
			uiRenderer.drawMessage(g, gameStateManager.getMessage());
		}
	}
	
	
	/**
	 * Getter methods for MultiplayerInputManager
	 */
	@Override
	public Entity getShip(String playerId) {
		if (playerId == null) {
			return ship;
		}
		for (Entity entity : gameStateManager.getEntities()) {
			if (entity instanceof ShipEntity && playerId.equals(entity.getOwnerId())) {
				return entity;
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
	
	/**
	 * Spawn a boss for the current round
	 */
	public void spawnBoss() {
		try {
			int round = gameStateManager.getCurrentRound();
			BossEntity boss = new BossEntity(this, 400, 120, round); // Center, slightly lower
			gameStateManager.getEntities().add(boss);
			
			// 보스 좌우에 1round_small.png 몬스터 추가 (조금 띄어서 배치)
			AlienEntity leftAlien = new AlienEntity(this, 200, 120); // 보스 왼쪽 (더 멀리)
			AlienEntity rightAlien = new AlienEntity(this, 600, 120); // 보스 오른쪽 (더 멀리)
			gameStateManager.getEntities().add(leftAlien);
			gameStateManager.getEntities().add(rightAlien);
			
		// 보스 스폰 완료
			
			gameStateManager.setMessage("⚠️ BOSS APPEARED! ⚠️");
			gameStateManager.setWaitingForKeyPress(true);
		} catch (Exception e) {
			System.err.println("Error spawning boss: " + e.getMessage());
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
	@Override
	public void notifyBossDefeated(String playerId) {
		// 보스 처치 완료
		
		gameStateManager.setMessage("🎉 BOSS DEFEATED! 🎉 Round " + gameStateManager.getCurrentRound() + " Complete!");
		gameStateManager.setWaitingForKeyPress(true);
		
		// Advance to next round after boss defeat
		boolean roundAdvanced = gameStateManager.advanceRound();
		if (roundAdvanced) {
			// Clear entities and start next round
			gameStateManager.getEntities().clear();
			initEntities();
		} else {
			// Game completed
			gameStateManager.setMessage("🏆 GAME COMPLETED! 🏆 Congratulations!");
			gameStateManager.setWaitingForKeyPress(true);
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
