package org.newdawn.spaceinvaders.common.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;

import org.newdawn.spaceinvaders.common.GameContext;

/**
 * 싱글/멀티 공용 탄환 엔티티.
 */
public class ShotEntity extends Entity {
    private static final double PLAYER_SHOT_SPEED = -300;
    private static final double ALIEN_SHOT_SPEED = 300;
    private static final double SKILL_DROP_SPEED = 100;

    protected final GameContext game;
    protected boolean used;
    protected boolean alienShot;
    protected boolean skillDrop;
    protected boolean nearMonsterShot;
    protected boolean piercingShot;
    protected int skillType = -1;
    protected int skillValue;
    protected int nearMonsterRound = -1;
    protected String spritePath;

    public ShotEntity(GameContext game, String sprite, int x, int y) {
        super(sprite, x, y);
        this.game = game;
        this.dy = PLAYER_SHOT_SPEED;
        this.spritePath = sprite;
    }

    public ShotEntity(GameContext game, String sprite, int x, int y, boolean alienShot) {
        super(sprite, x, y);
        this.game = game;
        this.alienShot = alienShot;
        this.dy = alienShot ? ALIEN_SHOT_SPEED : PLAYER_SHOT_SPEED;
        this.spritePath = sprite;
    }

    public ShotEntity(GameContext game, String sprite, int x, int y, int skillType, int skillValue) {
        super(sprite, x, y);
        this.game = game;
        this.skillDrop = true;
        this.skillType = skillType;
        this.skillValue = skillValue;
        this.dy = SKILL_DROP_SPEED;
        this.spritePath = sprite;
    }

    public void setPiercingShot(boolean piercingShot) {
        this.piercingShot = piercingShot;
    }

    public void setNearMonsterShot(boolean nearMonsterShot, int round, String spritePath) {
        this.nearMonsterShot = nearMonsterShot;
        this.nearMonsterRound = round;
        this.spritePath = spritePath;
    }

    @Override
    public void move(long delta) {
        super.move(delta);

        if (skillDrop) {
            if (y > 700) {
                game.removeEntity(this);
            }
            return;
        }

        if (alienShot) {
            if (y > 700 || y < -100) {
                game.removeEntity(this);
            }
        } else if (y < -100 || y > 700) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (skillDrop) {
            drawSkillDrop(g);
            return;
        }
        if (piercingShot) {
            g.setColor(Color.YELLOW);
            g.fillRect((int) Math.round(x) - 2, (int) Math.round(y) - 10, 4, 20);
            return;
        }
        if (nearMonsterShot) {
            drawNearMonsterShot(g);
            return;
        }
        super.draw(g);
    }

    private void drawSkillDrop(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int size = 24;
        int drawX = (int) Math.round(x) - size / 2;
        int drawY = (int) Math.round(y) - size / 2;

        BufferedImage skillImage = loadSkillImageForDrop(skillType);
        if (skillImage != null) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(drawX + 2, drawY + 2, size, size);
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(drawX + 1, drawY + 1, size, size);
            g2d.drawImage(skillImage, drawX, drawY, size, size, null);
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(drawX, drawY, size, size);

            int circleX = drawX + size - 4;
            int circleY = drawY + size - 4;
            int radius = 6;
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillOval(circleX - radius, circleY - radius, radius * 2, radius * 2);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            String text = "1";
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            int textHeight = g2d.getFontMetrics().getHeight();
            g2d.drawString(text, circleX - textWidth / 2, circleY + textHeight / 4);
        } else {
            g2d.setColor(Color.CYAN);
            g2d.fillOval(drawX, drawY, size, size);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(drawX, drawY, size, size);
        }
    }

    private void drawNearMonsterShot(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if ("sprites/Skill/Heat.gif".equals(spritePath)) {
            int sphereSize = 20;
            g2d.setColor(Color.GREEN);
            g2d.fillOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.fillOval((int) x - sphereSize / 4, (int) y - sphereSize / 4, sphereSize / 2, sphereSize / 2);
            return;
        }
        if ("sprites/shot.gif".equals(spritePath) || (spritePath != null && spritePath.endsWith("/shot.gif"))) {
            int sphereSize = 20;
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawOval((int) x - sphereSize / 2, (int) y - sphereSize / 2, sphereSize, sphereSize);
            return;
        }
        if ("sprites/Boss_Attack/2round1.gif".equals(spritePath)) {
            sprite.draw(g, (int) x - 75, (int) y - 75, 150, 150);
            return;
        }
        if ("sprites/Boss_Attack/ice ball.gif".equals(spritePath)) {
            sprite.draw(g, (int) x - 50, (int) y - 50, 100, 100);
            return;
        }
        sprite.draw(g, (int) x - 10, (int) y - 10, 20, 20);
    }

    private BufferedImage loadSkillImageForDrop(int type) {
        String path;
        switch (type) {
            case 0:
                path = "sprites/Skill/1.png";
                break;
            case 2:
                path = "sprites/Skill/3.png";
                break;
            case 3:
                path = "sprites/Skill/4.png";
                break;
            default:
                path = "sprites/Skill/1.png";
        }
        try {
            return javax.imageio.ImageIO.read(Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(path)));
        } catch (Exception e) {
            System.err.println("Failed to load skill image: " + path);
            return null;
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (used) {
            return;
        }
        if (skillDrop) {
            handleSkillDropCollision(other);
            return;
        }
        if (alienShot) {
            handleAlienShotCollision(other);
            return;
        }

        if (other instanceof ShotEntity && !((ShotEntity) other).alienShot) {
            return;
        }
        if (other instanceof ShipEntity) {
            return;
        }

        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.AlienEntity
                || other instanceof org.newdawn.spaceinvaders.multyplay.entity.AlienEntity) {
            handleAlienHit(other);
            return;
        }
        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.NearEntity
                || other instanceof org.newdawn.spaceinvaders.multyplay.entity.NearEntity) {
            handleNearHit(other);
            return;
        }
        if (other instanceof org.newdawn.spaceinvaders.gameplay.entity.BossEntity
                || other instanceof org.newdawn.spaceinvaders.multyplay.entity.BossEntity) {
            handleBossHit(other);
        }
    }

    protected void handleSkillDropCollision(Entity other) {
        if (!(other instanceof ShipEntity)) {
            return;
        }
        ShipEntity ship = (ShipEntity) other;
        String ownerId = ship.getOwnerId();
        game.addSkillToInventory(ownerId, skillType, skillValue);
        game.removeEntity(this);
        used = true;
    }

    protected void handleAlienShotCollision(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            game.removeEntity(this);
            game.notifyPlayerDamaged(ship.getOwnerId(), 1);
            used = true;
        }
    }

    protected void handleAlienHit(Entity alien) {
        String ownerId = getOwnerId();
        int damage = game.getPlayerAttackPower(ownerId);
        tryInvokeTakeDamage(alien, damage);
        spawnHeatEffect(alien.getX(), alien.getY());
        if (!piercingShot) {
            game.removeEntity(this);
            used = true;
        }
    }

    protected void handleNearHit(Entity near) {
        if (nearMonsterShot) {
            return;
        }
        tryInvokeTakeDamage(near, game.getPlayerAttackPower(getOwnerId()));
        spawnHeatEffect(near.getX(), near.getY());
        if (!piercingShot) {
            game.removeEntity(this);
            used = true;
        }
    }

    protected void handleBossHit(Entity boss) {
        tryInvokeTakeDamage(boss, game.getPlayerAttackPower(getOwnerId()));
        spawnHeatEffect(boss.getX(), boss.getY());
        if (!piercingShot) {
            game.removeEntity(this);
            used = true;
        }
    }

    protected void tryInvokeTakeDamage(Entity entity, int damage) {
        try {
            java.lang.reflect.Method takeDamage = entity.getClass().getMethod("takeDamage", int.class);
            takeDamage.invoke(entity, damage);
        } catch (Exception ignored) {
        }
    }

    protected void spawnHeatEffect(double x, double y) {
        if (game != null) {
            game.createHeatEffect((int) x, (int) y, 60.0);
        }
    }

    public boolean isAlienShot() {
        return alienShot;
    }

    public boolean isSkillDrop() {
        return skillDrop;
    }

    public void setSpritePath(String spritePath) {
        this.spritePath = spritePath;
    }

}
