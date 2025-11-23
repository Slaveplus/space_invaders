package org.newdawn.spaceinvaders.multyplay.core;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * 게임플레이 배경 렌더러: 배경 이미지를 한 번만 로드/가속화하고 매 프레임 재사용합니다.
 */
public class BackgroundRenderer {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private volatile Image cachedBackground; // 가속화된 이미지
    private String resourcePath = SpriteConstants.BACKGROUND_2_JPG;

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
        }
    }

    /**
     * 이미지가 없으면 로드하고, 화면 호환 가속 이미지로 변환합니다.
     */
    private void ensureLoaded(Graphics2D g) {
        if (cachedBackground != null) return;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return;
            }
            BufferedImage src = ImageIO.read(is);
            GraphicsConfiguration gc = g.getDeviceConfiguration();
            // 화면 크기에 맞게 사전 스케일링하여 가속 이미지로 저장
            Image compatible = gc.createCompatibleImage(WIDTH, HEIGHT, Transparency.OPAQUE);
            Graphics2D ig = (Graphics2D) compatible.getGraphics();
            ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ig.drawImage(src, 0, 0, WIDTH, HEIGHT, null);
            ig.dispose();
            cachedBackground = compatible;
        } catch (Exception ignored) {
            // 상위에서 안전망 배경 처리
        }
    }
}
