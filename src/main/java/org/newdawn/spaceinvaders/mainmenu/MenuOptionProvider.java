package org.newdawn.spaceinvaders.mainmenu;

import java.util.Arrays;
import org.newdawn.spaceinvaders.login.User;
import org.newdawn.spaceinvaders.login.UserManager;

/**
 * 메뉴 옵션 생성을 전담하는 유틸리티.
 * MainMenu 내부의 상수/배열 중복을 제거해 응집도를 높인다.
 */
public class MenuOptionProvider {
    private static final String[] GAMEPLAY_OPTIONS = {
        "싱글 플레이",
        "멀티 플레이",
        "리더보드",
        "플레이기록",
        "이전메뉴"
    };

    private static final String[] RESOLUTION_OPTIONS = {
        "800x600",
        "1024x768",
        "1440x1080",
        "이전메뉴"
    };

    private static final int[][] RESOLUTION_VALUES = {
        {800, 600},
        {1024, 768},
        {1440, 1080},
        {0, 0}
    };

    private static final String[] ACCOUNT_OPTIONS = {
        "계정 정보",
        "게임 통계",
        "정보수정",
        "로그아웃",
        "이전메뉴"
    };

    public String[] gameplayOptions() {
        return GAMEPLAY_OPTIONS.clone();
    }

    public String[] settingsOptions(boolean musicEnabled) {
        return new String[]{
            "배경음악 " + (musicEnabled ? "ON" : "OFF"),
            "해상도 변경",
            "제작자",
            "이전메뉴"
        };
    }

    public String[] accountOptions() {
        return ACCOUNT_OPTIONS.clone();
    }

    public String[] resolutionOptions() {
        return RESOLUTION_OPTIONS.clone();
    }

    public int[][] resolutionValues() {
        int[][] copy = new int[RESOLUTION_VALUES.length][];
        for (int i = 0; i < RESOLUTION_VALUES.length; i++) {
            copy[i] = Arrays.copyOf(RESOLUTION_VALUES[i], RESOLUTION_VALUES[i].length);
        }
        return copy;
    }

    public String[] mainMenuOptions(UserManager userManager) {
        String welcomeMessage = "게스트";
        if (userManager != null && userManager.isLoggedIn()) {
            User current = userManager.getCurrentUser();
            if (current != null) {
                welcomeMessage = "환영합니다! " + current.getUsername();
            }
        }
        return new String[]{
            "게임플레이",
            "상점",
            "인벤토리",
            "설정",
            welcomeMessage,
            "게임 종료"
        };
    }
}

