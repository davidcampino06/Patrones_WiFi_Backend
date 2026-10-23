package com.wifisense.service;

import com.wifisense.dto.AgentReportRequest;
import com.wifisense.model.TrackedDevice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Template Method</b>: records the devices of one agent report, the same way for the homes of Mi Wi-Fi and for the
 * team networks seen by a sensor.
 * <p>
 * Problem: the same loop (new device or known one, maker, name, kind, private MAC, still connected though marked as
 * not authorized, connection periods) was written twice, in {@code AgentReportService} and in
 * {@code SensorDeviceService}; every change (for example the kind of device in Avance 16) had to be made in both.
 * <p>
 * Solution: {@link #register} is the algorithm and is {@code final}; the subclasses only say where their devices
 * live ({@link HomeDeviceRegistry}, {@link TeamDeviceRegistry}). The first report of a network only learns its
 * devices: they are all new, but announcing them would be noise, so it reports 0 new devices.
 */
public abstract class DeviceRegistry<D extends TrackedDevice> {

    /** How many devices of the report had never been seen, and how many marked as not authorized are connected. */
    public record Outcome(int newDevices, int unauthorizedPresent) {
    }

    private final DeviceSessionTracker sessions;
    private final DeviceSessionTracker.Owner owner;

    protected DeviceRegistry(DeviceSessionTracker sessions, DeviceSessionTracker.Owner owner) {
        this.sessions = sessions;
        this.owner = owner;
    }

    /** The template method. Must run inside a transaction (the periods reference the devices it saves). */
    public final Outcome register(Long networkId, AgentReportRequest request, Instant now) {
        List<String> hashes = request.deviceHashesOrEmpty().stream().distinct().toList();
        Map<String, String> vendors = request.vendorsByFingerprint();
        Map<String, String> names = request.namesByFingerprint();
        Map<String, AgentReportRequest.Identity> identities = request.identitiesByFingerprint();
        Set<String> privates = request.privateFingerprints();
        boolean learning = !hasDevices(networkId);
        Map<String, D> known = new HashMap<>();
        knownDevices(networkId).forEach(d -> known.put(d.getFingerprint(), d));
        int fresh = 0;
        int unauthorized = 0;
        for (String hash : hashes) {
            D device = known.get(hash);
            boolean isNew = device == null;
            if (isNew) {
                device = newDevice(networkId, hash, now);
            } else {
                device.seen(now);
            }
            device.identify(vendors.get(hash), names.get(hash));
            AgentReportRequest.Identity identity = identities.get(hash);
            if (identity != null) {
                device.describe(identity.kind(), identity.role(), identity.model());
            }
            device.markPrivateMac(privates.contains(hash));
            if (isNew) {
                save(device);
                fresh++;
            } else if (!device.isAuthorized()) {
                unauthorized++;
            }
        }
        flush();  // the periods below reference the devices just saved
        sessions.track(owner, networkId, hashes, now);
        return new Outcome(learning ? 0 : fresh, unauthorized);
    }

    /** Whether the network already has devices (false: this is its first report). */
    protected abstract boolean hasDevices(Long networkId);

    protected abstract List<D> knownDevices(Long networkId);

    protected abstract D newDevice(Long networkId, String fingerprint, Instant seenAt);

    protected abstract void save(D device);

    protected abstract void flush();
}
