package org.newdawn.spaceinvaders.shop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.newdawn.spaceinvaders.login.User;

/**
 * 상점 내 통화 상태를 관리하는 지갑.
 * Map 조작을 ShopManager에서 분리해 중복 로직을 줄인다.
 */
public class ShopCurrencyWallet {
    private final Map<String, Integer> balances = new HashMap<>();

    public ShopCurrencyWallet() {
        resetToDefault();
    }

    public void resetToDefault() {
        balances.clear();
        balances.put("COINS", 1000);
        balances.put("GEMS", 50);
        balances.put("POINTS", 0);
    }

    public void syncFromUser(User user) {
        if (user == null) {
            return;
        }
        balances.put("COINS", Math.max(0, user.getCoins()));
        balances.put("GEMS", Math.max(0, user.getGems()));
    }

    public boolean spend(String type, int amount) {
        int current = balances.getOrDefault(type, 0);
        if (current < amount) {
            return false;
        }
        balances.put(type, current - amount);
        return true;
    }

    public void add(String type, int amount) {
        balances.put(type, balances.getOrDefault(type, 0) + amount);
    }

    public int get(String type) {
        return balances.getOrDefault(type, 0);
    }

    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(balances));
    }
}

