package com.xyz.consumptionservice.dto;

import com.xyz.consumptionservice.entity.ConsumptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConsumptionRequest(
        @NotNull @Positive Long pedestalId,
        @NotNull ConsumptionType type,
        @NotNull @Positive BigDecimal value,
        @NotBlank String unit
) {
}
