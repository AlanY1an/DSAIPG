package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Game;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public class Gomoku implements Game {
    public static final int PLAYER_ONE = 1;
    public static final int PLAYER_TWO = 2;

    @Override
    public State<Gomoku> start() {
        return new GomokuState();
    }

    @Override
    public int opener() {
        return PLAYER_ONE;
    }
}
