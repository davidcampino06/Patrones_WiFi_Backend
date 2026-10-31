package com.wifisense.model;

/**
 * How well a device is identified, from the most to the least certain, so the page never shows a guess as a fact.
 * <ul>
 *   <li>ALIAS: the user named it (the label);</li>
 *   <li>CONFIRMED, ANNOUNCED, ROUTER, DNS: its automatic name, with where it came from ({@link NameSource});</li>
 *   <li>NAMED: it has a name but an agent older than 2.0.0 did not say where it came from;</li>
 *   <li>ESTIMATED: no name, only the maker (from the MAC) or the kind the agent deduced;</li>
 *   <li>UNKNOWN: nothing at all (typical of a phone with a private MAC that announces nothing).</li>
 * </ul>
 */
public enum DeviceIdentification {
    ALIAS, CONFIRMED, ANNOUNCED, ROUTER, DNS, NAMED, ESTIMATED, UNKNOWN;

    public static DeviceIdentification of(String label, String hostname, NameSource source, String vendor,
                                          String kind) {
        if (present(label)) {
            return ALIAS;
        }
        if (present(hostname)) {
            return source == null ? NAMED : valueOf(source.name());
        }
        return present(vendor) || present(kind) ? ESTIMATED : UNKNOWN;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
