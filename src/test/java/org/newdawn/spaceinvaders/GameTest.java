package org.newdawn.spaceinvaders;

import org.junit.Test;

public class GameTest {
    @Test
    public void testMain(){
        Game g = new Game();
        g.gameLoop();
    }
}
