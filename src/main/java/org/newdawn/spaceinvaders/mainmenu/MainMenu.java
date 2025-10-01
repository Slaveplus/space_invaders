package org.newdawn.spaceinvaders.mainmenu;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.shop.Shop;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.login.User;

/**
 * 우주 배경을 사용한 메인 메뉴 시스템
 */
public class MainMenu {
    // 상점 클래스 연결
    private Shop shop;
    private boolean showingShop = false;
    private UserManager userManager;
    // private User currentUser; // 직접 사용하지 않으므로 제거
    private boolean logoutRequested = false;

    /** 현재 메뉴 상태 */
    public enum MenuState {
        MAIN,           // 메인 메뉴
        SINGLE_PLAYER,  // 싱글플레이 서브메뉴
        MULTI_PLAYER,   // 멀티플레이 서브메뉴
        SHOP,           // 상점 메뉴
        INVENTORY,      // 인벤토리 메뉴
        SETTINGS,        // 설정 메뉴
        ACCOUNT         // 계정 메뉴
    }
    
    private MenuState currentState = MenuState.MAIN;
    private int selectedOption = 0;
    private BufferedImage backgroundImage;
    private Font titleFont;
    private Font menuFont;
    private Font submenuFont;
    private boolean gameStartRequested = false;
    private boolean musicEnabled = true;
    
    // 싱글플레이 서브메뉴 옵션들
    private String[] singlePlayerOptions = {
        "새게임",
        "플레이기록",
        "이전메뉴"
    };
    
    // 멀티플레이 서브메뉴 옵션들
    private String[] multiPlayerOptions = {
        "게임참가",
        "리더보드",
        "이전메뉴"
    };
    
    // 설정 메뉴 옵션들
    private String[] settingsOptions = {
        "음악: " + (musicEnabled ? "ON" : "OFF"),
        "제작자",
        "이전메뉴"
    };

    // 계정 메뉴 옵션들
    private String[] accountOptions = {
        "계정 정보",
        "게임 통계", 
        "정보수정",
        "로그아웃",
        "이전메뉴"
    };
    
    public MainMenu(UserManager userManager) {
        loadBackgroundImage();
        initializeFonts();
        shop = new Shop(userManager); // UserManager를 Shop에 전달
        this.userManager = userManager;
    // currentUser 캐싱은 사용하지 않음 (UserManager에서 직접 조회)
    }
    
    /**
     * 메인 메뉴 진입 시 인벤토리 새로고침
     */
    public void refreshInventory() {
        if (shop != null && userManager != null && userManager.isLoggedIn()) {
            System.out.println("MainMenu: 인벤토리 새로고침 시작");
            shop.getShopManager().loadInventoryFromDB();
            shop.getShopManager().loadEquipmentFromDB();
        }
    }

    private String[] getMainMenuOptions() {
        String welcomeMessage = userManager != null && userManager.isLoggedIn() 
            ? "환영합니다! " + userManager.getCurrentUser().getUsername()
            : "게스트";
        
        return new String[]{
            "싱글플레이",
            "멀티플레이", 
            "상점",
            "인벤토리",
            "설정",
            welcomeMessage,  // 환영 메시지
            "게임 종료"
        };
    }
    
    /**
     * 사용자 정보를 가져옵니다 (디버깅용)
     */
    public String getUserInfo() {
        if (userManager != null && userManager.isLoggedIn()) {
            User user = userManager.getCurrentUser();
            if (user != null) {
                return "사용자: " + user.getUsername() + 
                       " (레벨: " + user.getLevel() + 
                       ", 코인: " + user.getCoins() + 
                       ", 젬: " + user.getGems() + 
                       ", 최고점수: " + user.getHighScore() + ")";
            }
        }
        return "게스트 사용자";
    }
    
    /**
     * 배경 이미지를 로드합니다
     */
    private void loadBackgroundImage() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/backgrounds/Background-0.jpg");
            if (inputStream != null) {
                backgroundImage = ImageIO.read(inputStream);
            }
        } catch (IOException e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
            // 기본 배경 이미지 생성
            backgroundImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = backgroundImage.createGraphics();
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, 800, 600);
            g2d.dispose();
        }
    }
    
    /**
     * 폰트를 초기화합니다
     */
    private void initializeFonts() {
        try {
            // Kostar 폰트 로드
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                Font kostarFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                titleFont = kostarFont.deriveFont(Font.BOLD, 48f);
                menuFont = kostarFont.deriveFont(Font.BOLD, 24f);
                submenuFont = kostarFont.deriveFont(Font.BOLD, 20f);
                fontStream.close();
            } else {
                // 폰트 로드 실패 시 기본 폰트 사용
                System.err.println("Kostar 폰트를 로드할 수 없습니다. 기본 폰트를 사용합니다.");
                titleFont = new Font("Arial", Font.BOLD, 48);
                menuFont = new Font("Arial", Font.BOLD, 24);
                submenuFont = new Font("Arial", Font.BOLD, 20);
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 중 오류 발생: " + e.getMessage());
            // 오류 발생 시 기본 폰트 사용
            titleFont = new Font("Arial", Font.BOLD, 48);
            menuFont = new Font("Arial", Font.BOLD, 24);
            submenuFont = new Font("Arial", Font.BOLD, 20);
        }
    }
    
    /**
     * 메뉴 업데이트 (현재는 별도 업데이트 불필요)
     */
    public void update() {
        // 메뉴는 별도 업데이트 불필요
    }
    
    /**
     * 마우스 클릭을 처리합니다
     */
    public void handleMouseClick(int x, int y) {
        // 상점 마우스 클릭 처리
        if (showingShop) {
            // 상점은 마우스 지원이 필요하면 나중에 추가
            return;
        }
        
        // 메뉴 옵션 클릭 처리
        String[] options = getCurrentMenuOptions();
        int startY = 200;
        int lineHeight = 50;
        
        for (int i = 0; i < options.length; i++) {
            int optionY = startY + (i * lineHeight);
            if (x >= 300 && x <= 500 && y >= optionY - 20 && y <= optionY + 20) {
                selectedOption = i;
                handleMenuSelection();
                System.out.println("마우스 클릭: " + options[i]);
                break;
            }
        }
    }
    
    /**
     * 키 입력을 처리합니다
     */
    public void handleKeyInput(int keyCode) {
        // 상점 키 입력 처리
        if (showingShop) {
            shop.handleKeyInput(keyCode);
            if (shop.shouldExitShop()) {
                showingShop = false;
                shop.reset(); // exitRequested 플래그 리셋
            }
            return;
        }
        
        // 인벤토리 키 입력 처리
        if (currentState == MenuState.INVENTORY) {
            if (keyCode == KeyEvent.VK_ESCAPE) {
                // ESC 키는 직접 메인 메뉴로 돌아가기
                currentState = MenuState.MAIN;
                selectedOption = 3; // 인벤토리 옵션으로 돌아가기
                System.out.println("MainMenu: 인벤토리에서 메인 메뉴로 돌아가기");
                return;
            } else {
                // 다른 키는 상점의 인벤토리 입력 처리
                shop.handleInventoryInput(keyCode);
                return;
            }
        }
        
        switch (keyCode) {
            case KeyEvent.VK_UP:
                selectedOption = Math.max(0, selectedOption - 1);
                break;
            case KeyEvent.VK_DOWN:
                selectedOption = Math.min(getCurrentMenuOptions().length - 1, selectedOption + 1);
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                handleMenuSelection();
                break;
            case KeyEvent.VK_ESCAPE:
                if (currentState == MenuState.INVENTORY) {
                    currentState = MenuState.MAIN;
                    selectedOption = 3; // 인벤토리 옵션으로 돌아가기
                } else if (currentState != MenuState.MAIN) {
                    currentState = MenuState.MAIN;
                    selectedOption = 0;
                }
                break;
        }
    }
    
    /**
     * 현재 메뉴의 옵션들을 반환합니다
     */
    private String[] getCurrentMenuOptions() {
        switch (currentState) {
            case SINGLE_PLAYER:
                return singlePlayerOptions;
            case MULTI_PLAYER:
                return multiPlayerOptions;
            case SETTINGS:
                return settingsOptions;
            case ACCOUNT:
                return accountOptions;
            case INVENTORY:
                return new String[]{"뒤로가기"}; // 인벤토리는 뒤로가기만
            case SHOP:
                // 별도 Shop 화면을 갖고 있으므로 여기서는 기본 옵션 반환
                return new String[]{"뒤로가기"};
            default:
                return getMainMenuOptions(); // 동적으로 생성된 메인 메뉴 옵션
        }
    }
    
    /**
     * 메뉴 선택을 처리합니다
     */
    private void handleMenuSelection() {
    String[] options = getCurrentMenuOptions();
        if (selectedOption >= options.length) return;
        
        switch (currentState) {
            case MAIN:
                handleMainMenuSelection();
                break;
            case SINGLE_PLAYER:
                handleSinglePlayerSelection();
                break;
            case MULTI_PLAYER:
                handleMultiPlayerSelection();
                break;
            case SETTINGS:
                handleSettingsSelection();
                break;
            case ACCOUNT:
                handleAccountSelection();
                break;
            case INVENTORY:
                handleInventorySelection();
                break;
            case SHOP:
                // 현재 구조에서 SHOP 상태는 별도 Shop 화면으로 대체됨
                showingShop = true;
                shop.reset();
                break;
        }
    }
    
    /**
     * 메인 메뉴 선택을 처리합니다
     */
    private void handleMainMenuSelection() {
        switch (selectedOption) {
            case 0: // 싱글플레이
                currentState = MenuState.SINGLE_PLAYER;
                selectedOption = 0;
                break;
            case 1: // 멀티플레이
                currentState = MenuState.MULTI_PLAYER;
                selectedOption = 0;
                break;
            case 2: // 상점
                showingShop = true;
                shop.reset();
                break;
            case 3: // 인벤토리
                currentState = MenuState.INVENTORY;
                selectedOption = 0;
                break;
            case 4: // 설정
                currentState = MenuState.SETTINGS;
                selectedOption = 0;
                break;
            case 5: // 계정 (사용자 이메일)
                currentState = MenuState.ACCOUNT;
                selectedOption = 0;
                break;
            case 6: // 게임 종료
                System.exit(0);
                break;
        }
    }
    
    /**
     * 싱글플레이 메뉴 선택을 처리합니다
     */
    private void handleSinglePlayerSelection() {
        switch (selectedOption) {
            case 0: // 새게임
                gameStartRequested = true;
                break;
            case 1: // 플레이기록
                System.out.println("플레이기록을 표시합니다.");
                break;
            case 2: // 이전메뉴
                currentState = MenuState.MAIN;
                selectedOption = 0;
                break;
        }
    }
    
    /**
     * 멀티플레이 메뉴 선택을 처리합니다
     */
    private void handleMultiPlayerSelection() {
        switch (selectedOption) {
            case 0: // 게임참가
                System.out.println("멀티플레이 게임에 참가합니다.");
                break;
            case 1: // 리더보드
                System.out.println("리더보드를 표시합니다.");
                break;
            case 2: // 이전메뉴
                currentState = MenuState.MAIN;
                selectedOption = 0;
                break;
        }
    }
    
    
    /**
     * 설정 메뉴 선택을 처리합니다
     */
    private void handleSettingsSelection() {
        switch (selectedOption) {
            case 0: // 음악 on/off
                musicEnabled = !musicEnabled;
                settingsOptions[0] = "음악: " + (musicEnabled ? "ON" : "OFF");
                System.out.println("음악이 " + (musicEnabled ? "켜졌습니다" : "꺼졌습니다"));
                break;
            case 1: // 제작자 정보
                System.out.println("제작자: 권지민(팀장), 김승현, 장진우");
                break;
            case 2: // 이전메뉴
                currentState = MenuState.MAIN;
                selectedOption = 0;
                break;
        }
    }

    private void handleAccountSelection() {
        switch (selectedOption) {
            case 0: // 계정 정보
                // 계정 정보 표시 (구현 필요)
                break;
            case 1: // 게임 통계
                // 게임 통계 표시 (구현 필요)
                break;
            case 2: // 정보수정
                // 정보수정 화면 (구현 필요)
                break;
            case 3: // 로그아웃
                if (userManager != null) {
                    userManager.logoutUser();
                }
                // 즉시 메인 메뉴 상태로 돌아가 선택값 초기화 (다음 화면 전환 시 깔끔한 상태)
                currentState = MenuState.MAIN;
                selectedOption = 0;
                logoutRequested = true;
                System.out.println("로그아웃 완료");
                break;
            case 4: // 이전메뉴
                currentState = MenuState.MAIN;
                selectedOption = 0;
                break;
        }
    }
    
    private void handleInventorySelection() {
        switch (selectedOption) {
            case 0: // 뒤로가기
                currentState = MenuState.MAIN;
                selectedOption = 3; // 인벤토리 옵션으로 돌아가기
                break;
        }
    }
    
    /**
     * 메뉴를 그립니다
     */
    public void draw(Graphics2D g2d) {
        // 상점이 표시중이면 상점을 그리기
        if (showingShop) {
            shop.update(); // 메시지 타이머 업데이트
            shop.draw(g2d);
            return;
        }
        
        // 인벤토리가 표시중이면 상점의 인벤토리를 그리기
        if (currentState == MenuState.INVENTORY) {
            shop.setCurrentState(org.newdawn.spaceinvaders.shop.ShopState.INVENTORY);
            shop.draw(g2d);
            return;
        }
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, 800, 600);
        
        // 제목 그리기
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "SPACE INVADERS";
        int titleX = (800 - titleMetrics.stringWidth(title)) / 2;
        int titleY = 120;
        g2d.drawString(title, titleX, titleY);
        
        // 상태에 따라 다른 화면 그리기
        if (currentState == MenuState.ACCOUNT) {
            drawAccountMenu(g2d);
        } else if (currentState == MenuState.SETTINGS) {
            drawSettingsMenu(g2d);
        } else {
            // 메뉴 옵션들 그리기
            drawMenuOptions(g2d);
            
            // 조작 안내
            drawControls(g2d);
        }
    }
    
    /**
     * 메뉴 옵션들을 그립니다
     */
    private void drawMenuOptions(Graphics2D g2d) {
        String[] options = getCurrentMenuOptions();
        Font currentFont = (currentState == MenuState.MAIN) ? menuFont : submenuFont;
        g2d.setFont(currentFont);
        
        int startY = 200;
        int lineHeight = 50;
        
        for (int i = 0; i < options.length; i++) {
            Color textColor = (i == selectedOption) ? Color.YELLOW : Color.WHITE;
            g2d.setColor(textColor);
            
            FontMetrics metrics = g2d.getFontMetrics();
            int x = (800 - metrics.stringWidth(options[i])) / 2;
            int y = startY + (i * lineHeight);
            
            // 선택된 항목 강조
            if (i == selectedOption) {
                g2d.setColor(new Color(255, 255, 0, 100));
                g2d.fillRect(x - 10, y - metrics.getHeight() + 5, 
                           metrics.stringWidth(options[i]) + 20, metrics.getHeight() + 10);
                g2d.setColor(Color.YELLOW);
            }
            
            g2d.drawString(options[i], x, y);
        }
    }

    private void drawAccountMenu(Graphics2D g2d) {
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, 800, 600);
        
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
        
        // 제목 "계정"
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "계정";
        int titleX = 50 + (200 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100);
        
        // 메뉴 옵션들 (왼쪽 패널)
        String[] options = accountOptions;
        g2d.setFont(menuFont);
        
        int startY = 140;
        int lineHeight = 40;
        
        for (int i = 0; i < options.length; i++) {
            // 선택된 항목 강조
            if (i == selectedOption) {
                g2d.setColor(Color.WHITE);
                g2d.drawRect(60, startY + (i * lineHeight) - 25, 180, 30);
                
                // 화살표 그리기
                g2d.setColor(Color.YELLOW);
                g2d.drawString("→", 220, startY + (i * lineHeight));
            }
            
            g2d.setColor(i == selectedOption ? Color.YELLOW : Color.WHITE);
            int x = 70;
            int y = startY + (i * lineHeight);
            g2d.drawString(options[i], x, y);
        }
        
        // 오른쪽 패널에 선택된 항목의 상세 정보 표시
        drawAccountDetails(g2d);
    }
    
    private void drawAccountDetails(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        
        int startX = 290;
        int startY = 100;
        int lineHeight = 50;
        
        // 디버깅 정보 표시
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("UserManager 상태: " + (userManager != null ? "존재" : "null"), 50, 50);
        g2d.drawString("로그인 상태: " + (userManager != null ? userManager.isLoggedIn() : "false"), 50, 70);
        
        if (userManager != null && userManager.isLoggedIn()) {
            User currentUser = userManager.getCurrentUser();
            g2d.drawString("현재 사용자: " + (currentUser != null ? currentUser.getUsername() : "null"), 50, 90);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(menuFont);
            
            // 선택된 항목에 따라 다른 정보 표시
            switch (selectedOption) {
                case 0: // 계정정보
                    g2d.drawString("이메일 : " + currentUser.getEmail(), startX, startY);
                    g2d.drawString("레벨 : " + currentUser.getLevel(), startX, startY + lineHeight);
                    g2d.drawString("최고점수 : " + currentUser.getHighScore(), startX, startY + lineHeight * 2);
                    g2d.drawString("총 게임 수 : " + currentUser.getTotalGamesPlayed(), startX, startY + lineHeight * 3);
                    g2d.drawString("보유코인 : " + currentUser.getCoins(), startX, startY + lineHeight * 4);
                    g2d.drawString("보유젬 : " + currentUser.getGems(), startX, startY + lineHeight * 5);
                    break;
                case 1: // 게임통계
                    g2d.drawString("총 게임 수 : " + currentUser.getTotalGamesPlayed(), startX, startY);
                    g2d.drawString("승리 횟수 : " + currentUser.getTotalWins(), startX, startY + lineHeight);
                    g2d.drawString("승률 : " + String.format("%.1f", currentUser.getWinRate()) + "%", startX, startY + lineHeight * 2);
                    g2d.drawString("최고 점수 : " + currentUser.getHighScore(), startX, startY + lineHeight * 3);
                    g2d.drawString("현재 레벨 : " + currentUser.getLevel(), startX, startY + lineHeight * 4);
                    break;
                case 2: // 정보수정
                    g2d.drawString("정보수정 기능은", startX, startY);
                    g2d.drawString("추후 개발 예정입니다.", startX, startY + lineHeight);
                    break;
                case 3: // 로그아웃
                    g2d.drawString("로그아웃을 하시겠습니까?", startX, startY);
                    g2d.drawString("확인하려면 Enter를 누르세요.", startX, startY + lineHeight);
                    break;
                case 4: // 이전메뉴
                    g2d.drawString("메인 메뉴로 돌아갑니다.", startX, startY);
                    g2d.drawString("확인하려면 Enter를 누르세요.", startX, startY + lineHeight);
                    break;
            }
        } else {
            g2d.drawString("로그인이 필요합니다.", startX, startY);
        }
    }
    
    private void drawSettingsMenu(Graphics2D g2d) {
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, 800, 600);
        
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
        
        // 제목 "설정"
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "설정";
        int titleX = 50 + (200 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 80);
        
        // 메뉴 옵션들 (왼쪽 패널)
        String[] options = settingsOptions;
        g2d.setFont(menuFont);
        
        int startY = 120;
        int lineHeight = 40;
        
        for (int i = 0; i < options.length; i++) {
            // 선택된 항목 강조
            if (i == selectedOption) {
                g2d.setColor(Color.WHITE);
                g2d.drawRect(60, startY + (i * lineHeight) - 25, 180, 30);
                
                // 화살표 그리기
                g2d.setColor(Color.YELLOW);
                g2d.drawString("→", 220, startY + (i * lineHeight));
            }
            
            g2d.setColor(i == selectedOption ? Color.YELLOW : Color.WHITE);
            // int x = 70; // metrics 사용 제거하고 고정 여백 사용
            int x = 70;
            int y = startY + (i * lineHeight);
            g2d.drawString(options[i], x, y);
        }
        
        // 오른쪽 패널에 선택된 항목의 상세 정보 표시
        drawSettingsDetails(g2d);
    }
    
    private void drawSettingsDetails(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        
        int startX = 290;
        int startY = 100;
        int lineHeight = 30;
        
        // 선택된 항목에 따라 다른 정보 표시
        switch (selectedOption) {
            case 0: // 음악 on/off
                g2d.drawString("게임 배경음악을 켜거나 끌 수 있습니다.", startX, startY);
                g2d.drawString("현재 상태: " + (musicEnabled ? "ON" : "OFF"), startX, startY + lineHeight);
                g2d.drawString("Enter를 눌러 상태를 변경하세요.", startX, startY + lineHeight * 2);
                break;
            case 1: // 제작자 정보
                g2d.drawString("게임 제작자 정보", startX, startY);
                g2d.drawString("권지민 - 프로그래밍", startX, startY + lineHeight);
                g2d.drawString("김승현 - 디자인", startX, startY + lineHeight * 2);
                g2d.drawString("장진우 - 기획", startX, startY + lineHeight * 3);
                break;
            case 2: // 이전메뉴
                g2d.drawString("메인 메뉴로 돌아갑니다.", startX, startY);
                g2d.drawString("확인하려면 Enter를 누르세요.", startX, startY + lineHeight);
                break;
        }
    }
    
    /**
     * 조작 안내를 그립니다
     */
    private void drawControls(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("↑↓: 이동  Enter/Space: 선택  ESC: 뒤로가기", 280, 550);
    }
    
    /**
     * 새게임 시작 여부를 확인합니다
     */
    public boolean shouldStartGame() {
        return gameStartRequested;
    }
    
    /**
     * 메뉴 상태를 초기화합니다
     */
    public void reset() {
        currentState = MenuState.MAIN;
        selectedOption = 0;
        gameStartRequested = false;
        showingShop = false;
        logoutRequested = false;
        if (shop != null) {
            shop.reset();
        }
    }
    
    /**
     * 현재 메뉴 상태를 반환합니다
     */
    public MenuState getCurrentState() {
        return currentState;
    }

    // 상점 메뉴 선택 처리
    // 상점 메뉴 선택 처리는 handleMainMenuSelection과 SHOP case에서 처리

    public boolean isShowingShop() {
        return showingShop;
    }

    public UserManager getUserManager() {
        return userManager;
    }
    
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
        // 상점에도 UserManager 전달 (실시간 DB 동기화용)
        if (shop != null) {
            shop.setUserManager(userManager);
        }
    }
    
    public boolean isLogoutRequested() {
        return logoutRequested;
    }
    
    public void resetLogoutRequest() {
        logoutRequested = false;
    }
        
}
