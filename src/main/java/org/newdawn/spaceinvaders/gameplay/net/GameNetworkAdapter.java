package org.newdawn.spaceinvaders.gameplay.net;

import org.newdawn.spaceinvaders.gameplay.entity.EntitySnapshot;
import java.util.List;

/**
 * 게임 로직과 실제 전송 계층 사이의 추상화.
 * - 클라이언트: 입력 전송 및 스냅샷 수신
 * - 서버: 입력 수신 및 authoritative 스냅샷 브로드캐스트
 */
public interface GameNetworkAdapter {
    enum Mode { SERVER, CLIENT, LOCAL }
    Mode getMode();

    /** 로컬 플레이어 입력 큐 전송 */
    void sendInput(PlayerInput input);

    /** 최근 수신한 authoritative 스냅샷 */
    GameSnapshot pollLatestSnapshot();

    /** 주기적 틱(서버 authoritative 계산 수행) */
    void tick(long nowMillis);

    /** 연결 종료/정리 */
    void shutdown();

    /** 간단 브로드캐스트 이벤트 (채팅/시스템 등) */
    void sendEvent(GameEvent event);

    /** 누적된 이벤트 수신 */
    List<GameEvent> drainEvents();
}
