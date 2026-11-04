package com.xyz.smartpedestal.dto;

import com.xyz.smartpedestal.entity.PedestalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PedestalRequest(
        @NotBlank @Size(max = 50) String serialNumber,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String marina,
        @NotBlank @Size(max = 50) String pontoon,
        @NotNull PedestalStatus status
) {
}