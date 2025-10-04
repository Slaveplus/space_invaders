package org.newdawn.spaceinvaders.shop;

import java.util.List;
import java.util.ArrayList;

/**
 * 상점 아이템 클래스
 * 확장 가능한 아이템 시스템
 */
public class ShopItem {
    private String id;
    private String name;
    private String description;
    private int basePrice;
    private ShopCategory category;
    private ItemRarity rarity;
    private boolean isPurchased;
    private boolean isAvailable;
    private List<ItemEffect> effects;
    private String iconPath;
    private int level;
    private int maxLevel;
    private String requiredSpaceshipId; // 전용 우주선 ID (null이면 모든 우주선 호환)
    
    public ShopItem(String id, String name, String description, int basePrice, 
                   ShopCategory category, ItemRarity rarity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.category = category;
        this.rarity = rarity;
        this.effects = new ArrayList<>();
        this.isPurchased = false;
        this.isAvailable = true;
        this.level = 1;
        this.maxLevel = 1;
        this.requiredSpaceshipId = null; // 기본값: 모든 우주선 호환
    }
    
    // 확장 가능한 메서드들
    public int getFinalPrice() {
        return (int)(basePrice * rarity.getPriceMultiplier());
    }
    
    public void addEffect(ItemEffect effect) {
        effects.add(effect);
    }
    
    public boolean canUpgrade() {
        return level < maxLevel;
    }
    
    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getBasePrice() { return basePrice; }
    public int getPrice() { return getFinalPrice(); }
    public ShopCategory getCategory() { return category; }
    public ItemRarity getRarity() { return rarity; }
    public boolean isPurchased() { return isPurchased; }
    public boolean isAvailable() { return isAvailable; }
    public List<ItemEffect> getEffects() { return effects; }
    public String getIconPath() { return iconPath; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    
    public void setPurchased(boolean purchased) { this.isPurchased = purchased; }
    public void setAvailable(boolean available) { this.isAvailable = available; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }
    
    // 전용 우주선 관련 메서드들
    public String getRequiredSpaceshipId() { return requiredSpaceshipId; }
    public void setRequiredSpaceshipId(String requiredSpaceshipId) { this.requiredSpaceshipId = requiredSpaceshipId; }
    
    /**
     * 특정 우주선과 호환되는지 확인
     */
    public boolean isCompatibleWith(ShopItem spaceship) {
        if (requiredSpaceshipId == null) {
            return true; // 전용 우주선이 없으면 모든 우주선과 호환
        }
        return spaceship != null && requiredSpaceshipId.equals(spaceship.getId());
    }
}