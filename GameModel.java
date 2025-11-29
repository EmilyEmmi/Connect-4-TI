import java.awt.Point;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;

/**
 * REFACTORED FROM GameState.java Pure game logic model with no UI dependencies.
 * Manages the Connect Four game state, rules, and move validation.
 */
public class GameModel {
	private Cell[][] board; // CHANGED: from Boolean[][] to Cell[][]
	private Stack<Point> moves;
	private Stack<Point> prevCoins; // NEW: store prev state as well
	private String errorMessage;
	private boolean gameOver;
	private boolean redWins;
	private boolean yellowWins;
	private Cell.State currentPlayer; // CHANGED: from boolean redsTurn to Cell.State
	private Bot redBot; // NEW: added for AI
	private Bot yellowBot; // NEW: added for AI
	private int maxColumns; // NEW: change from constant to instance variable
	private int maxRows; // NEW: change from constant to instance variable
	private boolean playerIsYellow; // NEW: true if the human player is yellow (switches bot color)
	private int maxLuckyCoins; // NEW: lucky coins per player, changes based on difficulty
	private int redLuckyCoins; // NEW: lucky coins red has left
	private int yellowLuckyCoins; // NEW: lucky coins yellow has left

	// NEW: Difficulty options
	public enum Difficulty {
		HUMAN, BEGINNER, INTERMEDIATE, EXPERT,
	}

	/**
	 * Creates a new game model with an empty board. Red player starts first.
	 */
	public GameModel() {
		maxColumns = 7; // NEW: change from constant to instance variable
		maxRows = 6; // NEW: change from constant to instance variable
		maxLuckyCoins = 3;
		redBot = null; // NEW: support bot players
		yellowBot = null; // NEW: support bot players
		playerIsYellow = false; // NEW: allow color selection

		board = new Cell[maxColumns][maxRows];
		// NEW: Initialize all cells
		for (int i = 0; i < maxColumns; i++) {
			for (int j = 0; j < maxRows; j++) {
				board[i][j] = new Cell();
			}
		}
		gameOver = false;
		redWins = false;
		yellowWins = false;
		currentPlayer = Cell.State.RED; // CHANGED: from redsTurn = true
		moves = new Stack<>();
		prevCoins = new Stack<>(); // NEW: track lucky coin locations
		errorMessage = null;

		checkBotMove(); // NEW: if red is a bot, auto move
	}

	/**
	 * NEW: Updates the information about the board, erasing the board in the
	 * process
	 * 
	 * @param difficulty The difficulty. 0 is human, 1-3 affect the bot complexity and board size
	 * @param playerIsYellow true if the player is yellow, false otherwise.
	 */
	public void updateValues(Difficulty difficulty) {
		// adjust board size and bot complexity based on difficulty
		switch (difficulty) {
		default:
			yellowBot = null;
			maxColumns = 7;
			maxRows = 6;
			maxLuckyCoins = 3;
			break;
		case BEGINNER:
			yellowBot = new Bot(2, 20);
			maxColumns = 7;
			maxRows = 6;
			maxLuckyCoins = 3;
			break;
		case INTERMEDIATE:
			yellowBot = new Bot(3, 10);
			maxColumns = 14;
			maxRows = 12;
			maxLuckyCoins = 7;
			break;
		case EXPERT:
			yellowBot = new Bot(4, 5);
			maxColumns = 21;
			maxRows = 18;
			maxLuckyCoins = 11;
			break;
		}
		
		// make bot red
		if (playerIsYellow) {
			redBot = yellowBot;
			yellowBot = null;
		}
		
		// re-initialize board
		board = new Cell[maxColumns][maxRows];
		for (int i = 0; i < maxColumns; i++) {
			for (int j = 0; j < maxRows; j++) {
				board[i][j] = new Cell();
			}
		}
	}
	
	/**
	 * NEW: Updates the color of the bot.
	 * 
	 * @param color The color of the bot. Only supports Cell.State.RED or Cell.State.Yellow.
	 */
	public void updateBotColor(Cell.State color) {
		if (color == Cell.State.RED) {
			playerIsYellow = true;
			if (redBot == null) {
				redBot = yellowBot;
				yellowBot = null;
			}
		} else if (color == Cell.State.YELLOW) {
			playerIsYellow = false;
			if (yellowBot == null) {
				yellowBot = redBot;
				redBot = null;
			}
		}
	}

	/**
	 * Attempts to place a coin in the specified column. MODIFIED: Enhanced
	 * validation and error handling
	 * 
	 * @param column the column number (1-7)
	 * @return true if the move was successful, false otherwise
	 */
	public boolean makeMove(int column) {
		errorMessage = null;

		// NEW: Validate column number
		if (column < 1 || column > maxColumns) {
			errorMessage = "Invalid column. Please choose 1-7.";
			return false;
		}

		// Can't make a move if the game is over
		if (gameOver) {
			errorMessage = "The game is over.";
			return false;
		}

		int col = column - 1; // Convert to 0-indexed

		// Can't make a move if the specified column is full
		if (!board[col][maxRows - 1].isAvailable()) { // CHANGED: from pieces[row-1][5] != null
			errorMessage = "That column is full.";
			return false;
		}

		// Find the next open space in the specified column
		int row = 0;
		while (row < maxRows && !board[col][row].isAvailable()) { // CHANGED: from pieces[row-1][y] != null
			row++;
		}
		
		// NEW: Add last lucky coin location to lucky coin history
		Point luckySpot = findLuckyCoin();
		prevCoins.push(luckySpot);

		// Place the piece
		Cell.State prevState = board[col][row].getState(); // NEW: store prev state
		board[col][row].setState(currentPlayer); // CHANGED: from pieces[row-1][y] = redsTurn

		// Add the move to the move history
		moves.push(new Point(col, row));

		// Check if this move won the game
		checkForWin();

		// Switch players if game is not over
		if (!gameOver) {
			// NEW: if placed on a lucky coin, don't switch players
			if (prevState != Cell.State.LUCKY) {
				currentPlayer = (currentPlayer == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
				// CHANGED: from redsTurn = !redsTurn
				
				// NEW: Remove the lucky coin, and then try to place a new one.
				if (luckySpot != null) {
					board[luckySpot.x][luckySpot.y].clear();
				}
				checkLuckyCoinPlace(currentPlayer);
			} else if (currentPlayer == Cell.State.RED) {
				redLuckyCoins--; // NEW: lower lucky coin count if it was red's turn
			} else {
				yellowLuckyCoins--; // NEW: lower lucky coin count if it was yellow's turn
			}
			
			// NEW: Try a bot move. Note that this calls makeMove, making this recursive.
			checkBotMove();
		}

		return true;
	}

	/**
	 * NEW: Checks if the current player is a bot, and if they are, makes their move.
	 * @return true if the current player was a bot, and false otherwise
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
	 * NEW: Erases all lucky coins on the board, then checks if the specified player
	 * has a lucky coin. If so, places one at random.
	 * @param checkState The player to check lucky coin count for
	 * @return true if a lucky coin was placed, and false otherwise
	 */
	public boolean checkLuckyCoinPlace(Cell.State checkState) {
		int luckyCoinCount = (checkState == Cell.State.RED) ? redLuckyCoins : yellowLuckyCoins;
		if (luckyCoinCount > 0) {
			LinkedList<Integer> validMoves = new LinkedList<>();
			// put all valid columns in list
			for (int col = 0; col < maxColumns; col++) {
				if (board[col][maxRows - 1].isAvailable()) { // CHANGED: from pieces[row-1][5] != null
					validMoves.add(col);
				}
			}
			
			if (validMoves.size() == 0) return false; // if no valid moves exist, don't add coin
			
			// select column, then find top row and place
			int col = validMoves.get((int) (Math.random() * validMoves.size()));
			for (int row = 0; row < maxRows; row++) {
				if (board[col][row].isAvailable()) {
					board[col][row].setState(Cell.State.LUCKY);
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * NEW: Finds a lucky coin on the board, returning its location as a Point object.
	 * @return A Point object representing the lucky coin's location, with col as the x and row as the y, or NULL if none found.
	 */
	public Point findLuckyCoin() {
		for (int col = 0; col < maxColumns; col++) {
			for (int row = 0; row < maxRows; row++) {
				if (board[col][row].isLucky()) {
					return new Point(col, row);
				} else if (board[col][row].isAvailable()) {
					break; // No piece above empty cells
				}
			}
		}
		return null;
	}

	/**
	 * Writes the board and various game state variables to a file
	 * 
	 * @param name the name of the file you will create
	 * @return true if successful, false otherwise
	 */
	public boolean save() {

		return false;
	}

	/**
	 * loads game state from a named file
	 * 
	 * @param name the name of the file you will load
	 * @return true if successful, false otherwise
	 */
	public boolean load() {
		return false;
	}

	/**
	 * Undoes the last move. MODIFIED: Updated to work with Cell objects
	 * 
	 * @return true if undo was successful, false otherwise
	 */
	public boolean undo() {
		errorMessage = null;

		if (moves.empty()) {
			errorMessage = "No moves to undo.";
			return false;
		}
		
		// NEW: remove the lucky coin
		Point luckySpot = findLuckyCoin();
		if (luckySpot != null) {
			board[luckySpot.x][luckySpot.y].clear();
		}

		// Remove the last move
		Point lastMove = moves.pop();
		board[lastMove.x][lastMove.y].clear(); // CHANGED: from pieces[x][y] = null
		
		// NEW: Replace lucky coin based on history
		Point lastLuckyCoin = prevCoins.pop();
		if (lastLuckyCoin != null) {
			board[lastLuckyCoin.x][lastLuckyCoin.y].setState(Cell.State.LUCKY);
		}

		// Reset game over flags if necessary
		if (gameOver) {
			gameOver = false;
			redWins = false;
			yellowWins = false;
		} else if (!board[lastMove.x][lastMove.y].isLucky()) { // NEW: don't switch players if this was a lucky coin
			// Switch back to the previous player
			currentPlayer = (currentPlayer == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
			// CHANGED: from redsTurn = !redsTurn
		} else if (currentPlayer == Cell.State.RED) {
			redLuckyCoins++; // NEW: raise lucky coin count if it was red's turn
		} else {
			yellowLuckyCoins++; // NEW: raise lucky coin count if it was yellow's turn
		}

		// NEW: If it's a bot's turn, undo automatically
		Bot bot = null;
		if (currentPlayer == Cell.State.RED) {
			bot = redBot;
		} else if (currentPlayer == Cell.State.YELLOW) {
			bot = yellowBot;
		}
		if (bot != null) {
			if (!moves.empty()) {
				undo();
			} else {
				checkBotMove(); // auto move bot (when bot moved first)
			}
		}

		return true;
	}

	/**
	 * Resets the game to its initial state. MODIFIED: Updated to work with Cell
	 * objects
	 */
	public void restart() {
		// Clear all cells
		for (int i = 0; i < maxColumns; i++) {
			for (int j = 0; j < maxRows; j++) {
				board[i][j].clear(); // CHANGED: from pieces = new Boolean[7][6]
			}
		}
		gameOver = false;
		redWins = false;
		yellowWins = false;
		currentPlayer = Cell.State.RED; // CHANGED: from redsTurn = true
		moves.clear();
		prevCoins.clear(); // NEW: reset prev states
		errorMessage = null;
		redLuckyCoins = maxLuckyCoins; // NEW: reset lucky coins for both players
		yellowLuckyCoins = maxLuckyCoins; // NEW: reset lucky coins for both players
		checkBotMove(); // NEW: if red is a bot, auto move
	}

	/**
	 * Checks if the last move resulted in a win or tie. MODIFIED: Updated logic for
	 * Cell objects
	 */
	private void checkForWin() {
		gameOver = false;
		redWins = false;
		yellowWins = false;

		// Check for red win
		if (fourInARow(Cell.State.RED) || fourCorners(Cell.State.RED)) { // CHANGED: from fourInARow(true), added
																			// fourCorners()
			gameOver = true;
			redWins = true;
		}
		// Check for yellow win
		else if (fourInARow(Cell.State.YELLOW) || fourCorners(Cell.State.YELLOW)) { // CHANGED: from fourInARow(false),
																					// added fourCorners()
			gameOver = true;
			yellowWins = true;
		}
		// Check for tie
		else {
			boolean isTie = true;
			for (int i = 0; i < maxColumns; i++) {
				if (board[i][maxRows - 1].isAvailable()) { // CHANGED: from pieces[i][5] == null
					isTie = false;
					break;
				}
			}
			gameOver = isTie;
		}
	}

	/**
	 * Checks if there is a square made of a particular color. As in there are four
	 * corners not including in-between coins or the center. These can be any size
	 * that allows gaps, so at least 3x3.
	 * 
	 * @param playerState which player to check for
	 * @return true if a square is found using the current coin, false otherwise.
	 */
	private boolean fourCorners(Cell.State playerState) {
		for (int col = 0; col < maxColumns; col++) {
			for (int row = 0; row < maxRows; row++) {
				if (board[col][row].isAvailable()) {
					break; // No piece above empty cells
				} else if (board[col][row].getState() == playerState) {
					for (int i = 0; i < maxRows - row; i++) { // if a coin is found with your state it will loop right
																// until finding another
						if (board[col][row + i].getState() == playerState && i > 1 && i <= col
								&& board[col - i][row + i].getState() == playerState
								&& board[col - i][row].getState() == playerState) { // a series of further checks,
																					// looking for square
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if there are four pieces of the specified color in a row. MODIFIED:
	 * Updated to work with Cell.State instead of boolean
	 * 
	 * @param playerState the player's state to check for
	 * @return true if four in a row exists, false otherwise
	 */
	private boolean fourInARow(Cell.State playerState) { // CHANGED: from boolean color
		// Check vertical
		for (int col = 0; col < maxColumns; col++) {
			int count = 0;
			for (int row = 0; row < maxRows; row++) {
				if (board[col][row].isAvailable()) {
					break; // No piece above empty cells
				} else if (board[col][row].getState() == playerState) { // CHANGED: comparison method
					count++;
					if (count == 4)
						return true;
				} else {
					count = 0;
				}
			}
		}

		// Check horizontal
		for (int row = 0; row < maxRows; row++) {
			int count = 0;
			for (int col = 0; col < maxColumns; col++) {
				if (board[col][row].getState() == playerState) { // CHANGED: comparison method
					count++;
					if (count == 4)
						return true;
				} else {
					count = 0;
				}
			}
		}

		// Check diagonal (bottom-left to top-right: /)
		for (int startCol = 0; startCol < maxColumns; startCol++) {
			for (int startRow = 0; startRow < maxRows; startRow++) {
				int count = 0;
				for (int i = 0; startCol + i < maxColumns && startRow + i < maxRows; i++) {
					if (board[startCol + i][startRow + i].getState() == playerState) {
						count++;
						if (count == 4)
							return true;
					} else {
						count = 0;
					}
				}
			}
		}

		// Check diagonal (bottom-right to top-left: \)
		for (int startCol = 0; startCol < maxColumns; startCol++) {
			for (int startRow = 0; startRow < maxRows; startRow++) {
				int count = 0;
				for (int i = 0; startCol - i >= 0 && startRow + i < maxRows; i++) {
					if (board[startCol - i][startRow + i].getState() == playerState) {
						count++;
						if (count == 4)
							return true;
					} else {
						count = 0;
					}
				}
			}
		}

		return false;
	}

	// Getters

	public Cell[][] getBoard() { // CHANGED: from Boolean[][] getPieces()
		return board;
	}

	public Cell getCell(int col, int row) { // NEW METHOD
		if (col >= 0 && col < maxColumns && row >= 0 && row < maxRows) {
			return board[col][row];
		}
		return null;
	}

	public Stack<Point> getMoves() {
		return moves;
	}

	public String getErrorMessage() { // CHANGED: from getError()
		return errorMessage;
	}

	public boolean isRedWins() { // CHANGED: from getRedWins()
		return redWins;
	}

	public boolean isYellowWins() { // CHANGED: from getYellowWins()
		return yellowWins;
	}

	public Cell.State getCurrentPlayer() { // NEW: replaces getRedsTurn()
		return currentPlayer;
	}

	public boolean isGameOver() { // CHANGED: from getGameOver()
		return gameOver;
	}

	public int getColumns() { // NEW METHOD
		return maxColumns;
	}

	public int getRows() { // NEW METHOD
		return maxRows;
	}
}
