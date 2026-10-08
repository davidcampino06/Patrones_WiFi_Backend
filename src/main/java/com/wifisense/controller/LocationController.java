package com.wifisense.controller;

import com.wifisense.dto.LocationRequest;
import com.wifisense.dto.LocationResponse;
import com.wifisense.dto.ZoneRequest;
import com.wifisense.dto.ZoneResponse;
import com.wifisense.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/locations")
    public List<LocationResponse> locations() {
        return locationService.listLocations();
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse createLocation(@Valid @RequestBody LocationRequest request) {
        return locationService.createLocation(request);
    }

    @GetMapping("/zones")
    public List<ZoneResponse> zones(@RequestParam(required = false) Long locationId) {
        return locationService.listZones(locationId);
    }

    @PostMapping("/locations/{locationId}/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse createZone(@PathVariable Long locationId, @Valid @RequestBody ZoneRequest request) {
        return locationService.createZone(locationId, request);
    }
}
