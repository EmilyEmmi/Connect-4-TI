import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;

/**
 * REFACTORED FROM GameState.java Pure game logic model with no UI dependencies.
 * Manages the Connect Four game state, rules, and move validation.
 */
public class GameModel {
	private Cell[][] board;
	private Stack<Point> moves;
	private Stack<Point> prevCoins;
	private String errorMessage;
	private boolean gameOver;
	private boolean redWins;
	private boolean yellowWins;
	private Cell.State currentPlayer;
	private Bot redBot;
	private Bot yellowBot;
	private int maxColumns;
	private int maxRows;
	private boolean playerIsYellow;
	private int maxLuckyCoins;
	private int remainingLuckyCoins; // NEW: Shared counter for lucky coins
	private Difficulty currentDifficulty;
	private boolean processingBotMove;
	
	// NEW: Lucky coin spawning randomly around every 3 moves
	private int moveCounter;
	private int nextLuckyCoinMove;
	
	// NEW: Scoreboard for Human difficulty
	private int redScore;
	private int yellowScore;
	
	// NEW: Status message for temporary display
	private String statusMessage;
	private long statusMessageTime;
	private static final long STATUS_MESSAGE_DURATION = 3000; // 3 seconds

	public enum Difficulty {
		HUMAN, BEGINNER, INTERMEDIATE, EXPERT,
	}

	/**
	 * Creates a new game model with an empty board. Red player starts first.
	 */
	public GameModel() {
		maxColumns = 7;
		maxRows = 6;
		maxLuckyCoins = 3;
		remainingLuckyCoins = maxLuckyCoins; // Initialize shared counter
		redBot = null;
		yellowBot = null;
		playerIsYellow = false;
		currentDifficulty = Difficulty.HUMAN;
		processingBotMove = false;
		
		// NEW: Initialize lucky coin counter with randomness
		moveCounter = 0;
		nextLuckyCoinMove = 2 + (int)(Math.random() * 3); // Random between 2-4
		
		// NEW: Initialize scoreboard
		redScore = 0;
		yellowScore = 0;
		
		// NEW: Initialize status message
		statusMessage = null;
		statusMessageTime = 0;

		board = new Cell[maxColumns][maxRows];
		for (int i = 0; i < maxColumns; i++) {
			for (int j = 0; j < maxRows; j++) {
				board[i][j] = new Cell();
			}
		}
		gameOver = false;
		redWins = false;
		yellowWins = false;
		currentPlayer = Cell.State.RED;
		moves = new Stack<>();
		prevCoins = new Stack<>();
		errorMessage = null;

		checkBotMove();
	}

	/**
	 * NEW: Updates the information about the board, erasing the board in the
	 * process
	 * 
	 * @param difficulty The difficulty. 0 is human, 1-3 affect the bot complexity and board size
	 */
	public void updateValues(Difficulty difficulty) {
		currentDifficulty = difficulty;
		
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
	 * Attempts to place a coin in the specified column.
	 * 
	 * @param column the column number (1-7)
	 * @return true if the move was successful, false otherwise
	 */
	public boolean makeMove(int column) {
		errorMessage = null;

		if (column < 1 || column > maxColumns) {
			errorMessage = "Invalid column. Please choose 1-" + maxColumns + ".";
			return false;
		}

		if (gameOver) {
			errorMessage = "The game is over.";
			return false;
		}

		int col = column - 1;

		if (!board[col][maxRows - 1].isAvailable()) {
			errorMessage = "That column is full.";
			return false;
		}

		// Find the next open space in the specified column
		int row = 0;
		while (row < maxRows && !board[col][row].isAvailable()) {
			row++;
		}
		
		// NEW: Check if this cell is a lucky coin BEFORE we find other lucky coins
		Cell.State prevState = board[col][row].getState();
		boolean wasLuckyCoin = (prevState == Cell.State.LUCKY);
		
		// Find any OTHER lucky coin on the board (not the one we're about to claim)
		Point luckySpot = null;
		if (!wasLuckyCoin) {
			luckySpot = findLuckyCoin();
		}
		prevCoins.push(luckySpot);

		// Place the piece (this replaces the lucky coin if there was one)
		board[col][row].setState(currentPlayer);

		// Add the move to the move history
		moves.push(new Point(col, row));
		
		// NEW: Increment move counter
		moveCounter++;

		// Check if this move won the game
		checkForWin();

		// Switch players if game is not over
		if (!gameOver) {
			// Remove the old lucky coin if it exists
			if (luckySpot != null) {
				board[luckySpot.x][luckySpot.y].clear();
			}
			
			// NEW: Check if we should spawn a lucky coin (randomly around every 3 moves)
			if (moveCounter >= nextLuckyCoinMove && remainingLuckyCoins > 0) {
				boolean spawned = checkLuckyCoinPlace(currentPlayer); // Parameter doesn't matter
				if (spawned) {
					remainingLuckyCoins--; // Decrease counter when lucky coin spawns
					System.out.println("Lucky coin spawned! Remaining: " + remainingLuckyCoins);
				}
				nextLuckyCoinMove = moveCounter + 2 + (int)(Math.random() * 3); // Next in 2-4 moves
			}
			
			if (wasLuckyCoin) {
				// Lucky coin was claimed - player gets another turn
				String playerColor = (currentPlayer == Cell.State.RED) ? "Red" : "Yellow";
				setStatusMessage(playerColor + " claimed the Lucky Coin!");
				// Don't switch players - current player goes again
			} else {
				// Normal move - switch players
				currentPlayer = (currentPlayer == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
			}
			
			// Check if the current player is a bot and should make a move
			if (!processingBotMove) {
				checkBotMove();
			}
		} else {
			// NEW: Update scoreboard when game ends in Human difficulty
			if (currentDifficulty == Difficulty.HUMAN) {
				if (redWins) {
					redScore++;
				} else if (yellowWins) {
					yellowScore++;
				}
			}
		}

		return true;
	}

	/**
	 * Checks if the current player is a bot, and if they are, makes their move.
	 * @return true if the current player was a bot, and false otherwise
	 */
	public boolean checkBotMove() {
		// Don't allow nested bot moves
		if (processingBotMove || gameOver) {
			return false;
		}
		
		Bot bot = null;
		if (currentPlayer == Cell.State.RED) {
			bot = redBot;
		} else if (currentPlayer == Cell.State.YELLOW) {
			bot = yellowBot;
		}

		if (bot != null) {
			processingBotMove = true;
			try {
				int column = bot.getMove(currentPlayer, this);
				int attempts = 0;
				while (!makeMove(column) && attempts < 100) {
					column = bot.getMove(currentPlayer, this);
					attempts++;
				}
			} finally {
				processingBotMove = false;
			}
			
			// IMPORTANT: After bot finishes its move, check if it claimed a lucky coin
			// If it did, the currentPlayer is still the bot, so we need to make another bot move
			if (bot != null && currentPlayer == Cell.State.RED && redBot == bot) {
				// Bot is still red, call again for extra turn
				return checkBotMove();
			} else if (bot != null && currentPlayer == Cell.State.YELLOW && yellowBot == bot) {
				// Bot is still yellow, call again for extra turn
				return checkBotMove();
			}
			
			return true;
		}
		return false;
	}
	
	/**
	 * NEW: Erases all lucky coins on the board, then places one at random in a valid column.
	 * @param checkState The player to check lucky coin count for (not used anymore)
	 * @return true if a lucky coin was placed, and false otherwise
	 */
	public boolean checkLuckyCoinPlace(Cell.State checkState) {
		// Only spawn if we have remaining lucky coins
		if (remainingLuckyCoins <= 0) {
			return false;
		}
		
		LinkedList<Integer> validMoves = new LinkedList<>();
		// put all valid columns in list
		for (int col = 0; col < maxColumns; col++) {
			if (board[col][maxRows - 1].isAvailable()) {
				validMoves.add(col);
			}
		}
		
		if (validMoves.size() == 0) return false;
		
		// select column, then find top row and place
		int col = validMoves.get((int) (Math.random() * validMoves.size()));
		for (int row = 0; row < maxRows; row++) {
			if (board[col][row].isAvailable()) {
				board[col][row].setState(Cell.State.LUCKY);
				return true;
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
					break;
				}
			}
		}
		return null;
	}

	/**
	 * NEW: Writes the board and various game state variables to a file
	 * File format:
	 * - maxColumns, maxRows, maxLuckyCoins
	 * - currentDifficulty, playerIsYellow
	 * - moveCounter, nextLuckyCoinMove
	 * - remainingLuckyCoins (shared counter)
	 * - redScore, yellowScore
	 * - board state (col by col, row by row)
	 * - currentPlayer
	 * - gameOver, redWins, yellowWins
	 * - move history
	 * - previous coins history
	 * @return true if successful, false otherwise
	 */
	public boolean save() {
		FileWriter writer = null;
		try {
			writer = new FileWriter("game.txt");
			
			// Save board dimensions and difficulty settings
			writer.write(maxColumns + "\n");
			writer.write(maxRows + "\n");
			writer.write(maxLuckyCoins + "\n");
			writer.write(currentDifficulty.toString() + "\n");
			writer.write(playerIsYellow + "\n");
			
			// Save move counter and next lucky coin move
			writer.write(moveCounter + "\n");
			writer.write(nextLuckyCoinMove + "\n");
			
			// Save remaining lucky coins (shared counter)
			writer.write(remainingLuckyCoins + "\n");
			
			// Save scoreboard
			writer.write(redScore + "\n");
			writer.write(yellowScore + "\n");
			
			// Save board state
			System.out.println("Saving board state: " + maxColumns + " columns, " + maxRows + " rows");
			int pieceCount = 0;
			for (int col = 0; col < maxColumns; col++) {
				for (int row = 0; row < maxRows; row++) {
					Cell.State state = board[col][row].getState();
					writer.write(state.toString() + "\n");
					if (state != Cell.State.EMPTY) {
						System.out.println("Saving piece at col=" + col + ", row=" + row + ": " + state);
						pieceCount++;
					}
				}
			}
			System.out.println("Saved " + pieceCount + " pieces to file");
			
			// Save current player
			writer.write(currentPlayer.toString() + "\n");
			
			// Save game state flags
			writer.write(gameOver + "\n");
			writer.write(redWins + "\n");
			writer.write(yellowWins + "\n");
			
			// Save move history
			writer.write(moves.size() + "\n");
			for (Point p : moves) {
				writer.write(p.x + "," + p.y + "\n");
			}
			
			// Save previous coins history
			writer.write(prevCoins.size() + "\n");
			for (Point p : prevCoins) {
				if (p == null) {
					writer.write("null\n");
				} else {
					writer.write(p.x + "," + p.y + "\n");
				}
			}
			
			// Set status message
			setStatusMessage("Game saved successfully!");
			System.out.println("Game saved to: " + new File("game.txt").getAbsolutePath());
			
			return true;
		} catch (IOException e) {
			setStatusMessage("Save failed!");
			System.err.println("Failed to save game: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					System.err.println("Error closing file: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * NEW: Loads game state from a named file
	 * @return true if successful, false otherwise
	 */
	public boolean load() {
		Scanner scanner = null;
		try {
			File file = new File("game.txt");
			if (!file.exists()) {
				setStatusMessage("No save file found!");
				System.err.println("Save file not found at: " + file.getAbsolutePath());
				return false;
			}
			
			scanner = new Scanner(file);
			
			// Load board dimensions and difficulty settings
			maxColumns = Integer.parseInt(scanner.nextLine().trim());
			maxRows = Integer.parseInt(scanner.nextLine().trim());
			maxLuckyCoins = Integer.parseInt(scanner.nextLine().trim());
			currentDifficulty = Difficulty.valueOf(scanner.nextLine().trim());
			playerIsYellow = Boolean.parseBoolean(scanner.nextLine().trim());
			
			// Load move counter and next lucky coin move
			moveCounter = Integer.parseInt(scanner.nextLine().trim());
			nextLuckyCoinMove = Integer.parseInt(scanner.nextLine().trim());
			
			// Load remaining lucky coins (shared counter)
			remainingLuckyCoins = Integer.parseInt(scanner.nextLine().trim());
			
			// Load scoreboard
			redScore = Integer.parseInt(scanner.nextLine().trim());
			yellowScore = Integer.parseInt(scanner.nextLine().trim());
			
			// Recreate bots based on difficulty
			redBot = null;
			yellowBot = null;
			switch (currentDifficulty) {
				case BEGINNER:
					yellowBot = new Bot(2, 20);
					break;
				case INTERMEDIATE:
					yellowBot = new Bot(3, 10);
					break;
				case EXPERT:
					yellowBot = new Bot(4, 5);
					break;
				default:
					break;
			}
			
			// Swap bot to red if player is yellow
			if (playerIsYellow && yellowBot != null) {
				redBot = yellowBot;
				yellowBot = null;
			}
			
			// Reinitialize board with loaded dimensions
			board = new Cell[maxColumns][maxRows];
			for (int i = 0; i < maxColumns; i++) {
				for (int j = 0; j < maxRows; j++) {
					board[i][j] = new Cell();
				}
			}
			
			// Load board state
			System.out.println("Loading board state: " + maxColumns + " columns, " + maxRows + " rows");
			for (int col = 0; col < maxColumns; col++) {
				for (int row = 0; row < maxRows; row++) {
					if (!scanner.hasNextLine()) {
						setStatusMessage("Corrupted save file!");
						System.err.println("Save file is corrupted or incomplete at col=" + col + ", row=" + row);
						return false;
					}
					String stateStr = scanner.nextLine().trim();
					try {
						Cell.State state = Cell.State.valueOf(stateStr);
						board[col][row].setState(state);
						if (state != Cell.State.EMPTY) {
							System.out.println("Loaded piece at col=" + col + ", row=" + row + ": " + state);
						}
					} catch (IllegalArgumentException e) {
						setStatusMessage("Invalid save file!");
						System.err.println("Invalid cell state in save file: " + stateStr);
						return false;
					}
				}
			}
			System.out.println("Board state loaded successfully");
			
			// Load current player
			if (!scanner.hasNextLine()) {
				setStatusMessage("Corrupted save file!");
				System.err.println("Save file missing current player");
				return false;
			}
			currentPlayer = Cell.State.valueOf(scanner.nextLine().trim());
			
			// Load game state flags
			gameOver = Boolean.parseBoolean(scanner.nextLine().trim());
			redWins = Boolean.parseBoolean(scanner.nextLine().trim());
			yellowWins = Boolean.parseBoolean(scanner.nextLine().trim());
			
			// Load move history
			moves.clear();
			int movesSize = Integer.parseInt(scanner.nextLine().trim());
			for (int i = 0; i < movesSize; i++) {
				String[] coords = scanner.nextLine().trim().split(",");
				moves.push(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
			}
			
			// Load previous coins history
			prevCoins.clear();
			int prevCoinsSize = Integer.parseInt(scanner.nextLine().trim());
			for (int i = 0; i < prevCoinsSize; i++) {
				String line = scanner.nextLine().trim();
				if (line.equals("null")) {
					prevCoins.push(null);
				} else {
					String[] coords = line.split(",");
					prevCoins.push(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
				}
			}
			
			// Reset processing flag
			processingBotMove = false;
			
			// Set status message
			setStatusMessage("Game loaded successfully!");
			System.out.println("Game loaded from: " + file.getAbsolutePath());
			
			// Check if bot should make a move
			if (!gameOver) {
				checkBotMove();
			}
			
			return true;
		} catch (IOException e) {
			setStatusMessage("Load failed!");
			System.err.println("Failed to load game: " + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			setStatusMessage("Load error!");
			System.err.println("Error loading game: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	/**
	 * Undoes the last move.
	 * 
	 * @return true if undo was successful, false otherwise
	 */
	public boolean undo() {
		errorMessage = null;

		if (moves.empty()) {
			errorMessage = "No moves to undo.";
			return false;
		}
		
		// Remove the lucky coin
		Point luckySpot = findLuckyCoin();
		if (luckySpot != null) {
			board[luckySpot.x][luckySpot.y].clear();
			remainingLuckyCoins++;
		}

		// Remove the last move
		Point lastMove = moves.pop();
		Cell.State lastMoveState = board[lastMove.x][lastMove.y].getState();
		board[lastMove.x][lastMove.y].clear();
		
		// NEW: Decrement move counter
		moveCounter--;
		
		// Replace lucky coin based on history
		Point lastLuckyCoin = prevCoins.pop();
		if (lastLuckyCoin != null) {
			board[lastLuckyCoin.x][lastLuckyCoin.y].setState(Cell.State.LUCKY);
		}

		// Reset game over flags if necessary
		if (gameOver) {
			gameOver = false;
			redWins = false;
			yellowWins = false;
		} else if (lastMoveState != Cell.State.LUCKY) {
			// Switch back to the previous player
			currentPlayer = (currentPlayer == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
		}
		// Note: If lucky coin was claimed, we don't increment remainingLuckyCoins
		// because it already spawned and used up one from the counter

		// If it's a bot's turn, undo automatically
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
				checkBotMove();
			}
		}

		return true;
	}

	/**
	 * Resets the game to its initial state.
	 */
	public void restart() {
		// Clear all cells
		for (int i = 0; i < maxColumns; i++) {
			for (int j = 0; j < maxRows; j++) {
				board[i][j].clear();
			}
		}
		gameOver = false;
		redWins = false;
		yellowWins = false;
		currentPlayer = Cell.State.RED;
		moves.clear();
		prevCoins.clear();
		errorMessage = null;
		remainingLuckyCoins = maxLuckyCoins; // Reset shared counter
		processingBotMove = false;
		
		// NEW: Reset move counter with randomness
		moveCounter = 0;
		nextLuckyCoinMove = 2 + (int)(Math.random() * 3); // Random between 2-4
		
		// NEW: Don't reset scoreboard - it persists across restarts
		
		checkBotMove();
	}

	/**
	 * Checks if the last move resulted in a win or tie.
	 */
	private void checkForWin() {
		gameOver = false;
		redWins = false;
		yellowWins = false;

		// Check for red win
		if (fourInARow(Cell.State.RED) || fourCorners(Cell.State.RED)) {
			gameOver = true;
			redWins = true;
		}
		// Check for yellow win
		else if (fourInARow(Cell.State.YELLOW) || fourCorners(Cell.State.YELLOW)) {
			gameOver = true;
			yellowWins = true;
		}
		// Check for tie
		else {
			boolean isTie = true;
			for (int i = 0; i < maxColumns; i++) {
				if (board[i][maxRows - 1].isAvailable()) {
					isTie = false;
					break;
				}
			}
			gameOver = isTie;
		}
	}

	/**
	 * Checks if there is a square made of a particular color.
	 * 
	 * @param playerState which player to check for
	 * @return true if a square is found using the current coin, false otherwise.
	 */
	private boolean fourCorners(Cell.State playerState) {
		for (int col = 0; col < maxColumns; col++) {
			for (int row = 0; row < maxRows; row++) {
				if (board[col][row].isAvailable()) {
					break;
				} else if (board[col][row].getState() == playerState) {
					for (int i = 0; i < maxRows - row; i++) {
						if (board[col][row + i].getState() == playerState && i > 1 && i <= col
								&& board[col - i][row + i].getState() == playerState
								&& board[col - i][row].getState() == playerState) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if there are four pieces of the specified color in a row.
	 * 
	 * @param playerState the player's state to check for
	 * @return true if four in a row exists, false otherwise
	 */
	private boolean fourInARow(Cell.State playerState) {
		// Check vertical
		for (int col = 0; col < maxColumns; col++) {
			int count = 0;
			for (int row = 0; row < maxRows; row++) {
				if (board[col][row].isAvailable()) {
					break;
				} else if (board[col][row].getState() == playerState) {
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
				if (board[col][row].getState() == playerState) {
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
	
	// NEW: Status message methods
	/**
	 * Sets a temporary status message
	 * @param message the message to display
	 */
	public void setStatusMessage(String message) {
		this.statusMessage = message;
		this.statusMessageTime = System.currentTimeMillis();
	}
	
	/**
	 * Gets the current status message if it hasn't expired
	 * @return the status message or null if expired
	 */
	public String getStatusMessage() {
		if (statusMessage != null && System.currentTimeMillis() - statusMessageTime < STATUS_MESSAGE_DURATION) {
			return statusMessage;
		}
		return null;
	}

	// Getters

	public Cell[][] getBoard() {
		return board;
	}

	public Cell getCell(int col, int row) {
		if (col >= 0 && col < maxColumns && row >= 0 && row < maxRows) {
			return board[col][row];
		}
		return null;
	}

	public Stack<Point> getMoves() {
		return moves;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isRedWins() {
		return redWins;
	}

	public boolean isYellowWins() {
		return yellowWins;
	}

	public Cell.State getCurrentPlayer() {
		return currentPlayer;
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public int getColumns() {
		return maxColumns;
	}

	public int getRows() {
		return maxRows;
	}
	
	// NEW: Getters for scoreboard
	public int getRedScore() {
		return redScore;
	}
	
	public int getYellowScore() {
		return yellowScore;
	}
	
	public Difficulty getDifficulty() {
		return currentDifficulty;
	}
}
