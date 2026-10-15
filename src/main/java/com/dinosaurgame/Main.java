package com.dinosaurgame;

import com.dinosaurgame.game.GameEngine;
import com.dinosaurgame.storage.HighScoreStore;
import com.dinosaurgame.ui.GameView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private HighScoreStore scoreStore;
    private GameEngine game;
    private GameView view;

    public static void main(String[] args) {
        System.out.println("rawr!");
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        scoreStore = HighScoreStore.forCurrentUser();
        game = new GameEngine(scoreStore.load());
        view = new GameView(game, () -> scoreStore.save(game.bestScore()));
        Scene scene = new Scene(view, GameEngine.WIDTH, GameEngine.HEIGHT);
        view.attachInput(scene);

        // Configure and display and window :))
        stage.setTitle("Dinosaur Game");
        stage.setScene(scene);
        stage.setMinWidth(656);
        stage.setMinHeight(279);
        stage.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) view.pauseForFocusLoss();
        });
        stage.show();
        view.start();
    }

    @Override
    public void stop() {
        // Tuck the best score in before our little runner goes to sleep (－ω－)
        if (view != null) view.stop();
        if (scoreStore != null && game != null) scoreStore.save(game.bestScore());
    }
}
