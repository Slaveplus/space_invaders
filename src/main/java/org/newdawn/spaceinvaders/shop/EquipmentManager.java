package org.newdawn.spaceinvaders.shop;

import java.util.*;
import org.newdawn.spaceinvaders.login.UserManager;

/**
 * 장착 아이템 관리 클래스
 * 플레이어가 구매한 아이템을 장착하고 관리하는 시스템
 */
public class EquipmentManager {
    private Map<ShopCategory, ShopItem> equippedItems;
    private List<ShopItem> playerInventory;
    private UserManager userManager;
    
    public EquipmentManager(UserManager userManager) {
        this.userManager = userManager;
        this.equippedItems = new HashMap<>();
        this.playerInventory = new ArrayList<>();
        initializeEquippedItems();
    }
    
    /**
     * 장착 가능한 카테고리별로 기본값 초기화
     */
    private void initializeEquippedItems() {
        // 장착 가능한 카테고리들만 초기화
        equippedItems.put(ShopCategory.WEAPONS, null);
        equippedItems.put(ShopCategory.SPACESHIPS, null);
        equippedItems.put(ShopCategory.POWERUPS, null);
    }
    
    /**
     * 인벤토리 아이템 목록 설정
     */
    public void setPlayerInventory(List<ShopItem> inventory) {
        this.playerInventory = new ArrayList<>(inventory);
    }
    
    /**
     * 카테고리별 인벤토리 아이템 반환
     */
    public List<ShopItem> getInventoryByCategory(ShopCategory category) {
        return playerInventory.stream()
                .filter(item -> item.getCategory() == category)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 아이템 장착
     */
    public boolean equipItem(ShopItem item) {
        if (item == null || !item.isPurchased()) {
            return false;
        }
        
        ShopCategory category = item.getCategory();
        
        // 장착 가능한 카테고리인지 확인
        if (!equippedItems.containsKey(category)) {
            return false;
        }
        
        // 기존에 장착된 아이템이 있으면 해제
        ShopItem currentEquipped = equippedItems.get(category);
        if (currentEquipped != null) {
            unequipItem(category);
        }
        
        // 새 아이템 장착
        equippedItems.put(category, item);
        
        // DB에 장착 정보 저장
        saveEquippedItemsToDB();
        
        System.out.println("아이템 장착: " + item.getName() + " (" + category.getDisplayName() + ")");
        return true;
    }
    
    /**
     * 아이템 해제
     */
    public boolean unequipItem(ShopCategory category) {
        ShopItem equippedItem = equippedItems.get(category);
        if (equippedItem != null) {
            equippedItems.put(category, null);
            saveEquippedItemsToDB();
            System.out.println("아이템 해제: " + equippedItem.getName() + " (" + category.getDisplayName() + ")");
            return true;
        }
        return false;
    }
    
    /**
     * 현재 장착된 아이템 반환
     */
    public ShopItem getEquippedItem(ShopCategory category) {
        return equippedItems.get(category);
    }
    
    /**
     * 모든 장착된 아이템 반환
     */
    public Map<ShopCategory, ShopItem> getAllEquippedItems() {
        return new HashMap<>(equippedItems);
    }
    
    /**
     * 아이템이 장착되어 있는지 확인
     */
    public boolean isItemEquipped(ShopItem item) {
        return equippedItems.containsValue(item);
    }
    
    /**
     * 장착 가능한 카테고리 목록 반환
     */
    public List<ShopCategory> getEquippableCategories() {
        return new ArrayList<>(equippedItems.keySet());
    }
    
    /**
     * DB에 장착 정보 저장
     */
    private void saveEquippedItemsToDB() {
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                String uid = userManager.getCurrentUser().getUid();
                
                // 먼저 모든 아이템의 isEquipped를 false로 설정
                updateAllItemsEquippedStatus(false);
                
                // 현재 장착된 아이템들의 isEquipped를 true로 설정
                for (Map.Entry<ShopCategory, ShopItem> entry : equippedItems.entrySet()) {
                    ShopItem item = entry.getValue();
                    if (item != null) {
                        updateItemEquippedStatus(item.getId(), true);
                        System.out.println("DB 업데이트: " + item.getName() + " 장착 상태 = true");
                    }
                }
                
                System.out.println("장착 아이템 DB 저장 완료 (인벤토리 테이블 업데이트)");
            } catch (Exception e) {
                System.err.println("장착 아이템 DB 저장 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 모든 아이템의 isEquipped 상태를 업데이트
     */
    private void updateAllItemsEquippedStatus(boolean equipped) {
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                String uid = userManager.getCurrentUser().getUid();
                
                // 인벤토리 데이터 가져오기
                Object inventoryData = userManager.getFirebaseDB().getData(
                    "users/" + uid + "/inventory", 
                    Object.class
                );
                
                if (inventoryData instanceof java.util.Map) {
                    java.util.Map<String, Object> inventoryMap = (java.util.Map<String, Object>) inventoryData;
                    Object itemsObj = inventoryMap.get("items");
                    
                    if (itemsObj instanceof java.util.Map) {
                        java.util.Map<String, Object> items = (java.util.Map<String, Object>) itemsObj;
                        
                        // 모든 아이템의 isEquipped 상태 업데이트
                        for (String itemId : items.keySet()) {
                            updateItemEquippedStatus(itemId, equipped);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("모든 아이템 장착 상태 업데이트 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 특정 아이템의 isEquipped 상태를 업데이트
     */
    private void updateItemEquippedStatus(String itemId, boolean equipped) {
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                String uid = userManager.getCurrentUser().getUid();
                String path = "users/" + uid + "/inventory/items/" + itemId + "/isEquipped";
                
                boolean success = userManager.getFirebaseDB().updateData(path, equipped);
                if (!success) {
                    System.err.println("아이템 장착 상태 업데이트 실패: " + itemId);
                }
            } catch (Exception e) {
                System.err.println("아이템 장착 상태 업데이트 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * DB에서 장착 정보 로드
     */
    public void loadEquippedItemsFromDB(List<ShopItem> allShopItems) {
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                String uid = userManager.getCurrentUser().getUid();
                
                // 인벤토리 데이터에서 isEquipped가 true인 아이템들 찾기
                Object inventoryData = userManager.getFirebaseDB().getData(
                    "users/" + uid + "/inventory", 
                    Object.class
                );
                
                if (inventoryData instanceof java.util.Map) {
                    java.util.Map<String, Object> inventoryMap = (java.util.Map<String, Object>) inventoryData;
                    Object itemsObj = inventoryMap.get("items");
                    
                    if (itemsObj instanceof java.util.Map) {
                        java.util.Map<String, Object> items = (java.util.Map<String, Object>) itemsObj;
                        
                        // 각 아이템의 isEquipped 상태 확인
                        for (Map.Entry<String, Object> itemEntry : items.entrySet()) {
                            String itemId = itemEntry.getKey();
                            Object itemData = itemEntry.getValue();
                            
                            if (itemData instanceof java.util.Map) {
                                java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) itemData;
                                Object isEquippedObj = itemMap.get("isEquipped");
                                
                                if (isEquippedObj instanceof Boolean && (Boolean) isEquippedObj) {
                                    // isEquipped가 true인 아이템 찾기
                                    ShopItem item = allShopItems.stream()
                                            .filter(shopItem -> shopItem.getId().equals(itemId))
                                            .findFirst()
                                            .orElse(null);
                                    
                                    if (item != null) {
                                        ShopCategory category = item.getCategory();
                                        equippedItems.put(category, item);
                                        System.out.println("장착 정보 로드: " + item.getName() + " (" + category.getDisplayName() + ")");
                                    }
                                }
                            }
                        }
                        
                        System.out.println("장착 정보 DB 로드 완료 (인벤토리 테이블에서)");
                    }
                } else {
                    System.out.println("인벤토리 데이터가 없습니다.");
                }
            } catch (Exception e) {
                System.err.println("장착 정보 DB 로드 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 장착된 아이템의 효과를 게임에 적용
     */
    public void applyEquippedEffects() {
        // 이 메서드는 게임플레이에서 호출되어 장착된 아이템의 효과를 적용
        // 예: 무기 장착 시 공격력 증가, 우주선 장착 시 이동속도 증가 등
        
        for (Map.Entry<ShopCategory, ShopItem> entry : equippedItems.entrySet()) {
            ShopItem item = entry.getValue();
            if (item != null) {
                // 아이템 효과 적용 로직
                for (ItemEffect effect : item.getEffects()) {
                    // 효과 타입에 따른 게임플레이 적용
                    System.out.println("효과 적용: " + effect.getType() + " - " + effect.getValue());
                }
            }
        }
    }
}
