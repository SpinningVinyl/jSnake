package com.pavelurusov.jsnake;

import com.pavelurusov.squaregrid.SquareGrid;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class SnakeGame extends Application {

    private SquareGrid display;
    private ArrayList<Cell> snake;
    private Cell fruit;
    private final Color snakeColor = Color.CADETBLUE;
    private final Color fruitColor = Color.TOMATO;
    private int direction;
    private final StringProperty statusString = new SimpleStringProperty();
    private final StringProperty gameOverString = new SimpleStringProperty();

    private final int rows = 20;
    private final int columns = 20;
    private int scoreCounter = 0;
    private Button newGameButton;

    AnimationTimer gameTimer;
    private final double interval = 1e8;

    @Override
    public void start(Stage stage) throws Exception{

        // create the main display

        display = new SquareGrid(20, 20, 20);
        display.setDefaultColor(Color.BLACK);
        display.setAlwaysDrawGrid(false);

        // set up the game loop
        gameTimer = new AnimationTimer() {
            long lastFrameTime;
            @Override
            public void handle(long time) {
                if (time - lastFrameTime >= interval) {
                    tick();
                    lastFrameTime = time;
                }
            }
        };

        // create the UI
        Label statLabel = new Label("");
        statLabel.textProperty().bind(statusString);
        Label gameOverLabel = new Label("");
        gameOverLabel.textProperty().bind(gameOverString);
        newGameButton = new Button("New game");
        newGameButton.setDisable(true);
        newGameButton.setOnAction(e -> newGame());
        HBox bottom = new HBox(25, gameOverLabel, statLabel, newGameButton);
        bottom.setStyle("-fx-padding: 10px");
        bottom.setAlignment(Pos.BASELINE_CENTER);
        BorderPane root = new BorderPane();
        root.setCenter(display);
        root.setBottom(bottom);

        // set up the scene graph
        stage.setTitle("jSnake");
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> keyPressed(e));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // off we go
        newGame();
    }

    private void tick() {
        updateStats();
        move();
        refresh();
    }

    // stop the timer, etc.
    private void gameOver() {
        gameTimer.stop();
        gameOverString.setValue("GAME OVER");
        newGameButton.setDisable(false);
    }

    // resetting the game variables
    private void newGame() {
        createSnake(5,5);
        createFruit();
        direction = 0;
        scoreCounter = 0;
        gameOverString.setValue("");
        gameTimer.start();
        newGameButton.setDisable(true);
    }

    // refresh the screen

    private void refresh() {
        display.clearGrid();
        drawSnake();
        drawFruit();
        display.redraw();
    }

    private void keyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        if (code == KeyCode.LEFT || code == KeyCode.KP_LEFT) {
            left();
        } else if (code == KeyCode.RIGHT || code == KeyCode.KP_RIGHT) {
            right();
        } else if (code == KeyCode.UP || code == KeyCode.KP_UP) {
            up();
        } else if (code == KeyCode.DOWN || code == KeyCode.KP_DOWN) {
            down();
        }
    }

    private void drawFruit() {
        display.setCellColor(fruit.getY(), fruit.getX(), fruitColor);
    }

    public void drawSnake() {
        for (Cell segment : snake) {
            display.setCellColor(segment.getY(), segment.getX(), snakeColor);
        }
    }

    private void updateStats() {
        statusString.setValue("Length: " + snake.size() + ", score: " + scoreCounter);
    }

    private void createFruit() {
        boolean success = false;
        int x = 0, y = 0;
        while (!success) {
            x = ThreadLocalRandom.current().nextInt(columns);
            y = ThreadLocalRandom.current().nextInt(rows);
            if ((x == 0 && y == 0) || // don't spawn the fruit in the corners
                    (x == columns - 1 && y == rows - 1) ||
                    (x == 0 && y == rows - 1) ||
                    (x == columns - 1 && y == 0)) {
                continue;
            } // don't spawn the fruit inside the snake
            if (withinSnake(x, y)) {
                continue;
            }
            success = true;
        }
        fruit = new Cell(x, y);
    }

    private void createSnake(int x, int y) {
        snake = new ArrayList<>();
        snake.add(new Cell(x, y));
        snake.add(new Cell(x - 1, y));
        snake.add(new Cell(x - 2, y));
    }

    private void right() {
        if (direction != 2) { // can't turn 180 degrees
            direction = 0;
            move();
        }
    }

    public void down() {
        if (direction != 3) { // can't turn 180 degrees
            direction = 1;
            move();
        }
    }

    public void left() {
        if (direction != 0) { // can't turn 180 degrees
            direction = 2;
            move();
        }
    }

    public void up() {
        if (direction != 1) { // can't turn 180 degrees
            direction = 3;
            move();
        }
    }

    public void move() {
        int nextX = snake.get(0).getX();
        int nextY = snake.get(0).getY();
        // update the coordinates of the head according to the current direction
        switch(direction) {
            case 0:
                nextX++; // move right
                break;
            case 1:
                nextY++; // move down
                break;
            case 2:
                nextX--; // move left
                break;
            case 3:
                nextY--; // move up
                break;
        }
        if (nextX < 0 || nextY < 0 || nextX == columns || nextY == rows) {
            // if the head collides with a wall, game over!
            gameOver();
        }
        if (nextX == fruit.getX() && nextY == fruit.getY()) {
            // if the head collides with the fruit
            grow(); // grow the snake
            scoreCounter = scoreCounter + 100; // increment score counter
            createFruit(); // spawn a new fruit
        }
        collisionWithItself(nextX, nextY); // check if the snake collides with itself
        snake.add(0, new Cell(nextX, nextY)); // insert a new head at index 0
        snake.remove(snake.size() - 1); // chop off the last segment
        refresh();
    }

    public void grow() {
        int ultimateX = snake.get(snake.size() - 1).getX();
        int ultimateY = snake.get(snake.size() - 1).getY();
        if (snake.size() > 1) {
            int penultimateX = snake.get(snake.size() - 2).getX();
            int penultimateY = snake.get(snake.size() - 2).getY();
            int dx = ultimateX - penultimateX;
            int dy = ultimateY - penultimateY;
            snake.add(new Cell(ultimateX + dx, ultimateY + dy));
        } else { // this should never happen but still...
            switch (direction) {
                case 0:
                    snake.add(new Cell(ultimateX - 1, ultimateY));
                    break;
                case 1:
                    snake.add(new Cell(ultimateX, ultimateY - 1));
                    break;
                case 2:
                    snake.add(new Cell(ultimateX + 1, ultimateY));
                    break;
                case 3:
                    snake.add(new Cell(ultimateX, ultimateY + 1));
            }
        }
    }

    public boolean withinSnake(int x, int y) { // checks if given coordinates are inside the snake
        return snake.contains(new Cell(x, y));
    }

    // if the snake collides with itself, chop off its tail
    public void collisionWithItself(int x, int y) {
        if(x < 0 | y < 0) {
            return;
        }
        int collision = -1;
        collision = snake.indexOf(new Cell(x, y));
        if (collision != -1) {
            snake.subList(collision, snake.size()).clear();
            scoreCounter = Math.max(scoreCounter - 1000, 0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
