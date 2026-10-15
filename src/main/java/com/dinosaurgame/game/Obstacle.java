package com.dinosaurgame.game;

import java.util.ArrayList;
import java.util.List;

public final class Obstacle {
    public enum Kind {
        SMALL_CACTUS(24, 44, 0), LARGE_CACTUS(30, 60, 0), CACTUS_GROUP(60, 48, 0),
        LOW_BIRD(46, 30, 14), MIDDLE_BIRD(46, 30, 38), HIGH_BIRD(46, 30, 88);

        private final double width;
        private final double height;
        private final double clearance;

        Kind(double width, double height, double clearance) {
            this.width = width;
            this.height = height;
            this.clearance = clearance;
        }

        public boolean isBird() { return clearance > 0; }
    }

    private final Kind kind;
    private double x;

    public Obstacle(Kind kind, double x) {
        this.kind = kind;
        this.x = x;
    }

    void move(double distance) { x -= distance; }
    public Kind kind() { return kind; }
    public double x() { return x; }
    public double y() { return GameEngine.GROUND_Y - kind.height - kind.clearance; }
    public double width() { return kind.width; }
    public double height() { return kind.height; }

    public List<Hitbox> hitboxes() {
        if (kind.isBird()) {
            return List.of(new Hitbox(x + 5, y() + 9, width() - 10, 14));
        }
        List<Hitbox> boxes = new ArrayList<>();
        if (kind == Kind.CACTUS_GROUP) {
            for (int i = 0; i < 3; i++) {
                double height = i == 1 ? 48 : 38;
                addCactusBoxes(boxes, x + i * 20, GameEngine.GROUND_Y - height, 18, height);
            }
        } else {
            addCactusBoxes(boxes, x, y(), width(), height());
        }
        return boxes;
    }

    private static void addCactusBoxes(List<Hitbox> boxes, double x, double y, double width, double height) {
        boxes.add(new Hitbox(x + width * 0.36, y + 2, width * 0.28, height - 2));
        boxes.add(new Hitbox(x + 2, y + height * 0.32, width - 4, height * 0.36));
    }
}
