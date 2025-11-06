import java.awt.Point;
import java.util.Stack;

/**
 * REFACTORED FROM GameState.java
 * Pure game logic model with no UI dependencies.
 * Manages the Connect Four game state, rules, and move validation.
 */
public class GameModel {
    private Cell[][] board;  // CHANGED: from Boolean[][] to Cell[][]
    private Stack<Point> moves;
    private String errorMessage;
    private boolean gameOver;
    private boolean redWins;
    private boolean yellowWins;
    private Cell.State currentPlayer;  // CHANGED: from boolean redsTurn to Cell.State
    private Bot redBot; // added for AI
    private Bot yellowBot; // added for AI
    
    private static final int COLUMNS = 7;
    private static final int ROWS = 6;
    
    /**
     * Creates a new game model with an empty board.
     * Red player starts first.
     */
    public GameModel() {
        board = new Cell[COLUMNS][ROWS];
        // NEW: Initialize all cells
        for (int i = 0; i < COLUMNS; i++) {
            for (int j = 0; j < ROWS; j++) {
                board[i][j] = new Cell();
            }
        }
        gameOver = false;
        redWins = false;
        yellowWins = false;
        currentPlayer = Cell.State.RED;  // CHANGED: from redsTurn = true
        moves = new Stack<>();
        errorMessage = null;
        redBot = null;
        yellowBot = new Bot(); // replace with NULL to disable bot
    }
    
    /**
     * Attempts to place a coin in the specified column.
     * MODIFIED: Enhanced validation and error handling
     * @param column the column number (1-7)
     * @return true if the move was successful, false otherwise
     */
    public boolean makeMove(int column) {
        errorMessage = null;
        
        // NEW: Validate column number
        if (column < 1 || column > COLUMNS) {
            errorMessage = "Invalid column. Please choose 1-7.";
            return false;
        }
        
        // Can't make a move if the game is over
        if (gameOver) {
            errorMessage = "The game is over.";
            return false;
        }
        
        int col = column - 1;  // Convert to 0-indexed
        
        // Can't make a move if the specified column is full
        if (!board[col][ROWS - 1].isAvailable()) {  // CHANGED: from pieces[row-1][5] != null
            errorMessage = "That column is full.";
            return false;
        }
        
        // Find the next open space in the specified column
        int row = 0;
        while (row < ROWS && !board[col][row].isAvailable()) {  // CHANGED: from pieces[row-1][y] != null
            row++;
        }
        
        // Place the piece
        board[col][row].setState(currentPlayer);  // CHANGED: from pieces[row-1][y] = redsTurn
        
        // Add the move to the move history
        moves.push(new Point(col, row));
        
        // Check if this move won the game
        checkForWin();
        
        // Switch players if game is not over
        if (!gameOver) {
            currentPlayer = (currentPlayer == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
            // CHANGED: from redsTurn = !redsTurn
            
            // Try a bot move. Note that this calls makeMove, making this recursive.
            checkBotMove();
        }
        
        return true;
    }
    
    /**
     * Checks if the current player is a bot, and if they are, makes their move.
     * TODO: undo support, make red bot move first if active
     */
    public boolean checkBotMove() {
    	Bot bot = null;
    	if (currentPlayer == Cell.State.RED) {
    		bot = redBot;
    	} else if (currentPlayer == Cell.State.YELLOW) {
    		bot = yellowBot;
    	}
    	
    	if (bot != null) {
    		int column = bot.getMove(currentPlayer, this);
    		while (!makeMove(column)) {
    			column = bot.getMove(currentPlayer, this);
    		}
    		return true;
    	}
    	return false;
    }
    
    /**
     * Undoes the last move.
     * MODIFIED: Updated to work with Cell objects
     * @return true if undo was successful, false otherwise
     */
    public boolean undo() {
        errorMessage = null;
        
        if (moves.empty()) {
            errorMessage = "No moves to undo.";
            return false;
        }
        
        // Remove the last move
        Point lastMove = moves.pop();
        board[lastMove.x][lastMove.y].clear();  // CHANGED: from pieces[x][y] = null
        
        // Reset game over flags if necessary
        if (gameOver) {
            gameOver = false;
            redWins = false;
            yellowWins = false;
        } else {
            // Switch back to the previous player
            currentPlayer = (currentPlayer == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
            // CHANGED: from redsTurn = !redsTurn
        }
        
        return true;
    }
    
    /**
     * Resets the game to its initial state.
     * MODIFIED: Updated to work with Cell objects
     */
    public void restart() {
        // Clear all cells
        for (int i = 0; i < COLUMNS; i++) {
            for (int j = 0; j < ROWS; j++) {
                board[i][j].clear();  // CHANGED: from pieces = new Boolean[7][6]
            }
        }
        gameOver = false;
        redWins = false;
        yellowWins = false;
        currentPlayer = Cell.State.RED;  // CHANGED: from redsTurn = true
        moves.clear();
        errorMessage = null;
    }
    
    /**
     * Checks if the last move resulted in a win or tie.
     * MODIFIED: Updated logic for Cell objects
     */
    private void checkForWin() {
        gameOver = false;
        redWins = false;
        yellowWins = false;
        
        // Check for red win
        if (fourInARow(Cell.State.RED)) {  // CHANGED: from fourInARow(true)
            gameOver = true;
            redWins = true;
        }
        // Check for yellow win
        else if (fourInARow(Cell.State.YELLOW)) {  // CHANGED: from fourInARow(false)
            gameOver = true;
            yellowWins = true;
        }
        // Check for tie
        else {
            boolean isTie = true;
            for (int i = 0; i < COLUMNS; i++) {
                if (board[i][ROWS - 1].isAvailable()) {  // CHANGED: from pieces[i][5] == null
                    isTie = false;
                    break;
                }
            }
            gameOver = isTie;
        }
    }
    
    /**
     * Checks if there are four pieces of the specified color in a row.
     * MODIFIED: Updated to work with Cell.State instead of boolean
     * @param playerState the player's state to check for
     * @return true if four in a row exists, false otherwise
     */
    private boolean fourInARow(Cell.State playerState) {  // CHANGED: from boolean color
        // Check vertical
        for (int col = 0; col < COLUMNS; col++) {
            int count = 0;
            for (int row = 0; row < ROWS; row++) {
                if (board[col][row].isAvailable()) {
                    break;  // No piece above empty cells
                } else if (board[col][row].getState() == playerState) {  // CHANGED: comparison method
                    count++;
                    if (count == 4) return true;
                } else {
                    count = 0;
                }
            }
        }
        
        // Check horizontal
        for (int row = 0; row < ROWS; row++) {
            int count = 0;
            for (int col = 0; col < COLUMNS; col++) {
                if (board[col][row].getState() == playerState) {  // CHANGED: comparison method
                    count++;
                    if (count == 4) return true;
                } else {
                    count = 0;
                }
            }
        }
        
        // Check diagonal (bottom-left to top-right: /)
        for (int startCol = 0; startCol < COLUMNS; startCol++) {
            for (int startRow = 0; startRow < ROWS; startRow++) {
                int count = 0;
                for (int i = 0; startCol + i < COLUMNS && startRow + i < ROWS; i++) {
                    if (board[startCol + i][startRow + i].getState() == playerState) {
                        count++;
                        if (count == 4) return true;
                    } else {
                        count = 0;
                    }
                }
            }
        }
        
        // Check diagonal (bottom-right to top-left: \)
        for (int startCol = 0; startCol < COLUMNS; startCol++) {
            for (int startRow = 0; startRow < ROWS; startRow++) {
                int count = 0;
                for (int i = 0; startCol - i >= 0 && startRow + i < ROWS; i++) {
                    if (board[startCol - i][startRow + i].getState() == playerState) {
                        count++;
                        if (count == 4) return true;
                    } else {
                        count = 0;
                    }
                }
            }
        }
        
        return false;
    }
    
    // Getters
    
    public Cell[][] getBoard() {  // CHANGED: from Boolean[][] getPieces()
        return board;
    }
    
    public Cell getCell(int col, int row) {  // NEW METHOD
        if (col >= 0 && col < COLUMNS && row >= 0 && row < ROWS) {
            return board[col][row];
        }
        return null;
    }
    
    public Stack<Point> getMoves() {
        return moves;
    }
    
    public String getErrorMessage() {  // CHANGED: from getError()
        return errorMessage;
    }
    
    public boolean isRedWins() {  // CHANGED: from getRedWins()
        return redWins;
    }
    
    public boolean isYellowWins() {  // CHANGED: from getYellowWins()
        return yellowWins;
    }
    
    public Cell.State getCurrentPlayer() {  // NEW: replaces getRedsTurn()
        return currentPlayer;
    }
    
    public boolean isGameOver() {  // CHANGED: from getGameOver()
        return gameOver;
    }
    
    public int getColumns() {  // NEW METHOD
        return COLUMNS;
    }
    
    public int getRows() {  // NEW METHOD
        return ROWS;
    }
}
