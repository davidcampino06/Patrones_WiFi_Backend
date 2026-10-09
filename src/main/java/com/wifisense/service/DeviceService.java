package com.wifisense.service;

import com.wifisense.dto.DeviceRequest;
import com.wifisense.dto.DeviceResponse;
import com.wifisense.dto.TrafficSessionResponse;
import com.wifisense.model.Device;
import com.wifisense.model.Network;
import com.wifisense.repository.DeviceRepository;
import com.wifisense.repository.NetworkRepository;
import com.wifisense.repository.TrafficSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository devices;
    private final NetworkRepository networks;
    private final TrafficSessionRepository sessions;

    public DeviceService(DeviceRepository devices, NetworkRepository networks, TrafficSessionRepository sessions) {
        this.devices = devices;
        this.networks = networks;
        this.sessions = sessions;
    }

    public List<DeviceResponse> list(Long networkId) {
        List<Device> result = networkId == null
                ? devices.findAllByOrderByLastSeenAtDesc()
                : devices.findByNetworkIdOrderByLastSeenAtDesc(networkId);
        return result.stream().map(DeviceResponse::from).toList();
    }

    @Transactional
    public DeviceResponse create(DeviceRequest request) {
        if (devices.existsByMacAddress(request.macAddress().toUpperCase())) {
            throw new DuplicateResourceException("Ya existe un dispositivo con la MAC " + request.macAddress());
        }
        Network network = networks.findById(request.networkId())
                .orElseThrow(() -> new ResourceNotFoundException("la red", request.networkId()));
        Device device = new Device(network, request.hostname(), request.ipAddress(), request.macAddress(),
                request.type(), request.signalStrength());
        return DeviceResponse.from(devices.save(device));
    }

    public List<TrafficSessionResponse> sessions(Long deviceId) {
        if (!devices.existsById(deviceId)) {
            throw new ResourceNotFoundException("el dispositivo", deviceId);
        }
        return sessions.findByDeviceIdOrderByStartedAtDesc(deviceId).stream().map(TrafficSessionResponse::from).toList();
    }
}
