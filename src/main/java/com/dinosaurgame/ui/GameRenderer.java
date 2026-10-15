package com.dinosaurgame.ui;

import com.dinosaurgame.game.Dinosaur;
import com.dinosaurgame.game.GameEngine;
import com.dinosaurgame.game.GameState;
import com.dinosaurgame.game.Obstacle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

final class GameRenderer {
    private static final Color DAY = Color.web("#fafafa");
    private static final Color NIGHT = Color.web("#202124");
    private Color ink;
    private Color paper;
    private Color muted;

    void draw(GraphicsContext g, GameEngine game, double width, double height) {
        paper = game.isNight() ? NIGHT : DAY;
        ink = game.isNight() ? Color.web("#e8eaed") : Color.web("#535353");
        muted = game.isNight() ? Color.web("#9aa0a6") : Color.web("#737373");
        g.setFill(paper);
        g.fillRect(0, 0, width, height);
        double scale = Math.min(width / GameEngine.WIDTH, height / GameEngine.HEIGHT);
        g.save();
        g.translate((width - GameEngine.WIDTH * scale) / 2, (height - GameEngine.HEIGHT * scale) / 2);
        g.scale(scale, scale);
        g.beginPath();
        g.rect(0, 0, GameEngine.WIDTH, GameEngine.HEIGHT);
        g.clip();

        drawSky(g, game);
        drawGround(g, game.distance());
        for (Obstacle obstacle : game.obstacles()) drawObstacle(g, obstacle, game.elapsed());
        DinoSprite.draw(g, game.dinosaur(), game.state() == GameState.RUNNING,
                game.elapsed(), game.state() == GameState.GAME_OVER, ink, paper);
        drawHud(g, game);
        drawMessage(g, game);
        g.restore();
    }

    private void drawSky(GraphicsContext g, GameEngine game) {
        if (game.isNight()) {
            g.setFill(muted);
            for (int i = 0; i < 9; i++) {
                double x = wrap(i * 137 + 60 - game.distance() * 0.025, 1020) - 30;
                double y = 63 + (i * 29 % 90);
                g.fillRect(x, y, 2, 2);
                if (i % 3 == 0) {
                    g.fillRect(x - 2, y + 1, 6, 1);
                    g.fillRect(x + 1, y - 2, 1, 6);
                }
            }
            g.setFill(ink);
            g.fillOval(750, 75, 27, 27);
            g.setFill(paper);
            g.fillOval(760, 70, 24, 27);
        }
        g.setStroke(muted.deriveColor(0, 1, 1, 0.45));
        g.setLineWidth(1.2);
        for (int i = 0; i < 4; i++) {
            double x = wrap(220 + i * 277 - game.distance() * 0.12, 1100) - 70;
            double y = 90 + i * 23 % 61;
            g.strokePolyline(new double[]{x, x, x+11, x+11, x+20, x+20, x+37, x+37, x+49, x+49, x+62, x+62, x},
                    new double[]{y+19, y+14, y+14, y+7, y+7, y, y, y+8, y+8, y+14, y+14, y+19, y+19}, 13);
        }
    }

    private void drawGround(GraphicsContext g, double distance) {
        g.setFill(ink);
        g.fillRect(24, GameEngine.GROUND_Y, GameEngine.WIDTH - 48, 1);
        g.setGlobalAlpha(0.6);
        for (int i = 0; i < 32; i++) {
            double x = wrap(i * 43 - distance, 1376) - 50;
            double y = GameEngine.GROUND_Y + 5 + (i * 13 % 15);
            if (x > 24 && x < GameEngine.WIDTH - 35) g.fillRect(Math.floor(x), y, 2 + i % 6, 1);
        }
        g.setGlobalAlpha(1);
    }

    private void drawObstacle(GraphicsContext g, Obstacle obstacle, double elapsed) {
        g.setFill(ink);
        if (obstacle.kind().isBird()) {
            g.save();
            g.translate(Math.round(obstacle.x()), obstacle.y());
            g.fillRect(0, 13, 10, 4);
            g.fillRect(7, 9, 11, 10);
            g.fillRect(14, 14, 25, 9);
            g.fillRect(36, 12, 10, 5);
            if ((int) (elapsed * 7) % 2 == 0) {
                g.fillRect(18, 4, 5, 14);
                g.fillRect(23, 0, 5, 17);
                g.fillRect(28, 7, 4, 10);
            } else {
                g.fillRect(18, 19, 6, 8);
                g.fillRect(24, 20, 5, 10);
                g.fillRect(29, 20, 4, 6);
            }
            g.setFill(paper);
            g.fillRect(10, 11, 2, 2);
            g.restore();
        } else if (obstacle.kind() == Obstacle.Kind.CACTUS_GROUP) {
            for (int i = 0; i < 3; i++) {
                double height = i == 1 ? 48 : 38;
                drawCactus(g, obstacle.x() + i * 20, GameEngine.GROUND_Y - height, 18, height);
            }
        } else {
            drawCactus(g, obstacle.x(), obstacle.y(), obstacle.width(), obstacle.height());
        }
    }

    private void drawCactus(GraphicsContext g, double x, double y, double width, double height) {
        g.save();
        g.translate(Math.round(x), y);
        g.setFill(ink);
        g.fillRect(Math.round(width * 0.35), 2, Math.round(width * 0.3), height - 2);
        g.fillRect(Math.round(width * 0.42), 0, Math.round(width * 0.16), 3);
        g.fillRect(1, height * 0.3, width * 0.2, height * 0.28);
        g.fillRect(2, height * 0.51, width * 0.4, height * 0.12);
        g.fillRect(width * 0.79, height * 0.18, width * 0.2, height * 0.34);
        g.fillRect(width * 0.6, height * 0.45, width * 0.36, height * 0.12);
        g.restore();
    }

    private void drawHud(GraphicsContext g, GameEngine game) {
        text(g, "˚｡⋆୨୧˚DINO RUN˚୨୧⋆｡˚", 30, 39, 15, ink, TextAlignment.LEFT, true);
        text(g, "OFFLINE RUNNER", 30, 57, 10, muted, TextAlignment.LEFT, false);
        text(g, String.format("HI %05d", game.bestScore()), 796, 44, 20, muted, TextAlignment.RIGHT, true);
        boolean flash = game.score() > 0 && game.score() % 100 < 5
                && game.state() == GameState.RUNNING && (int) (game.elapsed() * 8) % 2 == 0;
        if (!flash) text(g, String.format("%05d", game.score()), 930, 44, 20, ink, TextAlignment.RIGHT, true);

        text(g, "SPACE / ↑  Jump", 30, 325, 12, muted, TextAlignment.LEFT, false);
        text(g, "↓  Duck", 348, 325, 12, muted, TextAlignment.LEFT, false);
        text(g, "P / ESC  Pause", 536, 325, 12, muted, TextAlignment.LEFT, false);
        text(g, "ENTER  Restart", 930, 325, 12, muted, TextAlignment.RIGHT, false);
    }

    private void drawMessage(GraphicsContext g, GameEngine game) {
        switch (game.state()) {
            case READY -> {
                g.setFill(paper);
                g.fillRect(280, 109, 500, 100);
                text(g, "A little dino, a long way to go...♡", 530, 139, 22, ink, TextAlignment.CENTER, true);
                text(g, "Press Space or click to start", 530, 174, 14, muted, TextAlignment.CENTER, false);
                text(g, "Hold to jump higher", 530, 197, 11, muted, TextAlignment.CENTER, false);
            }
            case PAUSED -> {
                banner(g);
                text(g, "P A U S E", 480, 133, 24, ink, TextAlignment.CENTER, true);
                text(g, "Press Space, P, or click to resume", 480, 172, 13, muted, TextAlignment.CENTER, false);
            }
            case GAME_OVER -> {
                banner(g);
                text(g, "G A M E  O V E R", 480, 120, 23, ink, TextAlignment.CENTER, true);
                g.setFill(ink);
                g.fillRoundRect(464, 141, 32, 29, 4, 4);
                g.setStroke(paper);
                g.setLineWidth(2);
                g.strokePolyline(new double[]{473, 486, 486, 475, 475},
                        new double[]{149, 149, 161, 161, 157}, 5);
                g.setFill(paper);
                g.fillPolygon(new double[]{469, 475, 475}, new double[]{149, 144, 154}, 3);
                text(g, game.isNewBest() ? "New high score!  " + game.score() + " points" : game.score() + " points",
                        480, 193, 13, ink, TextAlignment.CENTER, false);
                text(g, "Press Enter, Space, or click to restart", 480, 217, 12, muted, TextAlignment.CENTER, false);
            }
            default -> { }
        }
    }

    private void banner(GraphicsContext g) {
        g.setFill(paper.deriveColor(0, 1, 1, 0.95));
        g.fillRect(275, 88, 410, 138);
    }

    private void text(GraphicsContext g, String value, double x, double y, int size, Color color,
                      TextAlignment alignment, boolean bold) {
        g.setFill(color);
        g.setTextAlign(alignment);
        g.setFont(Font.font("Consolas", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        g.fillText(value, x, y);
    }

    private double wrap(double value, double period) {
        return (value % period + period) % period;
    }
}
