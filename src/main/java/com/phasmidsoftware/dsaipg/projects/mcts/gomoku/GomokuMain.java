package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import java.util.Optional;

/**
 * GomokuMain 用来测试不同模型（Random, MCTS, OptimizedMCTS）之间的对战效果。
 * 可以自由选择 X玩家和O玩家使用的模型。
 */
public class    GomokuMain {

    enum PlayerType {
        RANDOM,
        MCTS,
        OPTIMIZED_MCTS
    }

    private static final int ITERATIONS = 1000; // MCTS/OptimizedMCTS搜索迭代次数

    public static void main(String[] args) {

        // 选择 X 和 O 双方使用的策略
        PlayerType player1 = PlayerType.MCTS;            // X 方 (先手)
        PlayerType player2 = PlayerType.OPTIMIZED_MCTS;  // O 方 (后手)

        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        System.out.println("Start New Game: " + player1 + " (X) vs " + player2 + " (O)");
        System.out.println(state);

        while (!state.isTerminal()) {
            int currentPlayer = state.player();
            PlayerType playerType = currentPlayer == Gomoku.PLAYER_ONE ? player1 : player2;

            Move<Gomoku> move = decideMove(state, playerType);

            System.out.printf("%s (%s) Move: %s%n", playerType, (currentPlayer == Gomoku.PLAYER_ONE ? "X" : "O"), move);

            state = (GomokuState) state.next(move);
            System.out.println(state);
            System.out.println("--------------------------------------");
        }

        Optional<Integer> winner = state.winner();
        if (winner.isPresent()) {
            System.out.println("Game Over! Winner: Player " + (winner.get() == Gomoku.PLAYER_ONE ? "X" : "O"));
        } else {
            System.out.println("Game Over! Result: Draw");
        }
    }

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