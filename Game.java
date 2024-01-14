package javaapplication66;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

// Enum to represent the status of the game
enum GameStatus {
    NOT_STARTED, RUNNING, PAUSED, GAME_OVER
}

// Enum to represent the directions the snake can move
enum Direction {
    UP, DOWN, LEFT, RIGHT;

    // Helper methods to check the direction
    public boolean isX() {
        return this == LEFT || this == RIGHT;
    }

    public boolean isY() {
        return this == UP || this == DOWN;
    }
}

// Represents a point in 2D space
class Point {
    private final int x;
    private final int y;

    // Constructor to initialize the point
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Copy constructor to create a new point from an existing one
    public Point(Point p) {
        this.x = p.getX();
        this.y = p.getY();
    }

    // Move the point in a given direction with a specified value
    public Point move(Direction d, int value) {
        switch (d) {
            case UP:
                return new Point(x, y - value);
            case DOWN:
                return new Point(x, y + value);
            case RIGHT:
                return new Point(x + value, y);
            case LEFT:
                return new Point(x - value, y);
            default:
                return this;
        }
    }

    // Getters for x and y coordinates
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // String representation of the point
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    // Check if two points are equal
    public boolean equals(Point p) {
        return this.x == p.getX() && this.y == p.getY();
    }

    // Check if two points intersect within a specified tolerance
    public boolean intersects(Point p) {
        return intersects(p, 10);
    }

    // Check if two points intersect within a specified tolerance
    public boolean intersects(Point p, int tolerance) {
        int diffX = Math.abs(x - p.getX());
        int diffY = Math.abs(y - p.getY());
        return this.equals(p) || (diffX <= tolerance && diffY <= tolerance);
    }
}

// Represents the snake in the game
class Snake {
    private Direction direction;
    private Point head;
    private ArrayList<Point> tail;

    // Constructor to initialize the snake at a given position
    public Snake(Point initialPosition) {
        this.head = new Point(initialPosition);
        this.direction = Direction.RIGHT;
        this.tail = new ArrayList<Point>();
        // Initialize some tail segments (can be adjusted)
        this.tail.add(new Point(0, 0));
        this.tail.add(new Point(0, 0));
        this.tail.add(new Point(0, 0));
    }

    // Move the snake by updating its position and tail
    public void move() {
        ArrayList<Point> newTail = new ArrayList<Point>();
        for (int i = 0, size = tail.size(); i < size; i++) {
            Point previous = i == 0 ? head : tail.get(i - 1);
            newTail.add(new Point(previous.getX(), previous.getY()));
        }
        this.tail = newTail;
        this.head = this.head.move(this.direction, 10);
    }

    // Add a new tail segment to the snake
    public void addTail() {
        this.tail.add(new Point(-10, -10));
    }

    // Change the direction of the snake
    public void turn(Direction d) {
        if (d.isX() && direction.isY() || d.isY() && direction.isX()) {
            direction = d;
        }
    }

    // Getter for the tail segments
    public ArrayList<Point> getTail() {
        return this.tail;
    }

    // Getter for the head position
    public Point getHead() {
        return this.head;
    }
}

// Represents the main game class
class Game extends JPanel {
    private final Object lock = new Object(); // Object for synchronization
    private Snake snake;
    private Point cherry;
    private int points = 0;
    private int best = 0;
    private BufferedImage image;
    private GameStatus status;
    private boolean didLoadCherryImage = true;

    private static Font FONT_M = new Font("MV Boli", Font.PLAIN, 24);
    private static Font FONT_M_ITALIC = new Font("MV Boli", Font.ITALIC, 24);
    private static Font FONT_L = new Font("MV Boli", Font.PLAIN, 84);
    private static Font FONT_XL = new Font("MV Boli", Font.PLAIN, 150);
    private static int WIDTH = 760;
    private static int HEIGHT = 520;
    private static int DELAY;

    private boolean isGameRunning = false;
    private final int snakeSpeed;
    private final int cherrySpawnDelay;
    
    private final Object lock1 = new Object(); // New lock for deadlock simulation
    private final Object lock2 = new Object(); // New lock for deadlock simulation

    private class Thread1 extends Thread {
        @Override
        public void run() {
            synchronized (lock1) {
                System.out.println("Thread1 acquired lock1");
                try {
                    Thread.sleep(100); // Simulate some work
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (lock2) {
                    System.out.println("Thread1 acquired lock2");
                }
            }
        }
    }

    private class Thread2 extends Thread {
        @Override
        public void run() {
            synchronized (lock2) {
                System.out.println("Thread2 acquired lock2");
                try {
                    Thread.sleep(100); // Simulate some work
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (lock1) {
                    System.out.println("Thread2 acquired lock1");
                }
            }
        }
    }
    public void runDeadlockExample() {
        Thread1 thread1 = new Thread1();
        Thread2 thread2 = new Thread2();

        thread1.start();
        thread2.start();
    }


    // Constructor with additional parameters for speed and cherry spawn delay
    public Game(int width, int height, int snakeSpeed, int cherrySpawnDelay) {
        this.snakeSpeed = snakeSpeed;
        this.cherrySpawnDelay = cherrySpawnDelay;

        WIDTH = width;
        HEIGHT = height;
        DELAY = snakeSpeed;

        try {
            image = ImageIO.read(new File("cherry.png"));
        } catch (IOException e) {
            didLoadCherryImage = false;
        }

        addKeyListener(new KeyListener());
        setFocusable(true);
        setBackground(new Color(130, 205, 71));
        setDoubleBuffered(true);

        snake = new Snake(new Point(WIDTH / 2, HEIGHT / 2));
        status = GameStatus.NOT_STARTED;
        repaint();
    }

    // Paints the components of the game
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        render(g);
        Toolkit.getDefaultToolkit().sync();
    }

    // Update the game state (e.g., move snake, check collisions)
    private void update() {
        synchronized (lock) {
            snake.move();

            if (cherry != null && snake.getHead().intersects(cherry, 20)) {
                snake.addTail();
                cherry = null;
                points++;
            }

            if (cherry == null) {
                spawnCherry();
            }

            checkForGameOver();
        }
    }

    // Reset the game state
    private void reset() {
        synchronized (lock) {
            points = 0;
            cherry = null;
            snake = new Snake(new Point(WIDTH / 2, HEIGHT / 2));
            setStatus(GameStatus.RUNNING);
        }
    }

    // Set the game status
    private void setStatus(GameStatus newStatus) {
        synchronized (lock) {
            switch (newStatus) {
                case RUNNING:
                    if (!isGameRunning) {
                        startGameLoop();
                        isGameRunning = true;
                    }
                    break;
                case PAUSED:
                case GAME_OVER:
                    if (isGameRunning) {
                        isGameRunning = false;
                    }
                    break;
            }

            status = newStatus;
        }
    }

    // Start the game loop threads
    private void startGameLoop() {
        GameLoopThread gameLoopThread = new GameLoopThread();
        RenderThread renderThread = new RenderThread();

        gameLoopThread.start();
        renderThread.start();
    }

    // Toggle the game pause state
    private void togglePause() {
        synchronized (lock) {
            setStatus(status == GameStatus.PAUSED ? GameStatus.RUNNING : GameStatus.PAUSED);
        }
    }

    // Check for game over conditions
    private void checkForGameOver() {
        Point head = snake.getHead();
        boolean hitBoundary = head.getX() <= 20 || head.getX() >= WIDTH + 10 || head.getY() <= 40
                || head.getY() >= HEIGHT + 30;

        boolean ateItself = false;

        for (Point t : snake.getTail()) {
            ateItself = ateItself || head.equals(t);
        }

        if (hitBoundary || ateItself) {
            setStatus(GameStatus.GAME_OVER);
        }
    }

    // Draw a centered string on the screen
    public void drawCenteredString(Graphics g, String text, Font font, int y) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = (WIDTH - metrics.stringWidth(text)) / 2;

        g.setFont(font);
        g.drawString(text, x, y);
    }

    // Render the game components
    private void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.setFont(FONT_M);

        if (status == GameStatus.NOT_STARTED) {
            drawCenteredString(g2d, "SNAKE", FONT_XL, 200);
            drawCenteredString(g2d, "GAME", FONT_XL, 300);
            drawCenteredString(g2d, "Press  any  key  to  begin", FONT_M_ITALIC, 330);

            return;
        }

        Point p = snake.getHead();

        g2d.drawString("SCORE: " + String.format("%02d", points), 20, 30);
        // Update and display the best score
        best = Math.max(points, best);
        g2d.drawString("BEST: " + String.format("%02d", best), 630, 30);

        if (cherry != null) {
            if (didLoadCherryImage) {
                g2d.drawImage(image, cherry.getX(), cherry.getY(), 60, 60, null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillOval(cherry.getX(), cherry.getY(), 10, 10);
                g2d.setColor(Color.BLACK);
            }
        }

        if (status == GameStatus.GAME_OVER) {
            drawCenteredString(g2d, "Press  enter  to  start  again", FONT_M_ITALIC, 330);
            drawCenteredString(g2d, "GAME OVER", FONT_L, 300);
        }

        if (status == GameStatus.PAUSED) {
            g2d.drawString("Paused", 600, 14);
        }

        g2d.setColor(new Color(33, 70, 199));
        g2d.fillRect(p.getX(), p.getY(), 10, 10);

        for (int i = 0, size = snake.getTail().size(); i < size; i++) {
            Point t = snake.getTail().get(i);

            g2d.fillRect(t.getX(), t.getY(), 10, 10);
        }

        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(20, 40, WIDTH, HEIGHT);
    }

    // Spawn a new cherry in a random position
    public void spawnCherry() {
        cherry = new Point((new Random()).nextInt(WIDTH - 60) + 20,
                (new Random()).nextInt(HEIGHT - 60) + 40);
    }

    // KeyListener for user input
    private class KeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (status == GameStatus.RUNNING) {
                switch (key) {
                    case KeyEvent.VK_LEFT:
                        snake.turn(Direction.LEFT);
                        break;
                    case KeyEvent.VK_RIGHT:
                        snake.turn(Direction.RIGHT);
                        break;
                    case KeyEvent.VK_UP:
                        snake.turn(Direction.UP);
                        break;
                    case KeyEvent.VK_DOWN:
                        snake.turn(Direction.DOWN);
                        break;
                }
            }

            if (status == GameStatus.NOT_STARTED) {
                synchronized (lock) {
                    setStatus(GameStatus.RUNNING);
                }
            }

            if (status == GameStatus.GAME_OVER && key == KeyEvent.VK_ENTER) {
                synchronized (lock) {
                    reset();
                }
            }

            if (key == KeyEvent.VK_P) {
                synchronized (lock) {
                    togglePause();
                }
            }
        }
    }

    // Thread for the game loop
    private class GameLoopThread extends Thread {
        @Override
        public void run() {
            while (status == GameStatus.RUNNING) {
                update();
                try {
                    Thread.sleep(DELAY);
                } 
                catch (InterruptedException e) {
                    e.printStackTrace();
            }
        }

        // After the game loop exits, set the status to GAME_OVER and repaint one last time
        SwingUtilities.invokeLater(() -> {
            setStatus(GameStatus.GAME_OVER);
            repaint();
        });
    }
}


    // Thread for rendering
    private class RenderThread extends Thread {
        @Override
        public void run() {
            while (status == GameStatus.RUNNING) {
                repaint();
                try {
                    Thread.sleep(16); // Adjust as needed
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

// Represents the main game window
class SnakeGame extends JFrame {
    public SnakeGame() {
        initUI();
    }

    private void initUI() {
        // Dialog for selecting the game difficulty level
        String[] levels = {"Easy", "Moderate", "Hard"};
        String level = (String) JOptionPane.showInputDialog(this, "Select Level", "Level Selection",
                JOptionPane.QUESTION_MESSAGE, null, levels, levels[0]);

        int snakeSpeed;
        int cherrySpawnDelay;

        // Set snake speed and cherry spawn delay based on the selected level
        switch (level) {
            case "Easy":
                snakeSpeed = 50;
                cherrySpawnDelay = 500;
                break;
            case "Moderate":
                snakeSpeed = 35;
                cherrySpawnDelay = 300;
                break;
            case "Hard":
                snakeSpeed = 25;
                cherrySpawnDelay = 200;
                break;
            default:
                snakeSpeed = 50;
                cherrySpawnDelay = 500;
        }

        // Create the game with the specified parameters
        Game game = new Game(760, 520, snakeSpeed, cherrySpawnDelay);
        add(game);

        // Set up the main window properties
        setTitle("Snake Game");
        setSize(800, 610);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Make the game window visible
        game.setVisible(true);
        // Invoke the deadlock example
        game.runDeadlockExample();
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            SnakeGame snakeGame = new SnakeGame();
            snakeGame.setVisible(true);
        });
    }
}
