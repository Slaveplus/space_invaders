package org.newdawn.spaceinvaders.login;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.newdawn.spaceinvaders.database.FirebaseDatabaseClient;
import org.newdawn.spaceinvaders.database.UserProfile;
import org.newdawn.spaceinvaders.database.UserStats;

/**
 * Firebase 상의 사용자 데이터를 저장/조회하는 리포지토리.
 * UserManager에서 직접 REST 경로를 다루지 않도록 캡슐화한다.
 */
public class UserDataRepository {
    private static final Logger LOGGER = Logger.getLogger(UserDataRepository.class.getName());

    private final FirebaseDatabaseClient firebaseDB;

    public UserDataRepository(FirebaseDatabaseClient firebaseDB) {
        this.firebaseDB = Objects.requireNonNull(firebaseDB, "firebaseDB must not be null");
    }

    public boolean saveProfile(User user) {
        if (!isPersistable(user)) {
            return false;
        }
        UserProfile profile = new UserProfile(
            user.getUsername(),
            user.getEmail(),
            user.getUid(),
            user.getLevel(),
            user.getCoins(),
            user.getGems(),
            user.getFormattedLastLogin(),
            user.getFormattedRegistrationDate()
        );
        return firebaseDB.putData(profilePath(user.getUid()), profile);
    }

    public boolean saveStats(User user) {
        if (!isPersistable(user)) {
            return false;
        }
        UserStats stats = new UserStats(
            user.getHighScore(),
            user.getTotalGamesPlayed(),
            user.getTotalWins(),
            0,
            0,
            0
        );
        return firebaseDB.putData(statsPath(user.getUid()), stats);
    }

    public void saveAll(User user) {
        saveProfile(user);
        saveStats(user);
    }

    public boolean loadUserData(User user) {
        if (!isPersistable(user)) {
            return false;
        }
        String uid = user.getUid();
        LOGGER.log(Level.INFO, () -> "사용자 데이터 로드 시작: " + uid);

        UserProfile profile = firebaseDB.getData(profilePath(uid), UserProfile.class);
        if (profile != null) {
            user.setUsername(profile.getUsername());
            user.setLevel(profile.getLevel());
            user.setCoins(profile.getCoins());
            user.setGems(profile.getGems());
        }

        UserStats stats = firebaseDB.getData(statsPath(uid), UserStats.class);
        if (stats != null) {
            user.setHighScore(stats.getHighScore());
            user.setTotalGamesPlayed(stats.getTotalGamesPlayed());
            user.setTotalWins(stats.getTotalWins());
        }

        LOGGER.log(Level.INFO, () -> "사용자 데이터 로드 완료: " + user.getUsername());
        return true;
    }

    public boolean updateProfileField(String uid, String fieldName, Object value) {
        if (!hasText(uid) || !hasText(fieldName)) {
            return false;
        }
        return firebaseDB.updateData(profilePath(uid) + "/" + fieldName, value);
    }

    public boolean updateLastLogin(String uid, String timestamp) {
        return updateProfileField(uid, "lastLogin", timestamp);
    }

    public boolean deleteUser(String uid) {
        if (!hasText(uid)) {
            return false;
        }
        return firebaseDB.deleteData(userRoot(uid));
    }

    private boolean isPersistable(User user) {
        if (user == null) {
            LOGGER.warning("User 참조가 null입니다.");
            return false;
        }
        if (!hasText(user.getUid())) {
            LOGGER.warning("User UID가 비어 있습니다.");
            return false;
        }
        return true;
    }

    private String profilePath(String uid) {
        return userRoot(uid) + "/profile";
    }

    private String statsPath(String uid) {
        return userRoot(uid) + "/stats";
    }

    private String userRoot(String uid) {
        return "users/" + uid;
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}

