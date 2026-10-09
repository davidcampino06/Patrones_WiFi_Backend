package com.wifisense.service;

import com.wifisense.dto.LocationRequest;
import com.wifisense.dto.LocationResponse;
import com.wifisense.dto.ZoneRequest;
import com.wifisense.dto.ZoneResponse;
import com.wifisense.model.Location;
import com.wifisense.model.Zone;
import com.wifisense.repository.LocationRepository;
import com.wifisense.repository.ZoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locations;
    private final ZoneRepository zones;

    public LocationService(LocationRepository locations, ZoneRepository zones) {
        this.locations = locations;
        this.zones = zones;
    }

    public List<LocationResponse> listLocations() {
        return locations.findAll(Sort.by("name")).stream().map(LocationResponse::from).toList();
    }

    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        return LocationResponse.from(locations.save(new Location(request.name(), request.address(), request.city())));
    }

    public List<ZoneResponse> listZones(Long locationId) {
        List<Zone> result = locationId == null ? zones.findAllByOrderByName() : zones.findByLocationIdOrderByName(locationId);
        return result.stream().map(ZoneResponse::from).toList();
    }

    @Transactional
    public ZoneResponse createZone(Long locationId, ZoneRequest request) {
        Location location = locations.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", locationId));
        return ZoneResponse.from(zones.save(new Zone(location, request.name(), request.floor())));
    }
}
