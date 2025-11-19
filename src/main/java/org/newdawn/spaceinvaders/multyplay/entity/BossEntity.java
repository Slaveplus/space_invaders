package org.newdawn.spaceinvaders.multyplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.entity.attack.IceAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.IceBallAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.MagneticFieldEntity;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round2LaserAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round2MachineGunAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round2Phase1Attack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round2QuadAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round2RandomAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round3PullAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round3RandomAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round3StraightAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round4GreenSphereAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round4HealAttack;
import org.newdawn.spaceinvaders.multyplay.entity.attack.Round4PlayerLineAttack;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

/**
 * 싱글플레이어 보스 전투 동작을 미러링하는 멀티플레이어 보스 엔티티입니다.
 */
public class BossEntity extends Entity {

    private final MultiplayerGameContext game;
    private final int round;

    private int currentHP;
    private int maxHP;
    private int phase = 1;
    private boolean used = false;

    private double moveSpeed = 50;

    /** 라운드 1 타이머 */
    private long lastIceAttack = 0;
    private long iceAttackInterval = 4000;
    private long lastIceBallAttack = 0;
    private long iceBallAttackInterval = 4000;
    private long lastMagneticField = 0;
    private long magneticFieldInterval = 15000;

    private long lastAttackTime = 0;
    private long attackInterval = 3000;
    private final int[] attackPatterns = {1, 2, 3, 4, 5};
    private final boolean[] attackUsed = new boolean[attackPatterns.length];

    /** 라운드 2 타이머 */
    private long lastRound2Attack = 0;
    private long round2AttackInterval = 4000;
    private int round2AttackPattern = 0;

    /** 라운드 3 타이머 */
    private long lastRound3Attack = 0;
    private long round3AttackInterval = 4000;
    private int round3AttackPattern = 0;

    /** 라운드 4 타이머 */
    private long lastRound4HealAttack = 0;
    private long round4HealInterval = 60000;
    private long lastRound4GreenSphereAttack = 0;
    private long round4GreenSphereInterval = 2000;
    private long lastRound4PlayerLineAttack = 0;
    private long round4PlayerLineInterval = 5000;
    private boolean round4TimerStarted = false;
    private long round4StartTime = 0;
    private long round4TimeLimit = 600000;

    public BossEntity(MultiplayerGameContext game, int x, int y, int round) {
        super(getBossSpriteForRound(round), x, y);
        this.game = game;
        this.round = round;

        maxHP = 50 + (round * 30);
        currentHP = maxHP;
        moveSpeed = 50 + (round * 10);

        dx = 0;
        dy = 0;

        Arrays.fill(attackUsed, false);
    }

    @Override
    public void move(long delta) {
        dx = 0;
        dy = 0;

        if (!game.canEnemiesAttack()) {
            return;
        }

        switch (round) {
            case 1:
                tryMagneticFieldAttack();
                trySequentialRandomAttack();
                break;
            case 2:
                tryRound2Attack();
                break;
            case 3:
                tryRound3Attack();
                break;
            case 4:
                tryRound4Attacks();
                break;
            default:
                performFallbackPattern();
                break;
        }
    }

    private void tryMagneticFieldAttack() {
        if (!game.canEnemiesAttack()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastMagneticField < magneticFieldInterval) {
            return;
        }
        lastMagneticField = now;
        double fieldRadius = 190;
        double fieldStrength = -0.5;
        long duration = 4000;
        MagneticFieldEntity field = new MagneticFieldEntity(game, (int) x, (int) y, fieldRadius, fieldStrength, duration);
        game.addEntity(field);
    }

    private void trySequentialRandomAttack() {
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < attackInterval) {
            return;
        }

        boolean allUsed = true;
        for (boolean used : attackUsed) {
            if (!used) {
                allUsed = false;
                break;
            }
        }
        if (allUsed) {
            Arrays.fill(attackUsed, false);
        }

        int available = 0;
        for (boolean used : attackUsed) {
            if (!used) {
                available++;
            }
        }
        if (available == 0) {
            return;
        }

        int pick = (int) (Math.random() * available);
        int selectedPattern = attackPatterns[0];
        for (int i = 0; i < attackPatterns.length; i++) {
            if (!attackUsed[i]) {
                if (pick == 0) {
                    selectedPattern = attackPatterns[i];
                    attackUsed[i] = true;
                    break;
                }
                pick--;
            }
        }

        executeAttackPattern(selectedPattern);
        lastAttackTime = now;
    }

    private void executeAttackPattern(int pattern) {
        switch (pattern) {
            case 1:
                tryIceAttack();
                break;
            case 2:
                tryIceBallAttack();
                break;
            case 3:
                tryFanIceBallsAttack();
                break;
            case 4:
                tryWaveIceBallsAttack();
                break;
            case 5:
                trySpiralIceBallsAttack();
                break;
            default:
                break;
        }
    }

    private void tryIceAttack() {
        if (!game.canEnemiesAttack()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastIceAttack < iceAttackInterval) {
            return;
        }
        game.addEntity(new IceAttack(game, (int) x, (int) y + 150));
        lastIceAttack = now;
    }

    private void tryIceBallAttack() {
        if (!game.canEnemiesAttack()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastIceBallAttack < iceBallAttackInterval) {
            return;
        }
        performTripleIceBallAttack();
        lastIceBallAttack = now;
    }

    private void performTripleIceBallAttack() {
        IceBallAttack leftBall = new IceBallAttack(game, (int) x, (int) y + 150, -1.2, 1);
        IceBallAttack centerBall = new IceBallAttack(game, (int) x, (int) y + 150, 0, 1);
        IceBallAttack rightBall = new IceBallAttack(game, (int) x, (int) y + 150, 1.2, 1);
        game.addEntity(leftBall);
        game.addEntity(centerBall);
        game.addEntity(rightBall);
    }

    private void tryFanIceBallsAttack() {
        double[] angles = {-1.2, -0.6, 0.0, 0.6, 1.2};
        double speed = 0.4;
        for (double angle : angles) {
            IceBallAttack ball = new IceBallAttack(game, (int) x, (int) y + 150, angle * speed, speed);
            game.addEntity(ball);
        }
    }

    private void tryWaveIceBallsAttack() {
        double[] xOffsets = {-1.0, -0.6, -0.3, 0.0, 0.3, 0.6, 1.0};
        double[] ySpeeds = {0.2, 0.3, 0.4, 0.5, 0.4, 0.3, 0.2};
        double xSpeed = 0.2;
        for (int i = 0; i < xOffsets.length; i++) {
            IceBallAttack ball = new IceBallAttack(
                    game,
                    (int) (x + xOffsets[i] * 100),
                    (int) y + 150,
                    xOffsets[i] * xSpeed,
                    ySpeeds[i]);
            game.addEntity(ball);
        }
    }

    private void trySpiralIceBallsAttack() {
        int numBalls = 8;
        double baseSpeed = 0.3;
        double spiralSpeed = 0.1;
        for (int i = 0; i < numBalls; i++) {
            double angle = (2 * Math.PI * i) / numBalls;
            double speedX = Math.cos(angle) * baseSpeed + Math.sin(angle) * spiralSpeed;
            double speedY = Math.sin(angle) * baseSpeed + 0.3;
            IceBallAttack ball = new IceBallAttack(game, (int) x, (int) y + 150, speedX, speedY);
            game.addEntity(ball);
        }
    }

    private void tryRound2Attack() {
        long now = System.currentTimeMillis();
        if (now - lastRound2Attack < round2AttackInterval) {
            return;
        }

        switch (round2AttackPattern) {
            case 0:
                executeRound2LaserAttack();
                break;
            case 1:
                executeRound2PhaseAttack();
                break;
            case 2:
                executeRound2RandomAttack();
                break;
            case 3:
                executeRound2QuadAttack();
                break;
            default:
                executeRound2MachineGunAttack();
                break;
        }

        round2AttackPattern = (round2AttackPattern + 1) % 5;
        lastRound2Attack = now;
    }

    private void executeRound2LaserAttack() {
        Round2LaserAttack laser = new Round2LaserAttack(game, 400, 100);
        game.addEntity(laser);
    }

    private void executeRound2PhaseAttack() {
        Round2Phase1Attack phase1 = new Round2Phase1Attack(game, 400, 150);
        game.addEntity(phase1);
    }

    private void executeRound2RandomAttack() {
        int randomX = 200 + (int) (Math.random() * 400);
        Round2RandomAttack slash = new Round2RandomAttack(game, randomX, 150);
        game.addEntity(slash);
    }

    private void executeRound2QuadAttack() {
        int centerX = 400;
        int centerY = 150;
        for (int direction = 0; direction < 4; direction++) {
            Round2QuadAttack quad = new Round2QuadAttack(game, centerX, centerY, direction);
            game.addEntity(quad);
        }
    }

    private void executeRound2MachineGunAttack() {
        int randomX = 150 + (int) (Math.random() * 500);
        int startY = 0;
        for (int i = 0; i < 3; i++) {
            Round2MachineGunAttack shot = new Round2MachineGunAttack(game, randomX, startY);
            game.addEntity(shot);
        }
    }

    private void tryRound3Attack() {
        long now = System.currentTimeMillis();
        if (now - lastRound3Attack < round3AttackInterval) {
            return;
        }

        switch (round3AttackPattern) {
            case 0:
                executeRound3StraightAttack();
                break;
            case 1:
                executeRound3RandomAttack();
                break;
            default:
                executeRound3PullAttack();
                break;
        }

        round3AttackPattern = (round3AttackPattern + 1) % 3;
        lastRound3Attack = now;
    }

    private void executeRound3StraightAttack() {
        Round3StraightAttack straight = new Round3StraightAttack(game, (int) x, (int) y + 120);
        game.addEntity(straight);
    }

    private void executeRound3RandomAttack() {
        int randomX = 150 + (int) (Math.random() * 500);
        Round3RandomAttack random = new Round3RandomAttack(game, randomX, 250);
        game.addEntity(random);
    }

    private void executeRound3PullAttack() {
        int pullX = 200 + (int) (Math.random() * 400);
        int pullY = 280;
        Entity targetShip = game.getShip(null);
        if (targetShip != null) {
            pullY = targetShip.getY();
        }
        pullY = Math.max(120, Math.min(520, pullY));
        Round3PullAttack pull = new Round3PullAttack(game, pullX, pullY);
        game.addEntity(pull);
    }

    private void tryRound4Attacks() {
        if (!round4TimerStarted) {
            round4TimerStarted = true;
            round4StartTime = System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();

        if (now - lastRound4HealAttack >= round4HealInterval) {
            Round4HealAttack heal = new Round4HealAttack(game, 400, 250);
            game.addEntity(heal);
            lastRound4HealAttack = now;
        }

        if (now - lastRound4GreenSphereAttack >= round4GreenSphereInterval) {
            double playerX = 400;
            double playerY = 500;
            Entity ship = game.getShip(null);
            if (ship != null) {
                playerX = ship.getX();
                playerY = ship.getY();
            }
            double dx = playerX - x;
            double dy = playerY - (y + 150);
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > 0) {
                dx /= distance;
                dy /= distance;
            }
            Round4GreenSphereAttack sphere = new Round4GreenSphereAttack(game, (int) x, (int) y + 150, dx, dy);
            game.addEntity(sphere);
            lastRound4GreenSphereAttack = now;
        }

        if (now - lastRound4PlayerLineAttack >= round4PlayerLineInterval) {
            int randomX = 150 + (int) (Math.random() * 500);
            Round4PlayerLineAttack lineAttack = new Round4PlayerLineAttack(game, randomX, 500);
            game.addEntity(lineAttack);
            lastRound4PlayerLineAttack = now;
        }

        if (now - round4StartTime > round4TimeLimit) {
            round4StartTime = now;
        }
    }

    private void performFallbackPattern() {
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < 1500) {
            return;
        }
        Entity targetShip = game.getShip(null);
        double playerX = targetShip != null ? targetShip.getX() + 15 : x;
        double playerY = targetShip != null ? targetShip.getY() : y + 200;
        createDirectionalShot(x, y + 75, playerX - x, playerY - (y + 75), 300, false);
        lastAttackTime = now;
    }

    public void takeDamage(int damage, String playerId) {
        currentHP -= damage;
        if (currentHP < 0) {
            currentHP = 0;
        }

        updatePhase();

        if (currentHP <= 0 && !used) {
            grantBossRewards(playerId);
            createBossExplosion();
            game.notifyBossDefeated(playerId);
            game.removeEntity(this);
            used = true;
        }
    }

    private void updatePhase() {
        int newPhase;
        if (currentHP > maxHP * 0.66) {
            newPhase = 1;
        } else if (currentHP > maxHP * 0.33) {
            newPhase = 2;
        } else {
            newPhase = 3;
        }
        phase = newPhase;
    }

    private void grantBossRewards(String playerId) {
        game.addScore(playerId, 1000 * Math.max(1, round));
        game.addSkillPoints(playerId, 5 * Math.max(1, round));

        int[] bossCoinValues = {10, 15, 20, 25, 30};
        int coinValue = bossCoinValues[(int) (Math.random() * bossCoinValues.length)];
        game.addCoins(playerId, coinValue);
        game.showCoinEarned(playerId, (int) x, (int) y, coinValue);
    }

    private void createBossExplosion() {
        for (int i = 0; i < 5; i++) {
            int explosionX = (int) (x + (Math.random() - 0.5) * 100);
            int explosionY = (int) (y + (Math.random() - 0.5) * 100);
            game.createExplosion(explosionX, explosionY, 80.0);
        }
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public int getPhase() {
        return phase;
    }

    public void healToFull() {
        currentHP = maxHP;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (sprite != null) {
            int bossSize = round >= 5 ? 300 : 280;
            int drawX = (int) x - bossSize / 2;
            int drawY = (int) y - bossSize / 2;
            g2d.drawImage(sprite.getImage(), drawX, drawY, drawX + bossSize, drawY + bossSize, 0, 0,
                    sprite.getWidth(), sprite.getHeight(), null);
        }

        int bossSize = round >= 5 ? 300 : 280;
        int halfSize = bossSize / 2;

        g2d.setColor(Color.YELLOW);
        g2d.setFont(g2d.getFont().deriveFont(16f));
        g2d.drawString("Phase " + phase, (int) x - 25, (int) y - halfSize - 20);

        g2d.setColor(Color.RED);
        g2d.fillRect((int) x - 50, (int) y + halfSize + 10, 100, 8);
        g2d.setColor(Color.GREEN);
        int healthWidth = (int) (100 * ((double) currentHP / maxHP));
        g2d.fillRect((int) x - 50, (int) y + halfSize + 10, healthWidth, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawRect((int) x - 50, (int) y + halfSize + 10, 100, 8);
    }

    @Override
    public Rectangle getBounds() {
        int bossSize = round >= 5 ? 300 : 280;
        int halfSize = bossSize / 2;
        int topLeftX = (int) Math.round(x) - halfSize;
        int topLeftY = (int) Math.round(y) - halfSize;
        return new Rectangle(topLeftX, topLeftY, bossSize, bossSize);
    }

    @Override
    public void collidedWith(Entity other) {
        // Boss does not take collision damage here.
    }

    private void createDirectionalShot(double fireX, double fireY, double dirX, double dirY, double speed,
                                       boolean splitting) {
        double length = Math.sqrt(dirX * dirX + dirY * dirY);
        if (length == 0) {
            return;
        }
        dirX /= length;
        dirY /= length;
        BossShotEntity shot;
        if (splitting) {
            shot = new BossShotEntity(game, (int) fireX, (int) fireY, dirX, dirY, speed, 12, true, fireY + 150, 6);
        } else {
            shot = new BossShotEntity(game, (int) fireX, (int) fireY, dirX, dirY, speed);
        }
        game.addEntity(shot);
    }

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
            default:
                return "sprites/Boss/5Boss.png";
        }
    }

    @Override
    protected String snapshotMetadata() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("hp", Integer.toString(currentHP));
        map.put("max", Integer.toString(maxHP));
        map.put("phase", Integer.toString(phase));
        return MetadataCodec.encode(map);
    }

    @Override
    protected void applySnapshotMetadata(String metadata) {
        Map<String, String> map = MetadataCodec.decode(metadata);
        if (map.isEmpty()) {
            return;
        }
        try {
            currentHP = Integer.parseInt(map.getOrDefault("hp", Integer.toString(currentHP)));
        } catch (NumberFormatException ignore) {
        }
        try {
            maxHP = Integer.parseInt(map.getOrDefault("max", Integer.toString(maxHP)));
        } catch (NumberFormatException ignore) {
        }
        try {
            phase = Integer.parseInt(map.getOrDefault("phase", Integer.toString(phase)));
        } catch (NumberFormatException ignore) {
        }
    }
}