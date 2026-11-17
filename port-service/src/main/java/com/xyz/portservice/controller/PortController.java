package com.xyz.portservice.controller;

import com.xyz.portservice.dto.PortRequest;
import com.xyz.portservice.dto.PortResponse;
import com.xyz.portservice.service.PortService;
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
@RequestMapping("/api/v1/ports")
public class PortController {

    private final PortService service;

    public PortController(PortService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortResponse create(@Valid @RequestBody PortRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<PortResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PortResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public PortResponse update(@PathVariable Long id,
                               @Valid @RequestBody PortRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}