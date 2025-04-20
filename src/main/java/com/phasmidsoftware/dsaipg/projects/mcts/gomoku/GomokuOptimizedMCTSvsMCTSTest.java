package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import java.util.*;
import java.util.concurrent.*;

public class GomokuOptimizedMCTSvsMCTSTest {

    private static final int GAMES_PER_MODE = 100;
    private static final int THREADS = 12;

    public static void main(String[] args) {
        testAtIteration(1000);
    }

    private static void testAtIteration(int iteration) {
        System.out.println("Iteration: " + iteration + "\n");

        System.out.println("| First Player | OptimizedMCTS Wins | MCTS Wins | Draws | Optimized Win % | MCTS Win % | Draw % | Avg Time per Game |");
        System.out.println("| --- | --- | --- | --- | --- | --- | --- | --- |");

        runTest("OptimizedMCTS (X) first", true, iteration);
        runTest("MCTS (X) first", false, iteration);
        runAlternatingTest("Alternating", iteration);
    }

    private static void runTest(String label, boolean optimizedFirst, int iteration) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<long[]>> futures = new ArrayList<>();

        for (int i = 0; i < GAMES_PER_MODE; i++) {
            final boolean firstOptimized = optimizedFirst;
            futures.add(executor.submit(() -> playOneGame(firstOptimized, iteration)));
        }

        processResults(label, futures);
        executor.shutdown();
    }

    private static void runAlternatingTest(String label, int iteration) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<long[]>> futures = new ArrayList<>();

        boolean optimizedFirst = true;
        for (int i = 0; i < GAMES_PER_MODE; i++) {
            final boolean firstMoveOptimized = optimizedFirst;
            futures.add(executor.submit(() -> playOneGame(firstMoveOptimized, iteration)));
            optimizedFirst = !optimizedFirst;
        }

        processResults(label, futures);
        executor.shutdown();
    }

    private static void processResults(String label, List<Future<long[]>> futures) {
        int optimizedWins = 0;
        int mctsWins = 0;
        int draws = 0;
        long totalTimeNano = 0;

        for (Future<long[]> future : futures) {
            try {
                long[] result = future.get();
                int winner = (int) result[0];
                long timeNano = result[1];
                totalTimeNano += timeNano;

                if (winner == 1) optimizedWins++;
                else if (winner == 2) mctsWins++;
                else draws++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double total = optimizedWins + mctsWins + draws;
        double optimizedWinPercent = (optimizedWins / total) * 100;
        double mctsWinPercent = (mctsWins / total) * 100;
        double drawPercent = (draws / total) * 100;
        double avgMillis = totalTimeNano / 1_000_000.0 / GAMES_PER_MODE;

        System.out.printf("| %s | %d | %d | %d | %.2f%% | %.2f%% | %.2f%% | %.2f ms |\n",
                label, optimizedWins, mctsWins, draws, optimizedWinPercent, mctsWinPercent, drawPercent, avgMillis);
    }

    private static long[] playOneGame(boolean optimizedFirst, int iteration) {
        Gomoku game = new Gomoku();
        GomokuState state = (GomokuState) game.start();

        long start = System.nanoTime();

        while (!state.isTerminal()) {
            Move<Gomoku> move;
            int currentPlayer = state.player();

            if ((currentPlayer == Gomoku.PLAYER_ONE && optimizedFirst) ||
                    (currentPlayer == Gomoku.PLAYER_TWO && !optimizedFirst)) {
                // OptimizedMCTS player
                GomokuNode rootNode = new GomokuNode(state);
                OptimizedMCTS optimizedMCTS = new OptimizedMCTS(rootNode);
                move = optimizedMCTS.findNextMove(iteration);
            } else {
                // Normal MCTS player
                GomokuNode rootNode = new GomokuNode(state);
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(iteration);
            }

            state = (GomokuState) state.next(move);
        }

        long end = System.nanoTime();
        Optional<Integer> winner = state.winner();
        int result = winner.orElse(0);
        long duration = end - start;

        return new long[]{result, duration};
    }
}
