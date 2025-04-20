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

public class MCTS {

    private final Node<TicTacToe> root;
    private static final double EXPLORATION_CONSTANT = sqrt(2);
    private static final double EPSILON = Double.MIN_VALUE;
    private final Random random = new Random();

    public MCTS(Node<TicTacToe> root) {
        this.root = root;
    }

    public Move<TicTacToe> findNextMove(int iterations) {
        for (int i = 0; i < iterations; i++) {
            // 1. Selection: Start from root and use UCT to select a path down to a leaf node
            List<Node<TicTacToe>> path = tracePath(root);
            Node<TicTacToe> leaf = path.get(path.size() - 1);

            // 2. Expansion: If leaf is non-terminal, expand it
            if (!leaf.state().isTerminal()) {
                expandNode(leaf);
                if (!leaf.children().isEmpty()) {
                    Node<TicTacToe> expanded = selectChild(leaf);
                    path.add(expanded);
                    leaf = expanded;
                }
            }

            // 3. Simulation: Perform a random playout from the expanded node
            int reward = simulatePlayout(leaf.state());

            // 4. Backpropagation: Update statistics along the path with the simulation result
            for (Node<TicTacToe> node : path) {
                updateNode(node, reward);
            }
        }

        // Finally, select the child node with the highest win rate
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

    // Selection: Trace a path using UCT
    private List<Node<TicTacToe>> tracePath(Node<TicTacToe> node) {
        List<Node<TicTacToe>> path = new ArrayList<>();
        path.add(node);
        while (!node.isLeaf() && !node.children().isEmpty()) {
            node = selectChild(node);
            path.add(node);
        }
        return path;
    }

    // Select child based on UCT formula
    private Node<TicTacToe> selectChild(Node<TicTacToe> node) {
        Node<TicTacToe> best = null;
        double bestValue = -Double.MAX_VALUE;
        for (Node<TicTacToe> child : node.children()) {
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

    // Expansion: Expand the current node by generating all possible next moves
    private void expandNode(Node<TicTacToe> node) {
        Collection<Move<TicTacToe>> moves = node.state().moves(node.state().player());
        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> newState = node.state().next(move);
            node.addChild(newState);
        }
    }

    // Simulation: Perform a random playout
    private int simulatePlayout(State<TicTacToe> state) {
        State<TicTacToe> tempState = state;
        int startingPlayer = getPreviousPlayer(state);
        // Continue until the game reaches a terminal state
        while (!tempState.isTerminal()) {
            Collection<Move<TicTacToe>> moves = tempState.moves(tempState.player());
            if (moves.isEmpty()) break;
            List<Move<TicTacToe>> moveList = new ArrayList<>(moves);
            Move<TicTacToe> move = moveList.get(random.nextInt(moveList.size()));
            tempState = tempState.next(move);
        }
        Optional<Integer> winnerOpt = tempState.winner();
        // Reward scheme: Win = 2, Draw = 1, Loss = 0 (relative to the player who made the last move)
        if (!winnerOpt.isPresent()) return 1; // Draw
        if (winnerOpt.get() == startingPlayer) return 2; // Win
        return 0; // Loss
    }

    // Backpropagation: Update node statistics
    private void updateNode(Node<TicTacToe> node, int reward) {
        if (node instanceof TicTacToeNode) {
            ((TicTacToeNode) node).updateStats(reward);
        }
    }

    // Extract the move by comparing root state and child state
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

    // Get the previous player based on current turn
    private int getPreviousPlayer(State<TicTacToe> state) {
        return (state.player() == TicTacToe.X) ? TicTacToe.O : TicTacToe.X;
    }

    // Main method: MCTS (X) vs Random (O)
    public static void main(String[] args) {
        TicTacToe game = new TicTacToe();
        TicTacToe.TicTacToeState state = game.new TicTacToeState();
        System.out.println("Init:");
        System.out.println(state);
        System.out.println("---------------------");

        // Let O (Random) move first
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
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(50000);
                System.out.println("MCTS (X) Choose " + ((TicTacToe.TicTacToeMove) move).toString());
            } else {
                move = state.chooseMove(currentPlayer); // Random move for O
                System.out.println("Normal Player (O) Choose: " + ((TicTacToe.TicTacToeMove) move).toString());
            }
            state = (TicTacToe.TicTacToeState) state.next(move);
            System.out.println(state);
            System.out.println("---------------------");
        }

        Optional<Integer> winner = state.winner();
        if (winner.isPresent()) {
            String winStr = (winner.get() == TicTacToe.X) ? "MCTS (X)" : "Normal Player (O)";
            System.out.println("Winner: " + winStr);
        } else {
            System.out.println("Draw!");
        }
    }
}
