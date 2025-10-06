package org.newdawn.spaceinvaders.multyplay.net;

/**
 * 폭발/피해/라운드전환 등 이벤트 브로드캐스트용 (후속 채움)
 */
public class GameEventMsg {
    public String type; // e.g. HIT, ROUND, BOSS
    public String data; // 간단 키=값 or json-like string
}
