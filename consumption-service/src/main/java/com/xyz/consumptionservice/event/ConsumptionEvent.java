package com.xyz.consumptionservice.event;

import java.math.BigDecimal;

public record ConsumptionEvent(
        Long consumptionId,
        Long pedestalId,
        String type,
        BigDecimal value,
        String unit,
        String recordedAt
) {
}
