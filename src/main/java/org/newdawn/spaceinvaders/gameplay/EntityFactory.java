package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.alien.AlienEntity;
import org.newdawn.spaceinvaders.common.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.common.entity.near.NearEntity;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.entity.ShotEntity;
import org.newdawn.spaceinvaders.common.entity.projectile.MissileEntity;
import org.newdawn.spaceinvaders.common.entity.effect.ExplosionEntity;
import org.newdawn.spaceinvaders.common.entity.effect.HeatEffectEntity;
import org.newdawn.spaceinvaders.common.entity.ui.CoinDisplayEntity;
import org.newdawn.spaceinvaders.gameplay.core.GameplayContext;
import org.newdawn.spaceinvaders.gameplay.entity.GameplayAlienEnvironment;
import org.newdawn.spaceinvaders.gameplay.entity.GameplayMissileEnvironment;
import org.newdawn.spaceinvaders.gameplay.entity.CoinEntity;
import java.util.function.IntUnaryOperator;

/**
 * 엔티티 생성을 담당하는 팩토리 클래스
 * Game.java의 엔티티 생성 책임을 분리
 */
public class EntityFactory {
    private final Game game;
    private final GameplayAlienEnvironment alienEnvironment;
    private final IntUnaryOperator alienHpResolver;
    
    public EntityFactory(Game game, GameplayAlienEnvironment alienEnvironment, IntUnaryOperator alienHpResolver) {
        this.game = game;
        this.alienEnvironment = alienEnvironment;
        this.alienHpResolver = alienHpResolver;
    }
    
    /**
     * Alien 엔티티 생성
     */
    public Entity createAlien(int x, int y) {
        return new AlienEntity(alienEnvironment, x, y, alienHpResolver);
    }
    
    /**
     * Near 엔티티 생성
     */
    public Entity createNear(int round, int index) {
        int posX = 150 + (index * 100);
        return new NearEntity(game, posX, 120, round, index);
    }
    
    /**
     * Boss 엔티티 생성
     */
    public Entity createBoss(int bossRound) {
        return new BossEntity(game, 400, 120, bossRound);
    }
    
    /**
     * 플레이어 우주선 생성
     */
    public ShipEntity createPlayerShip(String playerId, String spaceshipSkin) {
        return new ShipEntity(game, spaceshipSkin, 370, 550);
    }
    
    /**
     * 탄환 생성
     */
    public ShotEntity createShot(String weaponSkin, double x, double y, boolean isAlienShot) {
        return new ShotEntity(game, weaponSkin, (int)x, (int)y, isAlienShot);
    }
    
    /**
     * 스킬 드롭 생성
     */
    public ShotEntity createSkillDrop(int x, int y, int skillType, int skillValue) {
        String spritePath;
        switch (skillType) {
            case 0: // Invincible
                spritePath = "sprites/Skill/1.png";
                break;
            case 2: // Triple Shot
                spritePath = "sprites/Skill/3.png";
                break;
            case 3: // Missile
                spritePath = "sprites/Skill/4.png";
                break;
            default:
                spritePath = "sprites/shot.gif";
                break;
        }
        return new ShotEntity(game, spritePath, x, y, skillType, skillValue);
    }
    
    /**
     * 코인 엔티티 생성
     */
    public CoinEntity createCoin(int x, int y, int coinValue) {
        return new CoinEntity(game, x, y, coinValue);
    }
    
    /**
     * 코인 표시 엔티티 생성
     */
    public CoinDisplayEntity createCoinDisplay(int x, int y, int coinAmount) {
        return new CoinDisplayEntity(game, x, y, coinAmount);
    }
    
    /**
     * 폭발 효과 생성
     */
    public ExplosionEntity createExplosion(int x, int y, double radius) {
        return new ExplosionEntity(game, "sprites/Skill/Explosion.png", x, y, radius);
    }
    
    /**
     * 열 효과 생성
     */
    public HeatEffectEntity createHeatEffect(int x, int y) {
        return new HeatEffectEntity(game, x, y);
    }
    
    /**
     * 미사일 생성
     */
    public MissileEntity createMissile(double startX, double startY, double targetX, double targetY) {
        return new MissileEntity(
            new GameplayMissileEnvironment(game),
            "sprites/Skill/Missile.png",
            (int)startX,
            (int)startY,
            targetX,
            targetY
        );
    }
}

