package com.pulsar.diagnostic.web.config;

import com.pulsar.diagnostic.web.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;

/**
 * Web configuration for CORS and WebSocket
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    /**
     * Configure CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Configure WebSocket handler
     */
    @Bean
    public org.springframework.web.socket.server.HandshakeHandler handshakeHandler() {
        return new org.springframework.web.socket.server.support.DefaultHandshakeHandler();
    }
}