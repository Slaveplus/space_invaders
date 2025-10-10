package org.newdawn.spaceinvaders.mainmenu;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.newdawn.spaceinvaders.shop.Shop;
import org.newdawn.spaceinvaders.shop.ShopAnimation;
import org.newdawn.spaceinvaders.login.UserManager;
import org.newdawn.spaceinvaders.login.User;
import org.newdawn.spaceinvaders.app.ScreenNavigator;
import org.newdawn.spaceinvaders.room.GameClient;

/**
 * 우주 배경을 사용한 메인 메뉴 시스템
 */
import java.util.Properties;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;

public class MainMenu {
    // Kostar 폰트 로드
    private static Font KOSTAR_FONT = null;
    
    static {
        loadKostarFont();
    }
    
    /**
     * Kostar 폰트 로드
     */
    private static void loadKostarFont() {
        try {
            InputStream fontStream = MainMenu.class.getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                KOSTAR_FONT = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontStream.close();
                System.out.println("✅ MainMenu Kostar 폰트 로드 성공");
            } else {
                System.err.println("❌ MainMenu Kostar 폰트 파일을 찾을 수 없습니다");
                KOSTAR_FONT = new Font("Arial", Font.PLAIN, 12); // 폴백
            }
        } catch (Exception e) {
            System.err.println("❌ MainMenu Kostar 폰트 로드 실패: " + e.getMessage());
            KOSTAR_FONT = new Font("Arial", Font.PLAIN, 12); // 폴백
        }
    }
    
    /**
     * Kostar 폰트를 지정된 크기로 반환
     */
    public static Font getKostarFont(int size) {
        if (KOSTAR_FONT != null) {
            return KOSTAR_FONT.deriveFont(Font.PLAIN, size);
        }
        return new Font("Arial", Font.PLAIN, size);
    }
    
    /**
     * Kostar 폰트를 지정된 크기와 스타일로 반환
     */
    public static Font getKostarFont(int style, int size) {
        if (KOSTAR_FONT != null) {
            return KOSTAR_FONT.deriveFont(style, size);
        }
        return new Font("Arial", style, size);
    }
    
    // 상점 클래스 연결
    private Shop shop;
    private boolean showingShop = false;
    private UserManager userManager;
    // private User currentUser; // 직접 사용하지 않으므로 제거
    private boolean logoutRequested = false;
    private ScreenNavigator navigator;

    /** 현재 메뉴 상태 */
    public enum MenuState {
        MAIN,           // 메인 메뉴
        SINGLE_PLAYER,  // 싱글플레이 서브메뉴
        MULTI_PLAYER,   // 멀티플레이 서브메뉴
        SHOP,           // 상점 메뉴
        INVENTORY,      // 인벤토리 메뉴
        SETTINGS,       // 설정 메뉴
        RESOLUTION,     // 해상도 변경 메뉴
        ACCOUNT,        // 계정 메뉴
        LEADERBOARD,    // 플레이기록 메뉴
        SERVER_CONNECT  // 서버 접속 정보 입력
    }
    
    private MenuState currentState = MenuState.MAIN;
    private int selectedOption = 0;
    private BufferedImage backgroundImage;
    private Font titleFont;
    private Font menuFont;
    private Font submenuFont;
    private boolean gameStartRequested = false;
    private boolean musicEnabled = true;
    
    // 애니메이션 시스템
    private ShopAnimation settingsAnimation;
    private ShopAnimation accountAnimation;
    
    // 플레이기록 관련
    private PlayRecordsManager playRecordsManager;
    
    // 싱글플레이 서브메뉴 옵션들
    private String[] singlePlayerOptions = {
        "새게임",
        "플레이기록",
        "이전메뉴"
    };
    
    // 설정 메뉴 옵션들 (동적 생성)
    private String[] getSettingsOptions() {
        return new String[] {
            "배경음악 " + (musicEnabled ? "ON" : "OFF"),
            "해상도 변경",
            "제작자",
            "이전메뉴"
        };
    }
    
    // 해상도 옵션들
    private String[] resolutionOptions = {
        "800x600",
        "1024x768",
        "1440x1080",
        "이전메뉴"
    };
    
    // 해상도 값들 (resolutionOptions와 매칭)
    private int[][] resolutionValues = {
        {800, 600},
        {1024, 768},
        {1440, 1080},
        {0, 0} // 이전메뉴는 무시
    };
    
    // 계정 메뉴 옵션들
    private String[] accountOptions = {
        "계정 정보",
        "게임 통계", 
        "정보수정",
        "로그아웃",
        "이전메뉴"
    };
    
    public MainMenu(UserManager userManager, ScreenNavigator navigator) {
        loadBackgroundImage();
        initializeFonts();
        shop = new Shop(userManager); // UserManager를 Shop에 전달
        this.userManager = userManager;
        this.navigator = navigator;
        // 플레이기록 관리자 초기화
        this.playRecordsManager = new PlayRecordsManager(userManager);
        // 애니메이션 초기화
        this.settingsAnimation = new ShopAnimation();
        this.accountAnimation = new ShopAnimation();
    // currentUser 캐싱은 사용하지 않음 (UserManager에서 직접 조회)
    }
    
    /**
     * 메인 메뉴 진입 시 인벤토리 새로고침
     */
    public void refreshInventory() {
        if (shop != null && userManager != null && userManager.isLoggedIn()) {
            shop.getShopManager().loadInventoryFromDB();
            shop.getShopManager().loadEquipmentFromDB();
        }
    }

    private String[] getMainMenuOptions() {
        String welcomeMessage = userManager != null && userManager.isLoggedIn() 
            ? "환영합니다! " + userManager.getCurrentUser().getUsername()
            : "게스트";
        
        return new String[]{
            "게임플레이",
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
                titleFont = getKostarFont(Font.BOLD, 48);
                menuFont = getKostarFont(Font.BOLD, 24);
                submenuFont = getKostarFont(Font.BOLD, 20);
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 중 오류 발생: " + e.getMessage());
            // 오류 발생 시 기본 폰트 사용
            titleFont = getKostarFont(Font.BOLD, 48);
            menuFont = getKostarFont(Font.BOLD, 24);
            submenuFont = getKostarFont(Font.BOLD, 20);
        }
    }
    
    /**
     * 메뉴 업데이트 (현재는 별도 업데이트 불필요)
     */
    public void update() {
        // 인벤토리 상태일 때도 상점 타이머 업데이트
        if (currentState == MenuState.INVENTORY && shop != null) {
            shop.update();
        }
        
        // 애니메이션 업데이트
        if (settingsAnimation != null) {
            settingsAnimation.update();
        }
        if (accountAnimation != null) {
            accountAnimation.update();
        }
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
            // 상점의 입력 핸들러로 전달
            shop.handleInventoryInput(keyCode);
            
            // 상점에서 종료 요청이 있으면 상점 종료
            if (shop.getShopManager().getInputHandler().isExitRequested()) {
                // 상점 나가기 애니메이션 시작
                shop.startExitAnimation();
                showingShop = false;
                shop.getShopManager().getInputHandler().resetExitRequest();
            }
            return;
        }
        
        // 인벤토리 키 입력 처리
        if (currentState == MenuState.INVENTORY) {
            if (keyCode == KeyEvent.VK_ESCAPE) {
                // 변경사항이 있는지 확인하고 저장 처리
                handleInventoryExit();
                return;
            } else {
                // 다른 키는 상점의 인벤토리 입력 처리
                shop.handleInventoryInput(keyCode);
                return;
            }
        }
        
        // 리더보드 키 입력 처리
        if (currentState == MenuState.LEADERBOARD) {
            handleLeaderboardInput(keyCode);
            return;
        }
        
        // 서버 연결 입력 모드일 경우 별도 처리 (방향키 중 일부는 옵션 이동 대신 입력 전용)
        if (currentState == MenuState.SERVER_CONNECT) {
            if (keyCode == KeyEvent.VK_ESCAPE) {
                currentState = MenuState.MULTI_PLAYER;
                selectedOption = 0;
                return;
            }
            handleServerConnectKey(keyCode);
            return;
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
                } else if (currentState == MenuState.RESOLUTION) {
                    // 설정 메뉴의 좌우 패널 애니메이션 시작 (상점과 동일한 방식)
                    if (settingsAnimation != null) {
                        settingsAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
                    }
                    currentState = MenuState.SETTINGS;
                    selectedOption = 0;
                } else if (currentState == MenuState.SETTINGS) {
                    // 설정에서 메인으로 돌아갈 때 슬라이드 아웃 애니메이션
                    if (settingsAnimation != null) {
                        settingsAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
                    }
                    currentState = MenuState.MAIN;
                    selectedOption = 4; // 설정 인덱스 조정
                } else if (currentState == MenuState.ACCOUNT) {
                    // 계정에서 메인으로 돌아갈 때 슬라이드 아웃 애니메이션
                    if (accountAnimation != null) {
                        accountAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
                    }
                    currentState = MenuState.MAIN;
                    selectedOption = 5; // 계정 인덱스 조정
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
                return getSettingsOptions();
            case RESOLUTION:
                return resolutionOptions;
            case ACCOUNT:
                return accountOptions;
            case INVENTORY:
                return new String[]{"뒤로가기"}; // 인벤토리는 뒤로가기만
            case SHOP:
                // 별도 Shop 화면을 갖고 있으므로 여기서는 기본 옵션 반환
                return new String[]{"뒤로가기"};
            case SERVER_CONNECT:
            case LEADERBOARD:
                return new String[]{};
            default:
                return getMainMenuOptions(); // 동적으로 생성된 메인 메뉴 옵션
        }
    }
    
    /**
     * 메뉴 선택을 처리합니다
     */
    private void handleMenuSelection() {
        String[] options = getCurrentMenuOptions();
        if (selectedOption >= options.length) {
            return;
        }
        
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
            case RESOLUTION:
                handleResolutionSelection();
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
                // 상점 진입 애니메이션 시작
                shop.startEntryAnimation();
                break;
            case SERVER_CONNECT:
            case LEADERBOARD:
                // 별도 입력 핸들러에서 처리
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
                // 상점 진입 애니메이션 시작
                shop.startEntryAnimation();
                break;
            case 3: // 인벤토리
                currentState = MenuState.INVENTORY;
                selectedOption = 0;
                // 인벤토리 진입 시 경고창 초기화
                if (shop != null && shop.getShopManager() != null) {
                    shop.getShopManager().clearWarningDialog();
                }
                // 인벤토리 진입 애니메이션 시작 (위에서 아래로)
                if (shop != null) {
                    shop.startInventoryEntryAnimation();
                }
                break;
            case 4: // 설정
                currentState = MenuState.SETTINGS;
                selectedOption = 0;
                // 설정 진입 애니메이션 시작
                if (settingsAnimation != null) {
                    settingsAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
                }
                break;
            case 5: // 계정 (사용자)
                currentState = MenuState.ACCOUNT;
                selectedOption = 0;
                // 계정 진입 애니메이션 시작
                if (accountAnimation != null) {
                    accountAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
                }
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
                playRecordsManager.loadGameRecords();
                currentState = MenuState.LEADERBOARD;
                selectedOption = 0;
                playRecordsManager.resetScrollOffset();
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
                currentState = MenuState.SERVER_CONNECT;
                resetServerConnectInputs();
                break;
            case 1: // 리더보드
                // TODO: 멀티플레이 리더보드 구현
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
            case 0: // 배경음악 ON/OFF
                musicEnabled = !musicEnabled;
                break;
            case 1: // 해상도 변경
                currentState = MenuState.RESOLUTION;
                selectedOption = 0;
                // 해상도 진입 애니메이션 시작
                if (settingsAnimation != null) {
                    settingsAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
                }
                break;
            case 2: // 제작자
                break;
            case 3: // 이전메뉴
                // 설정에서 메인으로 돌아갈 때 슬라이드 아웃 애니메이션
                if (settingsAnimation != null) {
                    settingsAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
                }
                currentState = MenuState.MAIN;
                selectedOption = 4; // 설정 옵션으로 돌아가기
                break;
        }
    }
    
    private void handleResolutionSelection() {
        switch (selectedOption) {
            case 0: // 800x600
            case 1: // 1024x768
            case 2: // 1440x1080
                int[] resolution = resolutionValues[selectedOption];
                if (resolution[0] > 0 && resolution[1] > 0) {
                    navigator.setResolution(resolution[0], resolution[1]);
                }
                break;
            case 3: // 이전메뉴
                // 설정 메뉴의 좌우 패널 애니메이션 시작 (상점과 동일한 방식)
                if (settingsAnimation != null) {
                    settingsAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_IN);
                }
                currentState = MenuState.SETTINGS;
                selectedOption = 1; // 해상도 변경 옵션으로 돌아가기
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
                break;
            case 4: // 이전메뉴
                // 계정에서 메인으로 돌아갈 때 슬라이드 아웃 애니메이션
                if (accountAnimation != null) {
                    accountAnimation.startAnimation(ShopAnimation.AnimationType.SLIDE_OUT);
                }
                currentState = MenuState.MAIN;
                selectedOption = 5; // 계정 옵션으로 돌아가기
                break;
        }
    }
    
    private void handleInventorySelection() {
        switch (selectedOption) {
            case 0: // 뒤로가기
                handleInventoryExit();
                break;
        }
    }
    
    /**
     * 인벤토리 나가기 처리
     */
    private void handleInventoryExit() {
        // 장착 변경사항이 있는지 확인
        if (shop.getShopManager().getEquipmentManager().hasLocalChanges()) {
            // 저장 중이 아닐 때만 저장 시작
            if (!shop.getShopManager().getEquipmentManager().isSaving()) {
                // 변경사항을 DB에 저장
                shop.getShopManager().getEquipmentManager().saveChangesToDB();
                // 인벤토리 변경사항 저장 완료
            }
        }
        
        // 인벤토리 나가기 애니메이션 시작 (아래에서 위로)
        if (shop != null) {
            shop.startInventoryExitAnimation();
        }
        
        // 메인 메뉴로 돌아가기
        currentState = MenuState.MAIN;
        selectedOption = 3; // 인벤토리 옵션으로 돌아가기
    }
    
    /**
     * 메뉴를 그립니다
     */
    public void draw(Graphics2D g2d) {
        // 해상도 스케일링 적용
        if (navigator != null && navigator.getResolutionManager() != null) {
            double scaleX = navigator.getResolutionManager().getScaleX();
            double scaleY = navigator.getResolutionManager().getScaleY();
            g2d.scale(scaleX, scaleY);
        }
        
        // 상점이 표시중이면 상점을 그리기
        if (showingShop) {
            shop.update(); // 메시지 타이머 업데이트
            shop.render(g2d);
            return;
        }
        
        // 인벤토리가 표시중이면 상점의 인벤토리를 그리기
        if (currentState == MenuState.INVENTORY) {
            shop.update(); // 경고창 타이머 업데이트
            shop.setCurrentState(org.newdawn.spaceinvaders.shop.ShopState.INVENTORY);
            shop.render(g2d);
            return;
        }
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, 800, 600);
        
        // 제목 그리기 (LEADERBOARD 상태가 아닐 때만)
        if (currentState != MenuState.LEADERBOARD) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(titleFont);
            FontMetrics titleMetrics = g2d.getFontMetrics();
            String title = "SPACE INVADERS";
            int titleX = (800 - titleMetrics.stringWidth(title)) / 2;
            int titleY = 120;
            g2d.drawString(title, titleX, titleY);
        }
        
        // 상태에 따라 다른 화면 그리기
        if (currentState == MenuState.ACCOUNT) {
            drawAccountMenu(g2d);
        } else if (currentState == MenuState.SETTINGS) {
            drawSettingsMenu(g2d);
        } else if (currentState == MenuState.RESOLUTION) {
            drawResolutionMenu(g2d);
        } else if (currentState == MenuState.LEADERBOARD) {
            playRecordsManager.drawLeaderboard(g2d);
        } else if (currentState == MenuState.SERVER_CONNECT) {
            drawServerConnectScreen(g2d);
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
        
        // 애니메이션 효과에 따른 패널 위치 계산
        float progress = accountAnimation.getProgress();
        int leftPanelX, rightPanelX;
        
        if (accountAnimation.isAnimating() && accountAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_IN) {
            // 양쪽에서 밀려오는 효과
            leftPanelX = (int) (50 - (200 * (1.0f - progress)));  // 왼쪽에서 오른쪽으로
            rightPanelX = (int) (270 + (480 * (1.0f - progress))); // 오른쪽에서 왼쪽으로
        } else if (accountAnimation.isAnimating() && accountAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_OUT) {
            // 양쪽으로 밀려나가는 효과
            leftPanelX = (int) (50 - (200 * progress));  // 왼쪽으로 밀려나감
            rightPanelX = (int) (270 + (480 * progress)); // 오른쪽으로 밀려나감
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
        
        // 제목 "계정" (왼쪽 패널 내부)
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "계정";
        int titleX = leftPanelX + (200 - titleMetrics.stringWidth(title)) / 2;
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
                g2d.drawRect(leftPanelX + 10, startY + (i * lineHeight) - 25, 180, 30);
                
                // 화살표 그리기
                g2d.setColor(Color.YELLOW);
                g2d.drawString("→", leftPanelX + 170, startY + (i * lineHeight));
            }
            
            g2d.setColor(i == selectedOption ? Color.YELLOW : Color.WHITE);
            int x = leftPanelX + 20;
            int y = startY + (i * lineHeight);
            g2d.drawString(options[i], x, y);
        }
        
        // 오른쪽 패널에 선택된 항목의 상세 정보 표시
        drawAccountDetails(g2d, rightPanelX);
    }
    
    private void drawAccountDetails(Graphics2D g2d, int panelX) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        
        int startX = panelX + 20;
        int startY = 100;
        int lineHeight = 50;
        
        // 디버깅 정보 표시
        g2d.setColor(Color.YELLOW);
        g2d.setFont(getKostarFont(12));
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
        
        // 애니메이션 효과에 따른 패널 위치 계산
        float progress = settingsAnimation.getProgress();
        int leftPanelX, rightPanelX;
        
        if (settingsAnimation.isAnimating() && settingsAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_IN) {
            // 양쪽에서 밀려오는 효과
            leftPanelX = (int) (50 - (200 * (1.0f - progress)));  // 왼쪽에서 오른쪽으로
            rightPanelX = (int) (270 + (480 * (1.0f - progress))); // 오른쪽에서 왼쪽으로
        } else if (settingsAnimation.isAnimating() && settingsAnimation.getCurrentType() == ShopAnimation.AnimationType.SLIDE_OUT) {
            // 양쪽으로 밀려나가는 효과
            leftPanelX = (int) (50 - (200 * progress));  // 왼쪽으로 밀려나감
            rightPanelX = (int) (270 + (480 * progress)); // 오른쪽으로 밀려나감
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
        
        // 제목 "설정" (왼쪽 패널 내부)
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "설정";
        int titleX = leftPanelX + (200 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100 );
        
        // 메뉴 옵션들 (왼쪽 패널)
        String[] options = getSettingsOptions();
        g2d.setFont(menuFont);
        
        int startY = 140;
        int lineHeight = 40;
        
        for (int i = 0; i < options.length; i++) {
            // 선택된 항목 강조
            if (i == selectedOption) {
                g2d.setColor(Color.WHITE);
                g2d.drawRect(leftPanelX + 10, startY + (i * lineHeight) - 25, 180, 30);
                
                // 화살표 그리기
                g2d.setColor(Color.YELLOW);
                g2d.drawString("→", leftPanelX + 170, startY + (i * lineHeight));
            }
            
            g2d.setColor(i == selectedOption ? Color.YELLOW : Color.WHITE);
            int x = leftPanelX + 20;
            int y = startY + (i * lineHeight);
            g2d.drawString(options[i], x, y);
        }
        
        // 오른쪽 패널에 선택된 항목의 상세 정보 표시
        drawSettingsDetails(g2d, rightPanelX);
    }
    
    private void drawSettingsDetails(Graphics2D g2d, int panelX) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        
        int startX = panelX + 20;
        int startY = 100;
        int lineHeight = 30;
        
        // 선택된 항목에 따라 다른 정보 표시
        switch (selectedOption) {
            case 0: // 배경음악 ON/OFF
                g2d.drawString("게임 배경음악을 켜거나 끌 수 있습니다.", startX, startY);
                g2d.drawString("현재 상태: " + (musicEnabled ? "ON" : "OFF"), startX, startY + lineHeight);
                g2d.drawString("Enter를 눌러 " + (musicEnabled ? "OFF" : "ON") + "로 변경하세요.", startX, startY + lineHeight * 2);
                break;
            case 1: // 해상도 변경
                g2d.drawString("게임 화면의 해상도를 변경할 수 있습니다.", startX, startY);
                g2d.drawString("다양한 해상도 옵션을 제공합니다.", startX, startY + lineHeight);
                g2d.drawString("Enter를 눌러 해상도 설정으로 이동하세요.", startX, startY + lineHeight * 2);
                break;
            case 2: // 제작자 정보
                g2d.drawString("게임 제작자 정보", startX, startY);
                g2d.drawString("권지민 - 프로그래밍", startX, startY + lineHeight);
                g2d.drawString("김승현 - 디자인", startX, startY + lineHeight * 2);
                g2d.drawString("장진우 - 기획", startX, startY + lineHeight * 3);
                break;
            case 3: // 이전메뉴
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
        g2d.setFont(getKostarFont(14));
        g2d.drawString("↑↓: 이동  Enter/Space: 선택  ESC: 뒤로가기", 280, 550);
    }

    // ===== 서버 연결 상태 필드 =====
    private String serverAddressInput = "";
    private String serverPortInput = "";
    private static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
    private static final String DEFAULT_SERVER_PORT = "7777";
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + ".spaceinvaders_server.properties";
    private boolean serverAddressFocus = true; // true 주소, false 포트 또는 버튼 영역
    private int serverSelectedButton = 0; // 0 연결 1 취소
    private String serverMessage = "";
    private int serverMessageTimer = 0;

    private void resetServerConnectInputs() {
        // 설정 파일에서 불러오기
        Properties props = new Properties();
        boolean loaded = false;
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            loaded = true;
        } catch (Exception e) {
            // 파일 없거나 오류시 무시
        }
        serverAddressInput = loaded ? props.getProperty("server.address", DEFAULT_SERVER_ADDRESS) : DEFAULT_SERVER_ADDRESS;
        serverPortInput = loaded ? props.getProperty("server.port", DEFAULT_SERVER_PORT) : DEFAULT_SERVER_PORT;
        serverAddressFocus = true;
        serverSelectedButton = 0;
        serverMessage = "";
        serverMessageTimer = 0;
    }

    private void handleServerConnectKey(int keyCode) {
        if (serverMessageTimer > 0) {
            serverMessageTimer--;
            if (serverMessageTimer == 0) serverMessage = "";
        }
        switch (keyCode) {
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_DOWN:
                if (serverAddressFocus) {
                    serverAddressFocus = false; // 포트 필드로 이동
                } else {
                    // 버튼 선택 모드 유지 (심플 처리)
                    serverAddressFocus = false;
                }
                break;
            case KeyEvent.VK_UP:
                if (!serverAddressFocus) {
                    serverAddressFocus = true;
                }
                break;
            case KeyEvent.VK_LEFT:
                if (!serverAddressFocus) serverSelectedButton = Math.max(0, serverSelectedButton - 1);
                break;
            case KeyEvent.VK_RIGHT:
                if (!serverAddressFocus) serverSelectedButton = Math.min(1, serverSelectedButton + 1);
                break;
            case KeyEvent.VK_BACK_SPACE:
                if (serverAddressFocus && serverAddressInput.length() > 0) {
                    serverAddressInput = serverAddressInput.substring(0, serverAddressInput.length()-1);
                } else if (!serverAddressFocus && serverPortInput.length() > 0) {
                    serverPortInput = serverPortInput.substring(0, serverPortInput.length()-1);
                }
                break;
            case KeyEvent.VK_ENTER:
                if (serverAddressFocus) {
                    serverAddressFocus = false; // 포트로 이동
                } else {
                    if (serverSelectedButton == 0) {
                        attemptServerConnection();
                    } else {
                        currentState = MenuState.MULTI_PLAYER;
                        selectedOption = 0;
                    }
                }
                break;
            default:
                if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z && serverAddressFocus) {
                    char c = (char)('a' + (keyCode - KeyEvent.VK_A));
                    serverAddressInput += c;
                } else if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
                    char c = (char)('0' + (keyCode - KeyEvent.VK_0));
                    if (serverAddressFocus) serverAddressInput += c; else serverPortInput += c;
                } else if (keyCode == KeyEvent.VK_PERIOD && serverAddressFocus) {
                    serverAddressInput += '.';
                } else if (keyCode == KeyEvent.VK_MINUS && serverAddressFocus) {
                    serverAddressInput += '-';
                }
                break;
        }
    }

    private void attemptServerConnection() {
        if (serverAddressInput.isEmpty()) { showServerMessage("주소 필요"); return; }
        if (serverPortInput.isEmpty()) { showServerMessage("포트 필요"); return; }
        int port;
        try { port = Integer.parseInt(serverPortInput); if (port<1||port>65535) { showServerMessage("포트 범위 오류"); return; } }
        catch(Exception e){ showServerMessage("포트 숫자 오류"); return; }
        showServerMessage("접속 시도: "+serverAddressInput+":"+port);
        // 실제 GameClient 생성 및 연결 시도
        try {
            String username = userManager!=null && userManager.isLoggedIn()? userManager.getCurrentUser().getUsername():"Guest";
            GameClient client = new GameClient(serverAddressInput, port, username);
            client.connect();
            // 접속 성공 시 서버 주소/포트 저장
            saveServerConfig(serverAddressInput, serverPortInput);
            // 방 목록 표시로 전환
            if (navigator != null) {
                navigator.showRoomList(client);
            } else {
                gameStartRequested = false; // 네비게이터 없으면 기존 로직 비활성화
            }
            // 메인메뉴 상태를 MAIN으로 전환 (다시 돌아와도 메인화면)
            currentState = MenuState.MAIN;
            selectedOption = 0;
        } catch (Exception ex) {
            showServerMessage("접속 실패: "+ex.getMessage());
        }
    }

    private void saveServerConfig(String address, String port) {
        Properties props = new Properties();
        props.setProperty("server.address", address);
        props.setProperty("server.port", port);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "SpaceInvaders 서버 접속 정보");
        } catch (Exception e) {
            // 저장 실패시 무시
        }
    }

    private void showServerMessage(String msg) {
        serverMessage = msg;
        serverMessageTimer = 180; // 3초
    }

    private void drawServerConnectScreen(Graphics2D g2d) {
        // 패널 배경
        g2d.setColor(new Color(0,0,0,160));
        g2d.fillRoundRect(140,140,520,320,20,20);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(140,140,520,320,20,20);
        g2d.setFont(titleFont.deriveFont(36f));
        String t = "서버 접속";
        FontMetrics tm = g2d.getFontMetrics();
        g2d.drawString(t, 400 - tm.stringWidth(t)/2, 190);
        g2d.setFont(menuFont);
        int labelX=200, boxX=320, y0=230, gap=60;
        // 주소
        g2d.setColor(Color.WHITE); g2d.drawString("주소:", labelX, y0);
        drawServerInputBox(g2d, boxX, y0-30, 300, 40, serverAddressFocus);
        g2d.setColor(serverAddressInput.isEmpty() && serverAddressFocus ? Color.GRAY:Color.WHITE);
        g2d.drawString(serverAddressInput.isEmpty() && serverAddressFocus?"예) 127.0.0.1":serverAddressInput, boxX+10, y0);
        // 포트
        g2d.setColor(Color.WHITE); g2d.drawString("포트:", labelX, y0+gap);
        drawServerInputBox(g2d, boxX, y0+gap-30, 300, 40, !serverAddressFocus);
        g2d.setColor(serverPortInput.isEmpty() && !serverAddressFocus ? Color.GRAY:Color.WHITE);
        g2d.drawString(serverPortInput.isEmpty() && !serverAddressFocus?"예) 7777":serverPortInput, boxX+10, y0+gap);
        // 버튼
        int btnY = y0 + gap*2 + 10; int bw=140; int bh=40; int space=40; int startX = 400 - (bw*2 + space)/2;
        drawServerButton(g2d, startX, btnY, bw, bh, !serverAddressFocus && serverSelectedButton==0, "연결");
        drawServerButton(g2d, startX + bw + space, btnY, bw, bh, !serverAddressFocus && serverSelectedButton==1, "취소");
        if (!serverMessage.isEmpty()) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(submenuFont);
            FontMetrics mm = g2d.getFontMetrics();
            g2d.drawString(serverMessage, 400 - mm.stringWidth(serverMessage)/2, btnY + 70);
        }
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String help = "Tab/↓: 다음  Enter: 확정/연결  ESC: 뒤로";
        FontMetrics hm = g2d.getFontMetrics();
        g2d.drawString(help, 400 - hm.stringWidth(help)/2, 470);
    }

    private void drawServerInputBox(Graphics2D g2d,int x,int y,int w,int h,boolean focus){
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x,y,w,h);
        g2d.setColor(focus?Color.YELLOW:Color.WHITE);
        g2d.drawRect(x,y,w,h);
    }

    private void drawServerButton(Graphics2D g2d,int x,int y,int w,int h,boolean sel,String text){
        g2d.setColor(new Color(40,40,40,200));
        g2d.fillRect(x,y,w,h);
        g2d.setColor(sel?Color.YELLOW:Color.WHITE);
        g2d.drawRect(x,y,w,h);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, x + (w - fm.stringWidth(text))/2, y + (h + fm.getAscent())/2 - 4);
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
        // 애니메이션 리셋
        if (settingsAnimation != null) {
            settingsAnimation.reset();
        }
        if (accountAnimation != null) {
            accountAnimation.reset();
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
    
    /**
     * 해상도 변경 메뉴를 그립니다
     */
    private void drawResolutionMenu(Graphics2D g2d) {
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
        String title = "해상도 변경";
        int titleX = (800 - titleMetrics.stringWidth(title)) / 2;
        int titleY = 120;
        g2d.drawString(title, titleX, titleY);
        
        // 해상도 옵션들 그리기 (메인메뉴 스타일)
        g2d.setFont(menuFont);
        
        int startY = 200;
        int lineHeight = 50;
        
        for (int i = 0; i < resolutionOptions.length; i++) {
            // 선택된 항목 강조
            if (i == selectedOption) {
                // 선택된 항목 배경 (메인메뉴 스타일)
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(300, startY + (i * lineHeight) - 20, 200, 30);
                
                // 선택된 항목 텍스트 (검은색)
                g2d.setColor(Color.BLACK);
            } else {
                // 선택되지 않은 항목 텍스트 (흰색)
                g2d.setColor(Color.WHITE);
            }
            
            // 텍스트 중앙 정렬
            FontMetrics metrics = g2d.getFontMetrics();
            int textX = (800 - metrics.stringWidth(resolutionOptions[i])) / 2;
            int textY = startY + (i * lineHeight);
            g2d.drawString(resolutionOptions[i], textX, textY);
        }
        
        // 현재 해상도 정보 표시 (하단)
        g2d.setColor(Color.YELLOW);
        g2d.setFont(getKostarFont(Font.BOLD, 14));
        String currentResolution = "현재 해상도: ";
        String scaleInfo = "";
        if (navigator != null) {
            currentResolution += navigator.getCurrentWidth() + " x " + navigator.getCurrentHeight();
            if (navigator.getResolutionManager() != null) {
                double scale = navigator.getResolutionManager().getUniformScale();
                scaleInfo = " (스케일: " + String.format("%.2f", scale) + "x)";
            }
        } else {
            currentResolution += "800 x 600";
            scaleInfo = " (스케일: 1.00x)";
        }
        FontMetrics currentMetrics = g2d.getFontMetrics();
        int currentX = (800 - currentMetrics.stringWidth(currentResolution + scaleInfo)) / 2;
        g2d.drawString(currentResolution + scaleInfo, currentX, 500);
        
        // 조작 안내
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(getKostarFont(12));
        String instructions = "↑↓: 선택  Enter: 확인  ESC: 뒤로가기";
        FontMetrics instructionMetrics = g2d.getFontMetrics();
        int instructionX = (800 - instructionMetrics.stringWidth(instructions)) / 2;
        g2d.drawString(instructions, instructionX, 530);
        
        // 스케일링 설명
        g2d.setColor(Color.CYAN);
        g2d.setFont(getKostarFont(11));
        String scaleDescription = "※ 균등 스케일링: 게임 비율을 유지하면서 크기만 조정합니다";
        FontMetrics descMetrics = g2d.getFontMetrics();
        int descX = (800 - descMetrics.stringWidth(scaleDescription)) / 2;
        g2d.drawString(scaleDescription, descX, 550);
    }
    
    /**
     * 리더보드 키 입력 처리
     */
    private void handleLeaderboardInput(int keyCode) {
        if (playRecordsManager.handleLeaderboardInput(keyCode)) {
            // 뒤로가기 요청
            currentState = MenuState.SINGLE_PLAYER;
            selectedOption = 1; // 플레이기록 옵션으로 돌아가기
        }
    }
        
}
