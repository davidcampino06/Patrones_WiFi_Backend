package com.wifisense.controller;

import com.wifisense.dto.DeviceRequest;
import com.wifisense.dto.DeviceResponse;
import com.wifisense.dto.TrafficSessionResponse;
import com.wifisense.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> list(@RequestParam(required = false) Long networkId) {
        return deviceService.list(networkId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse create(@Valid @RequestBody DeviceRequest request) {
        return deviceService.create(request);
    }

    @GetMapping("/{id}/sessions")
    public List<TrafficSessionResponse> sessions(@PathVariable Long id) {
        return deviceService.sessions(id);
    }
}
