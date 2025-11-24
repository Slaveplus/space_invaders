package org.newdawn.spaceinvaders.login;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * 로그인 화면 클래스
 * 확장 가능한 로그인 시스템
 */
public class LoginScreen {
    private static final String DEFAULT_FONT_NAME = "Arial";
    
    private UserManager userManager;
    private LoginInputHandler inputHandler;
    private BufferedImage backgroundImage;
    private Font titleFont;
    private Font menuFont;
    private Font inputFont;
    private String currentInput = "";
    private String username = "";
    private String password = "";
    private boolean isUsernameInput = true; // true: 사용자명 입력, false: 비밀번호 입력
    private String message = "";
    private int messageTimer = 0;
    private int selectedButton = 0; // 0: 로그인, 1: 회원가입
    private int cursorTimer = 0; // 커서 깜빡임 타이머
    private boolean showCursor = true; // 커서 표시 여부
    private boolean loginSucceeded = false; // 로그인 성공 플래그 (화면 전환 트리거)
    
    public LoginScreen(UserManager userManager) {
        this.userManager = userManager;
        this.inputHandler = new LoginInputHandler(this);
        loadBackgroundImage();
        initializeFonts();
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
    
    private void initializeFonts() {
        try {
            // Kostar 폰트 로드
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Kostar.ttf");
            if (fontStream != null) {
                Font kostarFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                titleFont = kostarFont.deriveFont(Font.BOLD, 36f);
                menuFont = kostarFont.deriveFont(Font.BOLD, 20f);
                inputFont = kostarFont.deriveFont(Font.PLAIN, 16f);
                fontStream.close();
            } else {
                System.err.println("Kostar 폰트를 로드할 수 없습니다. 기본 폰트를 사용합니다.");
                initializeDefaultFonts();
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 중 오류 발생: " + e.getMessage());
            initializeDefaultFonts();
        }
    }
    
    private void initializeDefaultFonts() {
        titleFont = new Font(DEFAULT_FONT_NAME, Font.BOLD, 36);
        menuFont = new Font(DEFAULT_FONT_NAME, Font.BOLD, 20);
        inputFont = new Font(DEFAULT_FONT_NAME, Font.PLAIN, 16);
    }
    
    public void handleKeyInput(int keyCode, char keyChar) {
        inputHandler.handleInput(keyCode, keyChar);
    }
    
    public void handleMouseClick(int x, int y) {
        inputHandler.handleMouseClick(x, y);
    }
    
    public void update() {
        if (messageTimer > 0) {
            messageTimer--;
            if (messageTimer == 0) {
                message = "";
            }
        }
        
        // 커서 깜빡임 처리 (30프레임마다 토글)
        cursorTimer++;
        if (cursorTimer >= 30) {
            showCursor = !showCursor;
            cursorTimer = 0;
        }
    }
    
    public void draw(Graphics2D g2d) {
        // 배경 이미지 그리기
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, 800, 600, null);
        }
        
        // 반투명 오버레이
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, 800, 600);
        
        // 제목
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "Space Invaders";
        int titleX = (800 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100);
        
        // 입력 필드들
        drawInputFields(g2d);
        
        // 버튼들
        drawButtons(g2d);
        
        // 메시지 표시
        if (!message.isEmpty()) {
            drawMessage(g2d);
        }
        
    }
    
    private void drawInputFields(Graphics2D g2d) {
        int startY = 200;
        int lineHeight = 50;
        
        drawUsernameField(g2d, startY);
        drawPasswordField(g2d, startY, lineHeight);
    }
    
    private void drawUsernameField(Graphics2D g2d, int startY) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        g2d.drawString("Email:", 200, startY);
        
        drawInputBox(g2d, 300, startY - 25, 300, 30);
        
        String displayUsername = isUsernameInput ? currentInput : username;
        drawInputText(g2d, displayUsername, 305, startY - 5, isUsernameInput && currentInput.isEmpty(), "Email을 입력하세요");
        
        if (isUsernameInput && showCursor) {
            drawCursor(g2d, displayUsername, 305, startY - 20, startY - 10);
        }
        }
        
    private void drawPasswordField(Graphics2D g2d, int startY, int lineHeight) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        g2d.drawString("비밀번호:", 200, startY + lineHeight);
        
        drawInputBox(g2d, 300, startY + lineHeight - 25, 300, 30);
        
        String displayPassword = !isUsernameInput ? currentInput : password;
        String maskedPassword = maskPassword(displayPassword);
        boolean isEmpty = !isUsernameInput && currentInput.isEmpty();
        drawInputText(g2d, maskedPassword, 305, startY + lineHeight - 5, isEmpty, "비밀번호를 입력하세요");
        
        if (!isUsernameInput && showCursor) {
            drawCursor(g2d, maskedPassword, 305, startY + lineHeight - 20, startY + lineHeight - 10);
        }
    }
    
    private void drawInputBox(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.GRAY);
        g2d.drawRect(x, y, width, height);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x + 1, y + 1, width - 2, height - 2);
    }
        
    private void drawInputText(Graphics2D g2d, String text, int x, int y, boolean isEmpty, String placeholder) {
        g2d.setFont(inputFont);
        if (isEmpty) {
            g2d.setColor(Color.GRAY);
            g2d.drawString(placeholder, x, y);
        } else {
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, x, y);
        }
    }
    
    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            masked.append("*");
            }
        return masked.toString();
        }
        
    private void drawCursor(Graphics2D g2d, String text, int baseX, int topY, int bottomY) {
            FontMetrics inputMetrics = g2d.getFontMetrics();
        int cursorX = baseX + inputMetrics.stringWidth(text);
            g2d.setColor(Color.WHITE);
        g2d.drawLine(cursorX, topY, cursorX, bottomY);
    }
    
    private void drawButtons(Graphics2D g2d) {
        int startY = 350;
        int buttonWidth = 150;
        int buttonHeight = 40;
        int buttonSpacing = 20;
        int totalWidth = buttonWidth * 2 + buttonSpacing;
        int startX = (800 - totalWidth) / 2;
        
        // 로그인 버튼
        Color loginColor = (selectedButton == 0) ? Color.YELLOW : Color.WHITE;
        g2d.setColor(loginColor);
        g2d.setFont(menuFont);
        
        if (selectedButton == 0) {
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.fillRect(startX - 5, startY - 5, buttonWidth + 10, buttonHeight + 10);
        }
        
        g2d.setColor(loginColor);
        g2d.drawRect(startX, startY, buttonWidth, buttonHeight);
        
        FontMetrics loginMetrics = g2d.getFontMetrics();
        int loginTextX = startX + (buttonWidth - loginMetrics.stringWidth("로그인")) / 2;
        int loginTextY = startY + (buttonHeight + loginMetrics.getHeight()) / 2 - 2;
        g2d.drawString("로그인", loginTextX, loginTextY);
        
        // 회원가입 버튼
        Color registerColor = (selectedButton == 1) ? Color.YELLOW : Color.WHITE;
        g2d.setColor(registerColor);
        
        if (selectedButton == 1) {
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.fillRect(startX + buttonWidth + buttonSpacing - 5, startY - 5, buttonWidth + 10, buttonHeight + 10);
        }
        
        g2d.setColor(registerColor);
        g2d.drawRect(startX + buttonWidth + buttonSpacing, startY, buttonWidth, buttonHeight);
        
        FontMetrics registerMetrics = g2d.getFontMetrics();
        int registerTextX = startX + buttonWidth + buttonSpacing + (buttonWidth - registerMetrics.stringWidth("회원가입")) / 2;
        int registerTextY = startY + (buttonHeight + registerMetrics.getHeight()) / 2 - 2;
        g2d.drawString("회원가입", registerTextX, registerTextY);
    }
    
    private void drawMessage(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.setFont(menuFont);
        FontMetrics messageMetrics = g2d.getFontMetrics();
        int messageX = (800 - messageMetrics.stringWidth(message)) / 2;
        g2d.drawString(message, messageX, 500);
    }
    
    
    // Getters and Setters
    public String getCurrentInput() { return currentInput; }
    public void setCurrentInput(String input) { this.currentInput = input; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isUsernameInput() { return isUsernameInput; }
    public void setUsernameInput(boolean usernameInput) { this.isUsernameInput = usernameInput; }
    
    public int getSelectedButton() { return selectedButton; }
    public void setSelectedButton(int selectedButton) { this.selectedButton = selectedButton; }
    
    public void setMessage(String message) { 
        this.message = message; 
        this.messageTimer = 180; // 3초간 표시 (60fps 기준)
    }
    
    /**
     * UserManager에 대한 접근을 제공합니다.
     * 주의: 이 메서드는 내부 표현을 노출할 수 있습니다.
     * 가능한 경우 위임 메서드(loginUser, registerUser 등)를 사용하세요.
     * 
     * @return UserManager 인스턴스 (null이 아님)
     * @deprecated 내부 표현 노출 방지를 위해 위임 메서드 사용을 권장합니다.
     *             하지만 호환성을 위해 유지됩니다.
     */
    @Deprecated
    public UserManager getUserManager() { 
        return userManager; 
    }
    
    /**
     * 로그인을 시도합니다.
     * 
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 로그인 성공 여부
     */
    public boolean loginUser(String email, String password) {
        return userManager != null && userManager.loginUser(email, password);
    }
    
    /**
     * 회원가입을 시도합니다.
     * 
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 회원가입 성공 여부
     */
    public boolean registerUser(String email, String password) {
        return userManager != null && userManager.registerUser(email, password);
    }
    
    public LoginInputHandler getInputHandler() { return inputHandler; }
    
    public void reset() {
        currentInput = "";
        username = "";
        password = "";
        isUsernameInput = true;
        selectedButton = 0;
        message = "";
        messageTimer = 0;
        loginSucceeded = false;
    }

    // 로그인 성공 상태 설정/소비
    public void markLoginSucceeded() { this.loginSucceeded = true; }
    public boolean consumeLoginSuccess() {
        if (loginSucceeded) { loginSucceeded = false; return true; }
        return false;
    }
}
