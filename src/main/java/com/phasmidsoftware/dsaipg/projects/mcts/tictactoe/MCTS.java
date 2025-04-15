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
            // 1. Selection: 从根节点开始，用 UCT 算法寻找一条路径直到叶节点
            List<Node<TicTacToe>> path = tracePath(root);
            Node<TicTacToe> leaf = path.get(path.size() - 1);

            // 2. Expansion: 如果叶节点不是终局状态，则扩展该节点
            if (!leaf.state().isTerminal()) {
                expandNode(leaf);
                if (!leaf.children().isEmpty()) {
                    Node<TicTacToe> expanded = selectChild(leaf);
                    path.add(expanded);
                    leaf = expanded;
                }
            }

            // 3. Simulation: 对扩展后节点做随机 playout 模拟
            int reward = simulatePlayout(leaf.state());

            // 4. Backpropagation: 将 simulation 得到的奖励沿路径向上传递更新统计数据
            for (Node<TicTacToe> node : path) {
                updateNode(node, reward);
            }
        }

        // 最后选择访问次数最高且胜率最高的子节点
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

    // Selection：沿路径使用 UCT 选择节点
    private List<Node<TicTacToe>> tracePath(Node<TicTacToe> node) {
        List<Node<TicTacToe>> path = new ArrayList<>();
        path.add(node);
        while (!node.isLeaf() && !node.children().isEmpty()) {
            node = selectChild(node);
            path.add(node);
        }
        return path;
    }

    // UCT 选择子节点
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

    // Expansion：为当前节点生成所有可能的下一步状态
    private void expandNode(Node<TicTacToe> node) {
        Collection<Move<TicTacToe>> moves = node.state().moves(node.state().player());
        for (Move<TicTacToe> move : moves) {
            State<TicTacToe> newState = node.state().next(move);
            node.addChild(newState);
        }
    }

    // Simulation：用纯随机策略进行 playout 模拟
    private int simulatePlayout(State<TicTacToe> state) {
        State<TicTacToe> tempState = state;
        int startingPlayer = getPreviousPlayer(state);
        // 直到达到终局状态
        while (!tempState.isTerminal()) {
            Collection<Move<TicTacToe>> moves = tempState.moves(tempState.player());
            if (moves.isEmpty()) break;
            List<Move<TicTacToe>> moveList = new ArrayList<>(moves);
            Move<TicTacToe> move = moveList.get(random.nextInt(moveList.size()));
            tempState = tempState.next(move);
        }
        Optional<Integer> winnerOpt = tempState.winner();
        // 奖励设定：赢=2，平局=1，输=0。注意这里“赢”是相对于刚刚落子的一方。
        if (!winnerOpt.isPresent()) return 1; // 平局
        if (winnerOpt.get() == startingPlayer) return 2; // 当前玩家获胜
        return 0; // 失败
    }

    // Backpropagation：更新节点统计数据
    private void updateNode(Node<TicTacToe> node, int reward) {
        if (node instanceof TicTacToeNode) {
            ((TicTacToeNode) node).updateStats(reward);
        }
    }

    // 从根状态和子状态比较差异提取实际落子
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

    // 获取上一步移动的玩家
    private int getPreviousPlayer(State<TicTacToe> state) {
        return (state.player() == TicTacToe.X) ? TicTacToe.O : TicTacToe.X;
    }

    // 主方法：MCTS (X) 对抗随机 (O)
    public static void main(String[] args) {
        TicTacToe game = new TicTacToe();
        TicTacToe.TicTacToeState state = game.new TicTacToeState();
        System.out.println("Init:");
        System.out.println(state);
        System.out.println("---------------------");

        // 让 O 先走，随机玩家先行
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
                move = state.chooseMove(currentPlayer); // 随机策略用于 O
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

