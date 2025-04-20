package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import java.util.Optional;

public class GomokuMain {
    public static void main(String[] args) {
        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        while (!state.isTerminal()) {
            int currentPlayer = state.player();
            Move<Gomoku> move;

            if (currentPlayer == Gomoku.PLAYER_ONE) {
                GomokuNode rootNode = new GomokuNode(state);
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(500);
                System.out.println("MCTS (X) Move: " + ((GomokuMove) move).toString());
            } else {
                move = state.chooseMove(currentPlayer);
                System.out.println("Random (O) Move: " + ((GomokuMove) move).toString());
            }

            state = (GomokuState) state.next(move);
            System.out.println(state);
            System.out.println("--------------------------------------");
        }

        Optional<Integer> winner = state.winner();
        System.out.println("Game Over! Winner: " + (winner.isPresent() ? "P" + winner.get() : "Draw"));
    }
}
