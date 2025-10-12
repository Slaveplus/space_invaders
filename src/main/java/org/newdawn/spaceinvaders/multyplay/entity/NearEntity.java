package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;
import org.newdawn.spaceinvaders.multyplay.sprite.SpriteStore;

/**
 * Near Entity – 보스전에 등장하는 중간 몬스터 구현.
 * 싱글 플레이어의 연출을 멀티플레이에 맞게 재구성한 버전이다.
 */
public class NearEntity extends Entity {

    private final MultiplayerGameContext game;
    private final int round;
    private final int monsterId;

    private Image nearImage;
    private int currentHP;
    private final int maxHP;
    private double moveSpeed = 50;
    private boolean movingRight = true;
    private long lastShot = 0;
    private final long shotInterval;
    private static final int NEAR_WIDTH = 120;
    private static final int NEAR_HEIGHT = 120;

    private static String spriteForRound(int round) {
        switch (round) {
            case 1: return "sprites/Boss/1near.png";
            case 3: return "sprites/Boss/2near.png";
            case 5: return "sprites/Boss/3near.png";
            case 7: return "sprites/Boss/4near.png";
            default: return "sprites/Boss/1near.png";
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
                return "sprites/Boss_Attack/ice ball.gif";
            case 3:
                return "sprites/shot.gif";
            case 5:
                return "sprites/shot.gif";
            case 7:
                return "sprites/Boss_Attack/5round1.gif";
            default:
                return "sprites/shot.gif";
        }
    }

    public NearEntity(MultiplayerGameContext game, int x, int y, int round, int monsterId) {
        super(spriteForRound(round), x, y);
        this.game = game;
        this.round = round;
        this.monsterId = monsterId;

        int bossRound = bossIndexFromNear(round);
        this.maxHP = 20 + (bossRound * 15);
        this.currentHP = maxHP;

        moveSpeed = 50 + (bossRound * 10);
        shotInterval = 3000 + (monsterId * 500);

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

    @Override
    public void move(long delta) {
        double distance = moveSpeed * delta / 1000.0;
        if (movingRight) {
            x += distance;
            if (x > 700) {
                movingRight = false;
            }
        } else {
            x -= distance;
            if (x < 100) {
                movingRight = true;
            }
        }

        preventOverlap();
        tryShoot();
    }

    private void preventOverlap() {
        for (Entity entity : game.getEntities()) {
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
                this.y += ny * push * 0.2; // subtle vertical adjustment
                other.x -= nx * push;
                other.y -= ny * push * 0.2;

                this.x = Math.max(60, Math.min(740, this.x));
                this.y = Math.max(60, Math.min(140, this.y));
                other.x = Math.max(60, Math.min(740, other.x));
                other.y = Math.max(60, Math.min(140, other.y));
            }
        }
    }

    private void tryShoot() {
        if (!game.canEnemiesAttack()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastShot < shotInterval) {
            return;
        }

        String shotPath = shotSpriteForRound(round);
        int spawnX = (int) Math.round(x);
        int spawnY = (int) Math.round(y) + (NEAR_HEIGHT / 2);

        ShotEntity shot = new ShotEntity(game, shotPath, spawnX, spawnY, true);
        shot.setVerticalMovement(300);
        shot.setNearMonsterShot(true);
        game.addEntity(shot);

        lastShot = now;
    }

    public void takeDamage(int damage, String killerPlayerId) {
        currentHP -= damage;
        if (currentHP <= 0) {
            die(killerPlayerId);
        }
    }

	private void die(String killerPlayerId) {
		game.removeEntity(this);
		game.onNearMonsterDestroyed(this, killerPlayerId, x, y);
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
        // 충돌 처리는 ShotEntity 쪽에서 담당한다.
    }

    @Override
    public java.awt.Rectangle getBounds() {
        int topLeftX = (int) x - NEAR_WIDTH / 2;
        int topLeftY = (int) y - NEAR_HEIGHT / 2;
        return new java.awt.Rectangle(topLeftX, topLeftY, NEAR_WIDTH, NEAR_HEIGHT);
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
