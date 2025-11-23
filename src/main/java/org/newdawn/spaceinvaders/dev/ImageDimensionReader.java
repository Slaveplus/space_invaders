package org.newdawn.spaceinvaders.dev;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageDimensionReader {
    public static void main(String[] args) {
        String[] files = {
            "sprites/Boss_Attack/2round.gif",
            "sprites/Boss_Attack/2round1.gif",
            "sprites/Boss_Attack/2round2.gif",
            "sprites/Boss_Attack/4round3.gif",
            "sprites/Boss_Attack/5round1.gif",
            "sprites/Boss_Attack/ice.gif",
            "sprites/Boss_Attack/ice ball.gif"
        };
        System.out.println("Reading image dimensions:");
        for (String filePath : files) {
            try (InputStream is = ImageDimensionReader.class.getClassLoader().getResourceAsStream(filePath)) {
                if (is == null) {
                    System.out.println(filePath + ": Not found");
                    continue;
                }
                BufferedImage image = ImageIO.read(is);
                if (image != null) {
                    System.out.println(filePath + ": " + image.getWidth() + "x" + image.getHeight());
                } else {
                    System.out.println(filePath + ": Could not read image");
                }
            } catch (IOException e) {
                System.out.println(filePath + ": Error reading image - " + e.getMessage());
            }
        }
    }
}
