package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;

import jakarta.transaction.Transactional;

/**
 * Servicio encargado de administrar la atención humana
 * de las conversaciones.
 */
@Service
public class ConversationAttentionService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    private final ConversationRepository conversationRepository;

    private final UserClientService userClientService;

    public ConversationAttentionService(
            ConversationRepository conversationRepository,
            UserClientService userClientService
    ) {

        this.conversationRepository =
                conversationRepository;

        this.userClientService =
                userClientService;
    }

    /**
     * Permite que un vendedor tome una conversación.
     *
     * Flujo:
     *
     * 1. Busca la conversación.
     * 2. Verifica que exista.
     * 3. Verifica que el vendedor exista.
     * 4. Verifica que esté disponible.
     * 5. Cambia BOT -> HUMAN.
     * 6. Asigna el vendedor.
     * 7. Guarda la fecha de asignación.
     */
    @Transactional
    public Conversation takeConversation(
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

        /*
         * Consultamos el usuario en Security Service.
         *
         * Esto evita asignar conversaciones a un usuario
         * inexistente.
         */
        var user = userClientService.findById(userId);

        if (user == null) {
            throw new RuntimeException(
                    "El usuario no existe."
            );
        }

        if (!user.isEnabled()) {
            throw new RuntimeException(
                    "El usuario está deshabilitado."
            );
        }

        if (!user.isAccountNonLocked()) {
            throw new RuntimeException(
                    "La cuenta del usuario está bloqueada."
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
         * Solamente permitimos tomar conversaciones
         * que todavía estén siendo manejadas por el BOT.
         */
        if (conversation.getStatus()
                != ConversationStatus.BOT) {

            throw new RuntimeException(
                    "La conversación ya no está disponible para atención."
            );
        }

        /*
         * Asignamos el vendedor.
         */
        conversation.setAssignedUserId(userId);

        /*
         * Guardamos cuándo fue asignada.
         */
        conversation.setAssignedAt(
                LocalDateTime.now(ZONE_ID)
        );

        /*
         * Cambiamos el control del BOT al vendedor.
         */
        conversation.setStatus(
                ConversationStatus.HUMAN
        );

        /*
         * Guardamos.
         */
        return conversationRepository.save(
                conversation
        );
    }

    /**
     * Cierra una conversación.
     */
    @Transactional
    public Conversation closeConversation(
            UUID conversationId
    ) {

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

        return conversationRepository.save(
                conversation
        );
    }

    /**
     * Permite volver una conversación al BOT.
     *
     * Esto puede ser útil posteriormente si un vendedor
     * quiere devolver la conversación a la automatización.
     */
    @Transactional
    public Conversation returnToBot(
            UUID conversationId
    ) {

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

        return conversationRepository.save(
                conversation
        );
    }
}
