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
    
    // 경고창 관련 변수들
    private String warningMessage = "";
    private int warningTimer = 0;
    private EquipmentManager equipmentManager;
    private ShopCategory inventoryCategory = ShopCategory.WEAPONS; // 인벤토리에서 현재 보고 있는 카테고리
    
    public ShopManager() {
        this.shopItems = new ArrayList<>();
        this.playerInventory = new ArrayList<>();
        this.playerCurrency = new HashMap<>();
        this.currentState = ShopState.MAIN;
        this.userManager = null;
        this.equipmentManager = null;
        initializeCurrency();
    }
    
    public ShopManager(UserManager userManager) {
        this.shopItems = new ArrayList<>();
        this.playerInventory = new ArrayList<>();
        this.playerCurrency = new HashMap<>();
        this.currentState = ShopState.MAIN;
        this.userManager = userManager;
        this.equipmentManager = new EquipmentManager(userManager);
        initializeCurrency();
        
        // 사용자가 로그인되어 있으면 인벤토리 로드
        if (userManager != null && userManager.isLoggedIn()) {
            loadInventoryFromDB();
            loadEquipmentFromDB();
        }
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
    
    // ShopManager에 기본 아이템들을 추가하는 static 메서드
    public static void initializeDefaultItems(ShopManager shopManager) {
        // 무기 아이템들
        ShopItem laserGun = new ShopItem("laser_gun", "레이저 건", "기본 레이저 무기", 100,
                                       ShopCategory.WEAPONS, ItemRarity.COMMON);
        laserGun.setIconPath("sprites/weapons/green_laser.png");
        shopManager.addItem(laserGun);
        
        ShopItem plasmaGun = new ShopItem("plasma_gun", "플라즈마 건", "강력한 플라즈마 무기", 300, 
                                       ShopCategory.WEAPONS, ItemRarity.RARE);
        plasmaGun.setIconPath("sprites/weapons/plasma.png");
        shopManager.addItem(plasmaGun);
        
        ShopItem missileLauncher = new ShopItem("missile_launcher", "미사일 발사기", "원거리 미사일 무기", 500, 
                                       ShopCategory.WEAPONS, ItemRarity.EPIC);
        missileLauncher.setIconPath("sprites/weapons/missile.png");
        shopManager.addItem(missileLauncher);

        ShopItem report = new ShopItem("report", "레포트 폭탄", "재앙이 몰려온다....", 2000, 
                                       ShopCategory.WEAPONS, ItemRarity.LEGENDARY);
        report.setIconPath("sprites/weapons/report.jpg");
        shopManager.addItem(report);

        ShopItem school_logo = new ShopItem("school_logo", "전북대의 상징", "총장님 전용 무기", 6000, 
                                       ShopCategory.WEAPONS, ItemRarity.LEGENDARY);
        school_logo.setIconPath("sprites/weapons/school_logo.png");
        school_logo.setRequiredSpaceshipId("king"); // 총장님 전용
        shopManager.addItem(school_logo);

        ShopItem kimbap_code = new ShopItem("kimbap_code", "김밥 코드", "교수님이 극혐하신다", 3000, 
                                       ShopCategory.WEAPONS, ItemRarity.LEGENDARY);
        kimbap_code.setIconPath("sprites/weapons/kimbap_code.png");
        kimbap_code.setRequiredSpaceshipId("destroyer"); // 평생지도교수님 전용
        shopManager.addItem(kimbap_code);

        
        // 우주선 아이템들
        ShopItem fighterShip = new ShopItem("fighter_ship", "Green SpaceShip", "기깔나는 초록색 전투기", 200, 
                                       ShopCategory.SPACESHIPS, ItemRarity.COMMON);
        fighterShip.setIconPath("sprites/ships/spaceship_green.png");
        shopManager.addItem(fighterShip);
        
        ShopItem battleship = new ShopItem("battleship", "Blue SpaceShip", "모두가 부러워하는 파란색 전투기", 200, 
                                       ShopCategory.SPACESHIPS, ItemRarity.RARE);
        battleship.setIconPath("sprites/ships/spaceship_blue.png");
        shopManager.addItem(battleship);
        
        ShopItem destroyer = new ShopItem("destroyer", "평생지도교수님", "초초초희귀 킹갓제너럴 프로페서", 1500, 
                                       ShopCategory.SPACESHIPS, ItemRarity.LEGENDARY);
        destroyer.setIconPath("sprites/ships/professor.png");
        shopManager.addItem(destroyer);

        ShopItem king = new ShopItem("king", "총장님", "천원의 아침밥", 5000, 
                                       ShopCategory.SPACESHIPS, ItemRarity.LEGENDARY);
        king.setIconPath("sprites/ships/king.png");
        shopManager.addItem(king);

        ShopItem software_king = new ShopItem("software_king", "학과장님", "꿀성대 보유", 2000, 
                                       ShopCategory.SPACESHIPS, ItemRarity.LEGENDARY);
        software_king.setIconPath("sprites/ships/software_king.png");
        shopManager.addItem(software_king);
        
        // 파워업 아이템들
        ShopItem healthBoost = new ShopItem("health_boost", "체력 부스트", "체력을 50% 증가", 150, 
                                       ShopCategory.POWERUPS, ItemRarity.COMMON);
        healthBoost.setIconPath("sprites/powerups/health_boost.png");
        shopManager.addItem(healthBoost);
        
        ShopItem speedBoost = new ShopItem("speed_boost", "속도 부스트", "이동 속도를 30% 증가", 200, 
                                       ShopCategory.POWERUPS, ItemRarity.RARE);
        speedBoost.setIconPath("sprites/powerups/speed_boost.png");
        shopManager.addItem(speedBoost);
        
        ShopItem shield = new ShopItem("shield", "방어막", "일정 시간 무적", 300, 
                                       ShopCategory.POWERUPS, ItemRarity.EPIC);
        shield.setIconPath("sprites/powerups/shield.png");
        shopManager.addItem(shield);
        
        // 장식품 아이템들
        ShopItem neonLights = new ShopItem("neon_lights", "네온 라이트", "우주선에 네온 장식", 100, 
                                       ShopCategory.DECORATIONS, ItemRarity.COMMON);
        neonLights.setIconPath("sprites/decorations/neon_lights.png");
        shopManager.addItem(neonLights);
        
        ShopItem goldenPaint = new ShopItem("golden_paint", "골든 페인트", "황금색 도색", 250, 
                                       ShopCategory.DECORATIONS, ItemRarity.RARE);
        goldenPaint.setIconPath("sprites/decorations/golden_paint.png");
        shopManager.addItem(goldenPaint);
        
        ShopItem rainbowTrail = new ShopItem("rainbow_trail", "무지개 궤적", "무지개색 궤적 효과", 400, 
                                       ShopCategory.DECORATIONS, ItemRarity.EPIC);
        rainbowTrail.setIconPath("sprites/decorations/rainbow_trail.png");
        shopManager.addItem(rainbowTrail);
        
        // 업그레이드 아이템들
        ShopItem engineUpgrade = new ShopItem("engine_upgrade", "엔진 업그레이드", "엔진 성능 향상", 300, 
                                       ShopCategory.UPGRADES, ItemRarity.COMMON);
        engineUpgrade.setIconPath("sprites/upgrades/engine_upgrade.png");
        shopManager.addItem(engineUpgrade);
        
        ShopItem weaponUpgrade = new ShopItem("weapon_upgrade", "무기 업그레이드", "무기 성능 향상", 500, 
                                       ShopCategory.UPGRADES, ItemRarity.RARE);
        weaponUpgrade.setIconPath("sprites/upgrades/weapon_upgrade.png");
        shopManager.addItem(weaponUpgrade);
        
        ShopItem shieldUpgrade = new ShopItem("shield_upgrade", "방어막 업그레이드", "방어막 성능 향상", 700, 
                                       ShopCategory.UPGRADES, ItemRarity.EPIC);
        shieldUpgrade.setIconPath("sprites/upgrades/shield_upgrade.png");
        shopManager.addItem(shieldUpgrade);
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
        if (item == null || !item.isAvailable()) {
            setPurchaseMessage("구매할 수 없는 아이템입니다.");
            return false;
        }
        
        // 이미 구매된 아이템인지 확인
        if (item.isPurchased()) {
            setPurchaseMessage("이미 구매한 아이템입니다.");
            return false;
        }
        
        int price = item.getFinalPrice();
        
        // UserManager가 있으면 실시간 DB 동기화
        if (userManager != null && userManager.isLoggedIn()) {
            int currentCoins = userManager.getCurrentUser().getCoins();
            
            if (userManager.spendCoins(price)) {
                item.setPurchased(true);
                playerInventory.add(item);
                // DB에 인벤토리 저장
                saveInventoryToDB();
                setPurchaseMessage(item.getName() + "을(를) 구매했습니다!");
                return true;
            } else {
                setPurchaseMessage("코인이 부족합니다! (필요: " + price + ", 보유: " + currentCoins + ")");
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
    
    // InputHandler 접근자 (MainMenu에서 종료 요청 확인용)
    private ShopInputHandler inputHandler;
    
    public void setInputHandler(ShopInputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }
    
    public ShopInputHandler getInputHandler() {
        return this.inputHandler;
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
                    // 인벤토리 DB 저장 완료
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
                String uid = userManager.getCurrentUser().getUid();
                
                // Firebase DB에서 인벤토리 로드
                Object inventoryData = userManager.getFirebaseDB().getData(
                    "users/" + uid + "/inventory", 
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
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("ShopManager: 인벤토리 DB 로드 중 오류: " + e.getMessage());
                e.printStackTrace();
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
    
    // 경고창 설정 (7초간 표시)
    public void setWarningMessage(String message) {
        this.warningMessage = message;
        this.warningTimer = 600; 
        System.out.println("경고창 설정: " + message + " (타이머: " + warningTimer + ")");
    }
    
    // 경고창 메시지 가져오기
    public String getWarningMessage() {
        return warningMessage;
    }
    
    // 경고창 표시 여부 확인
    public boolean hasWarningDialog() {
        return warningTimer > 0;
    }
    
    // 경고창 타이머 업데이트
    public void updateWarningTimer() {
        if (warningTimer > 0) {
            warningTimer--;
            if (warningTimer == 0) {
                System.out.println("경고창 자동 닫기 (타이머 만료)");
                warningMessage = "";
            }
        }
    }
    
    // 경고창 강제 초기화
    public void clearWarningDialog() {
        System.out.println("경고창 강제 초기화");
        this.warningMessage = "";
        this.warningTimer = 0;
    }
    
    // 장착 관련 메서드들
    
    public ShopCategory getInventoryCategory() {
        return inventoryCategory;
    }
    
    public void setInventoryCategory(ShopCategory category) {
        this.inventoryCategory = category;
    }
    
    public List<ShopItem> getInventoryByCategory(ShopCategory category) {
        return playerInventory.stream()
                .filter(item -> item.getCategory() == category)
                .collect(Collectors.toList());
    }
    
    public boolean equipItem(ShopItem item) {
        if (equipmentManager != null) {
            return equipmentManager.equipItem(item);
        }
        return false;
    }
    
    public boolean unequipItem(ShopCategory category) {
        if (equipmentManager != null) {
            return equipmentManager.unequipItem(category);
        }
        return false;
    }
    
    public ShopItem getEquippedItem(ShopCategory category) {
        if (equipmentManager != null) {
            return equipmentManager.getEquippedItem(category);
        }
        return null;
    }
    
    public boolean isItemEquipped(ShopItem item) {
        if (equipmentManager != null) {
            return equipmentManager.isItemEquipped(item);
        }
        return false;
    }
    
    public void loadEquipmentFromDB() {
        if (equipmentManager != null) {
            equipmentManager.loadEquippedItemsFromDB(shopItems);
        }
    }
    
    /**
     * EquipmentManager 접근자
     */
    public EquipmentManager getEquipmentManager() {
        return equipmentManager;
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