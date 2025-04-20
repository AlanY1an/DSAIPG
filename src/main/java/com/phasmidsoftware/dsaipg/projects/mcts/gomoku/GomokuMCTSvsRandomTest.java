package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;

import java.util.Optional;

public class GomokuMCTSvsRandomTest {

    private static final int GAMES_PER_MODE = 100;

    public static void main(String[] args) {
        testAtIteration(1000);
        testAtIteration(3000);
    }

    private static void testAtIteration(int iterations) {
        System.out.println("\nIteration: " + iterations);
        System.out.println("| First Player | MCTS (X) Wins | Random (O) Wins | Draws | MCTS Win % | Random Win % | Draw % | Average time per game |");
        System.out.println("| --- | --- | --- | --- | --- | --- | --- | --- |");

        runTest("X (MCTS) first", true, iterations);
        runTest("O (Random) first", false, iterations);
        runAlternatingTest("Alternating", iterations);
    }

    private static void runTest(String label, boolean mctsFirst, int iterations) {
        int mctsWins = 0;
        int randomWins = 0;
        int draws = 0;
        long totalTimeNano = 0;

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            int opener = mctsFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
            long start = System.nanoTime();
            int result = playSingleGame(opener, iterations);
            long end = System.nanoTime();
            totalTimeNano += (end - start);

            if (result == Gomoku.PLAYER_ONE) mctsWins++;
            else if (result == Gomoku.PLAYER_TWO) randomWins++;
            else draws++;
        }

        double avgMillis = totalTimeNano / 1_000_000.0 / GAMES_PER_MODE;
        printResultRow(label, mctsWins, randomWins, draws, avgMillis);
    }

    private static void runAlternatingTest(String label, int iterations) {
        int mctsWins = 0;
        int randomWins = 0;
        int draws = 0;
        long totalTimeNano = 0;
        boolean mctsFirst = true;

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            int opener = mctsFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
            long start = System.nanoTime();
            int result = playSingleGame(opener, iterations);
            long end = System.nanoTime();
            totalTimeNano += (end - start);

            if (result == Gomoku.PLAYER_ONE) mctsWins++;
            else if (result == Gomoku.PLAYER_TWO) randomWins++;
            else draws++;

            mctsFirst = !mctsFirst;
        }

        double avgMillis = totalTimeNano / 1_000_000.0 / GAMES_PER_MODE;
        printResultRow(label, mctsWins, randomWins, draws, avgMillis);
    }

    private static int playSingleGame(int opener, int iterations) {
        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        // Adjust starter if necessary
        if (opener == Gomoku.PLAYER_TWO) {
            state = new GomokuState(state.getBoard(), Gomoku.PLAYER_TWO);
        }

        while (!state.isTerminal()) {
            Move<Gomoku> move;
            if (state.player() == Gomoku.PLAYER_ONE) {
                // MCTS Move
                GomokuNode rootNode = new GomokuNode(state);
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(iterations);
            } else {
                // Random Move
                move = state.chooseMove(state.player());
            }
            state = (GomokuState) state.next(move);
        }

        Optional<Integer> winner = state.winner();
        return winner.orElse(0);
    }

    private static void printResultRow(String label, int mctsWins, int randomWins, int draws, double avgMillis) {
        double total = mctsWins + randomWins + draws;
        double mctsWinPercent = (mctsWins / total) * 100;
        double randomWinPercent = (randomWins / total) * 100;
        double drawPercent = (draws / total) * 100;
        System.out.printf("| %s | %d | %d | %d | %.2f%% | %.2f%% | %.2f%% | %.2f ms |\n",
                label, mctsWins, randomWins, draws, mctsWinPercent, randomWinPercent, drawPercent, avgMillis);
    }
}