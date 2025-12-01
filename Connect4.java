import java.io.IOException;
import java.util.Scanner;

/**
 * NEW CLASS - Main entry point for the Connect Four game.
 * Allows user to choose between GUI and text-based views.
 * REPLACES: Connect4UI.java main method
 * MODIFIED: Now prompts user for view selection if no command-line argument provided
 */
public class Connect4 {
    public static void main(String[] args) throws IOException {
        // Create the game model
        GameModel model = new GameModel();
        
        // Determine which view to use
        GameView view;

        // model.updateValues(GameModel.Difficulty.INTERMEDIATE); // TESTING
        
        // NEW: If command-line argument provided, use it
        if (args.length > 0 && args[0].equalsIgnoreCase("text")) {
            // Text-based view
            System.out.println("Starting Connect Four in text mode...\n");
            view = new TextView(model);
        } 
        else if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            // GUI view via command line
            view = new GUIView(model);
        }
        // NEW: Otherwise, prompt the user to choose
        else {
            view = promptForViewChoice(model);
        }
        
        // Display the view
        view.display();
    }
    
    /**
     * NEW METHOD - Prompts the user to choose between GUI and Text views.
     * @param model the game model to pass to the chosen view
     * @return the selected GameView implementation
     */
    private static GameView promptForViewChoice(GameModel model) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("========================================");
        System.out.println("       WELCOME TO CONNECT FOUR");
        System.out.println("========================================");
        System.out.println("\nPlease choose a view mode:");
        System.out.println("  1. GUI Mode (Graphical Interface)");
        System.out.println("  2. Text Mode (Console Interface)");
        System.out.println();
        
        while (true) {
            System.out.print("Enter your choice (1 or 2): ");
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("1") || choice.equalsIgnoreCase("gui")) {
                System.out.println("Starting GUI mode...\n");
                scanner.close();
                return new GUIView(model);
            } 
            else if (choice.equals("2") || choice.equalsIgnoreCase("text")) {
                System.out.println("Starting Text mode...\n");
                // Don't close scanner - TextView will use System.in
                return new TextView(model);
            } 
            else {
                System.out.println("Invalid choice. Please enter 1 or 2.\n");
            }
        }
    }
}
