package org.newdawn.spaceinvaders.shop;

/**
 * 상점 카테고리 열거형
 * 향후 새로운 카테고리 추가 가능
 */
public enum ShopCategory {
    WEAPONS("무기", "다양한 무기류", "weapon_icon.png"),
    SPACESHIPS("우주선", "우주선 및 함선", "ship_icon.png"),
    POWERUPS("파워업", "임시 효과 아이템", "powerup_icon.png"),
    DECORATIONS("장식품", "외관 커스터마이징", "cosmetic_icon.png"),
    UPGRADES("업그레이드", "영구적 능력 향상", "upgrade_icon.png"),
    CONSUMABLES("소모품", "일회용 아이템", "consumable_icon.png"),
    SPECIAL("특별", "이벤트 및 한정 아이템", "special_icon.png");
    
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