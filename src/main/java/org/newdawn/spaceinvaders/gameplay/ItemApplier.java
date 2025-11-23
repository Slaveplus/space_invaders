package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.shop.ShopCategory;
import org.newdawn.spaceinvaders.shop.ShopItem;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.util.Logger;
import org.newdawn.spaceinvaders.common.util.LoggerFactory;
import org.newdawn.spaceinvaders.common.sprite.SpriteConstants;

/**
 * 장착된 아이템 적용을 담당하는 클래스
 * Game.java의 applyEquippedItems 책임을 분리
 */
public class ItemApplier {
    private final UserManager userManager;
    private String currentSpaceshipSkin = SpriteConstants.SHIP_GIF;
    private String currentWeaponSkin = SpriteConstants.SHOT_GIF;
    
    /** 로거 */
    private static final Logger logger = LoggerFactory.getLogger(ItemApplier.class);
    
    public ItemApplier(UserManager userManager) {
        this.userManager = userManager;
    }
    
    /**
     * 장착된 아이템 적용
     */
    public void applyEquippedItems(ShipEntity ship) {
        if (userManager == null) {
            return;
        }
        
        applySpaceshipSkin(ship);
        applyWeaponSkin();
        applyPowerupEffects();
    }
    
    /**
     * 우주선 스킨 적용
     */
    public void applySpaceshipSkin(ShipEntity ship) {
        if (userManager == null) return;
        
        ShopItem equippedSpaceship = userManager.getShopManager().getEquippedItem(ShopCategory.SPACESHIPS);
        if (equippedSpaceship != null) {
            String skinFileName = mapItemIdToSkinFile(equippedSpaceship.getId());
            currentSpaceshipSkin = "sprites/ships/" + skinFileName;
            logger.debug("스킨 적용: " + equippedSpaceship.getName() + " -> " + currentSpaceshipSkin);
            
            if (ship != null) {
                ship.changeSkin(currentSpaceshipSkin);
            }
        } else {
            currentSpaceshipSkin = SpriteConstants.SHIP_GIF;
            logger.debug("기본 스킨 사용: " + currentSpaceshipSkin);
        }
    }
    
    /**
     * 무기 스킨 적용
     */
    public void applyWeaponSkin() {
        if (userManager == null) return;
        
        ShopItem equippedWeapon = userManager.getShopManager().getEquippedItem(ShopCategory.WEAPONS);
        if (equippedWeapon != null) {
            currentWeaponSkin = "sprites/weapons/" + equippedWeapon.getId() + ".png";
        }
    }
    
    /**
     * 파워업 효과 적용
     */
    private void applyPowerupEffects() {
        if (userManager == null) return;
        // 파워업 효과 적용 로직
    }
    
    /**
     * 아이템 ID를 스킨 파일명으로 매핑
     */
    private String mapItemIdToSkinFile(String itemId) {
        switch (itemId) {
            case "fighter_ship":
                return SpriteConstants.SHIPS_SPACESHIP_GREEN_PNG.substring("sprites/ships/".length());
            case "battleship":
                return SpriteConstants.SHIPS_SPACESHIP_BLUE_PNG.substring("sprites/ships/".length());
            case "professor":
                return SpriteConstants.SHIPS_PROFESSOR_PNG.substring("sprites/ships/".length());
            case "king":
                return SpriteConstants.SHIPS_KING_PNG.substring("sprites/ships/".length());
            case "software_king":
                return SpriteConstants.SHIPS_SOFTWARE_KING_PNG.substring("sprites/ships/".length());
            default:
                return itemId + ".png";
        }
    }
    
    public String getCurrentSpaceshipSkin() {
        return currentSpaceshipSkin;
    }
    
    public String getCurrentWeaponSkin() {
        return currentWeaponSkin;
    }
    
    public void setCurrentSpaceshipSkin(String skin) {
        this.currentSpaceshipSkin = skin;
    }
    
    public void setCurrentWeaponSkin(String skin) {
        this.currentWeaponSkin = skin;
    }
}

