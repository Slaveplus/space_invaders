package org.newdawn.spaceinvaders.multyplay.ui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * 멀티플레이 배경 렌더러 (싱글 BackgroundRenderer 포크)
 * - 향후 라운드/스테이지 번호에 따른 배경 전환 고려
 */
public class MultiBackgroundRenderer {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private volatile Image cachedBackground; // 가속화된 이미지
    private String resourcePath = "sprites/backgrounds/Background-2.jpg";

    public MultiBackgroundRenderer() {}

    public void setResourcePath(String resourcePath) {
        if (resourcePath != null && !resourcePath.isEmpty() && !resourcePath.equals(this.resourcePath)) {
            this.resourcePath = resourcePath;
            this.cachedBackground = null; // 다음 draw 시 다시 로드
        }
    }

    public void draw(Graphics2D g) {
        ensureLoaded(g);
        if (cachedBackground != null) {
            g.drawImage(cachedBackground, 0, 0, WIDTH, HEIGHT, null);
        } else {
            g.setColor(Color.black);
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void ensureLoaded(Graphics2D g) {
        if (cachedBackground != null) return;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) { return; }
            BufferedImage src = ImageIO.read(is);
            GraphicsConfiguration gc = g.getDeviceConfiguration();
            Image compatible = gc.createCompatibleImage(WIDTH, HEIGHT, Transparency.OPAQUE);
            Graphics2D ig = (Graphics2D) compatible.getGraphics();
            ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ig.drawImage(src, 0, 0, WIDTH, HEIGHT, null);
            ig.dispose();
            cachedBackground = compatible;
        } catch (Exception ignored) { }
    }
}
