package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GomokuStateTest {

    @Test
    public void testInitialStateIsEmpty() {
        GomokuState state = new GomokuState();
        int[][] board = state.getBoard();
        for (int i = 0; i < GomokuState.SIZE; i++) {
            for (int j = 0; j < GomokuState.SIZE; j++) {
                assertEquals("Board should be empty at start", 0, board[i][j]);
            }
        }
    }

    @Test
    public void testPlayerAlternation() {
        GomokuState state = new GomokuState();
        state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, 0, 0));
        assertEquals(Gomoku.PLAYER_TWO, state.player());
        state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_TWO, 1, 1));
        assertEquals(Gomoku.PLAYER_ONE, state.player());
    }

    @Test
    public void testHorizontalWin() {
        GomokuState state = new GomokuState();
        for (int i = 0; i < 5; i++) {
            state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, 0, i));
            if (i < 4) state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_TWO, i + 1, i));
        }
        assertEquals(Optional.of(Gomoku.PLAYER_ONE), state.winner());
    }

    @Test
    public void testVerticalWin() {
        GomokuState state = new GomokuState();
        for (int i = 0; i < 5; i++) {
            state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, i, 0));
            if (i < 4) state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_TWO, i, i + 1));
        }
        assertEquals(Optional.of(Gomoku.PLAYER_ONE), state.winner());
    }

    @Test
    public void testDiagonalWin() {
        GomokuState state = new GomokuState();
        for (int i = 0; i < 5; i++) {
            state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, i, i));
            if (i < 4) state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_TWO, i, GomokuState.SIZE - i - 1));
        }
        assertEquals(Optional.of(Gomoku.PLAYER_ONE), state.winner());
    }

    @Test
public void testAntiDiagonalWin() {
    GomokuState state = new GomokuState();
    for (int i = 0; i < 5; i++) {
        state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, i, 4 - i));
        if (i < 4)
            state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_TWO, i, i + 6)); // 避免干扰
    }
    assertEquals(Optional.of(Gomoku.PLAYER_ONE), state.winner());
}

    @Test
    public void testToStringVisual() {
        GomokuState state = new GomokuState();
        state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, 7, 7));
        String boardStr = state.toString();
        assertTrue(boardStr.contains("X"));  // X for PLAYER_ONE
    }

    @Test
public void testIsTerminalTrueOnWin() {
    GomokuState state = new GomokuState();
    for (int i = 0; i < 5; i++) {
        state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, 2, i));
        if (i < 4) {
            state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_TWO, 3, i));
        }
    }
    assertTrue(state.isTerminal());  
    assertEquals(Optional.of(Gomoku.PLAYER_ONE), state.winner());
}

    @Test
    public void testIsTerminalFalseMidGame() {
        GomokuState state = new GomokuState();
        state = (GomokuState) state.next(new GomokuMove(Gomoku.PLAYER_ONE, 0, 0));
        assertFalse(state.isTerminal());
    }
}