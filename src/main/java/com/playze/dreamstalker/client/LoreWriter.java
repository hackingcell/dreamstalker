package com.playze.dreamstalker.client;

import com.playze.dreamstalker.DreamStalker;
import com.playze.dreamstalker.Settings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Drops one lore .txt per call (in order) onto the Desktop (or the game folder).
 * Safety rules baked in:
 *  - only writes text taken from this mod's own assets/dreamstalker/lore/ folder
 *  - never overwrites an existing file
 *  - file names are validated (no paths)
 *  - capped by Settings.MAX_LORE_FILES
 *  - everything it writes is logged so /dreamstalker cleanup can delete exactly those files
 *
 * assets/dreamstalker/lore/index.txt format, one per line:   resource_name.txt|Name On Desktop.txt
 */
final class LoreWriter {
    private LoreWriter() {}

    private static final Pattern RESOURCE_NAME = Pattern.compile("[a-z0-9_\\-]+\\.txt");
    private static final Pattern DISPLAY_NAME = Pattern.compile("[A-Za-z0-9_\\- ]+\\.txt");

    private static Path progressFile() { return FMLPaths.CONFIGDIR.get().resolve("dreamstalker_progress.txt"); }
    private static Path writtenLog() { return FMLPaths.CONFIGDIR.get().resolve("dreamstalker_written.txt"); }

    private static Path targetDir() {
        if (Settings.WRITE_TO_DESKTOP) {
            Path home = Path.of(System.getProperty("user.home"));
            Path desktop = home.resolve("Desktop");
            return Files.isDirectory(desktop) ? desktop : home;
        }
        return FMLPaths.GAMEDIR.get().resolve("dreamstalker_files");
    }

    static Optional<Path> writeNext() {
        try {
            List<String[]> entries = readIndex();
            int n = Files.exists(progressFile()) ? Integer.parseInt(Files.readString(progressFile()).trim()) : 0;
            if (n >= entries.size() || n >= Settings.MAX_LORE_FILES) return Optional.empty();

            String[] e = entries.get(n);
            String text = readResource("lore/" + e[0]);
            if (text == null) return Optional.empty();

            Path dir = targetDir();
            Files.createDirectories(dir);
            Path out = dir.resolve(e[1]);
            try {
                Files.writeString(out, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException exists) {
                // don't touch the user's file; just move on to the next entry next time
                Files.writeString(progressFile(), String.valueOf(n + 1));
                return Optional.empty();
            }
            Files.writeString(progressFile(), String.valueOf(n + 1));
            Files.writeString(writtenLog(), out.toAbsolutePath() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return Optional.of(out);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /** Writes one fixed text file (used by the prank). Same rules: validated name, never overwrites, logged for cleanup. */
    static Optional<Path> writeText(String fileName, String text) {
        try {
            if (!DISPLAY_NAME.matcher(fileName).matches()) return Optional.empty();
            Path dir = targetDir();
            Files.createDirectories(dir);
            Path out = dir.resolve(fileName);
            Files.writeString(out, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            Files.writeString(writtenLog(), out.toAbsolutePath() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return Optional.of(out);
        } catch (Exception ex) {
            return Optional.empty(); // includes "already exists"
        }
    }

    /** Deletes only the files this mod wrote, and resets progress. */
    static int cleanup() {
        int removed = 0;
        try {
            if (Files.exists(writtenLog())) {
                for (String line : Files.readAllLines(writtenLog())) {
                    if (!line.isBlank() && Files.deleteIfExists(Path.of(line.trim()))) removed++;
                }
                Files.deleteIfExists(writtenLog());
            }
            Files.deleteIfExists(progressFile());
        } catch (IOException ignored) {
        }
        return removed;
    }

    private static List<String[]> readIndex() {
        List<String[]> out = new ArrayList<>();
        String idx = readResource("lore/index.txt");
        if (idx == null) return out;
        for (String line : idx.split("\\R")) {
            String[] parts = line.trim().split("\\|", 2);
            if (parts.length != 2) continue;
            if (RESOURCE_NAME.matcher(parts[0]).matches() && DISPLAY_NAME.matcher(parts[1]).matches()) out.add(parts);
        }
        return out;
    }

    private static String readResource(String path) {
        Identifier id = DreamStalker.id(path);
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
