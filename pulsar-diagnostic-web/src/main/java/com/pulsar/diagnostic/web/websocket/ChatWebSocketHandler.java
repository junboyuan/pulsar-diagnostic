package com.pulsar.diagnostic.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.web.dto.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * WebSocket handler for real-time chat streaming
 */
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final PulsarDiagnosticAgent agent;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(PulsarDiagnosticAgent agent, ObjectMapper objectMapper) {
        this.agent = agent;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());

        return session.receive()
                .flatMap(message -> {
                    try {
                        String payload = message.getPayloadAsText();
                        ChatRequest request = objectMapper.readValue(payload, ChatRequest.class);

                        log.debug("Received WebSocket message: {}",
                                request.message().substring(0, Math.min(50, request.message().length())));

                        // Stream the response
                        return session.send(
                                agent.chatStream(request.message())
                                        .map(session::textMessage)
                                        .onErrorResume(e -> {
                                            log.error("Error in WebSocket stream", e);
                                            return Mono.just(session.textMessage("Error: " + e.getMessage()));
                                        })
                        );

                    } catch (Exception e) {
                        log.error("Failed to process WebSocket message", e);
                        return session.send(Mono.just(session.textMessage("Error: " + e.getMessage())));
                    }
                })
                .then();
    }
}