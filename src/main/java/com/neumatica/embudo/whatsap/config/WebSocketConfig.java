package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {


    /**
     * ================================================================
     * ENDPOINT DE WEBSOCKET
     * ================================================================
     *
     * Angular se conecta a:
     *
     * wss://neumatica-embudo-whatsap.onrender.com/wss
     *
     * En localhost sería:
     *
     * ws://localhost:8080/wss
     */
    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        registry
                .addEndpoint("/wss")

                /*
                 * Permitimos los dos frontends.
                 *
                 * Esto es importante para el handshake
                 * del WebSocket.
                 */
                .setAllowedOriginPatterns(
                        "http://localhost:4200",
                        "https://neumatica-crm.netlify.app"
                );
    }


    /**
     * ================================================================
     * MESSAGE BROKER
     * ================================================================
     *
     * Los mensajes enviados a:
     *
     * /topic/...
     *
     * serán distribuidos por el broker.
     */
    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        /*
         * Suscripciones de los clientes.
         *
         * Ejemplo:
         *
         * /topic/contacts
         * /topic/notifications
         * /topic/conversations/{id}
         */
        registry.enableSimpleBroker(
                "/topic"
        );


        /*
         * Mensajes enviados desde Angular hacia
         * métodos @MessageMapping del backend.
         */
        registry.setApplicationDestinationPrefixes(
                "/app"
        );
    }
}