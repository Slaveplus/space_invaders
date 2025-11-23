package org.newdawn.spaceinvaders.common.entity.skill;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;

/**
 * 스킬 드롭 공통 구현.
 */
public abstract class BaseSkillEntity extends Entity {
    protected final SkillEnvironment environment;
    protected boolean used;
    protected final int skillType;
    protected final int skillValue;
    protected double moveSpeed = 140;
    protected BufferedImage skillIcon;

    protected BaseSkillEntity(SkillEnvironment environment, String sprite, int x, int y, int skillType, int skillValue) {
        super(sprite, x, y);
        this.environment = environment;
        this.skillType = skillType;
        this.skillValue = skillValue;
        loadSkillIcon();
        dx = 0;
        dy = moveSpeed;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (y > 700) {
            environment.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (used) {
            return;
        }
        if (other instanceof ShipEntity) {
            environment.removeEntity(this);
            environment.grantSkill(((ShipEntity) other).getOwnerId(), skillType, skillValue);
            used = true;
        }
    }

    public int getSkillType() {
        return skillType;
    }

    public int getSkillValue() {
        return skillValue;
    }

    public boolean isUsed() {
        return used;
    }

    protected void loadSkillIcon() {
        String iconPath = iconForSkill(skillType);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(iconPath)) {
            if (is != null) {
                skillIcon = ImageIO.read(is);
            }
        } catch (IOException e) {
            org.newdawn.spaceinvaders.common.util.Logger logger = 
                org.newdawn.spaceinvaders.common.util.LoggerFactory.getLogger(BaseSkillEntity.class);
            logger.error("Failed to load skill icon: " + iconPath, e);
        }
    }

    protected String iconForSkill(int skillType) {
        switch (skillType) {
            case 0:
                return SpriteConstants.SKILL_ICON_1_45_PNG;
            case 2:
                return SpriteConstants.SKILL_ICON_7_11_PNG;
            case 3:
                return SpriteConstants.SKILL_MISSILE_PNG;
            default:
                return SpriteConstants.SKILL_ICON_1_45_PNG;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (skillIcon != null) {
            g2d.drawImage(skillIcon, (int) x, (int) y, null);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect((int) x, (int) y, 32, 32);
            g2d.setColor(Color.GRAY);
            g2d.drawRect((int) x, (int) y, 32, 32);
            g2d.setColor(Color.WHITE);
            g2d.drawString("S" + skillType, (int) x + 8, (int) y + 20);
        }
    }
}
