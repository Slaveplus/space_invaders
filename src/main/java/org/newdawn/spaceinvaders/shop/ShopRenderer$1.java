package org.newdawn.spaceinvaders.shop;

/**
 * Manually replicated enum switch mapping class to satisfy runtime lookups.
 * The compiler usually generates this when using switch statements on enums, but
 * packaging issues were causing the synthetic class to be absent at runtime.
 */
final class ShopRenderer$1 {
    static final int[] $SwitchMap$org$newdawn$spaceinvaders$shop$ShopState;
    static final int[] $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory;
    static final int[] $SwitchMap$org$newdawn$spaceinvaders$shop$ItemRarity;

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

        $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory = new int[ShopCategory.values().length];
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory[ShopCategory.WEAPONS.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory[ShopCategory.SPACESHIPS.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory[ShopCategory.POWERUPS.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory[ShopCategory.DECORATIONS.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ShopCategory[ShopCategory.UPGRADES.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) { }

        $SwitchMap$org$newdawn$spaceinvaders$shop$ItemRarity = new int[ItemRarity.values().length];
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ItemRarity[ItemRarity.COMMON.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ItemRarity[ItemRarity.RARE.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ItemRarity[ItemRarity.EPIC.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) { }
        try {
            $SwitchMap$org$newdawn$spaceinvaders$shop$ItemRarity[ItemRarity.LEGENDARY.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) { }
    }

    private ShopRenderer$1() {
        // No instances.
    }
}
