package com.xyz.portservice.dto;


import com.xyz.portservice.entity.Port;

public record PortResponse(
        Long id,
        String code,
        String name,
        String city,
        String country
) {
    public static PortResponse from(Port port) {
        return new PortResponse(
                port.getId(),
                port.getCode(),
                port.getName(),
                port.getCity(),
                port.getCountry());
    }
}