package com.xyz.consumptionservice.repository;

import com.xyz.consumptionservice.entity.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    List<Consumption> findByPedestalId(Long pedestalId);
}