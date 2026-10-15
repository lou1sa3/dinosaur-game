package com.dinosaurgame.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class GameEngine {
    public static final double WIDTH = 960;
    public static final double HEIGHT = 360;
    public static final double GROUND_Y = 265;
    public static final double INITIAL_SPEED = 360;
    public static final double MAX_SPEED = 760;
    public static final double STEP = 1.0 / 120.0;

    private final Random random;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Obstacle> obstacleView = Collections.unmodifiableList(obstacles);
    private Dinosaur dinosaur = new Dinosaur();
    private GameState state = GameState.READY;
    private double distance;
    private double elapsed;
    private double spawnDistance = 300;
    private double speed = INITIAL_SPEED;
    private double gameOverTime;
    private int bestScore;
    private boolean newBest;

    public GameEngine(int bestScore) { this(new Random(), bestScore); }

    public GameEngine(Random random, int bestScore) {
        this.random = Objects.requireNonNull(random);
        this.bestScore = Math.max(0, bestScore);
    }

    public void restart() {
        dinosaur = new Dinosaur();
        obstacles.clear();
        distance = 0;
        elapsed = 0;
        spawnDistance = 300;
        speed = INITIAL_SPEED;
        gameOverTime = 0;
        newBest = false;
        state = GameState.RUNNING;
    }

    public void pressJump() {
        if (state == GameState.PAUSED) {
            togglePause();
            return;
        }
        if (state == GameState.READY || (state == GameState.GAME_OVER && gameOverTime >= 0.45)) {
            restart();
        }
        if (state == GameState.RUNNING) dinosaur.jump();
    }

    public void releaseJump() { dinosaur.releaseJump(); }
    public void setDucking(boolean down) { dinosaur.setDucking(down && state == GameState.RUNNING); }

    public void pause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
            releaseInputs();
        }
    }

    public void togglePause() {
        if (state == GameState.PAUSED) state = GameState.RUNNING;
        else pause();
    }

    public void releaseInputs() {
        dinosaur.setDucking(false);
        dinosaur.releaseJump();
    }

    public void update(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Time must be finite and non-negative.");
        }
        if (state == GameState.GAME_OVER) {
            gameOverTime += seconds;
            return;
        }
        // Tiny steps keep paws and cacti honest when a frame arrives late (•̀ᴗ•́)و
        double remaining = Math.min(seconds, 0.25);
        while (remaining > 1e-9 && state == GameState.RUNNING) {
            double step = Math.min(remaining, STEP);
            tick(step);
            remaining -= step;
        }
    }

    private void tick(double seconds) {
        elapsed += seconds;
        speed = Math.min(MAX_SPEED, INITIAL_SPEED + elapsed * 3.5);
        double travel = speed * seconds;
        distance += travel;
        dinosaur.update(seconds);
        for (Obstacle obstacle : obstacles) obstacle.move(travel);
        obstacles.removeIf(obstacle -> obstacle.x() + obstacle.width() < -10);
        spawnDistance -= travel;
        if (spawnDistance <= 0) {
            Obstacle obstacle = new Obstacle(nextObstacle(), WIDTH + 30);
            obstacles.add(obstacle);
            // Leave enough room to land and choose the next jump (˘ᵕ˘)
            spawnDistance += obstacle.width() + speed * (1.25 + random.nextDouble() * 0.65);
        }
        if (score() > bestScore) {
            bestScore = score();
            newBest = true;
        }
        if (obstacles.stream().anyMatch(this::hitsDinosaur)) {
            state = GameState.GAME_OVER;
            releaseInputs();
        }
    }

    private Obstacle.Kind nextObstacle() {
        if (score() >= 220 && random.nextDouble() < 0.28) {
            return switch (random.nextInt(3)) {
                case 0 -> Obstacle.Kind.LOW_BIRD;
                case 1 -> Obstacle.Kind.MIDDLE_BIRD;
                default -> Obstacle.Kind.HIGH_BIRD;
            };
        }
        return switch (random.nextInt(score() >= 80 ? 3 : 2)) {
            case 0 -> Obstacle.Kind.SMALL_CACTUS;
            case 1 -> Obstacle.Kind.LARGE_CACTUS;
            default -> Obstacle.Kind.CACTUS_GROUP;
        };
    }

    private boolean hitsDinosaur(Obstacle obstacle) {
        for (Hitbox dinoBox : dinosaur.hitboxes()) {
            for (Hitbox obstacleBox : obstacle.hitboxes()) {
                if (dinoBox.intersects(obstacleBox)) return true;
            }
        }
        return false;
    }

    public GameState state() { return state; }
    public Dinosaur dinosaur() { return dinosaur; }
    public List<Obstacle> obstacles() { return obstacleView; }
    public int score() { return (int) (distance / 40); }
    public int bestScore() { return bestScore; }
    public boolean isNewBest() { return newBest; }
    public double distance() { return distance; }
    public double elapsed() { return elapsed; }
    public double speed() { return speed; }
    public boolean isNight() { return (score() / 500) % 2 == 1; }
}
