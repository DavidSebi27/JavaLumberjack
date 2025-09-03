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

    // score
    private int score = 0;

    private int bossX;
    private int bossY;
    private int bossHealth;
    private int bossMaxHealth;
    private boolean bossActive = false;
    private String bossName = "Woody Harrelson";

    // 2D array to contain the level data
    private static final int TILE_EMPTY = 0;
    private static final int TILE_TREE  = 1;
    private int[][] levelGrid;

    // screen size
    private int screenWidth;
    private int screenHeight;

    // terminal window
    private Terminal terminal;

    // Horizontal collision/attack checks use 2 columns (emoji double-width or like 1.5),
    // vertical checks use 1 row. Movement stays at 1 column/row for responsiveness.
    private static final int H_STEP = 1;          // how far player moves horizontally per keypress
    private static final int H_COLL_OFFSET = 2;   // for checking emoji/tree/boss that render 2 columns wide
    private static final int V_COLL_OFFSET = 1;

    private AudioManager audioManager =  new AudioManager();
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

        // Draw score
        drawScore();

        gameLoop();

    }

        private void gameLoop() throws IOException
        {
            // Loop endlessly, processing for keystrokes
            while (true) {
                // Wait for keystroke
                KeyStroke key = terminal.readInput();

                // If escape is pressed, then exit the game
                if (key.getKeyType() == KeyType.Escape)
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
                levelGrid[x][y] = (int)(Math.random()*10) == 1 ?  TILE_TREE : TILE_EMPTY;
            }
        }
    }

    private void clearLevelToEmpty()
    {
        for (int x = 0; x < screenWidth; x++)
        {
            for (int y = 0; y < screenHeight; y++)
            {
                levelGrid[x][y] = TILE_EMPTY;
            }
        }
    }

    private boolean inBounds(int x, int y)
    {
        return x >= 0 && y >= 0 && x < screenWidth && y < screenHeight;
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
                if (levelGrid[x][y] == TILE_TREE)
                {
                    terminal.setCursorPosition(x,y);
                    terminal.putString("\uD83C\uDF32"); //🌲
                }
            }
        }

        if (bossActive)
        {
            drawBoss();
            drawBossUI();
        }
        terminal.flush();
    }

    private void drawCharacter() throws IOException {
        // Draw the player at the desired position

            terminal.setForegroundColor(score >= 30 ? TextColor.ANSI.CYAN_BRIGHT : TextColor.ANSI.WHITE);
        terminal.setCursorPosition(xPos,yPos);
        terminal.putString("\uD83D\uDC68"); //👨
        terminal.flush();
    }

    private void drawScore() throws IOException
    {
        String text = "Total Score: " + score;
        terminal.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
        terminal.setCursorPosition(screenWidth - 16,0);
        terminal.putString(text);
        terminal.flush();
    }

    private void drawBoss() throws IOException
    {
        if (!bossActive) return;
        if (inBounds(bossX, bossY))
        {
            terminal.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
            terminal.setCursorPosition(bossX,bossY);
            terminal.putString("\uD83C\uDF33"); // 🌳
        }
    }

    private void drawBossUI() throws IOException
    {
        if  (!bossActive) return;

        int nameRow = screenHeight - 2;
        int barRow  = screenHeight - 1;

        String name = bossName;
        int nameX = Math.max(0, (screenWidth - name.length()) / 2);
        terminal.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
        terminal.setCursorPosition(nameX, nameRow);
        terminal.putString(name);

        // very math
        int barWidth = Math.max(10, screenWidth - 4); // bar is either screen width - 4 or 10 based on which is bigger
        int filled = Math.max(0, Math.min(barWidth, (int) Math.round((bossHealth / (double) bossMaxHealth) * barWidth))); //health percentage - barWidth scales with this
        // then rounds to the nearest whole number
        // math min ensures that this never exceeds the bar size (also that its never negative) --> filled = number of # to show

        StringBuilder bar = new StringBuilder(barWidth);
        for (int i = 0; i < filled; i++) bar.append('#'); // fills # for current health
        for (int i = filled; i < barWidth; i++) bar.append('-'); // fills rest with -

        terminal.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
        terminal.setCursorPosition(2, barRow);
        terminal.putString(bar.toString());

        terminal.flush();
    }

    private void drawVictory() throws IOException
    {
        terminal.clearScreen();

        String msg = "Forest Unforested!";
        int x = Math.max(0, (screenWidth - msg.length()) / 2);
        int y = screenHeight / 2;

        terminal.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        terminal.setCursorPosition(x, y);
        terminal.putString(msg);
        terminal.flush();

        terminal.bell();
        try { Thread.sleep(100000); } catch (InterruptedException ignored) {}
    }

    private void spawnBoss() throws IOException
    {

        audioManager.startBackgroundMusic("C:/Users/david/IdeaProjects/JavaLumberjack/sounds/boss_music.wav");
        clearLevelToEmpty();

        bossActive = true;
        bossMaxHealth = 10;
        bossHealth = bossMaxHealth;

        bossX = screenWidth / 2;
        bossY = screenHeight / 2;

        if (bossX == xPos && bossY == yPos) {
            if (inBounds(bossX + H_STEP, bossY)) bossX += H_STEP;
        }

        drawLevel();
        drawCharacter();
        drawScore();
    }

    private boolean isBossAt(int x, int y) {
        return bossActive && bossX == x && bossY == y;
    }

    private void Update(KeyStroke key) throws IOException {
        // First clear the current position of the player on the screen
        terminal.setCursorPosition(xPos, yPos);
        terminal.putString(" ");


        // Allow the player to move using the arrow keys
        switch (key.getKeyType()) {
            case ArrowUp:
                if (yPos - 1 >= 0 &&
                        !isBossAt(xPos, yPos - 1) &&
                        !isBossAt(xPos + 1, yPos - 1) &&
                        !isBossAt(xPos - 1, yPos - 1) &&
                        levelGrid[xPos][yPos - 1] != 1 &&
                        (xPos + 1 >= screenWidth || levelGrid[xPos + 1][yPos - 1] != 1) &&
                        (xPos - 1 < 0 || levelGrid[xPos - 1][yPos - 1] != 1) && levelGrid[xPos][yPos - 1] != 10 &&
                        (xPos + 1 >= screenWidth || levelGrid[xPos + 1][yPos - 1] != 10) &&
                        (xPos - 1 < 0 || levelGrid[xPos - 1][yPos - 1] != 10))
                    yPos--;
                break;

            case ArrowRight:
                if (xPos + 1 < screenWidth && !isBossAt(xPos + 2, yPos) &&levelGrid[xPos + 2][yPos] != 1 && levelGrid[xPos + 2][yPos] != 10)
                    xPos++;
                break;

            case ArrowDown:
                if (yPos + 1 < screenHeight &&
                        !isBossAt(xPos, yPos + 1) &&
                        !isBossAt(xPos + 1, yPos + 1) &&
                        !isBossAt(xPos - 1, yPos + 1) &&
                        levelGrid[xPos][yPos + 1] != 1 &&
                        (xPos + 1 >= screenWidth || levelGrid[xPos + 1][yPos + 1] != 1) &&
                        (xPos - 1 < 0 || levelGrid[xPos - 1][yPos + 1] != 1) && levelGrid[xPos][yPos + 1] != 10 &&
                        (xPos + 1 >= screenWidth || levelGrid[xPos + 1][yPos + 1] != 10) &&
                        (xPos - 1 < 0 || levelGrid[xPos - 1][yPos + 1] != 10))
                    yPos++;
                break;

            case ArrowLeft:
                if (xPos - 2 >= 0 && !isBossAt(xPos - 2, yPos) &&levelGrid[xPos - 2][yPos] != 1 && levelGrid[xPos - 2][yPos] != 10)
                    xPos--;
                break;
        }
        drawCharacter();

        // 'e' = interact/attack
        if (key.getKeyType() == KeyType.Character && key.getCharacter() == 'e') {
            if (bossActive) {
                boolean hit = tryAttackBoss();
                if (hit) {
                    terminal.bell();
                    if (bossHealth <= 0)
                    {
                        audioManager.stopBackgroundMusic();
                        bossActive = false;
                        drawVictory();
                        // Exit the game after victory
                        System.exit(0);
                    } else {
                        // refresh boss glyph & UI
                        drawBoss();
                        drawBossUI();
                    }
                }
            } else {
                // chopping trees (left/right 2 cols away, to match emoji width)
                boolean chopped = false;

                int leftX  = xPos - H_COLL_OFFSET;
                int rightX = xPos + H_COLL_OFFSET;

                if (inBounds(leftX, yPos) && levelGrid[leftX][yPos] == TILE_TREE)
                {
                    levelGrid[leftX][yPos] = TILE_EMPTY;
                    chopped = true;
                } else if (inBounds(rightX, yPos) && levelGrid[rightX][yPos] == TILE_TREE)
                {
                    levelGrid[rightX][yPos] = TILE_EMPTY;
                    chopped = true;
                } else
                {
                    // also try up/down by 1 row (so you can chop vertically if you want)
                    int upY = yPos - V_COLL_OFFSET;
                    int dnY = yPos + V_COLL_OFFSET;
                    if (inBounds(xPos, upY) && levelGrid[xPos][upY] == TILE_TREE)
                    {
                        levelGrid[xPos][upY] = TILE_EMPTY;
                        chopped = true;
                    } else if (inBounds(xPos, dnY) && levelGrid[xPos][dnY] == TILE_TREE)
                    {
                        levelGrid[xPos][dnY] = TILE_EMPTY;
                        chopped = true;
                    }
                }

                if (chopped)
                {
                    terminal.bell();
                    score++;
                    drawLevel();
                    drawCharacter();
                    drawScore();
                    maybePlayMilestoneJingle();
                }

                // spawn boss at 100 score
                if (score >= 10 && !bossActive)
                {
                    spawnBoss();
                }
            }
        }
    }

    // Player attacks boss if exactly adjacent in 4 dirs
    private boolean tryAttackBoss() throws IOException
    {
        if (!bossActive) return false;

        int leftX  = xPos - H_COLL_OFFSET;
        int rightX = xPos + H_COLL_OFFSET;
        int upY    = yPos - V_COLL_OFFSET;
        int downY  = yPos + V_COLL_OFFSET;

        boolean adjacent =
                (bossY == yPos && bossX == leftX)  ||
                        (bossY == yPos && bossX == rightX) ||
                        (bossX == xPos && bossY == upY)    ||
                        (bossX == xPos && bossY == downY);

        if (adjacent) {
            bossHealth = Math.max(0, bossHealth - 1);
            return true;
        }
        return false;
    }

    private void maybePlayMilestoneJingle() throws IOException
    {
        if (score > 0 && score % 10 == 0) {
            terminal.bell();
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            terminal.bell();
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            terminal.bell();
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            terminal.bell();
        }
    }
}
