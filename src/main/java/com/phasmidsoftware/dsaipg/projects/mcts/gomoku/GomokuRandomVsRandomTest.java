package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;

import java.util.Optional;
import java.util.Random;

public class GomokuRandomVsRandomTest {

    private static final int GAMES_PER_MODE = 100000;

    public static void main(String[] args) {
        System.out.println("Running Random vs Random Tests...");

        System.out.println("| Turn Order | X Wins | O Wins | Draws | X Win % | O Win % | Draw % |");
        System.out.println("| --- | --- | --- | --- | --- | --- | --- |");

        runTest("O goes first", false);
        runTest("X goes first", true);
        runAlternatingTest("Alternating");
    }

    private static void runTest(String label, boolean xFirst) {
        int xWins = 0;
        int oWins = 0;
        int draws = 0;

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            int opener = xFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
            int result = playSingleGame(opener);
            if (result == Gomoku.PLAYER_ONE) xWins++;
            else if (result == Gomoku.PLAYER_TWO) oWins++;
            else draws++;
        }

        printResultRow(label, xWins, oWins, draws);
    }

    private static void runAlternatingTest(String label) {
        int xWins = 0;
        int oWins = 0;
        int draws = 0;
        boolean xFirst = true;

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            int opener = xFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
            int result = playSingleGame(opener);
            if (result == Gomoku.PLAYER_ONE) xWins++;
            else if (result == Gomoku.PLAYER_TWO) oWins++;
            else draws++;
            xFirst = !xFirst;
        }

        printResultRow(label, xWins, oWins, draws);
    }

    private static int playSingleGame(int opener) {
        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        // If the opener is O (PLAYER_TWO), swap the initial player
        if (opener == Gomoku.PLAYER_TWO) {
            state = new GomokuState(state.getBoard(), Gomoku.PLAYER_TWO);
        }

        while (!state.isTerminal()) {
            Move<Gomoku> move = state.chooseMove(state.player());
            state = (GomokuState) state.next(move);
        }

        Optional<Integer> winner = state.winner();
        return winner.orElse(0); // 0 means Draw
    }

    private static void printResultRow(String label, int xWins, int oWins, int draws) {
        double total = xWins + oWins + draws;
        double xWinPercent = (xWins / total) * 100;
        double oWinPercent = (oWins / total) * 100;
        double drawPercent = (draws / total) * 100;
        System.out.printf("| %s | %d | %d | %d | %.2f%% | %.2f%% | %.2f%% |\n",
                label, xWins, oWins, draws, xWinPercent, oWinPercent, drawPercent);
    }
}
