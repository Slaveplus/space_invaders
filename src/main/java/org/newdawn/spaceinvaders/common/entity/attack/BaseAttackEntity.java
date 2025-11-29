package org.newdawn.spaceinvaders.common.entity.attack;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import org.newdawn.spaceinvaders.common.GameContext;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;

/**
 * 보스 공격 엔티티의 공통 베이스.
 * 이미지 로딩, 화면 밖 체크, 충돌 처리 등의 공통 로직을 제공합니다.
 */
public abstract class BaseAttackEntity extends Entity {
    protected final GameContext game;

    protected BaseAttackEntity(GameContext game, String spriteRef, int x, int y) {
        super(spriteRef, x, y);
        this.game = game;
    }

    /**
     * ShipEntity에 데미지를 적용합니다.
     */
    protected void damageShip(ShipEntity ship, int damage) {
        if (ship != null) {
            game.notifyPlayerDamaged(ship.getOwnerId(), damage);
        } else {
            game.notifyPlayerDamaged(damage);
        }
    }

    /**
     * ShipEntity가 무적 상태인지 확인합니다.
     */
    protected boolean isInvincible(ShipEntity ship) {
        return game.isPlayerInvincible(ship != null ? ship.getOwnerId() : null);
    }

    /**
     * 중앙 기준으로 Bounds를 생성합니다.
     */
    protected Rectangle centeredBounds(int width, int height) {
        int drawX = (int) Math.round(x) - width / 2;
        int drawY = (int) Math.round(y) - height / 2;
        return new Rectangle(drawX, drawY, width, height);
    }

    /**
     * 좌상단 기준으로 Bounds를 생성합니다.
     */
    protected Rectangle topLeftBounds(int width, int height) {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    /**
     * 오프셋 기준으로 Bounds를 생성합니다.
     */
    protected Rectangle offsetBounds(int offsetX, int offsetY, int width, int height) {
        return new Rectangle((int) Math.round(x) + offsetX, (int) Math.round(y) + offsetY, width, height);
    }

    // ========== 공통 이미지 로딩 메서드 ==========

    /**
     * SpriteStore를 사용하여 이미지를 로드합니다.
     * 
     * @param spriteRef 스프라이트 참조 경로
     * @return 로드된 이미지, 실패 시 null
     */
    protected Image loadImageFromSpriteStore(String spriteRef) {
        try {
            return SpriteStore.get().getSprite(spriteRef).getImage();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Toolkit을 사용하여 이미지를 로드합니다.
     * 
     * @param imagePath 이미지 경로
     * @return 로드된 이미지, 실패 시 null
     */
    protected Image loadImageFromToolkit(String imagePath) {
        try {
            java.net.URL imageURL = getClass().getClassLoader().getResource(imagePath);
            if (imageURL != null) {
                return Toolkit.getDefaultToolkit().createImage(imageURL);
            }
        } catch (Exception e) {
            // 로드 실패 시 null 반환
        }
        return null;
    }

    /**
     * MediaTracker를 사용하여 이미지를 로드합니다.
     * 
     * @param imagePath 이미지 경로
     * @return 로드된 이미지, 실패 시 null
     */
    protected Image loadImageWithMediaTracker(String imagePath) {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource(imagePath);
            if (imageUrl != null) {
                Image image = Toolkit.getDefaultToolkit().createImage(imageUrl);
                MediaTracker tracker = new MediaTracker(new java.awt.Canvas());
                tracker.addImage(image, 0);
                tracker.waitForAll();
                if (!tracker.isErrorAny()) {
                    return image;
                }
            }
        } catch (Exception e) {
            // 로드 실패 시 null 반환
        }
        return null;
    }

    // ========== 공통 화면 밖 체크 메서드 ==========

    /**
     * 화면 밖으로 나갔는지 확인하고 제거합니다.
     * 기본 경계: x < -50 || x > 850 || y < -50 || y > 650
     * 
     * @return 화면 밖이면 true (제거됨)
     */
    protected boolean checkAndRemoveIfOutOfBounds() {
        return checkAndRemoveIfOutOfBounds(-50, 850, -50, 650);
    }

    /**
     * 화면 밖으로 나갔는지 확인하고 제거합니다.
     * 
     * @param minX 최소 X 좌표
     * @param maxX 최대 X 좌표
     * @param minY 최소 Y 좌표
     * @param maxY 최대 Y 좌표
     * @return 화면 밖이면 true (제거됨)
     */
    protected boolean checkAndRemoveIfOutOfBounds(double minX, double maxX, double minY, double maxY) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            game.removeEntity(this);
            return true;
        }
        return false;
    }

    /**
     * Y 좌표가 화면 밖으로 나갔는지 확인하고 제거합니다.
     * 
     * @param maxY 최대 Y 좌표 (기본값: 600)
     * @return 화면 밖이면 true (제거됨)
     */
    protected boolean checkAndRemoveIfOutOfBoundsY(double maxY) {
        if (y > maxY || y < -100) {
            game.removeEntity(this);
            return true;
        }
        return false;
    }

    // ========== 공통 충돌 처리 메서드 ==========

    /**
     * ShipEntity와의 기본 충돌 처리를 수행합니다.
     * 무적 상태 체크 및 데미지 적용을 포함합니다.
     * 
     * @param other 충돌한 엔티티
     * @param damage 적용할 데미지
     * @return 충돌 처리 완료 여부
     */
    protected boolean handleShipCollision(Entity other, int damage) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            if (isInvincible(ship)) {
                game.removeEntity(this);
                return true;
            }
            damageShip(ship, damage);
            game.removeEntity(this);
            return true;
        }
        return false;
    }

    /**
     * ShipEntity와의 거리 기반 충돌 처리를 수행합니다.
     * 
     * @param other 충돌한 엔티티
     * @param damage 적용할 데미지
     * @param collisionRadius 충돌 반경
     * @return 충돌 처리 완료 여부
     */
    protected boolean handleShipCollisionWithRadius(Entity other, int damage, double collisionRadius) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            if (isInvincible(ship)) {
                game.removeEntity(this);
                return true;
            }
            double distance = Math.hypot(this.x - other.getX(), this.y - other.getY());
            if (distance <= collisionRadius) {
                damageShip(ship, damage);
                game.removeEntity(this);
                return true;
            }
        }
        return false;
    }

    // ========== 공통 시간 기반 제거 메서드 ==========

    /**
     * 공격 지속 시간이 지났는지 확인하고 제거합니다.
     * 
     * @param startTime 공격 시작 시간
     * @param duration 공격 지속 시간 (밀리초)
     * @return 시간이 지났으면 true (제거됨)
     */
    protected boolean checkAndRemoveIfExpired(long startTime, long duration) {
        if (System.currentTimeMillis() - startTime > duration) {
            game.removeEntity(this);
            return true;
        }
        return false;
    }
}
