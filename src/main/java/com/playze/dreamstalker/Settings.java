package com.playze.dreamstalker;

/** Simple constants. Swap for a Forge config spec later if you want in-game tuning. */
public final class Settings {
    private Settings() {}

    // --- Dreams ---
    /** Ticks you must be lying in bed before the dream starts (vanilla wakes you at 100). */
    public static final int SLEEP_TRIGGER_TICKS = 60;
    /** 5 minutes. */
    public static final int DREAM_TICKS = 20 * 60 * 5;
    /** Chance a given sleep becomes a dream. (After a dream, your next sleep is always normal.) */
    public static final float DREAM_CHANCE = 1.0f;
    public static final int MAX_WATCHERS = 3;

    // --- Fake crash / desktop (client) ---
    public static final boolean FAKE_CRASH_ENABLED = true;
    /** Average ticks between fake crashes while dreaming (1 / this chance per tick). */
    public static final int FAKE_CRASH_AVG_TICKS = 20 * 60;
    /** Safety net: the fake desktop closes itself after this many ticks even if nothing is clicked. */
    public static final int DESKTOP_TIMEOUT_TICKS = 20 * 45;
    public static final float JUMPSCARE_VOLUME = 0.8f;

    // --- Faint ambient scare sound (faint.ogg) ---
    public static final boolean AMBIENT_SCARE_ENABLED = true;
    /** Waits a random 4-6 minutes between plays (average 5). */
    public static final int AMBIENT_MIN_TICKS = 20 * 60 * 4;
    public static final int AMBIENT_MAX_TICKS = 20 * 60 * 6;
    /** Very quiet on purpose. */
    public static final float AMBIENT_VOLUME = 0.08f;
    /** Fake desktop shows the NAMES of the items on the player's real Desktop (local only, never sent anywhere). */
    public static final boolean MIRROR_DESKTOP = true;
    public static final int MAX_MIRRORED_ICONS = 30;

    // --- Lore files (client) ---
    public static final boolean LORE_FILES_ENABLED = true;
    /** true = Desktop folder, false = <game dir>/dreamstalker_files */
    public static final boolean WRITE_TO_DESKTOP = true;
    public static final int MAX_LORE_FILES = 20;

    // --- Prank chain (H0ST -> fake James -> fake crash + note) ---
    public static final boolean PRANK_ENABLED = true;
    /** H0ST speaks this many ticks after the world/server starts (5 minutes). */
    public static final int PRANK_HOST_DELAY_TICKS = 20 * 60 * 5;
    /** Minecraft username to crash. Leave "" to crash whoever answers "no / nuh uh" to fake James. */
    public static final String PRANK_TARGET_USERNAME = "";
    public static final String PRANK_FULL_NAME = "James Fullbrook";
    /** Letters, digits, spaces, - and _ only, must end in .txt */
    public static final String PRANK_FILE_NAME = "hello James Fullbrook.txt";

    // --- Simple Voice Chat ---
    public static final boolean VOICE_MIMIC = true;
}
