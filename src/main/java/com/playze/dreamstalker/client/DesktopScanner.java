package com.playze.dreamstalker.client;

import com.playze.dreamstalker.Settings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Builds the icon list for the fake desktop from the names of the items on the player's own Desktop.
 *
 * Privacy rules (keep them if you edit this):
 *  - only file NAMES are read, never file contents
 *  - the result lives in memory for the lifetime of the fake-desktop screen; it is never written
 *    to disk, logged, or sent over the network (this class is client-only)
 *  - can be switched off with Settings.MIRROR_DESKTOP, which falls back to made-up icons
 */
final class DesktopScanner {
    private DesktopScanner() {}

    record Entry(String label, String tag, int color) {}

    private static final int COLOR_FOLDER = 0xFFD9A441;
    private static final int COLOR_APP = 0xFF4A90D9;
    private static final int COLOR_DOC = 0xFFE8E8E8;
    private static final int COLOR_IMAGE = 0xFF3E8E41;
    private static final int COLOR_MEDIA = 0xFF8E44AD;
    private static final int COLOR_OTHER = 0xFF7A7A7A;

    static List<Entry> scan() {
        List<Entry> out = new ArrayList<>();
        if (Settings.MIRROR_DESKTOP) {
            Set<String> seen = new HashSet<>();
            for (Path root : roots()) {
                try (Stream<Path> files = Files.list(root)) {
                    files.limit(300)
                            .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                            .forEach(p -> {
                                try {
                                    String name = p.getFileName().toString();
                                    String lower = name.toLowerCase(Locale.ROOT);
                                    if (name.startsWith(".") || lower.equals("desktop.ini") || lower.equals("thumbs.db")) return;
                                    boolean dir = Files.isDirectory(p);
                                    Entry e = toEntry(name, dir);
                                    if (seen.add(e.label().toLowerCase(Locale.ROOT)) && out.size() < Settings.MAX_MIRRORED_ICONS) {
                                        out.add(e);
                                    }
                                } catch (RuntimeException ignored) {
                                }
                            });
                } catch (Exception ignored) {
                    // missing folder / no permission: just skip this root
                }
            }
        }
        if (out.isEmpty()) { // scanning off or nothing found: made-up icons
            out.add(new Entry("This PC", "PC", COLOR_APP));
            out.add(new Entry("Recycle Bin", "BIN", COLOR_OTHER));
            out.add(new Entry("minecraft", "EXE", COLOR_APP));
            out.add(new Entry("photos", "IMG", COLOR_IMAGE));
        }
        out.add(new Entry("DO_NOT_OPEN", "!", 0xFFB02020)); // always one thing that shouldn't be there
        return out;
    }

    private static List<Path> roots() {
        List<Path> roots = new ArrayList<>();
        Path home = Path.of(System.getProperty("user.home", "."));
        roots.add(home.resolve("Desktop"));
        roots.add(home.resolve("OneDrive").resolve("Desktop"));
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) roots.add(Path.of("C:\\Users\\Public\\Desktop"));
        return roots;
    }

    private static Entry toEntry(String fileName, boolean dir) {
        if (dir) return new Entry(fileName, "DIR", COLOR_FOLDER);
        int dot = fileName.lastIndexOf('.');
        String ext = dot > 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        String label = dot > 0 ? fileName.substring(0, dot) : fileName;
        return switch (ext) {
            case "lnk", "url", "exe", "app", "desktop", "appref-ms", "msi" -> new Entry(label, "APP", COLOR_APP); // shortcuts show without extension, like a real desktop
            case "txt", "md", "doc", "docx", "pdf", "rtf", "odt", "xls", "xlsx", "ppt", "pptx" ->
                    new Entry(fileName, ext.toUpperCase(Locale.ROOT), COLOR_DOC);
            case "png", "jpg", "jpeg", "gif", "bmp", "webp" -> new Entry(fileName, "IMG", COLOR_IMAGE);
            case "mp3", "wav", "ogg", "mp4", "mkv", "mov", "avi" -> new Entry(fileName, "MED", COLOR_MEDIA);
            default -> new Entry(fileName, ext.isEmpty() ? "?" : ext.toUpperCase(Locale.ROOT), COLOR_OTHER);
        };
    }
}
