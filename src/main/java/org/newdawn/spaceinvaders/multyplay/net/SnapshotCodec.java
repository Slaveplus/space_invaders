package org.newdawn.spaceinvaders.multyplay.net;

import org.newdawn.spaceinvaders.multyplay.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 스냅샷 텍스트 직렬화/역직렬화 (파이프 구분 단순 포맷)
 * 포맷:
 *   헤더:  SNAP|<tick>|<entityCount>|<playerCount>
 *   엔티티: E|<id>|<sprite>|<x>|<y>|<dx>|<dy>
 *   플레이어: P|<slot>|<x>|<y>|<hp>|<sp>
 */
public class SnapshotCodec {

    public static String encode(GameSnapshotMsg snapshot, List<EntitySnapshot> entities){
        StringBuilder sb = new StringBuilder();
        sb.append("SNAP|").append(snapshot.tick).append('|')
          .append(entities.size()).append('|')
          .append(snapshot.players.size()).append('\n');
        for(EntitySnapshot es : entities){
            sb.append("E|").append(es.id).append('|')
              .append(safe(es.sprite)).append('|')
              .append(format(es.x)).append('|')
              .append(format(es.y)).append('|')
              .append(format(es.dx)).append('|')
              .append(format(es.dy)).append('\n');
        }
        for(GameSnapshotMsg.PlayerStateSnapshot ps : snapshot.players){
            sb.append("P|").append(ps.slot).append('|')
              .append(format(ps.x)).append('|')
              .append(format(ps.y)).append('|')
              .append(ps.hp).append('|')
              .append(ps.sp).append('\n');
        }
        return sb.toString();
    }

    public static DecodedSnapshot decode(String text){
        String[] lines = text.split("\\n");
        if(lines.length == 0) return null;
        String header = lines[0];
        String[] h = header.split("\\|");
        if(h.length < 4 || !"SNAP".equals(h[0])) return null;
        DecodedSnapshot out = new DecodedSnapshot();
        out.tick = Long.parseLong(h[1]);
        int entityCount = Integer.parseInt(h[2]);
        int playerCount = Integer.parseInt(h[3]);
        int idx = 1;
        // Entities
        for(int i=0;i<entityCount && idx < lines.length;i++, idx++){
            String[] p = lines[idx].split("\\|");
            if(p.length < 7 || !"E".equals(p[0])) continue;
            EntitySnapshot es = new EntitySnapshot(
                Long.parseLong(p[1]),
                null,
                p[2],
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                Double.parseDouble(p[6]),
                0,0
            );
            out.entities.add(es);
        }
        // Players
        for(int i=0;i<playerCount && idx < lines.length;i++, idx++){
            String[] p = lines[idx].split("\\|");
            if(p.length < 6 || !"P".equals(p[0])) continue;
            GameSnapshotMsg.PlayerStateSnapshot ps = new GameSnapshotMsg.PlayerStateSnapshot();
            ps.slot = Integer.parseInt(p[1]);
            ps.x = (float) Double.parseDouble(p[2]);
            ps.y = (float) Double.parseDouble(p[3]);
            ps.hp = Integer.parseInt(p[4]);
            ps.sp = Integer.parseInt(p[5]);
            out.players.add(ps);
        }
        return out;
    }

    private static String format(double v){ return String.format("%.2f", v); }
    private static String safe(String s){ return s == null ? "" : s.replace('|','/'); }

    public static class DecodedSnapshot {
        public long tick;
        public final List<EntitySnapshot> entities = new ArrayList<>();
        public final List<GameSnapshotMsg.PlayerStateSnapshot> players = new ArrayList<>();
    }
}
