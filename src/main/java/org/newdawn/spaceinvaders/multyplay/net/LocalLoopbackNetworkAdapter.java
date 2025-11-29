package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.state.MultiplayerGameStateManager;
import org.newdawn.spaceinvaders.multyplay.state.PlayerState;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.EntitySnapshot;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 싱글 플레이/테스트용 로컬 루프백 어댑터.
 * - 네트워크 없이 MultiplayerGameStateManager 데이터를 즉시 스냅샷화하여 반환.
 */
public class LocalLoopbackNetworkAdapter implements GameNetworkAdapter {
    private final MultiplayerGameStateManager gsm;
    private final Queue<PlayerInput> inputs = new ConcurrentLinkedQueue<>();
    private volatile GameSnapshot latest;
    private final Queue<GameEvent> events = new ConcurrentLinkedQueue<>();
    private long tickCounter;
    private long lastTickTime;
    private long lastReceivedTick;

    public LocalLoopbackNetworkAdapter(MultiplayerGameStateManager gsm) {
        this.gsm = gsm;
    }

    @Override
    public Mode getMode() { return Mode.LOCAL; }

    @Override
    public void sendInput(PlayerInput input) { inputs.add(input); }

    @Override
    public GameSnapshot pollLatestSnapshot() { return latest; }

    @Override
    public void tick(long nowMillis) {
        // 입력 소비 (현재는 별도 처리 없음 - Game의 MultiplayerInputManager가 직접 상태 반영)
        inputs.clear();
        long delta = lastTickTime == 0 ? 16 : (nowMillis - lastTickTime);
        lastTickTime = nowMillis;
        tickCounter++;
        // 엔티티 스냅샷 구성
        List<EntitySnapshot> entitySnaps = gsm.getEntities().stream().map(Entity::toSnapshot).collect(Collectors.toList());
        Map<String, GameSnapshot.PlayerScalarState> pmap = new LinkedHashMap<>();
        for (PlayerState ps : gsm.getPlayerStates()) {
            pmap.put(ps.getPlayerId(), new GameSnapshot.PlayerScalarState(
                    ps.getCurrentHP(),
                    ps.getMaxHP(),
                    ps.getAttackPower(),
                    ps.getAttackSpeed(),
                    ps.getSkillPoints(),
                    0, 0, 0, 0,
                    ps.getEarnedCoins(),
                    0L, 0L, 0L,
                    0, 0, 0));
        }
        latest = new GameSnapshot(tickCounter, nowMillis, delta, gsm.getCurrentRound(), entitySnaps, pmap,
                GameSnapshot.Phase.ACTIVE, false, Collections.emptyMap(), null);
        lastReceivedTick = tickCounter;
    }

    @Override
    public void shutdown() { inputs.clear(); events.clear(); }

    @Override
    public void sendEvent(GameEvent event) { events.add(event); }

    @Override
    public List<GameEvent> drainEvents() { List<GameEvent> list = new ArrayList<>(); while(!events.isEmpty()) list.add(events.poll()); return list; }

    @Override
    public long getLastReceivedTick() { return lastReceivedTick; }
}
