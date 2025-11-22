package org.newdawn.spaceinvaders.common.entity.boss;

import org.newdawn.spaceinvaders.common.entity.Entity;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import org.newdawn.spaceinvaders.common.entity.MagneticFieldEntity;
import org.newdawn.spaceinvaders.common.entity.attack.IceAttack;
import org.newdawn.spaceinvaders.common.entity.attack.IceBallAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round2LaserAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round2MachineGunAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round2Phase1Attack;
import org.newdawn.spaceinvaders.common.entity.attack.Round2QuadAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round2RandomAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round3PullAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round3RandomAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round3StraightAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round4GreenSphereAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round4HealAttack;
import org.newdawn.spaceinvaders.common.entity.attack.Round4PlayerLineAttack;
import org.newdawn.spaceinvaders.common.entity.projectile.BaseBossShotEntity;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

/**
 * 싱글플레이어 보스 전투 동작을 미러링하는 멀티플레이어 보스 엔티티입니다.
 */
public class BossEntity extends Entity {

    private final BossEnvironment environment;
    private final int round;
    private final BossConfig config;
    private final RoundAttackConfig attackConfig;

    private int currentHP;
    private int phase = 1;
    private boolean used = false;
    private AttackPatternSequence attackPatternSequence;

    public BossEntity(BossEnvironment environment, int x, int y, int round) {
        super(BossConfigFactory.getConfig(round).getSpritePath(), x, y);
        this.environment = environment;
        this.round = round;
        this.config = BossConfigFactory.getConfig(round);
        this.attackConfig = RoundAttackConfigFactory.getConfig(round);
        
        currentHP = config.getMaxHP();
        
        dx = 0;
        dy = 0;
        
        // 라운드 1의 경우 공격 패턴 시퀀스 초기화
        if (round == 1) {
            initializeAttackPatternSequence();
        }
    }
    
    private void initializeAttackPatternSequence() {
        attackPatternSequence = new AttackPatternSequence();
        attackPatternSequence.addPattern(new AttackPattern(1, "Ice Attack", this::tryIceAttack));
        attackPatternSequence.addPattern(new AttackPattern(2, "Ice Ball Attack", this::tryIceBallAttack));
        attackPatternSequence.addPattern(new AttackPattern(3, "Fan Ice Balls", this::tryFanIceBallsAttack));
        attackPatternSequence.addPattern(new AttackPattern(4, "Wave Ice Balls", this::tryWaveIceBallsAttack));
        attackPatternSequence.addPattern(new AttackPattern(5, "Spiral Ice Balls", this::trySpiralIceBallsAttack));
    }

    @Override
    public void move(long delta) {
        dx = 0;
        dy = 0;

        if (!environment.canEnemiesAttack()) {
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
        if (!environment.canEnemiesAttack()) {
            return;
        }
        if (!attackConfig.hasMagneticFieldTimer() || !attackConfig.getMagneticFieldTimer().canAttack()) {
            return;
        }
        attackConfig.getMagneticFieldTimer().recordAttack();
        double fieldRadius = 190;
        double fieldStrength = -0.5;
        long duration = 4000;
        MagneticFieldEntity field = new MagneticFieldEntity(environment, (int) x, (int) y, fieldRadius, fieldStrength, duration);
        environment.addEntity(field);
    }

    private void trySequentialRandomAttack() {
        if (!attackConfig.hasMainAttackTimer() || !attackConfig.getMainAttackTimer().canAttack()) {
            return;
        }
        
        AttackPattern selected = attackPatternSequence.selectAndExecuteRandom();
        if (selected != null) {
            attackConfig.getMainAttackTimer().recordAttack();
        }
    }

    // executeAttackPattern 메서드는 AttackPatternSequence에서 직접 호출되므로 제거

    private void tryIceAttack() {
        if (!environment.canEnemiesAttack()) {
            return;
        }
        if (!attackConfig.hasIceAttackTimer() || !attackConfig.getIceAttackTimer().canAttack()) {
            return;
        }
        attackConfig.getIceAttackTimer().recordAttack();
        environment.addEntity(new IceAttack(environment, (int) x, (int) y + 150));
    }

    private void tryIceBallAttack() {
        if (!environment.canEnemiesAttack()) {
            return;
        }
        if (!attackConfig.hasIceBallAttackTimer() || !attackConfig.getIceBallAttackTimer().canAttack()) {
            return;
        }
        attackConfig.getIceBallAttackTimer().recordAttack();
        performTripleIceBallAttack();
    }

    private void performTripleIceBallAttack() {
        IceBallAttack leftBall = new IceBallAttack(environment, (int) x, (int) y + 150, -1.2, 1);
        IceBallAttack centerBall = new IceBallAttack(environment, (int) x, (int) y + 150, 0, 1);
        IceBallAttack rightBall = new IceBallAttack(environment, (int) x, (int) y + 150, 1.2, 1);
        environment.addEntity(leftBall);
        environment.addEntity(centerBall);
        environment.addEntity(rightBall);
    }

    private void tryFanIceBallsAttack() {
        double[] angles = {-1.2, -0.6, 0.0, 0.6, 1.2};
        double speed = 0.4;
        for (double angle : angles) {
            IceBallAttack ball = new IceBallAttack(environment, (int) x, (int) y + 150, angle * speed, speed);
            environment.addEntity(ball);
        }
    }

    private void tryWaveIceBallsAttack() {
        double[] xOffsets = {-1.0, -0.6, -0.3, 0.0, 0.3, 0.6, 1.0};
        double[] ySpeeds = {0.2, 0.3, 0.4, 0.5, 0.4, 0.3, 0.2};
        double xSpeed = 0.2;
        for (int i = 0; i < xOffsets.length; i++) {
            IceBallAttack ball = new IceBallAttack(
                    environment,
                    (int) (x + xOffsets[i] * 100),
                    (int) y + 150,
                    xOffsets[i] * xSpeed,
                    ySpeeds[i]);
            environment.addEntity(ball);
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
            IceBallAttack ball = new IceBallAttack(environment, (int) x, (int) y + 150, speedX, speedY);
            environment.addEntity(ball);
        }
    }

    private void tryRound2Attack() {
        if (!attackConfig.hasMainAttackTimer() || !attackConfig.getMainAttackTimer().canAttack()) {
            return;
        }
        
        switch (attackConfig.getCurrentAttackPattern()) {
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
        
        attackConfig.nextAttackPattern(5);
        attackConfig.getMainAttackTimer().recordAttack();
    }

    private void executeRound2LaserAttack() {
        Round2LaserAttack laser = new Round2LaserAttack(environment, 400, 100);
        environment.addEntity(laser);
    }

    private void executeRound2PhaseAttack() {
        Round2Phase1Attack phase1 = new Round2Phase1Attack(environment, 400, 150);
        environment.addEntity(phase1);
    }

    private void executeRound2RandomAttack() {
        int randomX = 200 + (int) (Math.random() * 400);
        Round2RandomAttack slash = new Round2RandomAttack(environment, randomX, 150);
        environment.addEntity(slash);
    }

    private void executeRound2QuadAttack() {
        int centerX = 400;
        int centerY = 150;
        for (int direction = 0; direction < 4; direction++) {
            Round2QuadAttack quad = new Round2QuadAttack(environment, centerX, centerY, direction);
            environment.addEntity(quad);
        }
    }

    private void executeRound2MachineGunAttack() {
        int randomX = 150 + (int) (Math.random() * 500);
        int startY = 0;
        for (int i = 0; i < 3; i++) {
            Round2MachineGunAttack shot = new Round2MachineGunAttack(environment, randomX, startY);
            environment.addEntity(shot);
        }
    }

    private void tryRound3Attack() {
        if (!attackConfig.hasMainAttackTimer() || !attackConfig.getMainAttackTimer().canAttack()) {
            return;
        }
        
        switch (attackConfig.getCurrentAttackPattern()) {
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
        
        attackConfig.nextAttackPattern(3);
        attackConfig.getMainAttackTimer().recordAttack();
    }

    private void executeRound3StraightAttack() {
        Round3StraightAttack straight = new Round3StraightAttack(environment, (int) x, (int) y + 120);
        environment.addEntity(straight);
    }

    private void executeRound3RandomAttack() {
        int randomX = 150 + (int) (Math.random() * 500);
        Round3RandomAttack random = new Round3RandomAttack(environment, randomX, 250);
        environment.addEntity(random);
    }

    private void executeRound3PullAttack() {
        int pullX = 200 + (int) (Math.random() * 400);
        int pullY = 280;
        Entity targetShip = environment.getShip(null);
        if (targetShip != null) {
            pullY = targetShip.getY();
        }
        pullY = Math.max(120, Math.min(520, pullY));
        Round3PullAttack pull = new Round3PullAttack(environment, pullX, pullY);
        environment.addEntity(pull);
    }

    private void tryRound4Attacks() {
        attackConfig.startTimer();

        if (attackConfig.hasHealAttackTimer() && attackConfig.getHealAttackTimer().canAttack()) {
            Round4HealAttack heal = new Round4HealAttack(environment, 400, 250);
            environment.addEntity(heal);
            attackConfig.getHealAttackTimer().recordAttack();
        }

        if (attackConfig.hasGreenSphereAttackTimer() && attackConfig.getGreenSphereAttackTimer().canAttack()) {
            double playerX = 400;
            double playerY = 500;
            Entity ship = environment.getShip(null);
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
            Round4GreenSphereAttack sphere = new Round4GreenSphereAttack(environment, (int) x, (int) y + 150, dx, dy);
            environment.addEntity(sphere);
            attackConfig.getGreenSphereAttackTimer().recordAttack();
        }

        if (attackConfig.hasPlayerLineAttackTimer() && attackConfig.getPlayerLineAttackTimer().canAttack()) {
            int randomX = 150 + (int) (Math.random() * 500);
            Round4PlayerLineAttack lineAttack = new Round4PlayerLineAttack(environment, randomX, 500);
            environment.addEntity(lineAttack);
            attackConfig.getPlayerLineAttackTimer().recordAttack();
        }

        if (attackConfig.isTimeLimitExceeded()) {
            attackConfig.startTimer(); // 리셋
        }
    }

    private void performFallbackPattern() {
        AttackTimer fallbackTimer = new AttackTimer(1500);
        if (!fallbackTimer.canAttack()) {
            return;
        }
        Entity targetShip = environment.getShip(null);
        double playerX = targetShip != null ? targetShip.getX() + 15 : x;
        double playerY = targetShip != null ? targetShip.getY() : y + 200;
        createDirectionalShot(x, y + 75, playerX - x, playerY - (y + 75), 300, false);
        fallbackTimer.recordAttack();
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
            environment.notifyBossDefeated(playerId);
            environment.removeEntity(this);
            used = true;
        }
    }

    private void updatePhase() {
        int newPhase;
        if (currentHP > config.getMaxHP() * 0.66) {
            newPhase = 1;
        } else if (currentHP > config.getMaxHP() * 0.33) {
            newPhase = 2;
        } else {
            newPhase = 3;
        }
        phase = newPhase;
    }

    private void grantBossRewards(String playerId) {
        environment.addScore(playerId, config.getScoreValue());
        environment.addSkillPoints(playerId, config.getSkillPointsReward());

        int[] bossCoinValues = {10, 15, 20, 25, 30};
        int coinValue = bossCoinValues[(int) (Math.random() * bossCoinValues.length)];
        environment.addCoins(playerId, coinValue);
        environment.showCoinEarned(playerId, (int) x, (int) y, coinValue);
    }

    private void createBossExplosion() {
        for (int i = 0; i < 5; i++) {
            int explosionX = (int) (x + (Math.random() - 0.5) * 100);
            int explosionY = (int) (y + (Math.random() - 0.5) * 100);
            environment.createExplosion(explosionX, explosionY, 80.0);
        }
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return config.getMaxHP();
    }

    public int getPhase() {
        return phase;
    }

    public void healToFull() {
        currentHP = config.getMaxHP();
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
        int healthWidth = (int) (100 * ((double) currentHP / config.getMaxHP()));
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
        BaseBossShotEntity shot = environment.createBossShot(
                (int) fireX,
                (int) fireY,
                dirX,
                dirY,
                speed,
                splitting ? 12 : 8,
                splitting,
                splitting ? fireY + 150 : 0,
                splitting ? 6 : 0);
        if (shot != null) {
            environment.addEntity(shot);
        }
    }

    // getBossSpriteForRound 메서드는 BossConfigFactory에서 관리하므로 제거

    @Override
    protected String snapshotMetadata() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("hp", Integer.toString(currentHP));
        map.put("max", Integer.toString(config.getMaxHP()));
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
        // maxHP는 config에서 관리하므로 스냅샷에서 복원하지 않음
        try {
            phase = Integer.parseInt(map.getOrDefault("phase", Integer.toString(phase)));
        } catch (NumberFormatException ignore) {
        }
    }
}
