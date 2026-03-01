package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.web.dto.ChatRequest;
import com.pulsar.diagnostic.web.dto.ChatResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST controller for chat interactions
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final PulsarDiagnosticAgent agent;

    public ChatController(PulsarDiagnosticAgent agent) {
        this.agent = agent;
    }

    /**
     * Send a chat message and get a response
     */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}",
                request.message().substring(0, Math.min(50, request.message().length())));

        long startTime = System.currentTimeMillis();

        String response;
        if (request.history() != null && !request.history().isEmpty()) {
            response = agent.chat(request.message(), request.history());
        } else {
            response = agent.chat(request.message());
        }

        long processingTime = System.currentTimeMillis() - startTime;

        return ChatResponse.of(response, processingTime);
    }

    /**
     * Stream a chat response
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request");

        return agent.chatStream(request.message());
    }
}