package org.newdawn.spaceinvaders.multyplay.net.protocol;

/**
 * 멀티플레이 텍스트 메시지 공통 키 상수.
 */
public final class ProtocolKeys {
    private ProtocolKeys() {}

    public static final String ROOM_ID = "roomId";
    public static final String PLAYER_ID = "playerId";
    public static final String SEED = "seed";
    public static final String TICK = "tick";
    public static final String DELTA = "dt";
    public static final String SERVER_TIME = "time";
    public static final String ROUND = "round";
    public static final String ENTITIES = "entities";
    public static final String PLAYERS = "players";
    public static final String EVENTS = "events";
    public static final String INPUT_SEQ = "seq";
    public static final String INPUT_MASK = "mask";
    public static final String INPUT_TIME = "clientTime";
    public static final String ACTION = "action";
    public static final String DATA = "data";
    public static final String FROM_TICK = "fromTick";
    public static final String REASON = "reason";
}
