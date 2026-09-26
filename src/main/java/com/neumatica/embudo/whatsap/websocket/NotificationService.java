package com.neumatica.embudo.whatsap.websocket;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.neumatica.embudo.whatsap.dto.contact.ConversationSummaryResponse;
import com.neumatica.embudo.whatsap.dto.notification.Notification;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;

import lombok.RequiredArgsConstructor;

/**
 * Servicio centralizado para publicar eventos en tiempo real
 * mediante WebSocket/STOMP.
 *
 * Canales principales:
 *
 * /topic/contacts
 *      Compatibilidad con el frontend existente.
 *
 * /topic/conversations
 *      Actualizaciones ligeras de la lista de conversaciones.
 *
 * /topic/conversations/{conversationId}
 *      Mensajes nuevos de una conversación concreta.
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
     * Mantiene el evento existente utilizado por otras partes
     * del CRM.
     *
     * Este evento no debe ser utilizado como fuente principal
     * para la nueva lista compacta de conversaciones.
     */
    public void sendNewContact(Contact contact) {

        if (contact == null) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/contacts",
                contact
        );
    }


    // =========================================================
    // NOTIFICACIÓN GENERAL
    // =========================================================

    /**
     * Envía la notificación general que utiliza el CRM.
     */
    public void sendNotification(Contact contact) {

        if (contact == null) {
            return;
        }

        String contactName =
                contact.getName();

        if (contactName == null
                || contactName.isBlank()) {

            contactName = "Contacto";
        }

        Notification notification =
                new Notification(
                        UUID.randomUUID(),
                        "NEW_WHATSAPP_MESSAGE",
                        "Nuevo mensaje de WhatsApp",
                        contactName
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
    // MENSAJE DE CONVERSACIÓN
    // =========================================================

    /**
     * Publica un mensaje nuevo en la conversación indicada.
     *
     * Canal:
     *
     * /topic/conversations/{conversationId}
     */
    public void sendConversationMessage(
            UUID conversationId,
            Message message
    ) {

        if (conversationId == null
                || message == null) {

            return;
        }

        messagingTemplate.convertAndSend(
                conversationTopic(conversationId),
                message
        );
    }


    // =========================================================
    // MENSAJE DE CONVERSACIÓN DESPUÉS DEL COMMIT
    // =========================================================

    /**
     * Publica un mensaje solamente después de que la
     * transacción haya sido confirmada.
     *
     * Esto evita que Angular reciba un mensaje antes de
     * que el INSERT correspondiente haya sido confirmado
     * en la base de datos.
     */
    public void sendConversationMessageAfterCommit(
            UUID conversationId,
            Message message
    ) {

        if (conversationId == null
                || message == null) {

            return;
        }

        afterCommit(() ->
                sendConversationMessage(
                        conversationId,
                        message
                )
        );
    }


    // =========================================================
    // ACTUALIZACIÓN DE CONVERSACIÓN
    // =========================================================

    /**
     * Publica una actualización de conversación sin
     * información adicional del vendedor.
     *
     * Se mantiene este método para conservar compatibilidad
     * con las llamadas existentes.
     */
    public void sendConversationUpdate(
            Conversation conversation
    ) {

        sendConversationUpdate(
                conversation,
                null
        );
    }


    /**
     * Publica una actualización de conversación incluyendo
     * opcionalmente el nombre del vendedor asignado.
     *
     * Este método es especialmente útil cuando se toma una
     * conversación, porque ConversationAttentionService ya
     * conoce el UserResponseDto del vendedor y no necesitamos
     * volver a consultar el microservicio de seguridad.
     */
    public void sendConversationUpdate(
            Conversation conversation,
            String assignedUserName
    ) {

        if (conversation == null
                || conversation.getId() == null) {

            return;
        }

        Contact contact =
                conversation.getContact();

        if (contact == null
                || contact.getId() == null) {

            return;
        }

        Message lastMessage =
                getLastMessage(conversation);

        ConversationSummaryResponse summary =
                buildConversationSummary(
                        conversation,
                        contact,
                        lastMessage,
                        assignedUserName
                );

        messagingTemplate.convertAndSend(
                "/topic/conversations",
                summary
        );
    }


    // =========================================================
    // ACTUALIZACIÓN DE CONVERSACIÓN DESPUÉS DEL COMMIT
    // =========================================================

    /**
     * Publica una actualización de conversación después
     * del COMMIT.
     *
     * Se utiliza cuando no necesitamos enviar explícitamente
     * el nombre del vendedor.
     */
    public void sendConversationUpdateAfterCommit(
            Conversation conversation
    ) {

        sendConversationUpdateAfterCommit(
                conversation,
                null
        );
    }


    /**
     * Publica una actualización de conversación después
     * del COMMIT incluyendo opcionalmente el nombre
     * del vendedor asignado.
     */
    public void sendConversationUpdateAfterCommit(
            Conversation conversation,
            String assignedUserName
    ) {

        if (conversation == null
                || conversation.getId() == null) {

            return;
        }

        Contact contact =
                conversation.getContact();

        if (contact == null
                || contact.getId() == null) {

            return;
        }

        /*
         * Construimos el DTO antes del commit.
         *
         * Esto es importante porque Contact y Message pueden
         * utilizar relaciones LAZY.
         */
        Message lastMessage =
                getLastMessage(conversation);

        ConversationSummaryResponse summary =
                buildConversationSummary(
                        conversation,
                        contact,
                        lastMessage,
                        assignedUserName
                );

        afterCommit(() ->
                messagingTemplate.convertAndSend(
                        "/topic/conversations",
                        summary
                )
        );
    }


    // =========================================================
    // RESUMEN DE CONVERSACIÓN
    // =========================================================

    /**
     * Publica un resumen de conversación utilizando
     * explícitamente el último mensaje recibido.
     *
     * En este escenario normalmente no tenemos que resolver
     * nuevamente el nombre del vendedor.
     */
    public void sendConversationSummary(
            Conversation conversation,
            Message lastMessage
    ) {

        sendConversationSummary(
                conversation,
                lastMessage,
                null
        );
    }


    /**
     * Variante que permite incluir el nombre del vendedor
     * cuando el llamador ya dispone de esa información.
     */
    public void sendConversationSummary(
            Conversation conversation,
            Message lastMessage,
            String assignedUserName
    ) {

        if (conversation == null
                || conversation.getId() == null) {

            return;
        }

        Contact contact =
                conversation.getContact();

        if (contact == null
                || contact.getId() == null) {

            return;
        }

        ConversationSummaryResponse summary =
                buildConversationSummary(
                        conversation,
                        contact,
                        lastMessage,
                        assignedUserName
                );

        messagingTemplate.convertAndSend(
                "/topic/conversations",
                summary
        );
    }


    // =========================================================
    // RESUMEN DE CONVERSACIÓN DESPUÉS DEL COMMIT
    // =========================================================

    /**
     * Publica un resumen de conversación después
     * del COMMIT.
     */
    public void sendConversationSummaryAfterCommit(
            Conversation conversation,
            Message lastMessage
    ) {

        sendConversationSummaryAfterCommit(
                conversation,
                lastMessage,
                null
        );
    }


    /**
     * Variante que permite incluir el nombre del vendedor
     * en el resumen enviado después del COMMIT.
     */
    public void sendConversationSummaryAfterCommit(
            Conversation conversation,
            Message lastMessage,
            String assignedUserName
    ) {

        if (conversation == null
                || conversation.getId() == null) {

            return;
        }

        Contact contact =
                conversation.getContact();

        if (contact == null
                || contact.getId() == null) {

            return;
        }

        ConversationSummaryResponse summary =
                buildConversationSummary(
                        conversation,
                        contact,
                        lastMessage,
                        assignedUserName
                );

        afterCommit(() -> {

            System.out.println(
                    "🔥🔥🔥 PUBLICANDO /topic/conversations"
            );

            System.out.println(
                    "Conversation ID: "
                            + summary.getConversationId()
            );

            System.out.println(
                    "Last message: "
                            + summary.getLastMessage()
            );

            System.out.println(
                    "Status: "
                            + summary.getStatus()
            );

            System.out.println(
                    "Assigned user ID: "
                            + summary.getAssignedUserId()
            );

            System.out.println(
                    "Assigned user name: "
                            + summary.getAssignedUserName()
            );

            messagingTemplate.convertAndSend(
                    "/topic/conversations",
                    summary
            );

            System.out.println(
                    "🔥🔥🔥 PUBLICACIÓN TERMINADA"
            );
        });
    }


    // =========================================================
    // CONSTRUIR RESUMEN
    // =========================================================

    /**
     * Construye el DTO utilizado por la lista compacta
     * de conversaciones.
     *
     * assignedUserName no se obtiene desde Conversation porque
     * ese dato pertenece al microservicio de seguridad.
     *
     * El nombre puede ser suministrado por el servicio que ya
     * haya consultado al vendedor.
     */
    private ConversationSummaryResponse buildConversationSummary(
            Conversation conversation,
            Contact contact,
            Message lastMessage,
            String assignedUserName
    ) {

        return ConversationSummaryResponse
                .builder()

                .conversationId(
                        conversation.getId()
                )

                .contactId(
                        contact.getId()
                )

                .contactName(
                        contact.getName()
                )

                .phone(
                        contact.getPhone()
                )

                .registrationStep(
                        contact.getRegistrationStep()
                )

                .lastMessage(
                        lastMessage != null
                                ? lastMessage.getBody()
                                : null
                )

                .lastMessageType(
                        lastMessage != null
                                ? lastMessage.getType()
                                : null
                )

                .lastMessageAt(
                        lastMessage != null
                                ? lastMessage.getCreatedAt()
                                : conversation.getLastMessageAt()
                )

                .status(
                        conversation.getStatus()
                )

                .assignedUserId(
                        conversation.getAssignedUserId()
                )

                .assignedUserName(
                        assignedUserName
                )

                .build();
    }


    // =========================================================
    // OBTENER ÚLTIMO MENSAJE
    // =========================================================

    /**
     * Obtiene el último mensaje de la conversación.
     *
     * Se utiliza principalmente cuando cambia el estado
     * de la conversación y necesitamos enviar un
     * ConversationSummaryResponse completo.
     */
    private Message getLastMessage(
            Conversation conversation
    ) {

        if (conversation.getMessages() == null
                || conversation.getMessages().isEmpty()) {

            return null;
        }

        return conversation
                .getMessages()
                .stream()
                .filter(message ->
                        message != null
                                && message.getCreatedAt() != null
                )
                .max(
                        (message1, message2) ->
                                message1
                                        .getCreatedAt()
                                        .compareTo(
                                                message2.getCreatedAt()
                                        )
                )
                .orElse(null);
    }


    // =========================================================
    // TOPIC DE CONVERSACIÓN
    // =========================================================

    private String conversationTopic(
            UUID conversationId
    ) {

        return "/topic/conversations/"
                + conversationId;
    }


    // =========================================================
    // AFTER COMMIT
    // =========================================================

    /**
     * Ejecuta una acción inmediatamente si no existe
     * una transacción activa.
     *
     * Si existe una transacción activa, la ejecución
     * se mueve al AFTER_COMMIT.
     */
    private void afterCommit(
            Runnable action
    ) {

        if (action == null) {
            return;
        }

        if (!TransactionSynchronizationManager
                .isSynchronizationActive()
                || !TransactionSynchronizationManager
                        .isActualTransactionActive()) {

            action.run();

            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                action.run();
                            }
                        }
                );
    }
}