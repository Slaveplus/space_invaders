package org.newdawn.spaceinvaders.multyplay.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;

/**
 * Binary serializer for GameSnapshotMsg payloads.
 */
public final class SnapshotCodec {
    private SnapshotCodec() {}

    public static byte[] encode(GameSnapshotMsg snapshot) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF(snapshot.getRoomId());
            out.writeLong(snapshot.getTick());
            out.writeLong(snapshot.getServerTimestamp());
            out.writeInt(snapshot.getCurrentRound());
            out.writeInt(snapshot.getLastProcessedInputSequence());
            out.writeUTF(snapshot.getStatusMessage());

            List<GameSnapshotMsg.EntityState> entities = snapshot.getEntities();
            out.writeInt(entities.size());
            for (GameSnapshotMsg.EntityState entity : entities) {
                out.writeUTF(entity.getEntityId());
                out.writeUTF(entity.getType());
                out.writeDouble(entity.getX());
                out.writeDouble(entity.getY());
                out.writeDouble(entity.getVelocityX());
                out.writeDouble(entity.getVelocityY());
                out.writeInt(entity.getHp());
                out.writeInt(entity.getStateFlags());
            }

            Map<String, GameSnapshotMsg.PlayerState> players = snapshot.getPlayers();
            out.writeInt(players.size());
            for (Map.Entry<String, GameSnapshotMsg.PlayerState> entry : players.entrySet()) {
                GameSnapshotMsg.PlayerState ps = entry.getValue();
                out.writeUTF(entry.getKey());
                out.writeInt(ps.getHp());
                out.writeInt(ps.getMaxHp());
                out.writeInt(ps.getAttackPower());
                out.writeDouble(ps.getAttackSpeed());
                out.writeInt(ps.getSkillPoints());
            }
        }
        return baos.toByteArray();
    }

    public static GameSnapshotMsg decode(byte[] payload) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            String roomId = in.readUTF();
            long tick = in.readLong();
            long serverTimestamp = in.readLong();
            int currentRound = in.readInt();
            int lastSeq = in.readInt();
            String statusMessage = in.readUTF();

            int entityCount = in.readInt();
            List<GameSnapshotMsg.EntityState> entities = new ArrayList<>(entityCount);
            for (int i = 0; i < entityCount; i++) {
                entities.add(new GameSnapshotMsg.EntityState(
                        in.readUTF(),
                        in.readUTF(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readInt(),
                        in.readInt()
                ));
            }

            int playerCount = in.readInt();
            Map<String, GameSnapshotMsg.PlayerState> players = new HashMap<>(playerCount);
            for (int i = 0; i < playerCount; i++) {
                String id = in.readUTF();
                players.put(id, new GameSnapshotMsg.PlayerState(
                        id,
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readDouble(),
                        in.readInt()
                ));
            }

            return new GameSnapshotMsg(
                    roomId,
                    tick,
                    serverTimestamp,
                    currentRound,
                    entities,
                    players,
                    lastSeq,
                    statusMessage
            );
        }
    }
}
