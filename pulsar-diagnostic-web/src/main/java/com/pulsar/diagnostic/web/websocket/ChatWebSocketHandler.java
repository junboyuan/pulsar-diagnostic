package com.pulsar.diagnostic.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.web.dto.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * WebSocket handler for real-time chat streaming
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final PulsarDiagnosticAgent agent;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(PulsarDiagnosticAgent agent, ObjectMapper objectMapper) {
        this.agent = agent;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            ChatRequest request = objectMapper.readValue(payload, ChatRequest.class);

            log.debug("Received WebSocket message: {}",
                    request.message().substring(0, Math.min(50, request.message().length())));

            // Send response synchronously for now
            String response = agent.chat(request.message());
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            log.error("Failed to process WebSocket message", e);
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {} - {}", session.getId(), status);
    }
}