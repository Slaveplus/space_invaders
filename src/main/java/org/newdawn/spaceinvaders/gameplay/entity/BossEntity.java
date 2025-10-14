package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.gameplay.Game;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round2LaserAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round2Phase1Attack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round2RandomAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round2QuadAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round2MachineGunAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round3StraightAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round3RandomAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round3PullAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round4HealAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round4GreenSphereAttack;
import org.newdawn.spaceinvaders.gameplay.entity.entity_attack.Round4PlayerLineAttack;

import java.awt.Color;

/**
 * 보스 적을 나타내는 엔티티
 * 
 * @author Space Invaders Team
 */
public class BossEntity extends Entity {
    /** 이 엔티티가 존재하는 게임 */
    private Game game;
    /** 이 보스가 "사용됨"인지 여부, 즉 무언가에 맞았는지 */
    private boolean used = false;
    /** 보스 HP */
    private int currentHP;
    private int maxHP;
    /** 보스 이동 속도 */
    private double moveSpeed = 50;
    /** 이동 방향 */
    private boolean movingRight = true;
    /** 마지막 아이스 공격 이후의 시간 */
    private long lastIceAttack = 0;
    /** 아이스 공격 간격 */
    private long iceAttackInterval = 4000; // 4초마다 아이스 공격
    /** 마지막 아이스 볼 공격 이후의 시간 */
    private long lastIceBallAttack = 0;
    /** 아이스 볼 공격 간격 */
    private long iceBallAttackInterval = 4000; // 4초마다 아이스 볼 공격
    /** 마지막 자기장 공격 이후의 시간 */
    private long lastMagneticField = 0;
    /** 자기장 공격 간격 */
    private long magneticFieldInterval = 15000; // 15초마다 자기장 공격
    
    /** Attack pattern management */
    private long lastAttackTime = 0;
    private long attackInterval = 3000; // 3초마다 공격 패턴 변경
    private int currentAttackPattern = 0;
    private int[] attackPatterns = {1, 2, 3, 4, 5}; // 1: ice, 2: ice ball, 3: fan ice balls, 4: wave ice balls, 5: spiral ice balls
    private boolean[] attackUsed = {false, false, false, false, false}; // 각 패턴 사용 여부
    
    /** Round 2 specific attack management */
    private long lastRound2Attack = 0;
    private long round2AttackInterval = 4000; // 4초마다 2라운드 공격
    private int round2AttackPattern = 0; // 0: Laser, 1: Phase Attack, 2: Random Attack, 3: Quad Attack, 4: Machine Gun
    
    /** Round 3 specific attack management */
    private long lastRound3Attack = 0;
    private long round3AttackInterval = 4000; // 4초마다 3라운드 공격
    private int round3AttackPattern = 0; // 0: Straight, 1: Random, 2: Pull
    
    /** Round 4 attack variables */
    private long lastRound4HealAttack = 0;
    private long round4HealInterval = 60000; // 1분(60초)마다 체력 회복 공격
    private long lastRound4GreenSphereAttack = 0;
    private long round4GreenSphereInterval = 2000; // 2초마다 초록색 구체 공격
    private long lastRound4PlayerLineAttack = 0;
    private long round4PlayerLineInterval = 5000; // 5초마다 플레이어 라인 공격
    
    
    /** Round 4 timer variables */
    private long round4StartTime = 0;
    private long round4TimeLimit = 600000; // 10분 = 600,000ms (더 여유있게 설정)
    private boolean round4TimerStarted = false;
    
    /** Boss round number */
    private int round;
    /** Phase of the boss (1-3 based on HP) */
    private int phase = 1;
    /** Attack pattern counter (removed - now using random patterns) */
    /** Counter for fan attack animation */
    private double fanAngle = 0;
    
    /**
     * Get the boss sprite path based on round number
     * 
     * @param round The round number
     * @return The sprite path for the boss
     */
    private static String getBossSpriteForRound(int round) {
        switch (round) {
            case 1:
                return "sprites/Boss/1Boss.png";
            case 2:
                return "sprites/Boss/2Boss.png";
            case 3:
                return "sprites/Boss/3Boss.png";
            case 4:
                return "sprites/Boss/4Boss.png";
            case 5:
                return "sprites/Boss/5Boss.png";
            default:
                // For rounds 6 and above, use 5Boss
                return "sprites/Boss/5Boss.png";
        }
    }
    
    /**
     * Create a new boss entity
     * 
     * @param game The game in which the boss has been created
     * @param x The initial x location of the boss
     * @param y The initial y location of the boss
     * @param round The round number for scaling
     */
    public BossEntity(Game game, int x, int y, int round) {
        super(getBossSpriteForRound(round), x, y);
        
        this.game = game;
        this.round = round;
        
        // Scale boss stats based on round
        maxHP = 50 + (round * 30); // 80, 110, 140, 170, 200...
        currentHP = maxHP;
        
        // Scale movement speed
        moveSpeed = 50 + (round * 10);
        
        
        // Boss stays in center back - no movement
        dx = 0;
        dy = 0;
    }
    
    /**
     * Request that this boss moved based on time elapsed
     * 
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        try {
            // Boss stays stationary in center back
            dx = 0;
            dy = 0;
            
            // Round-specific attack patterns
            if (round == 1) {
                // 자기장 공격은 독립적으로 실행 (15초마다)
                tryMagneticFieldAttack();
                
                // 나머지 공격들은 순차적으로 랜덤하게 실행
                trySequentialRandomAttack();
            } else if (round == 2) {
                // 2라운드 전용 레이저 공격
                System.out.println("🔵 Boss is in Round 2, attempting laser attack...");
                tryRound2LaserAttack();
            } else if (round == 3) {
                // 3라운드 전용 공격
                System.out.println("🔴 Boss is in Round 3, attempting attack...");
                tryRound3StraightAttack();
            } else if (round == 4) {
                // 4라운드 전용 공격
                System.out.println("🟣 Boss is in Round 4, attempting attacks...");
                
                // 4라운드 타이머 시작 (한 번만)
                if (!round4TimerStarted) {
                    round4StartTime = System.currentTimeMillis();
                    round4TimerStarted = true;
                    System.out.println("🟣 Round 4 timer started! 10 minutes to defeat boss or instant death!");
                }
                
                tryRound4HealAttack();
                tryRound4GreenSphereAttack();
                tryRound4PlayerLineAttack();
                // checkRound4Timer(); // 타이머 완전 비활성화 - 즉사 버그 해결
            } else {
                System.out.println("🔵 Boss current round: " + round + " (not Round 2, 3, or 4)");
            }
        } catch (Exception e) {
            System.err.println("Error in boss move: " + e.getMessage());
            e.printStackTrace();
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
            
            // Update phase based on HP
            int newPhase;
            if (currentHP > maxHP * 0.66) {
                newPhase = 1;
            } else if (currentHP > maxHP * 0.33) {
                newPhase = 2;
            } else {
                newPhase = 3;
            }
            
            if (newPhase != phase) {
                phase = newPhase;
                System.out.println("Boss Phase " + phase + " activated!");
            }
            
            if (currentHP <= 0) {
                // Boss defeated
                createBossExplosion();
                game.addScore(1000 * round);
                game.addSkillPoints(5 * round);
                
                // 보스 처치 시 코인 획득 표시
                int[] bossCoinValues = {10, 15, 20, 25, 30};
                int randomIndex = (int)(Math.random() * bossCoinValues.length);
                int coinValue = bossCoinValues[randomIndex];
                
                game.showCoinEarned((int)x, (int)y, coinValue);
                System.out.println("💰 Boss defeated! Earned coin worth: " + coinValue);
                
                game.notifyBossDefeated();
                game.removeEntity(this);
                used = true;
            }
        } catch (Exception e) {
            System.err.println("Error in boss takeDamage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create explosion effect when boss is defeated
     */
    private void createBossExplosion() {
        try {
            // Create multiple explosions for dramatic effect
            for (int i = 0; i < 5; i++) {
                int explosionX = (int)(x + (Math.random() - 0.5) * 100);
                int explosionY = (int)(y + (Math.random() - 0.5) * 100);
                game.createExplosion(explosionX, explosionY, 80.0);
            }
        } catch (Exception e) {
            System.err.println("Error creating boss explosion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Draw this boss with health bar
     * 
     * @param g The graphics context on which to draw
     */
    public void draw(Graphics g) {
        try {
            // Draw the boss sprite with fixed size by round
            Graphics2D g2d = (Graphics2D) g;
            if (sprite != null) {
                // Draw boss sprite with fixed size: 1-4라운드: 280x280, 5라운드: 300x300
                int bossSize = (round == 5) ? 300 : 280; // 5라운드만 300픽셀, 나머지는 280픽셀
                int drawX = (int)x - bossSize/2; // Center horizontally
                int drawY = (int)y - bossSize/2; // Center vertically
                
                g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + bossSize, drawY + bossSize, 
                             0, 0, sprite.getWidth(), sprite.getHeight(), null);
            }
            
            // Calculate boss size for positioning (same as draw size)
            int bossSize = (round == 5) ? 300 : 280;
            int halfSize = bossSize / 2;
            
            // Phase indicator (above boss)
            g2d.setColor(Color.YELLOW);
            g2d.setFont(g2d.getFont().deriveFont(16f));
            g2d.drawString("Phase " + phase, (int)x - 25, (int)y - halfSize - 20);
            
            // Draw health bar (below boss)
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x - 50, (int)y + halfSize + 10, 100, 8);
            
            // Health bar foreground
            g2d.setColor(Color.GREEN);
            int healthWidth = (int)(100 * ((double)currentHP / maxHP));
            g2d.fillRect((int)x - 50, (int)y + halfSize + 10, healthWidth, 8);
            
            // Health bar border
            g2d.setColor(Color.WHITE);
            g2d.drawRect((int)x - 50, (int)y + halfSize + 10, 100, 8);
        } catch (Exception e) {
            System.err.println("Error drawing boss: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notification that this boss has collided with another entity
     * 
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        // Boss doesn't take collision damage from shots
        // Shot damage is handled in ShotEntity.collidedWith()
        
        // Alien과 충돌했을 때 Alien이 반대방향으로 이동하도록 함
        // (AlienEntity.collidedWith에서 처리됨)
    }
    
    /**
     * Get the boss's current HP
     * 
     * @return The current HP
     */
    public int getCurrentHP() {
        return currentHP;
    }
    
    /**
     * Get the boss's maximum HP
     * 
     * @return The maximum HP
     */
    public int getMaxHP() {
        return maxHP;
    }
    
    /**
     * Get the boss's current phase
     * 
     * @return The current phase (1-3)
     */
    public int getPhase() {
        return phase;
    }
    
    /**
     * Override getBounds to provide larger hitbox for the bigger boss
     * 
     * @return The bounds of the boss entity
     */
    public java.awt.Rectangle getBounds() {
        // Return bounds matching the visual size: 1-4라운드: 280x280, 5라운드: 300x300
        int bossSize = (round == 5) ? 300 : 280;
        int halfSize = bossSize / 2;
        return new java.awt.Rectangle((int)x - halfSize, (int)y - halfSize, bossSize, bossSize);
    }
    
    /**
     * Attempt to perform ice attack (Round 1 only)
     */
    public void tryIceAttack() {
        try {
            // Check if 3 seconds have passed since game start
            if (!game.canEnemiesAttack()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last ice attack
            if (currentTime - lastIceAttack < iceAttackInterval) {
                return;
            }
            
            // Create new ice attack using ice.gif (from boss center)
            org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceAttack iceAttack = new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceAttack(game, (int) x, (int) y + 150);
            
            // Add to game entities
            game.addEntity(iceAttack);
            
            // Update last ice attack time
            lastIceAttack = currentTime;
            
            System.out.println("Round 1 Boss performed ice attack with ice.gif!");
            
        } catch (Exception e) {
            System.err.println("Error in boss ice attack: " + e.getMessage());
            // 게임이 크래시되지 않도록 예외를 잡고 계속 진행
            lastIceAttack = System.currentTimeMillis(); // 다음 공격 시간 업데이트
        }
    }
    
    /**
     * Set ice attack interval
     */
    public void setIceAttackInterval(long interval) {
        this.iceAttackInterval = interval;
    }
    
    /**
     * Try to perform ice ball attack
     */
    public void tryIceBallAttack() {
        try {
            // Check if 3 seconds have passed since game start
            if (!game.canEnemiesAttack()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last ice ball attack
            if (currentTime - lastIceBallAttack < iceBallAttackInterval) {
                return;
            }
            
            // Always use triple attack (removed spiral attack)
            performTripleIceBallAttack();
            
            // Update last ice ball attack time
            lastIceBallAttack = currentTime;
            
            System.out.println("Round 1 Boss performed ice ball attack!");
            
        } catch (Exception e) {
            System.err.println("Error in boss ice ball attack: " + e.getMessage());
            // 게임이 크래시되지 않도록 예외를 잡고 계속 진행
            lastIceBallAttack = System.currentTimeMillis(); // 다음 공격 시간 업데이트
        }
    }
    
    /**
     * Perform triple ice ball attack (three directions)
     */
    private void performTripleIceBallAttack() {
        try {
            // Create three ice balls in different directions with much wider angles
            // Left direction (much wider angle)
            org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack leftBall = 
                new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack(game, (int) x, (int) y + 150, -1.2, 1);
            game.addEntity(leftBall);
            
            // Center direction (straight down)
            org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack centerBall = 
                new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack(game, (int) x, (int) y + 150, 0, 1);
            game.addEntity(centerBall);
            
            // Right direction (much wider angle)
            org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack rightBall = 
                new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack(game, (int) x, (int) y + 150, 1.2, 1);
            game.addEntity(rightBall);
            
            System.out.println("Triple ice ball attack performed with wider angles!");
            
        } catch (Exception e) {
            System.err.println("Error in triple ice ball attack: " + e.getMessage());
        }
    }
    
    
    /**
     * Set ice ball attack interval
     */
    public void setIceBallAttackInterval(long interval) {
        iceBallAttackInterval = interval;
    }
    
    /**
     * Attempt to perform magnetic field attack
     */
    public void tryMagneticFieldAttack() {
        try {
            // Check if 3 seconds have passed since game start
            if (!game.canEnemiesAttack()) {
                System.out.println("Magnetic field: Enemies cannot attack yet");
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last magnetic field attack
            if (currentTime - lastMagneticField < magneticFieldInterval) {
                return;
            }
            
            // Create magnetic field centered on boss (always repulsion to stop player attacks)
            boolean isAttraction = false; // 항상 척력으로 플레이어 공격을 멈춤
            double fieldStrength = -0.5; // 강한 척력
            
            // 자기장 크기와 지속 시간 (고정 크기 190픽셀, 4초 지속)
            double fieldRadius = 190; // 보스 크기보다 조금 더 넓게 (10픽셀 확장)
            long duration = 4000; // 정확히 4초 지속
            
            // 자기장 생성 위치 (보스 중심)
            int fieldX = (int)x;
            int fieldY = (int)y;
            
            // 자기장 엔티티 생성
            org.newdawn.spaceinvaders.gameplay.entity.entity_attack.MagneticFieldEntity magneticField = 
                new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.MagneticFieldEntity(game, fieldX, fieldY, fieldRadius, fieldStrength, duration);
            
            game.addEntity(magneticField);
            
            // Update last magnetic field attack time
            lastMagneticField = currentTime;
            
            System.out.println("🧲 Round 1 Boss created repulsion field! Duration: 4s, Next attack in: 15s");
            
        } catch (Exception e) {
            System.err.println("Error in boss magnetic field attack: " + e.getMessage());
            // 게임이 크래시되지 않도록 예외를 잡고 계속 진행
            lastMagneticField = System.currentTimeMillis(); // 다음 공격 시간 업데이트
        }
    }
    
    /**
     * Try to perform sequential random attacks (excluding magnetic field)
     */
    public void trySequentialRandomAttack() {
        try {
            // Check if 3 seconds have passed since game start
            if (!game.canEnemiesAttack()) {
                System.out.println("⏰ Boss attack blocked - 3 second delay not passed");
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last attack
            if (currentTime - lastAttackTime < attackInterval) {
                return;
            }
            
            // 모든 패턴이 사용되었으면 리셋
            boolean allUsed = true;
            for (boolean used : attackUsed) {
                if (!used) {
                    allUsed = false;
                    break;
                }
            }
            
            if (allUsed) {
                // 모든 패턴 리셋
                for (int i = 0; i < attackUsed.length; i++) {
                    attackUsed[i] = false;
                }
                System.out.println("🔄 All attack patterns reset - starting new sequence!");
            }
            
            // 사용되지 않은 패턴 중에서 랜덤 선택
            int availablePatterns = 0;
            for (boolean used : attackUsed) {
                if (!used) availablePatterns++;
            }
            
            if (availablePatterns > 0) {
                int randomIndex = (int)(Math.random() * availablePatterns);
                int selectedPattern = -1;
                
                // 사용되지 않은 패턴 중에서 선택
                for (int i = 0; i < attackPatterns.length; i++) {
                    if (!attackUsed[i]) {
                        if (randomIndex == 0) {
                            selectedPattern = attackPatterns[i];
                            attackUsed[i] = true;
                            break;
                        }
                        randomIndex--;
                    }
                }
                
                // 선택된 패턴 실행
                executeAttackPattern(selectedPattern);
                
                // Update last attack time
                lastAttackTime = currentTime;
            }
            
        } catch (Exception e) {
            System.err.println("Error in sequential random attack: " + e.getMessage());
            lastAttackTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Execute the selected attack pattern
     */
    private void executeAttackPattern(int pattern) {
        switch (pattern) {
            case 1: // Ice attack
                tryIceAttackNow();
                System.out.println("🧊 Executing: Ice Attack");
                break;
            case 2: // Ice ball attack
                tryIceBallAttackNow();
                System.out.println("❄️ Executing: Ice Ball Attack");
                break;
            case 3: // Fan ice balls attack
                tryFanIceBallsAttackNow();
                System.out.println("🌪️ Executing: Fan Ice Balls Attack");
                break;
            case 4: // Wave ice balls attack
                tryWaveIceBallsAttackNow();
                System.out.println("🌊 Executing: Wave Ice Balls Attack");
                break;
            case 5: // Spiral ice balls attack
                trySpiralIceBallsAttackNow();
                System.out.println("🌀 Executing: Spiral Ice Balls Attack");
                break;
        }
    }
    
    /**
     * Try to perform ice attack (immediate execution)
     */
    public void tryIceAttackNow() {
        try {
            // Create new ice attack using ice.gif (from boss center)
            org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceAttack iceAttack = 
                new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceAttack(game, (int) x, (int) y + 150);
            
            game.addEntity(iceAttack);
            lastIceAttack = System.currentTimeMillis();
            
        } catch (Exception e) {
            System.err.println("Error in boss ice attack: " + e.getMessage());
        }
    }
    
    /**
     * Try to perform ice ball attack (immediate execution)
     */
    public void tryIceBallAttackNow() {
        try {
            // Always use triple attack (removed spiral attack)
            performTripleIceBallAttack();
            lastIceBallAttack = System.currentTimeMillis();
            
        } catch (Exception e) {
            System.err.println("Error in boss ice ball attack: " + e.getMessage());
        }
    }
    
    /**
     * Try to perform fan ice balls attack (immediate execution)
     * 5개의 ice ball이 부채꼴 모양으로 발사
     */
    public void tryFanIceBallsAttackNow() {
        try {
            // 부채꼴 모양으로 5발 발사 (각도를 넓혀서 더 넓은 범위로)
            double[] angles = {-1.2, -0.6, 0.0, 0.6, 1.2}; // 각도를 더 넓게
            double speed = 0.4;
            
            for (double angle : angles) {
                org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack fanIceBall = 
                    new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack(game, (int) x, (int) y + 150, angle * speed, speed);
                game.addEntity(fanIceBall);
            }
            
            System.out.println("🌪️ Fan Ice Balls attack created!");
            
        } catch (Exception e) {
            System.err.println("Error in fan ice balls attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try to perform wave ice balls attack (immediate execution)
     * 파도 모양으로 ice ball들이 연속 발사
     */
    public void tryWaveIceBallsAttackNow() {
        try {
            // 파도 모양으로 7발 발사 (중앙에서 좌우로 퍼지는 패턴 - 범위를 더 넓게)
            double[] xOffsets = {-1.0, -0.6, -0.3, 0.0, 0.3, 0.6, 1.0};
            double[] ySpeeds = {0.2, 0.3, 0.4, 0.5, 0.4, 0.3, 0.2}; // 파도 모양
            double xSpeed = 0.2;
            
            for (int i = 0; i < xOffsets.length; i++) {
                org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack waveIceBall = 
                    new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack(game, 
                        (int)(x + xOffsets[i] * 100), (int) y + 150, 
                        xOffsets[i] * xSpeed, ySpeeds[i]);
                game.addEntity(waveIceBall);
            }
            
            System.out.println("🌊 Wave Ice Balls attack created!");
            
        } catch (Exception e) {
            System.err.println("Error in wave ice balls attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try to perform spiral ice balls attack (immediate execution)
     * 나선 모양으로 ice ball들이 발사
     */
    public void trySpiralIceBallsAttackNow() {
        try {
            // 나선 모양으로 8발 발사
            int numBalls = 8;
            double baseSpeed = 0.3;
            double spiralSpeed = 0.1;
            
            for (int i = 0; i < numBalls; i++) {
                double angle = (2 * Math.PI * i) / numBalls; // 360도를 8등분
                double speedX = Math.cos(angle) * baseSpeed + Math.sin(angle) * spiralSpeed;
                double speedY = Math.sin(angle) * baseSpeed + 0.3; // 아래쪽으로도 이동
                
                org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack spiralIceBall = 
                    new org.newdawn.spaceinvaders.gameplay.entity.entity_attack.IceBallAttack(game, (int) x, (int) y + 150, speedX, speedY);
                game.addEntity(spiralIceBall);
            }
            
            System.out.println("🌀 Spiral Ice Balls attack created!");
            
        } catch (Exception e) {
            System.err.println("Error in spiral ice balls attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try Round 2 attacks (alternating between laser and phase attacks)
     */
    private void tryRound2LaserAttack() {
        try {
            long currentTime = System.currentTimeMillis();
            System.out.println("🔵 tryRound2LaserAttack called - Current time: " + currentTime + ", Last attack: " + lastRound2Attack + ", Interval: " + round2AttackInterval);
            
            // Check if enough time has passed since last attack
            if (currentTime - lastRound2Attack < round2AttackInterval) {
                System.out.println("🔵 Not enough time passed since last Round 2 attack, waiting...");
                return;
            }
            
                // Alternate between attack patterns
                if (round2AttackPattern == 0) {
                    executeRound2LaserAttack();
                    System.out.println("🔵 Round 2 Laser Attack executed!");
                } else if (round2AttackPattern == 1) {
                    executeRound2PhaseAttack();
                    System.out.println("🎯 Round 2 Phase Attack executed!");
                } else if (round2AttackPattern == 2) {
                    executeRound2RandomAttack();
                    System.out.println("🎲 Round 2 Random Attack executed!");
                } else if (round2AttackPattern == 3) {
                    executeRound2QuadAttack();
                    System.out.println("🎯 Round 2 Quad Attack executed!");
                } else {
                    executeRound2MachineGunAttack();
                    System.out.println("🔫 Round 2 Machine Gun Attack executed!");
                }
                
                // Switch to next pattern
                round2AttackPattern = (round2AttackPattern + 1) % 5;
            lastRound2Attack = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in Round 2 attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 2 laser attack
     */
    private void executeRound2LaserAttack() {
        try {
            // Debug boss position
            System.out.println("🔵 Boss position - X:" + (int)x + " Y:" + (int)y + " W:" + sprite.getWidth() + " H:" + sprite.getHeight());
            
            // 가운데 레이저 공격
            int laserX = 400; // 화면 가운데
            int laserY = 100; // 화면 상단
            
            Round2LaserAttack laser = new Round2LaserAttack(game, laserX, laserY);
            game.addEntity(laser);
            
            System.out.println("🔵 Center Laser Attack launched at X:" + laserX + " Y:" + laserY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 2 Laser Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 2 phase attack (2round3.gif -> 2round1.gif spreading)
     */
    private void executeRound2PhaseAttack() {
        try {
            // Force phase attack to start from screen center since boss might be off-screen
            int phaseX = 400; // 화면 중앙
            int phaseY = 150; // 화면 중앙 상단
            
            Round2Phase1Attack phase1Attack = new Round2Phase1Attack(game, phaseX, phaseY);
            game.addEntity(phase1Attack);
            
            System.out.println("🎯 Round 2 Phase Attack launched from screen center at X:" + phaseX + " Y:" + phaseY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 2 Phase Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 2 random attack (2round2.gif)
     */
    private void executeRound2RandomAttack() {
        try {
            // 랜덤 위치에서 베기 공격 실행
            int randomX = 200 + (int)(Math.random() * 400); // 200~600 사이 랜덤 X
            int randomY = 150; // 보스 라인에서 시작
            
            Round2RandomAttack randomAttack = new Round2RandomAttack(game, randomX, randomY);
            game.addEntity(randomAttack);
            
            System.out.println("🎲 Round 2 Random Slash Attack launched at X:" + randomX + " Y:" + randomY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 2 Random Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 2 quad attack (2round1.gif - 4갈래)
     */
    private void executeRound2QuadAttack() {
        try {
            // 4갈래 공격을 보스 위치에서 발사
            int centerX = 400; // 화면 중앙
            int centerY = 150; // 보스 라인
            
            // 4방향으로 동시 발사 (0: Up, 1: Right, 2: Down, 3: Left)
            for (int direction = 0; direction < 4; direction++) {
                Round2QuadAttack quadAttack = new Round2QuadAttack(game, centerX, centerY, direction);
                game.addEntity(quadAttack);
            }
            
            System.out.println("🎯 Round 2 Quad Attack launched from center at X:" + centerX + " Y:" + centerY + " (4 directions)");
            
        } catch (Exception e) {
            System.err.println("Error in Round 2 Quad Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 2 machine gun attack (2round1.gif - 기관총식)
     */
    private void executeRound2MachineGunAttack() {
        try {
            // 천장부터 내려오는 공격을 랜덤 위치에서 발사
            int randomX = 150 + (int)(Math.random() * 500); // 150~650 사이 랜덤 X
            int startY = 0; // 천장에서 시작 (Y=0)
            
            // 3발 연속 발사
            for (int i = 0; i < 3; i++) {
                Round2MachineGunAttack machineGunAttack = new Round2MachineGunAttack(game, randomX, startY);
                game.addEntity(machineGunAttack);
            }
            
            System.out.println("🔫 Round 2 Ceiling Attack launched at X:" + randomX + " Y:" + startY + " (3 shots from ceiling)");
            
        } catch (Exception e) {
            System.err.println("Error in Round 2 Machine Gun Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try Round 3 attacks (rotates between Straight, Random, Pull)
     */
    private void tryRound3StraightAttack() {
        try {
            long currentTime = System.currentTimeMillis();
            System.out.println("🔴 tryRound3Attack called - Current time: " + currentTime + ", Last attack: " + lastRound3Attack + ", Interval: " + round3AttackInterval);
            
            // Check if enough time has passed since last attack
            if (currentTime - lastRound3Attack < round3AttackInterval) {
                System.out.println("🔴 Not enough time passed since last Round 3 attack, waiting...");
                return;
            }
            
            // Execute different attack patterns
            switch (round3AttackPattern) {
                case 0:
                    executeRound3StraightAttack();
                    System.out.println("🔴 Round 3 Straight Attack executed!");
                    break;
                case 1:
                    executeRound3RandomAttack();
                    System.out.println("🔴 Round 3 Random Attack executed!");
                    break;
                case 2:
                    executeRound3PullAttack();
                    System.out.println("🔴 Round 3 Pull Attack executed!");
                    break;
            }
            
            // Rotate attack pattern
            round3AttackPattern = (round3AttackPattern + 1) % 3;
            
            // Update last attack time
            lastRound3Attack = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in Round 3 Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 3 straight attack (3round4.gif)
     */
    private void executeRound3StraightAttack() {
        try {
            // 매우 큰 직선 공격을 보스 위치에서 발사
            int centerX = 400; // 화면 중앙
            int startY = 200; // 화면 아래쪽에서 시작 (100 → 200)
            
            // 3round4.gif를 사용한 매우 큰 직선 공격
            Round3StraightAttack straightAttack = new Round3StraightAttack(game, centerX, startY);
            game.addEntity(straightAttack);
            
            System.out.println("🔴 Round 3 MASSIVE Straight Attack launched at X:" + centerX + " Y:" + startY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 3 Straight Attack execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 3 random attack (3round2.gif) - 레이저 형태
     */
    private void executeRound3RandomAttack() {
        try {
            // 랜덤 위치에서 레이저 발사
            int randomX = 100 + (int)(Math.random() * 600); // 100-700 사이 랜덤
            int startY = 100; // 화면 상단에서 레이저 발사
            
            // 3round2.gif를 사용한 거대한 레이저 공격
            Round3RandomAttack randomAttack = new Round3RandomAttack(game, randomX, startY);
            game.addEntity(randomAttack);
            
            System.out.println("🔴 Round 3 MASSIVE Random Laser launched at X:" + randomX + " Y:" + startY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 3 Random Laser execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Execute Round 3 pull attack (3round3.gif)
     */
    private void executeRound3PullAttack() {
        try {
            // 플레이어 라인에 끌어당기기 공격 생성
            int randomX = 150 + (int)(Math.random() * 500); // 150-650 사이 랜덤
            int playerY = 550; // 플레이어 라인
            
            // 3round3.gif를 사용한 끌어당기기 공격
            Round3PullAttack pullAttack = new Round3PullAttack(game, randomX, playerY);
            game.addEntity(pullAttack);
            
            System.out.println("🔴 Round 3 Pull Attack launched at X:" + randomX + " Y:" + playerY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 3 Pull Attack execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Try Round 4 Heal Attack
     */
    private void tryRound4HealAttack() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last heal attack
            if (currentTime - lastRound4HealAttack < round4HealInterval) {
                return;
            }
            
            // Execute heal attack
            executeRound4HealAttack();
            
            // Update last heal attack time
            lastRound4HealAttack = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in Round 4 Heal Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute Round 4 Heal Attack
     */
    private void executeRound4HealAttack() {
        try {
            // 맵 중앙에 체력 회복 공격 생성
            int centerX = 400; // 맵 중앙
            int centerY = 300; // 맵 중앙
            
            // 4round.gif를 사용한 체력 회복 공격
            Round4HealAttack healAttack = new Round4HealAttack(game, centerX, centerY);
            game.addEntity(healAttack);
            
            System.out.println("🟣 Round 4 Heal Attack launched at X:" + centerX + " Y:" + centerY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 4 Heal Attack execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check Round 4 Timer - 10분 안에 보스를 잡지 못하면 플레이어 즉사
     */
    private void checkRound4Timer() {
        try {
            if (!round4TimerStarted) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - round4StartTime;
            long remainingTime = round4TimeLimit - elapsedTime;
            
            // 디버그: 타이머 상태 출력 (첫 5초간만)
            if (elapsedTime < 5000) {
                System.out.println("🟣 Round 4 Timer Debug - Elapsed: " + (elapsedTime/1000) + "s, Remaining: " + (remainingTime/1000) + "s, Limit: " + (round4TimeLimit/1000) + "s");
            }
            
            // 1분마다 남은 시간 출력 (더 정확한 체크)
            long minutesElapsed = elapsedTime / 60000;
            if (minutesElapsed > 0 && elapsedTime % 60000 < 100) {
                long remainingMinutes = remainingTime / 60000;
                if (remainingMinutes > 0) {
                    System.out.println("🟣 Round 4 Timer: " + remainingMinutes + " minutes remaining! (Elapsed: " + minutesElapsed + " minutes)");
                }
            }
            
            // 시간 초과시 플레이어 즉사 (elapsedTime이 limit보다 클 때만, 최소 1초 후에만 체크)
            if (elapsedTime >= 1000 && elapsedTime >= round4TimeLimit) {
                System.out.println("🟣 Round 4 Timer EXPIRED! Elapsed: " + (elapsedTime/1000) + " seconds, Limit: " + (round4TimeLimit/1000) + " seconds");
                killPlayer();
            }
            
        } catch (Exception e) {
            System.err.println("Error checking Round 4 timer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Kill player instantly
     */
    private void killPlayer() {
        try {
            // Find player ship and kill it
            for (Entity entity : game.getEntities()) {
                if (entity instanceof ShipEntity) {
                    ShipEntity player = (ShipEntity) entity;
                    
                    // Set player HP to 0 to kill instantly
                    try {
                        java.lang.reflect.Field hpField = player.getClass().getDeclaredField("currentHP");
                        hpField.setAccessible(true);
                        hpField.set(player, 0);
                        
                        System.out.println("🟣 Player killed by Round 4 timer expiration!");
                        game.notifyPlayerDamaged(999); // Massive damage to ensure death
                        
                    } catch (Exception ex) {
                        System.err.println("🟣 Failed to kill player: " + ex.getMessage());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error killing player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Try Round 4 Green Sphere Attack
     */
    private void tryRound4GreenSphereAttack() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last green sphere attack
            if (currentTime - lastRound4GreenSphereAttack < round4GreenSphereInterval) {
                return;
            }
            
            // Execute green sphere attack
            executeRound4GreenSphereAttack();
            
            // Update last green sphere attack time
            lastRound4GreenSphereAttack = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in Round 4 Green Sphere Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute Round 4 Green Sphere Attack
     */
    private void executeRound4GreenSphereAttack() {
        try {
            // Find player position
            ShipEntity player = game.getPlayerShip();
            if (player == null) {
                return;
            }
            
            double playerX = player.getX();
            double playerY = player.getY();
            
            // Calculate direction from boss to player
            double dx = playerX - this.x;
            double dy = playerY - this.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Normalize direction
            dx = dx / distance;
            dy = dy / distance;
            
            // Create green sphere attack moving towards player
            Round4GreenSphereAttack greenSphere = new Round4GreenSphereAttack(game, (int)this.x, (int)this.y, dx, dy);
            game.addEntity(greenSphere);
            
            System.out.println("🟣 Round 4 Green Sphere Attack launched towards player at X:" + playerX + " Y:" + playerY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 4 Green Sphere Attack execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Try Round 4 Player Line Attack
     */
    private void tryRound4PlayerLineAttack() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Check if enough time has passed since last player line attack
            if (currentTime - lastRound4PlayerLineAttack < round4PlayerLineInterval) {
                return;
            }
            
            // Execute player line attack
            executeRound4PlayerLineAttack();
            
            // Update last player line attack time
            lastRound4PlayerLineAttack = currentTime;
            
        } catch (Exception e) {
            System.err.println("Error in Round 4 Player Line Attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute Round 4 Player Line Attack
     */
    private void executeRound4PlayerLineAttack() {
        try {
            // 플레이어 라인에서 랜덤 위치에 공격 생성
            int randomX = 150 + (int)(Math.random() * 500); // 150-650 사이 랜덤 X
            int playerLineY = 550; // 플레이어 라인
            
            // 4round3.gif를 사용한 플레이어 라인 공격
            Round4PlayerLineAttack playerLineAttack = new Round4PlayerLineAttack(game, randomX, playerLineY);
            game.addEntity(playerLineAttack);
            
            System.out.println("🟣 Round 4 Player Line Attack launched at X:" + randomX + " Y:" + playerLineY);
            
        } catch (Exception e) {
            System.err.println("Error in Round 4 Player Line Attack execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Heal boss to full HP (called by Round4HealAttack)
     */
    public void healToFull() {
        currentHP = maxHP;
        System.out.println("🟣 Boss healed to full HP: " + currentHP + "/" + maxHP);
    }

}
