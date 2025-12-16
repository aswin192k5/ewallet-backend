package com.bill.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeviceWebSocketHandler
 * - devices connect to /ws/device?mac=<MAC>
 * - UIs connect to /ws/ui
 * - device sessions stored in deviceSessions (mac -> session)
 * - ui sessions stored in uiSessions
 * - device 'data'/'heartbeat' messages are forwarded to all UI sessions (or filtered by mac)
 */
@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    // mac -> device session
    private final Map<String, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();
    // ui sessionId -> session
    private final Map<String, WebSocketSession> uiSessions = new ConcurrentHashMap<>();
    // reverse map sessionId -> mac (only for devices)
    private final Map<String, String> sessionToMac = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String query = session.getUri() != null ? session.getUri().getQuery() : null;

        // Detect UI vs device by path or query param
        boolean isUi = path != null && path.contains("/ws/ui");
        String mac = null;
        if (!isUi && query != null) {
            for (String kv : query.split("&")) {
                String[] parts = kv.split("=");
                if (parts.length == 2 && parts[0].equalsIgnoreCase("mac")) {
                    mac = parts[1].toUpperCase();
                }
            }
        }

        if (isUi) {
            uiSessions.put(session.getId(), session);
            session.sendMessage(new TextMessage("{\"type\":\"ui-registered\"}"));
            System.out.println("UI client connected: " + session.getId() + " | UI count: " + uiSessions.size());
        } else if (mac != null && !mac.isEmpty()) {
            deviceSessions.put(mac, session);
            sessionToMac.put(session.getId(), mac);
            session.sendMessage(new TextMessage("{\"type\":\"registered\",\"mac\":\""+mac+"\"}"));
            System.out.println("Device connected: " + mac);
            // notify UIs that device connected
            broadcastToUIs(mapper.createObjectNode().put("type", "device-connected").put("mac", mac).toString());
        } else {
            // unknown connection: treat as UI by default
            uiSessions.put(session.getId(), session);
            session.sendMessage(new TextMessage("{\"type\":\"ui-registered\"}"));
            System.out.println("Anonymous WS connected as UI: " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode node = mapper.readTree(payload);
        String type = node.has("type") ? node.get("type").asText() : "";

        // If message from device (session is in sessionToMac)
        if (sessionToMac.containsKey(session.getId())) {
            String mac = sessionToMac.get(session.getId());
            switch (type) {
                case "data":
                case "heartbeat":
                    // Optionally add server timestamp
                    ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("serverTs", System.currentTimeMillis());
                    ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("mac", mac);
                    // broadcast to UIs
                    broadcastToUIs(mapper.writeValueAsString(node));
                    break;
                case "commandResponse":
                    // forward to UIs too
                    broadcastToUIs(mapper.writeValueAsString(node));
                    break;
                default:
                    System.out.println("Device message from " + mac + ": " + payload);
            }
            return;
        }

        // If message from UI, handle UI-specific messages (subscribe/unsubscribe)
        // Example UI message: {"type":"ui-register","mac":"AA:BB:..."}
        if ("ui-register".equals(type)) {
            String uiMac = node.has("mac") ? node.get("mac").asText().toUpperCase() : null;
            // Optionally send latest device state to this UI (if you store it)
            // For now just ack
            session.sendMessage(new TextMessage("{\"type\":\"ui-ack\",\"mac\":\""+(uiMac==null?"":uiMac)+"\"}"));
            return;
        }

        // Other unknown messages
        System.out.println("Unhandled WS message (from UI?): " + payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String mac = sessionToMac.remove(session.getId());
        if (mac != null) {
            deviceSessions.remove(mac);
            System.out.println("Device disconnected: " + mac);
            broadcastToUIs(mapper.createObjectNode().put("type","device-disconnected").put("mac",mac).toString());
        } else {
            uiSessions.remove(session.getId());
            System.out.println("UI disconnected: " + session.getId());
        }
    }

    // Send a JSON string to the device with given MAC. Returns true if delivered.
    public boolean sendCommandToDevice(String mac, String jsonCommand) {
        try {
            WebSocketSession session = deviceSessions.get(mac.toUpperCase());
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(jsonCommand));
                return true;
            } else {
                System.out.println("Device " + mac + " offline or no session.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean isDeviceConnected(String mac) {
        WebSocketSession s = deviceSessions.get(mac.toUpperCase());
        return s != null && s.isOpen();
    }

    // Broadcast a text message to all connected UI sessions
    private void broadcastToUIs(String text) {
        Set<Map.Entry<String, WebSocketSession>> entries = uiSessions.entrySet();
        for (Map.Entry<String, WebSocketSession> e : entries) {
            WebSocketSession s = e.getValue();
            try {
                if (s != null && s.isOpen()) s.sendMessage(new TextMessage(text));
            } catch (Exception ex) {
                System.out.println("Broadcast failed to UI " + e.getKey() + ": " + ex.getMessage());
            }
        }
    }
}
