package org.newdawn.spaceinvaders.multyplay.core;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.app.Screen;
import org.newdawn.spaceinvaders.multyplay.ui.MultiBackgroundRenderer;
import org.newdawn.spaceinvaders.multyplay.ui.MultiUIRenderer;

/**
 * Swing Canvas that hosts the multiplayer experience.
 */
public class MultiGameCanvas extends Canvas implements Screen {
    private final MultiGameRuntime runtime;
    private final MultiGameController controller;
    private final MultiBackgroundRenderer backgroundRenderer;
    private final MultiUIRenderer uiRenderer;

    public MultiGameCanvas(MultiGameRuntime runtime,
                           MultiGameController controller,
                           MultiBackgroundRenderer backgroundRenderer,
                           MultiUIRenderer uiRenderer) {
        this.runtime = runtime;
        this.controller = controller;
        this.backgroundRenderer = backgroundRenderer;
        this.uiRenderer = uiRenderer;
        setIgnoreRepaint(true);
        setBackground(Color.BLACK);
        setSize(SpaceInvadersApp.DEFAULT_WIDTH, SpaceInvadersApp.DEFAULT_HEIGHT);
    }

    @Override
    public void init() {
        runtime.setRepaintCallback(this::repaint);
    }

    @Override
    public void onShow() {
        runtime.start();
        addKeyListener(runtime.getInputManager());
        addMouseListener(runtime.getInputManager());
        requestFocusInWindow();
    }

    @Override
    public void onHide() {
        removeKeyListener(runtime.getInputManager());
        removeMouseListener(runtime.getInputManager());
        runtime.stop();
    }

    @Override
    public void update(long deltaMillis) {
        // runtime handles updates internally
    }

    @Override
    public void render(Graphics2D g) {
        backgroundRenderer.render(g, getWidth(), getHeight());
        controller.getGameState().renderEntities(g);
        uiRenderer.render(g, controller.getGameState());
    }
}
