package com.wifisense.dto;

import com.wifisense.model.Zone;

public record ZoneResponse(Long id, Long locationId, String locationName, String name, Integer floor) {

    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getLocation().getId(), zone.getLocation().getName(),
                zone.getName(), zone.getFloor());
    }
}
