package com.pulsar.diagnostic.web.controller;

import com.pulsar.diagnostic.agent.agent.PulsarDiagnosticAgent;
import com.pulsar.diagnostic.agent.dto.QAResponse;
import com.pulsar.diagnostic.agent.service.ChatService;
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
    private final ChatService chatService;

    public ChatController(PulsarDiagnosticAgent agent, ChatService chatService) {
        this.agent = agent;
        this.chatService = chatService;
    }

    /**
     * Send a chat message and get a response
     */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}",
                truncate(request.message(), 50));

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
     * 知识问答接口
     *
     * 专门用于基于知识库的问答，返回结构化响应
     */
    @PostMapping("/qa")
    public ChatResponse knowledgeQA(@Valid @RequestBody ChatRequest request) {
        log.info("Received knowledge QA request: {}", truncate(request.message(), 50));

        long startTime = System.currentTimeMillis();

        QAResponse qaResponse = chatService.chatQA(request.message(), request.history());

        long processingTime = System.currentTimeMillis() - startTime;

        return ChatResponse.fromQA(qaResponse, processingTime);
    }

    /**
     * Stream a chat response
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request");

        return agent.chatStream(request.message());
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}