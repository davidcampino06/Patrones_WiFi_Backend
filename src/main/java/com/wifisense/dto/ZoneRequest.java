package com.wifisense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZoneRequest(@NotBlank @Size(max = 100) String name, Integer floor) {
}
