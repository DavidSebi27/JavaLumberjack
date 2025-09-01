package org.example;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;

public class Game {

    // player X and Y position
    private int xPos = 0;
    private int yPos = 0;

    // 2D array to contain the level data
    private int[][] levelGrid;

    // screen size
    private int screenWidth;
    private int screenHeight;

    // terminal window
    private Terminal terminal;

    public Game(Terminal terminal) {
        try {
            this.terminal = terminal;
            this.screenWidth = terminal.getTerminalSize().getColumns();
            this.screenHeight = terminal.getTerminalSize().getRows();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws IOException {
        // We don't want to see the cursor prompt
        terminal.setCursorVisible(false);
        // Fill the 2D array with random numbers
        generateLevel();
        // Draw the level based on the data in the array
        drawLevel();
        // Draw the character at the starting position
        drawCharacter();

        // Loop endlessly, processing for keystrokes
        while (true)
        {
            // Wait for keystroke
            KeyStroke key = terminal.readInput();

            // If escape is pressed, then exit the game
            if(key.getKeyType() == KeyType.Escape)
                break;

            // For any other key, run the update logic
            Update(key);
        }
    }

    private void generateLevel()
    {
        levelGrid = new int[screenWidth][screenHeight];

        // A map is created by filling the grid with random numbers between 0 and 9
        // We can later use the numbers to draw them as different kinds of landscape elements
        // For now, 1 = tree, so around 10% of the screen will be filled with trees

        for (int x = 0; x < screenWidth; x++)
        {
            for (int y = 0; y < screenHeight; y++)
            {
                levelGrid[x][y] = (int)(Math.random()*10);
            }
        }
    }


    private void drawLevel() throws IOException {
        terminal.clearScreen();

        // Draw the map in the terminal window
        terminal.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        for (int x = 0; x < screenWidth; x++)
        {
            for (int y = 0; y < screenHeight; y++)
            {
                // For every 1 we encounter, we draw a tree
                // This can easily be extended to support more types of terrain
                if (levelGrid[x][y] == 1)
                {
                    terminal.setCursorPosition(x,y);
                    terminal.putString("\uD83C\uDF32");
                }
            }
        }
        terminal.flush();
    }

    private void drawCharacter() throws IOException {
        // Draw the player at the desired position
        terminal.setForegroundColor(TextColor.ANSI.WHITE);
        terminal.setCursorPosition(xPos,yPos);
        terminal.putString("\uD83D\uDC68");
        terminal.flush();
    }

    private void Update(KeyStroke key) throws IOException {
        // First clear the current position of the player on the screen
        terminal.setCursorPosition(xPos, yPos);
        terminal.putString(" ");


        // Allow the player to move using the arrow keys
        switch (key.getKeyType()) {
            case ArrowUp:
                if (yPos - 1 >= 0 &&
                        levelGrid[xPos][yPos - 1] != 1 &&
                        (xPos + 1 >= screenWidth || levelGrid[xPos + 1][yPos - 1] != 1) &&
                        (xPos - 1 < 0 || levelGrid[xPos - 1][yPos - 1] != 1))
                    yPos--;
                break;

            case ArrowRight:
                if (xPos + 2 < screenWidth && levelGrid[xPos + 2][yPos] != 1)
                    xPos++;
                break;

            case ArrowDown:
                if (yPos + 1 < screenHeight &&
                        levelGrid[xPos][yPos + 1] != 1 &&
                        (xPos + 1 >= screenWidth || levelGrid[xPos + 1][yPos + 1] != 1) &&
                        (xPos - 1 < 0 || levelGrid[xPos - 1][yPos + 1] != 1))
                    yPos++;
                break;

            case ArrowLeft:
                if (xPos - 2 >= 0 && levelGrid[xPos - 2][yPos] != 1)
                    xPos--;
                break;
        }
        drawCharacter();
    }
}
