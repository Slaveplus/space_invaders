package org.newdawn.spaceinvaders.gameplay.render;

import org.newdawn.spaceinvaders.gameplay.BackgroundRenderer;
import org.newdawn.spaceinvaders.gameplay.UIRenderer;
import org.newdawn.spaceinvaders.gameplay.entity.Entity;
import org.newdawn.spaceinvaders.gameplay.sprite.Sprite;
import org.newdawn.spaceinvaders.gameplay.sprite.SpriteStore;
import org.newdawn.spaceinvaders.net.Snapshot;

import java.awt.*;
import java.util.ArrayList;

/**
 * 싱글/멀티 공통 렌더 파사드.
 * - 엔티티 리스트를 직접 그리는 경로(renderEntities)
 * - 서버 스냅샷을 그리는 경로(renderSnapshot)
 * 두 경로 모두 동일한 비주얼(배경/스프라이트/총알)을 사용합니다.
 */
public class CommonRenderer {
    private final BackgroundRenderer background;

    // 캐시된 스프라이트
    private final Sprite shipSprite;
    private final Sprite shotSprite;
    private final Sprite[] alienFrames = new Sprite[4];

    // 간단한 에일리언 애니메이션 상태(스냅샷 모드에서만 사용)
    private long lastAlienFrameChange = 0L;
    private int alienFrameIndex = 0;

    public CommonRenderer() {
        this.background = new BackgroundRenderer("sprites/backgrounds/Background-2.jpg");
        SpriteStore store = SpriteStore.get();
        this.shipSprite = store.getSprite("sprites/ship.gif");
        this.shotSprite = store.getSprite("sprites/shot.gif");
        alienFrames[0] = store.getSprite("sprites/alien.gif");
        alienFrames[1] = store.getSprite("sprites/alien2.gif");
        alienFrames[2] = store.getSprite("sprites/alien.gif");
        alienFrames[3] = store.getSprite("sprites/alien3.gif");
    }

    public void renderEntities(Graphics2D g, ArrayList<Entity> entities, UIRenderer overlay, Runnable overlayDrawer) {
        background.draw(g);
        for (Entity e : entities) e.draw(g);
        if (overlay != null && overlayDrawer != null) overlayDrawer.run();
    }

    public void renderSnapshot(Graphics2D g, Snapshot snap, Runnable overlayDrawer) {
        if (snap == null) {
            background.draw(g);
            g.setColor(Color.white);
            g.drawString("서버 스냅샷 대기 중...", 40, 60);
            return;
        }
        background.draw(g);
        long now = System.currentTimeMillis();
        if (now - lastAlienFrameChange > 250) {
            lastAlienFrameChange = now;
            alienFrameIndex = (alienFrameIndex + 1) % alienFrames.length;
        }
        // players
        for (Snapshot.PlayerState ps : snap.players) {
            int x = Math.round(ps.x), y = Math.round(ps.y);
            int sw = shipSprite.getWidth(), sh = shipSprite.getHeight();
            shipSprite.draw(g, x - sw/2, y - sh/2);
            g.setColor(Color.WHITE);
            g.drawString("HP:"+ps.hp+" S:"+ps.score, x - 20, y - (sh/2) - 6);
        }
        // aliens
        for (Snapshot.EntityState a : snap.aliens) {
            int x = Math.round(a.x), y = Math.round(a.y);
            Sprite alien = alienFrames[alienFrameIndex];
            int aw = alien.getWidth(), ah = alien.getHeight();
            alien.draw(g, x - aw/2, y - ah/2);
        }
        // bullets
        for (Snapshot.EntityState b : snap.bullets) {
            int x = Math.round(b.x), y = Math.round(b.y);
            if ("bullet_player".equals(b.type)) {
                int ww = shotSprite.getWidth(), hh = shotSprite.getHeight();
                shotSprite.draw(g, x - ww/2, y - hh/2);
            } else {
                BulletRenderer.drawAlienBullet(g, x, y);
            }
        }
        if (overlayDrawer != null) overlayDrawer.run();
    }
}
