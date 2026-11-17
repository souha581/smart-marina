package com.xyz.portservice.service;



import com.xyz.portservice.dto.PortRequest;
import com.xyz.portservice.dto.PortResponse;
import com.xyz.portservice.entity.Port;
import com.xyz.portservice.exception.DuplicateResourceException;
import com.xyz.portservice.exception.ResourceNotFoundException;
import com.xyz.portservice.repository.PortRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class PortService {

    private final PortRepository repository;

    public PortService(PortRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PortResponse> findAll() {
        return repository.findAll().stream()
                .map(PortResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PortResponse findById(Long id) {
        return PortResponse.from(getPort(id));
    }

    public PortResponse create(PortRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Port code already used: " + request.code());
        }
        Port port = new Port();
        copyRequest(request, port);
        return PortResponse.from(repository.save(port));
    }

    public PortResponse update(Long id, PortRequest request) {
        Port port = getPort(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(
                            "Port code already used: " + request.code());
                });
        copyRequest(request, port);
        return PortResponse.from(repository.save(port));
    }

    public void delete(Long id) {
        repository.delete(getPort(id));
    }

    private Port getPort(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Port not found: " + id));
    }

    private void copyRequest(PortRequest request, Port port) {
        port.setCode(request.code());
        port.setName(request.name());
        port.setCity(request.city());
        port.setCountry(request.country());
    }
}