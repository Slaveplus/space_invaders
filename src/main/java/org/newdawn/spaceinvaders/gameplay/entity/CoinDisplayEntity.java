package org.newdawn.spaceinvaders.gameplay.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.io.InputStream;

import org.newdawn.spaceinvaders.gameplay.Game;

/**
 * 코인 획득 표시 엔티티 - 코인 이미지와 획득량을 표시
 * 적을 처치했을 때 코인 획득을 시각적으로 보여주는 엔티티
 */
public class CoinDisplayEntity extends Entity {
    /** 게임 인스턴스 */
    private Game game;
    /** 코인 이미지 */
    private BufferedImage coinImage;
    /** 코인 획득량 */
    private int coinAmount;
    /** 표시 지속 시간 (밀리초) */
    private long displayDuration = 2000; // 2초
    /** 생성 시간 */
    private long creationTime;
    /** 상승 속도 */
    private static final double RISE_SPEED = 50.0;
    /** 페이드 아웃 시작 시간 */
    private static final long FADE_START = 1500; // 1.5초 후 페이드 시작
    
    /**
     * 코인 표시 엔티티 생성자
     * 
     * @param game 게임 인스턴스
     * @param x X 좌표
     * @param y Y 좌표
     * @param coinAmount 획득한 코인 수
     */
    public CoinDisplayEntity(Game game, int x, int y, int coinAmount) {
        super("sprites/star coin normal.png", x, y);
        this.game = game;
        this.coinAmount = coinAmount;
        this.creationTime = System.currentTimeMillis();
        
        // 코인 이미지 로드
        loadCoinImage();
        
        // 위로 상승하도록 설정
        setVerticalMovement(-RISE_SPEED);
        setHorizontalMovement(0);
        
        System.out.println("💰 CoinDisplayEntity created at (" + x + ", " + y + ") showing: +" + coinAmount);
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
                System.err.println("Failed to load coin image: sprites/star coin normal.png");
            }
        } catch (Exception e) {
            System.err.println("Error loading coin image: " + e.getMessage());
        }
    }
    
    /**
     * 엔티티 이동 처리
     * 
     * @param delta 시간 간격
     */
    public void move(long delta) {
        // 부모 클래스의 move 메서드 호출 (위로 상승)
        super.move(delta);
        
        // 표시 시간이 지나면 제거
        long currentTime = System.currentTimeMillis();
        if (currentTime - creationTime > displayDuration) {
            game.removeEntity(this);
            System.out.println("💰 CoinDisplayEntity removed after " + displayDuration + "ms");
        }
    }
    
    /**
     * 엔티티 그리기
     * 
     * @param g 그래픽 컨텍스트
     */
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - creationTime;
        
        // 페이드 아웃 효과 계산
        float alpha = 1.0f;
        if (elapsed > FADE_START) {
            alpha = 1.0f - ((float)(elapsed - FADE_START) / (displayDuration - FADE_START));
            alpha = Math.max(0.0f, alpha);
        }
        
        // 알파 설정
        g2d.setComposite(java.awt.AlphaComposite.getInstance(
            java.awt.AlphaComposite.SRC_OVER, alpha));
        
        // 코인 이미지 그리기 (16x16 크기)
        if (coinImage != null) {
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                              java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, 
                              java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            
            g2d.drawImage(coinImage, (int)x, (int)y, 16, 16, null);
        } else {
            // 이미지 로드 실패 시 기본 원형 그리기
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)x, (int)y, 16, 16);
            g2d.setColor(Color.ORANGE);
            g2d.drawOval((int)x, (int)y, 16, 16);
        }
        
        // 코인 수량 텍스트 그리기
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        
        String coinText = "+" + coinAmount;
        int textX = (int)x + 20; // 코인 이미지 오른쪽에 텍스트 (16x16에 맞춤)
        int textY = (int)y + 12; // 코인 이미지 중앙에 텍스트 (16x16에 맞춤)
        
        // 텍스트 그림자 효과
        g2d.setColor(Color.BLACK);
        g2d.drawString(coinText, textX + 1, textY + 1);
        
        // 메인 텍스트
        g2d.setColor(Color.WHITE);
        g2d.drawString(coinText, textX, textY);
        
        // 알파 복원
        g2d.setComposite(java.awt.AlphaComposite.getInstance(
            java.awt.AlphaComposite.SRC_OVER, 1.0f));
    }
    
    /**
     * 다른 엔티티와의 충돌 처리 (코인 표시는 충돌하지 않음)
     * 
     * @param other 다른 엔티티
     */
    public void collidedWith(Entity other) {
        // 코인 표시는 다른 엔티티와 충돌하지 않음
    }
    
    /**
     * 코인 획득량 반환
     * 
     * @return 코인 획득량
     */
    public int getCoinAmount() {
        return coinAmount;
    }
    
    /**
     * 엔티티 로직 처리
     */
    public void doLogic() {
        // 코인 표시는 특별한 로직이 필요하지 않음
    }
}
