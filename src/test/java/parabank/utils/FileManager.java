package parabank.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/** Saves and cleans the evidence produced by the tests (screenshots). */
public final class FileManager {

    public static final Path EVIDENCE_DIR = Paths.get("target", "evidence");

    private FileManager() {
    }

    /** Saves a screenshot as target/evidence/<name>.png and returns its path. */
    public static Path saveScreenshot(String name, byte[] image) {
        try {
            Files.createDirectories(EVIDENCE_DIR);
            Path file = EVIDENCE_DIR.resolve(safeFileName(name) + ".png");
            Files.write(file, image);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save screenshot " + name, e);
        }
    }

    /** Deletes the evidence folder and everything inside it. */
    public static void cleanEvidence() {
        if (!Files.exists(EVIDENCE_DIR)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(EVIDENCE_DIR)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not clean " + EVIDENCE_DIR, e);
        }
    }

    public static String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
