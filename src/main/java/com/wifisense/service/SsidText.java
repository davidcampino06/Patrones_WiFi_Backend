package com.wifisense.service;

/**
 * Network names come from the radio, chosen by anyone: they can carry accents, emoji, quotes or even control
 * characters. They are stored as they are, except the control characters (they would break logs and tables) and
 * a name that is only blanks, which is a hidden network. React escapes them when they are shown.
 */
final class SsidText {

    private SsidText() {
    }

    /** The name to store, or null for a hidden network. */
    static String clean(String ssid) {
        if (ssid == null) {
            return null;
        }
        String visible = ssid.codePoints().filter(c -> !Character.isISOControl(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        if (visible.isBlank()) {
            return null;
        }
        return limit(visible, 64);
    }

    /** At most {@code max} characters, counted like PostgreSQL does (an emoji is one), without splitting one. */
    static String limit(String text, int max) {
        return text.codePointCount(0, text.length()) <= max ? text : text.substring(0, text.offsetByCodePoints(0, max));
    }

    /** A short text from the system (maker, radio, security), without control characters and within its column. */
    static String cleanLabel(String text, int max) {
        String clean = clean(text);
        if (clean == null) {
            return null;
        }
        clean = clean.strip();
        return clean.isEmpty() ? null : limit(clean, max);
    }
}
