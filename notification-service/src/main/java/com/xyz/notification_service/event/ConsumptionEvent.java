package com.xyz.notification_service.event;

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
