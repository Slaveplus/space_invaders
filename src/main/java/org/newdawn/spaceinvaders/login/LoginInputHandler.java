package org.newdawn.spaceinvaders.login;

import java.awt.event.KeyEvent;

/**
 * 로그인 입력 처리 클래스
 * 확장 가능한 입력 시스템
 */
public class LoginInputHandler {
    private LoginScreen loginScreen;
    
    public LoginInputHandler(LoginScreen loginScreen) {
        this.loginScreen = loginScreen;
    }
    
    public void handleInput(int keyCode, char keyChar) {
        switch (keyCode) {
            case KeyEvent.VK_TAB:
                // 입력 필드 간 이동 (사용자명 ↔ 비밀번호)
                // 현재 입력 내용을 저장
                if (loginScreen.isUsernameInput()) {
                    // 사용자명 입력 중이면 사용자명에 저장
                    loginScreen.setUsername(loginScreen.getCurrentInput());
                } else {
                    // 비밀번호 입력 중이면 비밀번호에 저장
                    loginScreen.setPassword(loginScreen.getCurrentInput());
                }
                
                // 입력 필드 전환
                loginScreen.setUsernameInput(!loginScreen.isUsernameInput());
                
                // 새로운 필드의 기존 내용을 currentInput에 설정
                if (loginScreen.isUsernameInput()) {
                    loginScreen.setCurrentInput(loginScreen.getUsername());
                } else {
                    loginScreen.setCurrentInput(loginScreen.getPassword());
                }
                
                System.out.println("입력 필드 이동: " + (loginScreen.isUsernameInput() ? "사용자명" : "비밀번호"));
                break;
                
            case KeyEvent.VK_LEFT:
                // 버튼 선택 (왼쪽)
                if (loginScreen.getSelectedButton() > 0) {
                    loginScreen.setSelectedButton(loginScreen.getSelectedButton() - 1);
                    System.out.println("버튼 선택: " + (loginScreen.getSelectedButton() == 0 ? "로그인" : "회원가입"));
                }
                break;
                
            case KeyEvent.VK_RIGHT:
                // 버튼 선택 (오른쪽)
                if (loginScreen.getSelectedButton() < 1) {
                    loginScreen.setSelectedButton(loginScreen.getSelectedButton() + 1);
                    System.out.println("버튼 선택: " + (loginScreen.getSelectedButton() == 0 ? "로그인" : "회원가입"));
                }
                break;
                
            case KeyEvent.VK_ENTER:
                handleSubmit();
                break;
                
            case KeyEvent.VK_BACK_SPACE:
                handleBackspace();
                break;
                
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
                
            default:
                handleTextInput(keyChar);
                break;
        }
    }
    
    private void handleSubmit() {
        if (loginScreen.isUsernameInput()) {
            // 사용자명 입력 완료
            if (loginScreen.getCurrentInput().trim().isEmpty()) {
                loginScreen.setMessage("사용자명을 입력해주세요.");
                return;
            }
            
            loginScreen.setUsername(loginScreen.getCurrentInput().trim());
            loginScreen.setCurrentInput("");
            loginScreen.setUsernameInput(false);
            System.out.println("사용자명 입력 완료: " + loginScreen.getUsername());
            
        } else {
            // 비밀번호 입력 완료
            if (loginScreen.getCurrentInput().trim().isEmpty()) {
                loginScreen.setMessage("비밀번호를 입력해주세요.");
                return;
            }
            
            loginScreen.setPassword(loginScreen.getCurrentInput().trim());
            System.out.println("비밀번호 입력 완료");
            
            // 선택된 버튼에 따라 로그인 또는 회원가입 처리
            if (loginScreen.getSelectedButton() == 0) {
                handleLogin();
            } else {
                handleRegistration();
            }
        }
    }
    
    private void handleLogin() {
        String email = loginScreen.getUsername(); // 이제 username은 email로 사용
        String password = loginScreen.getPassword();
        
        // 이메일 형식 검증
        if (!isValidEmail(email)) {
            loginScreen.setMessage("올바른 이메일 형식을 입력해주세요.");
            return;
        }
        
        if (loginScreen.getUserManager().loginUser(email, password)) {
            loginScreen.setMessage("로그인 성공! 환영합니다, " + email + "님!");
            System.out.println("로그인 성공: " + email);
        } else {
            loginScreen.setMessage("로그인 실패. 이메일 또는 비밀번호를 확인해주세요.");
            System.out.println("로그인 실패: " + email);
        }
        
        // 입력 필드 리셋
        loginScreen.setCurrentInput("");
        loginScreen.setUsernameInput(true);
    }
    
    private void handleRegistration() {
        String email = loginScreen.getUsername(); // 이제 username은 email로 사용
        String password = loginScreen.getPassword();
        
        // 이메일 형식 검증
        if (!isValidEmail(email)) {
            loginScreen.setMessage("올바른 이메일 형식을 입력해주세요.");
            return;
        }
        
        if (password.length() < 6) {
            loginScreen.setMessage("비밀번호는 6자 이상이어야 합니다.");
            return;
        }
        
        // Firebase 회원가입 시도
        if (loginScreen.getUserManager().registerUser(email, password)) {
            loginScreen.setMessage("회원가입 성공! " + email + "님, 환영합니다!");
            System.out.println("회원가입 성공: " + email);
        } else {
            loginScreen.setMessage("회원가입 실패. 다시 시도해주세요.");
            System.out.println("회원가입 실패: " + email);
        }
        
        // 입력 필드 리셋
        loginScreen.setCurrentInput("");
        loginScreen.setUsernameInput(true);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
    
    private void handleBackspace() {
        String current = loginScreen.getCurrentInput();
        if (!current.isEmpty()) {
            loginScreen.setCurrentInput(current.substring(0, current.length() - 1));
        }
    }
    
    private void handleTextInput(char keyChar) {
        // 제어 문자는 제외하고 모든 문자 허용 (ASCII 32-126 범위)
        if (keyChar >= 32 && keyChar <= 126) {
            String current = loginScreen.getCurrentInput();
            loginScreen.setCurrentInput(current + keyChar);
        }
    }
    
    public void handleMouseClick(int x, int y) {
        // 입력 필드 클릭 처리
        if (isPointInUsernameField(x, y)) {
            // 현재 입력 내용을 저장
            if (!loginScreen.isUsernameInput()) {
                loginScreen.setPassword(loginScreen.getCurrentInput());
            }
            
            loginScreen.setUsernameInput(true);
            loginScreen.setCurrentInput(loginScreen.getUsername());
            System.out.println("마우스 클릭: 사용자명 입력 필드");
        } else if (isPointInPasswordField(x, y)) {
            // 현재 입력 내용을 저장
            if (loginScreen.isUsernameInput()) {
                loginScreen.setUsername(loginScreen.getCurrentInput());
            }
            
            loginScreen.setUsernameInput(false);
            loginScreen.setCurrentInput(loginScreen.getPassword());
            System.out.println("마우스 클릭: 비밀번호 입력 필드");
        } else if (isPointInLoginButton(x, y)) {
            loginScreen.setSelectedButton(0);
            handleSubmit();
            System.out.println("마우스 클릭: 로그인 버튼");
        } else if (isPointInRegisterButton(x, y)) {
            loginScreen.setSelectedButton(1);
            handleSubmit();
            System.out.println("마우스 클릭: 회원가입 버튼");
        }
    }
    
    private boolean isPointInUsernameField(int x, int y) {
        return x >= 300 && x <= 600 && y >= 175 && y <= 205;
    }
    
    private boolean isPointInPasswordField(int x, int y) {
        return x >= 300 && x <= 600 && y >= 225 && y <= 255;
    }
    
    private boolean isPointInLoginButton(int x, int y) {
        int startY = 350;
        int buttonWidth = 150;
        int buttonHeight = 40;
        int buttonSpacing = 20;
        int totalWidth = buttonWidth * 2 + buttonSpacing;
        int startX = (800 - totalWidth) / 2;
        
        return x >= startX && x <= startX + buttonWidth && y >= startY && y <= startY + buttonHeight;
    }
    
    private boolean isPointInRegisterButton(int x, int y) {
        int startY = 350;
        int buttonWidth = 150;
        int buttonHeight = 40;
        int buttonSpacing = 20;
        int totalWidth = buttonWidth * 2 + buttonSpacing;
        int startX = (800 - totalWidth) / 2;
        
        return x >= startX + buttonWidth + buttonSpacing && 
               x <= startX + buttonWidth + buttonSpacing + buttonWidth && 
               y >= startY && y <= startY + buttonHeight;
    }
}
