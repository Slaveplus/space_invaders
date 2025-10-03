package org.newdawn.spaceinvaders.gameplay;

import java.awt.Canvas;
import java.awt.Graphics2D;
// no direct AWT listeners here; handled via InputManager
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.SystemTimer;
import org.newdawn.spaceinvaders.gameplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.gameplay.entity.BossEntity;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.entity.ExplosionEntity;
import org.newdawn.spaceinvaders.gameplay.entity.MissileEntity;
import org.newdawn.spaceinvaders.gameplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.gameplay.entity.ShotEntity;
import org.newdawn.spaceinvaders.gameplay.entity.Skill;

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
public class Game extends Canvas implements Screen
{
	/** The stragey that allows us to use accelerate page flipping */
	// BufferStrategy는 상위 App에서 관리
	// entities and removeList are now managed by GameStateManager
	/** The entity representing the player */
	private Entity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** UserManager for accessing equipped items */
	private UserManager userManager;
	// lastFire and firingInterval are now managed by GameStateManager
	/** The number of aliens left on the screen */
	private int alienCount;
	
	/** 현재 장착된 우주선 스킨 경로 */
	private String currentSpaceshipSkin = "sprites/ship.gif";
	/** 현재 장착된 무기 스킨 경로 */
	private String currentWeaponSkin = "sprites/shot.gif";

	/** The current number of frames recorded */
	// FPS 표시 기능은 상위에서 처리 가능, 내부적으로는 카운트만 유지하지 않음
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders 102";
	/** The game window that we'll update with the frame count */
	private JFrame container;
	/** Cached background image for better performance */
	private java.awt.image.BufferedImage cachedBackgroundImage;
	
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
	// gameplay는 mainmenu 패키지에 의존하지 않도록, 오버레이는 UIRenderer에서 처리
	
	/**
	 * Construct our game and set it running.
	 */
	public Game() {
		setIgnoreRepaint(true);
		setBounds(0,0,800,600);
		setFocusable(true);
		
		// initialize the game state manager
		gameStateManager = new GameStateManager();
		
		// initialize the skill manager
		skillManager = new SkillManager(this);
		
		// initialize the UI renderer
		uiRenderer = new UIRenderer(this);

		// background & overlays
		backgroundRenderer = new BackgroundRenderer("sprites/backgrounds/Background-2.jpg");
		
		// initialize the input manager
		inputManager = new InputManager(gameStateManager, this);
		
		// add input handlers (after inputManager is initialized)
		addKeyListener(inputManager.new KeyInputHandler());
		addMouseListener(inputManager.new MouseInputHandler());
		
		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();
	}

	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	public void startGame() {
		// 게임플레이 상태 초기화 (entities.clear() 포함)
		gameStateManager.startNewGame();
		
		// 엔티티 초기화 (startNewGame() 후에 호출)
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
	public void notifyAlienKilled() {
		// Give random skill points for killing aliens
		int earnedPoints = skillManager.getRandomSkillPoints(gameStateManager.getCurrentRound());
		gameStateManager.addSkillPoints(earnedPoints);
		
		// Random chance to drop a skill
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
			gameStateManager.getEntities().add(shot1);
			gameStateManager.getEntities().add(shot2);
			gameStateManager.getEntities().add(shot3);
		} else {
			// Fire single shot
			ShotEntity shot = new ShotEntity(this, currentWeaponSkin, ship.getX()+10, ship.getY()-30);
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
	 * Check if player has piercing shots
	 */
	public boolean hasPiercingShots() {
		return skillManager.hasPiercing();
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
	
	/**
	 * Fire missile at a target location
	 * 
	 * @param targetX The target x location
	 * @param targetY The target y location
	 */
	public void fireMissile(double targetX, double targetY) {
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
	 * Add entity to the game
	 * 
	 * @param entity The entity to add
	 */
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
		// 게임플레이 업데이트 (Game 화면은 항상 게임플레이)
		if (!gameStateManager.isWaitingForKeyPress() &&
			!gameStateManager.isShowingPauseMenu() &&
			!gameStateManager.isShowingSkillMenu()) {
			// Update skill effects
			skillManager.updateSkillEffects();
			ArrayList<Entity> entities = gameStateManager.getEntities();
			for (Entity entity : entities) {
				entity.move(delta);
			}
			tryAlienFire();
		}

		// Ship movement & fire
		if (ship != null && !gameStateManager.isShowingPauseMenu() && !gameStateManager.isShowingSkillMenu()) {
			ship.setHorizontalMovement(0);
			if (inputManager.isLeftPressed() && !inputManager.isRightPressed()) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if (inputManager.isRightPressed() && !inputManager.isLeftPressed()) {
				ship.setHorizontalMovement(moveSpeed);
			}
<<<<<<< HEAD
			if (inputManager.isFirePressed()) {
				tryToFire();
			}
		}

		// collisions
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

	public void render(Graphics2D g) {
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
			gameStateManager.getSelectedSkill()
		);
	}
	
	/**
	 * 일시정지 메뉴 그리기
	 */
	public void drawPauseMenu(java.awt.Graphics2D g2d) {
		uiRenderer.drawPauseOverlay(g2d, gameStateManager.getSelectedPauseMenuItem());
	}

	/**
	 * 게임플레이 배경 그리기
	 */
	private void drawGameplayBackground(java.awt.Graphics2D g) {
		try {
			// Load background image only once (cache it)
			if (cachedBackgroundImage == null) {
				cachedBackgroundImage = javax.imageio.ImageIO.read(
					getClass().getResourceAsStream("/sprites/backgrounds/Background-3.jpg")
				);
			}
			
			// Draw cached background image scaled to fit screen
			g.drawImage(cachedBackgroundImage, 0, 0, 800, 600, null);
		} catch (Exception e) {
			// Fallback to black background if image loading fails
			g.setColor(java.awt.Color.black);
			g.fillRect(0, 0, 800, 600);
		}
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
	 * Add score points
	 * 
	 * @param points Points to add
	 */
	public void addScore(int points) {
		System.out.println("Score added: " + points);
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
			
			System.out.println("BOSS SPAWNED! Round " + round + " Boss with " + boss.getMaxHP() + " HP!");
			System.out.println("Side aliens added to boss fight!");
			
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
	public void notifyBossDefeated() {
		System.out.println("BOSS DEFEATED! Round " + gameStateManager.getCurrentRound() + " completed!");
		
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
			gameStateManager.handleGameEnd();
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
	 * The entry point into the game. We'll simply create an
	 * instance of class which will start the display and game
	 * loop.
	 * 
	 * @param argv The arguments that are passed into our game
	 */
	public static void main(String argv[]) {
		Game g = new Game();

		// Start the main game loop, note: this method will not
		// return until the game has finished running. Hence we are
		// using the actual main thread to run the game.
		g.gameLoop();
	}
	
}
