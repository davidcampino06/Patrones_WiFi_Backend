package com.wifisense.dto;

import com.wifisense.model.Location;

public record LocationResponse(Long id, String name, String address, String city) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(location.getId(), location.getName(), location.getAddress(), location.getCity());
    }
}
