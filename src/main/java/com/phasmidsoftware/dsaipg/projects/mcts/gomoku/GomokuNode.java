package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import java.util.ArrayList;
import java.util.List;

import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public class GomokuNode implements SimpleNode<Gomoku> {

    private final State<Gomoku> state;
    private final List<SimpleNode<Gomoku>> children = new ArrayList<>();
    private int wins = 0;
    private int playouts = 0;

    public GomokuNode(State<Gomoku> state) {
        this.state = state;
    }

    @Override
    public State<Gomoku> state() {
        return state;
    }

    @Override
    public List<SimpleNode<Gomoku>> children() {
        return children;
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public void addChild(State<Gomoku> childState) {
        children.add(new GomokuNode(childState));
    }

    @Override
    public int wins() {
        return wins;
    }

    @Override
    public int playouts() {
        return playouts;
    }

    // 用于 MCTS 更新统计数据
    public void updateStats(int reward) {
        this.playouts++;
        this.wins += reward;
    }
}

