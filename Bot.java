import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public class Bot {
	private final int WINNING_VALUE = 2000;

	private int maxDepth;
	private int acceptableValueDist;

	/**
	 * Constructs a Bot with the given maximum depth acceptable value dist
	 * 
	 * @param maxDepth            The maximum amount of moves the Bot will look into
	 *                            the future
	 * @param acceptableValueDist How far away in value a move can be from the best
	 *                            move to be considered valid
	 */
	public Bot(int maxDepth, int acceptableValueDist) {
		this.maxDepth = maxDepth;
		this.acceptableValueDist = acceptableValueDist;
	}

	/**
	 * Constructs a Bot with a max depth of 4 and an acceptable value dist of zero
	 */
	public Bot() {
		this(4, 0);
	}

	/**
	 * Gets the move the bot will choose this turn for the specified player.
	 * 
	 * @param player The cell state of the currently active player
	 * @param model  The game model for the current game
	 * @return The column to be played in (1-indexed)
	 */
	public int getMove(Cell.State player, GameModel model) {
		HashMap<Integer, Integer> moveValues = new HashMap<>();

		int bestMoveValue = -WINNING_VALUE * 4;

		// if we or our opponent have a winning move, play in that location
		// this is so we always take the opportunity to win if we can, and
		// also always block our opponent.
		int winLocation = findWinningMove(player, model);
		if (winLocation == -1) { // check opponent if we don't have our own
			Cell.State opponentPlayer = (player == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
			winLocation = findWinningMove(opponentPlayer, model);
		}
		if (winLocation != -1) {
			//System.out.println("Win/block needed: "+(winLocation+1));
			return winLocation + 1;
		}

		Cell[][] board = model.getBoard();

		for (int col = 0; col < model.getColumns(); col++) {
			// Can't make a move if the specified column is full
			if (!model.getCell(col, model.getRows() - 1).isAvailable()) {
				continue;
			}

			int value = getMoveValue(col, player, model, true, -WINNING_VALUE, WINNING_VALUE, maxDepth);
			if (value >= WINNING_VALUE) {
				// don't need to think anymore, we found a winning move
				//System.out.println("Found winning: "+(col+1));
				return col + 1;
			} else if (bestMoveValue < value) {
				bestMoveValue = value;
			}
			moveValues.put(col, value);
		}

		//System.out.println("\nMove values:");
		LinkedList<Integer> validMoves = new LinkedList<>();
		// put moves in list based on value
		for (int col = 0; col < model.getColumns(); col++) {
			//System.out.println((col + 1) + " " + moveValues.get(col));
			if (moveValues.containsKey(col) && moveValues.get(col) >= bestMoveValue - acceptableValueDist) {
				validMoves.add(col);
			}
		}

		// pick random valid move
		double random = Math.random();
		return validMoves.get((int) (random * validMoves.size())) + 1;
	}

	/**
	 * Calculates the value of the move for the player playing in the specified
	 * column recursively using a min/max approach.
	 * 
	 * @param col      The column being played in
	 * @param player   The cell state of the currently active player
	 * @param model    The game model for the current game
	 * @param maximize = If we should maximize or minimize the score; determines if
	 *                 alpha or beta is set
	 * @param alpha    Highest value found by the maximizing player
	 * @param beta     = Lowest value found by the minimizing player
	 * @param depth    Amount of moves into the future to be considered
	 * @return The value of the move, between -2000 and 2000
	 */
	private int getMoveValue(int col, Cell.State player, GameModel model, boolean maximize, int alpha, int beta,
			int depth) {
		int COLUMNS = model.getColumns();
		int ROWS = model.getRows();

		// Find the next open space in the specified column
		int row = 0;
		while (row < ROWS && !model.getCell(col, row).isAvailable()) {
			row++;
		}

		Cell c = model.getCell(col, row);
		c.setState(player);
		int value = getPieceValueHere(col, row, player, model);

		if (depth != 0) {
			depth--;
			// get best move for opponent and subtract that value
			// most min-max algorithms have the opponent try to minimize the score,
			// but we instead maximize the score from their perspective,
			// accomplishing the same task.
			Cell.State opponentPlayer = (player == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
			int opponentBestValue = -WINNING_VALUE * 2;
			// get the opponent's win location or our own
			int winLocation = findWinningMove(opponentPlayer, model);
			if (winLocation == -1) {
				winLocation = findWinningMove(player, model); // check our own win location if the opponent doesn't have
																// one

				if (winLocation == -1) { // no win location found for us
					for (int col2 = 0; col2 < COLUMNS; col2++) {
						// Can't make a move if the specified column is full
						if (!model.getCell(col2, ROWS - 1).isAvailable()) {
							continue;
						}

						int opponentValue = getMoveValue(col2, opponentPlayer, model, !maximize, alpha, beta, depth);

						if (opponentBestValue < opponentValue) {
							opponentBestValue = opponentValue;
						}

						if (!maximize) { // if we're maximizing, opponent minimizes here
							alpha = Math.max(alpha, opponentValue);
						} else {
							beta = Math.min(beta, -opponentValue);
						}

						if (alpha >= beta || opponentBestValue >= WINNING_VALUE) {
							break; // alpha-beta pruning
						}
					}
				} else {
					// opponent MUST block
					opponentBestValue = getMoveValue(winLocation, opponentPlayer, model, !maximize, alpha, beta, depth);
					if (!maximize) { // if we're maximizing, opponent minimizes here
						alpha = Math.max(alpha, opponentBestValue);
					} else {
						beta = Math.min(beta, -opponentBestValue);
					}
				}
				value -= opponentBestValue;
			} else {
				value = -WINNING_VALUE * 2; // opponent wins
			}
		}

		c.setState(Cell.State.EMPTY);
		return value;
	}

	/**
	 * Returns a column the specified player can play in to win the game, or -1 if
	 * no winning column exists
	 * 
	 * @param player The cell state of the checked player
	 * @param model  The game model for the current game
	 * @return The column that allows the player to win the game, or -1 if no
	 *         winning column exists
	 */
	private int findWinningMove(Cell.State player, GameModel model) {
		int result = findFourInARowMove(player, model);
		if (result == -1)
			result = findFourCornersMove(player, model);
		return result;
	}

	/**
	 * Returns a column the specified player can play in to get a four in a row, or
	 * -1 if no such column exists
	 * 
	 * @param player The cell state of the checked player
	 * @param model  The game model for the current game
	 * @return The column that allows the player to get a four in a row, or -1 if no
	 *         such column exists
	 */
	private int findFourInARowMove(Cell.State player, GameModel model) {
		int COLUMNS = model.getColumns();
		int ROWS = model.getRows();

		// Check vertical (checks for 3 in a row and then a blank)
		for (int col = 0; col < COLUMNS; col++) {
			int count = 0;
			for (int row = 0; row < ROWS; row++) {
				Cell c = model.getCell(col, row);
				if (c.isAvailable()) {
					if (count == 3)
						return col;
					break; // No piece above empty cells
				} else if (c.getState() == player) { // CHANGED: comparison method
					count++;
					if (count == 4)
						return col;
				} else {
					count = 0;
				}
			}
		}

		// Check horizontal
		int blankCol = -1;
		for (int row = 0; row < ROWS; row++) {
			int count = 0;
			for (int col = 0; col < COLUMNS; col++) {
				Cell c = model.getCell(col, row);
				if (c.getState() == player) { // CHANGED: comparison method
					count++;
					if (count == 4)
						return blankCol;
				} else if (c.isAvailable() && blankCol == -1
						&& (row <= 0 || !(model.getCell(col, row - 1)).isAvailable())) {
					// count ONE blank move as valid if it can be played there
					count++;
					blankCol = col;
					if (count == 4)
						return blankCol;
				} else {
					count = 0;
					blankCol = -1;
				}
			}
		}

		// Check diagonal (bottom-left to top-right: /)
		blankCol = -1;
		for (int startCol = 0; startCol < COLUMNS; startCol++) {
			for (int startRow = 0; startRow < ROWS; startRow++) {
				int count = 0;
				for (int i = 0; startCol + i < COLUMNS && startRow + i < ROWS; i++) {
					int col = startCol + i;
					int row = startRow + i;
					Cell c = model.getCell(col, row);
					if (c.getState() == player) { // CHANGED: comparison method
						count++;
						if (count == 4)
							return blankCol;
					} else if (c.isAvailable() && blankCol == -1
							&& (row <= 0 || !(model.getCell(col, row - 1)).isAvailable())) {
						// count ONE blank move as valid if it can be played there
						count++;
						blankCol = col;
						if (count == 4)
							return blankCol;
					} else {
						count = 0;
						blankCol = -1;
					}
				}
			}
		}

		// Check diagonal (bottom-right to top-left: \)
		blankCol = -1;
		for (int startCol = 0; startCol < COLUMNS; startCol++) {
			for (int startRow = 0; startRow < ROWS; startRow++) {
				int count = 0;
				for (int i = 0; startCol - i >= 0 && startRow + i < ROWS; i++) {
					int col = startCol - i;
					int row = startRow + i;
					Cell c = model.getCell(col, row);
					if (c.getState() == player) { // CHANGED: comparison method
						count++;
						if (count == 4)
							return blankCol;
					} else if (c.isAvailable() && blankCol == -1
							&& (row <= 0 || !(model.getCell(col, row - 1)).isAvailable())) {
						// count ONE blank move as valid if it can be played there
						count++;
						blankCol = col;
						if (count == 4)
							return blankCol;
					} else {
						count = 0;
						blankCol = -1;
					}
				}
			}
		}

		return -1;
	}

	/**
	 * Returns a column the specified player can play in to get four corners, or -1
	 * if no such column exists
	 * 
	 * @param player The cell state of the checked player
	 * @param model  The game model for the current game
	 * @return The column that allows the player to get four corners, or -1 if no
	 *         such column exists
	 */
	private int findFourCornersMove(Cell.State player, GameModel model) {
		int COLUMNS = model.getColumns();
		int ROWS = model.getRows();

		for (int col = 0; col < COLUMNS; col++) {
			for (int row = 2; row < ROWS; row++) {
				Cell c = model.getCell(col, row);
				if (c.isAvailable()) {
					break; // No piece above empty cells
				} else if (c.getState() == player) {
					for (int i = 2; i <= row; i++) { // if a coin is found with your state it will loop down until
														// finding another
						if (model.getCell(col, row - i).getState() == player) {
							// bottom right is filled, top right is available
							if (col + i < COLUMNS && model.getCell(col + i, row - i).getState() == player
									&& model.getCell(col + i, row).isAvailable()
									&& !model.getCell(col + i, row - 1).isAvailable()) {
								return col + i;
							}

							// bottom left is filled, top left is available
							if (col - i >= 0 && model.getCell(col - i, row - i).getState() == player
									&& model.getCell(col - i, row).isAvailable()
									&& !model.getCell(col - i, row - 1).isAvailable()) {
								return col - i;
							}
						}
					}
				}
			}
		}

		return -1;
	}

	private int getPieceValueHere(int col, int row, Cell.State player, GameModel model) {
		int value = 0;

		// special case for vertical down
		int count = 1;
		for (int i = 1; i < row; i++) {
			Cell c = model.getCell(col, row - i);
			if (c.getState() == player) {
				count++;
				if (count >= 4) {
					return WINNING_VALUE * 2; // win
				}
			} else {
				break;
			}
		}
		value += (count - 1);

		count = countInLine(col, row, 1, 0, player, model); // horizontal right
		if (count >= 4)
			return WINNING_VALUE * 2;
		value += (count - 1);
		count = countInLine(col, row, -1, 0, player, model); // horizontal left
		if (count >= 4)
			return WINNING_VALUE * 2;
		value += (count - 1);
		count = countInLine(col, row, 1, 1, player, model); // vertical up-right
		if (count >= 4)
			return WINNING_VALUE * 2;
		value += (count - 1);
		count = countInLine(col, row, 1, -1, player, model); // vertical down-right
		if (count >= 4)
			return WINNING_VALUE * 2;
		value += (count - 1);
		count = countInLine(col, row, -1, 1, player, model); // vertical up-left
		if (count >= 4)
			return WINNING_VALUE * 2;
		value += (count - 1);
		count = countInLine(col, row, -1, -1, player, model); // vertical down-left
		if (count >= 4)
			return WINNING_VALUE * 2;
		value += (count - 1);

		// square check (top left and top right)
		int COLUMNS = model.getColumns();
		int ROWS = model.getRows();
		for (int i = 2; i < row; i++) {
			Cell c = model.getCell(col, row - i);
			if (c.getState() == player) {
				// right side
				count = 2;
				if (col + i < COLUMNS) {
					Cell c2 = model.getCell(col + i, row - i); // bottom right
					if (row > 0 && model.getCell(col + i, row - i - 1).isAvailable()) {
						// nothing; can't play at this square
					} else if (c2.isAvailable()) {
						// nothing
					} else if (c2.getState() == player) {
						count++;
						Cell c3 = model.getCell(col + i, row); // top right
						if (c2.isAvailable() || model.getCell(col + i, row - 1).isAvailable()) {
							// nothing; can't play at this square, or it is empty
						} else if (c3.getState() == player) {
							count++;
							return WINNING_VALUE; // win
						} else {
							count = 1;
						}
					} else {
						count = 1;
					}
				}
				value += (count - 1);

				// left side
				count = 2;
				if (col - i >= 0) {
					Cell c2 = model.getCell(col - i, row - i); // bottom right
					if (row > 0 && model.getCell(col - i, row - i - 1).isAvailable()) {
						// nothing; can't play at this square
					} else if (c2.isAvailable()) {
						// nothing
					} else if (c2.getState() == player) {
						count++;
						Cell c3 = model.getCell(col - i, row); // top right
						if (c2.isAvailable() || model.getCell(col - i, row - 1).isAvailable()) {
							// nothing; can't play at this square, or it is empty
						} else if (c3.getState() == player) {
							count++;
							return WINNING_VALUE; // win
						} else {
							count = 1;
						}
					} else {
						count = 1;
					}
				}
				value += (count - 1);
			}
		}
		
		if (row < ROWS - 2) {
			// square check (bottom right)
			count = 1;
			for (int i = 2; i <= col; i++) {
				if (row + i >= ROWS)
					break; // don't go oob

				Cell c = model.getCell(col - i, row); // bottom left
				if (c.getState() == player) {
					count++;
					Cell c2 = model.getCell(col - i, row + i); // top left
					if (row > 0 && model.getCell(col - i, row + i - 1).isAvailable()) {
						// nothing; can't play at this square
					} else if (c2.isAvailable()) {
						// nothing
					} else if (c2.getState() == player) {
						count++;
					} else {
						count = 1;
					}
				}
				value += (count - 1);
			}

			// square check (bottom left)
			count = 1;
			for (int i = 2; i < COLUMNS - col; i++) {
				if (row + i >= ROWS)
					break; // don't go oob

				Cell c = model.getCell(col + i, row); // bottom right
				if (c.getState() == player) {
					count++;
					Cell c2 = model.getCell(col + i, row + i); // top right
					if (row > 0 && model.getCell(col + i, row + i - 1).isAvailable()) {
						// nothing; can't play at this square
					} else if (c2.isAvailable()) {
						// nothing
					} else if (c2.getState() == player) {
						count++;
					} else {
						count = 1;
					}
				}
				value += (count - 1);
			}
		}

		return value;
	}

	private int countInLine(int startCol, int startRow, int colChange, int rowChange, Cell.State player,
			GameModel model) {
		int count = 1;
		boolean foundEmpty = false;

		int COLUMNS = model.getColumns();
		int ROWS = model.getRows();

		for (int i = 1; i <= Math.max(ROWS, COLUMNS); i++) {
			int row = startRow + rowChange * i;
			int col = startCol + colChange * i;
			if (row < 0 || row >= ROWS || col <= 0 || col >= COLUMNS)
				break; // avoid OOB

			Cell c = model.getCell(col, row);
			if (row > 0 && model.getCell(col, row - 1).isAvailable()) {
				break; // can't play at this square
			} else if (c.isAvailable()) {
				if (foundEmpty) {
					break;
				} else {
					foundEmpty = true;
				}
			} else if (c.getState() == player) {
				count++;
				if (count >= 4 && !foundEmpty) {
					return 4;
				}
			} else {
				count = 1;
				break;
			}
		}
		return count;
	}
}
