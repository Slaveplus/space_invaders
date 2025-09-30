package org.newdawn.spaceinvaders.shop;

import java.awt.event.KeyEvent;
import java.util.List;

/**
 * 상점 입력 처리 클래스
 * 확장 가능한 입력 시스템
 */
public class ShopInputHandler {
    private ShopManager shopManager;
    private int selectedOption = 0;
    private int selectedItem = 0;
    private boolean exitRequested = false;
    
    public ShopInputHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
    }
    
    public void handleInput(int keyCode) {
        System.out.println("상점 키 입력: " + keyCode + ", 상태: " + shopManager.getCurrentState());
        
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
                System.out.println("선택된 옵션: " + selectedOption);
                break;
            case KeyEvent.VK_DOWN:
                selectedOption = Math.min(mainOptions.length - 1, selectedOption + 1);
                System.out.println("선택된 옵션: " + selectedOption);
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                handleMainShopSelection();
                break;
            case KeyEvent.VK_ESCAPE:
                // 상점 종료
                break;
        }
    }
    
    private void handleMainShopSelection() {
        System.out.println("메인 상점 선택: " + selectedOption);
        switch (selectedOption) {
            case 0: // 무기 상점
                shopManager.setCurrentState(ShopState.CATEGORY);
                shopManager.setCurrentCategory(ShopCategory.WEAPONS);
                selectedItem = 0;
                break;
            case 1: // 우주선 상점
                shopManager.setCurrentState(ShopState.CATEGORY);
                shopManager.setCurrentCategory(ShopCategory.SPACESHIPS);
                selectedItem = 0;
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
                shopManager.returnToMainMenu();
                // selectedOption은 유지하고 selectedItem만 리셋
                selectedItem = 0;
                System.out.println("메인 메뉴로 돌아가기 - 이전 선택 유지: " + selectedOption);
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
            case KeyEvent.VK_ESCAPE:
                shopManager.returnToMainMenu();
                // selectedOption은 유지
                System.out.println("메인 메뉴로 돌아가기 - 이전 선택 유지: " + selectedOption);
                break;
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
        // 구매 확인 창의 예/아니요 버튼 클릭 처리
        int dialogX = 200; // (800 - 400) / 2
        int dialogY = 175; // (600 - 250) / 2
        int buttonY = dialogY + 170;
        int yesX = dialogX + (400 / 2) - 80 - 10; // 110
        int noX = dialogX + (400 / 2) + 10; // 210
        
        if (x >= yesX && x <= yesX + 80 && y >= buttonY && y <= buttonY + 30) {
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
        } else if (x >= noX && x <= noX + 80 && y >= buttonY && y <= buttonY + 30) {
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