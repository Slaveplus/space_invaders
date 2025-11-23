package org.newdawn.spaceinvaders.mainmenu;

import org.newdawn.spaceinvaders.database.GameRecord;
import org.newdawn.spaceinvaders.login.UserManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * 플레이 기록을 관리하는 클래스
 * 리더보드 표시, 데이터 로드, 스크롤 등의 기능을 담당
 */
public class PlayRecordsManager {
    private List<GameRecord> gameRecords;
    private int leaderboardScrollOffset = 0;
    private UserManager userManager;
    
    public PlayRecordsManager(UserManager userManager) {
        this.userManager = userManager;
        this.gameRecords = new ArrayList<>();
    }
    
    /**
     * 게임 기록 로드
     */
    public void loadGameRecords() {
        gameRecords = new ArrayList<>();
        if (userManager == null || !userManager.isLoggedIn()) {
            return;
        }

        try {
            loadRecordsForMode("single", GameRecord.GameMode.SINGLE);
            loadRecordsForMode("multi", GameRecord.GameMode.MULTI);

            gameRecords.sort((r1, r2) -> {
                java.time.LocalDateTime d1 = r1.getPlayDate();
                java.time.LocalDateTime d2 = r2.getPlayDate();
                if (d1 != null && d2 != null) {
                    int cmp = d2.compareTo(d1);
                    if (cmp != 0) {
                        return cmp;
                    }
                } else if (d1 != null) {
                    return -1;
                } else if (d2 != null) {
                    return 1;
                }
                return Long.compare(r2.getPlayTimeMs(), r1.getPlayTimeMs());
            });

            System.out.println("📊 Loaded " + gameRecords.size() + " total game records (single + multi)");
        } catch (Exception e) {
            System.err.println("Error loading game records: " + e.getMessage());
            e.printStackTrace();
            gameRecords = new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRecordsForMode(String modePath, GameRecord.GameMode fallbackMode) throws Exception {
        if (userManager == null || userManager.getCurrentUser() == null) {
            return;
        }
        String basePath = "users/" + userManager.getCurrentUser().getUid() + "/gameRecords/" + modePath;
        java.util.Map<String, Object> recordsData =
                (java.util.Map<String, Object>) userManager.getFirebaseDB().getData(basePath, java.util.Map.class);
        if (recordsData == null || recordsData.isEmpty()) {
            System.out.println("📊 No " + modePath + " records found");
            return;
        }

        for (Object recordObj : recordsData.values()) {
            if (!(recordObj instanceof java.util.Map)) {
                continue;
            }
            java.util.Map<String, Object> recordMap = (java.util.Map<String, Object>) recordObj;
            GameRecord record = parseRecord(recordMap, fallbackMode);
            gameRecords.add(record);
        }
    }

    private GameRecord parseRecord(java.util.Map<String, Object> recordMap, GameRecord.GameMode fallbackMode) {
        GameRecord record = new GameRecord();
        record.setMode(fallbackMode);

        parseBasicFields(record, recordMap);
        parseNumericFields(record, recordMap);
        parseCoPlayers(record, recordMap);

        return record;
    }
    
    private void parseBasicFields(GameRecord record, java.util.Map<String, Object> recordMap) {
        Object modeObj = recordMap.get("mode");
        if (modeObj instanceof String) {
            record.setMode((String) modeObj);
        }

        record.setRecordId((String) recordMap.get("recordId"));
        record.setUserId((String) recordMap.get("userId"));
        record.setUsername((String) recordMap.get("username"));
    }
    
    private void parseNumericFields(GameRecord record, java.util.Map<String, Object> recordMap) {
        Object playTimeMsObj = recordMap.get("playTimeMs");
        if (playTimeMsObj instanceof Number) {
            record.setPlayTimeMs(((Number) playTimeMsObj).longValue());
        }
        Object playTimeObj = recordMap.get("playTime");
        if (playTimeObj instanceof String) {
            record.setPlayTime((String) playTimeObj);
        }
        Object coinsObj = recordMap.get("earnedCoins");
        if (coinsObj instanceof Number) {
            record.setEarnedCoins(((Number) coinsObj).intValue());
        }
        Object completedObj = recordMap.get("completed");
        if (completedObj instanceof Boolean) {
            record.setCompleted((Boolean) completedObj);
        }
        Object finalRoundObj = recordMap.get("finalRound");
        if (finalRoundObj instanceof Number) {
            record.setFinalRound(((Number) finalRoundObj).intValue());
        }
        Object playDateObj = recordMap.get("playDate");
        if (playDateObj instanceof String) {
            record.setPlayDateString((String) playDateObj);
        }
    }
    
    private void parseCoPlayers(GameRecord record, java.util.Map<String, Object> recordMap) {
        Object coPlayersObj = recordMap.get("coPlayers");
        if (coPlayersObj instanceof java.util.Collection) {
            record.setCoPlayers(parseCoPlayersFromCollection((java.util.Collection<?>) coPlayersObj));
        } else if (coPlayersObj instanceof String) {
            record.setCoPlayers(parseCoPlayersFromString((String) coPlayersObj));
        }
    }
    
    private java.util.List<String> parseCoPlayersFromCollection(java.util.Collection<?> collection) {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (Object item : collection) {
            if (item != null) {
                String name = item.toString().trim();
                if (!name.isEmpty()) {
                    list.add(name);
                }
            }
        }
        return list;
    }
    
    private java.util.List<String> parseCoPlayersFromString(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        String[] parts = trimmed.split(",");
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String part : parts) {
            String partTrimmed = part.trim();
            if (!partTrimmed.isEmpty()) {
                list.add(partTrimmed);
            }
        }
        return list;
    }
    
    /**
     * 리더보드 화면 그리기
     */
    public void drawLeaderboard(Graphics2D g2d) {
        drawLeaderboardTitle(g2d);
        
        int tableX = 100;
        int tableY = 120;
        int tableWidth = 600;
        int tableHeight = 350;
        
        drawLeaderboardTable(g2d, tableX, tableY, tableWidth, tableHeight);
        drawLeaderboardRecords(g2d, tableX, tableY, tableWidth, tableHeight);
        drawBackButton(g2d);
    }
    
    private void drawLeaderboardTitle(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(getKostarFont(Font.BOLD, 32));
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "플레이 기록";
        int titleX = (800 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 80);
    }
    
    private void drawLeaderboardTable(Graphics2D g2d, int tableX, int tableY, int tableWidth, int tableHeight) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(tableX, tableY, tableWidth, tableHeight);
        
        g2d.setColor(Color.CYAN);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(tableX, tableY, tableWidth, tableHeight);
        
        drawLeaderboardHeader(g2d, tableX, tableY);
    }
    
    private void drawLeaderboardHeader(Graphics2D g2d, int tableX, int tableY) {
        g2d.setColor(Color.YELLOW);
        g2d.setFont(getKostarFont(Font.BOLD, 16));
        int headerY = tableY + 25;
        int rankCol = tableX + 20;
        int modeCol = tableX + 70;
        int nameCol = tableX + 120;
        int timeCol = tableX + 260;
        int statusCol = tableX + 330;
        int coinCol = tableX + 380;
        int dateCol = tableX + 450;
        
        g2d.drawString("순위", rankCol, headerY);
        g2d.drawString("모드", modeCol, headerY);
        g2d.drawString("사용자명", nameCol, headerY);
        g2d.drawString("시간", timeCol, headerY);
        g2d.drawString("상태", statusCol, headerY);
        g2d.drawString("코인", coinCol, headerY);
        g2d.drawString("날짜", dateCol, headerY);
        
        g2d.setColor(Color.CYAN);
        g2d.drawLine(tableX, headerY + 10, tableX + 600, headerY + 10);
    }
    
    private void drawLeaderboardRecords(Graphics2D g2d, int tableX, int tableY, int tableWidth, int tableHeight) {
        if (gameRecords == null || gameRecords.isEmpty()) {
            drawNoRecordsMessage(g2d, tableX, tableY, tableWidth, tableHeight);
            return;
        }
        
        int startIndex = leaderboardScrollOffset;
        int maxRecords = 10;
        int endIndex = Math.min(startIndex + maxRecords, gameRecords.size());
        int rowSpacing = 32;
        
        int rankCol = tableX + 20;
        int modeCol = tableX + 70;
        int nameCol = tableX + 120;
        int timeCol = tableX + 260;
        int statusCol = tableX + 330;
        int coinCol = tableX + 380;
        int dateCol = tableX + 450;
        
        for (int i = startIndex; i < endIndex; i++) {
            GameRecord record = gameRecords.get(i);
            int baseRowY = tableY + 50 + (i - startIndex) * rowSpacing;
            
            drawRecordRow(g2d, record, i, baseRowY, rankCol, modeCol, nameCol, timeCol, statusCol, coinCol, dateCol, tableX, tableWidth);
        }
        
        if (gameRecords.size() > maxRecords) {
            drawScrollInfo(g2d, tableX, tableY, tableWidth, tableHeight);
        }
    }
    
    private void drawRecordRow(Graphics2D g2d, GameRecord record, int index, int baseRowY, 
                               int rankCol, int modeCol, int nameCol, int timeCol, int statusCol, int coinCol, int dateCol,
                               int tableX, int tableWidth) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(getKostarFont(14));
        
        g2d.drawString(String.valueOf(index + 1), rankCol, baseRowY);
        
        String modeLabel = record.getMode() == GameRecord.GameMode.MULTI ? "멀티" : "싱글";
        g2d.drawString(modeLabel, modeCol, baseRowY);
        
        String username = record.getUsername() != null ? record.getUsername() : "-";
        if (username.length() > 12) {
            username = username.substring(0, 12) + "...";
        }
        g2d.drawString(username, nameCol, baseRowY);
        
        g2d.drawString(record.getPlayTime(), timeCol, baseRowY);
        
        String status = record.isCompleted() ? "클리어" : "실패";
        g2d.setColor(record.isCompleted() ? Color.GREEN : Color.RED);
        g2d.drawString(status, statusCol, baseRowY);
        g2d.setColor(Color.WHITE);
        
        g2d.drawString(String.valueOf(record.getEarnedCoins()), coinCol, baseRowY);
        
        String date = record.getPlayDateString();
        if (date != null && date.length() >= 16) {
            date = date.substring(5, 16);
        }
        g2d.drawString(date != null ? date : "-", dateCol, baseRowY);
        
        if (record.getMode() == GameRecord.GameMode.MULTI && record.hasCoPlayers()) {
            drawCoPlayersInfo(g2d, record, nameCol, baseRowY, tableX, tableWidth);
        }
    }
    
    private void drawCoPlayersInfo(Graphics2D g2d, GameRecord record, int nameCol, int baseRowY, int tableX, int tableWidth) {
        g2d.setFont(getKostarFont(12));
        g2d.setColor(new Color(180, 220, 255));
        String teammates = "동료: " + record.getCoPlayersLabel();
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int maxWidth = tableX + tableWidth - nameCol - 20;
        
        if (fontMetrics.stringWidth(teammates) > maxWidth) {
            teammates = truncateTeammatesText(g2d, teammates, maxWidth);
        }
        
        g2d.drawString(teammates, nameCol, baseRowY + 14);
        g2d.setFont(getKostarFont(14));
        g2d.setColor(Color.WHITE);
    }
    
    private String truncateTeammatesText(Graphics2D g2d, String teammates, int maxWidth) {
        FontMetrics fontMetrics = g2d.getFontMetrics();
        while (teammates.length() > 3 && fontMetrics.stringWidth(teammates + "...") > maxWidth) {
            teammates = teammates.substring(0, teammates.length() - 1);
            fontMetrics = g2d.getFontMetrics();
        }
        return teammates + "...";
    }
    
    private void drawNoRecordsMessage(Graphics2D g2d, int tableX, int tableY, int tableWidth, int tableHeight) {
        g2d.setColor(Color.GRAY);
        g2d.setFont(getKostarFont(16));
        String noRecords = "플레이 기록이 없습니다.";
        FontMetrics noRecordsMetrics = g2d.getFontMetrics();
        int noRecordsX = tableX + (tableWidth - noRecordsMetrics.stringWidth(noRecords)) / 2;
        g2d.drawString(noRecords, noRecordsX, tableY + tableHeight / 2);
    }
    
    private void drawScrollInfo(Graphics2D g2d, int tableX, int tableY, int tableWidth, int tableHeight) {
        g2d.setColor(Color.CYAN);
        g2d.setFont(getKostarFont(12));
        String scrollInfo = "↑↓: 스크롤";
        g2d.drawString(scrollInfo, tableX + tableWidth - 80, tableY + tableHeight - 20);
    }
    
    private void drawBackButton(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        g2d.setFont(getKostarFont(Font.BOLD, 16));
        String backButton = "ESC: 뒤로가기";
        g2d.drawString(backButton, 50, 550);
    }
    
    /**
     * 리더보드 키 입력 처리
     */
    public boolean handleLeaderboardInput(int keyCode) {
        if (keyCode == KeyEvent.VK_ESCAPE) {
            return true; // 뒤로가기 요청
        } else if (keyCode == KeyEvent.VK_UP && gameRecords != null && gameRecords.size() > 10) {
            if (leaderboardScrollOffset > 0) {
                leaderboardScrollOffset--;
            }
        } else if (keyCode == KeyEvent.VK_DOWN && gameRecords != null && gameRecords.size() > 10) {
            if (leaderboardScrollOffset < gameRecords.size() - 10) {
                leaderboardScrollOffset++;
            }
        }
        return false; // 계속 리더보드 표시
    }
    
    /**
     * 스크롤 오프셋 초기화
     */
    public void resetScrollOffset() {
        leaderboardScrollOffset = 0;
    }
    
    /**
     * 게임 기록 목록 반환
     */
    public List<GameRecord> getGameRecords() {
        return gameRecords;
    }
    
    /**
     * 게임 기록 개수 반환
     */
    public int getRecordCount() {
        return gameRecords != null ? gameRecords.size() : 0;
    }
    
    /**
     * Kostar 폰트를 지정된 크기와 스타일로 반환
     */
    private Font getKostarFont(int style, int size) {
        return MainMenu.getKostarFont(style, size);
    }
    
    /**
     * Kostar 폰트를 지정된 크기로 반환
     */
    private Font getKostarFont(int size) {
        return MainMenu.getKostarFont(size);
    }
}
