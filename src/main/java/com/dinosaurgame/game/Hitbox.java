package com.dinosaurgame.game;

public record Hitbox(double x, double y, double width, double height) {
    public boolean intersects(Hitbox other) {
        return x < other.x + other.width && x + width > other.x
                && y < other.y + other.height && y + height > other.y;
    }
}
