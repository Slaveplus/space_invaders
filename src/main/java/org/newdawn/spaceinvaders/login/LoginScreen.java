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
    
    public LoginScreen() {
        this.userManager = new UserManager();
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
                // 폰트 로드 실패 시 기본 폰트 사용
                System.err.println("Kostar 폰트를 로드할 수 없습니다. 기본 폰트를 사용합니다.");
                titleFont = new Font("Arial", Font.BOLD, 36);
                menuFont = new Font("Arial", Font.BOLD, 20);
                inputFont = new Font("Arial", Font.PLAIN, 16);
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 중 오류 발생: " + e.getMessage());
            // 오류 발생 시 기본 폰트 사용
            titleFont = new Font("Arial", Font.BOLD, 36);
            menuFont = new Font("Arial", Font.BOLD, 20);
            inputFont = new Font("Arial", Font.PLAIN, 16);
        }
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
        
        // 사용자명 입력 필드
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        g2d.drawString("Email:", 200, startY);
        
        // 사용자명 입력 박스
        g2d.setColor(Color.GRAY);
        g2d.drawRect(300, startY - 25, 300, 30);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(301, startY - 24, 298, 28);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(inputFont);
        String displayUsername = isUsernameInput ? currentInput : username;
        if (isUsernameInput && currentInput.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("Email을 입력하세요", 305, startY - 5);
        } else {
            g2d.setColor(Color.WHITE);
            g2d.drawString(displayUsername, 305, startY - 5);
        }
        
        // 사용자명 필드에 커서 그리기
        if (isUsernameInput && showCursor) {
            FontMetrics inputMetrics = g2d.getFontMetrics();
            int cursorX = 305 + inputMetrics.stringWidth(displayUsername);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(cursorX, startY - 20, cursorX, startY - 10);
        }
        
        // 비밀번호 입력 필드
        g2d.setColor(Color.WHITE);
        g2d.setFont(menuFont);
        g2d.drawString("비밀번호:", 200, startY + lineHeight);
        
        // 비밀번호 입력 박스
        g2d.setColor(Color.GRAY);
        g2d.drawRect(300, startY + lineHeight - 25, 300, 30);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(301, startY + lineHeight - 24, 298, 28);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(inputFont);
        String displayPassword = !isUsernameInput ? currentInput : password;
        if (!isUsernameInput && currentInput.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("비밀번호를 입력하세요", 305, startY + lineHeight - 5);
        } else {
            g2d.setColor(Color.WHITE);
            // 비밀번호는 *로 표시
            String maskedPassword = "";
            for (int i = 0; i < displayPassword.length(); i++) {
                maskedPassword += "*";
            }
            g2d.drawString(maskedPassword, 305, startY + lineHeight - 5);
        }
        
        // 비밀번호 필드에 커서 그리기
        if (!isUsernameInput && showCursor) {
            FontMetrics inputMetrics = g2d.getFontMetrics();
            String displayPasswordForCursor = !isUsernameInput ? currentInput : password;
            String maskedPasswordForCursor = "";
            for (int i = 0; i < displayPasswordForCursor.length(); i++) {
                maskedPasswordForCursor += "*";
            }
            int cursorX = 305 + inputMetrics.stringWidth(maskedPasswordForCursor);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(cursorX, startY + lineHeight - 20, cursorX, startY + lineHeight - 10);
        }
        
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
    
    public UserManager getUserManager() { return userManager; }
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
