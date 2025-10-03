package org.newdawn.spaceinvaders.gameplay;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.gameplay.render.CommonRenderer;
import org.newdawn.spaceinvaders.multiplay.MultiplayerClient;
import org.newdawn.spaceinvaders.net.Snapshot;
import org.newdawn.spaceinvaders.net.Snapshot.PlayerState;

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
public class Game extends Canvas implements Screen {
	// 네트워크 전용 화면: 상위 App이 BufferStrategy 관리
	private final ScreenNavigator navigator;
	private final CommonRenderer commonRenderer = new CommonRenderer();
	private final UIRenderer uiRenderer = new UIRenderer(this);

	private MultiplayerClient client; // 네트워크 클라이언트(로컬 임베디드/원격 공용)
	private volatile String statusText = ""; // 상태 텍스트

	// 입력 상태 (동시 키 처리)
	private boolean leftDown = false;
	private boolean rightDown = false;
	private int dx = 0; // -1,0,1
	// 스킬 오버레이 상태
	private boolean showingSkillMenuMulti = false;
	private int selectedSkillMulti = 0; // 0: 공격력, 1: 공속, 2: HP
	
	/** 외부 MultiplayerClient를 받아 화면을 구성 */
	public Game(ScreenNavigator navigator, MultiplayerClient externalClient, boolean isNetwork) {
		this.navigator = navigator;
		this.client = externalClient;
		setIgnoreRepaint(true);
		setBounds(0,0,800,600);
		setFocusable(true);

		// 입력 리스너 등록
		addKeyListener(keyAdapter);
	}

	// 동시 키 입력 처리 -> 서버로 전송
	private void recomputeDxAndSend() {
		int ndx;
		if (leftDown && !rightDown) ndx = -1;
		else if (rightDown && !leftDown) ndx = 1;
		else ndx = 0;
		if (ndx != dx) { dx = ndx; if (client != null) client.inputDx(dx); }
	}

	private final KeyAdapter keyAdapter = new KeyAdapter() {
		@Override public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				// 간단 일시정지 오버레이 토글은 UI에서 처리 가능 (필요 시 확장)
				return;
			}
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					if (client != null && showingSkillMenuMulti) { selectedSkillMulti = (selectedSkillMulti + 2) % 3; break; }
					leftDown = true; recomputeDxAndSend(); if (client != null) client.keyDownLeft(); break;
				case KeyEvent.VK_RIGHT:
					if (client != null && showingSkillMenuMulti) { selectedSkillMulti = (selectedSkillMulti + 1) % 3; break; }
					rightDown = true; recomputeDxAndSend(); if (client != null) client.keyDownRight(); break;
				case KeyEvent.VK_SPACE:
					if (client != null) {
						if (showingSkillMenuMulti) { client.upgrade(selectedSkillMulti); }
						else { client.inputShoot(); client.keyDownFire(); }
					}
					break;
				case KeyEvent.VK_ENTER:
					if (client != null && showingSkillMenuMulti) { client.upgrade(selectedSkillMulti); }
					break;
				case KeyEvent.VK_Q:
					if (client != null) { showingSkillMenuMulti = !showingSkillMenuMulti; }
					break;
			}
		}
		@Override public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT: leftDown = false; recomputeDxAndSend(); if (client != null) client.keyUpLeft(); break;
				case KeyEvent.VK_RIGHT: rightDown = false; recomputeDxAndSend(); if (client != null) client.keyUpRight(); break;
				case KeyEvent.VK_SPACE: if (client != null) client.keyUpFire(); break;
			}
		}
	};

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
	public void startGame() { leftDown = rightDown = false; dx = 0; if (client != null) client.inputDx(0); statusText = ""; }
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	// 로컬 시뮬레이션 제거: 엔티티 초기화 불필요
	
	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() { /* no-op in network mode */ }
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 * 
	 * @param entity The entity that should be removed
	 */
	// 로컬 시뮬 제거: 엔티티 제거 불필요
	
	/**
	 * Notification that the player has died. 
	 */
	public void notifyDeath() { /* handled by server in network mode */ }
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() { /* handled by server in network mode */ }
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() { /* handled by server in network mode */ }
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() { if (client != null) client.inputShoot(); }
	
	/**
	 * Add an alien shot to the game
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 */
	public void addAlienShot(int x, int y) { /* server authoritative */ }
	
	/**
	 * Add an alien shot with slight aim adjustment towards player
	 * 
	 * @param x The x location of the shot
	 * @param y The y location of the shot
	 * @param alienX The x location of the alien firing
	 */
	public void addAimedAlienShot(int x, int y, int alienX) { /* server authoritative */ }
	
	// 에일리언 사격 로직은 AlienFiringSystem으로 이동
	
	/**
	 * Get the player's current attack power
	 * 
	 * @return The player's attack power
	 */
	public int getPlayerAttackPower() { return 0; }
	
	/**
	 * Get the current round number
	 * 
	 * @return The current round
	 */
	public int getCurrentRound() { return 1; }
	
	/**
	 * Check if player is currently invincible
	 */
	public boolean isPlayerInvincible() { return false; }
	
	/**
	 * Check if player has piercing shots
	 */
	public boolean hasPiercingShots() { return false; }
	
	/**
	 * Check if player has triple shot
	 */
	public boolean hasTripleShot() { return false; }
	
	/**
	 * Add skill to inventory
	 */
	public void addSkillToInventory(int skillType, int skillValue) { /* client-side inventory removed */ }
	
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
	public void update(long delta) { /* 스냅샷 기반: 입력은 키 이벤트에서 처리 */ }

	public void render(Graphics2D g) {
		Snapshot snap = client != null ? client.getLatestSnapshot() : null;
		commonRenderer.renderSnapshot(g, snap, () -> {
			// HUD: 라운드/HP/스킬 포인트/스탯
			g.setColor(java.awt.Color.LIGHT_GRAY);
			int wave = (snap != null ? snap.wave : 0);
			g.drawString("라운드: " + wave, 20, 25);

			PlayerState me = null;
			String myId = (client != null ? client.getPlayerId() : null);
			if (snap != null && myId != null && snap.players != null) {
				for (PlayerState ps : snap.players) if (myId.equals(ps.id)) { me = ps; break; }
			}
			if (me != null) {
				// HP 텍스트
				g.setColor(java.awt.Color.WHITE);
				g.drawString("HP: " + me.hp + "/" + me.maxHp, 20, 50);
				// HP 바(간단 구현)
				int barX = 20, barY = 60, barW = 200, barH = 20;
				g.setColor(new java.awt.Color(50,50,50)); g.fillRect(barX, barY, barW, barH);
				int hpw = (int)(Math.max(0, Math.min(1.0, me.maxHp > 0 ? (double)me.hp/me.maxHp : 0)) * barW);
				g.setColor(new java.awt.Color(255,0,0)); g.fillRect(barX, barY, hpw, barH);
				g.setColor(java.awt.Color.WHITE); g.drawRect(barX, barY, barW, barH);

				// 스킬 포인트 및 스탯
				g.setColor(java.awt.Color.YELLOW); g.drawString("스킬 포인트: " + me.skillPoints, 20, 100);
				g.setColor(java.awt.Color.WHITE); g.drawString("공격력: " + me.attackPower, 20, 120);
				g.drawString("공격속도: " + String.format("%.1f", me.attackSpeed) + "x", 20, 135);
				g.setColor(java.awt.Color.CYAN); g.drawString("Q: 스킬 메뉴", 20, 155);
			}
			if (statusText != null && !statusText.isEmpty()) g.drawString(statusText, 10, 180);

			if (showingSkillMenuMulti) {
				int sp = me != null ? me.skillPoints : 0;
				int atk = me != null ? me.attackPower : 1;
				double aspd = me != null ? me.attackSpeed : 1.0;
				int hpmax = me != null ? me.maxHp : 3;
				int cAtk = me != null ? me.costAtk : 2;
				int cAspd = me != null ? me.costAspd : 2;
				int cHp = me != null ? me.costHp : 8;
				uiRenderer.drawSkillOverlay(g, sp, atk, aspd, hpmax, cAtk, cAspd, cHp, selectedSkillMulti);
			}
		});
	}
    
	// 간단 도우미: 네비게이션 사용 가능하게 유지
	void goToMainMenu() { if (navigator != null) navigator.showMainMenu(); }

	// ===== Stubs for legacy classes (compile-only, not used in network mode) =====
	public org.newdawn.spaceinvaders.gameplay.entity.Entity getShip() { return null; }
	public double getMoveSpeed() { return org.newdawn.spaceinvaders.gameplay.core.Rules.PLAYER_MOVE_SPEED; }
	public void removeEntity(org.newdawn.spaceinvaders.gameplay.entity.Entity entity) { /* no-op */ }
	public java.util.ArrayList<org.newdawn.spaceinvaders.gameplay.entity.Entity> getEntities() { return new java.util.ArrayList<>(); }
	public SkillManager getSkillManager() { return null; }
}
