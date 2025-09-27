package org.newdawn.spaceinvaders.shop;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * 메인 상점 클래스
 * 확장 가능한 상점 시스템의 진입점
 */
public class Shop {
    private ShopManager shopManager;
    private ShopRenderer shopRenderer;
    private ShopInputHandler inputHandler;
    private BufferedImage backgroundImage;
    
    public Shop() {
        this.shopManager = new ShopManager();
        this.shopRenderer = new ShopRenderer();
        this.inputHandler = new ShopInputHandler(shopManager);
        loadBackgroundImage();
        initializeShopItems();
    }
    
    // UserManager 설정 (실시간 DB 동기화용)
    public void setUserManager(org.newdawn.spaceinvaders.login.UserManager userManager) {
        this.shopManager.setUserManager(userManager);
        this.shopRenderer.setUserManager(userManager);
    }
    
    // 상점 상태 설정 (인벤토리 직접 접근용)
    public void setCurrentState(ShopState state) {
        this.shopManager.setCurrentState(state);
    }
    
    private void loadBackgroundImage() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/Background-0.jpg");
            if (inputStream != null) {
                backgroundImage = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void initializeShopItems() {
        // 무기 아이템들
        ShopItem laserGun = new ShopItem("laser_gun", "레이저 건", "기본 레이저 무기", 100, 
                                       ShopCategory.WEAPONS, ItemRarity.COMMON);
        laserGun.setIconPath("sprites/weapons/green_laser.png");
        shopManager.addItem(laserGun);
        
        ShopItem plasmaGun = new ShopItem("plasma_gun", "플라즈마 건", "강력한 플라즈마 무기", 300, 
                                       ShopCategory.WEAPONS, ItemRarity.RARE);
        plasmaGun.setIconPath("sprites/weapons/plasma.png");
        shopManager.addItem(plasmaGun);
        
        ShopItem missileLauncher = new ShopItem("missile_launcher", "미사일 발사기", "원거리 미사일 무기", 500, 
                                       ShopCategory.WEAPONS, ItemRarity.EPIC);
        missileLauncher.setIconPath("sprites/weapons/missile.png");
        shopManager.addItem(missileLauncher);

        ShopItem report = new ShopItem("report", "레포트 폭탄", "재앙이 몰려온다....", 2000, 
                                       ShopCategory.WEAPONS, ItemRarity.LEGENDARY);
        report.setIconPath("sprites/weapons/report.jpg");
        shopManager.addItem(report);
        
        // 우주선 아이템들
        ShopItem fighterShip = new ShopItem("fighter_ship", "Green SpaceShip", "기깔나는 초록색 전투기", 200, 
                                       ShopCategory.SPACESHIPS, ItemRarity.COMMON);
        fighterShip.setIconPath("sprites/ships/spaceship_green.png");
        shopManager.addItem(fighterShip);
        
        ShopItem battleship = new ShopItem("battleship", "Blue SpaceShip", "모두가 부러워하는 파란색 전투기", 200, 
                                       ShopCategory.SPACESHIPS, ItemRarity.RARE);
        battleship.setIconPath("sprites/ships/spaceship_blue.png");
        shopManager.addItem(battleship);
        
        ShopItem destroyer = new ShopItem("destroyer", "평생지도교수님", "개씹초희귀 킹갓제너럴", 1500, 
                                       ShopCategory.SPACESHIPS, ItemRarity.LEGENDARY);
        destroyer.setIconPath("sprites/ships/professor.png");
        shopManager.addItem(destroyer);
        
        // 파워업 아이템들
        ShopItem healthBoost = new ShopItem("health_boost", "체력 부스트", "체력을 50% 증가", 150, 
                                       ShopCategory.POWERUPS, ItemRarity.COMMON);
        healthBoost.setIconPath("sprites/powerups/health_boost.png");
        shopManager.addItem(healthBoost);
        ShopItem speedBoost = new ShopItem("speed_boost", "속도 부스트", "이동 속도를 30% 증가", 200, 
                                       ShopCategory.POWERUPS, ItemRarity.RARE);
        speedBoost.setIconPath("sprites/powerups/speed_boost.png");
        shopManager.addItem(speedBoost);
        
        ShopItem shield = new ShopItem("shield", "방어막", "일정 시간 무적", 300, 
                                       ShopCategory.POWERUPS, ItemRarity.EPIC);
        shield.setIconPath("sprites/powerups/shield.png");
        shopManager.addItem(shield);
        
        // 장식품 아이템들
        ShopItem neonLights = new ShopItem("neon_lights", "네온 라이트", "우주선에 네온 장식", 100, 
                                       ShopCategory.DECORATIONS, ItemRarity.COMMON);
        neonLights.setIconPath("sprites/decorations/neon_lights.png");
        shopManager.addItem(neonLights);
        
        ShopItem goldenPaint = new ShopItem("golden_paint", "골든 페인트", "황금색 도색", 250, 
                                       ShopCategory.DECORATIONS, ItemRarity.RARE);
        goldenPaint.setIconPath("sprites/decorations/golden_paint.png");
        shopManager.addItem(goldenPaint);
        
        ShopItem rainbowTrail = new ShopItem("rainbow_trail", "무지개 궤적", "무지개색 궤적 효과", 400, 
                                       ShopCategory.DECORATIONS, ItemRarity.EPIC);
        rainbowTrail.setIconPath("sprites/decorations/rainbow_trail.png");
        shopManager.addItem(rainbowTrail);
        
        // 업그레이드 아이템들
        ShopItem engineUpgrade = new ShopItem("engine_upgrade", "엔진 업그레이드", "엔진 성능 향상", 300, 
                                       ShopCategory.UPGRADES, ItemRarity.COMMON);
        engineUpgrade.setIconPath("sprites/upgrades/engine_upgrade.png");
        shopManager.addItem(engineUpgrade);
        
        ShopItem weaponUpgrade = new ShopItem("weapon_upgrade", "무기 업그레이드", "무기 성능 향상", 500, 
                                       ShopCategory.UPGRADES, ItemRarity.RARE);
        weaponUpgrade.setIconPath("sprites/upgrades/weapon_upgrade.png");
        shopManager.addItem(weaponUpgrade);
        
        ShopItem shieldUpgrade = new ShopItem("shield_upgrade", "방어막 업그레이드", "방어막 성능 향상", 700, 
                                       ShopCategory.UPGRADES, ItemRarity.EPIC);
        shieldUpgrade.setIconPath("sprites/upgrades/shield_upgrade.png");
        shopManager.addItem(shieldUpgrade);
    }
    
    public void handleKeyInput(int keyCode) {
        inputHandler.handleInput(keyCode);
    }
    
    public void handleMouseClick(int x, int y) {
        inputHandler.handleMouseClick(x, y);
    }
    
    public void update() {
        // 메시지 타이머 업데이트
        shopManager.updateMessageTimer();
    }
    
    public void draw(Graphics2D g2d) {
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, 800, 600);
        
        shopRenderer.draw(g2d, shopManager, inputHandler);
    }
    
    public boolean shouldExitShop() {
        return inputHandler.isExitRequested();
    }
    
    public void reset() {
        shopManager.setCurrentState(ShopState.MAIN);
        inputHandler.reset();
    }
}