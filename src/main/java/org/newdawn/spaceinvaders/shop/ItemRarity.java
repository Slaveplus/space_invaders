package org.newdawn.spaceinvaders.shop;

import java.awt.Color;
/**
 * 아이템 등급 시스템
 * 향후 새로운 등급 추가 가능
 */
public enum ItemRarity {
    COMMON("일반", Color.WHITE, 1.0f),
    UNCOMMON("고급", Color.GREEN, 1.2f),
    RARE("레어", Color.BLUE, 1.5f),
    EPIC("에픽", Color.MAGENTA, 2.0f),
    LEGENDARY("전설", Color.ORANGE, 3.0f),
    MYTHIC("신화", Color.RED, 5.0f);
    
    private final String displayName;
    private final Color color;
    private final float priceMultiplier;
    
    ItemRarity(String displayName, Color color, float priceMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.priceMultiplier = priceMultiplier;
    }
    
    public String getDisplayName() { return displayName; }
    public Color getColor() { return color; }
    public float getPriceMultiplier() { return priceMultiplier; }
}