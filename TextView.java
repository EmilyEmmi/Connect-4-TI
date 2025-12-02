import java.io.IOException;
import java.util.Scanner;

/**
 * NEW CLASS - Text-based console view for the Connect Four game. Allows users
 * to play the game using text commands.
 * NEW: Scoreboard display for Human difficulty
 * NEW: Status message display for save/load/lucky coin claims
 */
public class TextView implements GameView {
	private GameModel model;
	private Scanner scanner;
	private boolean running;

	/**
	 * Creates a new text-based view for the game.
	 * 
	 * @param model the game model to display
	 */
	public TextView(GameModel model) {
		this.model = model;
		this.scanner = new Scanner(System.in);
		this.running = false;
	}

	@Override
	public void display() throws IOException {
		running = true;
		printWelcome();
		printHelp();
		model.restart();
		updateView();

		// Main game loop
		while (running) {
			processCommand();
		}

		scanner.close();
	}

	@Override
	public void updateView() {
		System.out.println();
		
		// NEW: Display scoreboard if in Human difficulty
		if (model.getDifficulty() == GameModel.Difficulty.HUMAN) {
			printScoreboard();
		}
		
		printBoard();
		printGameState();
		
		// NEW: Display status message if present
		String statusMsg = model.getStatusMessage();
		if (statusMsg != null) {
			System.out.println("\n>>> " + statusMsg + " <<<");
		}
	}

	@Override
	public void showError(String message) {
		System.out.println("ERROR: " + message);
	}

	@Override
	public void showGameOver(String message) {
		System.out.println("\n" + "=".repeat(40));
		System.out.println("GAME OVER: " + message);
		System.out.println("=".repeat(40));
	}

	/**
	 * Prints the welcome message and prompts for difficulty.
	 */
	private void printWelcome() {
		System.out.println("=".repeat(40));
		System.out.println("       WELCOME TO CONNECT FOUR");
		System.out.println("=".repeat(40));

		System.out.println();
		String input = "";
		boolean prompt = true;
		while (prompt) {
			System.out.println("Select difficulty: ");
			System.out.println("0 - Human");
			System.out.println("1 - Beginner");
			System.out.println("2 - Intermediate");
			System.out.println("3 - Expert");
			System.out.print("\nInput: ");

			input = scanner.nextLine().trim().toLowerCase();
			prompt = false;
			switch (input) {
			default: {
				System.out.println("Invalid difficulty!");
				prompt = true;
				break;
			}
			case "0": {
				model.updateValues(GameModel.Difficulty.HUMAN);
				break;
			}
			case "1": {
				model.updateValues(GameModel.Difficulty.BEGINNER);
				break;
			}
			case "2": {
				model.updateValues(GameModel.Difficulty.INTERMEDIATE);
				break;
			}
			case "3": {
				model.updateValues(GameModel.Difficulty.EXPERT);
				break;
			}
			}
		}
		
		// prompt for color if we didn't pick human
		prompt = (!input.equals("0"));
		while (prompt) {
			System.out.println("Select color: ");
			System.out.println("0 - Red");
			System.out.println("1 - Yellow");
			System.out.print("\nInput: ");
			
			input = scanner.nextLine().trim().toLowerCase();
			prompt = false;
			switch (input) {
			default: {
				System.out.println("Invalid color!");
				prompt = true;
				break;
			}
			case "0": {
				model.updateBotColor(Cell.State.YELLOW);
				break;
			}
			case "1": {
				model.updateBotColor(Cell.State.RED);
				break;
			}
			}
		}
	}

	/**
	 * Prints the instructions.
	 */
	private void printHelp() {
		System.out.println("\nCommands:");
		System.out.println("  1-"+model.getColumns()+"      : Place piece in column 1-"+model.getColumns());
		System.out.println("  undo     : Undo last move");
		System.out.println("  save     : Records the current board");
		System.out.println("  load     : Resumes saved board");
		System.out.println("  restart  : Restart game");
		System.out.println("  help     : Show this help");
		System.out.println("  quit     : Exit game");
		System.out.println();
		System.out.println("Legend:");
		System.out.println("  R = Red coin");
		System.out.println("  Y = Yellow coin");
		System.out.println("  L = Lucky coin (can be claimed for extra turn)");
		System.out.println();
	}
	
	/**
	 * NEW METHOD: Prints the scoreboard for Human difficulty
	 */
	private void printScoreboard() {
		System.out.println("\n" + "=".repeat(40));
		System.out.println("SCOREBOARD - Red: " + model.getRedScore() + " | Yellow: " + model.getYellowScore());
		System.out.println("=".repeat(40));
	}

	/**
	 * FIXED: Prints the current game board with proper lucky coin display
	 */
	private void printBoard() {
		String columnNumbers = "";
		for (int i = 1; i <= model.getColumns(); i++) {
			columnNumbers += (i+"  ");
			if (i < 10) {
				columnNumbers += " "; // more space for single digit numbers
			}
		}

		System.out.println("\n  "+columnNumbers.trim());
		System.out.println("+"+"---+".repeat(model.getColumns()));

		// Print from top row to bottom
		for (int row = model.getRows() - 1; row >= 0; row--) {
			System.out.print("|");
			for (int col = 0; col < model.getColumns(); col++) {
				Cell cell = model.getCell(col, row);
				String symbol;
				// FIXED: Added proper lucky coin display
				if (cell.isRed()) {
					symbol = " R ";
				} else if (cell.isYellow()) {
					symbol = " Y ";
				} else if (cell.isLucky()) {
					symbol = " L "; // Lucky coin display
				} else {
					symbol = "   "; // Empty cell
				}
				System.out.print(symbol + "|");
			}
			System.out.println();
			System.out.println("+"+"---+".repeat(model.getColumns()));
		}
	}

	/**
	 * Prints the current game state.
	 */
	private void printGameState() {
		if (model.isRedWins()) {
			showGameOver("RED WINS!");
		} else if (model.isYellowWins()) {
			showGameOver("YELLOW WINS!");
		} else if (model.isGameOver()) {
			showGameOver("IT'S A TIE!");
		} else {
			String player = (model.getCurrentPlayer() == Cell.State.RED) ? "RED" : "YELLOW";
			System.out.println("\nCurrent Player: " + player);
		}

		if (model.getErrorMessage() != null) {
			showError(model.getErrorMessage());
		}
	}

	/**
	 * Processes user input commands.
	 * @throws IOException 
	 */
	private void processCommand() throws IOException {
		System.out.print("\nEnter command: ");
		String input = scanner.nextLine().trim().toLowerCase();

		if (input.isEmpty()) {
			return;
		}

		switch (input) {
		case "quit":
		case "exit":
			System.out.println("Thanks for playing!");
			running = false;
			break;

		case "help":
			printHelp();
			break;

		case "undo":
			if (model.undo()) {
				System.out.println("Move undone.");
			}
			updateView();
			break;
			
		case "save":
			if (model.save()) {
				// Status message is set in model.save()
			}
			updateView();
			break;
			
		case "load":
			if (model.load()) {
				// Status message is set in model.load()
			}
			updateView();
			break;

		case "restart":
			model.restart();
			System.out.println("Game restarted.");
			updateView();
			break;

		default:
			try {
				int column = Integer.parseInt(input);
				if (column <= 0 || column > model.getColumns()) {
					showError("Invalid column number.");
				} else if (model.makeMove(column)) {
					updateView();
				} else {
					showError(model.getErrorMessage());
				}
			} catch (NumberFormatException e) {
				showError("Unknown command. Type 'help' for instructions.");
			}
			break;
		}
	}
}