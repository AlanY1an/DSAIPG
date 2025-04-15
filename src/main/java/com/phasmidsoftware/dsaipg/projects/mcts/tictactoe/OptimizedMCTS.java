package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public class OptimizedMCTS {

    private final Node<TicTacToe> root;
    private static final double EXPLORATION_CONSTANT = sqrt(2);
    private static final double EPSILON = Double.MIN_VALUE;
    private static final double HEURISTIC_WEIGHT = 1;
    private final Random random = new Random();

    public OptimizedMCTS(Node<TicTacToe> root) {
        this.root = root;
    }

    public Move<TicTacToe> findNextMove(int iterations) {
        for (int i = 0; i < iterations; i++) {
            List<Node<TicTacToe>> path = tracePath(root);
            Node<TicTacToe> leaf = path.get(path.size() - 1);

            if (!leaf.state().isTerminal()) {
                expandNode(leaf);
                if (!leaf.children().isEmpty()) {
                    Node<TicTacToe> expanded = selectChild(leaf);
                    path.add(expanded);
                    leaf = expanded;
                }
            }

            int reward = simulatePlayout(leaf.state());

            for (Node<TicTacToe> node : path) {
                updateNode(node, reward);
            }
        }

        Node<TicTacToe> bestChild = null;
        double bestScore = -Double.MAX_VALUE;
        for (Node<TicTacToe> child : root.children()) {
            if (child.playouts() == 0) continue;
            double score = (double) child.wins() / child.playouts();
            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        if (bestChild == null) return null;
        return extractMove(root.state(), bestChild.state());
    }

    private List<Node<TicTacToe>> tracePath(Node<TicTacToe> node) {
        List<Node<TicTacToe>> path = new ArrayList<>();
        path.add(node);
        while (!node.isLeaf() && !node.children().isEmpty()) {
            node = selectChild(node);
            path.add(node);
        }
        return path;
    }

    private int[] getMoveDiff(State<TicTacToe> before, State<TicTacToe> after) {
        int[][] a = ((TicTacToe.TicTacToeState) before).position().getGrid();
        int[][] b = ((TicTacToe.TicTacToeState) after).position().getGrid();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (a[i][j] != b[i][j]) return new int[]{i, j};
            }
        }
        return null;
    }


    private Node<TicTacToe> selectChild(Node<TicTacToe> node) {
        Node<TicTacToe> best = null;
        double bestValue = -Double.MAX_VALUE;
        for (Node<TicTacToe> child : node.children()) {
            double exploitation = (double) child.wins() / (child.playouts() + EPSILON);
            double exploration = EXPLORATION_CONSTANT *
                    sqrt(log(node.playouts() + 1) / (child.playouts() + EPSILON));
            double heuristic = getHeuristicScore(child.state(), node.state().player());
            double uctValue = exploitation + exploration + HEURISTIC_WEIGHT * heuristic;
            if (uctValue > bestValue) {
                bestValue = uctValue;
                best = child;
            }
        }
        return best;
    }

    private void expandNode(Node<TicTacToe> node) {
        Collection<Move<TicTacToe>> moves = node.state().moves(node.state().player());
        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> newState = node.state().next(move);
            node.addChild(newState);
        }
    }

    private int simulatePlayout(State<TicTacToe> state) {
        State<TicTacToe> tempState = state;
        int perspectivePlayer = root.state().player(); // Always from X's perspective

        while (!tempState.isTerminal()) {
            Collection<Move<TicTacToe>> moves = tempState.moves(tempState.player());
            if (moves.isEmpty()) break;

            Move<TicTacToe> move;
            if (random.nextDouble() < 0.5) {
                // Use heuristic move 50% of the time
                move = getBestHeuristicMove(tempState, moves);
            } else {
                // Use random move 50% of the time
                List<Move<TicTacToe>> moveList = new ArrayList<>(moves);
                move = moveList.get(random.nextInt(moveList.size()));
            }

            tempState = tempState.next(move);
        }

        Optional<Integer> winnerOpt = tempState.winner();
        if (!winnerOpt.isPresent()) return 1; // Draw
        if (winnerOpt.get() == perspectivePlayer) return 2; // Win
        return 0; // Loss
    }


    private Move<TicTacToe> getBestHeuristicMove(State<TicTacToe> state, Collection<Move<TicTacToe>> moves) {
        int currentPlayer = state.player();
        int opponent = (currentPlayer == TicTacToe.X) ? TicTacToe.O : TicTacToe.X;

        // Priority 1: Win immediately
        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> next = state.next(move);
            if (next.isTerminal() && next.winner().isPresent() && next.winner().get() == currentPlayer) {
                return move;
            }
        }

        // Priority 2: Block opponent's win
        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> next = state.next(move);
            int[] diff = getMoveDiff(state, next);
            if (diff != null) {
                int i = diff[0], j = diff[1];
                int[][] testGrid = ((TicTacToe.TicTacToeState) state).position().getGrid();
                int[][] clone = new int[3][3];
                for (int x = 0; x < 3; x++) System.arraycopy(testGrid[x], 0, clone[x], 0, 3);
                clone[i][j] = opponent;
                if (isWinningGrid(clone, opponent)) {
                    return move;
                }
            }
        }


        // Priority 3: Center (1,1)

        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> next = state.next(move);
            int[] diff = getMoveDiff(state, next);
            if (diff != null && diff[0] == 1 && diff[1] == 1) {
                return move;
            }
        }


        // Priority 4: Corners (0,0), (0,2), (2,0), (2,2)
        List<Move<TicTacToe>> corners = new ArrayList<>();
        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> next = state.next(move);
            int[] diff = getMoveDiff(state, next);
            if (diff != null) {
                int i = diff[0], j = diff[1];
                if ((i == 0 && j == 0) || (i == 0 && j == 2) ||
                        (i == 2 && j == 0) || (i == 2 && j == 2)) {
                    corners.add(move);
                }
            }
        }
        if (!corners.isEmpty()) {
            return corners.get(random.nextInt(corners.size()));
        }


        // Priority 5: Edges
        List<Move<TicTacToe>> edges = new ArrayList<>(moves);
        return edges.get(random.nextInt(edges.size()));
    }

    // Helper: Check if grid has a win for player
    private boolean isWinningGrid(int[][] grid, int player) {
        // Rows
        for (int i = 0; i < 3; i++) {
            if (grid[i][0] == player && grid[i][1] == player && grid[i][2] == player) return true;
        }
        // Columns
        for (int j = 0; j < 3; j++) {
            if (grid[0][j] == player && grid[1][j] == player && grid[2][j] == player) return true;
        }
        // Diagonals
        if (grid[0][0] == player && grid[1][1] == player && grid[2][2] == player) return true;
        if (grid[0][2] == player && grid[1][1] == player && grid[2][0] == player) return true;
        return false;
    }

    private double getHeuristicScore(State<TicTacToe> state, int perspectivePlayer) {
        int currentPlayer = state.player();
        int opponent = (perspectivePlayer == TicTacToe.X) ? TicTacToe.O : TicTacToe.X;
        double score = 0.0;

        if (state.isTerminal()) {
            if (state.winner().isPresent()) {
                if (state.winner().get() == perspectivePlayer) return 100.0;
                else return -100.0;
            }
            return 0.0;
        }

        int[][] grid = ((TicTacToe.TicTacToeState) state).position().getGrid();
        // Center
        if (grid[1][1] == perspectivePlayer) score += 5.0;
        // Corners
        if (grid[0][0] == perspectivePlayer) score += 3.0;
        if (grid[0][2] == perspectivePlayer) score += 3.0;
        if (grid[2][0] == perspectivePlayer) score += 3.0;
        if (grid[2][2] == perspectivePlayer) score += 3.0;

        // Check for blocking opponent's win
        Collection<Move<TicTacToe>> moves = state.moves(currentPlayer);
        for (Move<TicTacToe> move : moves) {
            int[][] currentGrid = ((TicTacToe.TicTacToeState) state).position().getGrid();
            int[][] nextGrid = ((TicTacToe.TicTacToeState) state.next(move)).position().getGrid();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (currentGrid[i][j] != nextGrid[i][j]) {
                        int[][] testGrid = new int[3][3];
                        for (int x = 0; x < 3; x++) {
                            for (int y = 0; y < 3; y++) {
                                testGrid[x][y] = currentGrid[x][y];
                            }
                        }
                        testGrid[i][j] = opponent;
                        if (isWinningGrid(testGrid, opponent)) {
                            score += 50.0;
                        }
                    }
                }
            }
        }

        return score;
    }

    private void updateNode(Node<TicTacToe> node, int reward) {
        if (node instanceof TicTacToeNode) {
            ((TicTacToeNode) node).updateStats(reward);
        }
    }

    private Move<TicTacToe> extractMove(State<TicTacToe> rootState, State<TicTacToe> childState) {
        int[][] rootGrid = ((TicTacToe.TicTacToeState) rootState).position().getGrid();
        int[][] childGrid = ((TicTacToe.TicTacToeState) childState).position().getGrid();
        for (int i = 0; i < rootGrid.length; i++) {
            for (int j = 0; j < rootGrid[i].length; j++) {
                if (rootGrid[i][j] != childGrid[i][j]) {
                    return new TicTacToe.TicTacToeMove(rootState.player(), i, j);
                }
            }
        }
        return null;
    }

    private int getPreviousPlayer(State<TicTacToe> state) {
        return (state.player() == TicTacToe.X) ? TicTacToe.O : TicTacToe.X;
    }

    public static void main(String[] args) {
        TicTacToe game = new TicTacToe();
        TicTacToe.TicTacToeState state = game.new TicTacToeState();
        System.out.println("Init:");
        System.out.println(state);
        System.out.println("---------------------");

        Move<TicTacToe> firstMove = state.chooseMove(TicTacToe.O);
        System.out.println("Normal Player (O) First Move: " + ((TicTacToe.TicTacToeMove) firstMove).toString());
        state = (TicTacToe.TicTacToeState) state.next(firstMove);
        System.out.println(state);
        System.out.println("---------------------");

        while (!state.isTerminal()) {
            int currentPlayer = state.player();
            Move<TicTacToe> move;
            if (currentPlayer == TicTacToe.X) {
                TicTacToeNode rootNode = new TicTacToeNode(state);
                OptimizedMCTS mcts = new OptimizedMCTS(rootNode);
                move = mcts.findNextMove(50000); // Increased iterations
                System.out.println("Optimized MCTS (X) Choose " + ((TicTacToe.TicTacToeMove) move).toString());
            } else {
                move = state.chooseMove(currentPlayer);
                System.out.println("Normal Player (O) Choose: " + ((TicTacToe.TicTacToeMove) move).toString());
            }
            state = (TicTacToe.TicTacToeState) state.next(move);
            System.out.println(state);
            System.out.println("---------------------");
        }

        Optional<Integer> winner = state.winner();
        if (winner.isPresent()) {
            String winStr = (winner.get() == TicTacToe.X) ? "Optimized MCTS (X)" : "Normal Player (O)";
            System.out.println("Winner: " + winStr);
        } else {
            System.out.println("Draw!");
        }
    }
}