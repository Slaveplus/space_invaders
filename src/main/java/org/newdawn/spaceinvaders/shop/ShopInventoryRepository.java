package org.newdawn.spaceinvaders.shop;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.newdawn.spaceinvaders.login.User;
import org.newdawn.spaceinvaders.login.UserManager;

/**
 * 인벤토리 저장/로딩 책임을 전담하는 리포지토리.
 * ShopManager의 Firebase 접근 코드를 한 곳으로 모아 책임을 분리한다.
 */
public class ShopInventoryRepository {
    private UserManager userManager;

    public ShopInventoryRepository(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void saveInventory(List<ShopItem> playerInventory) {
        if (!isReady()) {
            return;
        }
        try {
            Map<String, Object> inventoryData = new HashMap<>();
            Map<String, Object> items = new HashMap<>();
            Map<String, String> equipped = new HashMap<>();

            for (ShopItem item : playerInventory) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("purchased", true);
                itemData.put("purchasedDate", LocalDateTime.now().toString());
                itemData.put("isEquipped", false);
                itemData.put("level", 1);
                itemData.put("upgradeCount", 0);
                items.put(item.getId(), itemData);
            }

            equipped.put("weapon", null);
            equipped.put("spaceship", null);
            equipped.put("powerup", null);

            inventoryData.put("items", items);
            inventoryData.put("equipped", equipped);

            userManager.getFirebaseDB().putData(userInventoryPath(), inventoryData);
        } catch (Exception e) {
            System.err.println("ShopInventoryRepository: 인벤토리 저장 실패 - " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadInventory(List<ShopItem> allItems, List<ShopItem> playerInventory) {
        if (!isReady()) {
            return;
        }
        try {
            Object inventoryData = userManager.getFirebaseDB().getData(userInventoryPath(), Object.class);
            playerInventory.clear();
            if (inventoryData == null) {
                return;
            }
            if (inventoryData instanceof List) {
                List<String> ids = (List<String>) inventoryData;
                restoreInventoryFromIds(allItems, playerInventory, ids);
            } else if (inventoryData instanceof Map) {
                Object itemsObj = ((Map<?, ?>) inventoryData).get("items");
                if (itemsObj instanceof Map) {
                    Map<String, Object> items = (Map<String, Object>) itemsObj;
                    restoreInventoryFromIds(allItems, playerInventory, new ArrayList<>(items.keySet()));
                }
            }
        } catch (Exception e) {
            System.err.println("ShopInventoryRepository: 인벤토리 로드 실패 - " + e.getMessage());
        }
    }

    private void restoreInventoryFromIds(List<ShopItem> allItems, List<ShopItem> playerInventory, List<String> ids) {
        if (ids == null) {
            return;
        }
        for (String itemId : ids) {
            ShopItem item = findItem(allItems, itemId);
            if (item != null) {
                item.setPurchased(true);
                playerInventory.add(item);
            }
        }
    }

    private ShopItem findItem(List<ShopItem> allItems, String itemId) {
        return allItems.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    private boolean isReady() {
        return userManager != null
            && userManager.isLoggedIn()
            && userManager.getCurrentUser() != null;
    }

    private String userInventoryPath() {
        User user = userManager.getCurrentUser();
        String uid = user != null ? user.getUid() : null;
        return "users/" + uid + "/inventory";
    }
}

