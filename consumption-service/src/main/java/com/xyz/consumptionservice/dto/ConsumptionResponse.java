package com.xyz.consumptionservice.dto;

import com.xyz.consumptionservice.entity.Consumption;
import com.xyz.consumptionservice.entity.ConsumptionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumptionResponse(
        Long id,
        Long pedestalId,
        ConsumptionType type,
        BigDecimal value,
        String unit,
        LocalDateTime recordedAt
) {
    public static ConsumptionResponse from(Consumption c) {
        return new ConsumptionResponse(
                c.getId(),
                c.getPedestalId(),
                c.getType(),
                c.getValue(),
                c.getUnit(),
                c.getRecordedAt()
        );
    }
}