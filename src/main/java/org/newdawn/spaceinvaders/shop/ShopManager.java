package org.newdawn.spaceinvaders.shop;

import java.util.*;
import java.util.stream.Collectors;
import org.newdawn.spaceinvaders.login.UserManager;

/**
 * 상점 관리자 클래스
 * 확장 가능한 상점 로직 관리
 */
public class ShopManager {
    private List<ShopItem> shopItems;
    private List<ShopItem> playerInventory;
    private Map<String, Integer> playerCurrency;
    private ShopState currentState;
    private ShopCategory currentCategory;
    private String searchKeyword;
    private ShopCategory filterCategory;
    private ItemRarity filterRarity;
    private UserManager userManager;
    private String purchaseMessage = "";
    private int messageTimer = 0;
    
    public ShopManager() {
        this.shopItems = new ArrayList<>();
        this.playerInventory = new ArrayList<>();
        this.playerCurrency = new HashMap<>();
        this.currentState = ShopState.MAIN;
        this.userManager = null;
        initializeCurrency();
    }
    
    public ShopManager(UserManager userManager) {
        this.shopItems = new ArrayList<>();
        this.playerInventory = new ArrayList<>();
        this.playerCurrency = new HashMap<>();
        this.currentState = ShopState.MAIN;
        this.userManager = userManager;
        initializeCurrency();
    }
    
    private void initializeCurrency() {
        playerCurrency.put("COINS", 1000);
        playerCurrency.put("GEMS", 50);
        playerCurrency.put("POINTS", 0);
    }
    
    // 아이템 관리
    public void addItem(ShopItem item) {
        shopItems.add(item);
    }
    
    public void removeItem(String itemId) {
        shopItems.removeIf(item -> item.getId().equals(itemId));
    }
    
    public ShopItem getItemById(String itemId) {
        return shopItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }
    
    // 구매 시스템
    public boolean purchaseItem(String itemId) {
        ShopItem item = getItemById(itemId);
        if (item == null || item.isPurchased() || !item.isAvailable()) {
            setPurchaseMessage("구매할 수 없는 아이템입니다.");
            return false;
        }
        
        int price = item.getFinalPrice();
        
        // UserManager가 있으면 실시간 DB 동기화
        if (userManager != null && userManager.isLoggedIn()) {
            if (userManager.spendCoins(price)) {
                item.setPurchased(true);
                playerInventory.add(item);
                // DB에 인벤토리 저장
                saveInventoryToDB();
                setPurchaseMessage(item.getName() + "을(를) 구매했습니다!");
                return true;
            } else {
                setPurchaseMessage("코인이 부족합니다! (필요: " + price + ", 보유: " + userManager.getCurrentUser().getCoins() + ")");
                return false;
            }
        } else {
            // 기존 로직 (UserManager가 없는 경우)
            if (playerCurrency.get("COINS") >= price) {
                playerCurrency.put("COINS", playerCurrency.get("COINS") - price);
                item.setPurchased(true);
                playerInventory.add(item);
                setPurchaseMessage(item.getName() + "을(를) 구매했습니다!");
                return true;
            } else {
                setPurchaseMessage("코인이 부족합니다! (필요: " + price + ", 보유: " + playerCurrency.get("COINS") + ")");
                return false;
            }
        }
    }
    
    // 검색 및 필터링
    public List<ShopItem> getItemsByCategory(ShopCategory category) {
        return shopItems.stream()
                .filter(item -> item.getCategory() == category)
                .collect(Collectors.toList());
    }
    
    public List<ShopItem> searchItems(String keyword) {
        this.searchKeyword = keyword;
        return shopItems.stream()
                .filter(item -> item.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                              item.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    public List<ShopItem> getFilteredItems() {
        return shopItems.stream()
                .filter(item -> filterCategory == null || item.getCategory() == filterCategory)
                .filter(item -> filterRarity == null || item.getRarity() == filterRarity)
                .filter(item -> searchKeyword == null || 
                              item.getName().toLowerCase().contains(searchKeyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    // 확장 가능한 메서드들
    public void addCurrency(String type, int amount) {
        playerCurrency.put(type, playerCurrency.getOrDefault(type, 0) + amount);
    }
    
    public boolean spendCurrency(String type, int amount) {
        int current = playerCurrency.getOrDefault(type, 0);
        if (current >= amount) {
            playerCurrency.put(type, current - amount);
            return true;
        }
        return false;
    }
    
    public void setFilter(ShopCategory category, ItemRarity rarity) {
        this.filterCategory = category;
        this.filterRarity = rarity;
    }
    
    // Getters
    public List<ShopItem> getShopItems() { return shopItems; }
    public List<ShopItem> getPlayerInventory() { return playerInventory; }
    public Map<String, Integer> getPlayerCurrency() { return playerCurrency; }
    public ShopState getCurrentState() { return currentState; }
    
    public void setCurrentState(ShopState state) { this.currentState = state; }
    public void setCurrentCategory(ShopCategory category) { this.currentCategory = category; }
    public ShopCategory getCurrentCategory() { return currentCategory; }
    
    // 메인 메뉴로 돌아갈 때 카테고리 정보 유지
    public void returnToMainMenu() {
        this.currentState = ShopState.MAIN;
        // currentCategory는 유지
    }
    
    // UserManager 설정
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
        // UserManager가 설정되면 현재 사용자 데이터로 동기화 (안전하게 처리)
        try {
            if (userManager != null && userManager.isLoggedIn()) {
                syncFromUserManager();
                loadInventoryFromDB(); // 인벤토리도 로드
            }
        } catch (Exception e) {
            System.err.println("상점 데이터 동기화 중 오류 발생: " + e.getMessage());
            // 동기화 실패해도 계속 진행
        }
    }
    
    // UserManager에서 사용자 데이터 동기화
    private void syncFromUserManager() {
        if (userManager != null && userManager.isLoggedIn()) {
            org.newdawn.spaceinvaders.login.User currentUser = userManager.getCurrentUser();
            if (currentUser != null) {
                playerCurrency.put("COINS", currentUser.getCoins());
                playerCurrency.put("GEMS", currentUser.getGems());
                System.out.println("상점 데이터를 사용자 데이터로 동기화 완료");
            }
        }
    }
    
    // DB에 인벤토리 저장 (확장된 형식)
    private void saveInventoryToDB() {
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                // 인벤토리를 상세한 객체 형식으로 변환
                java.util.Map<String, Object> inventoryData = new java.util.HashMap<>();
                java.util.Map<String, Object> items = new java.util.HashMap<>();
                java.util.Map<String, String> equipped = new java.util.HashMap<>();
                
                // 각 아이템의 상세 정보 저장
                for (ShopItem item : playerInventory) {
                    java.util.Map<String, Object> itemData = new java.util.HashMap<>();
                    itemData.put("purchased", true);
                    itemData.put("purchasedDate", java.time.LocalDateTime.now().toString());
                    itemData.put("isEquipped", false); // 기본값
                    itemData.put("level", 1); // 기본 레벨
                    itemData.put("upgradeCount", 0); // 업그레이드 횟수
                    
                    items.put(item.getId(), itemData);
                }
                
                // 장착된 아이템 정보 (기본값)
                equipped.put("weapon", null);
                equipped.put("spaceship", null);
                equipped.put("powerup", null);
                
                inventoryData.put("items", items);
                inventoryData.put("equipped", equipped);
                
                // Firebase DB에 인벤토리 저장
                boolean success = userManager.getFirebaseDB().putData(
                    "users/" + userManager.getCurrentUser().getUid() + "/inventory", 
                    inventoryData
                );
                
                if (success) {
                    System.out.println("인벤토리 DB 저장 완료: " + items.size() + "개 아이템 (확장 형식)");
                } else {
                    System.err.println("인벤토리 DB 저장 실패");
                }
            } catch (Exception e) {
                System.err.println("인벤토리 DB 저장 중 오류: " + e.getMessage());
            }
        }
    }
    
    // DB에서 인벤토리 로드 (확장된 형식 지원)
    public void loadInventoryFromDB() {
        if (userManager != null && userManager.isLoggedIn()) {
            try {
                // Firebase DB에서 인벤토리 로드
                Object inventoryData = userManager.getFirebaseDB().getData(
                    "users/" + userManager.getCurrentUser().getUid() + "/inventory", 
                    Object.class
                );
                
                if (inventoryData != null) {
                    // 기존 인벤토리 초기화
                    playerInventory.clear();
                    
                    if (inventoryData instanceof List) {
                        // 기존 형식 (문자열 배열) 지원
                        List<String> inventoryIds = (List<String>) inventoryData;
                        for (String itemId : inventoryIds) {
                            ShopItem item = getItemById(itemId);
                            if (item != null) {
                                item.setPurchased(true);
                                playerInventory.add(item);
                            }
                        }
                        System.out.println("인벤토리 DB 로드 완료 (기존 형식): " + inventoryIds.size() + "개 아이템");
                    } else if (inventoryData instanceof java.util.Map) {
                        // 새로운 형식 (객체) 지원
                        java.util.Map<String, Object> inventoryMap = (java.util.Map<String, Object>) inventoryData;
                        Object itemsObj = inventoryMap.get("items");
                        
                        if (itemsObj instanceof java.util.Map) {
                            java.util.Map<String, Object> items = (java.util.Map<String, Object>) itemsObj;
                            
                            for (String itemId : items.keySet()) {
                                ShopItem item = getItemById(itemId);
                                if (item != null) {
                                    item.setPurchased(true);
                                    playerInventory.add(item);
                                }
                            }
                            System.out.println("인벤토리 DB 로드 완료 (확장 형식): " + items.size() + "개 아이템");
                        }
                    }
                } else {
                    System.out.println("인벤토리가 비어있습니다.");
                }
            } catch (Exception e) {
                System.err.println("인벤토리 DB 로드 중 오류: " + e.getMessage());
            }
        }
    }
    
    // 구매 메시지 설정
    private void setPurchaseMessage(String message) {
        this.purchaseMessage = message;
        this.messageTimer = 180; // 3초간 표시 (60fps 기준)
    }
    
    // 구매 메시지 가져오기
    public String getPurchaseMessage() {
        return purchaseMessage;
    }
    
    // 메시지 타이머 업데이트
    public void updateMessageTimer() {
        if (messageTimer > 0) {
            messageTimer--;
            if (messageTimer == 0) {
                purchaseMessage = "";
            }
        }
    }
    
    // 메시지 표시 여부 확인
    public boolean hasMessage() {
        return messageTimer > 0;
    }
    
    // 선택된 아이템 가져오기 (구매 확인용)
    public ShopItem getSelectedItem(int selectedIndex) {
        // 현재 카테고리의 아이템들 중에서 선택된 아이템 반환
        List<ShopItem> categoryItems = getItemsByCategory(currentCategory);
        if (selectedIndex >= 0 && selectedIndex < categoryItems.size()) {
            return categoryItems.get(selectedIndex);
        }
        return null;
    }
    
    // 선택된 아이템 ID로 아이템 가져오기
    public ShopItem getSelectedItemById(String itemId) {
        return getItemById(itemId);
    }
}