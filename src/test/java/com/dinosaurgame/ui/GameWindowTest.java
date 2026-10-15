package com.dinosaurgame.ui;

import com.dinosaurgame.game.GameEngine;
import com.dinosaurgame.game.GameState;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "dino.uiTests", matches = "true")
class GameWindowTest {
    private record Window(Stage stage, Scene scene, GameView view, GameEngine game) { }

    @Test void playPauseCrashRestartAndResizeInRealWindow() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX must start.");
        Platform.setImplicitExit(false);
        Window window = null;
        try {
            window = fx(() -> {
                GameEngine game = new GameEngine(new Random(42), 0);
                GameView view = new GameView(game, () -> { });
                Scene scene = new Scene(view, 960, 360);
                Stage stage = new Stage();
                stage.setTitle("Dinosaur Game - UI test");
                stage.setScene(scene);
                view.attachInput(scene);
                stage.show();
                view.start();
                return new Window(stage, scene, view, game);
            });
            Window w = window;
            waitFor(() -> w.stage().isShowing() && w.view().getWidth() > 0);
            snapshot(w, "ready");
            fx(() -> {
                key(w.scene(), KeyCode.SPACE, true);
                key(w.scene(), KeyCode.SPACE, true);
                assertEquals(GameState.RUNNING, w.game().state());
                key(w.scene(), KeyCode.SPACE, false);
                return null;
            });
            waitFor(() -> w.game().score() > 0);
            snapshot(w, "running");
            waitFor(() -> w.game().dinosaur().isGrounded());
            fx(() -> {
                key(w.scene(), KeyCode.DOWN, true);
                assertTrue(w.game().dinosaur().isDucking());
                return null;
            });
            snapshot(w, "ducking");
            fx(() -> {
                key(w.scene(), KeyCode.DOWN, false);
                key(w.scene(), KeyCode.P, true);
                key(w.scene(), KeyCode.P, false);
                assertEquals(GameState.PAUSED, w.game().state());
                return null;
            });
            double pausedDistance = fx(() -> w.game().distance());
            Thread.sleep(150);
            assertEquals(pausedDistance, fx(() -> w.game().distance()), 1e-9);
            snapshot(w, "paused");
            fx(() -> {
                key(w.scene(), KeyCode.SPACE, true);
                key(w.scene(), KeyCode.SPACE, false);
                return null;
            });
            waitFor(() -> w.game().state() == GameState.GAME_OVER);
            snapshot(w, "game-over");
            fx(() -> {
                key(w.scene(), KeyCode.ENTER, true);
                key(w.scene(), KeyCode.ENTER, false);
                assertEquals(GameState.RUNNING, w.game().state());
                assertEquals(0, w.game().score());
                assertTrue(w.game().obstacles().isEmpty());
                w.view().pauseForFocusLoss();
                assertEquals(GameState.PAUSED, w.game().state());
                mouse(w.view(), true);
                mouse(w.view(), false);
                assertEquals(GameState.RUNNING, w.game().state());
                w.view().pauseForFocusLoss();
                w.stage().setWidth(720);
                w.stage().setHeight(330);
                return null;
            });
            waitFor(() -> w.view().getWidth() < 750);
            snapshot(w, "resized");
        } finally {
            if (window != null) {
                Window w = window;
                fx(() -> {
                    w.view().stop();
                    w.stage().close();
                    return null;
                });
            }
            Platform.exit();
        }
    }

    private static void key(Scene scene, KeyCode key, boolean pressed) {
        Event.fireEvent(scene, new KeyEvent(pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                "", "", key, false, false, false, false));
    }

    private static void mouse(GameView view, boolean pressed) {
        Event.fireEvent(view, new MouseEvent(pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                500, 160, 500, 160, MouseButton.PRIMARY, 1,
                false, false, false, false, pressed, false, false, false, false, true, null));
    }

    private static void snapshot(Window window, String name) throws Exception {
        // Give the canvas one pulse to put on its best little face (•ᴗ•)
        Thread.sleep(80);
        fx(() -> {
            WritableImage image = window.view().snapshot(null, null);
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            assertTrue(width >= 640 && height >= 240);
            int[] pixels = new int[width * height];
            image.getPixelReader().getPixels(0, 0, width, height,
                    PixelFormat.getIntArgbInstance(), pixels, 0, width);
            BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            png.setRGB(0, 0, width, height, pixels, 0, width);
            Path directory = Path.of(System.getProperty("dino.screenshots"));
            Files.createDirectories(directory);
            ImageIO.write(png, "png", directory.resolve(name + ".png").toFile());
            return null;
        });
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            if (fx(condition::getAsBoolean)) return;
            Thread.sleep(25);
        }
        fail("The expected window state was not reached.");
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
