package com.wifisense.service;

import com.wifisense.model.SensorDevice;
import com.wifisense.repository.SensorDeviceRepository;

import java.time.Instant;
import java.util.List;

/**
 * The devices a sensor sees on a team network ({@code network_sensor_devices}): the steps of {@link DeviceRegistry}
 * for them.
 */
class TeamDeviceRegistry extends DeviceRegistry<SensorDevice> {

    private final SensorDeviceRepository devices;

    TeamDeviceRegistry(SensorDeviceRepository devices, DeviceSessionTracker sessions) {
        super(sessions, DeviceSessionTracker.Owner.TEAM);
        this.devices = devices;
    }

    @Override
    protected boolean hasDevices(Long networkId) {
        return devices.existsByNetworkId(networkId);
    }

    @Override
    protected List<SensorDevice> knownDevices(Long networkId) {
        return devices.findByNetworkIdOrderByFirstSeenAtAsc(networkId);
    }

    @Override
    protected SensorDevice newDevice(Long networkId, String fingerprint, Instant seenAt) {
        return new SensorDevice(networkId, fingerprint, seenAt);
    }

    @Override
    protected void save(SensorDevice device) {
        devices.save(device);
    }

    @Override
    protected void flush() {
        devices.flush();
    }
}
