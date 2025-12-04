package com.bill.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToMac = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        String mac = null;

        if (query != null) {
            for (String kv : query.split("&")) {
                String[] parts = kv.split("=");
                if (parts.length == 2 && parts[0].equalsIgnoreCase("mac")) {
                    mac = parts[1].toUpperCase();
                }
            }
        }

        if (mac != null) {
            deviceSessions.put(mac, session);
            sessionToMac.put(session.getId(), mac);
            session.sendMessage(new TextMessage("{\"type\":\"registered\",\"mac\":\""+mac+"\"}"));
            System.out.println("Device connected → " + mac);
        } else {
            System.out.println("UI client connected");
            session.sendMessage(new TextMessage("{\"type\":\"ui-registered\"}"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = mapper.readTree(message.getPayload());

        String type = node.has("type") ? node.get("type").asText() : "";

        switch (type) {
            case "data":
            case "heartbeat":
                System.out.println("Device Data: " + node);
                break;

            case "commandResponse":
                System.out.println("Response: " + node);
                break;

            default:
                System.out.println("Unknown message: " + node);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String mac = sessionToMac.remove(session.getId());
        if (mac != null) {
            deviceSessions.remove(mac);
            System.out.println("Device disconnected → " + mac);
        }
    }

    public boolean sendCommandToDevice(String mac, String json) {
        try {
            WebSocketSession session = deviceSessions.get(mac.toUpperCase());
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(json));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isDeviceConnected(String mac) {
        WebSocketSession s = deviceSessions.get(mac.toUpperCase());
        return s != null && s.isOpen();
    }
}
