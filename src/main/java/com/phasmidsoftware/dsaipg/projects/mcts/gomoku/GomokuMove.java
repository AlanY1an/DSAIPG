package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;

public class GomokuMove implements Move<Gomoku> {
    private final int player;
    private final int row;
    private final int col;

    public GomokuMove(int player, int row, int col) {
        this.player = player;
        this.row = row;
        this.col = col;
    }

    @Override
    public int player() {
        return player;
    }

    public int[] move() {
        return new int[]{row, col};
    }

    @Override
    public String toString() {
        return "[" + row + ", " + col + "]";
    }

    public int getPlayer() {
        return player;
    }
}

