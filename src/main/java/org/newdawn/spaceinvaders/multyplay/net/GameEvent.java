package org.newdawn.spaceinvaders.multyplay.net;

/**
 * 간단한 게임 이벤트 (채팅, 시스템 알림 등). 필요 시 타입 세분화.
 */
public class GameEvent {
    public enum Type { CHAT, SYSTEM, SKILL_REQUEST, SKILL_UPGRADE, SKILL_UPDATE, COIN_POPUP }
    public final Type type;
    public final String fromPlayerId;
    public final String message;
    public final long time;

    public GameEvent(Type type, String fromPlayerId, String message, long time) {
        this.type = type; this.fromPlayerId = fromPlayerId; this.message = message; this.time = time;
    }
}
