package org.newdawn.spaceinvaders.multyplay.system;

import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.newdawn.spaceinvaders.multyplay.net.MultiNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.msg.PlayerInputMsg;

/**
 * Collects local input and ships it to the network layer.
 */
public class MultiInputManager extends KeyAdapter implements MouseListener, MouseMotionListener {
    private final String playerId;
    private final Set<Integer> pressedKeys = Collections.synchronizedSet(new HashSet<>());
    private final AtomicInteger inputSequence = new AtomicInteger();
    private volatile Point cursor = new Point();
    private volatile boolean firing;

    public MultiInputManager(String playerId) {
        this.playerId = playerId;
    }

    public void pollAndSend(MultiNetworkAdapter adapter) {
        if (adapter == null) return;
        PlayerInputMsg msg = new PlayerInputMsg(
                playerId,
                inputSequence.incrementAndGet(),
                new HashSet<>(pressedKeys),
                cursor.x,
                cursor.y,
                firing
        );
        adapter.sendInput(msg);
    }

    public void shutdown() {
        pressedKeys.clear();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            firing = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            firing = false;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        firing = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        firing = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // no-op
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        cursor = e.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        cursor = e.getPoint();
    }
}
