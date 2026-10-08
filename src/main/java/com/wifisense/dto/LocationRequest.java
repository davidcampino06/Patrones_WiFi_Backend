package com.wifisense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(@NotBlank @Size(max = 100) String name, @Size(max = 200) String address,
                              @Size(max = 100) String city) {
}
