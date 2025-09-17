package com.rasel.server.logging;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * AI
 * Lightweight logging utility with common log levels and simple formatting.
 *
 * Usage: Log.info("Starting server on port %d", port); Log.error("Auth failed
 * for user %s", username, ex);
 *
 * Features: - Levels: TRACE, DEBUG, INFO, WARN, ERROR - Configurable minimum
 * level via setLevel or LOG_LEVEL env (ERROR|WARN|INFO|DEBUG|TRACE) -
 * Timestamp, thread, caller class, and colored output (disabled if NO_COLOR env
 * is set)
 */
public final class Log {

    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    private static volatile Level minLevel = resolveInitialLevel();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
    private static final boolean USE_COLOR = System.getenv("NO_COLOR") == null;

    private Log() {
    }

    public static void setLevel(Level level) {
        if (level != null) {
            minLevel = level;
        }
    }

    public static Level getLevel() {
        return minLevel;
    }

    public static void trace(String msg, Object... args) {
        log(Level.TRACE, null, msg, args);
    }

    public static void debug(String msg, Object... args) {
        log(Level.DEBUG, null, msg, args);
    }

    public static void info(String msg, Object... args) {
        log(Level.INFO, null, msg, args);
    }

    public static void warn(String msg, Object... args) {
        log(Level.WARN, null, msg, args);
    }

    public static void error(String msg, Object... args) {
        log(Level.ERROR, null, msg, args);
    }

    public static void trace(String msg, Throwable t, Object... args) {
        log(Level.TRACE, t, msg, args);
    }

    public static void debug(String msg, Throwable t, Object... args) {
        log(Level.DEBUG, t, msg, args);
    }

    public static void info(String msg, Throwable t, Object... args) {
        log(Level.INFO, t, msg, args);
    }

    public static void warn(String msg, Throwable t, Object... args) {
        log(Level.WARN, t, msg, args);
    }

    public static void error(String msg, Throwable t, Object... args) {
        log(Level.ERROR, t, msg, args);
    }

    private static void log(Level level, Throwable t, String msg, Object... args) {
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }

        final String ts = LocalDateTime.now().format(TS);
        final String thread = Thread.currentThread().getName();
        final String caller = findCaller();
        final String levelStr = level.name();
        final String icon = levelIcon(level);
        final String coloredLevel = USE_COLOR ? colorize(level, levelStr) : levelStr;
        final String formatted = safeFormat(msg, args);

        String line = String.format("%s %s [%s] (%s) %s", ts, icon, coloredLevel, thread + ":" + caller, formatted);

        PrintStream stream = (level.ordinal() >= Level.WARN.ordinal()) ? System.err : System.out;
        stream.println(line);
        if (t != null) {
            t.printStackTrace(stream);
        }
    }

    private static String findCaller() {
        try {
            // Skip 0: findCaller, 1: log, 2+: public wrappers
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            for (int i = 3; i < st.length; i++) {
                String cls = st[i].getClassName();
                if (!cls.equals(Log.class.getName())) {
                    String method = st[i].getMethodName();
                    return simpleClassName(cls) + "." + method;
                }
            }
        } catch (Throwable ignored) {
        }
        return "?";
    }

    private static String simpleClassName(String fqcn) {
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }

    private static String safeFormat(String msg, Object... args) {
        try {
            return (args == null || args.length == 0) ? msg : String.format(Locale.ROOT, msg, args);
        } catch (Throwable e) {
            return msg + " (formatting failed)";
        }
    }

    private static String levelIcon(Level level) {
        return switch (level) {
            case TRACE ->
                USE_COLOR ? "·" : "·";
            case DEBUG ->
                USE_COLOR ? "🔧" : "DEBUG";
            case INFO ->
                USE_COLOR ? "ℹ️" : "INFO";
            case WARN ->
                USE_COLOR ? "⚠️" : "WARN";
            case ERROR ->
                USE_COLOR ? "❌" : "ERROR";
        };
    }

    private static String colorize(Level level, String text) {
        // ANSI colors
        final String RESET = "\u001B[0m";
        final String GRAY = "\u001B[90m";
        final String BLUE = "\u001B[34m";
        final String YELLOW = "\u001B[33m";
        final String RED = "\u001B[31m";
        final String GREEN = "\u001B[32m";

        String color = switch (level) {
            case TRACE ->
                GRAY;
            case DEBUG ->
                BLUE;
            case INFO ->
                GREEN;
            case WARN ->
                YELLOW;
            case ERROR ->
                RED;
        };
        return color + text + RESET;
    }

    private static Level resolveInitialLevel() {
        String env = System.getenv("LOG_LEVEL");
        if (env != null) {
            try {
                return Level.valueOf(env.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Level.INFO;
    }
}
