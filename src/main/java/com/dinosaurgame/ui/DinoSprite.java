package com.dinosaurgame.ui;

import com.dinosaurgame.game.Dinosaur;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

final class DinoSprite {
    // 45 x 43 pixels from the supplied dinosaur template.
    private static final String[] STANDING = {
        ".............................................",
        ".........................##############......",
        ".......................##################....",
        ".......................##################....",
        "......................####################...",
        "......................#####..#############...",
        "......................#####..#############...",
        "......................####################...",
        "......................####################...",
        "......................####################...",
        "......................####################...",
        "......................####################...",
        "......................###################....",
        ".......................##########............",
        ".......................##########............",
        "......................###############........",
        "......................##########.............",
        "......................##########.............",
        "......................##########.............",
        ".....................###########.............",
        "...................#############.............",
        "..................##############.............",
        "#................##############..............",
        "##..............####################.........",
        "###...........######################.........",
        ".###........###################...##.........",
        ".####......####################...##.........",
        ".##############################...##.........",
        "....##########################...............",
        "....##########################...............",
        ".....#########################...............",
        ".......######################................",
        "........#####################................",
        "............##################...............",
        "...............###############...............",
        "................##############...............",
        ".................#####....####...............",
        ".................#####....####...............",
        "................######....#####..............",
        "................###........####..............",
        "................###........####..............",
        "................######.....#######...........",
        "................######.....#######..........."
    };

    private static final String[] DUCKING = createDuck();
    private static final Map<String, WritableImage> IMAGES = new HashMap<>();

    private DinoSprite() { }

    static void draw(GraphicsContext g, Dinosaur dino, boolean running, double elapsed,
                     boolean dead, Color ink, Color paper) {
        boolean ducking = dino.isDucking();
        int frame = running && dino.isGrounded() ? (int) (elapsed * 12) % 2 : -1;
        String key = ducking + ":" + frame + ":" + dead + ":" + ink + ":" + paper;
        WritableImage image = IMAGES.computeIfAbsent(key,
                ignored -> createImage(ducking, frame, dead, ink, paper));
        // Enlarge the whole sprite so no tiny seams sneak between its pixels (•ᴗ•)
        g.save();
        g.setImageSmoothing(false);
        g.drawImage(image, Dinosaur.X, Math.round(dino.y()),
                Math.round(image.getWidth() * Dinosaur.SCALE),
                Math.round(image.getHeight() * Dinosaur.SCALE));
        g.restore();
    }

    private static WritableImage createImage(boolean ducking, int frame, boolean dead, Color ink, Color paper) {
        String[] sprite = ducking ? DUCKING : STANDING;
        WritableImage image = new WritableImage(sprite[0].length(), sprite.length);
        PixelWriter pixels = image.getPixelWriter();
        for (int y = 0; y < sprite.length; y++) {
            for (int x = 0; x < sprite[y].length(); x++) {
                if (frame >= 0 && !ducking && y >= 35) continue;
                if (frame >= 0 && ducking && y >= 23 && ((frame == 0 && x < 30) || (frame == 1 && x > 30))) continue;
                if (sprite[y].charAt(x) == '#') pixels.setColor(x, y, ink);
            }
        }
        if (frame >= 0 && !ducking) {
            if (frame == 0) {
                paint(pixels, ink, 17, 35, 4, 4);
                paint(pixels, ink, 18, 38, 7, 2);
                paint(pixels, ink, 27, 35, 4, 7);
                paint(pixels, ink, 27, 41, 7, 2);
            } else {
                paint(pixels, ink, 17, 35, 4, 7);
                paint(pixels, ink, 16, 41, 6, 2);
                paint(pixels, ink, 27, 35, 4, 4);
                paint(pixels, ink, 28, 38, 6, 2);
            }
        }
        if (dead) {
            int eyeX = ducking ? 44 : 27;
            paint(pixels, paper, eyeX - 1, 4, 4, 4);
            paint(pixels, ink, eyeX - 1, 4, 1, 1);
            paint(pixels, ink, eyeX + 2, 4, 1, 1);
            paint(pixels, ink, eyeX, 5, 2, 2);
            paint(pixels, ink, eyeX - 1, 7, 1, 1);
            paint(pixels, ink, eyeX + 2, 7, 1, 1);
        }
        return image;
    }

    private static void paint(PixelWriter pixels, Color color, int x, int y, int width, int height) {
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) pixels.setColor(px, py, color);
        }
    }

    private static String[] createDuck() {
        // Same curious face, just a little closer to the ground (•ᴗ•)
        char[][] pixels = new char[26][61];
        for (char[] row : pixels) Arrays.fill(row, '.');
        for (int y = 0; y < 16; y++) {
            for (int x = 22; x < 43; x++) pixels[y][x + 17] = STANDING[y].charAt(x);
        }
        fill(pixels, 14, 11, 43, 20);
        fill(pixels, 20, 9, 42, 12);
        fill(pixels, 18, 20, 41, 22);
        for (int x = 0; x < 15; x++) {
            int top = 8 + x / 2;
            fill(pixels, x, top, x + 1, Math.min(21, top + 3 + x / 3));
        }
        fill(pixels, 20, 21, 24, 25);
        fill(pixels, 19, 24, 27, 26);
        fill(pixels, 38, 21, 42, 25);
        fill(pixels, 38, 24, 46, 26);
        String[] rows = new String[pixels.length];
        for (int y = 0; y < rows.length; y++) rows[y] = new String(pixels[y]);
        return rows;
    }

    private static void fill(char[][] pixels, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) pixels[y][x] = '#';
        }
    }
}
