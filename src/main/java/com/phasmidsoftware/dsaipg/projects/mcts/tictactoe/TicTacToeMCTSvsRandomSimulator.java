package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Optional;

public class TicTacToeMCTSvsRandomSimulator {

    public static void main(String[] args) {
        runSimulation("X always first", 500, TicTacToe.X, TicTacToe.X);
        runSimulation("O always first", 500, TicTacToe.O, TicTacToe.O);
        runSimulation("Alternating",     500, -1, -1);
    }

    private static void runSimulation(String label, int totalGames, int fixedFirstPlayer, int altStartValue) {
        System.out.println("\n=== Running: " + label + " ===");

        int xWins = 0;
        int oWins = 0;
        int draws = 0;

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalGames; i++) {
            int firstPlayer = (fixedFirstPlayer == -1)
                    ? ((i % 2 == 0) ? TicTacToe.X : TicTacToe.O)
                    : fixedFirstPlayer;

            TicTacToe game = new TicTacToe();
            TicTacToe.TicTacToeState state = game.new TicTacToeState();

            Move<TicTacToe> firstMove;
            if (firstPlayer == TicTacToe.X) {
                TicTacToeNode rootNode = new TicTacToeNode(state);
                MCTS mcts = new MCTS(rootNode);
                firstMove = mcts.findNextMove(5000);
            } else {
                firstMove = state.chooseMove(TicTacToe.O);
            }
            state = (TicTacToe.TicTacToeState) state.next(firstMove);

            while (!state.isTerminal()) {
                int currentPlayer = state.player();
                Move<TicTacToe> move;
                if (currentPlayer == TicTacToe.X) {
                    TicTacToeNode rootNode = new TicTacToeNode(state);
                    MCTS mcts = new MCTS(rootNode);
                    move = mcts.findNextMove(1250);
                } else {
                    move = state.chooseMove(currentPlayer);
                }
                state = (TicTacToe.TicTacToeState) state.next(move);
            }

            Optional<Integer> winner = state.winner();
            if (winner.isEmpty()) {
                draws++;
            } else if (winner.get() == TicTacToe.X) {
                xWins++;
            } else {
                oWins++;
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerGame = totalTime / (double) totalGames;

        System.out.println("\n=== " + label + " Results ===");
        System.out.printf("Total games played: %d\n", totalGames);
        System.out.printf("MCTS (X) wins: %d (%.2f%%)\n", xWins, xWins * 100.0 / totalGames);
        System.out.printf("Random (O) wins: %d (%.2f%%)\n", oWins, oWins * 100.0 / totalGames);
        System.out.printf("Draws: %d (%.2f%%)\n", draws, draws * 100.0 / totalGames);
        System.out.printf("Total runtime: %.2f seconds\n", totalTime / 1000.0);
        System.out.printf("Average time per game: %.2f ms\n", avgTimePerGame);
    }
}
