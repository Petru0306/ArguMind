package com.ArguMind.ArguMind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Topic-urile încep cu /topic pentru broadcast și /queue pentru mesaje private
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint-ul de conexiune. Este protejat automat de Spring Security 
        // deoarece nu a fost adăugat în requestMatchers().permitAll() din SecurityConfig.
        registry.addEndpoint("/ws-arena")
                .setAllowedOriginPatterns("*");
    }
}
