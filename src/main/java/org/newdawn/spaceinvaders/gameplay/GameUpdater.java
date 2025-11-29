package org.newdawn.spaceinvaders.gameplay;

import java.util.ArrayList;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEntity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.gameplay.net.GameNetworkAdapter;

/**
 * 게임 업데이트 로직을 담당하는 클래스
 * Game.java의 업데이트 책임을 분리
 */
public class GameUpdater {
    private final Game game;
    private final GameStateManager gameStateManager;
    private final SkillManager skillManager;
    private final InputManager inputManager;
    private final GameNetworkAdapter networkAdapter;
    private ShipEntity ship;
    private final double moveSpeed;
    
    private boolean wasPaused = false;
    
    public GameUpdater(Game game, GameStateManager gameStateManager, 
                      SkillManager skillManager, InputManager inputManager,
                      GameNetworkAdapter networkAdapter, ShipEntity ship, double moveSpeed) {
        this.game = game;
        this.gameStateManager = gameStateManager;
        this.skillManager = skillManager;
        this.inputManager = inputManager;
        this.networkAdapter = networkAdapter;
        this.ship = ship;
        this.moveSpeed = moveSpeed;
    }
    
    /**
     * ShipEntity 업데이트 (ship이 변경될 수 있으므로)
     */
    public void updateShipReference(ShipEntity newShip) {
        this.ship = newShip;
    }
    
    /**
     * 게임 업데이트 메인 로직
     */
    public void update(long delta) {
        // 1) 네트워크 틱
        if (networkAdapter != null) {
            networkAdapter.tick(System.currentTimeMillis());
        }

        // 라운드 정보 창 자동 닫기
        if (gameStateManager.shouldAutoCloseRoundInfo()) {
            gameStateManager.hideRoundInfo();
            gameStateManager.setWaitingForKeyPress(false);
        }
        
        // 일시정지 상태 변화 감지
        handlePauseState();
        
        // 게임플레이 업데이트 (일시정지 상태가 아닐 때만)
        if (shouldUpdateGameplay()) {
            updateGameplay(delta);
        }

        // Ship 이동 & 발사
        if (shouldUpdateShip()) {
            updateShipMovement();
        }

        // 충돌 처리
        if (shouldUpdateCollisions()) {
            updateCollisions();
        }
    }
    
    private void handlePauseState() {
        boolean currentlyPaused = gameStateManager.isPaused();
        
        if (currentlyPaused && !wasPaused) {
            gameStateManager.startPause();
        } else if (!currentlyPaused && wasPaused) {
            gameStateManager.endPause();
        }
        
        wasPaused = currentlyPaused;
    }
    
    private boolean shouldUpdateGameplay() {
        return gameStateManager.isGameplayActive();
    }
    
    private void updateGameplay(long delta) {
        // 스킬 효과 업데이트
        skillManager.updateSkillEffects();
        
        // 엔티티 이동
        ArrayList<Entity> entities = gameStateManager.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (entity != null) {
                entity.move(delta);
            }
        }
        
        // 적 발사 처리
        tryAlienFire();
    }
    
    private boolean shouldUpdateShip() {
        return ship != null && gameStateManager.isShipControlActive();
    }
    
    private void updateShipMovement() {
        ship.setHorizontalMovement(0);
        if (inputManager.isLeftPressed() && !inputManager.isRightPressed()) {
            ship.setHorizontalMovement(-moveSpeed);
        } else if (inputManager.isRightPressed() && !inputManager.isLeftPressed()) {
            ship.setHorizontalMovement(moveSpeed);
        }
        if (inputManager.isFirePressed()) {
            game.tryToFire();
        }
    }
    
    private boolean shouldUpdateCollisions() {
        return gameStateManager.isCollisionCheckActive();
    }
    
    private void updateCollisions() {
        ArrayList<Entity> entities = gameStateManager.getEntities();
        
        // 충돌 검사
        for (int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            for (int j = i + 1; j < entities.size(); j++) {
                Entity e2 = entities.get(j);
                if (e1.collidesWith(e2)) {
                    e1.collidedWith(e2);
                    e2.collidedWith(e1);
                }
            }
        }
        
        // 제거 대기 엔티티 제거
        entities.removeAll(gameStateManager.getRemoveList());
        gameStateManager.getRemoveList().clear();
        
        // 로직 실행
        if (gameStateManager.isLogicRequiredThisLoop()) {
            for (Entity e : entities) {
                e.doLogic();
            }
            gameStateManager.setLogicRequiredThisLoop(false);
        }
    }
    
    /**
     * 적 발사 처리
     */
    private void tryAlienFire() {
        if (System.currentTimeMillis() - gameStateManager.getLastAlienFire() < 
            gameStateManager.getAlienFiringInterval()) {
            return;
        }
        
        ArrayList<AlienEntity> aliens = new ArrayList<>();
        ArrayList<Entity> entities = gameStateManager.getEntities();
        
        for (Entity entity : entities) {
            if (entity instanceof AlienEntity) {
                aliens.add((AlienEntity) entity);
            }
        }
        
        if (aliens.size() > 0) {
            int randomIndex = (int) (Math.random() * aliens.size());
            AlienEntity alien = aliens.get(randomIndex);
            alien.tryToFire();
            gameStateManager.setLastAlienFire(System.currentTimeMillis());
        }
    }
}

