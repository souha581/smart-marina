package com.xyz.smartpedestal.controller;

import com.xyz.smartpedestal.dto.PedestalRequest;
import com.xyz.smartpedestal.entity.Pedestal;
import com.xyz.smartpedestal.service.PedestalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedestals")
public class PedestalController {

    private final PedestalService service;

    public PedestalController(PedestalService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedestal create(@Valid @RequestBody PedestalRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Pedestal> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Pedestal findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Pedestal update(@PathVariable Long id,
                           @Valid @RequestBody PedestalRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}