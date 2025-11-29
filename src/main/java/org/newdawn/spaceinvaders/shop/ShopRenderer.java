package org.newdawn.spaceinvaders.shop;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.login.UserManager;

/**
 * 상점 렌더링 클래스
 * 확장 가능한 렌더링 시스템
 */
public class ShopRenderer {
    private static final String DEFAULT_UI_FONT_NAME = "SansSerif";
    // 상수 정의
    
    private Font titleFont;
    private Font menuFont;
    private Font itemFont;
    private Font descriptionFont;
    private UserManager userManager;
    private BufferedImage backgroundImage;
    private BufferedImage coinImage;
    private BufferedImage itemBoxImage;
    private Map<String, BufferedImage> itemImageCache;
    
    // 애니메이션 시스템 추가
    private ShopAnimation animation;
    private ShopAnimation categoryAnimation; // 카테고리 상점용 애니메이션
    
    public ShopRenderer() {
        initializeFonts();
        loadBackgroundImage();
        loadCoinImage();
        loadItemBoxImage();
        itemImageCache = new HashMap<>();
        this.userManager = null;
        this.animation = new ShopAnimation();
        this.categoryAnimation = new ShopAnimation();
    }
    
    // UserManager 설정 (실시간 데이터 표시용)
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
    
    // 실시간 코인 데이터 가져오기
    private int getCurrentCoins() {
        if (userManager != null && userManager.isLoggedIn() && userManager.getCurrentUser() != null) {
            int coins = userManager.getCurrentUser().getCoins();
            System.out.println("ShopRenderer: 현재 코인 " + coins + "개 표시");
            return coins;
        } else {
            System.out.println("ShopRenderer: UserManager 또는 사용자 정보 없음 - 코인 0개 표시");
            return 0; // 기본값
        }
    }
    
    // 실시간 젬 데이터 가져오기
    // private int getCurrentGems() {
    //     if (userManager != null && userManager.isLoggedIn() && userManager.getCurrentUser() != null) {
    //         return userManager.getCurrentUser().getGems();
    //     }
    //     return 0; // 기본값
    // }
    
    private void initializeFonts() {
        try {
            // "Kostar" 폰트가 등록되었으므로 이름으로 직접 사용
            Font kostarFont = new Font("Kostar", Font.PLAIN, 12);
            // 폰트가 정상적으로 로드되었는지 확인
            if (kostarFont.getFontName().startsWith("Kostar")) {
                titleFont = kostarFont.deriveFont(Font.BOLD, 36f);
                menuFont = kostarFont.deriveFont(Font.BOLD, 20f);
                itemFont = kostarFont.deriveFont(Font.BOLD, 16f);
                descriptionFont = kostarFont.deriveFont(Font.PLAIN, 12f);
            } else {
                throw new Exception("Kostar font not found.");
            }
        } catch (Exception e) {
            System.err.println("Kostar 폰트를 찾을 수 없습니다. OS 기본 폰트를 사용합니다: " + e.getMessage());
            // OS 독립적인 "SansSerif"를 대체 폰트로 사용
            initializeDefaultFonts();
        }
    }
    
    private void initializeDefaultFonts() {
        titleFont = new Font(DEFAULT_UI_FONT_NAME, Font.BOLD, 36);
        menuFont = new Font(DEFAULT_UI_FONT_NAME, Font.BOLD, 20);
        itemFont = new Font(DEFAULT_UI_FONT_NAME, Font.BOLD, 16);
        descriptionFont = new Font(DEFAULT_UI_FONT_NAME, Font.PLAIN, 12);
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
    
    private void loadCoinImage() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/star coin normal.png");
            if (inputStream != null) {
                coinImage = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("코인 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void loadItemBoxImage() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/item_box.png");
            if (inputStream != null) {
                itemBoxImage = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("아이템 박스 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
    }
    
    /**
     * 아이템 이미지를 로드합니다 (캐시 사용)
     */
    private BufferedImage loadItemImage(String iconPath) {
        if (iconPath == null || iconPath.isEmpty()) {
            return null;
        }
        
        // 캐시에서 확인
        if (itemImageCache.containsKey(iconPath)) {
            return itemImageCache.get(iconPath);
        }
        
        // 이미지 로드
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(iconPath);
            if (inputStream != null) {
                BufferedImage image = ImageIO.read(inputStream);
                itemImageCache.put(iconPath, image);
                return image;
            }
        } catch (IOException e) {
            System.err.println("아이템 이미지를 로드할 수 없습니다: " + iconPath + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 애니메이션 시작 (상점 진입 시 호출)
     */
    public void startEntryAnimation() {
        animation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
    }
    
    /**
     * 애니메이션 시작 (상점 나가기 시 호출)
     */
    public void startExitAnimation() {
        animation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
    }
    
    /**
     * 카테고리 상점 진입 애니메이션 시작
     */
    public void startCategoryEntryAnimation() {
        categoryAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
    }
    
    /**
     * 카테고리 상점 나가기 애니메이션 시작
     */
    public void startCategoryExitAnimation() {
        categoryAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
    }
    
    /**
     * 인벤토리 진입 애니메이션 시작 (상점 카테고리와 동일한 방식)
     */
    public void startInventoryEntryAnimation() {
        categoryAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
    }
    
    /**
     * 인벤토리 나가기 애니메이션 시작 (상점 카테고리와 동일한 방식)
     */
    public void startInventoryExitAnimation() {
        categoryAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
    }
    
    /**
     * 애니메이션 업데이트
     */
    public void updateAnimation() {
        animation.update();
        categoryAnimation.update();
    }
    
    /**
     * 애니메이션 중인지 확인
     */
    public boolean isAnimating() {
        return animation.isAnimating();
    }
    
    /**
     * 카테고리 애니메이션 중인지 확인
     */
    public boolean isCategoryAnimating() {
        return categoryAnimation.isAnimating();
    }
    
    /**
     * 애니메이션 리셋
     */
    public void reset() {
        animation.reset();
        categoryAnimation.reset();
    }
    
    public void draw(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, 800, 600);
        
        // 애니메이션 업데이트
        updateAnimation();
        
        // 현재 상태에 따라 다른 화면 그리기
        switch (shopManager.getCurrentState()) {
            case MAIN:
                drawMainShop(g2d, shopManager, inputHandler);
                break;
            case CATEGORY:
                drawCategoryShop(g2d, shopManager, inputHandler);
                break;
            case ITEM_DETAIL:
                drawItemDetail(g2d, shopManager, inputHandler);
                break;
            case PURCHASE:
                drawPurchaseConfirmation(g2d, shopManager, inputHandler);
                break;
            case INVENTORY:
                drawInventory(g2d, shopManager, inputHandler);
                break;
            case SEARCH:
                drawSearchResults(g2d, shopManager, inputHandler);
                break;
            case FILTER:
                drawFilteredItems(g2d, shopManager, inputHandler);
                break;
            case CART:
                drawCart(g2d, shopManager, inputHandler);
                break;
            case WISHLIST:
                drawWishlist(g2d, shopManager, inputHandler);
                break;
        }
    }
    
    private void drawMainShop(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 애니메이션 효과에 따른 패널 위치 계산
        float progress = animation.getProgress();
        int leftPanelX, rightPanelX;
        
        if (animation.isAnimating() && animation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_IN) {
            // 양쪽에서 밀려오는 효과
            leftPanelX = (int) (50 - (200 * (1.0f - progress)));  // 왼쪽에서 오른쪽으로
            rightPanelX = (int) (270 + (480 * (1.0f - progress))); // 오른쪽에서 왼쪽으로
        } else if (animation.isAnimating() && animation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_OUT) {
            // 양쪽으로 밀려나가는 효과
            leftPanelX = (int) (50 - (200 * progress));  // 왼쪽으로 밀려나감
            rightPanelX = (int) (270 + (480 * progress)); // 오른쪽으로 밀려나감
        } else if (animation.isAnimating() && animation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_DOWN) {
            // 위에서 아래로 내려오는 효과 (인벤토리 진입)
            leftPanelX = 50;
            rightPanelX = 270;
            // Y 위치는 애니메이션에 따라 조정 (나중에 구현)
        } else if (animation.isAnimating() && animation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_UP) {
            // 아래에서 위로 올라가는 효과 (인벤토리 나가기)
            leftPanelX = 50;
            rightPanelX = 270;
            // Y 위치는 애니메이션에 따라 조정 (나중에 구현)
        } else {
            // 애니메이션 완료 후 정상 위치
            leftPanelX = 50;
            rightPanelX = 270;
        }
        
        // 왼쪽 패널 (메뉴 네비게이션)
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(leftPanelX, 50, 200, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(leftPanelX, 50, 200, 500);
        
        // 오른쪽 패널 (콘텐츠 표시 영역)
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(rightPanelX, 50, 480, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(rightPanelX, 50, 480, 500);
        
        // 제목 "상점" (왼쪽 패널 내부)
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "상점";
        int titleX = leftPanelX + (200 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100);
        
        // 메뉴 옵션들 (왼쪽 패널)
        String[] mainOptions = {
            "무기 상점", "우주선 상점", "뒤로가기"
        };
        
        g2d.setFont(menuFont);
        
        int startY = 140;
        int lineHeight = 40;
        
        for (int i = 0; i < mainOptions.length; i++) {
            // 선택된 항목 강조
            if (i == inputHandler.getSelectedOption()) {
                g2d.setColor(Color.WHITE);
                g2d.drawRect(leftPanelX + 10, startY + (i * lineHeight) - 25, 180, 30);
                
                // 화살표 그리기
                g2d.setColor(Color.YELLOW);
                g2d.drawString("→", leftPanelX + 170, startY + (i * lineHeight));
            }
            
            g2d.setColor(i == inputHandler.getSelectedOption() ? Color.YELLOW : Color.WHITE);
            int x = leftPanelX + 20;
            int y = startY + (i * lineHeight);
            g2d.drawString(mainOptions[i], x, y);
        }
        
        // 오른쪽 패널에 선택된 항목의 상세 정보 표시
        drawShopDetails(g2d, shopManager, inputHandler, rightPanelX);
    }
    
    private void drawShopDetails(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler, int panelX) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        
        int startX = panelX+20;
        int startY = 100;
        int lineHeight = 30;
        
        // 플레이어 코인 표시 (코인 이미지 + 숫자) - 실시간 데이터
        int currentCoins = getCurrentCoins();
        
        if (coinImage != null) {
            // 코인 이미지 그리기 (24x24 크기)
            g2d.drawImage(coinImage, startX, startY - 20, 24, 24, null);
            // 코인 개수 텍스트
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont);
            g2d.drawString(": " + currentCoins, startX + 30, startY);
        } else {
            // 코인 이미지 로드 실패 시 텍스트로 표시
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont);
            g2d.drawString("보유 코인: " + currentCoins, startX, startY);
        }
        
        g2d.setColor(Color.WHITE);
        
        // 선택된 항목에 따라 다른 정보 표시
        switch (inputHandler.getSelectedOption()) {
            case 0: //  상점
                g2d.drawString("다양한 무기를 구매할 수 있습니다.", startX, startY + lineHeight * 2);
                g2d.drawString("레이저 건, 플라즈마 건, 미사일 발사기 등", startX, startY + lineHeight * 3);
                g2d.drawString("Enter를 눌러 무기 상점에 입장하세요.", startX, startY + lineHeight * 4);
                break;
            case 1: // 우주선 상점
                g2d.drawString("다양한 우주선을 구매할 수 있습니다.", startX, startY + lineHeight * 2);
                g2d.drawString("다양한 색상의 전투기, 기깔나는 전투기 등", startX, startY + lineHeight * 3);
                g2d.drawString("Enter를 눌러 우주선 상점에 입장하세요.", startX, startY + lineHeight * 4);
                break;
            case 2: // 뒤로가기
                g2d.drawString("메인화면으로 이동합니다.", startX, startY + lineHeight * 2);
                g2d.drawString("확인하려면 Enter를 누르세요.", startX, startY + lineHeight * 3);
                break;
            default:
                // 유효하지 않은 옵션은 무시
                break;
        }
        
        // 구매 메시지 표시
        if (shopManager.hasMessage()) {
            g2d.setColor(Color.RED);
            g2d.setFont(menuFont);
            String message = shopManager.getPurchaseMessage();
            FontMetrics messageMetrics = g2d.getFontMetrics();
            int messageX = startX + (400 - messageMetrics.stringWidth(message)) / 2;
            g2d.drawString(message, messageX, startY + 200);
        }
    }
    
    private void drawCategoryShop(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 애니메이션 효과에 따른 패널 위치 계산
        float progress = categoryAnimation.getProgress();
        int panelX, panelY;
        
        if (categoryAnimation.isAnimating() && categoryAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_IN) {
            // 위에서 아래로 밀려오는 효과
            panelX = 50;
            panelY = (int) (50 - (500 * (1.0f - progress))); // 위에서 아래로
        } else if (categoryAnimation.isAnimating() && categoryAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_OUT) {
            // 아래에서 위로 밀려나가는 효과
            panelX = 50;
            panelY = (int) (50 - (500 * progress)); // 아래에서 위로
        } else {
            // 애니메이션 완료 후 정상 위치
            panelX = 50;
            panelY = 50;
        }
        
        // 전체 화면을 활용한 단일 패널
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(panelX, panelY, 700, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(panelX, panelY, 700, 500);
        
        // 카테고리 제목 표시
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String categoryTitle = getCategoryTitle(shopManager.getCurrentCategory());
        int titleX = panelX + (700 - titleMetrics.stringWidth(categoryTitle)) / 2;
        int titleY = panelY + 50; // 패널 내부에서 50px 아래
        g2d.drawString(categoryTitle, titleX, titleY);
        
        // 아이템들 표시
        drawCategoryItems(g2d, shopManager, inputHandler, panelX, panelY);
    }
    
    private String getCategoryTitle(ShopCategory category) {
        switch (category) {
            case WEAPONS:
                return "무기 상점";
            case SPACESHIPS:
                return "우주선 상점";
            case POWERUPS:
                return "파워업 상점";
            case DECORATIONS:
                return "장식품 상점";
            case UPGRADES:
                return "업그레이드 상점";
            default:
                return "상점";
        }
    }
    
    private void drawCategoryItems(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler, int panelX, int panelY) {
        drawCategoryCoins(g2d, panelX, panelY);
        
        List<ShopItem> currentPageItems = shopManager.getItemsForCurrentPage(shopManager.getCurrentCategory());
        
        if (currentPageItems.isEmpty()) {
            drawEmptyCategoryMessage(g2d, panelX, panelY);
            return;
        }
        
        drawCategoryItemGrid(g2d, currentPageItems, shopManager, inputHandler, panelX, panelY);
        drawCategoryMessages(g2d, shopManager, panelX, panelY);
    }
    
    private void drawCategoryCoins(Graphics2D g2d, int panelX, int panelY) {
        int currentCoins = getCurrentCoins();
        if (coinImage != null) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont);
            String coinText = ": " + currentCoins;
            FontMetrics coinMetrics = g2d.getFontMetrics();
            
            int coinTextWidth = coinMetrics.stringWidth(coinText);
            int coinImageWidth = 24;
            int totalWidth = coinImageWidth + 5 + coinTextWidth;
            int coinImageX = panelX + 700 - totalWidth - 15;
            
            g2d.drawImage(coinImage, coinImageX, panelY + 20, 24, 24, null);
            int coinTextX = coinImageX + coinImageWidth + 5;
            g2d.drawString(coinText, coinTextX, panelY + 40);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont);
            String coinText = "보유 코인: " + currentCoins;
            FontMetrics coinMetrics = g2d.getFontMetrics();
            int coinTextX = panelX + 700 - coinMetrics.stringWidth(coinText) - 15;
            g2d.drawString(coinText, coinTextX, panelY + 40);
        }
    }
    
    private void drawEmptyCategoryMessage(Graphics2D g2d, int panelX, int panelY) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        g2d.drawString("이 카테고리에는 아이템이 없습니다.", panelX + 350, panelY + 200);
    }
    
    private void drawCategoryItemGrid(Graphics2D g2d, List<ShopItem> items, ShopManager shopManager, ShopInputHandler inputHandler, int panelX, int panelY) {
        int itemsPerRow = 3;
        int panelWidth = 700;
        int spacingX = 15;
        int spacingY = 20;
        int itemWidth = (panelWidth - spacingX * (itemsPerRow + 1)) / itemsPerRow;
        int itemHeight = 150;
        int startX = panelX + spacingX;
        int startY = panelY + 80;
        
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            
            int x = startX + col * (itemWidth + spacingX);
            int y = startY + row * (itemHeight + spacingY);
            
            drawCategoryItem(g2d, item, i, x, y, itemWidth, itemHeight, shopManager, inputHandler);
        }
    }
    
    private void drawCategoryItem(Graphics2D g2d, ShopItem item, int index, int x, int y, int itemWidth, int itemHeight, ShopManager shopManager, ShopInputHandler inputHandler) {
        boolean isPurchased = shopManager.isItemInInventory(item.getId());
        boolean isSelected = (index == inputHandler.getSelectedItem());
        
        drawCategoryItemBox(g2d, x, y, itemWidth, itemHeight, isPurchased, isSelected);
        
        BufferedImage itemImage = loadItemImage(item.getIconPath());
        if (itemImage != null) {
            int imageSize = 64;
            int imageX = x + (itemWidth - imageSize) / 2;
            int imageY = y + 20;
            g2d.drawImage(itemImage, imageX, imageY, imageSize, imageSize, null);
        }
        
        drawCategoryItemInfo(g2d, item, x, y, itemWidth, itemHeight, isPurchased, itemImage != null);
    }
    
    private void drawCategoryItemBox(Graphics2D g2d, int x, int y, int itemWidth, int itemHeight, boolean isPurchased, boolean isSelected) {
        if (itemBoxImage != null) {
            if (isSelected) {
                g2d.setColor(Color.YELLOW);
                g2d.drawRect(x - 2, y - 2, itemWidth + 4, itemHeight + 4);
            }
            g2d.drawImage(itemBoxImage, x, y, itemWidth, itemHeight, null);
            if (isPurchased) {
                g2d.setColor(new Color(0, 255, 0, 100));
                g2d.fillRect(x, y, itemWidth, itemHeight);
            }
        } else {
            if (isSelected) {
                g2d.setColor(Color.YELLOW);
            } else if (isPurchased) {
                g2d.setColor(new Color(0, 150, 0));
            } else {
                g2d.setColor(new Color(0, 100, 200));
            }
            g2d.fillRect(x, y, itemWidth, itemHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, itemWidth, itemHeight);
        }
    }
    
    private void drawCategoryItemInfo(Graphics2D g2d, ShopItem item, int x, int y, int itemWidth, int itemHeight, boolean isPurchased, boolean hasImage) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(itemFont);
        
        String itemName = item.getName();
        if (itemName.length() > 15) {
            itemName = itemName.substring(0, 15) + "...";
        }
        FontMetrics nameMetrics = g2d.getFontMetrics();
        int nameX = x + (itemWidth - nameMetrics.stringWidth(itemName)) / 2;
        int nameY = hasImage ? y + 100 : y + 45;
        g2d.drawString(itemName, nameX, nameY);
        
        g2d.setFont(descriptionFont);
        String statusText = isPurchased ? "보유중" : item.getPrice() + " 코인";
        Color statusColor = isPurchased ? Color.GREEN : Color.YELLOW;
        
        g2d.setColor(statusColor);
        FontMetrics statusMetrics = g2d.getFontMetrics();
        int statusX = x + (itemWidth - statusMetrics.stringWidth(statusText)) / 2;
        int statusY = nameY + 20;
        g2d.drawString(statusText, statusX, statusY);
        
        if (!isPurchased && coinImage != null) {
            g2d.drawImage(coinImage, statusX + statusMetrics.stringWidth(statusText) + 5, statusY - 15, 16, 16, null);
        }
    }
    
    private void drawCategoryMessages(Graphics2D g2d, ShopManager shopManager, int panelX, int panelY) {
        if (shopManager.hasMessage()) {
            g2d.setColor(Color.RED);
            g2d.setFont(menuFont);
            String message = shopManager.getPurchaseMessage();
            FontMetrics messageMetrics = g2d.getFontMetrics();
            int messageX = panelX + (700 - messageMetrics.stringWidth(message)) / 2;
            int messageY = panelY + 420;
            g2d.drawString(message, messageX, messageY);
        }
        
        g2d.setFont(menuFont);
        g2d.setColor(Color.YELLOW);
        String instructions = "↑↓←→: 아이템 이동  Enter: 구매  P/L: 페이지 전환  ESC: 뒤로가기";
        FontMetrics instructionMetrics = g2d.getFontMetrics();
        int instructionX = panelX + (700 - instructionMetrics.stringWidth(instructions)) / 2;
        int instructionY = panelY + 450;
        g2d.drawString(instructions, instructionX, instructionY);
    }
    
    // private String getCategoryTitle(ShopCategory category) {
    //     if (category == null) return "카테고리 상점";
        
    //     switch (category) {
    //         case WEAPONS: return "무기 상점";
    //         case SPACESHIPS: return "우주선 상점";
    //         case POWERUPS: return "파워업 상점";
    //         case DECORATIONS: return "장식품 상점";
    //         case UPGRADES: return "업그레이드 상점";
    //         default: return "카테고리 상점";
    //     }
    //     }
    
    /**
     * 텍스트를 지정된 길이로 줄바꿈
     */
    private String[] wrapText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return new String[]{text};
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (canAddWordToLine(currentLine, word, maxLength)) {
                appendWordToLine(currentLine, word);
            } else {
                processWordTooLong(lines, currentLine, word, maxLength);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    private boolean canAddWordToLine(StringBuilder currentLine, String word, int maxLength) {
        return currentLine.length() + word.length() + 1 <= maxLength;
    }
    
    private void appendWordToLine(StringBuilder currentLine, String word) {
        if (currentLine.length() > 0) {
            currentLine.append(" ");
        }
        currentLine.append(word);
    }
    
    private void processWordTooLong(java.util.List<String> lines, StringBuilder currentLine, String word, int maxLength) {
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
            currentLine.setLength(0);
            currentLine.append(word);
        } else {
            lines.add(word.substring(0, Math.min(word.length(), maxLength)));
            if (word.length() > maxLength) {
                currentLine.append(word.substring(maxLength));
            }
        }
    }
    
    /**
     * 텍스트를 픽셀 너비 기준으로 줄바꿈합니다
     */
    private String[] wrapTextByWidth(String text, int maxWidth, FontMetrics fontMetrics) {
        if (text == null || fontMetrics.stringWidth(text) <= maxWidth) {
            return new String[]{text};
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = buildTestLine(currentLine, word);
            if (fontMetrics.stringWidth(testLine) <= maxWidth) {
                appendWordToLine(currentLine, word);
            } else {
                processLineBreak(lines, currentLine, word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    private String buildTestLine(StringBuilder currentLine, String word) {
        return currentLine.length() > 0 ? currentLine + " " + word : word;
    }
    
    private void processLineBreak(java.util.List<String> lines, StringBuilder currentLine, String word) {
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
            currentLine.setLength(0);
            currentLine.append(word);
        } else {
            lines.add(word);
        }
    }
    
    /**
     * 희귀도에 따른 색상 반환
     */
    private Color getRarityColor(ItemRarity rarity) {
        switch (rarity) {
            case COMMON:
                return new Color(200, 200, 200); // 회색
            case RARE:
                return new Color(0, 150, 255); // 파란색
            case EPIC:
                return new Color(150, 0, 255); // 보라색
            case LEGENDARY:
                return new Color(255, 150, 0); // 주황색
            default:
                return Color.WHITE;
        }
    }
    
    private void drawItemDetail(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 아이템 상세 화면 구현
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("아이템 상세", 300, 100);
        
        // 아이템 정보 그리기
        // 구현 필요...
    }
    
    private void drawPurchaseConfirmation(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        drawPurchaseDialogBackground(g2d);
        
        int dialogWidth = 600;
        int dialogHeight = 450;
        int dialogX = (800 - dialogWidth) / 2;
        int dialogY = (600 - dialogHeight) / 2;
        
        drawPurchaseDialogFrame(g2d, dialogX, dialogY, dialogWidth, dialogHeight);
        
        ShopItem selectedItem = shopManager.getSelectedItem(inputHandler.getSelectedItem());
        if (selectedItem != null) {
            drawPurchaseItemImage(g2d, selectedItem, dialogX, dialogY);
            drawPurchaseItemInfo(g2d, selectedItem, dialogX, dialogY);
            drawPurchaseButtons(g2d, inputHandler, dialogX, dialogY, dialogWidth);
        }
    }
    
    private void drawPurchaseDialogBackground(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, 800, 600);
    }
    
    private void drawPurchaseDialogFrame(Graphics2D g2d, int dialogX, int dialogY, int dialogWidth, int dialogHeight) {
        g2d.setColor(new Color(50, 50, 50, 240));
        g2d.fillRect(dialogX, dialogY, dialogWidth, dialogHeight);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(dialogX, dialogY, dialogWidth, dialogHeight);
    }
    
    private void drawPurchaseItemImage(Graphics2D g2d, ShopItem item, int dialogX, int dialogY) {
        int imageSize = 180;
        int imageX = dialogX + 20;
        int imageY = dialogY + 40;
        
        try {
            java.awt.image.BufferedImage itemImage = javax.imageio.ImageIO.read(
                getClass().getClassLoader().getResourceAsStream(item.getIconPath())
            );
            if (itemImage != null) {
                g2d.drawImage(itemImage, imageX, imageY, imageSize, imageSize, null);
            }
        } catch (Exception e) {
            g2d.setColor(new Color(100, 100, 100, 150));
            g2d.fillRect(imageX, imageY, imageSize, imageSize);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(imageX, imageY, imageSize, imageSize);
            g2d.setFont(new Font(DEFAULT_UI_FONT_NAME, Font.PLAIN, 12));
            g2d.drawString("이미지 없음", imageX + 10, imageY + imageSize/2);
        }
    }
    
    private void drawPurchaseItemInfo(Graphics2D g2d, ShopItem item, int dialogX, int dialogY) {
        int infoX = dialogX + 220;
        int infoY = dialogY + 50;
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont.deriveFont(32f));
        g2d.drawString(item.getName(), infoX, infoY + 40);
        
        g2d.setFont(descriptionFont.deriveFont(16f));
        String description = item.getDescription();
        String[] descriptionLines = wrapText(description, 25);
        int lineHeight = 25;
        for (int i = 0; i < descriptionLines.length && i < 3; i++) {
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawString(descriptionLines[i], infoX, infoY + 80 + (i * lineHeight));
        }
        
        g2d.setFont(menuFont.deriveFont(18f));
        g2d.setColor(getRarityColor(item.getRarity()));
        g2d.drawString(item.getRarity().getDisplayName(), infoX, infoY + 160);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(menuFont.deriveFont(20f));
        String priceText = "가격: " + item.getPrice() + " 코인";
        FontMetrics priceMetrics = g2d.getFontMetrics();
        g2d.drawString(priceText, infoX, infoY + 190);
        
        if (coinImage != null) {
            g2d.drawImage(coinImage, infoX + priceMetrics.stringWidth(priceText) + 10, infoY + 175, 30, 30, null);
        }
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont.deriveFont(18f));
        String questionText = "이 아이템을 구매하시겠습니까?";
        FontMetrics questionMetrics = g2d.getFontMetrics();
        int questionX = dialogX + (600 - questionMetrics.stringWidth(questionText)) / 2;
        g2d.drawString(questionText, questionX, dialogY + 350);
    }
    
    private void drawPurchaseButtons(Graphics2D g2d, ShopInputHandler inputHandler, int dialogX, int dialogY, int dialogWidth) {
        int buttonWidth = 100;
        int buttonHeight = 40;
        int buttonY = dialogY + 380;
        int yesX = dialogX + (dialogWidth / 2) - buttonWidth - 15;
        int noX = dialogX + (dialogWidth / 2) + 15;
        
        drawPurchaseButton(g2d, "예", yesX, buttonY, buttonWidth, buttonHeight, inputHandler.getSelectedOption() == 0, true);
        drawPurchaseButton(g2d, "아니요", noX, buttonY, buttonWidth, buttonHeight, inputHandler.getSelectedOption() == 1, false);
    }
    
    private void drawPurchaseButton(Graphics2D g2d, String text, int x, int y, int width, int height, boolean isSelected, boolean isYes) {
        if (isSelected) {
            g2d.setColor(isYes ? new Color(0, 150, 0, 200) : new Color(150, 0, 0, 200));
        } else {
            g2d.setColor(isYes ? new Color(0, 100, 0, 150) : new Color(100, 0, 0, 150));
        }
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x, y, width, height);
        
        g2d.setFont(descriptionFont);
        FontMetrics metrics = g2d.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + (height + metrics.getAscent()) / 2 - 2;
        g2d.drawString(text, textX, textY);
    }
    
    private void drawInventory(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 애니메이션 효과에 따른 패널 위치 계산 (상점 카테고리와 동일한 방식)
        float progress = categoryAnimation.getProgress();
        int panelX, panelY;
        
        if (categoryAnimation.isAnimating() && categoryAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_IN) {
            // 위에서 아래로 밀려오는 효과
            panelX = 50;
            panelY = (int) (50 - (500 * (1.0f - progress))); // 위에서 아래로
        } else if (categoryAnimation.isAnimating() && categoryAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_OUT) {
            // 아래에서 위로 밀려나가는 효과
            panelX = 50;
            panelY = (int) (50 - (500 * progress)); // 아래에서 위로
        } else {
            // 애니메이션 완료 후 정상 위치
            panelX = 50;
            panelY = 50;
        }
        
        // 전체 화면을 인벤토리 영역으로 사용
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(panelX, panelY, 700, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(panelX, panelY, 700, 500);
        
        // 제목 "인벤토리" (화면 상단 중앙)
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "인벤토리";
        int titleX = panelX + (700 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, panelY + 50);
        
        // 카테고리 탭 표시
        drawInventoryCategoryTabs(g2d, shopManager, inputHandler, panelX, panelY);
        
        // 현재 선택된 카테고리의 아이템들 표시 (페이지네이션 적용)
        ShopCategory currentCategory = shopManager.getInventoryCategory();
        List<ShopItem> currentPageItems = shopManager.getInventoryItemsForCurrentPage(currentCategory);
        
        if (currentPageItems.isEmpty()) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(menuFont);
            String message = currentCategory.getDisplayName() + " 카테고리에 아이템이 없습니다.";
            g2d.drawString(message, panelX + (700 - g2d.getFontMetrics().stringWidth(message)) / 2, panelY + 170);
            g2d.drawString("상점에서 아이템을 구매해보세요!", panelX + (700 - g2d.getFontMetrics().stringWidth("상점에서 아이템을 구매해보세요!")) / 2, panelY + 200);
        } else {
            drawInventoryItems(g2d, currentPageItems, shopManager, inputHandler, panelX, panelY);
        }
        
        
        // 조작 안내
        g2d.setFont(menuFont);
        g2d.setColor(Color.YELLOW);
        String instructions = "Q: 카테고리 변경  Enter: 장착/해제  P/L: 페이지 전환  ESC: 뒤로가기";
        g2d.drawString(instructions, panelX + (700 - g2d.getFontMetrics().stringWidth(instructions)) / 2, panelY + 450);
        
        // 저장 중 표시
        if (shopManager.getEquipmentManager().isSaving()) {
            // 저장 중 오버레이
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(200, 250, 400, 100);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(200, 250, 400, 100);
            
            // "아이템 적용중..." 메시지
            g2d.setFont(titleFont);
            FontMetrics metrics = g2d.getFontMetrics();
            String message = "아이템 적용중...";
            int messageX = 200 + (400 - metrics.stringWidth(message)) / 2;
            g2d.drawString(message, messageX, 310);
        }
        
        // 경고창 표시
        if (shopManager.hasWarningDialog()) {
            drawWarningDialog(g2d, shopManager);
        }
    }
    
    /**
     * 경고창 그리기 (3초 후 자동 사라짐)
     */
    private void drawWarningDialog(Graphics2D g2d, ShopManager shopManager) {
        String message = shopManager.getWarningMessage();
        
        // 경고창 크기 계산
        g2d.setFont(menuFont);
        FontMetrics messageMetrics = g2d.getFontMetrics();
        
        int dialogWidth = Math.max(400, messageMetrics.stringWidth(message) + 80);
        int dialogHeight = 120;
        int dialogX = (800 - dialogWidth) / 2;
        int dialogY = (600 - dialogHeight) / 2;
        
        // 전체 화면 어둡게 처리
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, 800, 600);
        
        // 경고창 배경
        g2d.setColor(new Color(220, 50, 50, 240));
        g2d.fillRect(dialogX, dialogY, dialogWidth, dialogHeight);
        
        // 경고창 테두리
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new java.awt.BasicStroke(3));
        g2d.drawRect(dialogX, dialogY, dialogWidth, dialogHeight);
        g2d.setStroke(new java.awt.BasicStroke(1)); // 기본 스트로크로 복원
        
        // 경고 아이콘 (삼각형 + 느낌표)
        int iconX = dialogX + 30;
        int iconY = dialogY + 30;
        int iconSize = 30;
        
        // 삼각형 경고 아이콘
        g2d.setColor(Color.YELLOW);
        int[] triangleX = {iconX + iconSize/2, iconX, iconX + iconSize};
        int[] triangleY = {iconY, iconY + iconSize, iconY + iconSize};
        g2d.fillPolygon(triangleX, triangleY, 3);
        
        // 느낌표
        g2d.setColor(Color.RED);
        g2d.setFont(new Font(DEFAULT_UI_FONT_NAME, Font.BOLD, 20));
        g2d.drawString("!", iconX + iconSize/2 - 4, iconY + iconSize - 5);
        
        // 경고 메시지
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        int messageX = iconX + iconSize + 20;
        int messageY = dialogY + 50;
        g2d.drawString(message, messageX, messageY);
        
        // 자동 닫힘 안내
        g2d.setFont(new Font(DEFAULT_UI_FONT_NAME, Font.PLAIN, 12));
        g2d.setColor(Color.LIGHT_GRAY);
        String autoCloseText = "이 경고창은 자동으로 닫힙니다";
        FontMetrics autoMetrics = g2d.getFontMetrics();
        int autoX = dialogX + (dialogWidth - autoMetrics.stringWidth(autoCloseText)) / 2;
        int autoY = dialogY + dialogHeight - 15;
        g2d.drawString(autoCloseText, autoX, autoY);
    }
    
    private void drawInventoryCategoryTabs(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler, int panelX, int panelY) {
        ShopCategory currentCategory = shopManager.getInventoryCategory();
        ShopCategory[] equippableCategories = {
            ShopCategory.WEAPONS,
            ShopCategory.SPACESHIPS
        };
        
        int tabWidth = 150;
        int tabHeight = 30;
        int totalWidth = (tabWidth + 10) * equippableCategories.length - 10; // 전체 탭 너비
        int startX = panelX + (700 - totalWidth) / 2; // 중앙정렬
        int startY = panelY + 90;
        
        for (int i = 0; i < equippableCategories.length; i++) {
            ShopCategory category = equippableCategories[i];
            int x = startX + i * (tabWidth + 10);
            int y = startY;
            
            // 탭 배경
            if (category == currentCategory) {
                g2d.setColor(new Color(100, 100, 255, 200));
            } else {
                g2d.setColor(new Color(50, 50, 50, 200));
            }
            g2d.fillRect(x, y, tabWidth, tabHeight);
            
            // 탭 테두리
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, tabWidth, tabHeight);
            
            // 탭 텍스트 (아이템 개수 포함)
            g2d.setFont(menuFont);
            FontMetrics metrics = g2d.getFontMetrics();
            int totalItems = shopManager.getInventoryByCategory(category).size(); // 전체 아이템 개수
            String text = category.getDisplayName() + " (" + totalItems + ")";
            int textX = x + (tabWidth - metrics.stringWidth(text)) / 2;
            int textY = y + (tabHeight + metrics.getAscent()) / 2 - 2;
            g2d.drawString(text, textX, textY);
        }
    }
    
    private void drawInventoryItems(Graphics2D g2d, List<ShopItem> items, ShopManager shopManager, ShopInputHandler inputHandler, int panelX, int panelY) {
        int itemsPerRow = 3;
        int panelWidth = 700;
        int spacingX = 15;
        int spacingY = 20;
        int itemWidth = (panelWidth - spacingX * (itemsPerRow + 1)) / itemsPerRow;
        int itemHeight = 120;
        int startX = panelX + spacingX;
        int startY = panelY + 140;
        
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            
            int x = startX + col * (itemWidth + spacingX);
            int y = startY + row * (itemHeight + spacingY);
            
            drawInventoryItem(g2d, item, i, x, y, itemWidth, itemHeight, shopManager, inputHandler);
        }
    }
    
    private void drawInventoryItem(Graphics2D g2d, ShopItem item, int index, int x, int y, int itemWidth, int itemHeight, ShopManager shopManager, ShopInputHandler inputHandler) {
        boolean isSelected = (index == inputHandler.getSelectedItem());
        boolean isEquipped = shopManager.isItemEquipped(item);
        
        drawInventoryItemBox(g2d, x, y, itemWidth, itemHeight, isSelected);
        drawInventoryItemImage(g2d, item, x, y, itemWidth, itemHeight);
        drawInventoryItemText(g2d, item, x, y, itemWidth, itemHeight, isEquipped);
        
        if (isEquipped) {
            g2d.setColor(Color.YELLOW);
            g2d.drawRect(x, y, itemWidth, itemHeight);
        }
    }
    
    private void drawInventoryItemBox(Graphics2D g2d, int x, int y, int itemWidth, int itemHeight, boolean isSelected) {
        if (itemBoxImage != null) {
            g2d.drawImage(itemBoxImage, x, y, itemWidth, itemHeight, null);
        } else {
            g2d.setColor(isSelected ? new Color(100, 100, 255) : new Color(0, 150, 0));
            g2d.fillRect(x, y, itemWidth, itemHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, itemWidth, itemHeight);
        }
        
        if (isSelected) {
            g2d.setColor(Color.CYAN);
            g2d.drawRect(x - 2, y - 2, itemWidth + 4, itemHeight + 4);
        }
    }
    
    private void drawInventoryItemImage(Graphics2D g2d, ShopItem item, int x, int y, int itemWidth, int itemHeight) {
        int leftWidth = itemWidth / 2;
        BufferedImage itemImage = loadItemImage(item.getIconPath());
        if (itemImage != null) {
            int imageSize = 48;
            int imageX = x + (leftWidth - imageSize) / 2;
            int imageY = y + (itemHeight - imageSize) / 2;
            g2d.drawImage(itemImage, imageX, imageY, imageSize, imageSize, null);
        }
    }
    
    private void drawInventoryItemText(Graphics2D g2d, ShopItem item, int x, int y, int itemWidth, int itemHeight, boolean isEquipped) {
        int leftWidth = itemWidth / 2;
        int rightWidth = itemWidth - leftWidth;
        int rightX = x + leftWidth;
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(itemFont);
        
        String itemName = item.getName();
        FontMetrics nameMetrics = g2d.getFontMetrics();
        int nameX = rightX + 5;
        int nameY = y + 50;
        int availableWidth = rightWidth - 10;
        
        if (nameMetrics.stringWidth(itemName) > availableWidth) {
            String[] nameLines = wrapTextByWidth(itemName, availableWidth, nameMetrics);
            for (int lineIndex = 0; lineIndex < nameLines.length && lineIndex < 2; lineIndex++) {
                g2d.drawString(nameLines[lineIndex], nameX, nameY + (lineIndex * 15));
            }
        } else {
            g2d.drawString(itemName, nameX, nameY);
        }
        
        g2d.setFont(descriptionFont);
        String statusText = isEquipped ? "장착중" : "보유중";
        g2d.setColor(isEquipped ? Color.YELLOW : Color.GREEN);
        int statusX = rightX + 5;
        int statusY = nameY + 35;
        g2d.drawString(statusText, statusX, statusY);
    }
    
    private void drawSearchResults(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 검색 결과 화면 구현
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("검색 결과", 300, 100);
        
        // 검색 결과 목록 그리기
        // 구현 필요...
    }
    
    private void drawFilteredItems(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 필터링된 아이템 화면 구현
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("필터링된 아이템", 300, 100);
        
        // 필터링된 아이템 목록 그리기
        // 구현 필요...
    }
    
    private void drawCart(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 장바구니 화면 구현
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("장바구니", 300, 100);
        
        // 장바구니 아이템 목록 그리기
        // 구현 필요...
    }
    
    private void drawWishlist(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 위시리스트 화면 구현
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("위시리스트", 300, 100);
        
        // 위시리스트 아이템 목록 그리기
        // 구현 필요...
    }
    
    // private void drawControls(Graphics2D g2d) {
    //     g2d.setColor(Color.GRAY);
    //     g2d.setFont(new Font("Arial", Font.PLAIN, 14));
    //     g2d.drawString("↑↓: 이동  Enter/Space: 선택  ESC: 뒤로가기", 250, 550);
    // }
}