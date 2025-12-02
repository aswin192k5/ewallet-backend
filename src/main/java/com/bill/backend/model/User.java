package com.bill.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "esp_mac", nullable = false, unique = true, length = 100)
    private String espMac;

    @Column(name = "fullname", length = 100)
    private String fullname;

    // ---------- NEW FIELDS ----------
    // Available wallet balance in ₹
    @Column(name = "available_balance")
    private Double availableBalance = 0.0;

    // Monthly budget in ₹ (optional)
    @Column(name = "monthly_budget")
    private Double monthlyBudget = 0.0;

    // Allocated energy in kWh (converted from rupees)
    @Column(name = "allocated_energy_kwh")
    private Double allocatedEnergyKwh = 0.0;

    // Energy used in kWh (updated from ESP32 / consumption integration)
    @Column(name = "used_energy_kwh")
    private Double usedEnergyKwh = 0.0;

    // ---------- getters/setters ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEspMac() { return espMac; }
    public void setEspMac(String espMac) { this.espMac = espMac; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public Double getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(Double availableBalance) { this.availableBalance = availableBalance; }

    public Double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(Double monthlyBudget) { this.monthlyBudget = monthlyBudget; }

    public Double getAllocatedEnergyKwh() { return allocatedEnergyKwh; }
    public void setAllocatedEnergyKwh(Double allocatedEnergyKwh) { this.allocatedEnergyKwh = allocatedEnergyKwh; }

    public Double getUsedEnergyKwh() { return usedEnergyKwh; }
    public void setUsedEnergyKwh(Double usedEnergyKwh) { this.usedEnergyKwh = usedEnergyKwh; }
}
