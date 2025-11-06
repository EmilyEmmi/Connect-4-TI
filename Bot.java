import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public class Bot {
	private final int MAX_DEPTH = 4;
	
	public int getMove(Cell.State player, GameModel model) {
		HashMap<Integer, Integer> moveValues = new HashMap<>();
		
		// lowest move value is a bit less than -2000
		int bestMoveValue = -4000;
		
		// how far away in value this move can be from the best value to be valid
		// 0 always picks the best move
		int acceptableValueDist = 0;
		
		Cell[][] board = model.getBoard();
		
		for (int col = 0; col < model.getColumns(); col++) {
			// Can't make a move if the specified column is full
	        if (!board[col][model.getRows() - 1].isAvailable()) {
	            continue;
	        }
	        
	        int value = getMoveValue(col, player, board, MAX_DEPTH);
	        if (value >= 1000) {
		        // don't need to think anymore, we found a winning move
        		return col+1;
        	} else if (bestMoveValue < value) {
	        	bestMoveValue = value;
	        }
	        moveValues.put(col, value);
		}
		
		//System.out.println("\nMove values:");
		LinkedList<Integer> validMoves = new LinkedList<>();
		// put moves in list based on value
		for (int col = 0; col < model.getColumns(); col++) {
			//System.out.println((col+1)+" "+moveValues.get(col));
			if (moveValues.containsKey(col) && moveValues.get(col) >= bestMoveValue - acceptableValueDist) {
				validMoves.add(col);
			}
		}
		
		// pick random valid move
		double random = Math.random();
		return validMoves.get((int)(random * validMoves.size()))+1;
	}
	
	private int getMoveValue(int col, Cell.State player, Cell[][] board, int depth) {
        // Find the next open space in the specified column
        int row = 0;
        while (row < board[col].length && !board[col][row].isAvailable()) {
            row++;
        }
        
        int value = 0;
        board[col][row].setState(player);
        // basic check for win logic (TODO: more advanced; try to create win conditions)
        if (fourInARow(player, board)) {
            board[col][row].setState(Cell.State.EMPTY);
        	return 2000; // win
		}
        
        if (depth != 0) {
        	depth--;
        	// get best move for opponent and subtract that value (TODO: pruning)
        	Cell.State opponentPlayer = (player == Cell.State.RED) ? Cell.State.YELLOW : Cell.State.RED;
        	int opponentBestValue = -4000;
        	
        	for (int col2 = 0; col2 < board.length; col2++) {
    			// Can't make a move if the specified column is full
    	        if (!board[col2][board[col2].length - 1].isAvailable()) {
    	            continue;
    	        }
    	        
    	        int opponentValue = getMoveValue(col2, opponentPlayer, board, depth);
    	        if (opponentBestValue < opponentValue) {
    	        	opponentBestValue = opponentValue;
    	        	if (opponentBestValue >= 1000) break;
    	        }
        	}
        	value -= (opponentBestValue + depth); // prioritze blocking opponent even if it's hopeless
        }
        
        board[col][row].setState(Cell.State.EMPTY);
		return value;
	}
	
	// copy from gameModel (TEMP!!!)
	private boolean fourInARow(Cell.State playerState, Cell[][] board) {  // CHANGED: from boolean color
        int COLUMNS = board.length;
        int ROWS = board[0].length;
		
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
}
