package org.newdawn.spaceinvaders.multyplay.core;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.multyplay.net.LocalLoopbackMultiAdapter;
import org.newdawn.spaceinvaders.multyplay.net.MultiNetworkAdapter;
import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;
import org.newdawn.spaceinvaders.multyplay.system.MultiInputManager;
import org.newdawn.spaceinvaders.multyplay.ui.MultiBackgroundRenderer;
import org.newdawn.spaceinvaders.multyplay.ui.MultiUIRenderer;

/**
 * Utility harness to spin up a multiplayer runtime backed by the local loopback adapter.
 * Useful for smoke tests and manual play without a server.
 */
public final class MultiLoopbackHarness {
    private final MultiGameState gameState;
    private final MultiGameController controller;
    private final MultiNetworkAdapter networkAdapter;
    private final MultiInputManager inputManager;
    private final MultiGameRuntime runtime;
    private final MultiGameCanvas canvas;

    public MultiLoopbackHarness() {
        this("loopback-player");
    }

    public MultiLoopbackHarness(String playerId) {
        this.gameState = new MultiGameState();
        this.controller = new MultiGameController(gameState);
        this.networkAdapter = new LocalLoopbackMultiAdapter();
        this.inputManager = new MultiInputManager(playerId == null ? "loopback" : playerId);
        this.runtime = new MultiGameRuntime(controller, networkAdapter, inputManager);
        MultiBackgroundRenderer background = new MultiBackgroundRenderer("sprites/backgrounds/Background-2.jpg");
        MultiUIRenderer uiRenderer = new MultiUIRenderer();
        this.canvas = new MultiGameCanvas(runtime, controller, background, uiRenderer);
        this.canvas.setSize(SpaceInvadersApp.DEFAULT_WIDTH, SpaceInvadersApp.DEFAULT_HEIGHT);
    }

    public MultiGameCanvas getCanvas() {
        return canvas;
    }

    public MultiGameRuntime getRuntime() {
        return runtime;
    }

    public MultiNetworkAdapter getNetworkAdapter() {
        return networkAdapter;
    }

    public MultiInputManager getInputManager() {
        return inputManager;
    }

    public MultiGameController getController() {
        return controller;
    }

    public MultiGameState getGameState() {
        return gameState;
    }
}
