package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.gameplay.Game;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Near Entity - 보스 전에 나타나는 중간 보스 몬스터
 * 1near.png, 2near.png, 3near.png, 4near.png를 사용
 */
public class NearEntity extends Entity {
    /** 이 엔티티가 존재하는 게임 */
    private Game game;
    /** 근거리 몬스터 이미지 */
    private java.awt.Image nearImage;
    /** 현재 HP */
    private int currentHP;
    /** 최대 HP */
    private int maxHP;
    /** 이동 속도 */
    private double moveSpeed = 50;
    /** 좌우 이동 방향 */
    private boolean movingRight = true;
    /** 상하 이동 방향 */
    private boolean movingDown = true;
    /** 현재 실제 X 이동 속도 (부드러운 가속/감속용) */
    private double currentVelocityX = 0;
    /** 현재 실제 Y 이동 속도 (부드러운 가속/감속용) */
    private double currentVelocityY = 0;
    /** 목표 X 속도 */
    private double targetVelocityX = 0;
    /** 목표 Y 속도 */
    private double targetVelocityY = 0;
    /** 근거리 몬스터 라운드 번호 */
    private int round;
    /** 근거리 몬스터 크기 */
    private int nearWidth = 120;
    private int nearHeight = 120;
    /** 마지막 발사 이후의 시간 */
    private long lastShot = 0;
    /** 발사 간격 */
    private long shotInterval = 2000; // 2초마다 공격
    /** 고유 타이밍을 위한 몬스터 ID */
    private int monsterId;

    /**
     * 라운드 번호를 기반으로 근거리 스프라이트 경로를 가져옵니다
     * 
     * @param round 라운드 번호 (1,3,5,7 = 보스 라운드 1,2,3,4 전의 근거리 라운드)
     * @return 근거리 몬스터의 스프라이트 경로
     */
    private static String getNearSpriteForRound(int round) {
        switch (round) {
            case 1:
                return "sprites/Boss/1near.png"; // 1라운드 보스 전
            case 3:
                return "sprites/Boss/2near.png"; // 2라운드 보스 전
            case 5:
                return "sprites/Boss/3near.png"; // 3라운드 보스 전
            case 7:
                return "sprites/Boss/4near.png"; // 4라운드 보스 전
            default:
                return "sprites/Boss/1near.png";
        }
    }

    /**
     * Get the boss round number from near round number
     * 
     * @param nearRound The near round number (1,3,5,7)
     * @return The corresponding boss round number (1,2,3,4)
     */
    private static int getBossRoundFromNearRound(int nearRound) {
        switch (nearRound) {
            case 1:
                return 1; // 1라운드 보스 전
            case 3:
                return 2; // 2라운드 보스 전
            case 5:
                return 3; // 3라운드 보스 전
            case 7:
                return 4; // 4라운드 보스 전
            default:
                return 1;
        }
    }

    /**
     * Create a new near entity
     * 
     * @param game The game in which the near monster has been created
     * @param x The initial x location of the near monster
     * @param y The initial y location of the near monster
     * @param round The round number for scaling
     * @param monsterId Unique ID for this monster (0-5)
     */
    public NearEntity(Game game, int x, int y, int round, int monsterId) {
        super(getNearSpriteForRound(round), x, y);
        
        this.game = game;
        this.round = round;
        this.monsterId = monsterId;
        
        // Scale near monster stats based on boss round (1,2,3,4)
        int bossRound = getBossRoundFromNearRound(round);
        maxHP = 20 + (bossRound * 15); // 35, 50, 65, 80
        currentHP = maxHP;
        
        // Scale movement speed
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
        
        // Load the near image
        loadNearImage();
        
        System.out.println("Near Monster " + monsterId + " spawned! Round " + round + ", HP: " + currentHP + "/" + maxHP + ", Attack Interval: " + shotInterval + "ms");
    }

    /**
     * Load the near image
     */
    private void loadNearImage() {
        try {
            java.net.URL imageUrl = getClass().getClassLoader().getResource(getNearSpriteForRound(round));
            if (imageUrl != null) {
                nearImage = java.awt.Toolkit.getDefaultToolkit().createImage(imageUrl);
                System.out.println("✅ Successfully loaded near monster image: " + getNearSpriteForRound(round));
            } else {
                System.err.println("❌ Failed to find near monster image: " + getNearSpriteForRound(round));
                nearImage = java.awt.Toolkit.getDefaultToolkit().createImage("src/main/resources/" + getNearSpriteForRound(round));
                System.out.println("✅ Attempted direct loading: " + (nearImage != null));
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load near monster image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Request that this near monster moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        try {
            double deltaSeconds = delta / 1000.0;
            double acceleration = 200.0; // 가속도 (픽셀/초²)
            
            // 목표 속도 설정 (좌우) - 벽 끝에 부딪히면 즉시 방향 변경
            if (movingRight) {
                if (x >= 700) {
                    movingRight = false;
                    currentVelocityX = -moveSpeed; // 즉시 반대 방향으로 속도 설정
                }
                targetVelocityX = moveSpeed;
            } else {
                if (x <= 100) {
                    movingRight = true;
                    currentVelocityX = moveSpeed; // 즉시 반대 방향으로 속도 설정
                }
                targetVelocityX = -moveSpeed;
            }
            
            // 목표 속도 설정 (상하) - 맵 중간까지만 (60 ~ 300), 벽 끝에 부딪히면 즉시 방향 변경
            if (movingDown) {
                if (y >= 300) {
                    movingDown = false;
                    currentVelocityY = -moveSpeed * 0.6; // 즉시 반대 방향으로 속도 설정
                }
                targetVelocityY = moveSpeed * 0.6; // 상하 이동은 좌우보다 약간 느리게
            } else {
                if (y <= 60) {
                    movingDown = true;
                    currentVelocityY = moveSpeed * 0.6; // 즉시 반대 방향으로 속도 설정
                }
                targetVelocityY = -moveSpeed * 0.6;
            }
            
            // 부드러운 가속/감속 적용
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
            
            // 위치 업데이트
            x += currentVelocityX * deltaSeconds;
            y += currentVelocityY * deltaSeconds;
            
            // 경계 체크 및 제한
            x = Math.max(100, Math.min(700, x));
            y = Math.max(60, Math.min(300, y)); // 맵 중간까지만
            
            // Check collision with other near monsters
            checkNearMonsterCollision();
            
            // Try to shoot
            tryShoot();
            
        } catch (Exception e) {
            System.err.println("Error in near monster move: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check collision with other near monsters
     */
    private void checkNearMonsterCollision() {
        try {
            for (Entity entity : game.getGameStateManager().getEntities()) {
                if (entity != this && entity.getClass().getSimpleName().equals("NearEntity")) {
                    NearEntity otherNear = (NearEntity) entity;
                    
                    // Calculate distance between monsters
                    double dx = this.x - otherNear.x;
                    double dy = this.y - otherNear.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    // Collision detection (monster size is 120x120, so radius is 60)
                    double collisionDistance = 120; // Monster width/height
                    
                    if (distance < collisionDistance) {
                        // Push monsters apart
                        double pushForce = (collisionDistance - distance) / 2;
                        double pushX = (dx / distance) * pushForce;
                        double pushY = (dy / distance) * pushForce;
                        
                        // Apply push force
                        this.x += pushX;
                        this.y += pushY;
                        otherNear.x -= pushX;
                        otherNear.y -= pushY;
                        
                        // Keep monsters within screen bounds
                        this.x = Math.max(100, Math.min(700, this.x));
                        this.y = Math.max(60, Math.min(300, this.y)); // 맵 중간까지만
                        otherNear.x = Math.max(100, Math.min(700, otherNear.x));
                        otherNear.y = Math.max(60, Math.min(300, otherNear.y)); // 맵 중간까지만
                        
                        // 충돌 시 상대 위치를 보고 적절한 방향으로 변경
                        // X축 방향 변경: 상대가 왼쪽에 있으면 오른쪽으로, 오른쪽에 있으면 왼쪽으로
                        if (dx > 0) {
                            // 상대가 왼쪽에 있음 -> 오른쪽으로 이동
                            this.movingRight = true;
                            this.currentVelocityX = this.moveSpeed;
                        } else {
                            // 상대가 오른쪽에 있음 -> 왼쪽으로 이동
                            this.movingRight = false;
                            this.currentVelocityX = -this.moveSpeed;
                        }
                        
                        // Y축 방향 변경: 상대가 위에 있으면 아래로, 아래에 있으면 위로
                        if (dy > 0) {
                            // 상대가 위에 있음 -> 아래로 이동
                            this.movingDown = true;
                            this.currentVelocityY = this.moveSpeed * 0.6;
                        } else {
                            // 상대가 아래에 있음 -> 위로 이동
                            this.movingDown = false;
                            this.currentVelocityY = -this.moveSpeed * 0.6;
                        }
                        
                        // 다른 몬스터도 반대 방향으로 변경
                        if (dx > 0) {
                            otherNear.movingRight = false;
                            otherNear.currentVelocityX = -otherNear.moveSpeed;
                        } else {
                            otherNear.movingRight = true;
                            otherNear.currentVelocityX = otherNear.moveSpeed;
                        }
                        
                        if (dy > 0) {
                            otherNear.movingDown = false;
                            otherNear.currentVelocityY = -otherNear.moveSpeed * 0.6;
                        } else {
                            otherNear.movingDown = true;
                            otherNear.currentVelocityY = otherNear.moveSpeed * 0.6;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in near monster collision check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Try to shoot at player
     */
    private void tryShoot() {
        try {
            // Check if 3 seconds have passed since game start
            if (!game.canEnemiesAttack()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last shot
            if (currentTime - lastShot < shotInterval) {
                return;
            }
            
            // Create shot moving towards player with round-specific sprite
            String shotSprite = getShotSpriteForRound(round);
            ShotEntity shot = new ShotEntity(game, shotSprite, (int)x, (int)y + nearHeight/2, true); // isAlienShot = true
            shot.setVerticalMovement(300); // Move down at 300 pixels/sec
            shot.setNearMonsterShot(true); // Mark as near monster shot for smaller size
            shot.setNearMonsterRound(round); // 라운드 정보 전달
            
            game.addEntity(shot);
            
            // Update last shot time
            lastShot = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in near monster shoot: " + e.getMessage());
            lastShot = System.currentTimeMillis();
        }
    }

    /**
     * Take damage from a player shot
     * 
     * @param damage The amount of damage to take
     */
    public void takeDamage(int damage) {
        try {
            currentHP -= damage;
            System.out.println("🎯 Near Monster " + monsterId + " took " + damage + " damage! HP: " + currentHP + "/" + maxHP);
            
            if (currentHP <= 0) {
                System.out.println("🎯 Near Monster " + monsterId + " DEFEATED!");
                
                // Near monster defeated (no explosion)
                int bossRound = getBossRoundFromNearRound(round);
                game.addScore(100 * bossRound);
                game.addSkillPoints(2 + bossRound); // 3, 4, 5, 6 skill points
                
                // Random chance to drop skill, skill points, or coins
                double dropChance = Math.random();
                System.out.println("🎁 Drop chance: " + dropChance + " (need <0.2 for skill, <0.4 for points, <0.6 for coins)");
                
                if (dropChance < 0.2) { // 20% chance to drop skill
                    System.out.println("🎁 SKILL DROP TRIGGERED! Calling dropRandomSkill...");
                    game.dropRandomSkill((int)x, (int)y);
                } else if (dropChance < 0.4) { // 20% chance to drop skill points
                    System.out.println("🎁 SKILL POINTS DROP TRIGGERED! Calling dropRandomSkillPoints...");
                    game.dropRandomSkillPoints((int)x, (int)y);
                } else {
                    System.out.println("🎁 No drop this time (chance: " + dropChance + ")");
                }
                
                // Near 몬스터 처치 시 코인 획득 표시
                int coinReward = 5 + bossRound; // 6, 7, 8, 9 코인
                game.showCoinEarned((int)x, (int)y, coinReward);
                
                System.out.println("🎯 Removing Near Monster " + monsterId + " from game...");
                game.removeEntity(this);
                
                // Check if all near monsters are defeated
                System.out.println("🎯 Calling checkAllNearMonstersDefeated()...");
                game.checkAllNearMonstersDefeated();
            }
        } catch (Exception e) {
            System.err.println("Error in near monster takeDamage: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Draw this near monster with health bar
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            
            if (nearImage != null) {
                int drawX = (int)x - nearWidth / 2;
                int drawY = (int)y - nearHeight / 2;
                
                g2d.drawImage(nearImage, drawX, drawY, drawX + nearWidth, drawY + nearHeight, 
                             0, 0, nearImage.getWidth(null), nearImage.getHeight(null), null);
            } else {
                // Fallback rendering if image failed to load
                g2d.setColor(Color.ORANGE);
                g2d.fillRect((int)x - nearWidth / 2, (int)y - nearHeight / 2, nearWidth, nearHeight);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int)x - nearWidth / 2, (int)y - nearHeight / 2, nearWidth, nearHeight);
            }
            
            // Draw health bar (above near monster)
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x - 30, (int)y - nearHeight/2 - 15, 60, 6);
            
            // Health bar foreground
            g2d.setColor(Color.GREEN);
            int healthWidth = (int)(60 * ((double)currentHP / maxHP));
            g2d.fillRect((int)x - 30, (int)y - nearHeight/2 - 15, healthWidth, 6);
            
            // Health bar border
            g2d.setColor(Color.WHITE);
            g2d.drawRect((int)x - 30, (int)y - nearHeight/2 - 15, 60, 6);
            
        } catch (Exception e) {
            System.err.println("Error drawing near monster: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notification that this near monster has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Near monster doesn't take collision damage from shots
        // Shot damage is handled in ShotEntity.collidedWith()
    }

    /**
     * Get the near monster's current HP
     * 
     * @return The current HP
     */
    public int getCurrentHP() {
        return currentHP;
    }

    /**
     * Get the near monster's maximum HP
     * 
     * @return The maximum HP
     */
    public int getMaxHP() {
        return maxHP;
    }

    /**
     * Get the near monster's round number
     * 
     * @return The round number
     */
    public int getRound() {
        return round;
    }

    /**
     * Override getBounds to provide hitbox for the near monster
     * 
     * @return The bounds of the near monster entity
     */
    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle((int)x - nearWidth/2, (int)y - nearHeight/2, nearWidth, nearHeight);
    }
    
    /**
     * Get the shot sprite for the given near round
     * 
     * @param nearRound The near round number (1,3,5,7)
     * @return The sprite path for the shot
     */
    private String getShotSpriteForRound(int nearRound) {
        switch (nearRound) {
            case 1: return "sprites/Boss_Attack/ice ball.gif"; // 1라운드: ice ball.gif
            case 3: return "sprites/Boss_Attack/2round1.gif";  // 3라운드: 2round1.gif
            case 5: return "sprites/shot.gif";                 // 5라운드: 검정색 구체
            case 7: return "sprites/Skill/Heat.gif";          // 7라운드: 초록색 구체
            default: return "sprites/shot.gif"; // Default fallback
        }
    }
}
