package org.newdawn.spaceinvaders.login;

import java.util.Objects;

/**
 * 코인/젬/레벨/점수 등 사용자 경제 관련 상태를 관리하고 DB 반영을 위임한다.
 */
public class UserEconomyService {
    private final UserDataRepository repository;

    public UserEconomyService(UserDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public void updateCoins(User user, int newCoins) {
        if (!isMutable(user)) {
            return;
        }
        user.setCoins(newCoins);
        repository.updateProfileField(user.getUid(), "coins", newCoins);
    }

    public void updateGems(User user, int newGems) {
        if (!isMutable(user)) {
            return;
        }
        user.setGems(newGems);
        repository.updateProfileField(user.getUid(), "gems", newGems);
    }

    public void updateLevel(User user, int newLevel) {
        if (!isMutable(user)) {
            return;
        }
        user.setLevel(newLevel);
        repository.updateProfileField(user.getUid(), "level", newLevel);
    }

    public void updateHighScore(User user, int newScore) {
        if (!isMutable(user)) {
            return;
        }
        user.setHighScore(newScore);
        repository.updateProfileField(user.getUid(), "highScore", newScore);
    }

    public void addCoins(User user, int amount) {
        if (!isMutable(user)) {
            return;
        }
        updateCoins(user, user.getCoins() + amount);
    }

    public boolean spendCoins(User user, int amount) {
        if (!isMutable(user) || amount < 0) {
            return false;
        }
        if (user.getCoins() < amount) {
            return false;
        }
        updateCoins(user, user.getCoins() - amount);
        return true;
    }

    public void addGems(User user, int amount) {
        if (!isMutable(user)) {
            return;
        }
        updateGems(user, user.getGems() + amount);
    }

    public boolean spendGems(User user, int amount) {
        if (!isMutable(user) || amount < 0) {
            return false;
        }
        if (user.getGems() < amount) {
            return false;
        }
        updateGems(user, user.getGems() - amount);
        return true;
    }

    private boolean isMutable(User user) {
        return user != null && user.getUid() != null && !user.getUid().isEmpty();
    }
}

