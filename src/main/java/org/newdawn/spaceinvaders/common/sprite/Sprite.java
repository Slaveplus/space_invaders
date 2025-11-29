package org.newdawn.spaceinvaders.common.sprite;

import java.awt.Graphics;
import java.awt.Image;

/**
 * 화면에 표시될 스프라이트. 상태는 없고 이미지만을 보관한다.
 */
public class Sprite {
    /** 스프라이트로 사용할 이미지 */
    private final Image image;

    public Sprite(Image image) {
        this.image = image;
    }

    public int getWidth() {
        return image.getWidth(null);
    }

    public int getHeight() {
        return image.getHeight(null);
    }

    public Image getImage() {
        return image;
    }

    public void draw(Graphics g, int x, int y) {
        g.drawImage(image, x, y, null);
    }

    public void draw(Graphics g, int x, int y, int width, int height) {
        g.drawImage(image, x, y, width, height, null);
    }
}
