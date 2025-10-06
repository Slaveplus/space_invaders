package org.newdawn.spaceinvaders.multyplay.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.newdawn.spaceinvaders.multyplay.net.MultiNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameEventMsg;
import org.newdawn.spaceinvaders.multyplay.net.msg.GameSnapshotMsg;
import org.newdawn.spaceinvaders.multyplay.system.MultiInputManager;

/**
 * Owns the multiplayer gameplay lifecycle on the client.
 * Handles input polling, snapshot consumption, and render invalidation hooks.
 */
public class MultiGameRuntime {
    private final MultiGameController controller;
    private final MultiNetworkAdapter networkAdapter;
    private final MultiInputManager inputManager;
    private final ExecutorService runtimeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "multi-runtime");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Runnable repaintCallback = () -> {};
    private volatile SessionEndReason sessionEndReason;
    private Consumer<SessionEndReason> sessionEndHandler = reason -> {};

    public MultiGameRuntime(MultiGameController controller,
                            MultiNetworkAdapter networkAdapter,
                            MultiInputManager inputManager) {
        this.controller = controller;
        this.networkAdapter = networkAdapter;
        this.inputManager = inputManager;
    }

    /** Starts the background update loop. */
    public void start() {
        if (running.compareAndSet(false, true)) {
            sessionEndReason = null;
            runtimeExecutor.execute(this::runLoop);
        }
    }

    /** Stops the runtime and releases resources. */
    public void stop() {
        running.set(false);
        networkAdapter.shutdown();
        inputManager.shutdown();
        runtimeExecutor.shutdownNow();
    }

    /** Hook to allow the owning Canvas to request repaints. */
    public void setRepaintCallback(Runnable repaintCallback) {
        if (repaintCallback != null) {
            this.repaintCallback = repaintCallback;
        }
    }

    public MultiGameController getController() {
        return controller;
    }

    public MultiInputManager getInputManager() {
        return inputManager;
    }

    public void setSessionEndHandler(Consumer<SessionEndReason> handler) {
        if (handler != null) {
            this.sessionEndHandler = handler;
        }
    }

    private void runLoop() {
        long lastTick = System.currentTimeMillis();
        while (running.get()) {
            long now = System.currentTimeMillis();
            long delta = now - lastTick;
            lastTick = now;

            inputManager.pollAndSend(networkAdapter);
            networkAdapter.tick(now);

            boolean connected = networkAdapter.isConnected();
            controller.getGameState().setConnectionAlive(connected);

            GameEventMsg event;
            while ((event = networkAdapter.pollEvent()) != null) {
                handleEvent(event);
                if (!running.get()) {
                    break;
                }
            }
            if (!running.get()) {
                break;
            }

            if (!connected) {
                controller.getGameState().setStatusMessage("서버와의 연결이 끊어졌습니다.");
                controller.getGameState().setConnectionAlive(false);
                signalSessionEnd(SessionEndReason.DISCONNECTED);
                break;
            }

            GameSnapshotMsg snapshot = networkAdapter.pollSnapshot();
            if (snapshot != null) {
                controller.applySnapshot(snapshot);
            }

            controller.update(delta);
            repaintCallback.run();

            try {
                Thread.sleep(16); // target ~60Hz client loop
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        networkAdapter.shutdown();
        inputManager.shutdown();

        SessionEndReason reason = sessionEndReason;
        if (reason != null) {
            sessionEndHandler.accept(reason);
        }
    }

    private void handleEvent(GameEventMsg event) {
        if (event == null) {
            return;
        }
        String type = event.getType();
        if ("HOST_LEFT".equalsIgnoreCase(type) || "LEFT_ROOM".equalsIgnoreCase(type)) {
            controller.getGameState().setStatusMessage("호스트가 게임을 종료했습니다.");
            controller.getGameState().setConnectionAlive(false);
            signalSessionEnd(SessionEndReason.HOST_LEFT);
        } else if ("ERROR".equalsIgnoreCase(type)) {
            controller.getGameState().setStatusMessage(event.getPayload());
            controller.getGameState().setConnectionAlive(false);
            signalSessionEnd(SessionEndReason.ERROR);
        }
    }

    private void signalSessionEnd(SessionEndReason reason) {
        if (sessionEndReason == null) {
            sessionEndReason = reason;
        }
        running.set(false);
    }

    public enum SessionEndReason {
        HOST_LEFT,
        DISCONNECTED,
        ERROR
    }
}
