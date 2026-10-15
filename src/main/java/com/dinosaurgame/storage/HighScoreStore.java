package com.dinosaurgame.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HighScoreStore {
    private final Path file;

    public HighScoreStore(Path file) { this.file = file.toAbsolutePath(); }

    public static HighScoreStore forCurrentUser() {
        return new HighScoreStore(Path.of(System.getProperty("user.home"), ".dinosaur-game", "high-score.txt"));
    }

    public int load() {
        try {
            return Math.max(0, Integer.parseInt(Files.readString(file, StandardCharsets.UTF_8).trim()));
        } catch (IOException | NumberFormatException | SecurityException ignored) {
            // A sleepy score file should never stop a little adventure (˘ᵕ˘)
            return 0;
        }
    }

    public void save(int score) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            int best = Math.max(Math.max(0, score), load());
            temporary = Files.createTempFile(file.getParent(), "high-score-", ".tmp");
            Files.writeString(temporary, Integer.toString(best), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | SecurityException ignored) {
            // This run remembers its best even if the disk is having a nap (－ω－)
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException ignored) {
                    // A tiny temporary crumb can wait for the operating system.
                }
            }
        }
    }
}
