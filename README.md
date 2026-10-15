# dinosaur-game

A Java recreation of Google's classic Dinosaur Game, built with JavaFX.

A little endless runner you can play locally, featuring your dinosaur pixel art,
animated legs, cacti, pterodactyls, a day/night cycle, and a saved high score.

## Running on Windows

Requires a **JDK with Java 25 or later**. The launcher also detects the JDK bundled
with the **Language Support for Java by Red Hat** extension for VS Code.

From the project directory:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 play
```

On the first launch, the script downloads Maven, verifies its SHA-512 checksum,
and downloads the JavaFX dependencies into `.tools/`. It does not change any
global Java or Maven installation. UNC network paths are supported.

After the first successful launch, you can play without an internet connection:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 play -Offline
```

If you already have a JDK and Maven configured, you can also use `mvn javafx:run`.

## Controls

| Key | Action |
| --- | --- |
| Space, ↑, or W | Start / jump; hold for a higher jump |
| ↓ or S | Duck; fall faster while in the air |
| P or Esc | Pause / resume |
| Enter or R | Restart after Game Over; resume when paused |
| Left mouse button | Start, jump, resume, or restart after Game Over |

The game pauses automatically when you switch to another window. Press Space,
P, or click to resume. Pterodactyls appear from 220 points onward, and the time
of day changes every 500 points. Your high score is saved in
`%USERPROFILE%/.dinosaur-game/high-score.txt`.

## Testing and building

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 test
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 ui-test
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 verify
```

The standard tests check movement, collisions, speed, pausing, restarting,
obstacle sequences, the day/night cycle, and high-score storage without opening
a game window. The additional UI test opens a real window, checks keyboard and
mouse input and resizing, and saves screenshots in `target/ui-smoke/`.
It requires a graphical desktop session. With Maven installed, use
`mvn test`, `mvn -Ddino.uiTests=true test`, or `mvn verify`.

## Project structure

```text
src/main/java/com/dinosaurgame/
  Main.java                  Application and window lifecycle
  game/                      Game logic without JavaFX dependencies
    GameEngine.java          Game states, speed, score, and obstacles
    Dinosaur.java            Jumping, ducking, and hitboxes
    Obstacle.java            Cacti and pterodactyls
    Hitbox.java              Collision detection
    GameState.java           Ready, running, paused, and game over
  ui/
    GameView.java            Game loop, input, and resizing
    GameRenderer.java        Scenery, obstacles, and on-screen text
    DinoSprite.java          Original dinosaur template and animations
  storage/HighScoreStore.java Saving and loading the high score
src/test/java/               Game logic, storage, and window tests
docs/                       Requirements and game design
run.ps1                     Portable Windows launcher for playing and testing
pom.xml                     Java, JavaFX, and test configuration
```

`.tools/` and `target/` contain generated files and are ignored by Git.
