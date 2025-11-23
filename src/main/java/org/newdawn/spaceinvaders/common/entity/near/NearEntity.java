package org.newdawn.spaceinvaders.common.entity.near;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShotEntity;
import org.newdawn.spaceinvaders.common.sprite.SpriteStore;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

/**
 * 보스 전 근접 몬스터 공용 구현.
 */
public class NearEntity extends Entity {
    private final NearEnvironment environment;
    private final int round;
    private final int monsterId;

    private Image nearImage;
    private int currentHP;
    private final int maxHP;
    private double moveSpeed = 50;
    private boolean movingRight = true;
    private boolean movingDown = true;
    private double currentVelocityX = 0;
    private double currentVelocityY = 0;
    private double targetVelocityX = 0;
    private double targetVelocityY = 0;
    private long lastShot = 0;
    private final long shotInterval;

    private static final int NEAR_WIDTH = 120;
    private static final int NEAR_HEIGHT = 120;

    private static String spriteForRound(int round) {
        switch (round) {
            case 1: return SpriteConstants.BOSS_1_NEAR_PNG;
            case 3: return SpriteConstants.BOSS_2_NEAR_PNG;
            case 5: return SpriteConstants.BOSS_3_NEAR_PNG;
            case 7: return SpriteConstants.BOSS_4_NEAR_PNG;
            default: return SpriteConstants.BOSS_1_NEAR_PNG;
        }
    }

    private static int bossIndexFromNear(int nearRound) {
        switch (nearRound) {
            case 1: return 1;
            case 3: return 2;
            case 5: return 3;
            case 7: return 4;
            default: return 1;
        }
    }

    private static String shotSpriteForRound(int nearRound) {
        switch (nearRound) {
            case 1:
                return SpriteConstants.ICE_BALL_GIF;
            case 3:
                return SpriteConstants.ROUND_2_ATTACK_1_GIF;
            case 5:
                return SpriteConstants.SHOT_GIF;
            case 7:
                return SpriteConstants.HEAT_GIF;
            default:
                return SpriteConstants.SHOT_GIF;
        }
    }

    public NearEntity(NearEnvironment environment, int x, int y, int round, int monsterId) {
        super(spriteForRound(round), x, y);
        this.environment = environment;
        this.round = round;
        this.monsterId = monsterId;

        int bossRound = bossIndexFromNear(round);
        this.maxHP = 20 + (bossRound * 15);
        this.currentHP = maxHP;

        moveSpeed = 50 + (bossRound * 10);
        
        // Set unique shot interval based on monster ID and round
        // 1라운드는 그대로, 3,5,7라운드는 점진적으로 공격 속도 증가 (간격 감소)
        if (round == 1) {
            // 1라운드: 3000, 3500, 4000, 4500, 5000, 5500 ms (기본 속도)
            shotInterval = 3000 + (monsterId * 500);
        } else if (round == 3) {
            // 3라운드: 2500, 2900, 3300, 3700, 4100, 4500 ms (더 빠름)
            shotInterval = 2500 + (monsterId * 400);
        } else if (round == 5) {
            // 5라운드: 2000, 2300, 2600, 2900, 3200, 3500 ms (더 빠름)
            shotInterval = 2000 + (monsterId * 300);
        } else if (round == 7) {
            // 7라운드: 1500, 1700, 1900, 2100, 2300, 2500 ms (가장 빠름)
            shotInterval = 1500 + (monsterId * 200);
        } else {
            // 기본값 (1라운드와 동일)
            shotInterval = 3000 + (monsterId * 500);
        }

        // 초기 이동 속도 설정 (상하 이동 활성화)
        // 랜덤하게 위 또는 아래로 시작하여 자연스러운 움직임 생성
        boolean startMovingDown = Math.random() > 0.5;
        movingDown = startMovingDown;
        currentVelocityY = startMovingDown ? moveSpeed * 0.6 : -moveSpeed * 0.6;
        targetVelocityY = currentVelocityY;
        
        // 좌우 이동도 초기 속도 설정
        boolean startMovingRight = Math.random() > 0.5;
        movingRight = startMovingRight;
        currentVelocityX = startMovingRight ? moveSpeed : -moveSpeed;
        targetVelocityX = currentVelocityX;

        loadSprite();
    }

    private void loadSprite() {
        try {
            URL url = getClass().getClassLoader().getResource(spriteForRound(round));
            if (url != null) {
                nearImage = java.awt.Toolkit.getDefaultToolkit().createImage(url);
                if (nearImage != null && nearImage.getWidth(null) <= 0) {
                    nearImage = null;
                }
            }
        } catch (Exception ignored) {
            nearImage = null;
        }
        if (nearImage == null) {
            nearImage = SpriteStore.get().getSprite(spriteForRound(round)).getImage();
        }
    }

    private void updateTargetVelocity() {
        if (movingRight) {
            if (x >= 700) {
                movingRight = false;
                currentVelocityX = -moveSpeed;
            }
            targetVelocityX = moveSpeed;
        } else {
            if (x <= 100) {
                movingRight = true;
                currentVelocityX = moveSpeed;
            }
            targetVelocityX = -moveSpeed;
        }

        if (movingDown) {
            if (y >= 300) {
                movingDown = false;
                currentVelocityY = -moveSpeed * 0.6;
            }
            targetVelocityY = moveSpeed * 0.6;
        } else {
            if (y <= 60) {
                movingDown = true;
                currentVelocityY = moveSpeed * 0.6;
            }
            targetVelocityY = -moveSpeed * 0.6;
        }
    }

    private void updateVelocity(double deltaSeconds) {
        double acceleration = 200.0;
        double maxVelocityChange = acceleration * deltaSeconds;
        if (currentVelocityX < targetVelocityX) {
            currentVelocityX = Math.min(targetVelocityX, currentVelocityX + maxVelocityChange);
        } else if (currentVelocityX > targetVelocityX) {
            currentVelocityX = Math.max(targetVelocityX, currentVelocityX - maxVelocityChange);
        }

        if (currentVelocityY < targetVelocityY) {
            currentVelocityY = Math.min(targetVelocityY, currentVelocityY + maxVelocityChange);
        } else if (currentVelocityY > targetVelocityY) {
            currentVelocityY = Math.max(targetVelocityY, currentVelocityY - maxVelocityChange);
        }
    }

    private void updatePosition(double deltaSeconds) {
        x += currentVelocityX * deltaSeconds;
        y += currentVelocityY * deltaSeconds;
    }

    private void clampPosition() {
        x = Math.max(100, Math.min(700, x));
        y = Math.max(60, Math.min(300, y));
    }

    @Override
    public void move(long delta) {
        double deltaSeconds = delta / 1000.0;

        updateTargetVelocity();
        updateVelocity(deltaSeconds);
        updatePosition(deltaSeconds);
        clampPosition();

        preventOverlap();
        tryShoot();
    }

    private void preventOverlap() {
        for (Entity entity : environment.getEntities()) {
            if (entity == this || !(entity instanceof NearEntity)) {
                continue;
            }
            NearEntity other = (NearEntity) entity;
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double minDistance = NEAR_WIDTH;
            if (distance > 0 && distance < minDistance) {
                double push = (minDistance - distance) / 2.0;
                double nx = dx / distance;
                double ny = dy / distance;
                this.x += nx * push;
                this.y += ny * push * 0.2;
                other.x -= nx * push;
                other.y -= ny * push * 0.2;

                this.x = Math.max(100, Math.min(700, this.x));
                this.y = Math.max(60, Math.min(300, this.y));
                other.x = Math.max(100, Math.min(700, other.x));
                other.y = Math.max(60, Math.min(300, other.y));

                if (dx > 0) {
                    this.movingRight = true;
                    this.currentVelocityX = this.moveSpeed;
                    other.movingRight = false;
                    other.currentVelocityX = -other.moveSpeed;
                } else {
                    this.movingRight = false;
                    this.currentVelocityX = -this.moveSpeed;
                    other.movingRight = true;
                    other.currentVelocityX = other.moveSpeed;
                }

                if (dy > 0) {
                    this.movingDown = true;
                    this.currentVelocityY = this.moveSpeed * 0.6;
                    other.movingDown = false;
                    other.currentVelocityY = -other.moveSpeed * 0.6;
                } else {
                    this.movingDown = false;
                    this.currentVelocityY = -this.moveSpeed * 0.6;
                    other.movingDown = true;
                    other.currentVelocityY = other.moveSpeed * 0.6;
                }
            }
        }
    }

    private void tryShoot() {
        if (!environment.canEnemiesAttack()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastShot < shotInterval) {
            return;
        }

        String shotSprite = shotSpriteForRound(round);
        int spawnX = (int) Math.round(x);
        int spawnY = (int) Math.round(y) + (NEAR_HEIGHT / 2);

        ShotEntity shot = new ShotEntity(environment, shotSprite, spawnX, spawnY, true);
        shot.setVerticalMovement(300);
        shot.setNearMonsterShot(true, round, shotSprite);
        environment.addEntity(shot);
        lastShot = now;
    }

    public void takeDamage(int damage) {
        takeDamage(damage, null);
    }

    public void takeDamage(int damage, String killerPlayerId) {
        currentHP -= damage;
        if (currentHP <= 0) {
            die(killerPlayerId);
        }
    }

    private void die(String killerPlayerId) {
        environment.removeEntity(this);
        environment.onNearMonsterDestroyed(this, killerPlayerId, x, y);
    }

    @Override
    protected String snapshotMetadata() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("round", Integer.toString(round));
        map.put("monster", Integer.toString(monsterId));
        map.put("hp", Integer.toString(Math.max(0, currentHP)));
        map.put("max", Integer.toString(maxHP));
        return MetadataCodec.encode(map);
    }

    @Override
    protected void applySnapshotMetadata(String metadata) {
        Map<String, String> map = MetadataCodec.decode(metadata);
        if (!map.isEmpty()) {
            try {
                currentHP = Integer.parseInt(map.getOrDefault("hp", Integer.toString(maxHP)));
            } catch (NumberFormatException ignore) {
                currentHP = maxHP;
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int drawX = (int) x - NEAR_WIDTH / 2;
        int drawY = (int) y - NEAR_HEIGHT / 2;

        if (nearImage != null) {
            g2d.drawImage(nearImage, drawX, drawY, drawX + NEAR_WIDTH, drawY + NEAR_HEIGHT,
                    0, 0, nearImage.getWidth(null), nearImage.getHeight(null), null);
        } else {
            g2d.setColor(Color.ORANGE);
            g2d.fillRect(drawX, drawY, NEAR_WIDTH, NEAR_HEIGHT);
            g2d.setColor(Color.YELLOW);
            g2d.drawRect(drawX, drawY, NEAR_WIDTH, NEAR_HEIGHT);
        }

        int barWidth = 60;
        int barHeight = 6;
        int barX = (int) x - barWidth / 2;
        int barY = drawY - 15;

        g2d.setColor(Color.RED);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        double hpRatio = Math.max(0, (double) currentHP / maxHP);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(barX, barY, (int) (barWidth * hpRatio), barHeight);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }

    @Override
    public void collidedWith(Entity other) {
        // handled on shot entity side
    }

    @Override
    public Rectangle getBounds() {
        int topLeftX = (int) x - NEAR_WIDTH / 2;
        int topLeftY = (int) y - NEAR_HEIGHT / 2;
        return new Rectangle(topLeftX, topLeftY, NEAR_WIDTH, NEAR_HEIGHT);
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getRound() {
        return round;
    }

    public int getMonsterId() {
        return monsterId;
    }
}
