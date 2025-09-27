package org.newdawn.spaceinvaders.shop;

import java.util.*;
import java.util.stream.Collectors;

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
    
    public ShopManager() {
        this.shopItems = new ArrayList<>();
        this.playerInventory = new ArrayList<>();
        this.playerCurrency = new HashMap<>();
        this.currentState = ShopState.MAIN;
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
            return false;
        }
        
        int price = item.getFinalPrice();
        if (playerCurrency.get("COINS") >= price) {
            playerCurrency.put("COINS", playerCurrency.get("COINS") - price);
            item.setPurchased(true);
            playerInventory.add(item);
            return true;
        }
        return false;
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