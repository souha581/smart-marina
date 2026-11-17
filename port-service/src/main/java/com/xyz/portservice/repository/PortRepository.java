package com.xyz.portservice.repository;


import com.xyz.portservice.entity.Port;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortRepository extends JpaRepository<Port, Long> {

    boolean existsByCode(String code);

    Optional<Port> findByCode(String code);
}
