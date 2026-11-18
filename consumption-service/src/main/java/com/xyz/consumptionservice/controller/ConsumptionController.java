package com.xyz.consumptionservice.controller;

import com.xyz.consumptionservice.dto.ConsumptionRequest;
import com.xyz.consumptionservice.dto.ConsumptionResponse;
import com.xyz.consumptionservice.service.ConsumptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consumptions")
public class ConsumptionController {

    private final ConsumptionService service;

    public ConsumptionController(ConsumptionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsumptionResponse create(@Valid @RequestBody ConsumptionRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ConsumptionResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ConsumptionResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/pedestal/{pedestalId}")
    public List<ConsumptionResponse> findByPedestalId(@PathVariable Long pedestalId) {
        return service.findByPedestalId(pedestalId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}