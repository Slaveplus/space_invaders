package org.newdawn.spaceinvaders.mainmenu;

import org.newdawn.spaceinvaders.database.FirebaseConfig;
import org.newdawn.spaceinvaders.database.FirebaseDatabaseClient;
import org.newdawn.spaceinvaders.database.LeaderboardRecord;
import org.newdawn.spaceinvaders.database.LeaderboardRepository;
import org.newdawn.spaceinvaders.login.UserManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 글로벌 리더보드 화면을 렌더링하는 매니저.
 * 좌측에는 싱글플레이, 우측에는 멀티플레이 상위 기록을 보여준다.
 */
public class GlobalLeaderboardManager {
    private static final int MAX_ENTRIES = 10;

    private final UserManager userManager;
    private final FirebaseDatabaseClient fallbackDb =
            new FirebaseDatabaseClient(FirebaseConfig.DATABASE_URL);

    private List<LeaderboardRecord> singleRecords = new ArrayList<>();
    private List<LeaderboardRecord> multiRecords = new ArrayList<>();

    public GlobalLeaderboardManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void loadLeaderboards() {
        FirebaseDatabaseClient db = resolveDatabaseClient();
        singleRecords = LeaderboardRepository.loadRecords(
                db, LeaderboardRecord.Mode.SINGLE, MAX_ENTRIES);
        multiRecords = LeaderboardRepository.loadRecords(
                db, LeaderboardRecord.Mode.MULTI, MAX_ENTRIES);
    }

    private FirebaseDatabaseClient resolveDatabaseClient() {
        if (userManager != null && userManager.getFirebaseDB() != null) {
            return userManager.getFirebaseDB();
        }
        return fallbackDb;
    }

    public void draw(Graphics2D g2d) {
        drawTitle(g2d);
        drawPanels(g2d);
        drawFooter(g2d);
    }

    private void drawTitle(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(MainMenu.getKostarFont(Font.BOLD, 32));
        String title = "글로벌 리더보드";
        FontMetrics metrics = g2d.getFontMetrics();
        int x = (800 - metrics.stringWidth(title)) / 2;
        g2d.drawString(title, x, 80);
    }

    private void drawPanels(Graphics2D g2d) {
        int panelWidth = 320;
        int panelHeight = 400;
        int leftPanelX = 70;
        int rightPanelX = 800 - panelWidth - 70;
        int panelY = 120;

        drawPanel(g2d, leftPanelX, panelY, panelWidth, panelHeight,
                "싱글 플레이", singleRecords);
        drawPanel(g2d, rightPanelX, panelY, panelWidth, panelHeight,
                "멀티 플레이", multiRecords);
    }

    private void drawPanel(Graphics2D g2d,
                           int x, int y,
                           int width, int height,
                           String title,
                           List<LeaderboardRecord> records) {
        // 배경
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(x, y, width, height);

        // 테두리
        g2d.setColor(Color.CYAN);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, width, height);

        // 제목
        g2d.setColor(Color.YELLOW);
        g2d.setFont(MainMenu.getKostarFont(Font.BOLD, 18));
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleX = x + (width - titleMetrics.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, y + 30);

        // 헤더
        g2d.setColor(new Color(180, 220, 255));
        g2d.setFont(MainMenu.getKostarFont(Font.BOLD, 14));
        int headerY = y + 60;
        g2d.drawString("순위", x + 10, headerY);
        g2d.drawString("플레이어", x + 60, headerY);
        g2d.drawString("시간", x + width - 90, headerY);
        g2d.setColor(Color.CYAN);
        g2d.drawLine(x + 5, headerY + 5, x + width - 5, headerY + 5);

        // 데이터
        g2d.setFont(MainMenu.getKostarFont(13));
        g2d.setColor(Color.WHITE);
        int rowY = headerY + 25;
        int lineSpacing = 30;

        if (records == null || records.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("기록이 없습니다.", x + (width - 100) / 2, y + height / 2);
            return;
        }

        int rank = 1;
        for (LeaderboardRecord record : records) {
            if (rank > MAX_ENTRIES) {
                break;
            }
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.valueOf(rank), x + 15, rowY);

            String names = record.getPlayerNamesLabel();
            FontMetrics fm = g2d.getFontMetrics();
            int maxNameWidth = width - 140;
            if (fm.stringWidth(names) > maxNameWidth) {
                while (names.length() > 3 && fm.stringWidth(names + "...") > maxNameWidth) {
                    names = names.substring(0, names.length() - 1);
                    fm = g2d.getFontMetrics();
                }
                names += "...";
            }
            g2d.drawString(names, x + 60, rowY);

            g2d.setColor(new Color(255, 215, 0));
            String time = record.getPlayTimeFormatted();
            g2d.drawString(time, x + width - 90, rowY);

            g2d.setColor(new Color(150, 150, 150));
            g2d.setFont(MainMenu.getKostarFont(11));
            String created = record.getCreatedAtLabel();
            g2d.drawString(created, x + 60, rowY + 14);

            g2d.setFont(MainMenu.getKostarFont(13));
            g2d.setColor(Color.WHITE);
            rowY += lineSpacing;
            rank++;
        }
    }

    private void drawFooter(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(MainMenu.getKostarFont(12));
        String instructions = "ESC: 뒤로가기";
        FontMetrics metrics = g2d.getFontMetrics();
        int x = (800 - metrics.stringWidth(instructions)) / 2;
        g2d.drawString(instructions, x, 540);

        g2d.setColor(new Color(120, 200, 255));
        g2d.setFont(MainMenu.getKostarFont(11));
        String sub = "플레이 시간은 빠를수록 상위에 랭크됩니다.";
        FontMetrics subMetrics = g2d.getFontMetrics();
        int subX = (800 - subMetrics.stringWidth(sub)) / 2;
        g2d.drawString(sub, subX, 560);
    }
}
