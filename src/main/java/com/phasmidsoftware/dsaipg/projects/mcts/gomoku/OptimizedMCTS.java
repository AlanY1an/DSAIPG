package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

public class OptimizedMCTS {

    private final SimpleNode<Gomoku> root;
    private static final double EXPLORATION_CONSTANT = 1.8;
    private static final double EPSILON = 1e-6;
    private static final Random random = new Random();
    private final ExecutorService executor;

    public OptimizedMCTS(SimpleNode<Gomoku> root) {
        this.root = root;
        this.executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    // Find the best move using MCTS with parallel iterations
    public Move<Gomoku> findNextMove(int iterations) {
        int[][] board = ((GomokuState) root.state()).getBoard();
        int size = board.length;
        boolean isEmpty = true;
        for (int i = 0; i < size && isEmpty; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != 0) {
                    isEmpty = false;
                    break;
                }
            }
        }

        // Return center move for empty board
        if (isEmpty) {
            int center = size / 2;
            return new GomokuMove(root.state().player(), center, center);
        }

        int adjustedIterations = iterations;
        // Increase iterations for critical board states
        if (hasCriticalPattern(board, root.state().player())) {
            adjustedIterations *= 2;
        }

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        int iterationsPerThread = Math.max(1, adjustedIterations / threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        List<SimpleNode<Gomoku>> path = new ArrayList<>();
                        path.add(root);
                        SimpleNode<Gomoku> node = root;

                        while (!node.isLeaf() && !node.children().isEmpty()) {
                            node = selectChild(node);
                            path.add(node);
                        }

                        if (!node.state().isTerminal()) {
                            synchronized (node) {
                                if (node.isLeaf()) {
                                    expandNode(node);
                                }
                                if (!node.children().isEmpty()) {
                                    node = selectChild(node);
                                    path.add(node);
                                }
                            }
                        }

                        int reward = simulatePlayout(node.state());

                        for (SimpleNode<Gomoku> n : path) {
                            synchronized (n) {
                                n.updateStats(reward);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        SimpleNode<Gomoku> bestChild = null;
        double bestScore = -Double.MAX_VALUE;
        synchronized (root) {
            for (SimpleNode<Gomoku> child : root.children()) {
                if (child.playouts() == 0) continue;
                double score = (double) child.wins() / child.playouts();
                if (score > bestScore) {
                    bestScore = score;
                    bestChild = child;
                }
            }
        }

        return bestChild == null ? null : extractMove(root.state(), bestChild.state());
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private SimpleNode<Gomoku> selectChild(SimpleNode<Gomoku> node) {
        synchronized (node) {
            if (node.children().isEmpty()) return node;

            SimpleNode<Gomoku> best = null;
            double bestValue = -Double.MAX_VALUE;
            double totalPlayouts = node.playouts() + 1;

            for (SimpleNode<Gomoku> child : node.children()) {
                double childPlayouts = child.playouts() + EPSILON;
                double exploitation = (double) child.wins() / childPlayouts;
                double exploration = EXPLORATION_CONSTANT * sqrt(log(totalPlayouts) / childPlayouts);
                double heuristicBias = getHeuristicBias(child.state(), node.state().player()) / childPlayouts;

                double uctValue = exploitation + exploration + heuristicBias;
                if (uctValue > bestValue) {
                    bestValue = uctValue;
                    best = child;
                }
            }

            return best;
        }
    }

    private double getHeuristicBias(State<Gomoku> state, int player) {
        int[][] board = ((GomokuState) state).getBoard();
        int[][] rootBoard = ((GomokuState) root.state()).getBoard();
        int x = -1, y = -1;

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] != rootBoard[i][j]) {
                    x = i;
                    y = j;
                    break;
                }
            }
            if (x != -1) break;
        }

        if (x == -1) return 0;

        int opponent = player == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
        int ownScore = evaluateMove(board, player, x, y);
        int opponentScore = evaluateMove(board, opponent, x, y);

        if (opponentScore >= 5000000) return opponentScore / 20.0;
        if (ownScore >= 4000000) return ownScore / 25.0;
        if (ownScore >= 3000000) return ownScore / 30.0;
        if (opponentScore >= 2000000) return opponentScore / 35.0;
        if (ownScore >= 1000000) return ownScore / 40.0;
        if (opponentScore >= 900000) return opponentScore / 45.0;
        return (ownScore * 0.3 + opponentScore * 5.0) / 1000.0;
    }

    // Expand node with prioritized moves
    private void expandNode(SimpleNode<Gomoku> node) {
        Collection<Move<Gomoku>> moves = node.state().moves(node.state().player());
        List<Move<Gomoku>> prioritizedMoves = prioritizeMoves(moves, node.state());
        int maxExpansions = Math.min(prioritizedMoves.size(), 15);

        for (int i = 0; i < maxExpansions; i++) {
            State<Gomoku> newState = node.state().next(prioritizedMoves.get(i));
            node.addChild(newState);
        }
    }

    // Prioritize moves based on critical patterns
    private List<Move<Gomoku>> prioritizeMoves(Collection<Move<Gomoku>> moves, State<Gomoku> state) {
        List<MoveScore> scoredMoves = new ArrayList<>();
        int[][] board = ((GomokuState) state).getBoard();
        int player = state.player();
        int opponent = player == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
        int size = board.length;

        List<int[]> occupied = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != 0) occupied.add(new int[]{i, j});
            }
        }

        List<Move<Gomoku>> nearbyMoves = new ArrayList<>();
        for (Move<Gomoku> move : moves) {
            int x = ((GomokuMove) move).getX();
            int y = ((GomokuMove) move).getY();
            boolean isNearby = occupied.isEmpty();
            for (int[] pos : occupied) {
                if (Math.abs(x - pos[0]) + Math.abs(y - pos[1]) <= 2) {
                    isNearby = true;
                    break;
                }
            }
            if (isNearby) nearbyMoves.add(move);
        }
        if (nearbyMoves.isEmpty()) nearbyMoves.addAll(moves);

        for (Move<Gomoku> move : nearbyMoves) {
            int x = ((GomokuMove) move).getX();
            int y = ((GomokuMove) move).getY();

            int ownScore = evaluateMove(board, player, x, y);
            int opponentScore = evaluateMove(board, opponent, x, y);

            int[][] tempBoard = deepCopy(board);
            tempBoard[x][y] = player;
            if (hasWinningPattern(tempBoard, x, y, player)) {
                scoredMoves.add(new MoveScore(move, Integer.MAX_VALUE));
                continue;
            }

            tempBoard = deepCopy(board);
            tempBoard[x][y] = opponent;
            if (hasWinningPattern(tempBoard, x, y, opponent)) {
                scoredMoves.add(new MoveScore(move, Integer.MAX_VALUE - 1));
                continue;
            }

            // Boost score for moves creating forcing sequences
            int forcingBonus = 0;
            if (ownScore >= 3000000) {
                tempBoard = deepCopy(board);
                tempBoard[x][y] = player;
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (tempBoard[i][j] == 0) {
                            tempBoard[i][j] = player;
                            if (hasFourOrOpenThree(tempBoard, i, j, player, true)) {
                                forcingBonus += 1000000;
                            }
                            tempBoard[i][j] = 0;
                        }
                    }
                }
            }

            int score = Math.max(ownScore, opponentScore * 12) + forcingBonus;
            scoredMoves.add(new MoveScore(move, score));
        }

        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        return scoredMoves.stream().map(ms -> ms.move).collect(Collectors.toList());
    }

    private static class MoveScore {
        Move<Gomoku> move;
        int score;
        MoveScore(Move<Gomoku> move, int score) {
            this.move = move;
            this.score = score;
        }
    }

    private boolean hasWinningPattern(int[][] board, int x, int y, int player) {
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int count = 1;
            for (int k = 1; k < 5; k++) {
                int nx = x + d[0] * k;
                int ny = y + d[1] * k;
                if (nx < 0 || ny < 0 || nx >= board.length || ny >= board.length || board[nx][ny] != player) break;
                count++;
            }
            for (int k = 1; k < 5; k++) {
                int nx = x - d[0] * k;
                int ny = y - d[1] * k;
                if (nx < 0 || ny < 0 || nx >= board.length || ny >= board.length || board[nx][ny] != player) break;
                count++;
            }
            if (count >= 5) return true;
        }
        return false;
    }

    private boolean hasFourOrOpenThree(int[][] board, int x, int y, int player, boolean checkFour) {
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int consecutive = 0;
            int openEnds = 0;

            for (int step = 1; step <= 4; step++) {
                int nx = x + d[0] * step;
                int ny = y + d[1] * step;
                if (nx < 0 || ny < 0 || nx >= board.length || ny >= board.length) break;
                if (board[nx][ny] == player) consecutive++;
                else if (board[nx][ny] == 0) { openEnds++; break; }
                else break;
            }

            for (int step = 1; step <= 4; step++) {
                int nx = x - d[0] * step;
                int ny = y - d[1] * step;
                if (nx < 0 || ny < 0 || nx >= board.length || ny >= board.length) break;
                if (board[nx][ny] == player) consecutive++;
                else if (board[nx][ny] == 0) { openEnds++; break; }
                else break;
            }

            if (checkFour && consecutive == 4 && openEnds >= 1) return true;
            if (!checkFour && consecutive == 3 && openEnds == 2) return true;
        }
        return false;
    }

    private boolean hasCriticalPattern(int[][] board, int player) {
        int opponent = player == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
        int size = board.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) {
                    int[][] tempBoard = deepCopy(board);
                    tempBoard[i][j] = opponent;
                    if (hasFourOrOpenThree(tempBoard, i, j, opponent, true) || hasFourOrOpenThree(tempBoard, i, j, opponent, false)) {
                        return true;
                    }
                    tempBoard[i][j] = player;
                    if (hasFourOrOpenThree(tempBoard, i, j, player, true) || hasFourOrOpenThree(tempBoard, i, j, player, false)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int simulatePlayout(State<Gomoku> state) {
        State<Gomoku> tempState = state;
        int currentPlayer = getPreviousPlayer(tempState);
        int maxSteps = 60;
        int steps = 0;

        while (!tempState.isTerminal() && steps < maxSteps) {
            Collection<Move<Gomoku>> moves = tempState.moves(tempState.player());
            List<MoveScore> scoredMoves = new ArrayList<>();
            int[][] board = ((GomokuState) tempState).getBoard();
            int player = tempState.player();
            int opponent = player == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;

            for (Move<Gomoku> move : moves) {
                int x = ((GomokuMove) move).getX();
                int y = ((GomokuMove) move).getY();
                int score = evaluateMove(board, player, x, y);
                scoredMoves.add(new MoveScore(move, score));
            }

            scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
            Move<Gomoku> move = scoredMoves.isEmpty() ? moves.iterator().next() : scoredMoves.get(0).move;
            tempState = tempState.next(move);
            steps++;
        }

        Optional<Integer> winnerOpt = tempState.winner();
        return winnerOpt.map(w -> w == currentPlayer ? 2 : 0).orElse(1);
    }

    // Evaluate move for critical patterns
    private int evaluateMove(int[][] board, int player, int x, int y) {
        int score = 0;
        int opponent = player == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
        int size = board.length;

        // Check board occupancy for center preference
        int occupiedCount = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != 0) occupiedCount++;
            }
        }

        // Strong center preference for empty or nearly empty board
        int center = size / 2;
        if (occupiedCount <= 1 && x == center && y == center) {
            score += 1500000;
        }

        if (board[x][y] != 0) return score;

        int[][] tempBoard = deepCopy(board);
        tempBoard[x][y] = opponent;
        if (hasFourOrOpenThree(tempBoard, x, y, opponent, true)) {
            return 5000000;
        }

        tempBoard = deepCopy(board);
        tempBoard[x][y] = player;
        if (hasFourOrOpenThree(tempBoard, x, y, player, true)) {
            return 4000000;
        }

        if (hasFourOrOpenThree(tempBoard, x, y, player, false)) {
            return 4000000;
        }

        tempBoard = deepCopy(board);
        tempBoard[x][y] = opponent;
        if (hasFourOrOpenThree(tempBoard, x, y, opponent, false)) {
            return 3000000;
        }

        tempBoard = deepCopy(board);
        tempBoard[x][y] = player;
        if (hasWinningPattern(tempBoard, x, y, player)) {
            return 1000000;
        }

        tempBoard = deepCopy(board);
        tempBoard[x][y] = opponent;
        if (hasWinningPattern(tempBoard, x, y, opponent)) {
            return 900000;
        }

        return score;
    }

    private Move<Gomoku> extractMove(State<Gomoku> rootState, State<Gomoku> childState) {
        int[][] rootGrid = ((GomokuState) rootState).getBoard();
        int[][] childGrid = ((GomokuState) childState).getBoard();
        for (int i = 0; i < rootGrid.length; i++) {
            for (int j = 0; j < rootGrid[i].length; j++) {
                if (rootGrid[i][j] != childGrid[i][j]) {
                    return new GomokuMove(rootState.player(), i, j);
                }
            }
        }
        return null;
    }

    private int getPreviousPlayer(State<Gomoku> state) {
        return state.player() == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
    }

    private int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
        }
        return copy;
    }
}