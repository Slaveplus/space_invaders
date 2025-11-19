package org.newdawn.spaceinvaders.multyplay.entity.attack;

import java.util.LinkedHashMap;
import java.util.Map;

import org.newdawn.spaceinvaders.multyplay.core.MultiplayerGameContext;
import org.newdawn.spaceinvaders.multyplay.net.protocol.MetadataCodec;

/**
 * 멀티플레이 전용 메타데이터 동기화를 추가한 자기장 엔티티.
 */
public class MagneticFieldEntity extends org.newdawn.spaceinvaders.common.entity.MagneticFieldEntity {
    public MagneticFieldEntity(MultiplayerGameContext game, int x, int y, double radius, double strength, long duration) {
        super(game, x, y, radius, strength, duration);
    }

    @Override
    protected String snapshotMetadata() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("radius", Double.toString(fieldRadius));
        meta.put("strength", Double.toString(fieldStrength));
        return MetadataCodec.encode(meta);
    }

    @Override
    protected void applySnapshotMetadata(String metadata) {
        Map<String, String> meta = MetadataCodec.decode(metadata);
        if (meta.isEmpty()) {
            return;
        }
        try {
            fieldRadius = Double.parseDouble(meta.getOrDefault("radius", Double.toString(fieldRadius)));
        } catch (NumberFormatException ignore) {
        }
        try {
            fieldStrength = Double.parseDouble(meta.getOrDefault("strength", Double.toString(fieldStrength)));
        } catch (NumberFormatException ignore) {
        }
    }
}
