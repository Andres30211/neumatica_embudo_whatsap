package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;
import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;

import jakarta.transaction.Transactional;

/**
 * Servicio responsable de enviar mensajes manuales
 * desde el CRM hacia WhatsApp.
 */
@Service
public class ManualMessageService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    private final ConversationRepository
            conversationRepository;

    private final MessageRepository
            messageRepository;

    private final WhatsappResponseAutimatics
            whatsappResponseAutimatics;

    private final UserClientService
            userClientService;

    public ManualMessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WhatsappResponseAutimatics whatsappResponseAutimatics,
            UserClientService userClientService
    ) {

        this.conversationRepository =
                conversationRepository;

        this.messageRepository =
                messageRepository;

        this.whatsappResponseAutimatics =
                whatsappResponseAutimatics;

        this.userClientService =
                userClientService;
    }

    /**
     * Envía un mensaje manual desde el CRM.
     */
    @Transactional
    public Message sendMessage(
            UUID conversationId,
            UUID userId,
            String text
    ) {

        /*
         * Validamos los parámetros.
         */
        if (conversationId == null) {
            throw new IllegalArgumentException(
                    "El conversationId es obligatorio."
            );
        }

        if (userId == null) {
            throw new IllegalArgumentException(
                    "El userId es obligatorio."
            );
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "El mensaje no puede estar vacío."
            );
        }

        /*
         * Verificamos que el vendedor exista.
         */
        var user =
                userClientService.findById(userId);

        if (user == null) {
            throw new RuntimeException(
                    "Usuario no encontrado."
            );
        }

        /*
         * Buscamos la conversación.
         */
        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                    "Conversación no encontrada."
                                )
                        );

        /*
         * El vendedor solamente puede enviar mensajes
         * en una conversación HUMAN.
         */
        if (conversation.getStatus()
                != ConversationStatus.HUMAN) {

            throw new RuntimeException(
                    "La conversación no está en atención humana."
            );
        }

        /*
         * Verificamos que este vendedor sea el asignado.
         */
        if (!userId.equals(
                conversation.getAssignedUserId()
        )) {

            throw new RuntimeException(
                    "La conversación no está asignada a este vendedor."
            );
        }

        /*
         * Obtenemos el contacto.
         */
        Contact contact =
                conversation.getContact();

        if (contact == null) {
            throw new RuntimeException(
                    "La conversación no tiene contacto."
            );
        }

        /*
         * WhatsApp necesita un teléfono.
         */
        if (contact.getPhone() == null
                || contact.getPhone().isBlank()) {

            throw new RuntimeException(
                    "El contacto no tiene número de WhatsApp."
            );
        }

        /*
         * Primero enviamos el mensaje a WhatsApp.
         *
         * Tu servicio actual ya posee:
         *
         * whatsappResponseAutimatics.sendText(...)
         */
        whatsappResponseAutimatics.sendText(
                contact.getPhone(),
                text
        );

        /*
         * Creamos el mensaje para el historial del CRM.
         */
        LocalDateTime now =
                LocalDateTime.now(ZONE_ID);

        Message message =
                Message.builder()

                        /*
                         * Por ahora dejamos null porque
                         * tu método sendText() actual no devuelve
                         * el wamid generado por Meta.
                         */
                        .whatsappMessageId(null)

                        .conversation(conversation)

                        .direction(
                                Direction.OUTGOING
                        )

                        /*
                         * Identificamos al vendedor.
                         */
                        .senderUserId(userId)

                        .type(
                                MessageType.TEXT
                        )

                        .body(text)

                        .createdAt(now)

                        .whatsappTimestamp(
                                now.atZone(ZONE_ID)
                                        .toEpochSecond()
                        )

                        .build();

        /*
         * Agregamos el mensaje a la conversación.
         */
        conversation.addMessage(message);

        /*
         * Actualizamos la fecha del último mensaje.
         */
        conversation.setLastMessageAt(now);

        /*
         * Guardamos la conversación.
         *
         * CascadeType.ALL también persistirá el mensaje.
         */
        conversationRepository.save(
                conversation
        );

        return message;
    }
}