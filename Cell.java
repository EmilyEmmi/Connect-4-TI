/**
 * NEW CLASS - Represents a single cell on the Connect Four game board.
 * This class encapsulates the state of a cell including whether it's occupied
 * and which player occupies it.
 */
public class Cell {
    // Enum to represent the state of a cell
    public enum State {
        EMPTY,    // Cell is available for a move
        RED,      // Cell is occupied by a red coin
        YELLOW    // Cell is occupied by a yellow coin
    }
    
    private State state;
    
    /**
     * Creates a new empty cell.
     */
    public Cell() {
        this.state = State.EMPTY;
    }
    
    /**
     * Creates a cell with the specified state.
     * @param state the initial state of the cell
     */
    public Cell(State state) {
        this.state = state;
    }
    
    /**
     * Checks if the cell is available (empty).
     * @return true if the cell is empty, false otherwise
     */
    public boolean isAvailable() {
        return state == State.EMPTY;
    }
    
    /**
     * Checks if the cell is occupied by a red coin.
     * @return true if the cell contains a red coin, false otherwise
     */
    public boolean isRed() {
        return state == State.RED;
    }
    
    /**
     * Checks if the cell is occupied by a yellow coin.
     * @return true if the cell contains a yellow coin, false otherwise
     */
    public boolean isYellow() {
        return state == State.YELLOW;
    }
    
    /**
     * Gets the current state of the cell.
     * @return the current state (EMPTY, RED, or YELLOW)
     */
    public State getState() {
        return state;
    }
    
    /**
     * Sets the state of the cell.
     * @param state the new state for the cell
     */
    public void setState(State state) {
        this.state = state;
    }
    
    /**
     * Clears the cell, making it empty.
     */
    public void clear() {
        this.state = State.EMPTY;
    }
    
    @Override
    public String toString() {
        switch (state) {
            case RED:
                return "R";
            case YELLOW:
                return "Y";
            default:
                return ".";
        }
    }
}
