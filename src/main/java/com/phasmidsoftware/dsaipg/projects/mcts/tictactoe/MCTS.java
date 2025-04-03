package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;
import java.util.*;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;


public class MCTS {

    private final Node<TicTacToe> root;
    private static final double EXPLORATION_CONSTANT = sqrt(2);

    public MCTS(Node<TicTacToe> root) {
        this.root = root;
    }


    public Move<TicTacToe> findNextMove(int iterations) {
        for (int i = 0; i < iterations; i++) {
            List<Node<TicTacToe>> path = new ArrayList<>();
            Node<TicTacToe> node = root;
            path.add(node);
            while (!node.isLeaf() && !node.children().isEmpty()) {
                node = selectChild(node);
                path.add(node);
            }
            if (!node.state().isTerminal()) {
                expandNode(node);
                if (!node.children().isEmpty()) {
                    node = selectChild(node);
                    path.add(node);
                }
            }
            int simulationReward = simulatePlayout(node.state());
            for (Node<TicTacToe> n : path) {
                updateNode(n, simulationReward);
            }
        }

        Node<TicTacToe> bestChild = null;
        double bestScore = -Double.MAX_VALUE;
        for (Node<TicTacToe> child : root.children()) {
            double score = (double) child.wins() / child.playouts();
            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }
        return extractMove(root.state(), bestChild.state());
    }

    private Node<TicTacToe> selectChild(Node<TicTacToe> node) {
        Node<TicTacToe> best = null;
        double bestValue = -Double.MAX_VALUE;
        for (Node<TicTacToe> child : node.children()) {
            double exploitation = (double) child.wins() / (child.playouts() + 1e-6);
            double exploration = EXPLORATION_CONSTANT * sqrt(log(node.playouts() + 1) / (child.playouts() + 1e-6));
            double uctValue = exploitation + exploration;
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
        int currentPlayer = getPreviousPlayer(tempState);
        while (!tempState.isTerminal()) {
            Move<TicTacToe> move = tempState.chooseMove(tempState.player());
            tempState = tempState.next(move);
        }
        Optional<Integer> winnerOpt = tempState.winner();
        if (winnerOpt.isPresent()) {
            return (winnerOpt.get() == currentPlayer) ? 2 : 0;
        } else {
            return 1;
        }
    }

    private void updateNode(Node<TicTacToe> node, int reward) {
        if (node instanceof TicTacToeNode) {
            TicTacToeNode ttNode = (TicTacToeNode) node;
            ttNode.updateStats(reward);
        }
    }


    private Move<TicTacToe> extractMove(State<TicTacToe> rootState, State<TicTacToe> childState) {
        TicTacToe.TicTacToeState rootTState = (TicTacToe.TicTacToeState) rootState;
        TicTacToe.TicTacToeState childTState = (TicTacToe.TicTacToeState) childState;
        int[][] rootGrid = rootTState.position().getGrid();
        int[][] childGrid = childTState.position().getGrid();
        for (int i = 0; i < rootGrid.length; i++) {
            for (int j = 0; j < rootGrid[i].length; j++) {
                if (rootGrid[i][j] != childGrid[i][j]) {
                    int movePlayer = rootState.player();
                    return new TicTacToe.TicTacToeMove(movePlayer, i, j);
                }
            }
        }
        return null;
    }

    private int getPreviousPlayer(State<TicTacToe> state) {
        int nextPlayer = state.player();
        return (nextPlayer == TicTacToe.X) ? TicTacToe.O : TicTacToe.X;
    }


    public static void main(String[] args) {
        TicTacToe game = new TicTacToe();
        TicTacToe.TicTacToeState state = game.new TicTacToeState();
        System.out.println("Init:");
        System.out.println(state);
        System.out.println("---------------------");

        while (!state.isTerminal()) {
            int currentPlayer = state.player();
            Move<TicTacToe> move;
            if (currentPlayer == TicTacToe.X) {
                TicTacToeNode rootNode = new TicTacToeNode(state);
                MCTS mcts = new MCTS(rootNode);
                move = mcts.findNextMove(10000);
                System.out.println("MCTS (X) Choose " + Arrays.toString(((TicTacToe.TicTacToeMove) move).move()));
            } else {
                move = state.chooseMove(currentPlayer);
                System.out.println("Normal Player (O) Choose: " + Arrays.toString(((TicTacToe.TicTacToeMove) move).move()));
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
            System.out.println("Draw！");
        }
    }
}
