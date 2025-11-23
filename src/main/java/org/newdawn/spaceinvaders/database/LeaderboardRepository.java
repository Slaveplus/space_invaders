package org.newdawn.spaceinvaders.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 글로벌 리더보드 저장/조회 유틸리티.
 */
public final class LeaderboardRepository {
    private LeaderboardRepository() {}

    private static String pathForMode(LeaderboardRecord.Mode mode) {
        String suffix = mode == LeaderboardRecord.Mode.MULTI ? "multi" : "single";
        return "leaderboard/" + suffix;
    }

    public static boolean saveRecord(FirebaseDatabaseClient db,
                                     LeaderboardRecord.Mode mode,
                                     LeaderboardRecord record) {
        if (db == null || record == null) {
            return false;
        }
        String path = pathForMode(mode) + "/" + record.getRecordId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recordId", record.getRecordId());
        payload.put("mode", mode != null ? mode.name() : LeaderboardRecord.Mode.SINGLE.name());
        payload.put("playerNames", new ArrayList<>(record.getPlayerNames()));
        payload.put("playTimeMs", record.getPlayTimeMs());
        payload.put("playTime", record.getPlayTimeFormatted());
        payload.put("createdAt", record.getCreatedAt());
        return db.putData(path, payload);
    }

    @SuppressWarnings("unchecked")
    public static List<LeaderboardRecord> loadRecords(FirebaseDatabaseClient db,
                                                      LeaderboardRecord.Mode mode,
                                                      int limit) {
        if (db == null) {
            return Collections.emptyList();
        }
        String path = pathForMode(mode);
        Map<String, Object> raw = db.getData(path, Map.class);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<LeaderboardRecord> records = parseRecordsFromRawData(raw, mode);
        return sortAndLimitRecords(records, limit);
    }
    
    /**
     * 원시 데이터에서 레코드 리스트 파싱
     */
    @SuppressWarnings("unchecked")
    private static List<LeaderboardRecord> parseRecordsFromRawData(Map<String, Object> raw, 
                                                                     LeaderboardRecord.Mode mode) {
        List<LeaderboardRecord> records = new ArrayList<>();
        for (Object value : raw.values()) {
            if (!(value instanceof Map)) {
                continue;
            }
            Map<String, Object> entry = (Map<String, Object>) value;
            LeaderboardRecord record = parseRecordFromEntry(entry, mode);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }
    
    /**
     * 엔트리에서 LeaderboardRecord 생성
     */
    private static LeaderboardRecord parseRecordFromEntry(Map<String, Object> entry, 
                                                          LeaderboardRecord.Mode defaultMode) {
        String recordId = (String) entry.get("recordId");
        long playTimeMs = parseLong(entry.get("playTimeMs"));
        long createdAt = parseLong(entry.get("createdAt"), System.currentTimeMillis());
        
        List<String> names = parsePlayerNames(entry.get("playerNames"));
        LeaderboardRecord.Mode resolvedMode = resolveMode(entry.get("mode"), defaultMode);
        
        return new LeaderboardRecord(
                resolvedMode,
                names,
                playTimeMs,
                createdAt,
                recordId);
    }
    
    /**
     * 플레이어 이름 리스트 파싱
     */
    private static List<String> parsePlayerNames(Object namesObj) {
        if (namesObj instanceof List) {
            return parsePlayerNamesFromList((List<?>) namesObj);
        } else if (namesObj instanceof String) {
            return parsePlayerNamesFromString((String) namesObj);
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * List 타입에서 플레이어 이름 파싱
     */
    private static List<String> parsePlayerNamesFromList(List<?> namesList) {
        List<String> names = new ArrayList<>();
        for (Object item : namesList) {
            if (item != null) {
                String name = item.toString().trim();
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }
    
    /**
     * String 타입에서 플레이어 이름 파싱 (쉼표로 구분)
     */
    private static List<String> parsePlayerNamesFromString(String namesStr) {
        List<String> names = new ArrayList<>();
        String[] parts = namesStr.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !names.contains(trimmed)) {
                names.add(trimmed);
            }
        }
        return names;
    }
    
    /**
     * 모드 값 해석
     */
    private static LeaderboardRecord.Mode resolveMode(Object modeValue, 
                                                       LeaderboardRecord.Mode defaultMode) {
        if (modeValue instanceof String) {
            LeaderboardRecord.Mode resolved = LeaderboardRecord.Mode.fromString((String) modeValue);
            return resolved != null ? resolved : defaultMode;
        }
        return defaultMode;
    }
    
    /**
     * 레코드 정렬 및 limit 적용
     */
    private static List<LeaderboardRecord> sortAndLimitRecords(List<LeaderboardRecord> records, 
                                                                int limit) {
        records.sort(Comparator
                .comparingLong(LeaderboardRecord::getPlayTimeMs)
                .thenComparingLong(LeaderboardRecord::getCreatedAt));
        
        if (limit > 0 && records.size() > limit) {
            return new ArrayList<>(records.subList(0, limit));
        }
        return records;
    }

    private static long parseLong(Object value) {
        return parseLong(value, 0L);
    }

    private static long parseLong(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) { }
        }
        return defaultValue;
    }
}
