package org.newdawn.spaceinvaders.shop;

import java.awt.event.KeyEvent;
import java.util.List;

/**
 * 상점 입력 처리 클래스
 * 확장 가능한 입력 시스템
 */
public class ShopInputHandler {
    private ShopManager shopManager;
    private Shop shop; // Shop 참조 추가 (애니메이션용)
    private int selectedOption = 0;
    private int selectedItem = 0;
    private boolean exitRequested = false;
    
    public ShopInputHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
    }
    
    // Shop 참조 설정 (애니메이션용)
    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    public void handleInput(int keyCode) {
        
        switch (shopManager.getCurrentState()) {
            case MAIN:
                handleMainShopInput(keyCode);
                break;
            case CATEGORY:
                handleCategoryInput(keyCode);
                break;
            case ITEM_DETAIL:
                handleItemDetailInput(keyCode);
                break;
            case PURCHASE:
                handlePurchaseInput(keyCode);
                break;
            case INVENTORY:
                handleInventoryInput(keyCode);
                break;
            case SEARCH:
                handleSearchInput(keyCode);
                break;
            case FILTER:
                handleFilterInput(keyCode);
                break;
            case CART:
                handleCartInput(keyCode);
                break;
            case WISHLIST:
                handleWishlistInput(keyCode);
                break;
        }
    }
    
    private void handleMainShopInput(int keyCode) {
        String[] mainOptions = {
            "무기 상점", "우주선 상점", "뒤로가기"
        };
        
        switch (keyCode) {
            case KeyEvent.VK_UP:
                selectedOption = Math.max(0, selectedOption - 1);
                break;
            case KeyEvent.VK_DOWN:
                selectedOption = Math.min(mainOptions.length - 1, selectedOption + 1);
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                handleMainShopSelection();
                break;
            case KeyEvent.VK_ESCAPE:
                // 상점 종료 요청
                exitRequested = true;
                break;
        }
    }
    
    private void handleMainShopSelection() {
        switch (selectedOption) {
            case 0: // 무기 상점
                shopManager.setCurrentState(ShopState.CATEGORY);
                shopManager.setCurrentCategory(ShopCategory.WEAPONS);
                selectedItem = 0;
                // 카테고리 진입 애니메이션 시작
                if (shop != null) {
                    shop.startCategoryEntryAnimation();
                }
                break;
            case 1: // 우주선 상점
                shopManager.setCurrentState(ShopState.CATEGORY);
                shopManager.setCurrentCategory(ShopCategory.SPACESHIPS);
                selectedItem = 0;
                // 카테고리 진입 애니메이션 시작
                if (shop != null) {
                    shop.startCategoryEntryAnimation();
                }
                break;
            case 2: // 뒤로가기
                exitRequested = true;
                break;
        }
    }
    
    private void handleCategoryInput(int keyCode) {
        // 현재 카테고리의 아이템들만 필터링
        List<ShopItem> categoryItems = shopManager.getShopItems().stream()
                .filter(item -> item.getCategory() == shopManager.getCurrentCategory())
                .collect(java.util.stream.Collectors.toList());
        
    int itemsPerRow = 3;
        
        switch (keyCode) {
            case KeyEvent.VK_UP:
                if (selectedItem >= itemsPerRow) {
                    selectedItem -= itemsPerRow;
                }
                System.out.println("선택된 아이템: " + selectedItem);
                break;
            case KeyEvent.VK_DOWN:
                if (selectedItem + itemsPerRow < categoryItems.size()) {
                    selectedItem += itemsPerRow;
                }
                System.out.println("선택된 아이템: " + selectedItem);
                break;
            case KeyEvent.VK_LEFT:
                if (selectedItem > 0) {
                    selectedItem--;
                }
                System.out.println("선택된 아이템: " + selectedItem);
                break;
            case KeyEvent.VK_RIGHT:
                if (selectedItem < categoryItems.size() - 1) {
                    selectedItem++;
                }
                System.out.println("선택된 아이템: " + selectedItem);
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                if (selectedItem < categoryItems.size()) {
                    shopManager.setCurrentState(ShopState.PURCHASE);
                    selectedOption = 0; // 예 버튼 선택
                    System.out.println("구매 확인으로 이동: " + categoryItems.get(selectedItem).getName());
                }
                break;
            case KeyEvent.VK_ESCAPE:
                // 카테고리 나가기 애니메이션 시작
                if (shop != null) {
                    shop.startCategoryExitAnimation();
                }
                shopManager.returnToMainMenu();
                // selectedOption은 유지하고 selectedItem만 리셋
                selectedItem = 0;
                break;
        }
    }
    
    private void handleItemDetailInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                shopManager.setCurrentState(ShopState.PURCHASE);
                break;
            case KeyEvent.VK_ESCAPE:
                shopManager.setCurrentState(ShopState.CATEGORY);
                break;
        }
    }
    
    private void handlePurchaseInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                // 예/아니요 버튼 간 이동
                selectedOption = (selectedOption == 0) ? 1 : 0;
                System.out.println("구매 확인 선택: " + (selectedOption == 0 ? "예" : "아니요"));
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                if (selectedOption == 0) {
                    // 예 - 구매 실행
                    List<ShopItem> categoryItems = shopManager.getShopItems().stream()
                            .filter(item -> item.getCategory() == shopManager.getCurrentCategory())
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (selectedItem < categoryItems.size()) {
                        ShopItem item = categoryItems.get(selectedItem);
                        if (shopManager.purchaseItem(item.getId())) {
                            System.out.println(item.getName() + "을(를) 구매했습니다!");
                        } else {
                            // 구매 실패 메시지는 ShopManager에서 이미 설정됨
                            System.out.println("구매에 실패했습니다. (코인 부족 또는 이미 구매함)");
                        }
                    }
                }
                // 구매 확인 후 카테고리 화면으로 돌아가기
                shopManager.setCurrentState(ShopState.CATEGORY);
                break;
            case KeyEvent.VK_ESCAPE:
                // 아니요 - 카테고리 화면으로 돌아가기
                shopManager.setCurrentState(ShopState.CATEGORY);
                break;
        }
    }
    
    private void handleInventoryInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_Q:
                // Q 키로 카테고리 변경 (Tab 키 대신)
                changeInventoryCategory(1);
                break;
            case KeyEvent.VK_LEFT:
                // 아이템 선택 왼쪽으로
                moveInventorySelection(-1);
                break;
            case KeyEvent.VK_RIGHT:
                // 아이템 선택 오른쪽으로
                moveInventorySelection(1);
                break;
            case KeyEvent.VK_UP:
                // 아이템 선택 위로
                moveInventorySelection(-3); // 3컬럼이므로 -3
                break;
            case KeyEvent.VK_DOWN:
                // 아이템 선택 아래로
                moveInventorySelection(3); // 3컬럼이므로 +3
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                // 아이템 장착/해제
                toggleItemEquip();
                break;
            case KeyEvent.VK_ESCAPE:
                shopManager.returnToMainMenu();
                // selectedOption은 유지
                break;
        }
    }
    
    private void changeInventoryCategory(int direction) {
        ShopCategory[] categories = {
            ShopCategory.WEAPONS,
            ShopCategory.SPACESHIPS
        };
        
        ShopCategory currentCategory = shopManager.getInventoryCategory();
        System.out.println("현재 카테고리: " + currentCategory + ", 방향: " + direction);
        
        int currentIndex = -1;
        
        for (int i = 0; i < categories.length; i++) {
            if (categories[i] == currentCategory) {
                currentIndex = i;
                break;
            }
        }
        
        System.out.println("현재 인덱스: " + currentIndex);
        
        if (currentIndex != -1) {
            int newIndex = currentIndex + direction;
            if (newIndex < 0) {
                newIndex = categories.length - 1;
            } else if (newIndex >= categories.length) {
                newIndex = 0;
            }
            
            System.out.println("새 인덱스: " + newIndex + " -> " + categories[newIndex]);
            shopManager.setInventoryCategory(categories[newIndex]);
            selectedItem = 0; // 카테고리 변경 시 선택 초기화
            System.out.println("인벤토리 카테고리 변경 완료: " + categories[newIndex].getDisplayName());
        } else {
            System.out.println("현재 카테고리를 찾을 수 없음: " + currentCategory);
        }
    }
    
    private void moveInventorySelection(int direction) {
        List<ShopItem> categoryItems = shopManager.getInventoryByCategory(shopManager.getInventoryCategory());
        if (!categoryItems.isEmpty()) {
            int newSelectedItem = selectedItem + direction;
            
            // 범위 체크 및 순환 처리
            if (newSelectedItem < 0) {
                newSelectedItem = categoryItems.size() - 1;
            } else if (newSelectedItem >= categoryItems.size()) {
                newSelectedItem = 0;
            }
            
            selectedItem = newSelectedItem;
            System.out.println("인벤토리 아이템 선택: " + selectedItem + " (총 " + categoryItems.size() + "개)");
        }
    }
    
    private void toggleItemEquip() {
        List<ShopItem> categoryItems = shopManager.getInventoryByCategory(shopManager.getInventoryCategory());
        if (!categoryItems.isEmpty() && selectedItem < categoryItems.size()) {
            ShopItem item = categoryItems.get(selectedItem);
            ShopCategory category = item.getCategory();
            
            if (shopManager.isItemEquipped(item)) {
                // 아이템 해제
                if (shopManager.unequipItem(category)) {
                    System.out.println("아이템 해제: " + item.getName());
                }
            } else {
                // 호환성 확인 (무기 아이템의 경우)
                if (item.getCategory() == ShopCategory.WEAPONS) {
                    ShopItem currentSpaceship = shopManager.getEquippedItem(ShopCategory.SPACESHIPS);
                    if (!item.isCompatibleWith(currentSpaceship)) {
                        String requiredShipName = getSpaceshipNameById(item.getRequiredSpaceshipId());
                        shopManager.setWarningMessage("이 무기는 " + requiredShipName + " 전용입니다!");
                        System.out.println("장착 실패: " + item.getName() + "은(는) " + requiredShipName + " 전용 무기입니다.");
                        return;
                    }
                }
                
                // 아이템 장착
                if (shopManager.equipItem(item)) {
                    System.out.println("아이템 장착: " + item.getName());
                }
            }
        }
    }
    
    /**
     * 우주선 ID로 우주선 이름을 반환
     */
    private String getSpaceshipNameById(String spaceshipId) {
        if (spaceshipId == null) return "모든 우주선";
        
        switch (spaceshipId) {
            case "king":
                return "총장님";
            case "professor":
                return "평생지도교수님";
            case "software_king":
                return "학과장님";
            case "fighter_ship":
                return "Green SpaceShip";
            case "battleship":
                return "Blue SpaceShip";
            default:
                return "알 수 없는 우주선";
        }
    }
    
    private void handleSearchInput(int keyCode) {
        // 검색 입력 처리
        // 구현 필요...
    }
    
    private void handleFilterInput(int keyCode) {
        // 필터 입력 처리
        // 구현 필요...
    }
    
    private void handleCartInput(int keyCode) {
        // 장바구니 입력 처리
        // 구현 필요...
    }
    
    private void handleWishlistInput(int keyCode) {
        // 위시리스트 입력 처리
        // 구현 필요...
    }

    public void reset(){
        selectedOption = 0;
        selectedItem = 0;
        exitRequested = false;
    }
    
    public boolean isExitRequested() {
        return exitRequested;
    }
    
    public void resetExitRequest() {
        exitRequested = false;
    }
    
    public int getSelectedOption() { 
        return selectedOption; 
    }
    
    public int getSelectedItem() { 
        return selectedItem; 
    }
    
    public void setSelectedOption(int option) { this.selectedOption = option; }
    public void setSelectedItem(int item) { this.selectedItem = item; }
    
    public void handleMouseClick(int x, int y) {
        switch (shopManager.getCurrentState()) {
            case MAIN:
                handleMainShopMouseClick(x, y);
                break;
            case CATEGORY:
                handleCategoryMouseClick(x, y);
                break;
            case ITEM_DETAIL:
                handleItemDetailMouseClick(x, y);
                break;
            case PURCHASE:
                handlePurchaseMouseClick(x, y);
                break;
            case INVENTORY:
                handleInventoryMouseClick(x, y);
                break;
            case SEARCH:
                handleSearchMouseClick(x, y);
                break;
            case FILTER:
                handleFilterMouseClick(x, y);
                break;
            case CART:
                handleCartMouseClick(x, y);
                break;
            case WISHLIST:
                handleWishlistMouseClick(x, y);
                break;
        }
    }
    
    private void handleMainShopMouseClick(int x, int y) {
        String[] mainOptions = {
            "무기 상점", "우주선 상점", "뒤로가기"
        };
        
        int startY = 150;
        int lineHeight = 40;
        
        for (int i = 0; i < mainOptions.length; i++) {
            int optionY = startY + (i * lineHeight);
            if (x >= 300 && x <= 500 && y >= optionY - 20 && y <= optionY + 20) {
                selectedOption = i;
                handleMainShopSelection();
                System.out.println("마우스 클릭: " + mainOptions[i]);
                break;
            }
        }
    }
    
    private void handleCategoryMouseClick(int x, int y) {
        // 카테고리 화면에서 아이템 클릭 처리
        List<ShopItem> categoryItems = shopManager.getShopItems().stream()
                .filter(item -> item.getCategory() == shopManager.getCurrentCategory())
                .collect(java.util.stream.Collectors.toList());
        
        int startY = 150;
        int lineHeight = 30;
        
        for (int i = 0; i < categoryItems.size(); i++) {
            int itemY = startY + (i * lineHeight);
            if (x >= 50 && x <= 750 && y >= itemY - 15 && y <= itemY + 15) {
                selectedItem = i;
                shopManager.setCurrentState(ShopState.PURCHASE);
                selectedOption = 0; // 예 버튼 선택
                System.out.println("마우스 클릭: " + categoryItems.get(i).getName());
                break;
            }
        }
    }
    
    private void handleItemDetailMouseClick(int x, int y) {
        // 아이템 상세 화면에서 구매 버튼 클릭 처리
        if (x >= 300 && x <= 500 && y >= 400 && y <= 450) {
            shopManager.setCurrentState(ShopState.PURCHASE);
            System.out.println("마우스 클릭: 구매 버튼");
        }
    }
    
    private void handlePurchaseMouseClick(int x, int y) {
        // 구매 확인 창의 예/아니요 버튼 클릭 처리 (새로운 크기에 맞게 수정)
        int dialogX = 100; // (800 - 600) / 2
        int dialogY = 75; // (600 - 450) / 2
        int buttonY = dialogY + 380;
        int yesX = dialogX + (600 / 2) - 100 - 15; // 185
        int noX = dialogX + (600 / 2) + 15; // 315
        
        if (x >= yesX && x <= yesX + 100 && y >= buttonY && y <= buttonY + 40) {
            // 예 버튼 클릭
            selectedOption = 0;
            // 구매 실행
            List<ShopItem> categoryItems = shopManager.getShopItems().stream()
                    .filter(item -> item.getCategory() == shopManager.getCurrentCategory())
                    .collect(java.util.stream.Collectors.toList());
            
            if (selectedItem < categoryItems.size()) {
                ShopItem item = categoryItems.get(selectedItem);
                if (shopManager.purchaseItem(item.getId())) {
                    System.out.println(item.getName() + "을(를) 구매했습니다!");
                } else {
                    System.out.println("구매에 실패했습니다. (코인 부족 또는 이미 구매함)");
                }
            }
            shopManager.setCurrentState(ShopState.CATEGORY);
            System.out.println("마우스 클릭: 구매 확인");
        } else if (x >= noX && x <= noX + 100 && y >= buttonY && y <= buttonY + 40) {
            // 아니요 버튼 클릭
            selectedOption = 1;
            shopManager.setCurrentState(ShopState.CATEGORY);
            System.out.println("마우스 클릭: 구매 취소");
        }
    }
    
    private void handleInventoryMouseClick(int x, int y) {
        // 인벤토리 화면에서 아이템 클릭 처리
        List<ShopItem> inventory = shopManager.getPlayerInventory();
        int startY = 150;
        int lineHeight = 30;
        
        for (int i = 0; i < inventory.size(); i++) {
            int itemY = startY + (i * lineHeight);
            if (x >= 50 && x <= 750 && y >= itemY - 15 && y <= itemY + 15) {
                selectedItem = i;
                System.out.println("마우스 클릭: " + inventory.get(i).getName());
                break;
            }
        }
    }
    
    private void handleSearchMouseClick(int x, int y) {
        // 검색 화면에서 검색 버튼 클릭 처리
        if (x >= 300 && x <= 500 && y >= 300 && y <= 350) {
            System.out.println("마우스 클릭: 검색 버튼");
        }
    }
    
    private void handleFilterMouseClick(int x, int y) {
        // 필터 화면에서 필터 옵션 클릭 처리
        System.out.println("마우스 클릭: 필터 옵션");
    }
    
    private void handleCartMouseClick(int x, int y) {
        // 장바구니 화면에서 아이템 클릭 처리
        System.out.println("마우스 클릭: 장바구니 아이템");
    }
    
    private void handleWishlistMouseClick(int x, int y) {
        // 위시리스트 화면에서 아이템 클릭 처리
        System.out.println("마우스 클릭: 위시리스트 아이템");
    }
}