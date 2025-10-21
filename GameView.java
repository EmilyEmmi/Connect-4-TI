/**
 * NEW INTERFACE - Defines the contract for different game views.
 * This allows for multiple implementations (GUI, text-based, etc.)
 * following the Strategy pattern.
 */
public interface GameView {
    /**
     * Updates the view to reflect the current game state.
     */
    void updateView();
    
    /**
     * Displays an error message to the user.
     * @param message the error message to display
     */
    void showError(String message);
    
    /**
     * Displays a game over message with the result.
     * @param message the game result message
     */
    void showGameOver(String message);
    
    /**
     * Initializes and displays the view.
     */
    void display();
}
