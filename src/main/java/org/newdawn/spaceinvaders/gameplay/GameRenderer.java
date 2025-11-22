package org.newdawn.spaceinvaders.gameplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.ShotEntity;

/**
 * 게임 렌더링을 담당하는 클래스
 * Game.java의 렌더링 책임을 분리
 */
public class GameRenderer {
    private final BackgroundRenderer backgroundRenderer;
    private final UIRenderer uiRenderer;
    private final ResolutionManager resolutionManager;
    
    public GameRenderer(BackgroundRenderer backgroundRenderer, 
                       UIRenderer uiRenderer, ResolutionManager resolutionManager) {
        this.backgroundRenderer = backgroundRenderer;
        this.uiRenderer = uiRenderer;
        this.resolutionManager = resolutionManager;
    }
    
    /**
     * 게임 전체 렌더링
     */
    public void render(Graphics2D g, GameStateManager gameStateManager, SkillManager skillManager) {
        AffineTransform original = g.getTransform();
        
        // 해상도 스케일링 적용
        if (resolutionManager != null) {
            double offsetX = resolutionManager.getOffsetX();
            double offsetY = resolutionManager.getOffsetY();
            double scale = resolutionManager.getUniformScale();
            g.translate(offsetX, offsetY);
            g.scale(scale, scale);
        }
        
        // 배경 렌더링
        backgroundRenderer.draw(g);
        
        // 엔티티 렌더링
        ArrayList<Entity> entities = gameStateManager.getEntities();
        for (Entity entity : entities) {
            entity.draw(g);
        }
        
        // UI & 오버레이 렌더링
        uiRenderer.drawGameUI(g, gameStateManager, skillManager);
        
        // 메뉴 오버레이 렌더링
        if (gameStateManager.isShowingPauseMenu()) {
            uiRenderer.drawPauseOverlay(g, gameStateManager.getSelectedPauseMenuItem());
        }
        if (gameStateManager.isShowingSkillMenu()) {
            uiRenderer.drawSkillOverlay(
                g,
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
        if (gameStateManager.isShowingRoundInfo()) {
            uiRenderer.drawRoundInfoOverlay(g, gameStateManager);
        }
        if (gameStateManager.isShowingQuitConfirm()) {
            uiRenderer.drawQuitConfirmOverlay(g, gameStateManager);
        }
        if (gameStateManager.isWaitingForKeyPress()) {
            uiRenderer.drawMessage(g, gameStateManager.getMessage());
        }
        
        // 디버그 히트박스 오버레이
        if (gameStateManager.isDebugDrawHitboxes()) {
            drawHitboxOverlay(g, gameStateManager);
        }
        
        // 원래 Transform 복원
        if (resolutionManager != null) {
            g.setTransform(original);
        }
    }
    
    /**
     * 히트박스 디버그 오버레이 그리기
     */
    private void drawHitboxOverlay(Graphics2D g, GameStateManager gameStateManager) {
        Stroke previous = g.getStroke();
        g.setStroke(new BasicStroke(1f));
        
        ArrayList<Entity> entities = gameStateManager.getEntities();
        for (Entity entity : entities) {
            if (entity == null) {
                continue;
            }
            java.awt.Rectangle rect = entity.getBounds();
            if (rect == null) {
                continue;
            }
            
            if (entity instanceof ShipEntity) {
                g.setColor(new Color(0, 200, 0, 160));
            } else if (entity instanceof ShotEntity) {
                ShotEntity shot = (ShotEntity) entity;
                g.setColor(shot.isAlienShot() ? new Color(255, 0, 0, 160) : new Color(0, 200, 255, 160));
            } else {
                g.setColor(new Color(255, 255, 0, 120));
            }
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
        
        g.setStroke(previous);
    }
}

