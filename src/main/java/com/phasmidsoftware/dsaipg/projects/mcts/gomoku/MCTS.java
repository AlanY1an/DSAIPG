package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.*;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

public class MCTS {

    private final SimpleNode<Gomoku> root;
    private static final double EXPLORATION_CONSTANT = sqrt(2);
    private static final double EPSILON = 1e-6;
    private static final Random random = new Random();

    public MCTS(SimpleNode<Gomoku> root) {
        this.root = root;
    }

    public Move<Gomoku> findNextMove(int iterations) {
        for (int i = 0; i < iterations; i++) {
            // 1. Selection: traverse the tree using UCT to reach a leaf node
            List<SimpleNode<Gomoku>> path = tracePath(root);
            SimpleNode<Gomoku> leaf = path.get(path.size() - 1);

            // 2. Expansion: expand the leaf node if it's not terminal
            if (!leaf.state().isTerminal()) {
                expandNode(leaf);
                if (!leaf.children().isEmpty()) {
                    SimpleNode<Gomoku> expanded = selectChild(leaf);
                    path.add(expanded);
                    leaf = expanded;
                }
            }

            // 3. Simulation: run a random playout from the leaf node
            int reward = simulatePlayout(leaf.state());

            // 4. Backpropagation: update statistics along the traversed path
            for (SimpleNode<Gomoku> node : path) {
                updateNode(node, reward);
            }
        }

        // After all iterations, choose the best child based on win rate
        SimpleNode<Gomoku> bestChild = null;
        double bestScore = -Double.MAX_VALUE;
        for (SimpleNode<Gomoku> child : root.children()) {
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

    // Selection phase: follow UCT to trace a path to a promising leaf
    private List<SimpleNode<Gomoku>> tracePath(SimpleNode<Gomoku> node) {
        List<SimpleNode<Gomoku>> path = new ArrayList<>();
        path.add(node);
        while (!node.isLeaf() && !node.children().isEmpty()) {
            node = selectChild(node);
            path.add(node);
        }
        return path;
    }

    // Select child node based on UCT value
    private SimpleNode<Gomoku> selectChild(SimpleNode<Gomoku> node) {
        SimpleNode<Gomoku> best = null;
        double bestValue = -Double.MAX_VALUE;
        for (SimpleNode<Gomoku> child : node.children()) {
            double exploitation = (double) child.wins() / (child.playouts() + EPSILON);
            double exploration = EXPLORATION_CONSTANT *
                    sqrt(log(node.playouts() + 1) / (child.playouts() + EPSILON));
            double uctValue = exploitation + exploration;
            if (uctValue > bestValue) {
                bestValue = uctValue;
                best = child;
            }
        }
        return best;
    }

    // Expansion phase: add all possible moves as child nodes
    private void expandNode(SimpleNode<Gomoku> node) {
        Collection<Move<Gomoku>> moves = node.state().moves(node.state().player());
        for (Move<Gomoku> move : moves) {
            State<Gomoku> newState = node.state().next(move);
            node.addChild(newState);
        }
    }

    // Simulation phase: play random moves near existing stones
    private int simulatePlayout(State<Gomoku> state) {
        State<Gomoku> tempState = state;
        int currentPlayer = getPreviousPlayer(tempState);
        int maxSteps = 50; // Limit to avoid infinite games

        int steps = 0;
        while (!tempState.isTerminal() && steps < maxSteps) {
            Move<Gomoku> move = chooseMoveNearStones(tempState, 2);
            tempState = tempState.next(move);
            steps++;
        }
        Optional<Integer> winnerOpt = tempState.winner();
        if (!winnerOpt.isPresent()) return 1; // Draw
        if (winnerOpt.get() == currentPlayer) return 2; // Win
        return 0; // Loss
    }

    // Update node's statistics during backpropagation
    private void updateNode(SimpleNode<Gomoku> node, int reward) {
        node.updateStats(reward);
    }

    // Extract the move that led from rootState to childState
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

    // Get the player who moved previously
    private int getPreviousPlayer(State<Gomoku> state) {
        return (state.player() == Gomoku.PLAYER_ONE) ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
    }

    // Choose a random move near existing stones within a specified range
    private Move<Gomoku> chooseMoveNearStones(State<Gomoku> state, int range) {
        int[][] board = ((GomokuState) state).getBoard();
        List<int[]> candidates = new ArrayList<>();
        int size = board.length;

        boolean[][] mark = new boolean[size][size];

        // Mark all empty cells within 'range' of existing stones
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != 0) {
                    for (int dx = -range; dx <= range; dx++) {
                        for (int dy = -range; dy <= range; dy++) {
                            int ni = i + dx;
                            int nj = j + dy;
                            if (ni >= 0 && nj >= 0 && ni < size && nj < size && board[ni][nj] == 0) {
                                mark[ni][nj] = true;
                            }
                        }
                    }
                }
            }
        }

        // Collect all marked empty cells
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (mark[i][j]) {
                    candidates.add(new int[]{i, j});
                }
            }
        }

        // If no nearby empty cells found, pick a random move on the whole board
        if (candidates.isEmpty()) {
            return state.chooseMove(state.player());
        } else {
            int[] movePos = candidates.get(random.nextInt(candidates.size()));
            return new GomokuMove(state.player(), movePos[0], movePos[1]);
        }
    }

}
