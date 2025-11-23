package org.newdawn.spaceinvaders.shop;

import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;

/**
 * 상점 카테고리 열거형
 * 향후 새로운 카테고리 추가 가능
 */
public enum ShopCategory {
    WEAPONS("무기", "다양한 무기류", SpriteConstants.WEAPON_ICON_PNG),
    SPACESHIPS("우주선", "우주선 및 함선", SpriteConstants.SHIP_ICON_PNG),
    POWERUPS("파워업", "임시 효과 아이템", SpriteConstants.POWERUP_ICON_PNG),
    DECORATIONS("장식품", "외관 커스터마이징", SpriteConstants.COSMETIC_ICON_PNG),
    UPGRADES("업그레이드", "영구적 능력 향상", SpriteConstants.UPGRADE_ICON_PNG),
    CONSUMABLES("소모품", "일회용 아이템", SpriteConstants.CONSUMABLE_ICON_PNG),
    SPECIAL("특별", "이벤트 및 한정 아이템", SpriteConstants.SPECIAL_ICON_PNG);
    
    private final String displayName;
    private final String description;
    private final String iconPath;
    
    ShopCategory(String displayName, String description, String iconPath) {
        this.displayName = displayName;
        this.description = description;
        this.iconPath = iconPath;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }
}