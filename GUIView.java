import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Scanner;

import javax.swing.*;

/**
 * REFACTORED from BoardDrawing.java and Connect4UI.java
 * GUI-based view for the Connect Four game.
 * NEW FEATURE: Click on columns to make moves instead of using buttons.
 */
public class GUIView extends JFrame implements GameView {
    private GameModel model;
    private BoardPanel boardPanel;
    
    // Board dimensions (from original BoardDrawing constants)
    private static final int BOARD_START_X = 182;
    private static final int BOARD_START_Y = 75;
    private static int BOARD_WIDTH = 386;
    private static int BOARD_HEIGHT = 340;
    private static int HOLE_DIAMETER = 36;
    private static int HOLE_DISTANCE = 50;
    private static int HOLE_OFFSET = 25;
    private static int HOLE_START_X = BOARD_START_X + HOLE_OFFSET;
    private static int HOLE_START_Y = BOARD_START_Y + BOARD_HEIGHT - HOLE_OFFSET - HOLE_DIAMETER;
    
    /**
     * Creates a new GUI view for the game.
     * @param model the game model to display
     */
    public GUIView(GameModel model) {
        this.model = model;
        setupMainMenu();
    }
    
    /**
     * NEW: Sets up main menu for difficulty and color selection
     */
    public void setupMainMenu() {
    	setTitle("CONNECT 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        
        // switch to vertical layout
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        
    	// Create three control panels: One for difficulty, one for color, and one to start
        JPanel difficultyPanel = new JPanel();
        JPanel colorPanel = new JPanel();
        JPanel startPanel = new JPanel();
        
        // Difficulty options
        JLabel difficultyLabel = new JLabel("Select difficulty:", SwingConstants.CENTER);
        difficultyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ButtonGroup difficultyButtonGroup = new ButtonGroup();
        
        JRadioButton humanButton = new JRadioButton("Human");
        humanButton.setBackground(Color.cyan);
        humanButton.setSelected(true);
        humanButton.addActionListener(e -> {
            model.updateValues(GameModel.Difficulty.HUMAN);
        });
        difficultyPanel.add(humanButton);
        difficultyButtonGroup.add(humanButton);
        
        JRadioButton beginnerButton = new JRadioButton("Beginner");
        beginnerButton.setBackground(Color.green);
        beginnerButton.addActionListener(e -> {
            model.updateValues(GameModel.Difficulty.BEGINNER);
        });
        difficultyPanel.add(beginnerButton);
        difficultyButtonGroup.add(beginnerButton);
        
        JRadioButton intermediateButton = new JRadioButton("Intermediate");
        intermediateButton.setBackground(Color.yellow);
        intermediateButton.addActionListener(e -> {
            model.updateValues(GameModel.Difficulty.INTERMEDIATE);
        });
        difficultyPanel.add(intermediateButton);
        difficultyButtonGroup.add(intermediateButton);
        
        JRadioButton expertButton = new JRadioButton("Expert");
        expertButton.setBackground(new Color(255, 100, 100));
        expertButton.addActionListener(e -> {
            model.updateValues(GameModel.Difficulty.EXPERT);
        });
        difficultyPanel.add(expertButton);
        difficultyButtonGroup.add(expertButton);
        
        // Color options
        JLabel colorLabel = new JLabel("Select your color:", SwingConstants.CENTER);
        colorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ButtonGroup colorButtonGroup = new ButtonGroup();
        
        JRadioButton redButton = new JRadioButton("Red");
        redButton.setBackground(new Color(255, 100, 100));
        redButton.setSelected(true);
        redButton.addActionListener(e -> {
            model.updateBotColor(Cell.State.YELLOW);
        });
        colorPanel.add(redButton);
        colorButtonGroup.add(redButton);
        
        JRadioButton yellowButton = new JRadioButton("Yellow");
        yellowButton.setBackground(Color.yellow);
        yellowButton.addActionListener(e -> {
            model.updateBotColor(Cell.State.RED);
        });
        colorPanel.add(yellowButton);
        colorButtonGroup.add(yellowButton);
        
        // Start button
        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> {
        	// Erase menu buttons
        	remove(difficultyLabel);
        	remove(difficultyPanel);
        	remove(colorLabel);
        	remove(colorPanel);
        	remove(startPanel);
        	
        	setSize(0, 0); // force display to reload
        	setupFrame();
        });
        startPanel.add(startButton);
        
        // Add components to frame
        add(difficultyLabel);
        add(difficultyPanel);
        add(colorLabel);
        add(colorPanel);
        add(startPanel);
    }
    
    /**
     * Sets up the main frame and components.
     * MODIFIED: Removed button panel, added click handling
     */
    private void setupFrame() {
    	// NEW: switch back to border layout (default)
    	setLayout(new BorderLayout());
        
    	// NEW: Recalculate board size for different difficulties
    	// Default should be 386, 340
    	BOARD_WIDTH = HOLE_DISTANCE * (model.getColumns() - 1) + 2 * HOLE_OFFSET + HOLE_DIAMETER;
    	BOARD_HEIGHT = HOLE_DISTANCE * (model.getRows() - 1) + 2 * HOLE_OFFSET + HOLE_DIAMETER + 4;
    	HOLE_START_X = BOARD_START_X + HOLE_OFFSET;
        HOLE_START_Y = BOARD_START_Y + BOARD_HEIGHT - HOLE_OFFSET - HOLE_DIAMETER;
    	
        // NEW: Scale window based on board size
        // Default should be 750, 550
        setSize(BOARD_START_X * 2 + BOARD_WIDTH, BOARD_START_Y * 2 + BOARD_HEIGHT + 60);
        
        // NEW: half hole size if the size is too large for our primary display
    	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    	if (screenSize.getWidth() < getSize().getWidth() || screenSize.getHeight() < getSize().getHeight()) {
    		HOLE_DIAMETER = HOLE_DIAMETER / 2;
    		HOLE_DISTANCE = HOLE_DISTANCE / 2;
    		HOLE_OFFSET = HOLE_OFFSET / 2;
    		setupFrame();
    		return ;
    	}
        
        // Create the board panel
        boardPanel = new BoardPanel();
        
        // NEW: Add mouse listener for column clicks
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleBoardClick(e.getX(), e.getY());
            }
        });
        
        // Create control panel with only Undo and Restart buttons
        JPanel controlPanel = new JPanel();
        
        // NEW: Save and load buttons
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            model.save();
            updateView();
            
        });
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> {
            model.load();
            updateView();
        });
        
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> {
            model.undo();
            updateView();
        });
        
        JButton restartButton = new JButton("Restart");
        restartButton.addActionListener(e -> {
            model.restart();
            updateView();
        });
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        controlPanel.add(undoButton);
        controlPanel.add(restartButton);
        
        // Add components to frame
        add(boardPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        
        // restart game
        model.restart();
    }
    
    /**
     * NEW METHOD: Handles mouse clicks on the board.
     * Determines which column was clicked and makes a move.
     * @param x the x-coordinate of the click
     * @param y the y-coordinate of the click
     */
    private void handleBoardClick(int x, int y) {
        // Check if click is in the clickable area (above the board)
        if (y >= BOARD_START_Y - 50 && y <= BOARD_START_Y + BOARD_HEIGHT) {
            // Determine which column was clicked
            for (int col = 0; col < model.getColumns(); col++) {
                int colX = HOLE_START_X + col * HOLE_DISTANCE;
                if (x >= colX && x < colX + HOLE_DISTANCE) {
                    model.makeMove(col + 1);  // Convert to 1-indexed
                    updateView();
                    return;
                }
            }
        }
    }
    
    @Override
    public void updateView() {
        boardPanel.repaint();
    }
    
    @Override
    public void showError(String message) {
        // Error is displayed on the board panel
        boardPanel.repaint();
    }
    
    @Override
    public void showGameOver(String message) {
        // Game over state is displayed on the board panel
        boardPanel.repaint();
    }
    
    @Override
    public void display() {
        setVisible(true);
    }
    
    /**
     * Inner class for the board drawing panel.
     * REFACTORED from BoardDrawing.java
     */
    private class BoardPanel extends JComponent {
        /**
         * Paints the game board and pieces.
         * MODIFIED: Updated to work with Cell objects and GameModel
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(new Font("TimesRoman", Font.BOLD, 20));
            
            // Draw the board
            g2.setColor(Color.BLUE);
            g2.fill(new Rectangle(BOARD_START_X, BOARD_START_Y, BOARD_WIDTH, BOARD_HEIGHT));
            
            // Draw the holes
            g2.setColor(Color.WHITE);
            for (int col = 0; col < model.getColumns(); col++) {
                for (int row = 0; row < model.getRows(); row++) {
                    Ellipse2D.Double hole = new Ellipse2D.Double(
                        HOLE_START_X + col * HOLE_DISTANCE,
                        HOLE_START_Y - row * HOLE_DISTANCE,
                        HOLE_DIAMETER,
                        HOLE_DIAMETER
                    );
                    g2.fill(hole);
                }
            }
            
            // Draw column numbers
            g2.setColor(Color.BLACK);
            for (int col = 0; col < model.getColumns(); col++) {
                String label = String.valueOf(col + 1);
                int w = g2.getFontMetrics().stringWidth(label);
                g2.drawString(
                    label,
                    HOLE_START_X + HOLE_DIAMETER / 2 - w / 2 + col * HOLE_DISTANCE,
                    BOARD_START_Y + BOARD_HEIGHT + 25
                );
            }
            
            // Draw game state message
            drawStateMessage(g2);
            
            // Draw error message if present
            if (model.getErrorMessage() != null) {
                g2.setColor(Color.BLACK);
                g2.drawString("Oops!", 15, 220);
                g2.drawString(model.getErrorMessage(), 15, 250);
            }
            
            // Draw the pieces
            // CHANGED: Updated to use Cell objects, support lucky coins
            for (int col = 0; col < model.getColumns(); col++) {
                for (int row = 0; row < model.getRows(); row++) {
                    Cell cell = model.getCell(col, row);
                    if (!cell.isEmpty()) {
                        if (cell.isRed()) {
                            g2.setColor(Color.RED);
                        } else if (cell.isYellow()) {
                            g2.setColor(Color.YELLOW);
                        } else {
                        	g2.setColor(Color.CYAN); // TODO: make image of four leaf clover
                        }
                        g2.fill(new Ellipse2D.Double(
                            HOLE_START_X + 2 + col * HOLE_DISTANCE,
                            HOLE_START_Y + 2 - row * HOLE_DISTANCE,
                            HOLE_DIAMETER - 4,
                            HOLE_DIAMETER - 4
                        ));
                    }
                }
            }
            
            // NEW: Draw clickable area hint
            if (!model.isGameOver()) {
                g2.setColor(new Color(200, 200, 200, 100));
                g2.fill(new Rectangle(BOARD_START_X, BOARD_START_Y - 50, BOARD_WIDTH, 50));
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Arial", Font.ITALIC, 12));
                String hint = "Click above a column to make a move";
                int hintWidth = g2.getFontMetrics().stringWidth(hint);
                g2.drawString(hint, BOARD_START_X + (BOARD_WIDTH - hintWidth) / 2, BOARD_START_Y - 60);
            }
        }
        
        /**
         * Draws the current game state message.
         * MODIFIED: Updated to use GameModel methods and Cell.State
         * @param g2 the graphics context
         */
        private void drawStateMessage(Graphics2D g2) {
            Rectangle textBack = new Rectangle(
                BOARD_START_X + (BOARD_WIDTH / 2) - 100,
                BOARD_START_Y - 50,
                200,
                40
            );
            
            String message;
            Color backColor;
            Color textColor;
            
            // CHANGED: Updated to use new GameModel methods
            if (model.isRedWins()) {
                backColor = Color.RED;
                textColor = Color.BLACK;
                message = "RED WINS!";
            } else if (model.isYellowWins()) {
                backColor = Color.YELLOW;
                textColor = Color.BLACK;
                message = "YELLOW WINS!";
            } else if (model.isGameOver()) {
                backColor = Color.BLACK;
                textColor = Color.WHITE;
                message = "IT'S A TIE!";
            } else if (model.getCurrentPlayer() == Cell.State.RED) {
                // CHANGED: from getRedsTurn()
                backColor = Color.LIGHT_GRAY;
                textColor = Color.RED;
                message = "Red's Turn";
            } else {
                backColor = Color.LIGHT_GRAY;
                textColor = Color.YELLOW;
                message = "Yellow's Turn";
            }
            
            g2.setColor(backColor);
            g2.fill(textBack);
            g2.setColor(textColor);
            
            int stringWidth = g2.getFontMetrics().stringWidth(message);
            int strX = BOARD_START_X + (BOARD_WIDTH - stringWidth) / 2;
            g2.drawString(message, strX, BOARD_START_Y - 22);
        }
    }
}
