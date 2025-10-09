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
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                // Firebase DB에서 플레이 기록 로드
                String dbPath = "users/" + userManager.getCurrentUser().getUid() + "/gameRecords/single";
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> recordsData = (java.util.Map<String, Object>) userManager.getFirebaseDB().getData(dbPath, java.util.Map.class);
                
                gameRecords = new ArrayList<>();
                
                if (recordsData != null) {
                    for (Object recordObj : recordsData.values()) {
                        if (recordObj instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> recordMap = (java.util.Map<String, Object>) recordObj;
                            
                            GameRecord record = new GameRecord();
                            record.setRecordId((String) recordMap.get("recordId"));
                            record.setUserId((String) recordMap.get("userId"));
                            record.setUsername((String) recordMap.get("username"));
                            record.setPlayTimeMs(((Number) recordMap.get("playTimeMs")).longValue());
                            record.setPlayTime((String) recordMap.get("playTime"));
                            record.setEarnedCoins(((Number) recordMap.get("earnedCoins")).intValue());
                            record.setCompleted((Boolean) recordMap.get("completed"));
                            record.setFinalRound(((Number) recordMap.get("finalRound")).intValue());
                            
                            gameRecords.add(record);
                        }
                    }
                    
                    // 플레이 시간 기준으로 정렬 (오름차순)
                    gameRecords.sort((r1, r2) -> Long.compare(r1.getPlayTimeMs(), r2.getPlayTimeMs()));
                    
                    System.out.println("📊 Loaded " + gameRecords.size() + " game records");
                } else {
                    System.out.println("📊 No game records found");
                }
            } catch (Exception e) {
                System.err.println("Error loading game records: " + e.getMessage());
                e.printStackTrace();
                gameRecords = new ArrayList<>();
            }
        } else {
            gameRecords = new ArrayList<>();
        }
    }
    
    /**
     * 리더보드 화면 그리기
     */
    public void drawLeaderboard(Graphics2D g2d) {
        // 제목
        g2d.setColor(Color.WHITE);
        g2d.setFont(getKostarFont(Font.BOLD, 32));
        FontMetrics titleMetrics = g2d.getFontMetrics();
        String title = "플레이 기록";
        int titleX = (800 - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 80);
        
        // 리더보드 테이블
        int tableX = 100;
        int tableY = 120;
        int tableWidth = 600;
        int tableHeight = 350;
        
        // 테이블 배경
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(tableX, tableY, tableWidth, tableHeight);
        
        // 테이블 테두리
        g2d.setColor(Color.CYAN);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(tableX, tableY, tableWidth, tableHeight);
        
        // 헤더
        g2d.setColor(Color.YELLOW);
        g2d.setFont(getKostarFont(Font.BOLD, 16));
        int headerY = tableY + 25;
        g2d.drawString("순위", tableX + 20, headerY);
        g2d.drawString("사용자명", tableX + 80, headerY);
        g2d.drawString("시간", tableX + 200, headerY);
        g2d.drawString("상태", tableX + 280, headerY);
        g2d.drawString("코인", tableX + 350, headerY);
        g2d.drawString("날짜", tableX + 420, headerY);
        
        // 구분선
        g2d.setColor(Color.CYAN);
        g2d.drawLine(tableX, headerY + 10, tableX + tableWidth, headerY + 10);
        
        // 기록 목록
        if (gameRecords != null && !gameRecords.isEmpty()) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(getKostarFont(14));
            
            int startIndex = leaderboardScrollOffset;
            int maxRecords = 10; // 화면에 표시할 최대 기록 수
            int endIndex = Math.min(startIndex + maxRecords, gameRecords.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                GameRecord record = gameRecords.get(i);
                int rowY = tableY + 50 + (i - startIndex) * 25;
                
                // 순위
                g2d.drawString(String.valueOf(i + 1), tableX + 20, rowY);
                
                // 사용자명
                String username = record.getUsername();
                if (username.length() > 10) {
                    username = username.substring(0, 10) + "...";
                }
                g2d.drawString(username, tableX + 80, rowY);
                
                // 시간
                g2d.drawString(record.getPlayTime(), tableX + 200, rowY);
                
                // 상태
                String status = record.isCompleted() ? "클리어" : "실패";
                g2d.setColor(record.isCompleted() ? Color.GREEN : Color.RED);
                g2d.drawString(status, tableX + 280, rowY);
                g2d.setColor(Color.WHITE);
                
                // 코인
                g2d.drawString(String.valueOf(record.getEarnedCoins()), tableX + 350, rowY);
                
                // 날짜 (간단한 형식)
                String date = record.getPlayDateString().substring(5, 10); // MM-DD
                g2d.drawString(date, tableX + 420, rowY);
            }
            
            // 스크롤 안내
            if (gameRecords.size() > maxRecords) {
                g2d.setColor(Color.CYAN);
                g2d.setFont(getKostarFont(12));
                String scrollInfo = "↑↓: 스크롤";
                g2d.drawString(scrollInfo, tableX + tableWidth - 80, tableY + tableHeight - 20);
            }
        } else {
            // 기록이 없을 때
            g2d.setColor(Color.GRAY);
            g2d.setFont(getKostarFont(16));
            String noRecords = "플레이 기록이 없습니다.";
            FontMetrics noRecordsMetrics = g2d.getFontMetrics();
            int noRecordsX = tableX + (tableWidth - noRecordsMetrics.stringWidth(noRecords)) / 2;
            g2d.drawString(noRecords, noRecordsX, tableY + tableHeight / 2);
        }
        
        // 뒤로가기 버튼
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
