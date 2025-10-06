package org.newdawn.spaceinvaders.app;

import org.newdawn.spaceinvaders.gameplay.ResolutionManager;
import org.newdawn.spaceinvaders.room.GameClient;

/**
 * 화면 전환을 위한 네비게이터. 구현체는 SpaceInvadersApp이 담당합니다.
 */
public interface ScreenNavigator {
    void showLogin();
    void showMainMenu();
    void startNewGame();
    void exitGame();
    void setResolution(int width, int height);
    int getCurrentWidth();
    int getCurrentHeight();
    
    // 해상도 관리자 추가
    ResolutionManager getResolutionManager();

    // ===== 멀티플레이 네비게이션 추가 =====
    /** 서버에 연결된 GameClient로 방 목록 화면을 연다 */
    void showRoomList(GameClient client);

    /** 특정 roomId 로비 화면을 연다 (이미 GameClient가 JOIN 완료된 상태여야 함) */
    void showRoomLobby(String roomId);

    // ===== 신규: 멀티플레이 게임 시작 (multyplay 패키지) =====
    /** 새 멀티플레이 전용 캔버스를 띄운다 (싱글 Game 과 분리). */
    void startMultiplayerGame();
}
