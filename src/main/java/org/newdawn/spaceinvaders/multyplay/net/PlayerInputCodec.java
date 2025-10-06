package org.newdawn.spaceinvaders.multyplay.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * Binary codec for PlayerInputMsg payloads.
 */
public final class PlayerInputCodec {
    private PlayerInputCodec() {}

    public static byte[] encode(PlayerInputMsg msg) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF(msg.getPlayerId());
            out.writeInt(msg.getSequence());
            List<Integer> keys = new ArrayList<>(msg.getPressedKeys());
            Collections.sort(keys);
            out.writeInt(keys.size());
            for (Integer key : keys) {
                out.writeInt(key);
            }
            out.writeInt(msg.getCursorX());
            out.writeInt(msg.getCursorY());
            out.writeBoolean(msg.isFiring());
        }
        return baos.toByteArray();
    }

    public static PlayerInputMsg decode(byte[] payload) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            String playerId = in.readUTF();
            int sequence = in.readInt();
            int keyCount = in.readInt();
            Set<Integer> keys = new HashSet<>(keyCount);
            for (int i = 0; i < keyCount; i++) {
                keys.add(in.readInt());
            }
            int cursorX = in.readInt();
            int cursorY = in.readInt();
            boolean firing = in.readBoolean();
            return new PlayerInputMsg(playerId, sequence, keys, cursorX, cursorY, firing);
        }
    }
}
