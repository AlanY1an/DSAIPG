// SimpleNode.java
package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import java.util.List;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Game;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public interface SimpleNode<G extends Game> {
    State<G> state();
    List<SimpleNode<G>> children();
    boolean isLeaf();
    void addChild(State<G> state);
    int wins();
    int playouts();
    void updateStats(int reward);
}

