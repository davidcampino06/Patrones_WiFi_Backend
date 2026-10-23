package com.wifisense.service;

import com.wifisense.model.AgentDevice;
import com.wifisense.repository.AgentDeviceRepository;

import java.time.Instant;
import java.util.List;

/** The devices of a home of Mi Wi-Fi ({@code agent_devices}): the steps of {@link DeviceRegistry} for them. */
class HomeDeviceRegistry extends DeviceRegistry<AgentDevice> {

    private final AgentDeviceRepository devices;

    HomeDeviceRegistry(AgentDeviceRepository devices, DeviceSessionTracker sessions) {
        super(sessions, DeviceSessionTracker.Owner.HOME);
        this.devices = devices;
    }

    @Override
    protected boolean hasDevices(Long networkId) {
        return devices.existsByNetworkId(networkId);
    }

    @Override
    protected List<AgentDevice> knownDevices(Long networkId) {
        return devices.findByNetworkIdOrderByFirstSeenAtAsc(networkId);
    }

    @Override
    protected AgentDevice newDevice(Long networkId, String fingerprint, Instant seenAt) {
        return new AgentDevice(networkId, fingerprint, seenAt);
    }

    @Override
    protected void save(AgentDevice device) {
        devices.save(device);
    }

    @Override
    protected void flush() {
        devices.flush();
    }
}
