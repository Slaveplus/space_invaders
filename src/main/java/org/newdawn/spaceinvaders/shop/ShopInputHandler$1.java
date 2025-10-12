package org.newdawn.spaceinvaders.shop;

/**
 * Synthetic enum switch map for ShopInputHandler. Added explicitly because the
 * automatically generated helper from javac was not packaged, leading to
 * NoClassDefFoundError at runtime.
 */
final class ShopInputHandler$1 {
    static final int[] $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState;

    static {
        $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState = new int[ShopState.values().length];
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.MAIN.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.CATEGORY.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.ITEM_DETAIL.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.PURCHASE.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.INVENTORY.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.SEARCH.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.FILTER.ordinal()] = 7;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.CART.ordinal()] = 8;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState[ShopState.WISHLIST.ordinal()] = 9;
        } catch (NoSuchFieldError ignored) { }
    }

    private ShopInputHandler$1() {
        // No instances.
    }
}
