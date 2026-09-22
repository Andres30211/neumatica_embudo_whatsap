package com.neumatica.embudo.whatsap.websocket;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.notification.Notification;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Message;

import lombok.RequiredArgsConstructor;

/**
 * Servicio encargado de enviar información
 * en tiempo real mediante WebSocket / STOMP.
 *
 * Actualmente maneja:
 *
 * 1. Nuevos contactos.
 * 2. Notificaciones generales.
 * 3. Nuevos mensajes de conversaciones HUMAN.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    private final SimpMessagingTemplate messagingTemplate;


    // =========================================================
    // NUEVO CONTACTO
    // =========================================================

    /**
     * Envía un nuevo contacto a todos los clientes
     * conectados al canal de contactos.
     */
    public void sendNewContact(Contact contact) {

        messagingTemplate.convertAndSend(
                "/topic/contacts",
                contact
        );
    }


    // =========================================================
    // NOTIFICACIÓN GENERAL
    // =========================================================

    /**
     * Envía una notificación general de nuevo mensaje
     * de WhatsApp.
     *
     * Este comportamiento conserva el funcionamiento
     * que ya tenías.
     */
    public void sendNotification(Contact contact) {

        Notification notification =
                new Notification(
                        UUID.randomUUID(),
                        "NEW_WHATSAPP_MESSAGE",
                        "Nuevo mensaje de WhatsApp",
                        contact.getName()
                                + " ha enviado un mensaje",
                        contact.getId(),
                        LocalDateTime.now(ZONE_ID),
                        false
                );

        messagingTemplate.convertAndSend(
                "/topic/notifications",
                notification
        );
    }


    // =========================================================
    // NUEVO MENSAJE DE CONVERSACIÓN
    // =========================================================

    /**
     * Envía un mensaje nuevo a todos los clientes
     * suscritos a la conversación correspondiente.
     *
     * Canal:
     *
     * /topic/conversations/{conversationId}
     *
     * Ejemplo:
     *
     * /topic/conversations/44d5f1fa-a206-4d9f-ad2e-f9bdb8f22fb2
     */
    public void sendConversationMessage(
            UUID conversationId,
            Message message
    ) {

        if (conversationId == null) {
            return;
        }

        if (message == null) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/conversations/"
                        + conversationId,
                message
        );
    }
}
