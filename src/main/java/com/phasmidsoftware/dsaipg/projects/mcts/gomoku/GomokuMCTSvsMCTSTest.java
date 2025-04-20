package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;

import java.util.*;
import java.util.concurrent.*;

public class GomokuMCTSvsMCTSTest {

    private static final int GAMES_PER_MODE = 100;
    private static final int THREADS = 12;

    public static void main(String[] args) {
        testAtIteration(1000);
    }

    private static void testAtIteration(int iteration) {
        System.out.println("Iteration: " + iteration + "\n");

        System.out.println("| First Player | MCTS (X) Wins | MCTS (O) Wins | Draws | X Win % | O Win % | Draw % | Average time per game |");
        System.out.println("| --- | --- | --- | --- | --- | --- | --- | --- |");

        runTest("X (MCTS) first", true, iteration);
        runTest("O (MCTS) first", false, iteration);
        runAlternatingTest("Alternating", iteration);
    }

    private static void runTest(String label, boolean xFirst, int iteration) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<long[]>> futures = new ArrayList<>();

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            final boolean firstX = xFirst;
            futures.add(executor.submit(() -> playOneGame(firstX, iteration)));
        }

        processResults(label, futures);
        executor.shutdown();
    }

    private static void runAlternatingTest(String label, int iteration) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<long[]>> futures = new ArrayList<>();

        boolean xFirst = true;
        for (int i = 0; i < GAMES_PER_MODE; i++) {
            final boolean firstMoveX = xFirst;
            futures.add(executor.submit(() -> playOneGame(firstMoveX, iteration)));
            xFirst = !xFirst;
        }

        processResults(label, futures);
        executor.shutdown();
    }

    private static void processResults(String label, List<Future<long[]>> futures) {
        int xWins = 0;
        int oWins = 0;
        int draws = 0;
        long totalTimeNano = 0;

        for (Future<long[]> future : futures) {
            try {
                long[] result = future.get();
                int winner = (int) result[0];
                long timeNano = result[1];
                totalTimeNano += timeNano;

                if (winner == Gomoku.PLAYER_ONE) xWins++;
                else if (winner == Gomoku.PLAYER_TWO) oWins++;
                else draws++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double total = xWins + oWins + draws;
        double xWinPercent = (xWins / total) * 100;
        double oWinPercent = (oWins / total) * 100;
        double drawPercent = (draws / total) * 100;
        double avgMillis = totalTimeNano / 1_000_000.0 / GAMES_PER_MODE;

        System.out.printf("| %s | %d | %d | %d | %.2f%% | %.2f%% | %.2f%% | %.2f ms |\n",
                label, xWins, oWins, draws, xWinPercent, oWinPercent, drawPercent, avgMillis);
    }

    private static long[] playOneGame(boolean xFirst, int iteration) {
        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        if (!xFirst) {
            state = new GomokuState(state.getBoard(), Gomoku.PLAYER_TWO);
        }

        long start = System.nanoTime();

        while (!state.isTerminal()) {
            Move<Gomoku> move;
            GomokuNode rootNode = new GomokuNode(state);
            MCTS mcts = new MCTS(rootNode);
            move = mcts.findNextMove(iteration);
            state = (GomokuState) state.next(move);
        }

        long end = System.nanoTime();
        Optional<Integer> winner = state.winner();
        int result = winner.orElse(0);
        long duration = end - start;

        return new long[]{result, duration};
    }
}
