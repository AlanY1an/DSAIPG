package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;
import java.util.Stack;

public class GomokuSwingApp {
    // Theme colors
    private static final Color BACKGROUND_COLOR = new Color(219, 177, 122); // Light wood color
    private static final Color BOARD_COLOR = new Color(240, 217, 181);      // Board color
    private static final Color GRID_COLOR = new Color(85, 65, 45);          // Darker grid lines
    private static final Color BUTTON_COLOR = new Color(205, 133, 63);      // Button color
    private static final Color BUTTON_TEXT_COLOR = new Color(255, 250, 240); // Button text color
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 28);
    private static final Font NORMAL_FONT = new Font("Arial", Font.PLAIN, 14);

    public static void main(String[] args) {
        try {
            // Set system look and feel for better integration
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(GomokuSwingApp::showWelcome);
    }

    private static void showWelcome() {
        JFrame welcomeFrame = new JFrame("Gomoku Game");
        welcomeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomeFrame.setSize(500, 400);
        welcomeFrame.setLayout(new BorderLayout(10, 20));
        welcomeFrame.getContentPane().setBackground(BACKGROUND_COLOR);

        // Create a custom panel for the title with a slight shadow effect
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                String text = "Welcome to Gomoku!";
                FontMetrics fm = g2d.getFontMetrics(TITLE_FONT);
                int textWidth = fm.stringWidth(text);

                g2d.setColor(new Color(0, 0, 0, 50)); // Shadow color
                g2d.drawString(text, (getWidth() - textWidth) / 2 + 2, 42);

                g2d.setColor(new Color(50, 30, 0)); // Text color
                g2d.setFont(TITLE_FONT);
                g2d.drawString(text, (getWidth() - textWidth) / 2, 40);
            }
        };
        titlePanel.setPreferredSize(new Dimension(500, 80));
        titlePanel.setOpaque(false);
        welcomeFrame.add(titlePanel, BorderLayout.NORTH);

        // Create a stylish center panel with better layout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.setBackground(new Color(BACKGROUND_COLOR.getRed(), BACKGROUND_COLOR.getGreen(), BACKGROUND_COLOR.getBlue(), 220));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Add game description
        JLabel descLabel = new JLabel("<html><body style='width: 300px'>" +
                "Gomoku is a classic board game where players take turns placing stones. " +
                "The first to form an unbroken chain of five stones wins!</body></html>");
        descLabel.setFont(NORMAL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(descLabel, gbc);

        // Add difficulty selection with improved styling
        JLabel diffLabel = new JLabel("Select AI Difficulty:");
        diffLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        centerPanel.add(diffLabel, gbc);

        String[] diffOptions = {"Normal (MCTS)", "Difficult (OptimizedMCTS)"};
        JComboBox<String> diffBox = new JComboBox<>(diffOptions);
        diffBox.setFont(NORMAL_FONT);
        diffBox.setBackground(Color.WHITE);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(diffBox, gbc);

        welcomeFrame.add(centerPanel, BorderLayout.CENTER);

        // Create a panel for buttons with better styling
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setBackground(new Color(BACKGROUND_COLOR.getRed() - 20, BACKGROUND_COLOR.getGreen() - 20, BACKGROUND_COLOR.getBlue() - 20));

        // Custom button style
        JButton startAsBlack = createStyledButton("Play First (Black)", new ImageIcon());
        JButton startAsWhite = createStyledButton("Play Second (White)", new ImageIcon());

        startAsBlack.addActionListener(e -> {
            welcomeFrame.dispose();
            startGame(true, diffBox.getSelectedIndex() == 1);
        });

        startAsWhite.addActionListener(e -> {
            welcomeFrame.dispose();
            startGame(false, diffBox.getSelectedIndex() == 1);
        });

        buttonPanel.add(startAsBlack);
        buttonPanel.add(startAsWhite);
        welcomeFrame.add(buttonPanel, BorderLayout.SOUTH);

        welcomeFrame.setLocationRelativeTo(null);
        welcomeFrame.setVisible(true);
    }

    // Create a styled button with hover effect
    private static JButton createStyledButton(String text, Icon icon) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setBackground(BUTTON_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 40));

        if (icon != null && icon.getIconWidth() > 0) {
            button.setIcon(icon);
        }

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(
                        Math.min(255, BUTTON_COLOR.getRed() + 20),
                        Math.min(255, BUTTON_COLOR.getGreen() + 20),
                        Math.min(255, BUTTON_COLOR.getBlue() + 20)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_COLOR);
            }
        });

        return button;
    }

    private static void startGame(boolean playerFirst, boolean hard) {
        JFrame frame = new JFrame("Gomoku: " + (playerFirst ? "You (Black)" : "AI (Black)") +
                " vs " + (!playerFirst ? "You (White)" : "AI (White)") +
                " - " + (hard ? "Difficult" : "Normal"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(850, 900);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BACKGROUND_COLOR);

        // Create game status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(BACKGROUND_COLOR.getRed() - 10, BACKGROUND_COLOR.getGreen() - 10, BACKGROUND_COLOR.getBlue() - 10));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel statusLabel = new JLabel("Game in progress...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JLabel turnLabel = new JLabel(playerFirst ? "Your turn (Black)" : "AI thinking...");
        turnLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusPanel.add(turnLabel, BorderLayout.EAST);

        frame.add(statusPanel, BorderLayout.NORTH);

        // Create board panel with status updates
        GomokuBoardPanel board = new GomokuBoardPanel(playerFirst, hard, frame, statusLabel, turnLabel);
        frame.add(board, BorderLayout.CENTER);

        // Create control panel for buttons
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
        controlPanel.setBackground(new Color(BACKGROUND_COLOR.getRed() - 20, BACKGROUND_COLOR.getGreen() - 20, BACKGROUND_COLOR.getBlue() - 20));

        JButton undoButton = createStyledButton("Undo Move", null);
        undoButton.addActionListener(e -> board.undo());

        JButton restartButton = createStyledButton("New Game", null);
        restartButton.addActionListener(e -> {
            frame.dispose();
            showWelcome();
        });

        controlPanel.add(undoButton);
        controlPanel.add(restartButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class GomokuBoardPanel extends JPanel {
    private static final int SIZE = 15;
    private static final int CELL_SIZE = 50;
    private static final int BOARD_PADDING = 40;
    private static final Color BOARD_COLOR = new Color(240, 217, 181);
    private static final Color GRID_COLOR = new Color(85, 65, 45);
    private static final Color HOVER_COLOR = new Color(255, 100, 100, 80);
    private static final Color LAST_MOVE_MARKER = new Color(255, 0, 0, 150);

    private GomokuState state;
    private final Stack<GomokuState> history = new Stack<>();
    private boolean gameOver = false;
    private final int playerColor;
    private final boolean useOptimized;
    private final JFrame parentFrame;
    private final JLabel statusLabel;
    private final JLabel turnLabel;

    // Track hover position and last move
    private Point hoverPoint = null;
    private Point lastMovePoint = null;

    public GomokuBoardPanel(boolean playerFirst, boolean hard, JFrame parentFrame, JLabel statusLabel, JLabel turnLabel) {
        this.state = new GomokuState();
        this.playerColor = playerFirst ? Gomoku.PLAYER_ONE : Gomoku.PLAYER_TWO;
        this.useOptimized = hard;
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.turnLabel = turnLabel;

        setPreferredSize(new Dimension(SIZE * CELL_SIZE + 2 * BOARD_PADDING, SIZE * CELL_SIZE + 2 * BOARD_PADDING));
        setBackground(BOARD_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createLineBorder(new Color(60, 40, 20), 2)
        ));

        // Handle mouse movements for hover effect
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameOver || state.player() != playerColor) return;

                int x = (e.getX() - BOARD_PADDING + CELL_SIZE / 2) / CELL_SIZE;
                int y = (e.getY() - BOARD_PADDING + CELL_SIZE / 2) / CELL_SIZE;

                // Check if valid position
                if (x >= 0 && x < SIZE && y >= 0 && y < SIZE && state.getBoard()[y][x] == 0) {
                    hoverPoint = new Point(x, y);
                } else {
                    hoverPoint = null;
                }
                repaint();
            }
        });

        // Handle mouse exit to clear hover
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverPoint = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (gameOver || state.player() != playerColor) return;

                int x = (e.getX() - BOARD_PADDING + CELL_SIZE / 2) / CELL_SIZE;
                int y = (e.getY() - BOARD_PADDING + CELL_SIZE / 2) / CELL_SIZE;

                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || state.getBoard()[y][x] != 0) return;

                // Make player move
                makeMove(new GomokuMove(playerColor, y, x));
                lastMovePoint = new Point(x, y);
                hoverPoint = null;

                // Update status
                turnLabel.setText("AI thinking...");
                statusLabel.setText("Your move placed. AI is thinking...");

                repaint();
                SwingUtilities.invokeLater(() -> checkWinnerAndContinue());
            }
        });

        // Start AI move if player goes second
        if (!playerFirst) {
            SwingUtilities.invokeLater(() -> {
                turnLabel.setText("AI thinking...");
                makeAIMove();
                turnLabel.setText("Your turn (White)");
            });
        }
    }

    private void makeMove(GomokuMove move) {
        history.push(state);
        state = (GomokuState) state.next(move);
    }

    private void makeAIMove() {
        if (gameOver) return;

        history.push(state);
        statusLabel.setText("AI is thinking...");
        turnLabel.setText("AI thinking...");

        // Create progress dialog
        JDialog progressDialog = new JDialog(parentFrame, "AI Thinking...", false);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JLabel thinkingLabel = new JLabel("AI is calculating next move...");
        thinkingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        progressDialog.add(thinkingLabel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(parentFrame);

        // Use SwingWorker for AI move to prevent UI freeze
        SwingWorker<Move<Gomoku>, Void> worker = new SwingWorker<>() {
            private long timeTaken;

            @Override
            protected Move<Gomoku> doInBackground() {
                long startTime = System.currentTimeMillis();

                GomokuNode root = new GomokuNode(state);
                Move<Gomoku> aiMove = useOptimized ?
                        new OptimizedMCTS(root).findNextMove(500) :
                        new MCTS(root).findNextMove(500);

                long endTime = System.currentTimeMillis();
                timeTaken = endTime - startTime;

                return aiMove;
            }

            @Override
            protected void done() {
                try {
                    progressDialog.dispose();

                    Move<Gomoku> aiMove = get();
                    GomokuMove move = (GomokuMove) aiMove;

                    // Update last move point
                    lastMovePoint = new Point(move.getX(), move.getY());

                    // Update UI
                    parentFrame.setTitle("Gomoku - AI move took: " + timeTaken + " ms");
                    state = (GomokuState) state.next(aiMove);
                    statusLabel.setText("AI has moved. Your turn.");
                    turnLabel.setText("Your turn (" + (playerColor == Gomoku.PLAYER_ONE ? "Black" : "White") + ")");

                    repaint();

                    // Check if AI won
                    Optional<Integer> winner = state.winner();
                    if (winner.isPresent()) {
                        gameOver = true;
                        statusLabel.setText("Game over! AI wins!");
                        turnLabel.setText("Game ended");
                        showGameOver("AI wins!");
                    } else if (state.isTerminal()) {
                        gameOver = true;
                        statusLabel.setText("Game over! It's a draw!");
                        turnLabel.setText("Game ended");
                        showGameOver("It's a draw!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    private void checkWinnerAndContinue() {
        Optional<Integer> winner = state.winner();
        if (winner.isPresent()) {
            gameOver = true;
            statusLabel.setText("Game over! " + (winner.get() == playerColor ? "You win!" : "AI wins!"));
            turnLabel.setText("Game ended");
            showGameOver(winner.get() == playerColor ? "You win!" : "AI wins!");
        } else if (state.isTerminal()) {
            gameOver = true;
            statusLabel.setText("Game over! It's a draw!");
            turnLabel.setText("Game ended");
            showGameOver("It's a draw!");
        } else {
            makeAIMove();
        }
    }

    public void undo() {
        if (history.size() >= 2 && !gameOver) {
            state = history.pop(); // Undo AI move
            state = history.pop(); // Undo player move

            // Clear last move marker
            lastMovePoint = null;

            // Update status
            statusLabel.setText("Moves undone. Your turn again.");
            turnLabel.setText("Your turn (" + (playerColor == Gomoku.PLAYER_ONE ? "Black" : "White") + ")");

            repaint();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Cannot undo any further or game is already over.",
                    "Undo Not Available",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showGameOver(String message) {
        // Use a more stylish custom dialog
        JDialog gameOverDialog = new JDialog(parentFrame, "Game Over", true);
        gameOverDialog.setLayout(new BorderLayout());
        gameOverDialog.setSize(350, 200);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(new Color(245, 222, 179));

        JLabel resultLabel = new JLabel(message, JLabel.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 24));
        contentPanel.add(resultLabel, BorderLayout.NORTH);

        JLabel questionLabel = new JLabel("Would you like to play again?", JLabel.CENTER);
        questionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        contentPanel.add(questionLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        JButton yesButton = new JButton("Yes, New Game");
        yesButton.setFont(new Font("Arial", Font.BOLD, 14));
        yesButton.setBackground(new Color(100, 180, 100));
        yesButton.setForeground(Color.WHITE);
        yesButton.setFocusPainted(false);

        JButton noButton = new JButton("No, Exit Game");
        noButton.setFont(new Font("Arial", Font.BOLD, 14));
        noButton.setBackground(new Color(180, 100, 100));
        noButton.setForeground(Color.WHITE);
        noButton.setFocusPainted(false);

        yesButton.addActionListener(e -> {
            gameOverDialog.dispose();
            SwingUtilities.getWindowAncestor(this).dispose();
            GomokuSwingApp.main(null);
        });

        noButton.addActionListener(e -> {
            System.exit(0);
        });

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        gameOverDialog.add(contentPanel);
        gameOverDialog.setLocationRelativeTo(parentFrame);
        gameOverDialog.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw board background
        g2d.setColor(BOARD_COLOR);
        g2d.fillRect(BOARD_PADDING - 20, BOARD_PADDING - 20,
                SIZE * CELL_SIZE + 40, SIZE * CELL_SIZE + 40);

        // Draw grid with highlighted points
        drawBoard(g2d);

        // Draw hover effect if applicable
        if (hoverPoint != null && state.player() == playerColor) {
            g2d.setColor(HOVER_COLOR);
            int x = BOARD_PADDING + hoverPoint.x * CELL_SIZE - 15;
            int y = BOARD_PADDING + hoverPoint.y * CELL_SIZE - 15;
            g2d.fillOval(x, y, 30, 30);
        }

        // Draw pieces
        drawPieces(g2d);

        // Highlight last move
        if (lastMovePoint != null) {
            g2d.setColor(LAST_MOVE_MARKER);
            int x = BOARD_PADDING + lastMovePoint.x * CELL_SIZE;
            int y = BOARD_PADDING + lastMovePoint.y * CELL_SIZE;
            g2d.drawLine(x - 5, y - 5, x + 5, y + 5);
            g2d.drawLine(x + 5, y - 5, x - 5, y + 5);
        }
    }

    private void drawBoard(Graphics2D g) {
        // Draw board grid
        g.setColor(GRID_COLOR);
        g.setStroke(new BasicStroke(1.2f));

        for (int i = 0; i < SIZE; i++) {
            g.drawLine(BOARD_PADDING, BOARD_PADDING + i * CELL_SIZE,
                    BOARD_PADDING + (SIZE - 1) * CELL_SIZE, BOARD_PADDING + i * CELL_SIZE);
            g.drawLine(BOARD_PADDING + i * CELL_SIZE, BOARD_PADDING,
                    BOARD_PADDING + i * CELL_SIZE, BOARD_PADDING + (SIZE - 1) * CELL_SIZE);
        }

        // Draw the five star points (standard for Go/Gomoku boards)
        int[] starPoints = {3, 7, 11}; // Traditional star point positions for 15x15
        for (int i : starPoints) {
            for (int j : starPoints) {
                g.fillOval(BOARD_PADDING + i * CELL_SIZE - 3, BOARD_PADDING + j * CELL_SIZE - 3, 6, 6);
            }
        }

        // Add coordinate labels
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < SIZE; i++) {
            // Letters for columns (horizontal axis)
            char letter = (char) ('A' + (i < 8 ? i : i + 1)); // Skip 'I' as it looks like '1'
            g.drawString(String.valueOf(letter), BOARD_PADDING + i * CELL_SIZE - 4, BOARD_PADDING - 10);

            // Numbers for rows (vertical axis)
            String number = String.valueOf(i + 1);
            g.drawString(number, BOARD_PADDING - 20, BOARD_PADDING + i * CELL_SIZE + 4);
        }
    }

    private void drawPieces(Graphics2D g) {
        int[][] board = state.getBoard();

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int player = board[row][col];
                if (player == 0) continue;

                int x = BOARD_PADDING + col * CELL_SIZE - 15;
                int y = BOARD_PADDING + row * CELL_SIZE - 15;

                // Draw shadow for 3D effect
                g.setColor(new Color(0, 0, 0, 50));
                g.fillOval(x + 2, y + 2, 30, 30);

                // Draw stone
                g.setColor(player == Gomoku.PLAYER_ONE ? Color.BLACK : Color.WHITE);
                g.fillOval(x, y, 30, 30);

                // Add shine effect to stones
                if (player == Gomoku.PLAYER_TWO) { // Only for white stones
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillOval(x + 5, y + 5, 10, 10);
                } else { // For black stones
                    g.setColor(new Color(100, 100, 100, 80));
                    g.fillOval(x + 7, y + 7, 8, 8);
                }

                // Draw outline
                g.setColor(player == Gomoku.PLAYER_ONE ? new Color(50, 50, 50) : Color.BLACK);
                g.setStroke(new BasicStroke(1.0f));
                g.drawOval(x, y, 30, 30);
            }
        }
    }
}