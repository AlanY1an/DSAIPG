package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public class TicTacToeRandomSimulator {

    public static void main(String[] args) {
        int totalGames = 5000;
        int xWins = 0;
        int oWins = 0;
        int draws = 0;

        for (int i = 1; i <= totalGames; i++) {


//        int firstPlayer = TicTacToe.O;
//        int firstPlayer = TicTacToe.X;
            int firstPlayer = (i % 2 == 0) ? TicTacToe.X : TicTacToe.O;


            TicTacToe game = new TicTacToe();
            State<TicTacToe> state = game.start();

            int player = firstPlayer;

            // Run till game over
            while (!state.isTerminal()) {
                state = state.next(state.chooseMove(player));
                player = 1 - player;
            }

            // Win rate statistics
            var winner = state.winner();
            if (winner.isEmpty()) {
                draws++;
                System.out.println("Game " + i + ": Draw");
            } else if (winner.get() == TicTacToe.X) {
                xWins++;
                System.out.println("Game " + i + ": X wins");
            } else {
                oWins++;
                System.out.println("Game " + i + ": O wins");
            }

            // Print the board every 50 games
            if (i % 50 == 0) {
                System.out.println("Final Board for game " + i + ":");
                System.out.println(state);
            }
        }

        // Print all the info
        System.out.println("\n=== Final Statistics ===");
        System.out.printf("Total games played: %d\n", totalGames);
        System.out.printf("X wins: %d (%.2f%%)\n", xWins, xWins * 100.0 / totalGames);
        System.out.printf("O wins: %d (%.2f%%)\n", oWins, oWins * 100.0 / totalGames);
        System.out.printf("Draws : %d (%.2f%%)\n", draws, draws * 100.0 / totalGames);
    }
}
