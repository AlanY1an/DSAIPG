package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;

import java.util.Optional;

public class GomokuMCTSvsMCTSTest {

    private static final int GAMES_PER_MODE = 100;

    public static void main(String[] args) {
        testAtIteration(1000, 3000);
    }

    private static void testAtIteration(int iterationX, int iterationO) {
        System.out.println("\nMCTS vs MCTS | Iteration: X=" + iterationX + ", O=" + iterationO);
        System.out.println("| First Player | X Wins | O Wins | Draws | X Win % | O Win % | Draw % | Average time per game |");
        System.out.println("| --- | --- | --- | --- | --- | --- | --- | --- |");

        runTest("X first (1000)", true, iterationX, iterationO);
        runTest("O first (3000)", false, iterationX, iterationO);
        runAlternatingTest("Alternating", iterationX, iterationO);
    }

    private static void runTest(String label, boolean xFirst, int iterationX, int iterationO) {
        int xWins = 0;
        int oWins = 0;
        int draws = 0;
        long totalTimeNano = 0;

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            int opener = xFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
            long start = System.nanoTime();
            int result = playSingleGame(opener, iterationX, iterationO);
            long end = System.nanoTime();
            totalTimeNano += (end - start);

            if (result == Gomoku.PLAYER_ONE) xWins++;
            else if (result == Gomoku.PLAYER_TWO) oWins++;
            else draws++;
        }

        double avgMillis = totalTimeNano / 1_000_000.0 / GAMES_PER_MODE;
        printResultRow(label, xWins, oWins, draws, avgMillis);
    }

    private static void runAlternatingTest(String label, int iterationX, int iterationO) {
        int xWins = 0;
        int oWins = 0;
        int draws = 0;
        long totalTimeNano = 0;
        boolean xFirst = true;

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            int opener = xFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
            long start = System.nanoTime();
            int result = playSingleGame(opener, iterationX, iterationO);
            long end = System.nanoTime();
            totalTimeNano += (end - start);

            if (result == Gomoku.PLAYER_ONE) xWins++;
            else if (result == Gomoku.PLAYER_TWO) oWins++;
            else draws++;

            xFirst = !xFirst;
        }

        double avgMillis = totalTimeNano / 1_000_000.0 / GAMES_PER_MODE;
        printResultRow(label, xWins, oWins, draws, avgMillis);
    }

    private static int playSingleGame(int opener, int iterationX, int iterationO) {
        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        if (opener == Gomoku.PLAYER_TWO) {
            state = new GomokuState(state.getBoard(), Gomoku.PLAYER_TWO);
        }

        while (!state.isTerminal()) {
            Move<Gomoku> move;
            if (state.player() == Gomoku.PLAYER_ONE) {
                GomokuNode rootNode = new GomokuNode(state);
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(iterationX);
            } else {
                GomokuNode rootNode = new GomokuNode(state);
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(iterationO);
            }
            state = (GomokuState) state.next(move);
        }

        Optional<Integer> winner = state.winner();
        return winner.orElse(0);
    }

    private static void printResultRow(String label, int xWins, int oWins, int draws, double avgMillis) {
        double total = xWins + oWins + draws;
        double xWinPercent = (xWins / total) * 100;
        double oWinPercent = (oWins / total) * 100;
        double drawPercent = (draws / total) * 100;
        System.out.printf("| %s | %d | %d | %d | %.2f%% | %.2f%% | %.2f%% | %.2f ms |\n",
                label, xWins, oWins, draws, xWinPercent, oWinPercent, drawPercent, avgMillis);
    }
}