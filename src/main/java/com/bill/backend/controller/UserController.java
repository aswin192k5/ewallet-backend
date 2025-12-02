package com.bill.backend.controller;

import com.bill.backend.model.User;
import com.bill.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private static final double TARIFF_PER_KWH = 8.0; // ₹8 per unit

    // ===================== SIGNUP =====================
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        if (userRepository.existsByUsername(user.getUsername())) {
            response.put("error", "Username already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            response.put("error", "Email already registered");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (userRepository.existsByEspMac(user.getEspMac())) {
            response.put("error", "ESP MAC address already in use");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        userRepository.save(user);
        response.put("message", "Signup successful");
        response.put("espMac", user.getEspMac());
        return ResponseEntity.ok(response);
    }

    // ===================== LOGIN =====================
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginRequest) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getPassword() != null && user.getPassword().equals(loginRequest.getPassword())) {
                    response.put("message", "Login successful");
                    response.put("username", user.getUsername());
                    response.put("espMac", user.getEspMac());
                    return ResponseEntity.ok(response);
                } else {
                    response.put("error", "Invalid username or password");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===================== GET USER PROFILE =====================
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(userOpt.get());
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===================== GET BUDGET & BILL =====================
    @GetMapping("/budget/{username}")
    public ResponseEntity<?> getBudget(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                double remaining = Math.max(0, user.getAllocatedEnergyKwh() - user.getUsedEnergyKwh());
                double predictiveBill = user.getUsedEnergyKwh() * TARIFF_PER_KWH;

                response.put("availableBalance", user.getAvailableBalance());
                response.put("allocatedEnergyKwh", user.getAllocatedEnergyKwh());
                response.put("usedEnergyKwh", user.getUsedEnergyKwh());
                response.put("remainingEnergyKwh", remaining);
                response.put("predictiveBill", predictiveBill);

                // If energy is 0, system should turn OFF (you’ll connect this to ESP32 later)
                if (remaining <= 0) {
                    response.put("status", "Power Off - No energy remaining");
                } else {
                    response.put("status", "Power On");
                }

                return ResponseEntity.ok(response);
            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===================== RECHARGE =====================
    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            double amount = ((Number) request.get("amount")).doubleValue();
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAvailableBalance(user.getAvailableBalance() + amount);
                userRepository.save(user);
                response.put("availableBalance", user.getAvailableBalance());
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===================== ALLOCATE ENERGY =====================
    @PostMapping("/allocate")
    public ResponseEntity<?> allocateEnergy(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            double amountKwh = request.get("amountKwh") != null ? ((Number) request.get("amountKwh")).doubleValue() : 0.0;
            boolean fromBalance = request.get("fromBalance") != null && (Boolean) request.get("fromBalance");

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Deduct balance if fromBalance is true
                if (fromBalance) {
                    double cost = amountKwh * TARIFF_PER_KWH;
                    if (user.getAvailableBalance() >= cost) {
                        user.setAvailableBalance(user.getAvailableBalance() - cost);
                    } else {
                        response.put("error", "Insufficient balance");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                }

                // Allocate energy
                user.setAllocatedEnergyKwh(user.getAllocatedEnergyKwh() + amountKwh);
                userRepository.save(user);

                double remaining = Math.max(0, user.getAllocatedEnergyKwh() - user.getUsedEnergyKwh());
                response.put("allocatedEnergyKwh", user.getAllocatedEnergyKwh());
                response.put("usedEnergyKwh", user.getUsedEnergyKwh());
                response.put("remainingEnergyKwh", remaining);
                response.put("availableBalance", user.getAvailableBalance());
                return ResponseEntity.ok(response);

            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===================== CONSUME ENERGY =====================
    @PostMapping("/consume")
    public ResponseEntity<?> consumeEnergy(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            double consumedKwh = ((Number) request.get("consumedKwh")).doubleValue();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Update used energy
                double newUsed = user.getUsedEnergyKwh() + consumedKwh;
                if (newUsed > user.getAllocatedEnergyKwh()) newUsed = user.getAllocatedEnergyKwh();
                user.setUsedEnergyKwh(newUsed);

                userRepository.save(user);

                double remaining = Math.max(0, user.getAllocatedEnergyKwh() - user.getUsedEnergyKwh());
                double predictiveBill = user.getUsedEnergyKwh() * TARIFF_PER_KWH;

                response.put("allocatedEnergyKwh", user.getAllocatedEnergyKwh());
                response.put("usedEnergyKwh", user.getUsedEnergyKwh());
                response.put("remainingEnergyKwh", remaining);
                response.put("predictiveBill", predictiveBill);

                if (remaining <= 0) {
                    response.put("status", "Power Off - No energy remaining");
                } else {
                    response.put("status", "Power On");
                }

                return ResponseEntity.ok(response);
            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===================== RESET MONTHLY ALLOCATION =====================
    @PostMapping("/resetMonthly")
    public ResponseEntity<?> resetMonthly(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAllocatedEnergyKwh(0.0);
                user.setUsedEnergyKwh(0.0);
                userRepository.save(user);
                response.put("message", "Monthly allocation reset successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
