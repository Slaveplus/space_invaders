package org.newdawn.spaceinvaders.gameplay.entity;

import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.util.Logger;
import org.newdawn.spaceinvaders.common.util.LoggerFactory;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 코인 엔티티 - 아래로 떨어지는 코인
 * 플레이어가 획득할 수 있는 코인 아이템
 */
public class CoinEntity extends Entity {
    /** 게임 인스턴스 */
    private Game game;
    /** 코인 이미지 */
    private BufferedImage coinImage;
    /** 코인 가치 */
    private int coinValue;
    /** 코인 크기 */
    private static final int COIN_SIZE = 24;
    /** 떨어지는 속도 */
    private static final double FALL_SPEED = 150.0;
    
    /** 로거 */
    private static final Logger logger = LoggerFactory.getLogger(CoinEntity.class);
    
    /**
     * 코인 엔티티 생성자
     * 
     * @param game 게임 인스턴스
     * @param x X 좌표
     * @param y Y 좌표
     * @param coinValue 코인 가치
     */
    public CoinEntity(Game game, int x, int y, int coinValue) {
        super("sprites/star coin normal.png", x, y);
        this.game = game;
        this.coinValue = coinValue;
        
        // 코인 이미지 로드
        loadCoinImage();
        
        // 아래로 떨어지도록 설정
        setVerticalMovement(FALL_SPEED);
        setHorizontalMovement(0);
        
        logger.debug("CoinEntity created at (" + x + ", " + y + ") with value: " + coinValue);
    }
    
    /**
     * 코인 이미지 로드
     */
    private void loadCoinImage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/star coin normal.png");
            if (is != null) {
                coinImage = ImageIO.read(is);
                is.close();
            } else {
                logger.error("Failed to load coin image: sprites/star coin normal.png");
            }
        } catch (Exception e) {
            logger.error("Error loading coin image: " + e.getMessage(), e);
        }
    }
    
    /**
     * 엔티티 이동 처리
     * 
     * @param delta 시간 간격
     */
    public void move(long delta) {
        // 부모 클래스의 move 메서드 호출
        super.move(delta);
        
        // 화면 밖으로 나가면 제거
        if (y > 600) {
            game.removeEntity(this);
            logger.debug("Coin fell off screen and removed");
        }
    }
    
    /**
     * 엔티티 그리기
     * 
     * @param g 그래픽 컨텍스트
     */
    public void draw(Graphics2D g) {
        if (coinImage != null) {
            // 코인 이미지 그리기
            g.drawImage(coinImage, (int)x, (int)y, COIN_SIZE, COIN_SIZE, null);
        } else {
            // 이미지 로드 실패 시 기본 원형 그리기
            g.setColor(java.awt.Color.YELLOW);
            g.fillOval((int)x, (int)y, COIN_SIZE, COIN_SIZE);
            g.setColor(java.awt.Color.ORANGE);
            g.drawOval((int)x, (int)y, COIN_SIZE, COIN_SIZE);
        }
    }
    
    /**
     * 다른 엔티티와의 충돌 처리
     * 
     * @param other 다른 엔티티
     */
    public void collidedWith(Entity other) {
        // 플레이어와 충돌 시 코인 획득
        if (other.getClass().getSimpleName().equals("ShipEntity")) {
            logger.debug("Player collected coin worth: " + coinValue);
            
            // 게임에 코인 획득 알림
            game.addEarnedCoins(coinValue);
            
            // 코인 제거
            game.removeEntity(this);
        }
    }
    
    /**
     * 코인 가치 반환
     * 
     * @return 코인 가치
     */
    public int getCoinValue() {
        return coinValue;
    }
    
    /**
     * 엔티티 로직 처리
     */
    public void doLogic() {
        // 코인은 특별한 로직이 필요하지 않음
    }
}