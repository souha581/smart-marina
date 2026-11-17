package com.xyz.smartpedestal.repository;

import com.xyz.smartpedestal.entity.Pedestal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedestalRepository extends JpaRepository<Pedestal, Long> {

    Optional<Pedestal> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);
}