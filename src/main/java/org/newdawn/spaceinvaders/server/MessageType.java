package org.newdawn.spaceinvaders.server;

/**
 * 문자열 프로토콜 키/타입 상수 모음 (간단)
 * 포맷 예: TYPE|k1=v1|k2=v2 ... \n
 */
public final class MessageType {
    private MessageType() {}
    // Client -> Server
    public static final String CONNECT = "CONNECT"; // username
    public static final String LIST_ROOMS = "LIST_ROOMS"; // none
    public static final String CREATE_ROOM = "CREATE_ROOM"; // name,type=single|multi,max=4
    public static final String JOIN_ROOM = "JOIN_ROOM"; // roomId
    public static final String LEAVE_ROOM = "LEAVE_ROOM"; // none
    public static final String TOGGLE_READY = "TOGGLE_READY"; // none
    public static final String START_GAME = "START_GAME"; // none (host only)
    public static final String CHAT = "CHAT"; // msg=...
    public static final String GET_ROOM_STATE = "GET_ROOM_STATE"; // roomId(optional 현재 방)
    public static final String PING = "PING"; // 클라이언트 -> 서버
    public static final String PONG = "PONG"; // 서버 -> 클라이언트

    // Multiplayer (Client -> Server)
    public static final String GAME_READY = "GAME_READY"; // roomId, seedAck
    public static final String GAME_INPUT = "GAME_INPUT"; // roomId, playerId, seq, mask, time
    public static final String GAME_ACTION = "GAME_ACTION"; // roomId, playerId, action=...,data=...
    public static final String STATE_ACK = "STATE_ACK"; // roomId, tick
    public static final String STATE_REQUEST = "STATE_REQUEST"; // roomId, fromTick

    // Server -> Client
    public static final String ROOMS = "ROOMS"; // list=roomId,name,type,cur,max;...
    public static final String ROOM_JOINED = "ROOM_JOINED"; // roomId,hostId
    public static final String ROOM_STATE = "ROOM_STATE"; // roomId,hostId,players=id,username,ready,isHost;...
    public static final String CHAT_MSG = "CHAT"; // from=,msg=
    public static final String HOST_LEFT = "HOST_LEFT"; // roomId
    public static final String GAME_START = "GAME_START"; // roomId
    public static final String GAME_INIT = "GAME_INIT"; // roomId, seed, tick
    public static final String GAME_STATE = "GAME_STATE"; // roomId, tick, dt, payload
    public static final String GAME_EVENT = "GAME_EVENT"; // roomId, tick, type, data
    public static final String GAME_ABORT = "GAME_ABORT"; // roomId, reason
    public static final String INFO = "INFO"; // msg
    public static final String ERROR = "ERROR"; // msg
}
