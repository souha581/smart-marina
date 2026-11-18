package com.xyz.consumptionservice.service;

import com.xyz.consumptionservice.client.PedestalClient;
import com.xyz.consumptionservice.dto.ConsumptionRequest;
import com.xyz.consumptionservice.dto.ConsumptionResponse;
import com.xyz.consumptionservice.entity.Consumption;
import com.xyz.consumptionservice.event.ConsumptionEvent;
import com.xyz.consumptionservice.repository.ConsumptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ConsumptionService {

    private final ConsumptionRepository repository;
    private final PedestalClient pedestalClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ConsumptionService(ConsumptionRepository repository,
                              PedestalClient pedestalClient,
                              KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.pedestalClient = pedestalClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(readOnly = true)
    public List<ConsumptionResponse> findAll() {
        return repository.findAll().stream()
                .map(ConsumptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsumptionResponse findById(Long id) {
        return ConsumptionResponse.from(getConsumption(id));
    }

    @Transactional(readOnly = true)
    public List<ConsumptionResponse> findByPedestalId(Long pedestalId) {
        return repository.findByPedestalId(pedestalId).stream()
                .map(ConsumptionResponse::from)
                .toList();
    }

    public ConsumptionResponse create(ConsumptionRequest request) {
        checkPedestalExists(request.pedestalId());

        Consumption c = new Consumption();
        c.setPedestalId(request.pedestalId());
        c.setType(request.type());
        c.setValue(request.value());
        c.setUnit(request.unit());
        c.setRecordedAt(LocalDateTime.now());

        // 1. Save to database
        Consumption saved = repository.save(c);

        // 2. Build the event
        ConsumptionEvent event = new ConsumptionEvent(
                saved.getId(),
                saved.getPedestalId(),
                saved.getType().name(),
                saved.getValue(),
                saved.getUnit(),
                saved.getRecordedAt().toString()
        );

        // 3. Publish to Kafka!
        kafkaTemplate.send("consumption-events", saved.getPedestalId().toString(), event);

        return ConsumptionResponse.from(saved);
    }

    public void delete(Long id) {
        repository.delete(getConsumption(id));
    }

    private Consumption getConsumption(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Consumption not found: " + id));
    }

    private void checkPedestalExists(Long pedestalId) {
        if (!pedestalClient.pedestalExists(pedestalId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pedestal not found: " + pedestalId);
        }
    }
}
