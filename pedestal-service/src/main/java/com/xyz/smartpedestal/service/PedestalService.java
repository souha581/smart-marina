package com.xyz.smartpedestal.service;

import com.xyz.smartpedestal.client.PortClient;
import com.xyz.smartpedestal.dto.PedestalRequest;
import com.xyz.smartpedestal.dto.PedestalResponse;
import com.xyz.smartpedestal.entity.Pedestal;
import com.xyz.smartpedestal.repository.PedestalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class PedestalService {

    private final PedestalRepository repository;
    private final PortClient portClient;

    public PedestalService(PedestalRepository repository, PortClient portClient) {
        this.repository = repository;
        this.portClient = portClient;
    }

    @Transactional(readOnly = true)
    public List<PedestalResponse> findAll() {
        return repository.findAll().stream()
                .map(PedestalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedestalResponse findById(Long id) {
        return PedestalResponse.from(getPedestal(id));
    }

    public PedestalResponse create(PedestalRequest request) {
        checkPortExists(request.portId());
        if (repository.existsBySerialNumber(request.serialNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Numéro de série déjà utilisé");
        }
        Pedestal p = new Pedestal();
        copyRequest(request, p);
        return PedestalResponse.from(repository.save(p));
    }

    public PedestalResponse update(Long id, PedestalRequest request) {
        Pedestal p = getPedestal(id);
        checkPortExists(request.portId());
        repository.findBySerialNumber(request.serialNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Numéro de série déjà utilisé");
                });
        copyRequest(request, p);
        return PedestalResponse.from(repository.save(p));
    }

    public void delete(Long id) {
        repository.delete(getPedestal(id));
    }

    private Pedestal getPedestal(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Borne introuvable : " + id));
    }

    private void checkPortExists(Long portId) {
        if (!portClient.portExists(portId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Port introuvable : " + portId);
        }
    }

    private void copyRequest(PedestalRequest request, Pedestal p) {
        p.setSerialNumber(request.serialNumber());
        p.setName(request.name());
        p.setPortId(request.portId());
        p.setPontoon(request.pontoon());
        p.setStatus(request.status());
    }
}