package com.xyz.notification_service.listener;

import com.xyz.notification_service.event.ConsumptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumptionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ConsumptionNotificationListener.class);

    @KafkaListener(topics = "consumption-events", groupId = "notification-group")
    public void handleConsumptionEvent(ConsumptionEvent event) {
        log.info("📢 [NOTIFICATION] Received consumption reading for Pedestal #{}", event.pedestalId());
        log.info("📊 Details: {} {} of {}", event.value(), event.unit(), event.type());

        // Business rule: Alert if electricity consumption is high (> 50 kWh)
        if ("ELECTRICITY".equalsIgnoreCase(event.type()) && event.value().doubleValue() > 50.0) {
            log.warn("⚠️ [ALERT] High consumption detected for Pedestal #{}! Value: {} {}",
                    event.pedestalId(), event.value(), event.unit());
        }
    }
}
