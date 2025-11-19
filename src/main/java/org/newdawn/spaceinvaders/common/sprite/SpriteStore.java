package org.newdawn.spaceinvaders.common.sprite;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 * 싱글/멀티 공통 스프라이트 캐시.
 */
public class SpriteStore {
    private static final SpriteStore INSTANCE = new SpriteStore();

    public static SpriteStore get() {
        return INSTANCE;
    }

    private final HashMap<String, Sprite> sprites = new HashMap<>();

    public Sprite getSprite(String ref) {
        Sprite cached = sprites.get(ref);
        if (cached != null) {
            return cached;
        }

        BufferedImage sourceImage = loadImage(ref);
        BufferedImage sizedImage = resizeImageForGameplay(sourceImage, ref);
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        Image image = gc.createCompatibleImage(sizedImage.getWidth(), sizedImage.getHeight(), Transparency.BITMASK);
        image.getGraphics().drawImage(sizedImage, 0, 0, null);

        Sprite sprite = new Sprite(image);
        sprites.put(ref, sprite);
        return sprite;
    }

    private BufferedImage loadImage(String ref) {
        try {
            URL url = getClass().getClassLoader().getResource(ref);
            if (url == null) {
                fail("Can't find ref: " + ref);
            }
            return ImageIO.read(url);
        } catch (IOException e) {
            fail("Failed to load: " + ref);
            return null; // never reached
        }
    }

    private BufferedImage resizeImageForGameplay(BufferedImage sourceImage, String ref) {
        int targetWidth;
        int targetHeight;

        if (ref.contains("ships/")) {
            targetWidth = 64;
            targetHeight = 64;
        } else if (ref.contains("weapons/")) {
            targetWidth = 32;
            targetHeight = 32;
        } else if (ref.contains("shot.gif")) {
            targetWidth = 16;
            targetHeight = 16;
        } else if (ref.contains("alien")) {
            targetWidth = 48;
            targetHeight = 48;
        } else {
            return sourceImage;
        }

        if (sourceImage.getWidth() == targetWidth && sourceImage.getHeight() == targetHeight) {
            return sourceImage;
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    private void fail(String message) {
        System.err.println(message);
        System.exit(0);
    }
}
