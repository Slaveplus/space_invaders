package org.newdawn.spaceinvaders.multyplay.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;

/**
 * Draws the multiplayer backdrop.
 */
public class MultiBackgroundRenderer {
    private final Image background;

    public MultiBackgroundRenderer(String path) {
        this.background = Toolkit.getDefaultToolkit().getImage(path);
    }

    public void render(Graphics2D g, int width, int height) {
        if (background != null) {
            g.drawImage(background, 0, 0, width, height, null);
        }
    }
}
