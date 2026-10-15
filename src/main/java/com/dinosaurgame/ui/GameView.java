package com.dinosaurgame.ui;

import com.dinosaurgame.game.GameEngine;
import com.dinosaurgame.game.GameState;
import java.util.EnumSet;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

public final class GameView extends Region {
    private final GameEngine game;
    private final Canvas canvas = new Canvas(GameEngine.WIDTH, GameEngine.HEIGHT);
    private final GameRenderer renderer = new GameRenderer();
    private final EnumSet<KeyCode> heldKeys = EnumSet.noneOf(KeyCode.class);
    private final Runnable saveScore;
    private long previousFrame;
    private boolean mouseHeld;
    private Scene inputScene;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (previousFrame != 0) {
                GameState before = game.state();
                game.update(Math.min((now - previousFrame) / 1_000_000_000.0, 0.1));
                if (before == GameState.RUNNING && game.state() == GameState.GAME_OVER) saveScore.run();
            }
            previousFrame = now;
            render();
        }
    };

    public GameView(GameEngine game, Runnable saveScore) {
        this.game = game;
        this.saveScore = saveScore;
        getChildren().add(canvas);
        setPrefSize(GameEngine.WIDTH, GameEngine.HEIGHT);
        setMinSize(640, 240);
        setFocusTraversable(true);
        setAccessibleText("Dinosaur Game. Press Space to jump or the down arrow to duck.");
        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                requestFocus();
                mouseHeld = true;
                game.pressJump();
            }
        });
        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                mouseHeld = false;
                releaseJumpIfNeeded();
            }
        });
    }

    public void attachInput(Scene scene) {
        inputScene = scene;
        scene.setOnKeyPressed(event -> {
            if (!heldKeys.add(event.getCode())) return;
            switch (event.getCode()) {
                case SPACE, UP, W -> game.pressJump();
                case DOWN, S -> game.setDucking(true);
                case P, ESCAPE -> game.togglePause();
                case ENTER, R -> {
                    if (game.state() == GameState.GAME_OVER || game.state() == GameState.READY) {
                        game.restart();
                    } else if (game.state() == GameState.PAUSED) {
                        game.togglePause();
                    }
                }
                default -> { return; }
            }
            event.consume();
        });
        scene.setOnKeyReleased(event -> {
            heldKeys.remove(event.getCode());
            switch (event.getCode()) {
                case SPACE, UP, W -> releaseJumpIfNeeded();
                case DOWN, S -> game.setDucking(heldKeys.contains(KeyCode.DOWN) || heldKeys.contains(KeyCode.S));
                default -> { return; }
            }
            event.consume();
        });
    }

    private void releaseJumpIfNeeded() {
        if (!mouseHeld && !heldKeys.contains(KeyCode.SPACE)
                && !heldKeys.contains(KeyCode.UP) && !heldKeys.contains(KeyCode.W)) game.releaseJump();
    }

    public void pauseForFocusLoss() {
        // We wait patiently while you visit another window (˘ᵕ˘)
        heldKeys.clear();
        mouseHeld = false;
        game.releaseInputs();
        game.pause();
        previousFrame = 0;
        saveScore.run();
    }

    public void start() {
        previousFrame = 0;
        timer.start();
        requestFocus();
    }

    public void stop() {
        timer.stop();
        heldKeys.clear();
        game.releaseInputs();
        if (inputScene != null) {
            inputScene.setOnKeyPressed(null);
            inputScene.setOnKeyReleased(null);
        }
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        render();
    }

    private void render() {
        renderer.draw(canvas.getGraphicsContext2D(), game, canvas.getWidth(), canvas.getHeight());
    }
}
