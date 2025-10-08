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
        this.inputHandler.setShop(this); // ShopInputHandler에 Shop 참조 설정
        this.shopManager.setInputHandler(inputHandler); // ShopManager에 InputHandler 설정
        loadBackgroundImage();
        initializeShopItems();
    }
    
    public Shop(org.newdawn.spaceinvaders.login.UserManager userManager) {
        this.shopManager = new ShopManager(userManager);
        this.shopRenderer = new ShopRenderer();
        this.shopRenderer.setUserManager(userManager); // ShopRenderer에도 UserManager 설정
        this.inputHandler = new ShopInputHandler(shopManager);
        this.inputHandler.setShop(this); // ShopInputHandler에 Shop 참조 설정
        this.shopManager.setInputHandler(inputHandler); // ShopManager에 InputHandler 설정
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
    
    // 인벤토리 입력 처리 (MainMenu에서 호출)
    public void handleInventoryInput(int keyCode) {
        this.inputHandler.handleInput(keyCode);
    }
    
    // ShopManager 접근자 (MainMenu에서 인벤토리 새로고침용)
    public ShopManager getShopManager() {
        return this.shopManager;
    }
    
    private void loadBackgroundImage() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/backgrounds/Background-0.jpg");
            if (inputStream != null) {
                backgroundImage = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }
    
    public void initializeShopItems() {
        // ShopManager의 static 메서드 사용
        ShopManager.initializeDefaultItems(shopManager);
    }
    
    public void render(Graphics2D g2d) {
        // 배경 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 상점 렌더링
        shopRenderer.draw(g2d, shopManager, inputHandler);
    }
    
    public void update() {
        // 상점 업데이트 로직
        shopManager.updateMessageTimer();
        shopManager.updateWarningTimer();
    }
    
    public void reset() {
        // ShopInputHandler 리셋
        if (inputHandler != null) {
            inputHandler.reset();
        }
        // ShopManager 상태 리셋
        if (shopManager != null) {
            shopManager.setCurrentState(org.newdawn.spaceinvaders.shop.ShopState.MAIN);
        }
        // 애니메이션 리셋
        if (shopRenderer != null) {
            shopRenderer.reset();
        }
    }
    
    /**
     * 상점 진입 애니메이션 시작
     */
    public void startEntryAnimation() {
        if (shopRenderer != null) {
            shopRenderer.startEntryAnimation();
        }
    }
    
    /**
     * 상점 나가기 애니메이션 시작
     */
    public void startExitAnimation() {
        if (shopRenderer != null) {
            shopRenderer.startExitAnimation();
        }
    }
    
    /**
     * 카테고리 상점 진입 애니메이션 시작
     */
    public void startCategoryEntryAnimation() {
        if (shopRenderer != null) {
            shopRenderer.startCategoryEntryAnimation();
        }
    }
    
    /**
     * 카테고리 상점 나가기 애니메이션 시작
     */
    public void startCategoryExitAnimation() {
        if (shopRenderer != null) {
            shopRenderer.startCategoryExitAnimation();
        }
    }
    
    /**
     * 인벤토리 진입 애니메이션 시작 (위에서 아래로)
     */
    public void startInventoryEntryAnimation() {
        if (shopRenderer != null) {
            shopRenderer.startInventoryEntryAnimation();
        }
    }
    
    /**
     * 인벤토리 나가기 애니메이션 시작 (아래에서 위로)
     */
    public void startInventoryExitAnimation() {
        if (shopRenderer != null) {
            shopRenderer.startInventoryExitAnimation();
        }
    }
    
    /**
     * 애니메이션 중인지 확인
     */
    public boolean isAnimating() {
        return shopRenderer != null && shopRenderer.isAnimating();
    }
    
    /**
     * 카테고리 애니메이션 중인지 확인
     */
    public boolean isCategoryAnimating() {
        return shopRenderer != null && shopRenderer.isCategoryAnimating();
    }
}