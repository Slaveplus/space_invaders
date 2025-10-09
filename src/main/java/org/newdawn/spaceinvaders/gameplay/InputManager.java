package org.newdawn.spaceinvaders.gameplay;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 입력 관리 클래스
 * 키보드와 마우스 입력을 통합 관리합니다
 */
public class InputManager {
    private GameStateManager gameStateManager;
    private Game game;
    private SkillManager skillManager;
    
    // 게임플레이 관련 입력 상태
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean firePressed = false;
    
    public InputManager(GameStateManager gameStateManager, Game game) {
        this.gameStateManager = gameStateManager;
        this.game = game;
        this.skillManager = game.getSkillManager();
    }
    
    /**
     * 키보드 입력 핸들러
     */
    public class KeyInputHandler extends KeyAdapter {
        
        /**
         * 키가 눌렸을 때 처리
         */
        public void keyPressed(KeyEvent e) { handleGameplayKeyPressed(e); }
        
        /**
         * 키가 떼어졌을 때 처리
         */
        public void keyReleased(KeyEvent e) { handleGameplayKeyReleased(e); }
        
        /**
         * 키가 타이핑되었을 때 처리
         */
        public void keyTyped(KeyEvent e) {
            // 라운드 설명 창이 열려있을 때 (자동 닫기 기능으로 인해 키 입력 처리 제거)
            if (gameStateManager.isShowingRoundInfo()) {
                return;
            }
            
            // 게임플레이 중 "any key" 대기 상태일 때
            if (gameStateManager.isWaitingForKeyPress()) {
                // 스킬 메뉴가 열려있고 waitingForKeyPress가 true인 경우 (스킬 포인트 부족 메시지)
                if (gameStateManager.isShowingSkillMenu()) {
                    gameStateManager.setWaitingForKeyPress(false);
                    // 스킬 메뉴는 그대로 유지
                    return;
                }
                
                // 일시정지 메뉴가 열려있으면 무시
                if (gameStateManager.isShowingPauseMenu()) {
                    return;
                }
                
                // 게임 클리어 상태일 때는 메인메뉴로 이동
                if (gameStateManager.isGameCompleted()) {
                    gameStateManager.setWaitingForKeyPress(false);
                    gameStateManager.setGameCompleted(false);
                    // 메인메뉴로 이동
                    game.returnToMainMenu();
                    return;
                }
                
                // 키를 한 번만 눌러도 게임 시작
                gameStateManager.setWaitingForKeyPress(false);
                
                // 라운드 전환 중이면 게임을 초기화하지 않고 플래그만 해제
                if (gameStateManager.isRoundTransition()) {
                    gameStateManager.setRoundTransition(false);
                    // 라운드 전환 완료
                } else {
                    // 새로운 게임 시작 (게임 오버 후 재시작 등)
                    game.startGame();
                }
            }
        }
        
        /**
         * 게임플레이 중 키 눌림 처리
         */
        private void handleGameplayKeyPressed(KeyEvent e) {
            // "any key" 대기 중이면 키 입력 무시 (스킬 메뉴에서 메시지 대기 중이 아닌 경우)
            if (gameStateManager.isWaitingForKeyPress() && !gameStateManager.isShowingSkillMenu()) {
                return;
            }
            
            // Handle ESC key for pause menu (only during gameplay)
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (gameStateManager.isShowingPauseMenu()) {
                    gameStateManager.hidePauseMenu();
                } else if (!gameStateManager.isShowingSkillMenu()) {
                    // Only open pause menu if skill menu is not open
                    gameStateManager.showPauseMenu();
                }
                return;
            }
            
            // Handle Q key for skill menu
            if (e.getKeyCode() == KeyEvent.VK_Q) {
                if (gameStateManager.isShowingSkillMenu()) {
                    gameStateManager.hideSkillMenu();
                    gameStateManager.setWaitingForKeyPress(false); // 메시지 대기 상태도 해제
                } else if (!gameStateManager.isShowingPauseMenu()) {
                    // Only open skill menu if pause menu is not open
                    gameStateManager.showSkillMenu();
                }
                return;
            }
            
            // Handle pause menu navigation when pause menu is open
            if (gameStateManager.isShowingPauseMenu()) {
                handlePauseMenuInput(e);
                return;
            }
            
            // Handle skill menu navigation when skill menu is open
            if (gameStateManager.isShowingSkillMenu()) {
                handleSkillMenuInput(e);
                return;
            }
            
            // Handle quit confirmation dialog when it's open
            if (gameStateManager.isShowingQuitConfirm()) {
                handleQuitConfirmInput(e);
                return;
            }
            
            // Handle skill activation keys (only when skill menu is not open)
            if (e.getKeyCode() == KeyEvent.VK_1) {
                skillManager.extendSkill(0, 5); // 5 seconds invincible
                return;
            }
            
            if (e.getKeyCode() == KeyEvent.VK_2) {
                skillManager.extendSkill(1, 10); // 10 seconds piercing
                return;
            }
            
            if (e.getKeyCode() == KeyEvent.VK_3) {
                skillManager.extendSkill(2, 8); // 8 seconds triple shot
                return;
            }
            
            if (e.getKeyCode() == KeyEvent.VK_4) {
                skillManager.activateSkill(3, 1); // Activate missile skill
                return;
            }
            
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                firePressed = true;
            }
        }
        
        /**
         * 게임플레이 중 키 릴리즈 처리
         */
        private void handleGameplayKeyReleased(KeyEvent e) {
            // "any key" 대기 중이면 키 입력 무시 (스킬 메뉴에서 메시지 대기 중이 아닌 경우)
            if (gameStateManager.isWaitingForKeyPress() && !gameStateManager.isShowingSkillMenu()) {
                return;
            }
            
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                firePressed = false;
            }
        }
    }
    
    /**
     * 마우스 입력 핸들러
     */
    public class MouseInputHandler extends MouseAdapter {
        /**
         * 마우스 클릭 이벤트 처리
         */
        public void mouseClicked(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            System.out.println("게임 중 마우스 클릭: (" + x + ", " + y + ")");
        }
    }
    
    /**
     * 게임플레이 입력 상태 업데이트
     */
    public void updateGameplayInput() {
        if (game.getShip() != null && 
            !gameStateManager.isShowingPauseMenu() && !gameStateManager.isShowingSkillMenu()) {
            
            // 우주선 이동 처리 (좌우만)
            game.getShip().setHorizontalMovement(0);
            
            if (leftPressed && !rightPressed) {
                game.getShip().setHorizontalMovement(-game.getMoveSpeed());
            } else if (rightPressed && !leftPressed) {
                game.getShip().setHorizontalMovement(game.getMoveSpeed());
            }
            
            // 발사 처리
            if (firePressed) {
                game.tryToFire();
            }
        }
    }
    
    /**
     * 입력 상태 초기화
     */
    public void reset() {
        leftPressed = false;
        rightPressed = false;
        firePressed = false;
    }
    
    /**
     * 일시정지 메뉴 입력 처리
     */
    private void handlePauseMenuInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            int current = gameStateManager.getSelectedPauseMenuItem();
            gameStateManager.setSelectedPauseMenuItem((current - 1 + 3) % 3);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            int current = gameStateManager.getSelectedPauseMenuItem();
            gameStateManager.setSelectedPauseMenuItem((current + 1) % 3);
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
            handlePauseMenuSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameStateManager.hidePauseMenu();
        }
    }
    
    /**
     * 일시정지 메뉴 선택 처리
     */
    private void handlePauseMenuSelection() {
        switch (gameStateManager.getSelectedPauseMenuItem()) {
            case 0: // 계속하기
                gameStateManager.hidePauseMenu();
                break;
            case 1: // 그만두기
                gameStateManager.hidePauseMenu();
                gameStateManager.showQuitConfirm();
                break;
        }
    }
    
    /**
     * 스킬 메뉴 입력 처리
     */
    private void handleSkillMenuInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            // 스킬 선택을 왼쪽으로 이동 (이전 스킬)
            int currentSkill = gameStateManager.getSelectedSkill();
            int totalSkills = 3; // Attack Power, Attack Speed, HP Up
            gameStateManager.setSelectedSkill((currentSkill - 1 + totalSkills) % totalSkills);
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            // 스킬 선택을 오른쪽으로 이동 (다음 스킬)
            int currentSkill = gameStateManager.getSelectedSkill();
            int totalSkills = 3; // Attack Power, Attack Speed, HP Up
            gameStateManager.setSelectedSkill((currentSkill + 1) % totalSkills);
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
            // 스킬 선택 처리
            handleSkillSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_Q) {
            // 스킬 메뉴 닫기
            gameStateManager.hideSkillMenu();
            gameStateManager.setWaitingForKeyPress(false); // 메시지 대기 상태도 해제
        }
    }
    
    /**
     * 스킬 선택 처리
     */
    private void handleSkillSelection() {
        int selectedSkill = gameStateManager.getSelectedSkill();
        int skillPoints = gameStateManager.getSkillPoints();
        
        String result = "";
        boolean success = false;
        
        switch (selectedSkill) {
            case 0: // Attack Power
                if (skillPoints >= skillManager.getAttackPowerCost()) {
                    gameStateManager.setAttackPower(gameStateManager.getAttackPower() + 1);
                    gameStateManager.setSkillPoints(skillPoints - skillManager.getAttackPowerCost());
                    skillManager.increaseAttackPowerLevel(); // 강화 레벨 증가
                    result = "공격력이 증가했습니다! (현재: " + gameStateManager.getAttackPower() + ", 레벨: " + skillManager.getAttackPowerLevel() + ")";
                    success = true;
                } else {
                    result = "스킬 포인트가 부족합니다! (필요: " + skillManager.getAttackPowerCost() + ", 보유: " + skillPoints + ")";
                }
                break;
                
            case 1: // Attack Speed
                if (skillPoints >= skillManager.getAttackSpeedCost()) {
                    gameStateManager.setAttackSpeed(gameStateManager.getAttackSpeed() + 0.2);
                    gameStateManager.setSkillPoints(skillPoints - skillManager.getAttackSpeedCost());
                    skillManager.increaseAttackSpeedLevel(); // 강화 레벨 증가
                    result = "공격 속도가 증가했습니다! (현재: " + String.format("%.1f", gameStateManager.getAttackSpeed()) + ", 레벨: " + skillManager.getAttackSpeedLevel() + ")";
                    success = true;
                } else {
                    result = "스킬 포인트가 부족합니다! (필요: " + skillManager.getAttackSpeedCost() + ", 보유: " + skillPoints + ")";
                }
                break;
                
            case 2: // HP Up & Heal
                if (skillPoints >= skillManager.getHpUpCost()) {
                    gameStateManager.setMaxHP(gameStateManager.getMaxHP() + 3);
                    gameStateManager.setCurrentHP(gameStateManager.getMaxHP()); // Also heal to full
                    gameStateManager.setSkillPoints(skillPoints - skillManager.getHpUpCost());
                    skillManager.increaseHpUpLevel(); // 강화 레벨 증가
                    result = "최대 체력이 증가하고 체력이 회복되었습니다! (현재: " + gameStateManager.getMaxHP() + ", 레벨: " + skillManager.getHpUpLevel() + ")";
                    success = true;
                } else {
                    result = "스킬 포인트가 부족합니다! (필요: " + skillManager.getHpUpCost() + ", 보유: " + skillPoints + ")";
                }
                break;
        }
        
        // 메시지 설정
        gameStateManager.setMessage(result);
        
        // 성공했든 실패했든 스킬 메뉴는 유지하고 메시지만 표시
        if (success) {
            // 스킬 강화 성공 시 메시지 표시 후 대기
            gameStateManager.setWaitingForKeyPress(true);
            gameStateManager.setRoundTransition(false);
        } else {
            // 스킬 포인트 부족 시 waitingForKeyPress를 설정하여 메시지 표시
            // keyTyped에서 스킬 메뉴 상태일 때 처리하도록 개선됨
            gameStateManager.setWaitingForKeyPress(true);
            gameStateManager.setRoundTransition(false); // 스킬 선택 실패 시 라운드 전환 플래그 해제
        }
    }
    
    /**
     * 그만두기 확인 창 입력 처리
     */
    private void handleQuitConfirmInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            gameStateManager.setSelectedQuitOption(0); // 아니요
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            gameStateManager.setSelectedQuitOption(1); // 예
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
            handleQuitConfirmSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameStateManager.hideQuitConfirm();
        }
    }
    
    /**
     * 그만두기 확인 창 선택 처리
     */
    private void handleQuitConfirmSelection() {
        if (gameStateManager.getSelectedQuitOption() == 0) {
            // 아니요 선택 - 일시정지 메뉴로 돌아가기
            gameStateManager.hideQuitConfirm();
            gameStateManager.showPauseMenu();
        } else {
            // 예 선택 - 메인메뉴로 돌아가기 (코인 저장)
            gameStateManager.hideQuitConfirm();
            game.saveCoinsAndPlayTime(); // 코인 저장
            game.goToMainMenu();
        }
    }
    
    // Getters for movement state
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }
    public boolean isFirePressed() { return firePressed; }
}