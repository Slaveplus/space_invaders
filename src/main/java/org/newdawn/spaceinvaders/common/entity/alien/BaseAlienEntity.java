package org.newdawn.spaceinvaders.common.entity.alien;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.IntUnaryOperator;
 
 import org.newdawn.spaceinvaders.common.entity.Entity;
 import org.newdawn.spaceinvaders.common.sprite.Sprite;
/**
 * 싱글/멀티 공통 Alien 구현.
 */
public abstract class BaseAlienEntity extends Entity {
    protected final AlienEnvironment environment;

    protected double moveSpeed = 75;
    protected double horizontalSpeed = 75;
    protected Sprite[] frames = new Sprite[4];
    protected long lastFrameChange;
    protected long frameDuration = 250;
    protected int frameNumber;
    protected long lastFire = 0;
    protected long firingInterval = 2000;
    protected int currentHP = 2;
    protected int maxHP = 2;
    protected boolean movingRight = Math.random() < 0.5;
    protected boolean movingDown = Math.random() < 0.5;
    protected long lastDirectionChange = 0;
    protected long directionChangeInterval = 2000;
    protected long lastCollisionTime = 0;
    protected static final long COLLISION_COOLDOWN = 500;

    protected BaseAlienEntity(AlienEnvironment environment, String spriteRef, int x, int y, IntUnaryOperator hpResolver) {
        super(spriteRef, x, y);
        this.environment = environment;

        // Initialize HP using the resolver first
        this.maxHP = Math.max(1, hpResolver.applyAsInt(environment.getCurrentRound()));
        this.currentHP = this.maxHP;

        // Then initialize the rest
        initialize(environment.getCurrentRound());
    }

    private void initialize(int round) {
        frames[0] = sprite;
        frames[1] = sprite;
        frames[2] = sprite;
        frames[3] = sprite;
        moveSpeed = resolveBaseMoveSpeed(round);
        horizontalSpeed = moveSpeed * (0.8 + (Math.random() * 0.4));
        firingInterval = resolveFiringInterval(round);
        double speedVariation = 0.8 + (Math.random() * 0.4);
        double actualSpeed = moveSpeed * speedVariation;
        dx = movingRight ? actualSpeed : -actualSpeed;
        if (Math.random() < 0.3) {
            dy = (Math.random() - 0.5) * actualSpeed * 0.3;
        }
        lastDirectionChange = System.currentTimeMillis();
    }

    protected double resolveBaseMoveSpeed(int round) {
        return 75 + (round * 10);
    }

    protected long resolveFiringInterval(int round) {
        long interval;
        if (round == 1) {
            interval = 1000;
        } else if (round <= 3) {
            interval = 2000 - (round * 500);
        } else if (round <= 6) {
            interval = 700 - ((round - 3) * 100);
        } else {
            interval = Math.max(200, 400 - ((round - 6) * 50));
        }
        double randomFactor = 0.8 + (Math.random() * 0.4);
        return (long) (interval * randomFactor);
    }

    protected double getScaleForRound(int round) {
        return round == 1 ? 0.18 : 0.75;
    }

    protected void onAlienKilled() {
        environment.notifyAlienKilled();
    }

    private void updateFrame(long delta) {
        lastFrameChange += delta;
        if (lastFrameChange > frameDuration) {
            lastFrameChange = 0;
            frameNumber = (frameNumber + 1) % frames.length;
            sprite = frames[frameNumber];
        }
    }

    private void updateDirection(long currentTime) {
        if (currentTime - lastDirectionChange > directionChangeInterval) {
            shuffleHorizontalDirection(currentTime);
        }
    }

    private void onHorizontalBoundaryCollision(long currentTime) {
        syncHorizontalFromDx();
        if (Math.random() < 0.4) {
            changeVerticalDirection();
        }
        directionChangeInterval = 600 + (long) (Math.random() * 1200);
        lastDirectionChange = currentTime;
    }

    private void handleHorizontalMovement(double deltaSeconds, long currentTime) {
        if (movingRight) {
            x += horizontalSpeed * deltaSeconds;
            if (x >= 750) {
                x = 745;
                movingRight = false;
                onHorizontalBoundaryCollision(currentTime);
            }
        } else {
            x -= horizontalSpeed * deltaSeconds;
            if (x <= 10) {
                x = 15;
                movingRight = true;
                onHorizontalBoundaryCollision(currentTime);
            }
        }
    }

    private void handleVerticalMovement(long delta) {
        if (!movingDown) {
            y -= moveSpeed * delta * 0.0005;
            if (y <= 80) {
                movingDown = true;
            }
        } else {
            y += moveSpeed * delta * 0.0005;
            if (y >= 180) {
                movingDown = false;
            }
        }
    }

    private void clampYPosition() {
        if (y < 50) {
            y = 50;
        } else if (y > 300) {
            y = 300;
        }
    }

    @Override
    public void move(long delta) {
        updateFrame(delta);
        long currentTime = System.currentTimeMillis();
        updateDirection(currentTime);
        double deltaSeconds = delta * 0.001;
        handleHorizontalMovement(deltaSeconds, currentTime);
        handleVerticalMovement(delta);
        clampYPosition();
    }

    public void doLogic() {
        // no-op
    }

    public void tryToFire() {
        if (System.currentTimeMillis() - lastFire < firingInterval) {
            return;
        }
        int round = environment.getCurrentRound();
        double chance = round <= 2 ? 0.3 : round <= 4 ? 0.5 : round <= 6 ? 0.7 : 0.85;
        if (Math.random() < chance) {
            lastFire = System.currentTimeMillis();
            environment.addAimedAlienShot((int) (x + 10), (int) (y + 30), (int) x);
        }
    }

    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP <= 0) {
            environment.removeEntity(this);
            onAlienKilled();
        }
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    private void shuffleHorizontalDirection(long currentTime) {
        changeDirection();
        if (Math.random() < 0.1) {
            changeVerticalDirection();
        }
        lastDirectionChange = currentTime;
    }

    private void changeDirection() {
        movingRight = !movingRight;
        double speedVariation = 0.9 + (Math.random() * 0.2);
        dx = movingRight ? moveSpeed * speedVariation : -moveSpeed * speedVariation;
        syncHorizontalFromDx();
    }

    private void syncHorizontalFromDx() {
        horizontalSpeed = Math.abs(dx);
    }

    private void changeVerticalDirection() {
        movingDown = !movingDown;
        double verticalSpeedVariation = 0.8 + (Math.random() * 0.4);
        dy = movingDown ? moveSpeed * 0.3 * verticalSpeedVariation : -moveSpeed * 0.3 * verticalSpeedVariation;
    }

    public void collideWithAlien() {
        changeDirection();
        if (Math.random() < 0.3) {
            changeVerticalDirection();
        }
    }

    private boolean isBossRound() {
        return environment.getCurrentRound() % 5 == 0;
    }

    private void handleAlienCollision(Entity other) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCollisionTime < COLLISION_COOLDOWN) {
            return;
        }
        lastCollisionTime = currentTime;
        double dxToOther = other.getX() - x;
        double dyToOther = other.getY() - y;
        double distanceToOther = Math.sqrt(dxToOther * dxToOther + dyToOther * dyToOther);
        if (distanceToOther > 0) {
            double moveAwayX = -dxToOther / distanceToOther;
            double moveAwayY = -dyToOther / distanceToOther;
            this.dx = moveAwayX * moveSpeed * 1.2;
            this.dy = moveAwayY * moveSpeed * 0.4;
            movingRight = this.dx > 0;
            movingDown = this.dy > 0;
        }
    }

    private void avoidPlayer() {
        try {
            int playerX = environment.getPlayerShipX();
            int playerY = environment.getPlayerShipY();
            double dxToPlayer = playerX - x;
            double dyToPlayer = playerY - y;
            double distanceToPlayer = Math.sqrt(dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer);
            double minDistance = 120;
            if (distanceToPlayer < minDistance && distanceToPlayer > 0) {
                double moveAwayX = -dxToPlayer / distanceToPlayer;
                double moveAwayY = -dyToPlayer / distanceToPlayer;
                this.dx = moveAwayX * moveSpeed * 1.5;
                this.dy = moveAwayY * moveSpeed * 0.5;
                movingRight = this.dx > 0;
                movingDown = this.dy > 0;
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (sprite != null) {
            double scale = getScaleForRound(environment.getCurrentRound());
            int scaledWidth = (int) (sprite.getWidth() * scale);
            int scaledHeight = (int) (sprite.getHeight() * scale);
            int drawX = (int) Math.round(x) - scaledWidth / 2;
            int drawY = (int) Math.round(y) - scaledHeight / 2;
            g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + scaledWidth, drawY + scaledHeight,
                    0, 0, sprite.getWidth(), sprite.getHeight(), null);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (sprite != null) {
            double scale = getScaleForRound(environment.getCurrentRound());
            int scaledWidth = (int) (sprite.getWidth() * scale);
            int scaledHeight = (int) (sprite.getHeight() * scale);
            int drawX = (int) Math.round(x) - scaledWidth / 2;
            int drawY = (int) Math.round(y) - scaledHeight / 2;
            int hitboxWidth = (int) Math.max(4, scaledWidth * 0.05);
            int hitboxHeight = (int) Math.max(4, scaledHeight * 0.03);
            int hitboxX = drawX + (scaledWidth - hitboxWidth) / 2;
            int hitboxY = drawY + (scaledHeight - hitboxHeight) / 2;
            return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
        }
        return super.getBounds();
    }

    @Override
    public void collidedWith(Entity other) {
        String type = other.getClass().getSimpleName();
        if ("BossEntity".equals(type)) {
            double dxToBoss = other.getX() - x;
            double dyToBoss = other.getY() - y;
            double distance = Math.sqrt(dxToBoss * dxToBoss + dyToBoss * dyToBoss);
            if (distance > 0) {
                double moveAwayX = -dxToBoss / distance;
                double moveAwayY = -dyToBoss / distance;
                this.dx = moveAwayX * moveSpeed * 1.5;
                this.dy = moveAwayY * moveSpeed * 0.3;
                movingRight = this.dx > 0;
                movingDown = this.dy > 0;
            }
        } else if (type.equals(getClass().getSimpleName()) && !isBossRound()) {
            handleAlienCollision(other);
        }
    }
}
