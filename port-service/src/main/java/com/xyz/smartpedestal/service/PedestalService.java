package com.xyz.smartpedestal.service;

import com.xyz.smartpedestal.dto.PedestalRequest;
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

    public PedestalService(PedestalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Pedestal> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedestal findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Borne introuvable : " + id));
    }

    public Pedestal create(PedestalRequest request) {
        if (repository.existsBySerialNumber(request.serialNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Numéro de série déjà utilisé");
        }
        Pedestal p = new Pedestal();
        copyRequest(request, p);
        return repository.save(p);
    }

    public Pedestal update(Long id, PedestalRequest request) {
        Pedestal p = findById(id);
        repository.findBySerialNumber(request.serialNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Numéro de série déjà utilisé");
                });
        copyRequest(request, p);
        return repository.save(p);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private void copyRequest(PedestalRequest request, Pedestal p) {
        p.setSerialNumber(request.serialNumber());
        p.setName(request.name());
        p.setMarina(request.marina());
        p.setPontoon(request.pontoon());
        p.setStatus(request.status());
    }
}