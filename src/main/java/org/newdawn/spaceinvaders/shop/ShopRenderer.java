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
    private Font titleFont;
    private Font menuFont;
    private Font itemFont;
    private Font descriptionFont;
    private UserManager userManager;
    private BufferedImage backgroundImage;
    private BufferedImage coinImage;
    private BufferedImage itemBoxImage;
    private Map<String, BufferedImage> itemImageCache;
    
    public ShopRenderer() {
        initializeFonts();
        loadBackgroundImage();
        loadCoinImage();
        loadItemBoxImage();
        itemImageCache = new HashMap<>();
        this.userManager = null;
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
            // Kostar 폰트 로드
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                Font kostarFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                titleFont = kostarFont.deriveFont(Font.BOLD, 36f);
                menuFont = kostarFont.deriveFont(Font.BOLD, 20f);
                itemFont = kostarFont.deriveFont(Font.BOLD, 16f);
                descriptionFont = kostarFont.deriveFont(Font.PLAIN, 12f);
                fontStream.close();
            } else {
                // 폰트 로드 실패 시 기본 폰트 사용
                System.err.println("Kostar 폰트를 로드할 수 없습니다. 기본 폰트를 사용합니다.");
                titleFont = new Font("Arial", Font.BOLD, 36);
                menuFont = new Font("Arial", Font.BOLD, 20);
                itemFont = new Font("Arial", Font.BOLD, 16);
                descriptionFont = new Font("Arial", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 중 오류 발생: " + e.getMessage());
            // 오류 발생 시 기본 폰트 사용
        titleFont = new Font("Arial", Font.BOLD, 36);
        menuFont = new Font("Arial", Font.BOLD, 20);
        itemFont = new Font("Arial", Font.BOLD, 16);
        descriptionFont = new Font("Arial", Font.PLAIN, 12);
        }
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
    
    public void draw(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, 800, 600);
        
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
        // 왼쪽 패널 (메뉴 네비게이션)
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(50, 50, 200, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, 50, 200, 500);
        
        // 오른쪽 패널 (콘텐츠 표시 영역)
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(270, 50, 480, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(270, 50, 480, 500);
        
        // 제목 "상점"
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "상점";
        int titleX = 50 + (200 - titleMetrics.stringWidth(title)) / 2;
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
                g2d.drawRect(60, startY + (i * lineHeight) - 25, 180, 30);
                
                // 화살표 그리기
                g2d.setColor(Color.YELLOW);
                g2d.drawString("→", 220, startY + (i * lineHeight));
            }
            
            g2d.setColor(i == inputHandler.getSelectedOption() ? Color.YELLOW : Color.WHITE);
            int x = 70;
            int y = startY + (i * lineHeight);
            g2d.drawString(mainOptions[i], x, y);
        }
        
        // 오른쪽 패널에 선택된 항목의 상세 정보 표시
        drawShopDetails(g2d, shopManager, inputHandler);
    }
    
    private void drawShopDetails(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        
        int startX = 290;
        int startY = 100;
        int lineHeight = 30;
        
        // 플레이어 코인 표시 (코인 이미지 + 숫자) - 실시간 데이터
        int currentCoins = getCurrentCoins();
        
        // 디버깅 정보 표시
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("UserManager: " + (userManager != null ? "있음" : "없음"), 50, 30);
        g2d.drawString("로그인: " + (userManager != null ? userManager.isLoggedIn() : "false"), 50, 45);
        g2d.drawString("사용자: " + (userManager != null && userManager.getCurrentUser() != null ? userManager.getCurrentUser().getUsername() : "null"), 50, 60);
        
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
            case 0: // 무기 상점
                g2d.drawString("다양한 무기를 구매할 수 있습니다.", startX, startY + lineHeight * 2);
                g2d.drawString("레이저 건, 플라즈마 건, 미사일 발사기 등", startX, startY + lineHeight * 3);
                g2d.drawString("Enter를 눌러 무기 상점에 입장하세요.", startX, startY + lineHeight * 4);
                break;
            case 1: // 우주선 상점
                g2d.drawString("다양한 우주선을 구매할 수 있습니다.", startX, startY + lineHeight * 2);
                g2d.drawString("다양한 색상의 전투기, 평생지도교수님 등", startX, startY + lineHeight * 3);
                g2d.drawString("Enter를 눌러 우주선 상점에 입장하세요.", startX, startY + lineHeight * 4);
                break;
            // case 2: // 파워업 상점
            //     g2d.drawString("게임을 도와주는 파워업을 구매할 수 있습니다.", startX, startY + lineHeight * 2);
            //     g2d.drawString("속도 증가, 방어막, 추가 생명 등", startX, startY + lineHeight * 3);
            //     g2d.drawString("Enter를 눌러 파워업 상점에 입장하세요.", startX, startY + lineHeight * 4);
            //     break;
            // case 3: // 장식품 상점
            //     g2d.drawString("우주선을 꾸밀 수 있는 장식품을 구매할 수 있습니다.", startX, startY + lineHeight * 2);
            //     g2d.drawString("도색, 이펙트, 테마 등", startX, startY + lineHeight * 3);
            //     g2d.drawString("Enter를 눌러 장식품 상점에 입장하세요.", startX, startY + lineHeight * 4);
            //     break;
            // case 4: // 업그레이드 상점
            //     g2d.drawString("기존 아이템을 강화할 수 있습니다.", startX, startY + lineHeight * 2);
            //     g2d.drawString("무기 강화, 우주선 개조 등", startX, startY + lineHeight * 3);
            //     g2d.drawString("Enter를 눌러 업그레이드 상점에 입장하세요.", startX, startY + lineHeight * 4);
            //     break;
            // case 5: // 인벤토리
            //     g2d.drawString("보유한 아이템을 확인할 수 있습니다.", startX, startY + lineHeight * 2);
            //     g2d.drawString("구매한 무기, 우주선, 파워업 등", startX, startY + lineHeight * 3);
            //     g2d.drawString("Enter를 눌러 인벤토리를 확인하세요.", startX, startY + lineHeight * 4);
            //     break;
            // case 6: // 뒤로가기
            //     g2d.drawString("메인 메뉴로 돌아갑니다.", startX, startY + lineHeight * 2);
            //     g2d.drawString("확인하려면 Enter를 누르세요.", startX, startY + lineHeight * 3);
            //     break;
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
        // 전체 화면을 활용한 단일 패널
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(50, 50, 700, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, 50, 700, 500);
        
        // 카테고리 제목 표시
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String categoryTitle = getCategoryTitle(shopManager.getCurrentCategory());
        int titleX = 50 + (700 - titleMetrics.stringWidth(categoryTitle)) / 2;
        g2d.drawString(categoryTitle, titleX, 100);
        
        // 아이템들 표시
        drawCategoryItems(g2d, shopManager, inputHandler);
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
    
    private void drawCategoryItems(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 플레이어 코인 표시 (상단 우측) - 실시간 데이터
        int currentCoins = getCurrentCoins();
        if (coinImage != null) {
            g2d.drawImage(coinImage, 650, 70, 24, 24, null);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont);
            g2d.drawString(": " + currentCoins, 680, 90);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont);
            g2d.drawString("보유 코인: " + currentCoins, 650, 90);
        }
        
        // 해당 카테고리의 아이템 목록
        List<ShopItem> categoryItems = shopManager.getShopItems().stream()
                .filter(item -> item.getCategory() == shopManager.getCurrentCategory())
                .collect(java.util.stream.Collectors.toList());
        
        if (categoryItems.isEmpty()) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(menuFont);
            g2d.drawString("이 카테고리에는 아이템이 없습니다.", 350, 200);
            return;
        }
        
        // 아이템들을 그리드 형태로 표시 (3열로 확장)
        int itemsPerRow = 3;
        int panelWidth = 700; // 전체 패널 너비
        int panelStartX = 50; // 패널 시작 X 좌표
        int spacingX = 15; // 아이템 간 가로 간격
        int spacingY = 20; // 아이템 간 세로 간격
        int itemWidth = (panelWidth - spacingX * (itemsPerRow + 1)) / itemsPerRow; // 패널 너비에 맞게 계산
        int itemHeight = 150;
        int startX = panelStartX + spacingX;
        int startY = 130;
        
        for (int i = 0; i < categoryItems.size(); i++) {
            ShopItem item = categoryItems.get(i);
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            
            int x = startX + col * (itemWidth + spacingX);
            int y = startY + row * (itemHeight + spacingY);
            
            // 아이템 박스 그리기
            if (itemBoxImage != null) {
                // 선택된 아이템 강조 (노란색 테두리)
                if (i == inputHandler.getSelectedItem()) {
                    g2d.setColor(Color.YELLOW);
                    g2d.drawRect(x - 2, y - 2, itemWidth + 4, itemHeight + 4);
                }
                
                // 아이템 박스 이미지 그리기
                g2d.drawImage(itemBoxImage, x, y, itemWidth, itemHeight, null);
            } else {
                // 아이템 박스 이미지가 없으면 기본 박스 그리기
                if (i == inputHandler.getSelectedItem()) {
                    g2d.setColor(Color.YELLOW);
                } else {
                    g2d.setColor(new Color(0, 100, 200));
                }
                g2d.fillRect(x, y, itemWidth, itemHeight);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(x, y, itemWidth, itemHeight);
            }
            
            // 아이템 이미지 표시
            BufferedImage itemImage = loadItemImage(item.getIconPath());
            if (itemImage != null) {
                // 아이템 이미지를 박스 상단에 표시 (64x64 크기)
                int imageSize = 64;
                int imageX = x + (itemWidth - imageSize) / 2;
                int imageY = y + 20;
                g2d.drawImage(itemImage, imageX, imageY, imageSize, imageSize, null);
            }
            
            // 아이템 정보 표시
            g2d.setColor(Color.WHITE);
            g2d.setFont(itemFont);
            
            // 아이템 이름
            String itemName = item.getName();
            if (itemName.length() > 15) {
                itemName = itemName.substring(0, 15) + "...";
            }
            FontMetrics nameMetrics = g2d.getFontMetrics();
            int nameX = x + (itemWidth - nameMetrics.stringWidth(itemName)) / 2;
            int nameY = (itemImage != null) ? y + 100 : y + 45; // 이미지가 있으면 아래쪽에 표시
            g2d.drawString(itemName, nameX, nameY);
            
            // 아이템 가격
            g2d.setColor(Color.YELLOW);
            g2d.setFont(descriptionFont);
            String priceText = item.getPrice() + "";
            FontMetrics priceMetrics = g2d.getFontMetrics();
            int priceX = x + (itemWidth - priceMetrics.stringWidth(priceText)) / 2;
            int priceY = nameY + 20;
            g2d.drawString(priceText, priceX, priceY);
            
            // 코인 이미지 (가격 옆에)
            if (coinImage != null) {
                g2d.drawImage(coinImage, priceX + priceMetrics.stringWidth(priceText) + 5, priceY - 15, 16, 16, null);
            }
        }
        
        // 구매 메시지 표시
        if (shopManager.hasMessage()) {
            g2d.setColor(Color.RED);
            g2d.setFont(menuFont);
            String message = shopManager.getPurchaseMessage();
            FontMetrics messageMetrics = g2d.getFontMetrics();
            int messageX = 290 + (480 - messageMetrics.stringWidth(message)) / 2;
            g2d.drawString(message, messageX, 500);
        }
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
    // }
    
    private void drawItemDetail(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 아이템 상세 화면 구현
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("아이템 상세", 300, 100);
        
        // 아이템 정보 그리기
        // 구현 필요...
    }
    
    private void drawPurchaseConfirmation(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 구매 확인 창 배경 (반투명 검은색)
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, 800, 600);
        
        // 구매 확인 창 (흰색 테두리)
        int dialogWidth = 400;
        int dialogHeight = 250;
        int dialogX = (800 - dialogWidth) / 2;
        int dialogY = (600 - dialogHeight) / 2;
        
        g2d.setColor(new Color(50, 50, 50, 240));
        g2d.fillRect(dialogX, dialogY, dialogWidth, dialogHeight);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(dialogX, dialogY, dialogWidth, dialogHeight);
        
        // 아이템 정보
        ShopItem selectedItem = shopManager.getSelectedItem(inputHandler.getSelectedItem());
        if (selectedItem != null) {
            // 아이템 이름
            g2d.setColor(Color.WHITE);
            g2d.setFont(titleFont);
            String itemName = selectedItem.getName();
            FontMetrics nameMetrics = g2d.getFontMetrics();
            int nameX = dialogX + (dialogWidth - nameMetrics.stringWidth(itemName)) / 2;
            g2d.drawString(itemName, nameX, dialogY + 60);
            
            // 가격 정보
            g2d.setFont(menuFont);
            String priceText = "가격: " + selectedItem.getPrice() + " 코인";
            FontMetrics priceMetrics = g2d.getFontMetrics();
            int priceX = dialogX + (dialogWidth - priceMetrics.stringWidth(priceText)) / 2;
            g2d.drawString(priceText, priceX, dialogY + 100);
            
            // 코인 이미지
            if (coinImage != null) {
                g2d.drawImage(coinImage, priceX + priceMetrics.stringWidth(priceText) + 5, dialogY + 85, 20, 20, null);
            }
            
            // 구매 확인 질문
            g2d.setFont(descriptionFont);
            String questionText = "이 아이템을 구매하시겠습니까?";
            FontMetrics questionMetrics = g2d.getFontMetrics();
            int questionX = dialogX + (dialogWidth - questionMetrics.stringWidth(questionText)) / 2;
            g2d.drawString(questionText, questionX, dialogY + 140);
            
            // 예/아니요 버튼
            int buttonWidth = 80;
            int buttonHeight = 30;
            int buttonY = dialogY + 170;
            int yesX = dialogX + (dialogWidth / 2) - buttonWidth - 10;
            int noX = dialogX + (dialogWidth / 2) + 10;
            
            // 예 버튼
            if (inputHandler.getSelectedOption() == 0) {
                g2d.setColor(new Color(0, 150, 0, 200));
            } else {
                g2d.setColor(new Color(0, 100, 0, 150));
            }
            g2d.fillRect(yesX, buttonY, buttonWidth, buttonHeight);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(yesX, buttonY, buttonWidth, buttonHeight);
            
            g2d.setFont(descriptionFont);
            FontMetrics yesMetrics = g2d.getFontMetrics();
            int yesTextX = yesX + (buttonWidth - yesMetrics.stringWidth("예")) / 2;
            int yesTextY = buttonY + (buttonHeight + yesMetrics.getAscent()) / 2 - 2;
            g2d.drawString("예", yesTextX, yesTextY);
            
            // 아니요 버튼
            if (inputHandler.getSelectedOption() == 1) {
                g2d.setColor(new Color(150, 0, 0, 200));
            } else {
                g2d.setColor(new Color(100, 0, 0, 150));
            }
            g2d.fillRect(noX, buttonY, buttonWidth, buttonHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(noX, buttonY, buttonWidth, buttonHeight);
            
            g2d.setFont(descriptionFont);
            FontMetrics noMetrics = g2d.getFontMetrics();
            int noTextX = noX + (buttonWidth - noMetrics.stringWidth("아니요")) / 2;
            int noTextY = buttonY + (buttonHeight + noMetrics.getAscent()) / 2 - 2;
            g2d.drawString("아니요", noTextX, noTextY);
        }
    }
    
    private void drawInventory(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        // 전체 화면을 인벤토리 영역으로 사용
        g2d.setColor(new Color(0, 0, 0, 150)); // 반투명 검은색
        g2d.fillRect(50, 50, 700, 500);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, 50, 700, 500);
        
        // 제목 "인벤토리" (화면 상단 중앙)
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "인벤토리";
        int titleX = 50 + (700 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100);
        
        // 카테고리 탭 표시
        drawInventoryCategoryTabs(g2d, shopManager, inputHandler);
        
        // 현재 선택된 카테고리의 아이템들 표시
        ShopCategory currentCategory = shopManager.getInventoryCategory();
        List<ShopItem> categoryItems = shopManager.getInventoryByCategory(currentCategory);
        
        if (categoryItems.isEmpty()) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(menuFont);
            String message = currentCategory.getDisplayName() + " 카테고리에 아이템이 없습니다.";
            g2d.drawString(message, 50 + (700 - g2d.getFontMetrics().stringWidth(message)) / 2, 250);
            g2d.drawString("상점에서 아이템을 구매해보세요!", 50 + (700 - g2d.getFontMetrics().stringWidth("상점에서 아이템을 구매해보세요!")) / 2, 280);
        } else {
            drawInventoryItems(g2d, categoryItems, shopManager, inputHandler);
        }
        
        // 조작 안내
        g2d.setFont(menuFont);
        g2d.setColor(Color.YELLOW);
        String instructions = "Q: 카테고리 변경  ↑↓←→: 아이템 이동  Enter: 장착/해제  ESC: 뒤로가기";
        g2d.drawString(instructions, 50 + (700 - g2d.getFontMetrics().stringWidth(instructions)) / 2, 520);
    }
    
    private void drawInventoryCategoryTabs(Graphics2D g2d, ShopManager shopManager, ShopInputHandler inputHandler) {
        ShopCategory currentCategory = shopManager.getInventoryCategory();
        ShopCategory[] equippableCategories = {
            ShopCategory.WEAPONS,
            ShopCategory.SPACESHIPS
        };
        
        int tabWidth = 150;
        int tabHeight = 30;
        int totalWidth = (tabWidth + 10) * equippableCategories.length - 10; // 전체 탭 너비
        int startX = 50 + (700 - totalWidth) / 2; // 중앙정렬
        int startY = 120;
        
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
            
            // 탭 텍스트
            g2d.setFont(menuFont);
            FontMetrics metrics = g2d.getFontMetrics();
            String text = category.getDisplayName();
            int textX = x + (tabWidth - metrics.stringWidth(text)) / 2;
            int textY = y + (tabHeight + metrics.getAscent()) / 2 - 2;
            g2d.drawString(text, textX, textY);
        }
    }
    
    private void drawInventoryItems(Graphics2D g2d, List<ShopItem> items, ShopManager shopManager, ShopInputHandler inputHandler) {
        int itemsPerRow = 3; // 2에서 3으로 변경
        int panelWidth = 700;
        int panelStartX = 50;
        int spacingX = 15; // 간격을 좀 더 줄임
        int spacingY = 20;
        int itemWidth = (panelWidth - spacingX * (itemsPerRow + 1)) / itemsPerRow;
        int itemHeight = 120;
        int startX = panelStartX + spacingX;
        int startY = 170;
        
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            
            int x = startX + col * (itemWidth + spacingX);
            int y = startY + row * (itemHeight + spacingY);
            
            // 선택된 아이템인지 확인
            boolean isSelected = (i == inputHandler.getSelectedItem());
            
            // 아이템 박스 그리기
            if (itemBoxImage != null) {
                // 아이템 박스 이미지 그리기
                g2d.drawImage(itemBoxImage, x, y, itemWidth, itemHeight, null);
            } else {
                // 아이템 박스 이미지가 없으면 기본 박스 그리기
                if (isSelected) {
                    g2d.setColor(new Color(100, 100, 255)); // 선택된 아이템은 파란색
                } else {
                    g2d.setColor(new Color(0, 150, 0)); // 기본은 초록색
                }
                g2d.fillRect(x, y, itemWidth, itemHeight);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(x, y, itemWidth, itemHeight);
            }
            
            // 선택된 아이템은 추가 테두리 표시
            if (isSelected) {
                g2d.setColor(Color.CYAN);
                g2d.drawRect(x - 2, y - 2, itemWidth + 4, itemHeight + 4);
            }
            
            // 아이템 박스를 2등분: 왼쪽에 이미지, 오른쪽에 텍스트
            int leftWidth = itemWidth / 2;
            int rightWidth = itemWidth - leftWidth;
            int rightX = x + leftWidth;
            
            // 왼쪽: 아이템 이미지 표시
            BufferedImage itemImage = loadItemImage(item.getIconPath());
            if (itemImage != null) {
                // 아이템 이미지를 왼쪽 영역 중앙에 표시 (48x48 크기로 축소)
                int imageSize = 48;
                int imageX = x + (leftWidth - imageSize) / 2;
                int imageY = y + (itemHeight - imageSize) / 2;
                g2d.drawImage(itemImage, imageX, imageY, imageSize, imageSize, null);
            }
            
            // 오른쪽: 아이템 정보 표시
            g2d.setColor(Color.WHITE);
            g2d.setFont(itemFont);
            
            // 아이템 이름 (더 길게 표시 가능)
            String itemName = item.getName();
            if (itemName.length() > 15) {
                itemName = itemName.substring(0, 15) + "...";
            }
            FontMetrics nameMetrics = g2d.getFontMetrics();
            int nameX = rightX + 5; // 오른쪽 영역 시작점에서 5px 여백
            int nameY = y + 60; // 상단에서 25px 아래
            g2d.drawString(itemName, nameX, nameY);
            
            // 장착 상태 표시
            boolean isEquipped = shopManager.isItemEquipped(item);
            g2d.setFont(descriptionFont);
            String statusText = isEquipped ? "장착중" : "보유중";
            g2d.setColor(isEquipped ? Color.YELLOW : Color.GREEN);
            
            FontMetrics statusMetrics = g2d.getFontMetrics();
            int statusX = rightX + 5; // 오른쪽 영역 시작점에서 5px 여백
            int statusY = nameY + 20; // 이름 아래 20px
            g2d.drawString(statusText, statusX, statusY);
            
            // 장착된 아이템은 테두리를 노란색으로
            if (isEquipped) {
                g2d.setColor(Color.YELLOW);
                g2d.drawRect(x, y, itemWidth, itemHeight);
            }
        }
        
        // 인벤토리 아이템 개수 표시 (하단으로 이동하여 겹침 방지)
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        String itemCountText = "보유 아이템: " + items.size() + "개";
        g2d.drawString(itemCountText, 50 + (700 - g2d.getFontMetrics().stringWidth(itemCountText)) / 2, 480);
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