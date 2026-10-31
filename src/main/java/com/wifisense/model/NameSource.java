package com.wifisense.model;

/**
 * Where the automatic name of a device came from (agent 2.0.0). The alias the user writes is the device's label, and
 * the maker and kind are estimates: they never get a source of their own.
 */
public enum NameSource {
    /** Two independent sources gave the same name. */
    CONFIRMED,
    /** The device said it itself: mDNS, UPnP, NetBIOS or the host name of its DHCP request. */
    ANNOUNCED,
    /** The router of the network answered it (its DNS knows the names it handed out). */
    ROUTER,
    /** A reverse DNS lookup on the system's DNS server. */
    DNS;

    /** The source the agent sent, or null when it sent none or one this backend does not know. */
    public static NameSource parse(String value) {
        if (value == null) {
            return null;
        }
        for (NameSource source : values()) {
            if (source.name().equalsIgnoreCase(value.strip())) {
                return source;
            }
        }
        return null;
    }
}
