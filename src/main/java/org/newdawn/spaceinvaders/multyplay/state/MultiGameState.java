package org.newdawn.spaceinvaders.multyplay.state;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.newdawn.spaceinvaders.multyplay.entity.Entity;
import org.newdawn.spaceinvaders.multyplay.entity.EntityFactory;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;

/**
 * Client-side mutable state built from server snapshots.
 */
public class MultiGameState {
    private final Map<String, Entity> entities = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Entity> entityRenderOrder = new CopyOnWriteArrayList<>();
    private final Map<String, PlayerRuntimeState> players = new ConcurrentHashMap<>();

    private volatile long lastTick;
    private volatile int currentRound = 1;
    private volatile String statusMessage = "";
    private volatile boolean connectionAlive = true;

    public void ingestSnapshot(GameSnapshotMsg snapshot) {
        if (snapshot == null) {
            return;
        }
        lastTick = snapshot.getTick();
        currentRound = snapshot.getCurrentRound();
        statusMessage = snapshot.getStatusMessage();
        connectionAlive = true;

        snapshot.getPlayers().forEach((id, p) ->
                players.computeIfAbsent(id, PlayerRuntimeState::new).updateFromSnapshot(p)
        );
        players.keySet().removeIf(id -> !snapshot.getPlayers().containsKey(id));

        HashSet<String> seen = new HashSet<>();
        snapshot.getEntities().forEach(entityState -> {
            Entity entity = entities.computeIfAbsent(entityState.getEntityId(), id -> {
                Entity created = EntityFactory.create(id, entityState.getType());
                entityRenderOrder.add(created);
                return created;
            });
            seen.add(entity.getId());
            entity.apply(entityState);
        });

        entities.keySet().removeIf(id -> !seen.contains(id));
        entityRenderOrder.removeIf(entity -> !seen.contains(entity.getId()));
    }

    public void reset() {
        entities.clear();
        entityRenderOrder.clear();
        players.clear();
        statusMessage = "";
        lastTick = 0L;
        currentRound = 1;
        connectionAlive = true;
    }

    public void advanceTime(long deltaMillis) {
        // Placeholder for time-based systems (cooldowns, effects).
    }

    public Collection<Entity> getEntities() {
        return entityRenderOrder;
    }

    public Collection<PlayerRuntimeState> getPlayerStates() {
        return players.values();
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public long getLastTick() {
        return lastTick;
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message == null ? "" : message;
    }

    public boolean isConnectionAlive() {
        return connectionAlive;
    }

    public void setConnectionAlive(boolean connectionAlive) {
        this.connectionAlive = connectionAlive;
    }

    public void renderEntities(Graphics2D g) {
        for (Entity entity : entityRenderOrder) {
            entity.render(g);
        }
    }
}
