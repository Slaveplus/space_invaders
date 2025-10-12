package org.newdawn.spaceinvaders.database;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 글로벌 리더보드에 저장되는 기록 정보.
 * 플레이한 사용자 목록과 클리어 시간을 저장한다.
 */
public class LeaderboardRecord {
    public enum Mode {
        SINGLE,
        MULTI;

        public static Mode fromString(String value) {
            if (value == null) {
                return SINGLE;
            }
            try {
                return Mode.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return SINGLE;
            }
        }
    }

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final String recordId;
    private final Mode mode;
    private final List<String> playerNames = new ArrayList<>();
    private long playTimeMs;
    private String playTimeFormatted;
    private final long createdAt;

    public LeaderboardRecord(Mode mode, List<String> names, long playTimeMs) {
        this(mode, names, playTimeMs, System.currentTimeMillis(), generateRecordId(mode));
    }

    public LeaderboardRecord(Mode mode, List<String> names, long playTimeMs, long createdAt, String recordId) {
        this.mode = mode != null ? mode : Mode.SINGLE;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.recordId = recordId != null ? recordId : generateRecordId(this.mode);
        setPlayerNames(names);
        setPlayTimeMs(playTimeMs);
    }

    private static String generateRecordId(Mode mode) {
        return (mode != null ? mode.name().toLowerCase() : "single") + "_" +
               System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getRecordId() {
        return recordId;
    }

    public Mode getMode() {
        return mode;
    }

    public List<String> getPlayerNames() {
        return Collections.unmodifiableList(playerNames);
    }

    public void setPlayerNames(List<String> names) {
        playerNames.clear();
        if (names == null) {
            return;
        }
        for (Object entry : names) {
            if (entry == null) {
                continue;
            }
            String name = entry.toString().trim();
            if (!name.isEmpty() && !playerNames.contains(name)) {
                playerNames.add(name);
            }
        }
    }

    public long getPlayTimeMs() {
        return playTimeMs;
    }

    public void setPlayTimeMs(long playTimeMs) {
        if (playTimeMs < 0) {
            playTimeMs = 0;
        }
        this.playTimeMs = playTimeMs;
        this.playTimeFormatted = formatPlayTime(playTimeMs);
    }

    public String getPlayTimeFormatted() {
        return playTimeFormatted;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtLabel() {
        return DISPLAY_TIME.format(Instant.ofEpochMilli(createdAt));
    }

    public String getPlayerNamesLabel() {
        if (playerNames.isEmpty()) {
            return "-";
        }
        return String.join(", ", playerNames);
    }

    private static String formatPlayTime(long playTimeMs) {
        long totalSeconds = playTimeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = playTimeMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    @Override
    public String toString() {
        return "LeaderboardRecord{" +
                "recordId='" + recordId + '\'' +
                ", mode=" + mode +
                ", playerNames=" + playerNames +
                ", playTimeMs=" + playTimeMs +
                ", playTimeFormatted='" + playTimeFormatted + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
