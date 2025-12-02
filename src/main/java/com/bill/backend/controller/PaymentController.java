package com.bill.backend.controller;

import com.bill.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500",
        "https://aswin192k5.github.io"    // ⭐ Frontend hosted on GitHub Pages
})
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    private final RestTemplate restTemplate = new RestTemplate();

    // ---------------------------------------------------------
    // CREATE RAZORPAY ORDER
    // ---------------------------------------------------------
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            if (!payload.containsKey("amount")) {
                return ResponseEntity.badRequest().body(Map.of("error", "amount_missing"));
            }

            double amount = Double.parseDouble(payload.get("amount").toString());
            String currency = payload.getOrDefault("currency", "INR").toString();
            String receipt = payload.getOrDefault("receipt", "rcpt_" + System.currentTimeMillis()).toString();

            Map<String, Object> order = paymentService.createOrder(amount, currency, receipt);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "order_creation_failed", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------
    // VERIFY PAYMENT & RECHARGE WALLET
    // ---------------------------------------------------------
    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndRecharge(@RequestBody Map<String, Object> payload) {
        try {
            String username = payload.getOrDefault("username", "").toString();
            if (username.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "username_missing"));
            }

            double amount = Double.parseDouble(payload.getOrDefault("amount", "0").toString());
            String razorpayOrderId = payload.getOrDefault("razorpay_order_id", "").toString();
            String razorpayPaymentId = payload.getOrDefault("razorpay_payment_id", "").toString();
            String razorpaySignature = payload.getOrDefault("razorpay_signature", "").toString();

            if (razorpayOrderId.isBlank() || razorpayPaymentId.isBlank() || razorpaySignature.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "missing_payment_fields"));
            }

            boolean verified = paymentService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            if (!verified) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "verification_failed"));
            }

            // ---------------------------------------------------------
            // UPDATED — USE DEPLOYED BACKEND URL INSTEAD OF localhost
            // ---------------------------------------------------------

            String rechargeUrl = "https://ewallet-backend-2-6ge9.onrender.com/api/user/recharge";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> rechargeBody = new HashMap<>();
            rechargeBody.put("username", username);
            rechargeBody.put("amount", amount);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(rechargeBody, headers);

            ResponseEntity<Map> rechargeResponse =
                    restTemplate.postForEntity(rechargeUrl, httpEntity, Map.class);

            if (!rechargeResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "recharge_failed",
                                "code", rechargeResponse.getStatusCodeValue()));
            }

            Map<?, ?> body = rechargeResponse.getBody();
            return ResponseEntity.ok(body != null ? body : Map.of("status", "recharged"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "verification_error", "message", e.getMessage()));
        }
    }
}
