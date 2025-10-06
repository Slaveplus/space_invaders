package org.newdawn.spaceinvaders.multyplay.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.SpaceInvadersApp;
import org.newdawn.spaceinvaders.multyplay.state.MultiGameState;
import org.newdawn.spaceinvaders.multyplay.state.PlayerRuntimeState;

/**
 * Responsible for HUD drawing during multiplayer sessions.
 */
public class MultiUIRenderer {
    private final Font titleFont = new Font("Arial", Font.BOLD, 24);
    private final Font bodyFont = new Font("Arial", Font.PLAIN, 16);

    public void render(Graphics2D g, MultiGameState gameState) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        String round = "ROUND " + gameState.getCurrentRound();
        g.drawString(round, 20, 40);

        g.setFont(bodyFont);
        FontMetrics bodyMetrics = g.getFontMetrics();
        int y = 70;
        for (PlayerRuntimeState ps : gameState.getPlayerStates()) {
            g.drawString(ps.summaryLine(), 20, y);
            y += bodyMetrics.getHeight();
        }

        String status = gameState.getStatusMessage();
        if (!status.isEmpty()) {
            g.setColor(Color.YELLOW);
            g.drawString(status, 20, y + 10);
            g.setColor(Color.WHITE);
        }

        boolean connectionAlive = gameState.isConnectionAlive();
        g.setFont(bodyFont);
        FontMetrics connMetrics = g.getFontMetrics();
        String connLabel = connectionAlive ? "CONNECTION: ONLINE" : "CONNECTION: OFFLINE";
        int canvasWidth = g.getClipBounds() != null ? g.getClipBounds().width : SpaceInvadersApp.DEFAULT_WIDTH;
        int connWidth = connMetrics.stringWidth(connLabel);
        g.setColor(connectionAlive ? Color.GREEN : Color.RED);
        g.drawString(connLabel, canvasWidth - connWidth - 20, 40);
        g.setColor(Color.WHITE);
    }
}
