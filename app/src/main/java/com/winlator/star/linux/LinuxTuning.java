package com.winlator.star.linux;

import com.winlator.star.container.Shortcut;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The performance switches a Linux session runs with, as set per entry in the Steam (Linux)
 * settings.
 *
 * <p>The client's interface is not short of CPU or GPU: measured on an Adreno 840, its menus ran at
 * ~14 fps with the core override confirmed applied while a game on the same device ran at 89. The
 * cost is the chain that draws the menu — Chromium to ANGLE to Zink to Turnip — so these make that
 * chain cheaper rather than asking for more hardware.
 *
 * <p>Each switch is one extra on the entry, {@code "1"} or {@code "0"}, empty meaning the default
 * below. Three set an environment variable for the session; {@link #EXTRA_STEAMDECK} is turned by
 * the session script into the client's own command line instead. Whatever ends up in effect is
 * written into the session's {@code device.txt}, so a measurement names what produced it.
 *
 * <p>None of the defaults is device-proven — they are the ranked candidates. Arbitrary variables
 * beyond these belong in the entry's own "Env vars" field, which is applied after these.
 */
public final class LinuxTuning {
    /** Zink's GL front end marshals on the calling thread; this moves that to a second one. */
    public static final String EXTRA_GLTHREAD = "linuxGlThread";
    /** The cheaper Zink descriptor path. */
    public static final String EXTRA_LAZY_DESCRIPTORS = "linuxLazyDescriptors";
    /** Skips GL error bookkeeping in the hot path. */
    public static final String EXTRA_NO_GL_ERROR = "linuxNoGlError";
    /** Runs the client as Deck hardware, which is what puts the Quick Access Menu on screen. */
    public static final String EXTRA_STEAMDECK = "linuxSteamDeckMode";

    /** The Steam client's update channel; unset follows Deck mode. */
    public static final String EXTRA_STEAM_CHANNEL = "linuxSteamChannel";

    /**
     * The client update channels offered, the empty first entry meaning "follow Deck mode".
     * Both carry an ARM64 client: publicbeta is what every session ran on before, and steamdeck_publicbeta is the Deck's beta channel.
     * The session script accepts only these two, so a stale saved value can never leave the client without an update to install.
     */
    public static final String[] STEAM_CHANNELS = {"", "publicbeta", "steamdeck_publicbeta"};

    /** Puts the Games tab's own games in the client's library as non-Steam shortcuts; on unless turned off. */
    public static final String EXTRA_APP_GAMES = "linuxAppGamesInSteam";
    /** Links those games' save folders to their containers' own, so both sides share progress; on unless turned off. */
    public static final String EXTRA_SHARE_SAVES = "linuxAppGamesShareSaves";
    /** Folders whose subfolders are games to add as well, joined with {@link LinuxAppGames#FOLDER_SEPARATOR}. */
    public static final String EXTRA_GAMES_FOLDERS = "linuxGamesFolders";

    /** gamescope's upscaler type; unset leaves gamescope's own default. */
    public static final String EXTRA_SCALER = "linuxScaler";
    /** gamescope's upscaler filter; unset leaves gamescope's own default. */
    public static final String EXTRA_FILTER = "linuxFilter";

    /**
     * The scaler and filter values gamescope accepts, as its --help lists them for this build.
     * The empty first entry means the flag is not passed at all.
     * Nothing outside these lists is ever handed to gamescope: a value it does not know stops the session from starting.
     */
    public static final String[] SCALERS = {"", "auto", "integer", "fit", "fill", "stretch"};
    public static final String[] FILTERS = {"", "linear", "nearest", "pixel", "fsr", "nis", "sgsr"};

    private LinuxTuning() {}

    /**
     * Whether a switch is on when the entry has never been edited.
     *
     * <p>The three environment ones are on so a tester who changes nothing is still testing them.
     * Deck mode is off, and the editor only turns it on through a warning, because on device it
     * breaks games: Steam Input takes the pad ({@code uses xinput : true} in Steam's controller log)
     * and the virtual pad it hands the game never arrives. It also leaves a Steam Client update that
     * cannot install, and the Quick Access Menu's performance overlay needs mangoapp, which the
     * rootfs does not ship. Its scaling is offered on its own instead ({@link #EXTRA_SCALER},
     * {@link #EXTRA_FILTER}); a frame cap is the in-game drawer's FPS limit.
     * Only {@code -steamdeck} is passed and never {@code -steamos3}; the session script says why.
     */
    public static boolean defaultOn(String extra) {
        return !EXTRA_STEAMDECK.equals(extra);
    }

    /** A switch's state for this entry: its own value, or the default when it has none. */
    public static boolean isOn(Shortcut shortcut, String extra) {
        String v = shortcut != null ? shortcut.getExtra(extra, "") : "";
        if (v == null || v.isEmpty()) return defaultOn(extra);
        return "1".equals(v);
    }

    /**
     * The client update channel this entry runs on.
     * An explicit choice wins; otherwise Deck mode takes the Deck's channel and everything else takes publicbeta.
     * Deck mode on publicbeta reinstalled the same client at every start, because the client read "installed version 0" against that manifest and exited 42 to apply it, while steamdeck_publicbeta comes up clean on the second launch. (Seen on device in The412Banner/SteamDeck, 2026-09-23.)
     */
    public static String steamChannel(Shortcut shortcut) {
        String chosen = oneOf(shortcut, EXTRA_STEAM_CHANNEL, STEAM_CHANNELS);
        if (!chosen.isEmpty()) return chosen;
        return isOn(shortcut, EXTRA_STEAMDECK) ? "steamdeck_publicbeta" : "publicbeta";
    }

    /** The saved scaler, or "" when it is unset or not one gamescope accepts. */
    public static String scaler(Shortcut shortcut) {
        return oneOf(shortcut, EXTRA_SCALER, SCALERS);
    }

    /** The saved filter, or "" when it is unset or not one gamescope accepts. */
    public static String filter(Shortcut shortcut) {
        return oneOf(shortcut, EXTRA_FILTER, FILTERS);
    }

    private static String oneOf(Shortcut shortcut, String extra, String[] allowed) {
        String v = shortcut != null ? shortcut.getExtra(extra, "") : "";
        if (v == null) return "";
        for (String a : allowed) if (a.equals(v)) return v;
        return "";
    }

    /** What the switches come to: the environment half, in the order they are shown. */
    public static Map<String, String> environment(Shortcut shortcut) {
        Map<String, String> env = new LinkedHashMap<>();
        if (isOn(shortcut, EXTRA_GLTHREAD)) env.put("mesa_glthread", "true");
        if (isOn(shortcut, EXTRA_LAZY_DESCRIPTORS)) env.put("ZINK_DESCRIPTORS", "lazy");
        if (isOn(shortcut, EXTRA_NO_GL_ERROR)) env.put("MESA_NO_ERROR", "1");
        return env;
    }

    /**
     * Adds the switches to a session's guest environment.
     *
     * <p>Deck mode travels as {@code BL_STEAMDECK} because the session script, not the guest,
     * is what turns it into {@code -steamdeck -steamos3} on the client's command line.
     */
    public static void apply(List<String> guest, Shortcut shortcut) {
        for (Map.Entry<String, String> e : environment(shortcut).entrySet()) {
            guest.add(e.getKey() + "=" + e.getValue());
        }
        guest.add("BL_STEAMDECK=" + (isOn(shortcut, EXTRA_STEAMDECK) ? "1" : "0"));
        guest.add("BL_STEAM_CHANNEL=" + steamChannel(shortcut));
        // The session script checks these against the same lists before gamescope sees them.
        String sc = scaler(shortcut);
        if (!sc.isEmpty()) guest.add("BL_SCALER=" + sc);
        String fi = filter(shortcut);
        if (!fi.isEmpty()) guest.add("BL_FILTER=" + fi);
    }

    /** One line per switch for the session's device report, so a number names its settings. */
    public static String report(Shortcut shortcut) {
        String[][] rows = {
                {"Threaded GL", EXTRA_GLTHREAD},
                {"Lazy descriptors", EXTRA_LAZY_DESCRIPTORS},
                {"Skip GL error checks", EXTRA_NO_GL_ERROR},
                {"Steam Deck mode", EXTRA_STEAMDECK},
                {"App games in Steam", EXTRA_APP_GAMES},
                {"Share saves with app", EXTRA_SHARE_SAVES},
        };
        StringBuilder b = new StringBuilder();
        for (String[] row : rows) {
            boolean on = isOn(shortcut, row[1]);
            String raw = shortcut != null ? shortcut.getExtra(row[1], "") : "";
            boolean set = raw != null && !raw.isEmpty();
            b.append(String.format("%-24s", row[0])).append(on ? "on" : "off")
             .append(set ? "" : " (default)").append('\n');
        }
        String chosen = oneOf(shortcut, EXTRA_STEAM_CHANNEL, STEAM_CHANNELS);
        b.append(String.format("%-24s", "Client update channel")).append(steamChannel(shortcut))
         .append(chosen.isEmpty() ? " (follows Deck mode)" : "").append('\n');
        String sc = scaler(shortcut);
        b.append(String.format("%-24s", "Scaling mode")).append(sc.isEmpty() ? "(gamescope default)" : sc).append('\n');
        String fi = filter(shortcut);
        b.append(String.format("%-24s", "Scaling filter")).append(fi.isEmpty() ? "(gamescope default)" : fi).append('\n');
        return b.toString();
    }
}
