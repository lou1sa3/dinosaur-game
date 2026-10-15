package com.dinosaurgame.game;

import java.util.List;

public final class Dinosaur {
    public static final double X = 96;
    public static final double SCALE = 1.3;
    public static final double STANDING_HEIGHT = 43 * SCALE;
    public static final double DUCKING_HEIGHT = 26 * SCALE;
    private double feetY = GameEngine.GROUND_Y;
    private double velocityY;
    private boolean downHeld;
    private boolean jumpHeld;

    public void jump() {
        if (isGrounded() && !downHeld) {
            velocityY = -720;
            jumpHeld = true;
        }
    }

    public void releaseJump() { jumpHeld = false; }
    public void setDucking(boolean down) { downHeld = down; }

    void update(double seconds) {
        if (isGrounded() && velocityY >= 0) return;
        // A little tap clears a cactus; holding jump lets our friend fly higher (˶ᵔ ᵕ ᵔ˶)
        if (!jumpHeld && GameEngine.GROUND_Y - feetY >= 45 && velocityY < -350) {
            velocityY = -350;
        }
        if (downHeld) velocityY = Math.max(velocityY, 450);
        velocityY += 2200 * seconds;
        feetY += velocityY * seconds;
        if (feetY >= GameEngine.GROUND_Y) {
            feetY = GameEngine.GROUND_Y;
            velocityY = 0;
        }
    }

    public boolean isGrounded() { return feetY >= GameEngine.GROUND_Y && velocityY >= 0; }
    public boolean isDucking() { return downHeld && isGrounded(); }
    public double feetY() { return feetY; }
    public double height() { return isDucking() ? DUCKING_HEIGHT : STANDING_HEIGHT; }
    public double y() { return feetY - height(); }

    public List<Hitbox> hitboxes() {
        if (isDucking()) {
            return List.of(box(12, 9, 30, 12), box(39, 1, 18, 15), box(19, 21, 24, 5));
        }
        // Kind hitboxes leave the empty pixels alone (｡•̀ᴗ-)✧
        return List.of(box(23, 3, 18, 12), box(17, 17, 14, 18), box(12, 27, 8, 6),
                box(17, 35, 5, 7), box(27, 34, 5, 8));
    }

    private Hitbox box(double x, double y, double width, double height) {
        return new Hitbox(X + x * SCALE, y() + y * SCALE, width * SCALE, height * SCALE);
    }
}
