package org.newdawn.spaceinvaders.gameplay.entity.entity_attack;

import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Round 4 Boss Heal Attack - 4round.gif를 사용한 체력 회복 공격
 * 맵 중앙에 생성되어 보스의 체력을 풀피로 회복시킴
 */
public class Round4HealAttack extends Entity {
    /** The game in which this entity exists */
    private Game game;
    /** The attack image */
    private java.awt.Image healImage;
    /** Duration of the heal attack */
    private long healDuration = 3000; // 3초간 유지
    /** Start time */
    private long startTime;
    /** Heal attack size - 매우 큰 회복 공격 */
    private int healWidth = 400;
    private int healHeight = 400;

    /**
     * Create a new heal attack entity
     * 
     * @param game The game in which this entity exists
     * @param x The initial x location of this entity
     * @param y The initial y location of this entity
     */
    public Round4HealAttack(Game game, int x, int y) {
        super("sprites/Boss_Attack/4round.gif", x, y);
        this.game = game;
        this.startTime = System.currentTimeMillis();
        
        // 회복 공격은 정지 상태로 시작 (맵 중앙에 고정)
        this.dx = 0;
        this.dy = 0;
        
        // Load the heal image
        loadHealImage();
    }

    /**
     * Load the heal image
     */
    private void loadHealImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource("sprites/Boss_Attack/4round.gif");
            if (imageUrl != null) {
                healImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("🟣 Successfully loaded heal attack GIF: 4round.gif via ClassLoader.getResource()");
            } else {
                System.err.println("🟣 Failed to find 4round.gif resource.");
                healImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/sprites/Boss_Attack/4round.gif");
                System.out.println("🟣 Attempted direct loading of 4round.gif: " + (healImage != null));
            }
            // Ensure image is loaded before proceeding
            if (healImage != null) {
                java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
                mt.addImage(healImage, 0);
                mt.waitForAll();
                if (mt.isErrorAny()) {
                    System.err.println("🟣 Error loading 4round.gif with MediaTracker.");
                    healImage = null;
                } else {
                    System.out.println("🟣 Heal attack image dimensions (after MediaTracker): " + healImage.getWidth(null) + "x" + healImage.getHeight(null));
                }
            }
        } catch (Exception e) {
            System.err.println("🟣 Failed to load heal attack GIF image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Request that this entity move itself based on a certain amount
     * of time having passed.
     * 
     * @param delta The amount of time that has passed in milliseconds
     */
    public void move(long delta) {
        // 회복 공격은 움직이지 않음 (맵 중앙에 고정)
        
        // Check if heal duration has expired
        if (System.currentTimeMillis() - startTime > healDuration) {
            System.out.println("🟣 Heal attack completed, healing boss to full HP!");
            healBossToFull();
            game.removeEntity(this);
            return;
        }
    }

    /**
     * Heal boss to full HP
     */
    private void healBossToFull() {
        try {
            // Find the boss entity and heal it to full HP
            for (Entity entity : game.getEntities()) {
                if (entity instanceof org.newdawn.spaceinvaders.gameplay.entity.BossEntity) {
                    org.newdawn.spaceinvaders.gameplay.entity.BossEntity boss = (org.newdawn.spaceinvaders.gameplay.entity.BossEntity) entity;
                    
                    // Use reflection to call healToFull method or set HP directly
                    try {
                        java.lang.reflect.Method healMethod = boss.getClass().getMethod("healToFull");
                        healMethod.invoke(boss);
                        System.out.println("🟣 Boss healed to full HP using healToFull method!");
                    } catch (NoSuchMethodException e) {
                        // If healToFull method doesn't exist, try to set HP directly
                        try {
                            java.lang.reflect.Field hpField = boss.getClass().getDeclaredField("currentHP");
                            hpField.setAccessible(true);
                            java.lang.reflect.Field maxHpField = boss.getClass().getDeclaredField("maxHP");
                            maxHpField.setAccessible(true);
                            
                            int maxHP = (Integer) maxHpField.get(boss);
                            hpField.set(boss, maxHP);
                            System.out.println("🟣 Boss healed to full HP: " + maxHP + " HP!");
                        } catch (Exception ex) {
                            System.err.println("🟣 Failed to heal boss: " + ex.getMessage());
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("🟣 Error healing boss: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Draw this entity to the graphics context provided
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            
            if (healImage != null) {
                int drawX = (int)x - healWidth / 2;
                int drawY = (int)y - healHeight / 2;
                
                g2d.drawImage(healImage, drawX, drawY, drawX + healWidth, drawY + healHeight, 
                             0, 0, healImage.getWidth(null), healImage.getHeight(null), null);
                
                System.out.println("🟣 Drawing MASSIVE heal attack at X:" + drawX + " Y:" + drawY + " Size:" + healWidth + "x" + healHeight);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.GREEN);
                g2d.fillRect((int)x - healWidth / 2, (int)y - healHeight / 2, healWidth, healHeight);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int)x - healWidth / 2, (int)y - healHeight / 2, healWidth, healHeight);
                System.out.println("🟣 Drawing MASSIVE heal attack fallback at X:" + (int)x + " Y:" + (int)y + " Size:" + healWidth + "x" + healHeight);
            }
        } catch (Exception e) {
            System.err.println("Error drawing heal attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this entity collided with another
     * 
     * @param other The entity with which we collided
     */
    public void collidedWith(Entity other) {
        // 회복 공격은 플레이어에게 데미지를 주지 않음
        // 단순히 보스를 회복시키는 용도
    }
}
