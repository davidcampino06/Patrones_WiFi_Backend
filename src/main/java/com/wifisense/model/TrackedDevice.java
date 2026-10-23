package com.wifisense.model;

import java.time.Instant;

/**
 * A device seen by an agent: a device of a home of Mi Wi-Fi ({@link AgentDevice}) or of a team network seen by its
 * sensor ({@link SensorDevice}). Known only by the salted fingerprint of its MAC; what it announces about itself
 * (maker, name, model, kind) can only add to what is known, never erase it.
 */
public interface TrackedDevice {

    String getFingerprint();

    boolean isAuthorized();

    /** Moves the last time it was seen forward (never back). */
    void seen(Instant at);

    /** Keeps the first maker reported and the latest name the device announced. */
    void identify(String vendor, String hostname);

    /** Keeps the latest kind, role and model reported. */
    void describe(String kind, String role, String model);

    /** A MAC never stops being random for the same fingerprint, so the flag is only ever turned on. */
    void markPrivateMac(boolean privateMac);
}
