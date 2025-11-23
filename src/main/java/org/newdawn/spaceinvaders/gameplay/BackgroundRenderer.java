package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import org.newdawn.spaceinvaders.common.util.Logger;
import org.newdawn.spaceinvaders.common.util.LoggerFactory;

/**
 * 게임플레이 배경 렌더러: 배경 이미지를 한 번만 로드/가속화하고 매 프레임 재사용합니다.
 */
public class BackgroundRenderer {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private volatile BufferedImage cachedBackground; // 가속화된 이미지
    private String resourcePath = SpriteConstants.BACKGROUND_2_JPG;
    
    /** 로거 */
    private static final Logger logger = LoggerFactory.getLogger(BackgroundRenderer.class);

    public BackgroundRenderer() { }

    public BackgroundRenderer(String resourcePath) {
        if (resourcePath != null && !resourcePath.isEmpty()) {
            this.resourcePath = resourcePath;
        }
    }

    /**
     * 배경 리소스를 교체합니다(다음 draw 시 로드/갱신).
     */
    public void setResourcePath(String resourcePath) {
        if (resourcePath != null && !resourcePath.isEmpty() && !resourcePath.equals(this.resourcePath)) {
            logger.debug("BackgroundRenderer: 배경 경로 변경 " + this.resourcePath + " -> " + resourcePath);
            this.resourcePath = resourcePath;
            this.cachedBackground = null; // 다음 그리기 때 다시 로드
        }
    }

    /**
     * 배경을 그립니다. 필요 시 한 번만 로드/가속화합니다.
     */
    public void draw(Graphics2D g) {
        ensureLoaded(g);
        if (cachedBackground != null) {
            g.drawImage(cachedBackground, 0, 0, WIDTH, HEIGHT, null);
        } else {
            // 안전망: 로드 실패 시 블랙 배경
            g.setColor(Color.black);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            logger.error("BackgroundRenderer: 배경 로드 실패, 블랙 배경 표시 - " + resourcePath);
        }
    }

    /**
     * 이미지가 없으면 로드하고, 화면 호환 가속 이미지로 변환합니다.
     */
    private void ensureLoaded(Graphics2D g) {
        if (cachedBackground != null) return;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.error("BackgroundRenderer: 이미지를 찾을 수 없습니다 - " + resourcePath);
                return;
            }
            BufferedImage src = ImageIO.read(is);
            if (src == null) {
                logger.error("BackgroundRenderer: 이미지 로드 실패 - " + resourcePath);
                return;
            }
            logger.debug("BackgroundRenderer: 이미지 로드 성공 - " + resourcePath + " (" + src.getWidth() + "x" + src.getHeight() + ")");
            
            // 더 간단한 방법: BufferedImage를 직접 사용
            cachedBackground = src;
            logger.debug("BackgroundRenderer: 배경 이미지 캐시 완료 - " + resourcePath);
        } catch (Exception e) {
            logger.error("BackgroundRenderer: 이미지 로드 중 오류 발생 - " + resourcePath + " - " + e.getMessage(), e);
        }
    }
}
