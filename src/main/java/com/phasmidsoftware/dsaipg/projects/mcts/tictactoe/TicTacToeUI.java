package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class TicTacToeUI extends JFrame {
    private final TicTacToe game;
    private TicTacToe.TicTacToeState currentState;
    private final JButton[][] buttons;
    private final JLabel statusLabel;
    private final JComboBox<String> difficultyComboBox;
    private final JRadioButton playerFirstRadio;
    private final JRadioButton aiFirstRadio;
    private final JButton newGameButton;
    private boolean playerTurn = true;
    private int playerMark = TicTacToe.O;
    private int aiMark = TicTacToe.X;
    private String difficulty = "normal";
    private int iterations = 10000;

    public TicTacToeUI() {
        super("Tic-Tac-Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 600);
        setMinimumSize(new Dimension(400, 550));
        setLocationRelativeTo(null);

        // Gradient background panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(94, 234, 212), 0, getHeight(), new Color(52, 143, 226));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        game = new TicTacToe();
        currentState = game.new TicTacToeState();

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Difficulty selector
        JLabel difficultyLabel = new JLabel("Difficulty:");
        difficultyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        difficultyLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(difficultyLabel, gbc);

        difficultyComboBox = new JComboBox<>(new String[]{"Normal", "Hard", "Very Hard"});
        styleComboBox(difficultyComboBox);
        difficultyComboBox.addActionListener(e -> {
            difficulty = difficultyComboBox.getSelectedItem().toString().toLowerCase();
            iterations = switch (difficulty) {
                case "hard" -> 10000;
                case "very hard" -> 50000;
                default -> 1000;
            };
        });
        gbc.gridx = 1;
        gbc.gridy = 0;
        controlPanel.add(difficultyComboBox, gbc);

        // First player selector
        JLabel firstPlayerLabel = new JLabel("First Move:");
        firstPlayerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        firstPlayerLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(firstPlayerLabel, gbc);

        JPanel firstPlayerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        firstPlayerPanel.setOpaque(false);
        playerFirstRadio = new JRadioButton("Player (O)", true);
        aiFirstRadio = new JRadioButton("AI (X)");
        styleRadioButton(playerFirstRadio);
        styleRadioButton(aiFirstRadio);
        ButtonGroup firstPlayerGroup = new ButtonGroup();
        firstPlayerGroup.add(playerFirstRadio);
        firstPlayerGroup.add(aiFirstRadio);
        firstPlayerPanel.add(playerFirstRadio);
        firstPlayerPanel.add(aiFirstRadio);
        gbc.gridx = 1;
        gbc.gridy = 1;
        controlPanel.add(firstPlayerPanel, gbc);

        // New game button
        newGameButton = new JButton("New Game");
        newGameButton.setForeground(Color.BLACK);
        styleButton(newGameButton);
        newGameButton.addActionListener(e -> resetGame());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        controlPanel.add(newGameButton, gbc);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        boardPanel.setOpaque(false);
        buttons = new JButton[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i;
                final int col = j;
                buttons[i][j] = new JButton("");
                styleBoardButton(buttons[i][j]);
                buttons[i][j].addActionListener(e -> handlePlayerMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }

        mainPanel.add(boardPanel, BorderLayout.CENTER);

        // Status label
        statusLabel = new JLabel("Select options and start a new game", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(new Color(59, 130, 246));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(29, 78, 216));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(59, 130, 246));
            }
        });
    }

    private void styleBoardButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 60));
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(17, 24, 39));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.getText().isEmpty()) {
                    button.setBackground(new Color(243, 244, 246));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }
        });
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(new Color(17, 24, 39));
        comboBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    private void styleRadioButton(JRadioButton radioButton) {
        radioButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        radioButton.setForeground(Color.WHITE);
        radioButton.setOpaque(false);
        radioButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void resetGame() {
        currentState = game.new TicTacToeState();
        playerMark = TicTacToe.O;
        aiMark = TicTacToe.X;
        playerTurn = playerFirstRadio.isSelected();
        updateUI();
        statusLabel.setText(playerTurn ? "Your turn (O)" : "AI's turn (X)");
        if (!playerTurn) {
            makeAIMoveAsync();
        }
    }

    private void handlePlayerMove(int row, int col) {
        if (!playerTurn || currentState.isTerminal() ||
                currentState.position().getGrid()[row][col] != -1) {
            return;
        }

        TicTacToe.TicTacToeMove playerMove = new TicTacToe.TicTacToeMove(playerMark, row, col);
        currentState = (TicTacToe.TicTacToeState) currentState.next(playerMove);
        updateUI();

        if (checkGameOver()) {
            return;
        }

        playerTurn = false;
        statusLabel.setText("AI is thinking...");
        makeAIMoveAsync();
    }

    private void makeAIMoveAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(500);
                makeAIMove();
                return null;
            }

            @Override
            protected void done() {
                updateUI();
                if (checkGameOver()) {
                    return;
                }
                playerTurn = true;
                statusLabel.setText("Your turn (O)");
            }
        };
        worker.execute();
    }

    private void makeAIMove() {
        Move<TicTacToe> aiMove;
        if (difficulty.equals("normal")) {
            aiMove = currentState.chooseMove(aiMark);
        } else {
            TicTacToeNode rootNode = new TicTacToeNode(currentState);
            if (difficulty.equals("very hard")) {
                OptimizedMCTS mcts = new OptimizedMCTS(rootNode);
                aiMove = mcts.findNextMove(iterations);
            } else {
                MCTS mcts = new MCTS(rootNode);
                aiMove = mcts.findNextMove(iterations);
            }
        }
        currentState = (TicTacToe.TicTacToeState) currentState.next(aiMove);
    }

    private boolean checkGameOver() {
        if (currentState.isTerminal()) {
            Optional<Integer> winner = currentState.winner();
            if (winner.isPresent()) {
                statusLabel.setText(winner.get() == playerMark ? "You win!" : "AI wins!");
            } else {
                statusLabel.setText("It's a draw!");
            }
            return true;
        }
        return false;
    }

    private void updateUI() {
        int[][] grid = currentState.position().getGrid();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(switch (grid[i][j]) {
                    case TicTacToe.O -> "O";
                    case TicTacToe.X -> "X";
                    default -> "";
                });
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TicTacToeUI();
        });
    }
}