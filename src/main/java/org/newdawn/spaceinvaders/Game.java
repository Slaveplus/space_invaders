package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;
import org.newdawn.spaceinvaders.login.LoginScreen;

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
public class Game extends Canvas 
{
	/** The stragey that allows us to use accelerate page flipping */
	private BufferStrategy strategy;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
	/** The list of all the entities that exist in our game */
	private ArrayList entities = new ArrayList();
	/** The list of entities that need to be removed from the game this loop */
	private ArrayList removeList = new ArrayList();
	/** The entity representing the player */
	private Entity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** The time at which last fired a shot */
	private long lastFire = 0;
	/** The interval between our players shot (ms) */
	private long firingInterval = 500;
	/** The number of aliens left on the screen */
	private int alienCount;
	
	/** The message to display which waiting for a key press */
	private String message = "";
	/** True if we're holding up game play until a key has been pressed */
	private boolean waitingForKeyPress = true;
	/** True if the left cursor key is currently pressed */
	private boolean leftPressed = false;
	/** True if the right cursor key is currently pressed */
	private boolean rightPressed = false;
	/** True if we are firing */
	private boolean firePressed = false;
	/** True if game logic needs to be applied this loop, normally as a result of a game event */
	private boolean logicRequiredThisLoop = false;
	/** The last time at which we recorded the frame rate */
	private long lastFpsTime;
	/** The current number of frames recorded */
	private int fps;
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders 102";
	/** The game window that we'll update with the frame count */
	private JFrame container;
	
	/** The login screen system */
	private LoginScreen loginScreen;
	/** True if we're currently showing the login screen */
	private boolean showingLogin = true;
	/** The main menu system */
	private MainMenu mainMenu;
	/** True if we're currently showing the main menu */
	private boolean showingMenu = false;
	
	/**
	 * Construct our game and set it running.
	 */
	public Game() {
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
		
		// add a key input system (defined below) to our canvas
		// so we can respond to key pressed
		addKeyListener(new KeyInputHandler());
		
		// add a mouse input system to our canvas
		addMouseListener(new MouseInputHandler());
		
		// request the focus so key events come to us
		requestFocus();

		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		
		// initialize the login screen
		loginScreen = new LoginScreen();
		
		// initialize the main menu
		mainMenu = new MainMenu();
		
		// MainMenu의 UserManager를 LoginScreen의 UserManager와 동기화
		mainMenu.getUserManager().setCurrentUser(loginScreen.getUserManager().getCurrentUser());
		
		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
		// clear out any existing entities and intialise a new set
		entities.clear();
		initEntities();
		
		// blank out any keyboard settings we might currently have
		leftPressed = false;
		rightPressed = false;
		firePressed = false;
	}
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// create the player ship and place it roughly in the center of the screen
		ship = new ShipEntity(this,"sprites/ship.gif",370,550);
		entities.add(ship);
		
		// create a block of aliens (5 rows, by 12 aliens, spaced evenly)
		alienCount = 0;
		for (int row=0;row<5;row++) {
			for (int x=0;x<12;x++) {
				Entity alien = new AlienEntity(this,100+(x*50),(50)+row*30);
				entities.add(alien);
				alienCount++;
			}
		}
	}
	
	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() {
		logicRequiredThisLoop = true;
	}
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 * 
	 * @param entity The entity that should be removed
	 */
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}
	
	/**
	 * Notification that the player has died. 
	 */
	public void notifyDeath() {
		message = "Oh no! They got you, try again?";
		waitingForKeyPress = true;
		// 게임 오버 후 메뉴로 돌아가기
		showingMenu = true;
		mainMenu.reset();
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		message = "Well done! You Win!";
		waitingForKeyPress = true;
		// 게임 승리 후 메뉴로 돌아가기
		showingMenu = true;
		mainMenu.reset();
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		// reduce the alient count, if there are none left, the player has won!
		alienCount--;
		
		if (alienCount == 0) {
			notifyWin();
		}
		
		// if there are still some aliens left then they all need to get faster, so
		// speed up all the existing aliens
		for (int i=0;i<entities.size();i++) {
			Entity entity = (Entity) entities.get(i);
			
			if (entity instanceof AlienEntity) {
				// speed up by 2%
				entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
			}
		}
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		lastFire = System.currentTimeMillis();
		ShotEntity shot = new ShotEntity(this,"sprites/shot.gif",ship.getX()+10,ship.getY()-30);
		entities.add(shot);
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
		while (gameRunning) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			// update the frame counter
			lastFpsTime += delta;
			fps++;
			
			// update our FPS counter if a second has passed since
			// we last recorded
			if (lastFpsTime >= 1000) {
				container.setTitle(windowTitle+" (FPS: "+fps+")");
				lastFpsTime = 0;
				fps = 0;
			}
			
			// Get hold of a graphics context for the accelerated 
			// surface and blank it out
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			
		// 로그인 화면이 표시 중일 때는 로그인 화면만 그리기
		if (showingLogin) {
			loginScreen.update();
			loginScreen.draw(g);
		} else if (showingMenu) {
			// 메뉴가 표시 중일 때는 메뉴만 그리기
			mainMenu.draw(g);
		} else {
				g.setColor(Color.black);
				g.fillRect(0,0,800,600);
				
				// cycle round asking each entity to move itself
				if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}
			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<entities.size();i++) {
				Entity entity = (Entity) entities.get(i);
				
				entity.draw(g);
			}
			
			// brute force collisions, compare every entity against
			// every other entity. If any of them collide notify 
			// both entities that the collision has occured
			for (int p=0;p<entities.size();p++) {
				for (int s=p+1;s<entities.size();s++) {
					Entity me = (Entity) entities.get(p);
					Entity him = (Entity) entities.get(s);
					
					if (me.collidesWith(him)) {
						me.collidedWith(him);
						him.collidedWith(me);
					}
				}
			}
			
			// remove any entity that has been marked for clear up
			entities.removeAll(removeList);
			removeList.clear();

			// if a game event has indicated that game logic should
			// be resolved, cycle round every entity requesting that
			// their personal logic should be considered.
			if (logicRequiredThisLoop) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					entity.doLogic();
				}
				
				logicRequiredThisLoop = false;
			}
			
				// if we're waiting for an "any key" press then draw the 
				// current message 
				if (waitingForKeyPress) {
					g.setColor(Color.white);
					g.drawString(message,(800-g.getFontMetrics().stringWidth(message))/2,250);
					g.drawString("Press any key",(800-g.getFontMetrics().stringWidth("Press any key"))/2,300);
				}
				
				// resolve the movement of the ship. First assume the ship 
				// isn't moving. If either cursor key is pressed then
				// update the movement appropraitely
				if (ship != null) {
					ship.setHorizontalMovement(0);
					
					if ((leftPressed) && (!rightPressed)) {
						ship.setHorizontalMovement(-moveSpeed);
					} else if ((rightPressed) && (!leftPressed)) {
						ship.setHorizontalMovement(moveSpeed);
					}
					
					// if we're pressing fire, attempt to fire
					if (firePressed) {
						tryToFire();
					}
				}
			}
			
			// finally, we've completed drawing so clear up the graphics
			// and flip the buffer over
			g.dispose();
			strategy.show();
			
			// we want each frame to take 10 milliseconds, to do this
			// we've recorded when we started the frame. We add 10 milliseconds
			// to this and then factor in the current time to give 
			// us our final value to wait for
			SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
		}
	}
	
	/**
	 * A class to handle keyboard input from the user. The class
	 * handles both dynamic input during game play, i.e. left/right 
	 * and shoot, and more static type input (i.e. press any key to
	 * continue)
	 * 
	 * This has been implemented as an inner class more through 
	 * habbit then anything else. Its perfectly normal to implement
	 * this as seperate class if slight less convienient.
	 * 
	 * @author Kevin Glass
	 */
	private class KeyInputHandler extends KeyAdapter {
		/** The number of key presses we've had while waiting for an "any key" press */
		private int pressCount = 1;
		
		/**
		 * Notification from AWT that a key has been pressed. Note that
		 * a key being pressed is equal to being pushed down but *NOT*
		 * released. Thats where keyTyped() comes in.
		 *
		 * @param e The details of the key that was pressed 
		 */
		public void keyPressed(KeyEvent e) {
			// 로그인 화면이 표시 중일 때는 로그인 화면에서 키 입력 처리
			if (showingLogin) {
				loginScreen.handleKeyInput(e.getKeyCode(), e.getKeyChar());
				// 로그인 성공 시 메인 메뉴로 이동
				if (loginScreen.getUserManager().isLoggedIn()) {
					// MainMenu의 UserManager를 LoginScreen의 UserManager로 완전히 교체
					mainMenu.setUserManager(loginScreen.getUserManager());
					showingLogin = false;
					showingMenu = true;
					System.out.println("로그인 성공, 메인 메뉴로 이동");
				}
				return;
			}
			
			// 메뉴가 표시 중일 때는 메뉴에서 키 입력 처리
			if (showingMenu) {
				mainMenu.handleKeyInput(e.getKeyCode());
				// 새게임 시작 요청이 있으면 메뉴 숨기고 게임 시작
				if (mainMenu.shouldStartGame()) {
					showingMenu = false;
					waitingForKeyPress = false;
					mainMenu.reset(); // 게임 시작 요청 플래그 리셋
					startGame();
				}
				// 로그아웃 요청이 있으면 로그인 화면으로 돌아가기
				if (mainMenu.isLogoutRequested()) {
					// LoginScreen의 UserManager도 로그아웃 처리
					loginScreen.getUserManager().logoutUser();
					// LoginScreen 필드 초기화
					loginScreen.reset();
					// 게임 상태 초기화
					entities.clear();
					waitingForKeyPress = false;
					// 메뉴 상태 완전 초기화
					mainMenu.reset();
					showingMenu = false;
					showingLogin = true;
					System.out.println("로그아웃 요청, 로그인 화면으로 이동 - 모든 상태 초기화");
				}
				return;
			}
			
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "press"
			if (waitingForKeyPress) {
				return;
			}
			
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = true;
			}

			// 상점 키 입력 처리
			if (showingMenu){
				mainMenu.handleKeyInput(e.getKeyCode());
                // 상점이 표시 중이면 상점에서 처리함
				if (mainMenu.isShowingShop()){
					return;
				}
                // 새게임 시작 요청이 있으면 메뉴를 숨기고 게임 시작
				if (mainMenu.shouldStartGame()){
					showingMenu = false;
					waitingForKeyPress = false;
					mainMenu.reset();
					startGame();
				}
				return;
			}
		} 
		
		/**
		 * Notification from AWT that a key has been released.
		 *
		 * @param e The details of the key that was released 
		 */
		public void keyReleased(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "released"
			if (waitingForKeyPress) {
				return;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = false;
			}
		}

		/**
		 * Notification from AWT that a key has been typed. Note that
		 * typing a key means to both press and then release it.
		 *
		 * @param e The details of the key that was typed. 
		 */
		public void keyTyped(KeyEvent e) {
			// if we're waiting for a "any key" type then
			// check if we've recieved any recently. We may
			// have had a keyType() event from the user releasing
			// the shoot or move keys, hence the use of the "pressCount"
			// counter.
			if (waitingForKeyPress) {
				if (pressCount == 1) {
					// since we've now recieved our key typed
					// event we can mark it as such and start 
					// our new game
					waitingForKeyPress = false;
					startGame();
					pressCount = 0;
				} else {
					pressCount++;
				}
			}
			
			// ESC 키로 게임 종료 기능 제거 - 이제 창 닫기 버튼으로만 종료 가능
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
	
	/**
	 * 마우스 입력 처리 클래스
	 * 게임 내 모든 마우스 이벤트를 처리합니다
	 */
	private class MouseInputHandler extends MouseAdapter {
		/**
		 * 마우스 클릭 이벤트 처리
		 */
		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			
			// 로그인 화면이 표시 중일 때는 로그인 화면에서 마우스 클릭 처리
			if (showingLogin) {
				loginScreen.handleMouseClick(x, y);
				// 로그인 성공 시 메인 메뉴로 이동
				if (loginScreen.getUserManager().isLoggedIn()) {
					// MainMenu의 UserManager를 LoginScreen의 UserManager로 완전히 교체
					mainMenu.setUserManager(loginScreen.getUserManager());
					showingLogin = false;
					showingMenu = true;
					System.out.println("로그인 성공, 메인 메뉴로 이동");
				}
				return;
			}
			
			// 메뉴가 표시 중일 때는 메뉴에서 마우스 클릭 처리
			if (showingMenu) {
				mainMenu.handleMouseClick(x, y);
				// 새게임 시작 요청이 있으면 메뉴 숨기고 게임 시작
				if (mainMenu.shouldStartGame()) {
					showingMenu = false;
					waitingForKeyPress = false;
					mainMenu.reset(); // 게임 시작 요청 플래그 리셋
					startGame();
				}
				// 로그아웃 요청이 있으면 로그인 화면으로 돌아가기
				if (mainMenu.isLogoutRequested()) {
					// LoginScreen의 UserManager도 로그아웃 처리
					loginScreen.getUserManager().logoutUser();
					// LoginScreen 필드 초기화
					loginScreen.reset();
					// 게임 상태 초기화
					entities.clear();
					waitingForKeyPress = false;
					// 메뉴 상태 완전 초기화
					mainMenu.reset();
					showingMenu = false;
					showingLogin = true;
					System.out.println("로그아웃 요청, 로그인 화면으로 이동 - 모든 상태 초기화");
				}
				return;
			}
			
			// 게임 중일 때는 게임 내 마우스 클릭 처리
			// (필요시 게임 내 마우스 기능 추가 가능)
			System.out.println("게임 중 마우스 클릭: (" + x + ", " + y + ")");
		}
	}
}
