package org.newdawn.spaceinvaders.multyplay.net.msg;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GameSnapshotMsg {
    private final String roomId;
    private final long tick;
    private final long serverTimestamp;
    private final int currentRound;
    private final List<EntityState> entities;
    private final Map<String, PlayerState> players;
    private final int lastProcessedInputSequence;
    private final String statusMessage;

    public GameSnapshotMsg(String roomId,
                           long tick,
                           long serverTimestamp,
                           int currentRound,
                           List<EntityState> entities,
                           Map<String, PlayerState> players,
                           int lastProcessedInputSequence,
                           String statusMessage) {
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.tick = tick;
        this.serverTimestamp = serverTimestamp;
        this.currentRound = currentRound;
        this.entities = entities == null ? Collections.emptyList() : Collections.unmodifiableList(entities);
        if (players == null || players.isEmpty()) {
            this.players = Collections.emptyMap();
        } else {
            this.players = Collections.unmodifiableMap(new LinkedHashMap<>(players));
        }
        this.lastProcessedInputSequence = lastProcessedInputSequence;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
    }

    public String getRoomId() {
        return roomId;
    }

    public long getTick() {
        return tick;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public List<EntityState> getEntities() {
        return entities;
    }

    public Map<String, PlayerState> getPlayers() {
        return players;
    }

    public int getLastProcessedInputSequence() {
        return lastProcessedInputSequence;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public static final class EntityState {
        private final String entityId;
        private final String type;
        private final double x;
        private final double y;
        private final double velocityX;
        private final double velocityY;
        private final int hp;
        private final int stateFlags;

        public EntityState(String entityId,
                           String type,
                           double x,
                           double y,
                           double velocityX,
                           double velocityY,
                           int hp,
                           int stateFlags) {
            this.entityId = Objects.requireNonNull(entityId, "entityId");
            this.type = Objects.requireNonNull(type, "type");
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.hp = hp;
            this.stateFlags = stateFlags;
        }

        public String getEntityId() {
            return entityId;
        }

        public String getType() {
            return type;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getVelocityX() {
            return velocityX;
        }

        public double getVelocityY() {
            return velocityY;
        }

        public int getHp() {
            return hp;
        }

        public int getStateFlags() {
            return stateFlags;
        }
    }

    public static final class PlayerState {
        private final String playerId;
        private final int hp;
        private final int maxHp;
        private final int attackPower;
        private final double attackSpeed;
        private final int skillPoints;

        public PlayerState(String playerId,
                           int hp,
                           int maxHp,
                           int attackPower,
                           double attackSpeed,
                           int skillPoints) {
            this.playerId = Objects.requireNonNull(playerId, "playerId");
            this.hp = hp;
            this.maxHp = maxHp;
            this.attackPower = attackPower;
            this.attackSpeed = attackSpeed;
            this.skillPoints = skillPoints;
        }

        public String getPlayerId() {
            return playerId;
        }

        public int getHp() {
            return hp;
        }

        public int getMaxHp() {
            return maxHp;
        }

        public int getAttackPower() {
            return attackPower;
        }

        public double getAttackSpeed() {
            return attackSpeed;
        }

        public int getSkillPoints() {
            return skillPoints;
        }
    }
}
