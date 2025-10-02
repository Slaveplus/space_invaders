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
            case 2: // 뒤로가기
                g2d.drawString("메인화면으로 이동합니다.", startX, startY + lineHeight * 2);
                g2d.drawString("확인하려면 Enter를 누르세요.", startX, startY + lineHeight * 3);
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
            
            // 아이템 박스 그리기 (구매 상태에 따라 다른 색상)
            boolean isPurchased = item.isPurchased();
            boolean isSelected = (i == inputHandler.getSelectedItem());
            
            if (itemBoxImage != null) {
                // 선택된 아이템 강조 (노란색 테두리)
                if (isSelected) {
                    g2d.setColor(Color.YELLOW);
                    g2d.drawRect(x - 2, y - 2, itemWidth + 4, itemHeight + 4);
                }
                
                // 아이템 박스 이미지 그리기
                g2d.drawImage(itemBoxImage, x, y, itemWidth, itemHeight, null);
                
                // 구매된 아이템은 반투명 오버레이 추가
                if (isPurchased) {
                    g2d.setColor(new Color(0, 255, 0, 100)); // 초록색 반투명
                    g2d.fillRect(x, y, itemWidth, itemHeight);
                }
            } else {
                // 아이템 박스 이미지가 없으면 기본 박스 그리기
                if (isSelected) {
                    g2d.setColor(Color.YELLOW);
                } else if (isPurchased) {
                    g2d.setColor(new Color(0, 150, 0)); // 구매된 아이템은 초록색
                } else {
                    g2d.setColor(new Color(0, 100, 200)); // 구매 가능한 아이템은 파란색
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
            
            // 아이템 가격 또는 상태 표시
            g2d.setFont(descriptionFont);
            String statusText;
            Color statusColor;
            
            if (isPurchased) {
                statusText = "보유중";
                statusColor = Color.GREEN;
            } else {
                statusText = item.getPrice() + " 코인";
                statusColor = Color.YELLOW;
            }
            
            g2d.setColor(statusColor);
            FontMetrics statusMetrics = g2d.getFontMetrics();
            int statusX = x + (itemWidth - statusMetrics.stringWidth(statusText)) / 2;
            int statusY = nameY + 20;
            g2d.drawString(statusText, statusX, statusY);
            
            // 코인 이미지 (구매 가능한 아이템에만)
            if (!isPurchased && coinImage != null) {
                g2d.drawImage(coinImage, statusX + statusMetrics.stringWidth(statusText) + 5, statusY - 15, 16, 16, null);
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
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // 단어가 너무 긴 경우 강제로 자르기
                    lines.add(word.substring(0, Math.min(word.length(), maxLength)));
                    if (word.length() > maxLength) {
                        currentLine = new StringBuilder(word.substring(maxLength));
                    }
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
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
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (fontMetrics.stringWidth(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // 단어가 너무 긴 경우 강제로 자르기
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
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
        // 구매 확인 창 배경 (반투명 검은색)
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, 800, 600);
        
        // 구매 확인 창 (더 큰 크기로 변경)
        int dialogWidth = 600;
        int dialogHeight = 450;
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
            // 아이템 이미지 (왼쪽에 큰 이미지)
            int imageSize = 180;
            int imageX = dialogX + 20;
            int imageY = dialogY + 40;
            
            try {
                // 아이템 이미지 로드 및 표시
                java.awt.image.BufferedImage itemImage = javax.imageio.ImageIO.read(
                    getClass().getClassLoader().getResourceAsStream(selectedItem.getIconPath())
                );
                if (itemImage != null) {
                    g2d.drawImage(itemImage, imageX, imageY, imageSize, imageSize, null);
                }
            } catch (Exception e) {
                // 이미지 로드 실패 시 기본 사각형 표시
                g2d.setColor(new Color(100, 100, 100, 150));
                g2d.fillRect(imageX, imageY, imageSize, imageSize);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(imageX, imageY, imageSize, imageSize);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("이미지 없음", imageX + 10, imageY + imageSize/2);
            }
            
            // 아이템 정보 (오른쪽에 배치)
            int infoX = dialogX + 220;
            int infoY = dialogY + 50;
            
            // 아이템 이름 (더 큰 폰트)
            g2d.setColor(Color.WHITE);
            g2d.setFont(titleFont.deriveFont(32f));
            String itemName = selectedItem.getName();
            g2d.drawString(itemName, infoX, infoY + 40);
            
            // 아이템 설명 (여러 줄로 표시)
            g2d.setFont(descriptionFont.deriveFont(16f));
            String description = selectedItem.getDescription();
            String[] descriptionLines = wrapText(description, 25); // 25자씩 줄바꿈
            int lineHeight = 25;
            for (int i = 0; i < descriptionLines.length && i < 3; i++) {
                g2d.setColor(new Color(200, 200, 200));
                g2d.drawString(descriptionLines[i], infoX, infoY + 80 + (i * lineHeight));
            }
            
            // 희귀도 표시
            g2d.setFont(menuFont.deriveFont(18f));
            g2d.setColor(getRarityColor(selectedItem.getRarity()));
            g2d.drawString(selectedItem.getRarity().getDisplayName(), infoX, infoY + 160);
            
            // 가격 정보 (더 크게)
            g2d.setColor(Color.YELLOW);
            g2d.setFont(menuFont.deriveFont(20f));
            String priceText = "가격: " + selectedItem.getPrice() + " 코인";
            FontMetrics priceMetrics = g2d.getFontMetrics();
            g2d.drawString(priceText, infoX, infoY + 190);
            
            // 코인 이미지 (더 크게)
            if (coinImage != null) {
                g2d.drawImage(coinImage, infoX + priceMetrics.stringWidth(priceText) + 10, infoY + 175, 30, 30, null);
            }
            
            // 구매 확인 질문 (중앙 하단)
            g2d.setColor(Color.WHITE);
            g2d.setFont(menuFont.deriveFont(18f));
            String questionText = "이 아이템을 구매하시겠습니까?";
            FontMetrics questionMetrics = g2d.getFontMetrics();
            int questionX = dialogX + (dialogWidth - questionMetrics.stringWidth(questionText)) / 2;
            g2d.drawString(questionText, questionX, dialogY + 350);
            
            // 예/아니요 버튼 (더 크게)
            int buttonWidth = 100;
            int buttonHeight = 40;
            int buttonY = dialogY + 380;
            int yesX = dialogX + (dialogWidth / 2) - buttonWidth - 15;
            int noX = dialogX + (dialogWidth / 2) + 15;
            
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
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("!", iconX + iconSize/2 - 4, iconY + iconSize - 5);
        
        // 경고 메시지
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        int messageX = iconX + iconSize + 20;
        int messageY = dialogY + 50;
        g2d.drawString(message, messageX, messageY);
        
        // 자동 닫힘 안내
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.LIGHT_GRAY);
        String autoCloseText = "이 경고창은 자동으로 닫힙니다";
        FontMetrics autoMetrics = g2d.getFontMetrics();
        int autoX = dialogX + (dialogWidth - autoMetrics.stringWidth(autoCloseText)) / 2;
        int autoY = dialogY + dialogHeight - 15;
        g2d.drawString(autoCloseText, autoX, autoY);
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
            
            // 아이템 이름 (줄바꿈 처리)
            String itemName = item.getName();
            FontMetrics nameMetrics = g2d.getFontMetrics();
            int nameX = rightX + 5; // 오른쪽 영역 시작점에서 5px 여백
            int nameY = y + 50; // 상단에서 30px 아래
            
            // 오른쪽 영역 너비에서 여백을 뺀 실제 사용 가능한 너비
            int availableWidth = rightWidth - 10; // 양쪽 여백 5px씩
            
            // 텍스트가 너비를 초과하는지 확인
            if (nameMetrics.stringWidth(itemName) > availableWidth) {
                // 단어 단위로 줄바꿈 처리
                String[] nameLines = wrapTextByWidth(itemName, availableWidth, nameMetrics);
                for (int lineIndex = 0; lineIndex < nameLines.length && lineIndex < 2; lineIndex++) { // 최대 2줄
                    g2d.drawString(nameLines[lineIndex], nameX, nameY + (lineIndex * 15));
                }
            } else {
                g2d.drawString(itemName, nameX, nameY);
            }
            
            // 장착 상태 표시
            boolean isEquipped = shopManager.isItemEquipped(item);
            g2d.setFont(descriptionFont);
            String statusText = isEquipped ? "장착중" : "보유중";
            g2d.setColor(isEquipped ? Color.YELLOW : Color.GREEN);
            
            int statusX = rightX + 5; // 오른쪽 영역 시작점에서 5px 여백
            int statusY = nameY + 35; // 이름 아래 35px (줄바꿈을 고려하여 더 아래로)
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