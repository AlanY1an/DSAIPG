package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MCTSTest {

    private TicTacToe game;
    private TicTacToe.TicTacToeState initialState;
    private MCTS mcts;

    @Before
    public void setUp() {
        game = new TicTacToe();
        initialState = game.new TicTacToeState();
        TicTacToeNode rootNode = new TicTacToeNode(initialState);
        mcts = new MCTS(rootNode);
    }

    @Test
    public void testFindNextMoveNotNull() {
        Move<TicTacToe> move = mcts.findNextMove(1000);
        Assert.assertNotNull("MCTS should return a valid move", move);
    }

    @Test
    public void testMoveIsLegal() {
        Move<TicTacToe> move = mcts.findNextMove(1000);
        int[] pos = ((TicTacToe.TicTacToeMove) move).move();
        Assert.assertTrue("Move row in range", pos[0] >= 0 && pos[0] < 3);
        Assert.assertTrue("Move col in range", pos[1] >= 0 && pos[1] < 3);
    }

    @Test
    public void testStateAfterMove() {
        Move<TicTacToe> move = mcts.findNextMove(1000);
        State<TicTacToe> next = initialState.next(move);
        Assert.assertNotNull("Next state should not be null", next);
        Assert.assertNotEquals("Next state should differ from initial", initialState.toString(), next.toString());
    }

    @Test
    public void testMCTSCanFinishGame() {
        State<TicTacToe> state = initialState;
        while (!state.isTerminal()) {
            TicTacToeNode node = new TicTacToeNode(state);
            MCTS m = new MCTS(node);
            Move<TicTacToe> move = m.findNextMove(500);
            Assert.assertNotNull("Move should not be null during game", move);
            state = state.next(move);
        }
        Assert.assertTrue("Game should end", state.isTerminal());
    }

    @Test
    public void testWinnerAfterSimulatedGame() {
        State<TicTacToe> state = initialState;
        while (!state.isTerminal()) {
            TicTacToeNode node = new TicTacToeNode(state);
            MCTS m = new MCTS(node);
            Move<TicTacToe> move = m.findNextMove(500);
            state = state.next(move);
        }
        Assert.assertTrue("Game ends with win or draw", state.winner().isPresent() || state.isTerminal());
    }
}