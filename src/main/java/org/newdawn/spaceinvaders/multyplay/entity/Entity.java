package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Graphics;
import java.awt.Rectangle;

import org.newdawn.spaceinvaders.multyplay.sprite.Sprite;
import org.newdawn.spaceinvaders.multyplay.sprite.SpriteStore;

/**
 * 엔티티는 게임에 나타나는 모든 요소를 나타냅니다.
 * 엔티티는 서브클래스나 외부에서 정의된 속성 집합을 기반으로
 * 충돌과 이동을 처리합니다.
 * 
 * 위치에 double을 사용한다는 점에 주의하세요. 픽셀 위치가 정수라는 점을 고려하면
 * 이상하게 보일 수 있지만, double을 사용하면 엔티티가 부분 픽셀을 이동할 수 있습니다.
 * 물론 픽셀의 절반 위치에 표시되는 것은 아니지만, 이동 시 정확도를 잃지 않을 수 있습니다.
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
	protected String spritePath;
	/** 이 엔티티의 현재 수평 속도 (픽셀/초) */
	protected double dx;
	/** 이 엔티티의 현재 수직 속도 (픽셀/초) */
	protected double dy;
	/** 충돌 해결 시 이 엔티티에 사용되는 사각형 */
	private Rectangle me = new Rectangle();
	/** 충돌 해결 시 다른 엔티티에 사용되는 사각형 */
	private Rectangle him = new Rectangle();
	
	/**
	 * 스프라이트 이미지와 위치를 기반으로 엔티티를 생성합니다.
	 * 
	 * @param ref 이 엔티티에 표시될 이미지에 대한 참조
 	 * @param x 이 엔티티의 초기 x 위치
	 * @param y 이 엔티티의 초기 y 위치
	 */
	public Entity(String ref,int x,int y) {
		this.spritePath = ref;
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
		return new EntitySnapshot(
				entityId,
				ownerId,
				typeId(),
				spriteRef(),
				x,
				y,
				dx,
				dy,
				getBounds().width,
				getBounds().height,
				snapshotMetadata());
	}

	/** 서브클래스가 sprite 경로를 알 수 있도록 기본 ref 표시 (SpriteStore 내부 구조 의존 회피) */
	protected String spriteRef() { return spritePath; }
	/** 네트워크 동기화를 위한 타입 식별자 */
	protected String typeId() { return getClass().getSimpleName(); }
	/** 추가 메타 데이터 직렬화 (필요없는 경우 null) */
	protected String snapshotMetadata() { return null; }
	/** 스냅샷 적용 시 메타데이터 반영 */
	protected void applySnapshotMetadata(String metadata) { /* default no-op */ }

	/** 스냅샷으로부터 좌표/속도 등 갱신 (예: 클라이언트 보간) */
	public void applySnapshot(EntitySnapshot snap) {
		if (snap.sprite != null && !snap.sprite.equals(spritePath)) {
			spritePath = snap.sprite;
			sprite = SpriteStore.get().getSprite(spritePath);
		}
		this.ownerId = snap.ownerId;
		this.x = snap.x; this.y = snap.y; this.dx = snap.dx; this.dy = snap.dy; // sprite 교체는 필요 시 별도 처리
		if (snap.metadata != null) {
			applySnapshotMetadata(snap.metadata);
		}
	}
	
	/**
	 * Request that this entity move itself based on a certain ammount
	 * of time passing.
	 * 
	 * @param delta The ammount of time that has passed in milliseconds
	 */
	public void move(long delta) {
		// update the location of the entity based on move speeds
		x += (delta * dx) / 1000;
		y += (delta * dy) / 1000;
	}
	
	/**
	 * Set the horizontal speed of this entity
	 * 
	 * @param dx The horizontal speed of this entity (pixels/sec)
	 */
	public void setHorizontalMovement(double dx) {
		this.dx = dx;
	}

	/**
	 * Set the vertical speed of this entity
	 * 
	 * @param dx The vertical speed of this entity (pixels/sec)
	 */
	public void setVerticalMovement(double dy) {
		this.dy = dy;
	}
	
	/**
	 * Get the horizontal speed of this entity
	 * 
	 * @return The horizontal speed of this entity (pixels/sec)
	 */
	public double getHorizontalMovement() {
		return dx;
	}

	/**
	 * Get the vertical speed of this entity
	 * 
	 * @return The vertical speed of this entity (pixels/sec)
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
		System.out.println("Entity.changeSkin 호출: " + newSkinPath);
		System.out.println("  - 이전 스킨: " + this.spritePath);
		this.spritePath = newSkinPath;
		this.sprite = SpriteStore.get().getSprite(newSkinPath);
		System.out.println("  - 새 스킨 적용됨: " + this.spritePath);
		System.out.println("  - 스프라이트 로드됨: " + (this.sprite != null ? "성공" : "실패"));
	}
	
	/**
	 * Notification that this entity collided with another.
	 * 
	 * @param other The entity with which this entity collided.
	 */
	public abstract void collidedWith(Entity other);
}
