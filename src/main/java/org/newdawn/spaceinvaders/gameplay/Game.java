package org.newdawn.spaceinvaders.gameplay;

import java.awt.Canvas;
import java.awt.Graphics2D;
// no direct AWT listeners here; handled via InputManager
import java.util.ArrayList;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.shop.ShopCategory;
import org.newdawn.spaceinvaders.shop.ShopItem;
import org.newdawn.spaceinvaders.gameplay.entity.AlienEntity;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.entity.ShipEntity;
import org.newdawn.spaceinvaders.gameplay.entity.ShotEntity;

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
	// 창 제목은 상위 App에서 관리
	/** navigator for screen transitions */
	private final ScreenNavigator navigator;
	
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
	public Game(ScreenNavigator navigator) {
		this.navigator = navigator;
		this.userManager = null;
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

	@Override
	public void onShow() {
		// 게임 화면이 표시될 때 포커스 보장
		requestFocusInWindow();
	}

	@Override
	public void onHide() {
		// 현재는 리스너를 생성자에서 등록했으므로 별도 해제는 없음.
		// 필요 시 입력 리셋 등 처리 가능
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
		
		// 장착된 아이템 적용
		applyEquippedItems();
	}
	
	
	/**
	 * UserManager 설정
	 */
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
		// ShopManager의 장착 정보 동기화
		if (userManager != null && userManager.isLoggedIn()) {
			userManager.getShopManager().loadInventoryFromDB();
			userManager.getShopManager().loadEquipmentFromDB();
			System.out.println("Game: UserManager 설정 완료 - 장착 정보 동기화");
		}
		this.userManager = userManager;
	}

	
	/**
	 * 현재 우주선 스킨 경로 반환
	 */
	public String getCurrentSpaceshipSkin() {
		return currentSpaceshipSkin;
	}
	
	/**
	 * 현재 무기 스킨 경로 반환
	 */
	public String getCurrentWeaponSkin() {
		return currentWeaponSkin;
	}
	
	/**
	 * 장착된 아이템을 게임에 적용
	 */
	private void applyEquippedItems() {
		if (userManager == null || !userManager.isLoggedIn()) {
			System.out.println("Game: UserManager가 없거나 로그인되지 않음 - 기본 설정 사용");
			return;
	}
		
		System.out.println("Game: 장착된 아이템 적용 시작");
		
		// 장착된 우주선 적용
		ShopItem equippedSpaceship = getEquippedItem(ShopCategory.SPACESHIPS);
		if (equippedSpaceship != null) {
			System.out.println("Game: 우주선 적용 - " + equippedSpaceship.getName());
			// 우주선 스킨 변경
			applySpaceshipSkin(equippedSpaceship);
		} else {
			// 기본 우주선 스킨 사용
			currentSpaceshipSkin = "sprites/ship.gif";
		}
		
		// 장착된 무기 적용
		ShopItem equippedWeapon = getEquippedItem(ShopCategory.WEAPONS);
		if (equippedWeapon != null) {
			System.out.println("Game: 무기 적용 - " + equippedWeapon.getName());
			// 무기 스킨 및 효과 적용
			applyWeaponSkin(equippedWeapon);
			applyWeaponEffects(equippedWeapon);
		} else {
			// 기본 무기 스킨 사용
			currentWeaponSkin = "sprites/shot.gif";
		}
		
		// 장착된 파워업 적용
		ShopItem equippedPowerup = getEquippedItem(ShopCategory.POWERUPS);
		if (equippedPowerup != null) {
			System.out.println("Game: 파워업 적용 - " + equippedPowerup.getName());
			// 파워업 효과 적용
			applyPowerupEffects(equippedPowerup);
		}
	}
	
	/**
	 * 특정 카테고리의 장착된 아이템 가져오기
	 */
	private ShopItem getEquippedItem(ShopCategory category) {
		System.out.println("Game: getEquippedItem 호출 - category: " + category);
		System.out.println("Game: userManager = " + (userManager != null ? "존재" : "null"));
		if (userManager != null) {
			System.out.println("Game: isLoggedIn = " + userManager.isLoggedIn());
		}
		if (userManager == null || !userManager.isLoggedIn()) {
			return null;
		}
		
		// UserManager에서 ShopManager를 통해 장착된 아이템 가져오기
		ShopItem result = userManager.getShopManager().getEquippedItem(category);
		System.out.println("Game: getEquippedItem 결과 = " + (result != null ? result.getName() : "null"));
		return result;
	}
	
	/**
	 * 우주선 스킨 적용
	 */
	private void applySpaceshipSkin(ShopItem spaceship) {
		System.out.println("Game: applySpaceshipSkin 호출 - " + spaceship.getName());
		System.out.println("Game: spaceship.getIconPath() = " + spaceship.getIconPath());
		// ShopItem의 getIconPath()를 사용하여 스킨 파일 경로 가져오기
		String newSkinPath = spaceship.getIconPath();
		
		// 스킨이 변경된 경우에만 업데이트
		if (!newSkinPath.equals(currentSpaceshipSkin)) {
			currentSpaceshipSkin = newSkinPath;
			updateShipSkin();
			System.out.println("Game: 우주선 스킨 변경 - " + currentSpaceshipSkin);
		}
	}
	
	/**
	 * 우주선 스킨 업데이트
	 */
	private void updateShipSkin() {
		System.out.println("Game: updateShipSkin 호출 - " + currentSpaceshipSkin);
		System.out.println("Game: ship = " + (ship != null ? "존재" : "null"));
		if (ship != null) {
			ship.changeSkin(currentSpaceshipSkin);
		}
	}
	
	/**
	 * 무기 스킨 적용
	 */
	private void applyWeaponSkin(ShopItem weapon) {
		// ShopItem의 getIconPath()를 사용하여 스킨 파일 경로 가져오기
		currentWeaponSkin = weapon.getIconPath();
		
		System.out.println("Game: 무기 스킨 변경 - " + currentWeaponSkin);
	}
	
	/**
	 * 무기 효과 적용
	 */
	private void applyWeaponEffects(ShopItem weapon) {
		// 무기별 효과 적용
		String weaponName = weapon.getName();
		if (weaponName.contains("강화")) {
			gameStateManager.setAttackPower(gameStateManager.getAttackPower() + 1);
			System.out.println("Game: 공격력 +1 증가");
		}
		if (weaponName.contains("빠른")) {
			gameStateManager.setAttackSpeed(gameStateManager.getAttackSpeed() * 1.2);
			System.out.println("Game: 공격속도 20% 증가");
		}
	}
	
	/**
	 * 파워업 효과 적용 (일단 쓰지마셈)
	 */
	private void applyPowerupEffects(ShopItem powerup) {
		// 파워업별 효과 적용
		String powerupName = powerup.getName();
		if (powerupName.contains("체력")) {
			gameStateManager.setMaxHP(gameStateManager.getMaxHP() + 1);
			gameStateManager.setCurrentHP(gameStateManager.getCurrentHP() + 1);
			System.out.println("Game: 최대 체력 +1 증가");
		}
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
			case 1: rows = 3; cols = 6; break;  // 18 aliens
			case 2: rows = 3; cols = 7; break;  // 21 aliens
			case 3: rows = 4; cols = 7; break;  // 28 aliens
			case 4: rows = 4; cols = 8; break;  // 32 aliens
			case 5: rows = 5; cols = 8; break;  // 40 aliens
			default: rows = 3; cols = 6; break;
		}
		
		for (int row=0; row<rows; row++) {
			for (int x=0; x<cols; x++) {
				Entity alien = new AlienEntity(this, 120+(x*70), (60)+row*35);
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
			// 게임 오버 후 메뉴로 돌아가기
			if (navigator != null) navigator.showMainMenu();
		}
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		boolean roundAdvanced = gameStateManager.advanceRound();
		
		if (roundAdvanced) {
			// Clear current entities and initialize next round
			gameStateManager.getEntities().clear();
			initEntities();
		} else {
			// Game completed
			gameStateManager.setMessage("Well done! You Win!");
			gameStateManager.setWaitingForKeyPress(true);
			// 게임 승리 후 메뉴로 돌아가기
			if (navigator != null) navigator.showMainMenu();
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
		ShotEntity shot = new ShotEntity(this, "sprites/alien2.gif", x, y, true);
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
		
		ShotEntity shot = new ShotEntity(this, "sprites/alien2.gif", x + aimOffset, y, true);
		gameStateManager.getEntities().add(shot);
	}
	
	/**
	 * Try to fire shots from aliens (only those close to player)
	 */
	private void tryAlienFire() {
		// check that we have waited long enough to fire
		if (System.currentTimeMillis() - gameStateManager.getLastAlienFire() < gameStateManager.getAlienFiringInterval()) {
			return;
		}
		
		// find aliens that are close enough to the player to fire
		ArrayList<AlienEntity> aliens = new ArrayList<>();
		ArrayList<Entity> entities = gameStateManager.getEntities();
		
		for (Entity entity : entities) {
			if (entity instanceof AlienEntity) {
				// Only aliens that are close to the player can fire (within 200 pixels vertically)
				if (Math.abs(entity.getY() - ship.getY()) < 200) {
					aliens.add((AlienEntity) entity);
				}
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

	// 메인메뉴로 이동 (InputManager가 호출)
	void goToMainMenu() {
		if (navigator != null) navigator.showMainMenu();
	}
	
	/**
	 * 게임플레이 배경 그리기
	 */
    // 배경 렌더링은 BackgroundRenderer가 담당
}