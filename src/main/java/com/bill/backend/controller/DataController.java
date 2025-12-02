package com.bill.backend.controller;

import com.bill.backend.model.EspData;
import com.bill.backend.repository.EspDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "http://127.0.0.1:5500") // CORS fix
public class DataController {

    @Autowired
    private EspDataRepository espDataRepository;

    // Save new ESP32 sensor data
    @PostMapping
    public ResponseEntity<String> saveEspData(@RequestBody EspData data) {
        espDataRepository.save(data);
        return ResponseEntity.ok("ESP data saved successfully");
    }

    // Get the latest record for a device
    @GetMapping("/{espMac}")
    public ResponseEntity<?> getLatestDataByMac(@PathVariable String espMac) {
        EspData latest = espDataRepository.findTopByEspMacOrderByTimestampDesc(espMac);
        if (latest == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No data found for ESP MAC: " + espMac);
        }

        latest.setEnergyUsage(latest.getEnergyUsage() != null ? latest.getEnergyUsage() : 0.0);
        double predictedBill = latest.getEnergyUsage() * 5;
        double peakUsage = latest.getEnergyUsage() * 1.2;
        boolean tamperAlert = false;

        return ResponseEntity.ok(new Object() {
            public final Double voltage = latest.getVoltage();
            public final Double temperature = latest.getTemperature();
            public final Double humidity = latest.getHumidity();
            public final Double current = latest.getEnergyUsage();
            public final Double energyUsage = latest.getEnergyUsage();
            public final Double predictedBillRs = predictedBill;
            public final Double peakUsageW = peakUsage;
            public final boolean tamperAlertFlag = tamperAlert;
        });
    }
}
