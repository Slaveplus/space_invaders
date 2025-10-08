package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;
import org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore;

import java.awt.*;
import java.util.List;

/**
 * Magnetic field entity that creates attraction or repulsion forces
 * Affects player ship movement and shot trajectories
 */
public class MagneticFieldEntity extends Entity {
    private Game game;
    private double fieldRadius; // 자기장 반경
    private double fieldStrength; // 자기장 강도 (양수=인력, 음수=척력)
    private long duration; // 지속 시간 (밀리초)
    private long startTime;
    private boolean isActive;
    
    // 시각적 효과용
    private int pulsePhase = 0;
    private double currentRadius;
    
    public MagneticFieldEntity(Game game, int x, int y, double radius, double strength, long duration) {
        super("sprites/shot.gif", x, y); // 임시 스프라이트, 실제로는 그리지 않음
        
        this.game = game;
        this.fieldRadius = radius;
        this.fieldStrength = strength;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
        this.currentRadius = radius;
        
        System.out.println("Magnetic field created: Center(" + x + "," + y + ") Radius:" + radius + " Strength:" + strength);
    }
    
    @Override
    public void move(long delta) {
        if (!isActive) return;
        
        long currentTime = System.currentTimeMillis();
        
        // 지속 시간 체크
        if (currentTime - startTime > duration) {
            isActive = false;
            game.removeEntity(this);
            System.out.println("Magnetic field expired");
            return;
        }
        
        // 고정 크기 유지 (펄스 효과 제거)
        currentRadius = fieldRadius;
        
        // 자기장 영향 적용
        applyMagneticEffects();
    }
    
    /**
     * 자기장의 영향을 받는 모든 엔티티에 힘을 적용
     */
    private void applyMagneticEffects() {
        List<Entity> entities = game.getEntities();
        
        for (Entity entity : entities) {
            if (entity == this) continue; // 자기 자신은 제외
            
            double distance = getDistanceTo(entity);
            
            // 자기장 범위 내에 있는지 확인
            if (distance <= currentRadius && distance > 0) {
                
                // 거리에 따른 힘 계산 (거리가 가까울수록 강함)
                double force = fieldStrength * (1.0 - distance / currentRadius);
                
                // 방향 계산 (자기장 중심으로부터의 방향)
                double angle = Math.atan2(entity.getY() - y, entity.getX() - x);
                
                // 인력/척력에 따른 방향 조정
                double forceX = Math.cos(angle) * force;
                double forceY = Math.sin(angle) * force;
                
                // 엔티티 타입별로 다른 영향 적용
                if (entity.getClass().getSimpleName().equals("ShipEntity")) {
                    applyForceToShip(entity, forceX, forceY);
                } else if (entity.getClass().getSimpleName().equals("ShotEntity")) {
                    applyForceToShot(entity, forceX, forceY);
                }
            }
        }
    }
    
    /**
     * 플레이어 함선에 자기장 힘 적용
     */
    private void applyForceToShip(Entity ship, double forceX, double forceY) {
        // 함선의 이동 속도에 자기장 힘을 추가
        ship.setDX(ship.getDX() + forceX * 0.001); // 0.001은 힘의 스케일링 팩터
        ship.setDY(ship.getDY() + forceY * 0.001);
        
        // 최대 속도 제한
        double maxSpeed = 0.5;
        double speed = Math.sqrt(ship.getDX() * ship.getDX() + ship.getDY() * ship.getDY());
        if (speed > maxSpeed) {
            ship.setDX(ship.getDX() / speed * maxSpeed);
            ship.setDY(ship.getDY() / speed * maxSpeed);
        }
    }
    
    /**
     * 총알에 자기장 힘 적용
     */
    private void applyForceToShot(Entity shot, double forceX, double forceY) {
        // 총알의 이동 방향을 자기장에 의해 변경
        shot.setDX(shot.getDX() + forceX * 0.002); // 총알은 더 민감하게 반응
        shot.setDY(shot.getDY() + forceY * 0.002);
        
        // 총알의 최대 속도 제한
        double maxShotSpeed = 1.0;
        double speed = Math.sqrt(shot.getDX() * shot.getDX() + shot.getDY() * shot.getDY());
        if (speed > maxShotSpeed) {
            shot.setDX(shot.getDX() / speed * maxShotSpeed);
            shot.setDY(shot.getDY() / speed * maxShotSpeed);
        }
    }
    
    /**
     * 다른 엔티티와의 거리 계산
     */
    private double getDistanceTo(Entity other) {
        double dx = other.getX() - this.x;
        double dy = other.getY() - this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    @Override
    public void draw(Graphics g) {
        if (!isActive) return;
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 자기장 시각화
        int centerX = (int) x;
        int centerY = (int) y;
        int radius = (int) currentRadius;
        
        
        // 고정 투명도
        float alpha = 0.4f;
        
        // 인력/척력에 따른 색상 결정
        if (fieldStrength > 0) {
            // 인력 - 빨간색 계열
            g2d.setColor(new Color(1.0f, 0.3f, 0.3f, alpha));
        } else {
            // 척력 - 파란색 계열
            g2d.setColor(new Color(0.3f, 0.3f, 1.0f, alpha));
        }
        
        // 자기장 원형 영역 그리기
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // 경계선 그리기
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(new Color(1.0f, 1.0f, 1.0f, alpha * 0.8f));
        g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // 중심점 표시
        g2d.setColor(Color.WHITE);
        g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
        
        g2d.dispose();
    }
    
    @Override
    public void collidedWith(Entity other) {
        // 자기장은 충돌하지 않음
    }
    
    // Getters
    public boolean isActive() {
        return isActive;
    }
    
    public double getFieldRadius() {
        return fieldRadius;
    }
    
    public double getFieldStrength() {
        return fieldStrength;
    }
    
    public double getCurrentRadius() {
        return currentRadius;
    }
}
