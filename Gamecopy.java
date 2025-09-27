package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;

/**
 * The main hook of our game. This class acts as a manager
 * for the display and central mediator for the game logic. 
 * 
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place.
 * 
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alien killed, player died) and will take
 * appropriate game actions.
 * 
 * @author Kevin Glass
 */
public class Game extends Canvas 
{
	/** The strategy that allows us to use accelerate page flipping */
	private BufferStrategy strategy;
	/** The game window that we'll update with the frame count */
	private JFrame container;
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders 102";
	
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** The entity representing the player */
	private Entity ship;
	
	// Manager classes
	private GameState gameState;
	private MenuManager menuManager;
	private SkillManager skillManager;
	private UIRenderer uiRenderer;
	private InputHandler inputHandler;
	
	/**
	 * Construct our game and set it running.
	 */
	public Game() {
		// Initialize manager classes
		gameState = new GameState();
		menuManager = new MenuManager();
		skillManager = new SkillManager(this);
		uiRenderer = new UIRenderer(this);
		inputHandler = new InputHandler(this, menuManager, skillManager, gameState);
		
		// create a frame to contain our game
		container = new JFrame("Space Invaders 102");
		
		// get hold the content of the frame and set up the resolution of the game
		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(800,600));
		panel.setLayout(null);
		
		// setup our canvas size and put it into the content of the frame
		setBounds(0,0,800,600);
		panel.add(this);
		
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);
		
		// finally make the window visible 
		container.pack();
		container.setResizable(false);
		container.setVisible(true);
		
		// add a listener to respond to the user closing the window. If they
		// do we'd like to exit the game
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		// add a key input system to our canvas
		addKeyListener(inputHandler);
		
		// request the focus so key events come to us
		requestFocus();

		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		
		// initialise the entities in our game so there's something
		// to see at startup (only when not showing menu)
		if (!menuManager.isShowMenu()) {
			initEntities();
		}
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	public void startNewGame() {
		// Reset game state
		gameState.startNewGame();
		skillManager.reset();
		
		// clear out any existing entities and initialise a new set
		gameState.getEntities().clear();
		initEntities();
	}
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entity will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// create the player ship and place it roughly in the center of the screen
		ship = new ShipEntity(this,"sprites/ship.gif",370,550);
		gameState.getEntities().add(ship);
		
		// Create aliens based on current round with balanced progression
		int alienCount = 0;
		int rows, cols;
		
		switch (gameState.getCurrentRound()) {
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
				gameState.getEntities().add(alien);
				alienCount++;
			}
		}
		
		gameState.setAlienCount(alienCount);
	}
	
	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() {
		gameState.setLogicRequiredThisLoop(true);
	}
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 * 
	 * @param entity The entity that should be removed
	 */
	public void removeEntity(Entity entity) {
		gameState.getRemoveList().add(entity);
	}
	
	/**
	 * Notification that the player has died. 
	 */
	public void notifyDeath() {
		// Check if player is invincible
		if (skillManager.isInvincible()) {
			return; // No damage taken when invincible
		}
		
		gameState.takeDamage();
		if (gameState.getCurrentHP() <= 0) {
			// Return to main menu after game over
			menuManager.setShowMenu(true);
		}
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		boolean roundAdvanced = gameState.advanceRound();
		
		if (roundAdvanced) {
			// Clear current entities and initialize next round
			gameState.getEntities().clear();
			initEntities();
		} else {
			// Game completed
			menuManager.setShowMenu(true);
		}
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		// Give random skill points for killing aliens
		int earnedPoints = skillManager.getRandomSkillPoints(gameState.getCurrentRound());
		gameState.addSkillPoints(earnedPoints);
		
		// Random chance to drop a skill
		double dropChance = skillManager.getSkillDropChance(gameState.getCurrentRound());
		if (Math.random() < dropChance) {
			skillManager.dropSkill(gameState.getCurrentRound());
		}
		
		// Count remaining aliens dynamically (excluding those marked for removal)
		int remainingAliens = 0;
		ArrayList<Entity> entities = gameState.getEntities();
		ArrayList<Entity> removeList = gameState.getRemoveList();
		
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
				double speedMultiplier = 1.015 + (gameState.getCurrentRound() * 0.01);
				entity.setHorizontalMovement(entity.getHorizontalMovement() * speedMultiplier);
				entity.setVerticalMovement(entity.getVerticalMovement() * speedMultiplier);
			}
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
		gameState.getEntities().add(shot);
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
		gameState.getEntities().add(shot);
	}
	
	/**
	 * Try to fire shots from aliens (only those close to player)
	 */
	private void tryAlienFire() {
		// check that we have waited long enough to fire
		if (System.currentTimeMillis() - gameState.getLastAlienFire() < gameState.getAlienFiringInterval()) {
			return;
		}
		
		// find aliens that are close enough to the player to fire
		ArrayList<AlienEntity> aliens = new ArrayList<>();
		ArrayList<Entity> entities = gameState.getEntities();
		
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
			gameState.setLastAlienFire(System.currentTimeMillis());
		}
	}
	
	/**
	 * Get the player's current attack power
	 * 
	 * @return The player's attack power
	 */
	public int getPlayerAttackPower() {
		return gameState.getAttackPower();
	}
	
	/**
	 * Get the current round number
	 * 
	 * @return The current round
	 */
	public int getCurrentRound() {
		return gameState.getCurrentRound();
	}
	
	/**
	 * Add a skill to inventory instead of immediately activating
	 */
	public void addSkillToInventory(int skillType, int skillValue) {
		skillManager.addSkillToInventory(skillType, skillValue);
	}
	
	/**
	 * Activate a skill effect from inventory
	 */
	public void activateSkill(int skillType, int skillValue) {
		skillManager.activateSkill(skillType, skillValue);
	}
	
	/**
	 * Extend or refresh a skill effect (can be used while skill is already active)
	 */
	public void extendSkill(int skillType, int skillValue) {
		skillManager.extendSkill(skillType, skillValue);
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
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// Calculate firing interval based on attack speed skill
		long currentFiringInterval = (long) (gameState.getFiringInterval() / gameState.getAttackSpeed());
		
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - gameState.getLastFire() < currentFiringInterval) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		gameState.setLastFire(System.currentTimeMillis());
		
		if (skillManager.hasTripleShot()) {
			// Fire three shots in a wider spread pattern
			ShotEntity shot1 = new ShotEntity(this,"sprites/shot.gif",ship.getX()-5,ship.getY()-30);
			ShotEntity shot2 = new ShotEntity(this,"sprites/shot.gif",ship.getX()+10,ship.getY()-30);
			ShotEntity shot3 = new ShotEntity(this,"sprites/shot.gif",ship.getX()+25,ship.getY()-30);
			gameState.getEntities().add(shot1);
			gameState.getEntities().add(shot2);
			gameState.getEntities().add(shot3);
		} else {
			// Fire single shot
			ShotEntity shot = new ShotEntity(this,"sprites/shot.gif",ship.getX()+10,ship.getY()-30);
			gameState.getEntities().add(shot);
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
	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();
		
		// keep looping round til the game ends
		while (gameState.isGameRunning()) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			// update the frame counter
			gameState.setLastFpsTime(gameState.getLastFpsTime() + delta);
			gameState.setFps(gameState.getFps() + 1);
			
			// update our FPS counter if a second has passed since
			// we last recorded
			if (gameState.getLastFpsTime() >= 1000) {
				container.setTitle(windowTitle+" (FPS: "+gameState.getFps()+")");
				gameState.setLastFpsTime(0);
				gameState.setFps(0);
			}
			
			// Get hold of a graphics context for the accelerated 
			// surface and blank it out
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(new Color(10, 10, 30)); // Dark blue space background
			g.fillRect(0,0,800,600);
			
			if (menuManager.isShowMenu()) {
				// Draw menu screen
				menuManager.update();
				menuManager.drawMainMenu(g);
			} else if (menuManager.isShowPauseMenu()) {
				// Draw pause menu
				menuManager.drawPauseMenu(g);
			} else if (menuManager.isShowSkillMenu()) {
				// Draw skill menu
				menuManager.drawSkillMenu(g, gameState.getSkillPoints(), gameState.getAttackPower(), 
					gameState.getAttackSpeed(), gameState.getMaxHP(), 
					skillManager.getAttackPowerCost(), skillManager.getAttackSpeedCost(), skillManager.getHpUpCost());
			} else {
				// cycle round asking each entity to move itself
				if (!gameState.isWaitingForKeyPress()) {
					// Update skill effects
					skillManager.updateSkillEffects();
					
					ArrayList<Entity> entities = gameState.getEntities();
					for (Entity entity : entities) {
						entity.move(delta);
					}
					
					// try to fire from aliens
					tryAlienFire();
				}
				
				// cycle round drawing all the visible entities we have in the game
				ArrayList<Entity> entities = gameState.getEntities();
				for (Entity entity : entities) {
					entity.draw(g);
				}
				
				// Simple collision detection
				for (int i=0;i<entities.size();i++) {
					Entity entity1 = entities.get(i);
					for (int j=i+1;j<entities.size();j++) {
						Entity entity2 = entities.get(j);
						if (entity1.collidesWith(entity2)) {
							entity1.collidedWith(entity2);
							entity2.collidedWith(entity1);
						}
					}
				}
				
				// remove any entity that has been marked for clear up
				entities.removeAll(gameState.getRemoveList());
				gameState.getRemoveList().clear();

				// if a game event has indicated that game logic should
				// be resolved, cycle round every entity requesting that
				// their personal logic should be considered.
				if (gameState.isLogicRequiredThisLoop()) {
					for (Entity entity : entities) {
						entity.doLogic();
					}
					
					gameState.setLogicRequiredThisLoop(false);
				}
				
				// Draw game UI (HP, skill points, etc.)
				uiRenderer.drawGameUI(g, gameState, skillManager);
				
				// if we're waiting for an "any key" press then draw the 
				// current message 
				if (gameState.isWaitingForKeyPress()) {
					uiRenderer.drawMessage(g, gameState.getMessage());
				}
			}
			
			// finally, we've completed drawing so clear up the graphics
			// and flip the buffer over
			g.dispose();
			strategy.show();
			
			// resolve the movement of the ship. First assume the ship 
			// isn't moving. If either cursor key is pressed then
			// update the movement appropriately
			if (ship != null) {
				ship.setHorizontalMovement(0);
				
				if (inputHandler.isLeftPressed() && !inputHandler.isRightPressed()) {
					ship.setHorizontalMovement(-moveSpeed);
				} else if (inputHandler.isRightPressed() && !inputHandler.isLeftPressed()) {
					ship.setHorizontalMovement(moveSpeed);
				}
				
				// if we're pressing fire, attempt to fire
				if (inputHandler.isFirePressed()) {
					tryToFire();
				}
			}
			
			// we want each frame to take 10 milliseconds, to do this
			// we've recorded when we started the frame. We add 10 milliseconds
			// to this and then factor in the current time to give 
			// us our final value to wait for
			SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
		}
	}
	
	// Getters for manager classes
	public GameState getGameState() { return gameState; }
	public MenuManager getMenuManager() { return menuManager; }
	public SkillManager getSkillManager() { return skillManager; }
	public UIRenderer getUIRenderer() { return uiRenderer; }
	public InputHandler getInputHandler() { return inputHandler; }
	
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