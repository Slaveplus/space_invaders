package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * 메인 메뉴에서 아군과 적이 전투하는 모습을 보여주는 애니메이션 배경 클래스
 */
public class CombatBackground {
    private BufferedImage backgroundImage;
    private BufferedImage allySpaceshipSprite;
    private BufferedImage enemyAlienSprite;
    private BufferedImage shotSprite;
    
    private List<AllySpaceship> allySpaceships;
    private List<EnemyAlien> enemyAliens;
    private List<Shot> shots;
    private List<Explosion> explosions;
    
    private Random random;
    private long lastShotTime;
    private static final long SHOT_INTERVAL = 800; // 0.8초마다 총알 발사
    
    /**
     * 아군 우주선을 나타내는 내부 클래스
     */
    private class AllySpaceship {
        private double x, y;
        private double targetY;
        private double speed;
        private boolean isAlive;
        private int lifeTime;
        
        public AllySpaceship(double x, double y) {
            this.x = x;
            this.y = y;
            this.targetY = y;
            this.speed = 30 + random.nextDouble() * 20; // 30-50 픽셀/초
            this.isAlive = true;
            this.lifeTime = 0;
        }
        
        public void update(long delta) {
            if (!isAlive) return;
            
            // 좌우로 부드럽게 움직임
            x -= speed * (delta / 1000.0);
            
            // 상하로 부드럽게 움직임
            double diff = targetY - y;
            y += diff * 0.02 * delta;
            
            // 새로운 목표 높이 설정
            if (lifeTime % 100 == 0) {
                targetY = 200 + random.nextDouble() * 200;
            }
            
            lifeTime++;
            
            // 화면 밖으로 나가면 제거
            if (x < -100 || lifeTime > 2000) {
                isAlive = false;
            }
        }
        
        public void draw(Graphics2D g2d) {
            if (!isAlive || allySpaceshipSprite == null) return;
            
            Graphics2D g = (Graphics2D) g2d.create();
            
            // 투명도 설정
            float alpha = Math.max(0.4f, 1.0f - (float)(lifeTime / 2000.0));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // 우주선 그리기
            int spriteWidth = allySpaceshipSprite.getWidth();
            int spriteHeight = allySpaceshipSprite.getHeight();
            g.drawImage(allySpaceshipSprite, (int)x - spriteWidth/2, (int)y - spriteHeight/2, null);
            
            g.dispose();
        }
        
        public boolean isAlive() { return isAlive; }
        public double getX() { return x; }
        public double getY() { return y; }
    }
    
    /**
     * 적 외계인을 나타내는 내부 클래스
     */
    private class EnemyAlien {
        private double x, y;
        private double targetY;
        private double speed;
        private boolean isAlive;
        private int lifeTime;
        
        public EnemyAlien(double x, double y) {
            this.x = x;
            this.y = y;
            this.targetY = y;
            this.speed = 20 + random.nextDouble() * 30; // 20-50 픽셀/초
            this.isAlive = true;
            this.lifeTime = 0;
        }
        
        public void update(long delta) {
            if (!isAlive) return;
            
            // 좌우로 부드럽게 움직임
            x -= speed * (delta / 1000.0);
            
            // 상하로 부드럽게 움직임
            double diff = targetY - y;
            y += diff * 0.015 * delta;
            
            // 새로운 목표 높이 설정
            if (lifeTime % 120 == 0) {
                targetY = 150 + random.nextDouble() * 250;
            }
            
            lifeTime++;
            
            // 화면 밖으로 나가면 제거
            if (x < -100 || lifeTime > 2500) {
                isAlive = false;
            }
        }
        
        public void draw(Graphics2D g2d) {
            if (!isAlive || enemyAlienSprite == null) return;
            
            Graphics2D g = (Graphics2D) g2d.create();
            
            // 투명도 설정
            float alpha = Math.max(0.3f, 1.0f - (float)(lifeTime / 2500.0));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // 외계인 그리기
            int spriteWidth = enemyAlienSprite.getWidth();
            int spriteHeight = enemyAlienSprite.getHeight();
            g.drawImage(enemyAlienSprite, (int)x - spriteWidth/2, (int)y - spriteHeight/2, null);
            
            g.dispose();
        }
        
        public boolean isAlive() { return isAlive; }
        public double getX() { return x; }
        public double getY() { return y; }
        public Rectangle getBounds() {
            if (enemyAlienSprite == null) return new Rectangle();
            int width = enemyAlienSprite.getWidth();
            int height = enemyAlienSprite.getHeight();
            return new Rectangle((int)x - width/2, (int)y - height/2, width, height);
        }
    }
    
    /**
     * 총알을 나타내는 내부 클래스
     */
    private class Shot {
        private double x, y;
        private double speed;
        private boolean isAlive;
        private int lifeTime;
        
        public Shot(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 200; // 총알 속도
            this.isAlive = true;
            this.lifeTime = 0;
        }
        
        public void update(long delta) {
            if (!isAlive) return;
            
            // 총알 이동 (오른쪽에서 왼쪽으로)
            x -= speed * (delta / 1000.0);
            
            lifeTime++;
            
            // 화면 밖으로 나가거나 너무 오래 살면 제거
            if (x < -50 || lifeTime > 1000) {
                isAlive = false;
            }
        }
        
        public void draw(Graphics2D g2d) {
            if (!isAlive || shotSprite == null) return;
            
            Graphics2D g = (Graphics2D) g2d.create();
            
            // 총알 그리기
            int spriteWidth = shotSprite.getWidth();
            int spriteHeight = shotSprite.getHeight();
            g.drawImage(shotSprite, (int)x - spriteWidth/2, (int)y - spriteHeight/2, null);
            
            g.dispose();
        }
        
        public boolean isAlive() { return isAlive; }
        public double getX() { return x; }
        public double getY() { return y; }
        public Rectangle getBounds() {
            if (shotSprite == null) return new Rectangle();
            int width = shotSprite.getWidth();
            int height = shotSprite.getHeight();
            return new Rectangle((int)x - width/2, (int)y - height/2, width, height);
        }
    }
    
    /**
     * 폭발 효과를 나타내는 내부 클래스
     */
    private class Explosion {
        private double x, y;
        private int frame;
        private int maxFrames;
        private boolean isAlive;
        
        public Explosion(double x, double y) {
            this.x = x;
            this.y = y;
            this.frame = 0;
            this.maxFrames = 30; // 30프레임 동안 폭발 효과
            this.isAlive = true;
        }
        
        public void update() {
            frame++;
            if (frame >= maxFrames) {
                isAlive = false;
            }
        }
        
        public void draw(Graphics2D g2d) {
            if (!isAlive) return;
            
            Graphics2D g = (Graphics2D) g2d.create();
            
            // 폭발 크기 계산
            float progress = (float)frame / maxFrames;
            int size = (int)(20 + progress * 40);
            
            // 폭발 색상 (노란색에서 주황색으로)
            Color color = new Color(255, (int)(255 - progress * 100), 0, (int)(255 - progress * 255));
            g.setColor(color);
            
            // 폭발 그리기
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
            
            // 외곽선
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2));
            g.drawOval((int)x - size/2, (int)y - size/2, size, size);
            
            g.dispose();
        }
        
        public boolean isAlive() { return isAlive; }
    }
    
    public CombatBackground() {
        loadImages();
        allySpaceships = new ArrayList<>();
        enemyAliens = new ArrayList<>();
        shots = new ArrayList<>();
        explosions = new ArrayList<>();
        random = new Random();
        lastShotTime = System.currentTimeMillis();
        
        // 초기 유닛들 생성
        spawnInitialUnits();
    }
    
    /**
     * 이미지들을 로드합니다
     */
    private void loadImages() {
        // 배경 이미지
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/Background-0.jpg");
            if (inputStream != null) {
                backgroundImage = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
        
        // 아군 우주선
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/spaceship.png");
            if (inputStream != null) {
                allySpaceshipSprite = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("아군 우주선 스프라이트를 로드할 수 없습니다: " + e.getMessage());
        }
        
        // 적 외계인
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/alien.gif");
            if (inputStream != null) {
                enemyAlienSprite = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("적 외계인 스프라이트를 로드할 수 없습니다: " + e.getMessage());
        }
        
        // 총알
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/shot.gif");
            if (inputStream != null) {
                shotSprite = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("총알 스프라이트를 로드할 수 없습니다: " + e.getMessage());
        }
    }
    
    /**
     * 초기 유닛들을 생성합니다
     */
    private void spawnInitialUnits() {
        // 아군 우주선들 생성
        for (int i = 0; i < 3; i++) {
            double x = 700 + random.nextDouble() * 100;
            double y = 200 + random.nextDouble() * 200;
            allySpaceships.add(new AllySpaceship(x, y));
        }
        
        // 적 외계인들 생성
        for (int i = 0; i < 4; i++) {
            double x = 800 + random.nextDouble() * 200;
            double y = 150 + random.nextDouble() * 250;
            enemyAliens.add(new EnemyAlien(x, y));
        }
    }
    
    /**
     * 애니메이션을 업데이트합니다
     */
    public void update(long delta) {
        long currentTime = System.currentTimeMillis();
        
        // 새로운 아군 우주선 생성
        if (allySpaceships.size() < 3 && random.nextDouble() < 0.001) {
            double x = 900;
            double y = 200 + random.nextDouble() * 200;
            allySpaceships.add(new AllySpaceship(x, y));
        }
        
        // 새로운 적 외계인 생성
        if (enemyAliens.size() < 4 && random.nextDouble() < 0.002) {
            double x = 1000;
            double y = 150 + random.nextDouble() * 250;
            enemyAliens.add(new EnemyAlien(x, y));
        }
        
        // 총알 발사
        if (currentTime - lastShotTime > SHOT_INTERVAL && !allySpaceships.isEmpty()) {
            AllySpaceship shooter = allySpaceships.get(random.nextInt(allySpaceships.size()));
            shots.add(new Shot(shooter.getX() - 20, shooter.getY()));
            lastShotTime = currentTime;
        }
        
        // 모든 유닛들 업데이트
        for (AllySpaceship spaceship : allySpaceships) {
            spaceship.update(delta);
        }
        
        for (EnemyAlien alien : enemyAliens) {
            alien.update(delta);
        }
        
        for (Shot shot : shots) {
            shot.update(delta);
        }
        
        for (Explosion explosion : explosions) {
            explosion.update();
        }
        
        // 충돌 감지 및 처리
        checkCollisions();
        
        // 죽은 객체들 제거
        allySpaceships.removeIf(ship -> !ship.isAlive());
        enemyAliens.removeIf(alien -> !alien.isAlive());
        shots.removeIf(shot -> !shot.isAlive());
        explosions.removeIf(explosion -> !explosion.isAlive());
    }
    
    /**
     * 충돌을 감지하고 처리합니다
     */
    private void checkCollisions() {
        // 총알과 적 외계인 충돌 체크
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            Rectangle shotBounds = shot.getBounds();
            
            for (int j = enemyAliens.size() - 1; j >= 0; j--) {
                EnemyAlien alien = enemyAliens.get(j);
                if (shotBounds.intersects(alien.getBounds())) {
                    // 충돌! 폭발 효과 생성
                    explosions.add(new Explosion(alien.getX(), alien.getY()));
                    
                    // 총알과 외계인 제거
                    shots.remove(i);
                    enemyAliens.remove(j);
                    break;
                }
            }
        }
    }
    
    /**
     * 전투 배경을 그립니다
     */
    public void draw(Graphics2D g2d) {
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 128));
        g2d.fillRect(0, 0, 800, 600);
        
        // 적 외계인들 그리기 (뒤쪽)
        for (EnemyAlien alien : enemyAliens) {
            alien.draw(g2d);
        }
        
        // 아군 우주선들 그리기 (중간)
        for (AllySpaceship spaceship : allySpaceships) {
            spaceship.draw(g2d);
        }
        
        // 총알들 그리기
        for (Shot shot : shots) {
            shot.draw(g2d);
        }
        
        // 폭발 효과들 그리기 (앞쪽)
        for (Explosion explosion : explosions) {
            explosion.draw(g2d);
        }
        
        // 별 효과 추가
        drawStars(g2d);
    }
    
    /**
     * 배경에 별들을 그립니다
     */
    private void drawStars(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        
        // 고정된 별들 그리기
        int[] starX = {100, 200, 350, 500, 650, 750};
        int[] starY = {80, 150, 200, 120, 180, 250};
        
        for (int i = 0; i < starX.length; i++) {
            g2d.fillOval(starX[i], starY[i], 2, 2);
            g2d.fillOval(starX[i] + 1, starY[i] + 1, 1, 1);
        }
        
        // 반짝이는 별 효과
        long time = System.currentTimeMillis();
        g2d.setColor(new Color(255, 255, 255, (int)(128 + 127 * Math.sin(time * 0.003))));
        g2d.fillOval(300, 100, 3, 3);
        g2d.fillOval(600, 200, 2, 2);
    }
}
