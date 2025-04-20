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
    private static final double BASE_EXPLORATION_CONSTANT = 1.4;
    private static final double EPSILON = 1e-6;
    private static final Random random = new Random();

    // Tuning parameters
    private static final int MAX_EXPANSIONS = 5;
    private static final int MAX_PLAYOUT_STEPS = 20;
    private static final int EARLY_GAME_STONES = 8;
    private static final int CENTER_RADIUS = 4;

    public OptimizedMCTS(SimpleNode<Gomoku> root) {
        this.root = root;
    }

    public Move<Gomoku> findNextMove(int iterations) {
        // Early game: if board empty, play center
        GomokuState rootState = (GomokuState) root.state();
        int[][] board = rootState.getBoard();
        boolean empty = true;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell != 0) { empty = false; break; }
            }
            if (!empty) break;
        }
        if (empty) {
            int center = board.length / 2;
            return new GomokuMove(rootState.player(), center, center);
        }

        // Standard MCTS iterations
        for (int i = 0; i < iterations; i++) {
            List<SimpleNode<Gomoku>> path = tracePath(root);
            SimpleNode<Gomoku> leaf = path.get(path.size() - 1);

            if (!leaf.state().isTerminal()) {
                expandNode(leaf);
                if (!leaf.children().isEmpty()) {
                    SimpleNode<Gomoku> next = selectChild(leaf);
                    path.add(next);
                    leaf = next;
                }
            }

            int reward = simulatePlayout(leaf.state());
            for (SimpleNode<Gomoku> node : path) {
                node.updateStats(reward);
            }
        }

        // Select best child by win rate
        SimpleNode<Gomoku> best = null;
        double bestRate = -1;
        for (SimpleNode<Gomoku> c : root.children()) {
            if (c.playouts() == 0) continue;
            double rate = (double) c.wins() / c.playouts();
            if (rate > bestRate) {
                bestRate = rate;
                best = c;
            }
        }
        if (best != null) return extractMove(root.state(), best.state());
        return rootState.chooseMove(rootState.player());
    }

    // Tree traversal
    private List<SimpleNode<Gomoku>> tracePath(SimpleNode<Gomoku> node) {
        List<SimpleNode<Gomoku>> path = new ArrayList<>();
        path.add(node);
        while (!node.isLeaf() && !node.children().isEmpty()) {
            node = selectChild(node);
            path.add(node);
        }
        return path;
    }

    private SimpleNode<Gomoku> selectChild(SimpleNode<Gomoku> node) {
        double total = node.playouts() + 1;
        SimpleNode<Gomoku> best = null;
        double bestVal = Double.NEGATIVE_INFINITY;
        for (SimpleNode<Gomoku> c : node.children()) {
            double w = (double) c.wins() / (c.playouts() + EPSILON);
            double u = BASE_EXPLORATION_CONSTANT * sqrt(log(total) / (c.playouts() + EPSILON));
            double bias = centerBias(c.state());
            double val = w + u + bias;
            if (val > bestVal) {
                bestVal = val;
                best = c;
            }
        }
        return best;
    }

    private void expandNode(SimpleNode<Gomoku> node) {
        int count = 0;
        for (Move<Gomoku> m : node.state().moves(node.state().player())) {
            if (count++ >= MAX_EXPANSIONS) break;
            node.addChild(node.state().next(m));
        }
    }

    // Simulation
    private int simulatePlayout(State<Gomoku> state) {
        State<Gomoku> s = state;
        int prev = opponent(s.player());
        int steps = 0;
        while (!s.isTerminal() && steps++ < MAX_PLAYOUT_STEPS) {
            s = s.next(chooseSmartMove(s));
        }
        Optional<Integer> w = s.winner();
        return w.map(win -> (win == prev ? 2 : 0)).orElse(1);
    }

    // Heuristic policy
    private Move<Gomoku> chooseSmartMove(State<Gomoku> state) {
        int player = state.player();
        // win or block
        for (Move<Gomoku> m : state.moves(player)) {
            State<Gomoku> ns = state.next(m);
            if (ns.isTerminal()) {
                Optional<Integer> w = ns.winner();
                if (w.isPresent()) {
                    if (w.get() == player) return m;
                    if (w.get() == opponent(player)) return m;
                }
            }
        }
        // early game center bias
        int stones = countStones(((GomokuState) state).getBoard());
        if (stones < EARLY_GAME_STONES) return chooseFromCenter(state);
        // neighbor heuristic
        return neighborHeuristic(state);
    }

    private Move<Gomoku> chooseFromCenter(State<Gomoku> state) {
        int[][] b = ((GomokuState) state).getBoard();
        int n = b.length, mid = n / 2;
        List<int[]> cands = new ArrayList<>();
        for (int i = mid - CENTER_RADIUS; i <= mid + CENTER_RADIUS; i++) {
            for (int j = mid - CENTER_RADIUS; j <= mid + CENTER_RADIUS; j++) {
                if (i >= 0 && j >= 0 && i < n && j < n && b[i][j] == 0) cands.add(new int[]{i, j});
            }
        }
        if (!cands.isEmpty()) {
            int[] p = cands.get(random.nextInt(cands.size()));
            return new GomokuMove(state.player(), p[0], p[1]);
        }
        return state.chooseMove(state.player());
    }

    private Move<Gomoku> neighborHeuristic(State<Gomoku> state) {
        int[][] b = ((GomokuState) state).getBoard();
        int n = b.length, player = state.player();
        boolean[][] mark = new boolean[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) if (b[i][j] != 0)
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                int x = i + dx, y = j + dy;
                if (x >= 0 && y >= 0 && x < n && y < n && b[x][y] == 0) mark[x][y] = true;
            }
        int best = -1; List<int[]> opts = new ArrayList<>();
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) if (mark[i][j]) {
            int sc = evaluateMove(b, player, i, j);
            if (sc > best) { best = sc; opts.clear(); opts.add(new int[]{i, j}); }
            else if (sc == best) opts.add(new int[]{i, j});
        }
        if (!opts.isEmpty()) {
            int[] p = opts.get(random.nextInt(opts.size()));
            return new GomokuMove(player, p[0], p[1]);
        }
        return state.chooseMove(player);
    }

    // Utilities
    private int opponent(int p) { return p == Gomoku.PLAYER_ONE ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE; }
    private int countStones(int[][] b) { int c = 0; for (int[] r : b) for (int v : r) if (v != 0) c++; return c; }
    private Move<Gomoku> extractMove(State<Gomoku> rs, State<Gomoku> cs) {
        int[][] a = ((GomokuState) rs).getBoard(), b = ((GomokuState) cs).getBoard();
        for (int i = 0; i < a.length; i++) for (int j = 0; j < a.length; j++)
            if (a[i][j] != b[i][j]) return new GomokuMove(rs.player(), i, j);
        return null;
    }
    private double centerBias(State<Gomoku> s) {
        int[][] curr = ((GomokuState) s).getBoard();
        int[][] rootB = ((GomokuState) root.state()).getBoard();
        int x = -1, y = -1;
        for (int i = 0; i < curr.length; i++) {
            for (int j = 0; j < curr.length; j++) {
                if (curr[i][j] != rootB[i][j]) { x = i; y = j; break; }
            }
            if (x != -1) break;
        }
        if (x < 0) return 0;
        double mid = (curr.length - 1) / 2.0;
        double d = Math.hypot(x - mid, y - mid);
        return Math.max(0, (CENTER_RADIUS - d) / CENTER_RADIUS);
    }
    private int evaluateMove(int[][] b, int p, int x, int y) {
        int opp = opponent(p);
        int[] dx = {1, 0, 1, 1}, dy = {0, 1, 1, -1};
        int sc = 0;
        for (int d = 0; d < 4; d++) {
            int co = 0, cp = 0;
            for (int k = -2; k <= 2; k++) {
                int xi = x + dx[d] * k, yi = y + dy[d] * k;
                if (xi >= 0 && yi >= 0 && xi < b.length && yi < b.length) {
                    if (b[xi][yi] == p) co++;
                    if (b[xi][yi] == opp) cp++;
                }
            }
            sc += co * 2 + cp;
        }
        return sc;
    }
}
