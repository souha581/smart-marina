package com.xyz.smartpedestal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "pedestals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pedestal_serial_number",
                columnNames = "serial_number"))
public class Pedestal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number", nullable = false, length = 50)
    private String serialNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String marina;

    @Column(nullable = false, length = 50)
    private String pontoon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PedestalStatus status;

    public Pedestal() {
    }

    public Long getId() { return id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMarina() { return marina; }
    public void setMarina(String marina) { this.marina = marina; }

    public String getPontoon() { return pontoon; }
    public void setPontoon(String pontoon) { this.pontoon = pontoon; }

    public PedestalStatus getStatus() { return status; }
    public void setStatus(PedestalStatus status) { this.status = status; }
}