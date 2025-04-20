package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public class GomokuState implements State<Gomoku> {
    public static final int SIZE = 15;
    private final int[][] board;
    private final int currentPlayer;

    public GomokuState() {
        this.board = new int[SIZE][SIZE];
        this.currentPlayer = Gomoku.PLAYER_ONE;
    }

    public GomokuState(int[][] board, int currentPlayer) {
        this.board = board;
        this.currentPlayer = currentPlayer;
    }

    @Override
    public int player() {
        return currentPlayer;
    }

    @Override
    public Collection<Move<Gomoku>> moves(int player) {
        List<Move<Gomoku>> result = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    result.add(new GomokuMove(player, i, j));
                }
            }
        }
        return result;
    }

    @Override
    public State<Gomoku> next(Move<Gomoku> move) {
        GomokuMove gm = (GomokuMove) move;
        int[][] newBoard = deepCopy(board);
        newBoard[gm.move()[0]][gm.move()[1]] = gm.getPlayer();
        int nextPlayer = (currentPlayer == Gomoku.PLAYER_ONE) ? Gomoku.PLAYER_TWO : Gomoku.PLAYER_ONE;
        return new GomokuState(newBoard, nextPlayer);
    }

    @Override
    public boolean isTerminal() {
        return winner().isPresent() || moves(currentPlayer).isEmpty();
    }

    @Override
    public Optional<Integer> winner() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int player = board[i][j];
                if (player != 0 && hasFiveInRow(i, j, player)) {
                    return Optional.of(player);
                }
            }
        }
        return Optional.empty();
    }

    private boolean hasFiveInRow(int row, int col, int player) {
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int count = 1;
            for (int k = 1; k < 5; k++) {
                int x = row + d[0] * k;
                int y = col + d[1] * k;
                if (x < 0 || y < 0 || x >= SIZE || y >= SIZE || board[x][y] != player) break;
                count++;
            }
            if (count == 5) return true;
        }
        return false;
    }

    private int[][] deepCopy(int[][] original) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int[] row : board) {
            for (int cell : row) {
                sb.append(cell == 0 ? ". " : (cell == 1 ? "X " : "O "));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    @Override
        public Gomoku game() {
            return new Gomoku();
        }

    @Override
        public Random random() {
            return new Random();
        }
    
        public int[][] getBoard() {
            return board;
        }
}
