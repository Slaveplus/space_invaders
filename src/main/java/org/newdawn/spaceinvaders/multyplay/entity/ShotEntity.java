package org.newdawn.spaceinvaders.multyplay.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

/**
 * Multiplayer variant of the player/alien shot entity that mirrors single-player behaviour
 * while retaining network metadata and multiplayer specific flags.
 */
public class ShotEntity extends Entity {
	private static final double PLAYER_SHOT_SPEED = -300;
	private static final double ALIEN_SHOT_SPEED = 300;
	private static final double SKILL_DROP_SPEED = 100;

	private final MultiplayerGameContext game;
	private boolean used;
	private boolean isAlienShot;
	private boolean isSkillDrop;
	private int skillType = -1;
	private int skillValue;
	private boolean hasPiercing;
	private boolean nearMonsterShot;

	public ShotEntity(MultiplayerGameContext game, String sprite, int x, int y) {
		super(sprite, x, y);
		this.game = game;
		this.dy = PLAYER_SHOT_SPEED;
	}

	public ShotEntity(MultiplayerGameContext game, String sprite, int x, int y, boolean isAlienShot) {
		super(sprite, x, y);
		this.game = game;
		this.isAlienShot = isAlienShot;
		this.dy = isAlienShot ? ALIEN_SHOT_SPEED : PLAYER_SHOT_SPEED;
	}

	public ShotEntity(MultiplayerGameContext game, String sprite, int x, int y,
					  boolean isAlienShot, boolean hasPiercing) {
		this(game, sprite, x, y, isAlienShot);
		this.hasPiercing = hasPiercing;
	}

	public ShotEntity(MultiplayerGameContext game, String sprite, int x, int y,
					  boolean isAlienShot, int skillType, int skillValue) {
		super(sprite, x, y);
		this.game = game;
		this.isAlienShot = false;
		this.isSkillDrop = true;
		this.skillType = skillType;
		this.skillValue = skillValue;
		this.dx = 0;
		this.dy = SKILL_DROP_SPEED;
	}

	@Override
	public void move(long delta) {
		super.move(delta);

		if (isSkillDrop) {
			if (y > 700) {
				game.removeEntity(this);
			}
			return;
		}

		if (isAlienShot) {
			if (y > 700 || y < -100) {
				game.removeEntity(this);
			}
		} else if (y < -100 || y > 700) {
			game.removeEntity(this);
		}
	}

	@Override
	public void draw(Graphics g) {
		if (isSkillDrop) {
			drawSkillDrop(g);
			return;
		}
		if (hasPiercing) {
			g.setColor(Color.YELLOW);
			g.fillRect((int) Math.round(x) - 2, (int) Math.round(y) - 10, 4, 20);
			return;
		}
		if (nearMonsterShot) {
			drawScaledSprite(g, 50);
			return;
		}
		super.draw(g);
	}

	@Override
	public void collidedWith(Entity other) {
		if (used) {
			return;
		}

		if (isSkillDrop) {
			if (other instanceof ShipEntity) {
				ShipEntity ship = (ShipEntity) other;
				game.removeEntity(this);
				game.addSkillToInventory(ship.getOwnerId(), skillType, skillValue);
				used = true;
			}
			return;
		}

		if (isAlienShot) {
			if (other instanceof ShipEntity) {
				ShipEntity ship = (ShipEntity) other;
				game.removeEntity(this);
				game.notifyPlayerDamaged(ship.getOwnerId(), 1);
				used = true;
			}
			return;
		}

		if (other instanceof ShotEntity && !((ShotEntity) other).isAlienShot()) {
			return;
		}
		if (other instanceof ShipEntity) {
			return;
		}

		if (other instanceof AlienEntity) {
			handleAlienHit((AlienEntity) other);
			return;
		}
		if (other instanceof NearEntity) {
			handleNearHit((NearEntity) other);
			return;
		}
		if (other instanceof BossEntity) {
			handleBossHit((BossEntity) other);
		}
	}

	private void handleAlienHit(AlienEntity alien) {
		String ownerId = getOwnerId();
		int damage = game.getPlayerAttackPower(ownerId);
		alien.takeDamage(damage);
		if (alien.getCurrentHP() <= 0) {
			game.notifyAlienKilled(ownerId, alien.getX(), alien.getY());
		}
		spawnHeatEffect(alien.getX(), alien.getY());
		if (!hasPiercing) {
			game.removeEntity(this);
			used = true;
		}
	}

	private void handleNearHit(NearEntity near) {
		String ownerId = getOwnerId();
		int damage = game.getPlayerAttackPower(ownerId);
		near.takeDamage(damage, ownerId);
		spawnHeatEffect(near.getX(), near.getY());
		game.removeEntity(this);
		used = true;
	}

	private void handleBossHit(BossEntity boss) {
		String ownerId = getOwnerId();
		int damage = game.getPlayerAttackPower(ownerId);
		boss.takeDamage(damage, ownerId);
		spawnHeatEffect((int) Math.round(x), (int) Math.round(y));
		if (!hasPiercing) {
			game.removeEntity(this);
			used = true;
		}
	}

	private void spawnHeatEffect(int x, int y) {
		HeatEffectEntity effect = new HeatEffectEntity(game, x, y);
		game.addEntity(effect);
	}

	public boolean hasPiercing() {
		return hasPiercing;
	}

	public void setPiercing(boolean hasPiercing) {
		this.hasPiercing = hasPiercing;
	}

	public boolean isAlienShot() {
		return isAlienShot;
	}

	public void setNearMonsterShot(boolean nearMonsterShot) {
		this.nearMonsterShot = nearMonsterShot;
	}

	@Override
	public Rectangle getBounds() {
		if (nearMonsterShot) {
			int hitboxSize = 20;
			return new Rectangle((int) Math.round(x) - hitboxSize / 2,
					(int) Math.round(y) - hitboxSize / 2,
					hitboxSize,
					hitboxSize);
		}
		return super.getBounds();
	}

	@Override
	protected String snapshotMetadata() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("alien", isAlienShot ? "1" : "0");
		map.put("skill", isSkillDrop ? "1" : "0");
		map.put("skillType", Integer.toString(skillType));
		map.put("skillValue", Integer.toString(skillValue));
		map.put("pierce", hasPiercing ? "1" : "0");
		map.put("near", nearMonsterShot ? "1" : "0");
		return MetadataCodec.encode(map);
	}

	@Override
	protected void applySnapshotMetadata(String metadata) {
		Map<String, String> map = MetadataCodec.decode(metadata);
		if (map.isEmpty()) {
			return;
		}
		isAlienShot = "1".equals(map.get("alien"));
		isSkillDrop = "1".equals(map.get("skill"));
		try {
			skillType = Integer.parseInt(map.getOrDefault("skillType", "-1"));
		} catch (NumberFormatException ignore) {
			skillType = -1;
		}
		try {
			skillValue = Integer.parseInt(map.getOrDefault("skillValue", "0"));
		} catch (NumberFormatException ignore) {
			skillValue = 0;
		}
		hasPiercing = "1".equals(map.get("pierce"));
		nearMonsterShot = "1".equals(map.get("near"));
	}

	private BufferedImage loadSkillImage(int skillType) {
		String imagePath;
		switch (skillType) {
			case 0:
				imagePath = "sprites/Skill/1.png";
				break;
			case 1:
				imagePath = "sprites/Skill/2.png";
				break;
			case 2:
				imagePath = "sprites/Skill/3.png";
				break;
			case 3:
				imagePath = "sprites/Skill/4.png";
				break;
			default:
				imagePath = "sprites/Skill/1.png";
				break;
		}

		try (InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath)) {
			if (is != null) {
				return ImageIO.read(is);
			}
		} catch (Exception e) {
			// ignore and fall back
		}
		return null;
	}

	private void drawSkillDrop(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		int itemSize = 24;
		int drawX = (int) Math.round(x) - itemSize / 2;
		int drawY = (int) Math.round(y) - itemSize / 2;

		BufferedImage skillImage = loadSkillImage(skillType);
		if (skillImage != null) {
			g2d.setColor(new Color(0, 0, 0, 200));
			g2d.fillRect(drawX + 2, drawY + 2, itemSize, itemSize);
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(drawX + 1, drawY + 1, itemSize, itemSize);
			g2d.drawImage(skillImage, drawX, drawY, itemSize, itemSize, null);
			g2d.setColor(new Color(255, 255, 255, 200));
			g2d.setStroke(new BasicStroke(2));
			g2d.drawRect(drawX, drawY, itemSize, itemSize);

			int circleX = drawX + itemSize - 4;
			int circleY = drawY + itemSize - 4;
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
			g2d.fillOval(drawX, drawY, itemSize, itemSize);
			g2d.setColor(Color.WHITE);
			g2d.drawOval(drawX, drawY, itemSize, itemSize);
		}
	}

	private void drawScaledSprite(Graphics g, int size) {
		if (sprite == null) {
			super.draw(g);
			return;
		}
		Graphics2D g2d = (Graphics2D) g;
		int drawX = (int) Math.round(x) - size / 2;
		int drawY = (int) Math.round(y) - size / 2;
		g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + size, drawY + size,
				0, 0, sprite.getWidth(), sprite.getHeight(), null);
	}
}
