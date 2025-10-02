package org.newdawn.spaceinvaders.app;

/**
 * 화면 전환을 위한 네비게이터. 구현체는 SpaceInvadersApp이 담당합니다.
 */
public interface ScreenNavigator {
    void showLogin();
    void showMainMenu();
    void startNewGame();
    // 멀티플레이 관련 전환
    void showMultiplayerConnect();
    void showMultiplayerRoomList();
    void showMultiplayerLobby();
    void startMultiplayerGame();
    void exitGame();
}
