package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.neumatica.embudo.whatsap.dto.user.UserResponseDto;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.websocket.NotificationService;

import jakarta.transaction.Transactional;

/**
 * Servicio encargado de administrar la atención humana
 * de las conversaciones.
 *
 * Responsabilidades:
 *
 * - Tomar una conversación BOT y asignarla a un vendedor.
 * - Cambiar una conversación a HUMAN.
 * - Cerrar una conversación.
 * - Devolver una conversación al BOT.
 * - Notificar los cambios mediante WebSocket después
 *   de que la transacción haya sido confirmada.
 */
@Service
public class ConversationAttentionService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    private final ConversationRepository conversationRepository;

    private final UserClientService userClientService;

    private final NotificationService notificationService;

    public ConversationAttentionService(
            ConversationRepository conversationRepository,
            UserClientService userClientService,
            NotificationService notificationService
    ) {

        this.conversationRepository =
                conversationRepository;

        this.userClientService =
                userClientService;

        this.notificationService =
                notificationService;
    }


    // =========================================================
    // TOMAR CONVERSACIÓN
    // BOT -> HUMAN
    // =========================================================

    /**
     * Permite que un vendedor tome una conversación.
     *
     * Flujo:
     *
     * 1. Valida el vendedor.
     * 2. Busca la conversación.
     * 3. Verifica que esté disponible en BOT.
     * 4. Asigna el vendedor.
     * 5. Cambia BOT -> HUMAN.
     * 6. Guarda la conversación.
     * 7. Después del COMMIT publica el cambio por WebSocket.
     */
    @Transactional
    public Conversation takeConversation(
            UUID conversationId,
            UUID userId,
            String accessToken
    ) {

        validateRequiredParameters(
                conversationId,
                userId
        );

        /*
         * Obtenemos la información completa del vendedor.
         *
         * Además de validar al usuario, utilizaremos su nombre
         * para enviarlo inmediatamente al frontend mediante
         * WebSocket.
         */
        UserResponseDto user =
                userClientService.findById(
                        userId,
                        accessToken
                );

        validateUser(user);

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversación no encontrada."
                                )
                        );

        /*
         * Una conversación solamente puede ser tomada
         * si actualmente está siendo atendida por el BOT.
         */
        if (conversation.getStatus()
                != ConversationStatus.BOT) {

            throw new RuntimeException(
                    "La conversación no está disponible para atención."
            );
        }

        LocalDateTime now =
                LocalDateTime.now(ZONE_ID);

        /*
         * Asignamos el vendedor.
         */
        conversation.setAssignedUserId(
                userId
        );

        conversation.setAssignedAt(
                now
        );

        /*
         * Cambiamos BOT -> HUMAN.
         */
        conversation.setStatus(
                ConversationStatus.HUMAN
        );

        Conversation savedConversation =
                conversationRepository.save(
                        conversation
                );

        /*
         * Publicamos después del COMMIT.
         *
         * Enviamos también el nombre del vendedor que ya
         * obtuvimos del microservicio de seguridad.
         *
         * De esta manera NotificationService no necesita
         * realizar una segunda consulta HTTP.
         */
        publishAfterCommit(
                savedConversation,
                user.getName()
        );

        return savedConversation;
    }


    // =========================================================
    // CERRAR CONVERSACIÓN
    // =========================================================

    /**
     * Cierra una conversación.
     *
     * El cambio se publica por WebSocket después
     * de que la transacción haya sido confirmada.
     */
    @Transactional
    public Conversation closeConversation(
            UUID conversationId
    ) {

        if (conversationId == null) {

            throw new IllegalArgumentException(
                    "El conversationId es obligatorio."
            );
        }

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversación no encontrada."
                                )
                        );

        conversation.setStatus(
                ConversationStatus.CLOSED
        );

        conversation.setClosedAt(
                LocalDateTime.now(ZONE_ID)
        );

        Conversation savedConversation =
                conversationRepository.save(
                        conversation
                );

        /*
         * Notificamos después del COMMIT.
         */
        publishAfterCommit(
                savedConversation,
                null
        );

        return savedConversation;
    }


    // =========================================================
    // DEVOLVER AL BOT
    // =========================================================

    /**
     * Devuelve una conversación a la automatización.
     *
     * HUMAN -> BOT
     *
     * También elimina la asignación del vendedor.
     */
    @Transactional
    public Conversation returnToBot(
            UUID conversationId
    ) {

        if (conversationId == null) {

            throw new IllegalArgumentException(
                    "El conversationId es obligatorio."
            );
        }

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversación no encontrada."
                                )
                        );

        conversation.setStatus(
                ConversationStatus.BOT
        );

        /*
         * Eliminamos la asignación anterior.
         */
        conversation.setAssignedUserId(null);

        conversation.setAssignedAt(null);

        conversation.setClosedAt(null);

        Conversation savedConversation =
                conversationRepository.save(
                        conversation
                );

        /*
         * Notificamos después del COMMIT.
         */
        publishAfterCommit(
                savedConversation,
                null
        );

        return savedConversation;
    }


    // =========================================================
    // VALIDACIÓN DE PARÁMETROS
    // =========================================================

    private void validateRequiredParameters(
            UUID conversationId,
            UUID userId
    ) {

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
    }


    // =========================================================
    // VALIDACIÓN DEL VENDEDOR
    // =========================================================

    private void validateUser(
            UserResponseDto user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "El vendedor no existe."
            );
        }

        if (!user.isEnabled()) {

            throw new RuntimeException(
                    "El vendedor está deshabilitado."
            );
        }

        if (!user.isAccountNonLocked()) {

            throw new RuntimeException(
                    "El vendedor está bloqueado."
            );
        }
    }


    // =========================================================
    // WEBSOCKET DESPUÉS DEL COMMIT
    // =========================================================

    /**
     * Publica la actualización de la conversación
     * únicamente después de que la transacción haya
     * sido confirmada correctamente.
     *
     * Esto evita que el frontend reciba un evento
     * mientras los cambios todavía no son visibles
     * de forma definitiva en la base de datos.
     */
    private void publishAfterCommit(
            Conversation conversation,
            String assignedUserName
    ) {

        if (conversation == null) {
            return;
        }

        /*
         * Fallback para el caso excepcional en el que este
         * método se ejecute fuera de una transacción
         * administrada por Spring.
         */
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {

            notificationService
                    .sendConversationUpdate(
                            conversation,
                            assignedUserName
                    );

            return;
        }

        /*
         * Esperamos a que PostgreSQL confirme la transacción
         * antes de publicar la actualización por WebSocket.
         */
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                notificationService
                                        .sendConversationUpdate(
                                                conversation,
                                                assignedUserName
                                        );
                            }
                        }
                );
    }
}