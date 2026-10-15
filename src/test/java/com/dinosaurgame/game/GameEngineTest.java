package com.dinosaurgame.game;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {
    private GameEngine game() { return new GameEngine(new Random(42), 0); }

    private void advance(GameEngine game, double seconds) {
        for (int i = 0; i < Math.round(seconds * 120); i++) game.update(GameEngine.STEP);
    }

    @Test void waitsForStart() {
        GameEngine game = game();
        advance(game, 5);
        assertEquals(GameState.READY, game.state());
        assertEquals(0, game.score());
        assertTrue(game.obstacles().isEmpty());
    }

    @Test void startsWithJumpAndLandsExactlyOnGround() {
        GameEngine game = game();
        game.pressJump();
        advance(game, 0.2);
        assertEquals(GameState.RUNNING, game.state());
        assertTrue(game.dinosaur().feetY() < GameEngine.GROUND_Y - 60);
        advance(game, 0.8);
        assertTrue(game.dinosaur().isGrounded());
        assertEquals(GameEngine.GROUND_Y, game.dinosaur().feetY());
    }

    @Test void cannotDoubleJump() {
        GameEngine a = game();
        GameEngine b = game();
        a.pressJump();
        b.pressJump();
        advance(a, 0.2);
        advance(b, 0.2);
        a.pressJump();
        advance(a, 0.1);
        advance(b, 0.1);
        assertEquals(b.dinosaur().feetY(), a.dinosaur().feetY(), 1e-9);
    }

    @Test void holdingJumpGoesHigherThanTapping() {
        GameEngine held = game();
        GameEngine tapped = game();
        held.pressJump();
        tapped.pressJump();
        tapped.releaseJump();
        advance(held, 0.3);
        advance(tapped, 0.3);
        assertTrue(held.dinosaur().feetY() < tapped.dinosaur().feetY() - 20);
    }

    @Test void downSpeedsUpLandingThenDucks() {
        GameEngine game = game();
        game.pressJump();
        advance(game, 0.15);
        game.setDucking(true);
        advance(game, 0.2);
        assertTrue(game.dinosaur().isGrounded());
        assertTrue(game.dinosaur().isDucking());
        assertEquals(Dinosaur.DUCKING_HEIGHT, game.dinosaur().height());
        game.setDucking(false);
        assertEquals(Dinosaur.STANDING_HEIGHT, game.dinosaur().height());
    }

    @Test void duckingAvoidsMiddleBirdButNotLowBird() {
        Dinosaur dino = new Dinosaur();
        Obstacle middle = new Obstacle(Obstacle.Kind.MIDDLE_BIRD, Dinosaur.X + 20);
        Obstacle low = new Obstacle(Obstacle.Kind.LOW_BIRD, Dinosaur.X + 20);
        assertTrue(collides(dino, middle));
        dino.setDucking(true);
        assertFalse(collides(dino, middle));
        assertTrue(collides(dino, low));
    }

    private boolean collides(Dinosaur dino, Obstacle obstacle) {
        return dino.hitboxes().stream().anyMatch(a -> obstacle.hitboxes().stream().anyMatch(a::intersects));
    }

    @Test void pauseFreezesWorldAndClearsHeldInputs() {
        GameEngine game = game();
        game.restart();
        advance(game, 1);
        game.setDucking(true);
        game.pause();
        double distance = game.distance();
        double x = game.obstacles().getFirst().x();
        advance(game, 3);
        assertEquals(GameState.PAUSED, game.state());
        assertEquals(distance, game.distance());
        assertEquals(x, game.obstacles().getFirst().x());
        assertFalse(game.dinosaur().isDucking());
        game.pressJump();
        advance(game, 0.1);
        assertEquals(GameState.RUNNING, game.state());
        assertTrue(game.distance() > distance);
    }

    @Test void collisionStopsRunAndRestartPreservesBest() {
        GameEngine game = game();
        game.restart();
        advance(game, 6);
        assertEquals(GameState.GAME_OVER, game.state());
        int score = game.score();
        double distance = game.distance();
        advance(game, 2);
        assertEquals(distance, game.distance());
        assertEquals(score, game.score());
        game.restart();
        assertEquals(GameState.RUNNING, game.state());
        assertEquals(0, game.score());
        assertEquals(score, game.bestScore());
        assertEquals(GameEngine.INITIAL_SPEED, game.speed());
        assertTrue(game.obstacles().isEmpty());
        assertTrue(game.dinosaur().isGrounded());
    }

    @Test void collisionIsNotSkippedDuringSlowFrames() {
        GameEngine game = game();
        game.restart();
        for (int i = 0; i < 24; i++) game.update(0.25);
        assertEquals(GameState.GAME_OVER, game.state());
    }

    @Test void simulationIsConsistentAt30And120FramesPerSecond() {
        GameEngine slow = game();
        GameEngine fast = game();
        slow.pressJump();
        fast.pressJump();
        for (int i = 0; i < 60; i++) slow.update(1.0 / 30);
        advance(fast, 2);
        assertEquals(fast.distance(), slow.distance(), 1e-7);
        assertEquals(fast.dinosaur().feetY(), slow.dinosaur().feetY(), 1e-7);
        assertEquals(fast.obstacles().getFirst().x(), slow.obstacles().getFirst().x(), 1e-7);
    }

    @Test void invalidTimeIsRejectedAndHugeFramesAreBounded() {
        GameEngine game = game();
        game.restart();
        assertThrows(IllegalArgumentException.class, () -> game.update(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> game.update(-1));
        game.update(100);
        assertTrue(game.distance() < 100);
    }

    @Test void obstacleCollectionCannotBeModifiedFromOutside() {
        assertThrows(UnsupportedOperationException.class,
                () -> game().obstacles().add(new Obstacle(Obstacle.Kind.SMALL_CACTUS, 0)));
    }

    @Test void longRunsStayPlayableAndCycleDayAndNight() {
        for (int seed = 0; seed < 5; seed++) {
            GameEngine game = new GameEngine(new Random(seed), 0);
            game.restart();
            boolean sawNight = false;
            boolean sawDayAgain = false;
            boolean sawBird = false;
            for (int frame = 0; frame < 120 * 150; frame++) {
                Obstacle next = game.obstacles().stream()
                        .filter(o -> o.x() + o.width() > Dinosaur.X + 15)
                        .findFirst().orElse(null);
                game.setDucking(next != null && next.kind() == Obstacle.Kind.MIDDLE_BIRD);
                if (next != null && next.kind().isBird()) sawBird = true;
                if (next != null && next.kind() != Obstacle.Kind.MIDDLE_BIRD
                        && next.kind() != Obstacle.Kind.HIGH_BIRD && game.dinosaur().isGrounded()
                        && next.x() - (Dinosaur.X + 40 * Dinosaur.SCALE) < game.speed() * 0.25) {
                    game.pressJump();
                }
                game.update(GameEngine.STEP);
                assertEquals(GameState.RUNNING, game.state(),
                        "seed=" + seed + ", score=" + game.score() + ", next=" + (next == null ? "-" : next.kind()));
                sawNight |= game.isNight();
                sawDayAgain |= sawNight && !game.isNight();
            }
            assertTrue(sawBird);
            assertTrue(sawNight);
            assertTrue(sawDayAgain);
            assertEquals(GameEngine.MAX_SPEED, game.speed());
            assertTrue(game.obstacles().size() < 8);
        }
    }
}
