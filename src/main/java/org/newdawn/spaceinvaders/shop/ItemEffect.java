package org.newdawn.spaceinvaders.shop;

/**
 * 아이템 효과 인터페이스
 * 다양한 효과 타입 추가 가능
 */
public interface ItemEffect {
    String getEffectName();
    String getEffectDescription();
    void applyEffect();
    void removeEffect();
    boolean isActive();
    String getType();
    double getValue();
}