package org.newdawn.spaceinvaders.shop;

/**
 * 상점 상태 열거형
 * 향후 새로운 상태 추가 가능
 */
public enum ShopState {
    MAIN("메인 상점"),
    CATEGORY("카테고리별 상점"),
    ITEM_DETAIL("아이템 상세"),
    PURCHASE("구매 확인"),
    INVENTORY("인벤토리"),
    SEARCH("검색"),
    FILTER("필터링"),
    CART("장바구니"),
    WISHLIST("위시리스트");
    
    private final String displayName;
    
    ShopState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}