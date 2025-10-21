import java.util.Scanner;

/**
 * NEW CLASS - Text-based console view for the Connect Four game.
 * Allows users to play the game using text commands.
 */
public class TextView implements GameView {
    private GameModel model;
    private Scanner scanner;
    private boolean running;
    
    /**
     * Creates a new text-based view for the game.
     * @param model the game model to display
     */
    public TextView(GameModel model) {
        this.model = model;
        this.scanner = new Scanner(System.in);
        this.running = false;
    }
    
    @Override
    public void display() {
        running = true;
        printWelcome();
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
        printBoard();
        printGameState();
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
     * Prints the welcome message and instructions.
     */
    private void printWelcome() {
        System.out.println("=".repeat(40));
        System.out.println("       WELCOME TO CONNECT FOUR");
        System.out.println("=".repeat(40));
        System.out.println("\nCommands:");
        System.out.println("  1-7      : Place piece in column 1-7");
        System.out.println("  undo     : Undo last move");
        System.out.println("  restart  : Restart game");
        System.out.println("  help     : Show this help");
        System.out.println("  quit     : Exit game");
        System.out.println();
    }
    
    /**
     * Prints the current game board.
     */
    private void printBoard() {
        System.out.println("\n  1   2   3   4   5   6   7");
        System.out.println("+---+---+---+---+---+---+---+");
        
        // Print from top row to bottom
        for (int row = model.getRows() - 1; row >= 0; row--) {
            System.out.print("|");
            for (int col = 0; col < model.getColumns(); col++) {
                Cell cell = model.getCell(col, row);
                String symbol;
                if (cell.isRed()) {
                    symbol = " R ";
                } else if (cell.isYellow()) {
                    symbol = " Y ";
                } else {
                    symbol = "   ";
                }
                System.out.print(symbol + "|");
            }
            System.out.println();
            System.out.println("+---+---+---+---+---+---+---+");
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
     */
    private void processCommand() {
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
                printWelcome();
                break;
                
            case "undo":
                if (model.undo()) {
                    System.out.println("Move undone.");
                }
                updateView();
                break;
                
            case "restart":
                model.restart();
                System.out.println("Game restarted.");
                updateView();
                break;
                
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
                try {
                    int column = Integer.parseInt(input);
                    if (model.makeMove(column)) {
                        updateView();
                    } else {
                        showError(model.getErrorMessage());
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid column number.");
                }
                break;
                
            default:
                showError("Unknown command. Type 'help' for instructions.");
                break;
        }
    }
}