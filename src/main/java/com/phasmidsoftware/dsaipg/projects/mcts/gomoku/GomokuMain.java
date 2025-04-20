package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import java.util.Optional;

/**
 * GomokuMain is used to test matches between different agents (Random, MCTS, OptimizedMCTS).
 * You can control which agent plays X and which plays O through command-line arguments.
 *
 * Usage: java GomokuMain <X-strategy> <O-strategy>
 * Strategies: RANDOM, MCTS, OPTIMIZED_MCTS
 */
public class GomokuMain {

    enum PlayerType {
        RANDOM,
        MCTS,
        OPTIMIZED_MCTS
    }

    private static final int ITERATIONS = 1000; // Number of MCTS/OptimizedMCTS search iterations

    public static void main(String[] args) {
        // Default: OptimizedMCTS plays X, MCTS plays O
        PlayerType player2 = PlayerType.MCTS;
        PlayerType player1 = PlayerType.OPTIMIZED_MCTS;

        // If user provides command-line arguments, override defaults
        if (args.length >= 2) {
            player1 = PlayerType.valueOf(args[0].toUpperCase());
            player2 = PlayerType.valueOf(args[1].toUpperCase());
        }

        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        System.out.println("Start New Game: " + player1 + " (X) vs " + player2 + " (O)");
        System.out.println(state);

        // Main game loop
        while (!state.isTerminal()) {
            int currentPlayer = state.player();
            PlayerType playerType = (currentPlayer == Gomoku.PLAYER_ONE) ? player1 : player2;
            Move<Gomoku> move = decideMove(state, playerType);
            System.out.printf("%s (%s) Move: %s%n", playerType,
                    (currentPlayer == Gomoku.PLAYER_ONE ? "X" : "O"), move);

            state = (GomokuState) state.next(move);
            System.out.println(state);
            System.out.println("--------------------------------------");
        }

        // Announce winner
        Optional<Integer> winner = state.winner();
        if (winner.isPresent()) {
            System.out.println("Game Over! Winner: Player " +
                    (winner.get() == Gomoku.PLAYER_ONE ? "X" : "O"));
        } else {
            System.out.println("Game Over! Result: Draw");
        }
    }

    /**
     * Choose a move based on the player's assigned strategy type.
     */
    private static Move<Gomoku> decideMove(GomokuState state, PlayerType type) {
        switch (type) {
            case RANDOM:
                return state.chooseMove(state.player());
            case MCTS:
                GomokuNode mctsRoot = new GomokuNode(state);
                MCTS mcts = new MCTS(mctsRoot);
                return mcts.findNextMove(ITERATIONS);
            case OPTIMIZED_MCTS:
                GomokuNode optRoot = new GomokuNode(state);
                OptimizedMCTS optimizedMCTS = new OptimizedMCTS(optRoot);
                return optimizedMCTS.findNextMove(ITERATIONS);
            default:
                throw new IllegalArgumentException("Unknown player type: " + type);
        }
    }
}
