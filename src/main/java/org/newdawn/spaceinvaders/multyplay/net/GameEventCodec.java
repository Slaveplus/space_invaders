package org.newdawn.spaceinvaders.multyplay.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.newdawn.spaceinvaders.multyplay.net.msg.GameEventMsg;

/**
 * Binary codec for GameEventMsg payloads.
 */
public final class GameEventCodec {
    private GameEventCodec() {}

    public static byte[] encode(GameEventMsg event) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF(event.getType());
            out.writeUTF(event.getPayload());
            out.writeLong(event.getServerTimestamp());
        }
        return baos.toByteArray();
    }

    public static GameEventMsg decode(byte[] payload) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            String type = in.readUTF();
            String data = in.readUTF();
            long ts = in.readLong();
            return new GameEventMsg(type, data, ts);
        }
    }
}
