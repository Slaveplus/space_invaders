package org.newdawn.spaceinvaders.gameplay.net;

import org.newdawn.spaceinvaders.gameplay.GameStateManager;
import org.newdawn.spaceinvaders.gameplay.PlayerState;
import org.newdawn.spaceinvaders.common.entity.Entity;
import org.newdawn.spaceinvaders.common.entity.EntitySnapshot;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 싱글 플레이/테스트용 로컬 루프백 어댑터.
 * - 네트워크 없이 GameStateManager 데이터를 즉시 스냅샷화하여 반환.
 */
public class LocalLoopbackNetworkAdapter implements GameNetworkAdapter {
    private final GameStateManager gsm;
    private final Queue<PlayerInput> inputs = new ConcurrentLinkedQueue<>();
    private volatile GameSnapshot latest;
    private final Queue<GameEvent> events = new ConcurrentLinkedQueue<>();

    public LocalLoopbackNetworkAdapter(GameStateManager gsm) {
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
        // 입력 소비 (현재는 별도 처리 없음 - Game의 InputManager가 직접 상태 반영)
        inputs.clear();
        // 엔티티 스냅샷 구성
        List<EntitySnapshot> entitySnaps = gsm.getEntities().stream().map(Entity::toSnapshot).collect(Collectors.toList());
        Map<String, GameSnapshot.PlayerScalarState> pmap = new LinkedHashMap<>();
        for (PlayerState ps : gsm.getPlayerStates()) {
            pmap.put(ps.getPlayerId(), new GameSnapshot.PlayerScalarState(
                    ps.getCurrentHP(),
                    ps.getMaxHP(),
                    ps.getAttackPower(),
                    ps.getAttackSpeed(),
                    ps.getSkillPoints()));
        }
        latest = new GameSnapshot(nowMillis, gsm.getCurrentRound(), entitySnaps, pmap);
    }

    @Override
    public void shutdown() { inputs.clear(); events.clear(); }

    @Override
    public void sendEvent(GameEvent event) { events.add(event); }

    @Override
    public List<GameEvent> drainEvents() { List<GameEvent> list = new ArrayList<>(); while(!events.isEmpty()) list.add(events.poll()); return list; }
}
