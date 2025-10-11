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
            }
        } catch (Exception ignored) {
            nearImage = null;
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
            double minDistance = 120; // approximate size
            if (distance > 0 && distance < minDistance) {
                double push = (minDistance - distance) / 2.0;
                double nx = dx / distance;
                double ny = dy / distance;
                this.x += nx * push;
                this.y += ny * push * 0.2; // subtle vertical adjustment
                other.x -= nx * push;
                other.y -= ny * push * 0.2;

                this.x = Math.max(60, Math.min(740, this.x));
                this.y = Math.max(80, Math.min(220, this.y));
                other.x = Math.max(60, Math.min(740, other.x));
                other.y = Math.max(80, Math.min(220, other.y));
            }
        }
    }

    private void tryShoot() {
        long now = System.currentTimeMillis();
        if (now - lastShot < shotInterval) {
            return;
        }

        // Near 몬스터는 플레이어를 향해 직선으로 에너지 구체를 쏜다.
        ShotEntity shot = new ShotEntity(game, "sprites/shot.gif", (int) x, (int) y + 40, true);
        shot.setVerticalMovement(300);
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
        int width = 120;
        int height = 120;
        int drawX = (int) x - width / 2;
        int drawY = (int) y - height / 2;

        if (nearImage != null) {
            g2d.drawImage(nearImage, drawX, drawY, drawX + width, drawY + height,
                    0, 0, nearImage.getWidth(null), nearImage.getHeight(null), null);
        } else {
            g2d.setColor(Color.ORANGE);
            g2d.fillRect(drawX, drawY, width, height);
            g2d.setColor(Color.YELLOW);
            g2d.drawRect(drawX, drawY, width, height);
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
        int width = 120;
        int height = 120;
        int topLeftX = (int) x - width / 2;
        int topLeftY = (int) y - height / 2;
        return new java.awt.Rectangle(topLeftX, topLeftY, width, height);
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
