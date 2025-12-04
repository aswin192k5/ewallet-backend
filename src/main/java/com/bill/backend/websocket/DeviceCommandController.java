package com.bill.backend.websocket;

import com.bill.backend.websocket.DeviceWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceCommandController {

    private final DeviceWebSocketHandler wsHandler;

    public DeviceCommandController(DeviceWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(@RequestBody Map<String, Object> body) {
        String mac = ((String) body.get("mac")).toUpperCase();
        String command = (String) body.get("command");

        Map<String, Object> cmdPayload = new HashMap<>();
        cmdPayload.put("type", "command");
        cmdPayload.put("command", command);

        // convert to JSON string quickly
        String json = String.format("{\"type\":\"command\",\"command\":\"%s\"}", command);

        boolean sent = wsHandler.sendCommandToDevice(mac, json);
        if (sent) {
            return ResponseEntity.ok(Map.of("status", "sent"));
        } else {
            return ResponseEntity.status(503).body(Map.of("status", "device-offline"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> deviceStatus(@RequestParam String mac) {
        boolean online = wsHandler.isDeviceConnected(mac);
        return ResponseEntity.ok(Map.of("mac", mac, "connected", online));
    }
}
