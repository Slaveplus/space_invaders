package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Rectangle;

import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;
import org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore;

/**
 * 엔티티는 게임에 나타나는 모든 요소를 나타냅니다.
 * 엔티티는 서브클래스나 외부에서 정의된 속성 세트를 기반으로
 * 충돌과 이동을 해결하는 책임을 집니다.
 * 
 * 위치에 double을 사용한다는 점에 주목하세요. 픽셀 위치가 정수라는 점을 고려하면
 * 이상할 수 있습니다. 하지만 double을 사용하면 엔티티가 부분 픽셀을 이동할 수 있습니다.
 * 물론 픽셀 중간에 표시된다는 의미는 아니지만, 이동할 때 정확도를 잃지 않도록 해줍니다.
 * 
 * @author Kevin Glass
 */
public abstract class Entity {
	// ===== 멀티플레이 확장 필드 =====
	private static final java.util.concurrent.atomic.AtomicLong ID_GENERATOR = new java.util.concurrent.atomic.AtomicLong();
	/** 전역 유니크 엔티티 ID (동기화/직렬화 용) */
	private final long entityId = ID_GENERATOR.incrementAndGet();
	/** 이 엔티티를 소유/생성한 플레이어 ID (탄환 등); 글로벌/중립은 null */
	private String ownerId;

	/** 이 엔티티의 현재 x 위치 */ 
	protected double x;
	/** 이 엔티티의 현재 y 위치 */
	protected double y;
	/** 이 엔티티를 나타내는 스프라이트 */
	protected Sprite sprite;
	/** 이 엔티티의 현재 수평 속도 (픽셀/초) */
	protected double dx;
	/** 이 엔티티의 현재 수직 속도 (픽셀/초) */
	protected double dy;
	/** 충돌 해결 중 이 엔티티에 사용되는 사각형 */
	private Rectangle me = new Rectangle();
	/** 충돌 해결 중 다른 엔티티에 사용되는 사각형 */
	private Rectangle him = new Rectangle();
	
	/**
	 * 스프라이트 이미지와 위치를 기반으로 엔티티를 구성합니다.
	 * 
	 * @param ref 이 엔티티에 표시될 이미지의 참조
 	 * @param x 이 엔티티의 초기 x 위치
	 * @param y 이 엔티티의 초기 y 위치
	 */
	public Entity(String ref,int x,int y) {
		this.sprite = SpriteStore.get().getSprite(ref);
		this.x = x;
		this.y = y;
	}

	// ---- 멀티플레이 편의 메서드 ----
	public long getEntityId() { return entityId; }
	public String getOwnerId() { return ownerId; }
	public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

	/**
	 * 상태 스냅샷 직렬화용 DTO (간단 버전). 실제 네트워크 전송 시 확장 가능.
	 */
	public EntitySnapshot toSnapshot() {
		return new EntitySnapshot(entityId, ownerId, spriteRef(), x, y, dx, dy, getBounds().width, getBounds().height);
	}

	/** 서브클래스가 sprite 경로를 알 수 있도록 기본 ref 표시 (SpriteStore 내부 구조 의존 회피) */
	protected String spriteRef() { return null; /* Sprite 원본 경로 관리 필요시 SpriteStore 개선 후 구현 */ }

	/** 스냅샷으로부터 좌표/속도 등 갱신 (예: 클라이언트 보간) */
	public void applySnapshot(EntitySnapshot snap) {
		this.x = snap.x; this.y = snap.y; this.dx = snap.dx; this.dy = snap.dy; // sprite 교체는 필요 시 별도 처리
	}
	
	/**
	 * 경과된 시간을 기반으로 이 엔티티가 스스로 이동하도록 요청합니다.
	 * 
	 * @param delta 경과된 시간 (밀리초)
	 */
	public void move(long delta) {
		// 이동 속도를 기반으로 엔티티의 위치 업데이트
		x += (delta * dx) / 1000;
		y += (delta * dy) / 1000;
	}
	
	/**
	 * 이 엔티티의 수평 속도를 설정합니다
	 * 
	 * @param dx 이 엔티티의 수평 속도 (픽셀/초)
	 */
	public void setHorizontalMovement(double dx) {
		this.dx = dx;
	}

	/**
	 * 이 엔티티의 수직 속도를 설정합니다
	 * 
	 * @param dy 이 엔티티의 수직 속도 (픽셀/초)
	 */
	public void setVerticalMovement(double dy) {
		this.dy = dy;
	}
	
	/**
	 * 이 엔티티의 수평 속도를 가져옵니다
	 * 
	 * @return 이 엔티티의 수평 속도 (픽셀/초)
	 */
	public double getHorizontalMovement() {
		return dx;
	}

	/**
	 * 이 엔티티의 수직 속도를 가져옵니다
	 * 
	 * @return 이 엔티티의 수직 속도 (픽셀/초)
	 */
	public double getVerticalMovement() {
		return dy;
	}
	
	/**
	 * Draw this entity to the graphics context provided
	 * 
	 * @param g The graphics context on which to draw
	 */
	public void draw(Graphics g) {
		sprite.draw(g,(int) x,(int) y);
	}
	
	/**
	 * Do the logic associated with this entity. This method
	 * will be called periodically based on game events
	 */
	public void doLogic() {
	}
	
	/**
	 * Get the x location of this entity
	 * 
	 * @return The x location of this entity
	 */
	public int getX() {
		return (int) x;
	}

	/**
	 * Get the y location of this entity
	 * 
	 * @return The y location of this entity
	 */
	public int getY() {
		return (int) y;
	}
	
	/**
	 * Check if this entity collised with another.
	 * 
	 * @param other The other entity to check collision against
	 * @return True if the entities collide with each other
	 */
	public boolean collidesWith(Entity other) {
		// Use getBounds() if available (for entities with custom collision boxes)
		// Otherwise use default sprite bounds
		Rectangle myBounds = getBounds();
		Rectangle otherBounds = other.getBounds();
		
		me.setBounds(myBounds);
		him.setBounds(otherBounds);

		return me.intersects(him);
	}
	
	/**
	 * Get the collision bounds for this entity
	 * 
	 * @return Rectangle representing the collision bounds
	 */
	public Rectangle getBounds() {
		return new Rectangle((int) x, (int) y, sprite.getWidth(), sprite.getHeight());
	}
	
	/**
	 * 엔티티의 스킨을 동적으로 변경
	 * 
	 * @param newSkinPath 새로운 스킨 파일 경로
	 */
	public void changeSkin(String newSkinPath) {
		this.sprite = SpriteStore.get().getSprite(newSkinPath);
	}
	
	/**
	 * Notification that this entity collided with another.
	 * 
	 * @param other The entity with which this entity collided.
	 */
	public abstract void collidedWith(Entity other);
	
	// Getters and setters for dx and dy
	public double getDX() {
		return dx;
	}
	
	public double getDY() {
		return dy;
	}
	
	public void setDX(double dx) {
		this.dx = dx;
	}
	
	public void setDY(double dy) {
		this.dy = dy;
	}
	
}