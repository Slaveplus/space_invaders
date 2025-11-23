package org.newdawn.spaceinvaders.mainmenu;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * 메인 메뉴에서 사용하는 폰트/배경 등 리소스를 로드하고 보관한다.
 * 리소스 로딩 책임을 한 곳으로 모아 MainMenu의 God Object 스멜을 줄인다.
 */
public final class MainMenuAssets {
    private static final String FONT_RESOURCE = "fonts/Kostar.ttf";
    private static final String BACKGROUND_RESOURCE = SpriteConstants.BACKGROUND_0_JPG;
    private static final Font SHARED_BASE_FONT = loadBaseFont();

    private final BufferedImage backgroundImage;
    private final Font titleFont;
    private final Font menuFont;
    private final Font submenuFont;

    private MainMenuAssets(BufferedImage backgroundImage, Font titleFont, Font menuFont, Font submenuFont) {
        this.backgroundImage = backgroundImage;
        this.titleFont = titleFont;
        this.menuFont = menuFont;
        this.submenuFont = submenuFont;
    }

    public static MainMenuAssets load() {
        Font baseFont = SHARED_BASE_FONT;
        BufferedImage background = loadBackground();

        Font title = baseFont.deriveFont(Font.BOLD, 48f);
        Font menu = baseFont.deriveFont(Font.BOLD, 24f);
        Font submenu = baseFont.deriveFont(Font.BOLD, 20f);

        return new MainMenuAssets(background, title, menu, submenu);
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public Font getTitleFont() {
        return titleFont;
    }

    public Font getMenuFont() {
        return menuFont;
    }

    public Font getSubmenuFont() {
        return submenuFont;
    }

    static Font sharedBaseFont() {
        return SHARED_BASE_FONT;
    }

    private static Font loadBaseFont() {
        try (InputStream fontStream = MainMenu.class.getClassLoader().getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream != null) {
                return Font.createFont(Font.TRUETYPE_FONT, fontStream);
            }
            System.err.println("MainMenuAssets: 폰트 파일을 찾을 수 없어 기본 폰트를 사용합니다.");
        } catch (Exception e) {
            System.err.println("MainMenuAssets: 폰트 로드 실패 - " + e.getMessage());
        }
        return new Font("Arial", Font.PLAIN, 12);
    }

    private static BufferedImage loadBackground() {
        try (InputStream inputStream = MainMenu.class.getClassLoader().getResourceAsStream(BACKGROUND_RESOURCE)) {
            if (inputStream != null) {
                return ImageIO.read(inputStream);
            }
            System.err.println("MainMenuAssets: 배경 이미지를 찾을 수 없어 기본 배경을 생성합니다.");
        } catch (IOException e) {
            System.err.println("MainMenuAssets: 배경 로드 실패 - " + e.getMessage());
        }
        return createFallbackBackground();
    }

    private static BufferedImage createFallbackBackground() {
        BufferedImage fallback = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = fallback.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, fallback.getWidth(), fallback.getHeight());
        g2d.dispose();
        return fallback;
    }
}

