package com.bill.backend.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "esp_data")
public class EspData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "esp_mac", nullable = false, length = 100)
    private String espMac;  // ✅ not unique

    @Column(name = "temperature", nullable = true)
    private Double temperature;

    @Column(name = "humidity", nullable = true)
    private Double humidity;

    @Column(name = "voltage", nullable = true)
    private Double voltage;

    @Column(name = "energy_usage", nullable = true)
    private Double energyUsage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp", updatable = false)
    @org.hibernate.annotations.CreationTimestamp  // ✅ auto timestamp
    private Date timestamp;

    public EspData() {}

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getEspMac() {
        return espMac;
    }

    public void setEspMac(String espMac) {
        this.espMac = espMac;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getVoltage() {
        return voltage;
    }

    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    public Double getEnergyUsage() {
        return energyUsage;
    }

    public void setEnergyUsage(Double energyUsage) {
        this.energyUsage = energyUsage;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    // ✅ optional setter (only if you want to set manually)
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
