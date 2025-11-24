package org.newdawn.spaceinvaders.gameplay;

import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.shop.ShopCategory;
import org.newdawn.spaceinvaders.shop.ShopItem;
import org.newdawn.spaceinvaders.common.entity.ShipEntity;
import org.newdawn.spaceinvaders.common.util.Logger;
import org.newdawn.spaceinvaders.common.util.LoggerFactory;

/**
 * 장착된 아이템 적용을 담당하는 클래스
 * Game.java의 applyEquippedItems 책임을 분리
 */
public class ItemApplier {
    private UserManager userManager;
    private String currentSpaceshipSkin = "sprites/ship.gif";
    private String currentWeaponSkin = "sprites/shot.gif";
    
    /** 로거 */
    private static final Logger logger = LoggerFactory.getLogger(ItemApplier.class);
    
    public ItemApplier(UserManager userManager) {
        this.userManager = userManager;
    }
    
    /**
     * UserManager 설정 (나중에 설정되는 경우를 위해)
     */
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
        logger.debug("ItemApplier: UserManager 업데이트됨");
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
            currentSpaceshipSkin = "sprites/ship.gif";
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
            String weaponFileName = mapWeaponIdToSkinFile(equippedWeapon.getId());
            currentWeaponSkin = "sprites/weapons/" + weaponFileName;
            logger.debug("무기 스킨 적용: " + equippedWeapon.getName() + " -> " + currentWeaponSkin);
        } else {
            currentWeaponSkin = "sprites/shot.gif";
            logger.debug("기본 무기 스킨 사용: " + currentWeaponSkin);
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
                return "spaceship_green.png";
            case "battleship":
                return "spaceship_blue.png";
            case "professor":
                return "professor.png";
            case "king":
                return "king.png";
            case "software_king":
                return "software_king.png";
            default:
                return itemId + ".png";
        }
    }
    
    /**
     * 무기 아이템 ID를 스킨 파일명으로 매핑
     */
    private String mapWeaponIdToSkinFile(String itemId) {
        switch (itemId) {
            case "laser_gun":
                return "green_laser.png";
            case "plasma_gun":
                return "plasma.png";
            case "missile_launcher":
                return "missile.png";
            case "report":
                return "report.jpg"; // 확장자가 .jpg
            case "school_logo":
                return "school_logo.png";
            case "kimbap_code":
                return "kimbap_code.png";
            case "breakfast_1000":
                return "breakfast_1000.png";
            default:
                // 기본값으로 아이템 ID + .png 사용
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

