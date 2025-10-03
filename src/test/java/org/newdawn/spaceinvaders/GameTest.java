package org.newdawn.spaceinvaders;

import org.junit.Test;
import org.newdawn.spaceinvaders.gameplay.Game;

public class GameTest {
    @Test
    public void testMain(){
        Game g = new Game();
        g.gameLoop();
    }
}
