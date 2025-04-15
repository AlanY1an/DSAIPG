package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Optional;

public class TicTacToeMCTSvsMCTSSimulator {

    public static void main(String[] args) {
        int totalGames = 500;
        int xWins = 0;
        int oWins = 0;
        int draws = 0;

        for (int i = 1; i <= totalGames; i++) {
            TicTacToe game = new TicTacToe();
            TicTacToe.TicTacToeState state = game.new TicTacToeState();

            while (!state.isTerminal()) {
                int currentPlayer = state.player();
                TicTacToeNode rootNode = new TicTacToeNode(state);
                MCTS mcts = new MCTS(rootNode);
                Move<TicTacToe> move = mcts.findNextMove(5000); // MCTS search depth

                state = (TicTacToe.TicTacToeState) state.next(move);
            }

            Optional<Integer> winner = state.winner();
            if (winner.isEmpty()) {
                draws++;
                System.out.println("Game " + i + ": Draw");
            } else if (winner.get() == TicTacToe.X) {
                xWins++;
                System.out.println("Game " + i + ": X (MCTS) wins");
            } else {
                oWins++;
                System.out.println("Game " + i + ": O (MCTS) wins");
            }

            if (i % 50 == 0) {
                System.out.println("Board at game " + i + ":\n" + state);
            }
        }

        System.out.println("\n=== MCTS (X) vs MCTS (O) ===");
        System.out.printf("Total games played: %d\n", totalGames);
        System.out.printf("X (MCTS) wins: %d (%.2f%%)\n", xWins, xWins * 100.0 / totalGames);
        System.out.printf("O (MCTS) wins: %d (%.2f%%)\n", oWins, oWins * 100.0 / totalGames);
        System.out.printf("Draws: %d (%.2f%%)\n", draws, draws * 100.0 / totalGames);
    }
}
