package com.dinosaurgame.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HighScoreStoreTest {
    @TempDir Path directory;

    @Test void missingFileStartsAtZeroAndScoreSurvivesReload() {
        Path file = directory.resolve("nested/high-score.txt");
        HighScoreStore store = new HighScoreStore(file);
        assertEquals(0, store.load());
        store.save(123);
        assertEquals(123, new HighScoreStore(file).load());
    }

    @Test void smallerScoreCannotReplaceBest() {
        HighScoreStore store = new HighScoreStore(directory.resolve("score.txt"));
        store.save(300);
        store.save(12);
        store.save(-10);
        assertEquals(300, store.load());
    }

    @Test void damagedAndNegativeFilesAreHandled() throws Exception {
        Path file = directory.resolve("score.txt");
        HighScoreStore store = new HighScoreStore(file);
        Files.writeString(file, "a very sleepy dinosaur");
        assertEquals(0, store.load());
        store.save(44);
        assertEquals(44, store.load());
        Files.writeString(file, "-50");
        assertEquals(0, store.load());
    }

    @Test void unavailableDirectoryDoesNotCrashGame() throws Exception {
        Path fileAsParent = directory.resolve("not-a-directory");
        Files.writeString(fileAsParent, "blocked");
        HighScoreStore store = new HighScoreStore(fileAsParent.resolve("score.txt"));
        assertDoesNotThrow(() -> store.save(100));
        assertEquals(0, store.load());
    }
}
