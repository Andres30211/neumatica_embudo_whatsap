package com.neumatica.embudo.whatsap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;

public interface ConversationRepository
        extends JpaRepository<Conversation, UUID> {

    /*
     * Busca una conversación de un contacto
     * que tenga un estado específico.
     */
    Optional<Conversation> findFirstByContactAndStatus(
            Contact contact,
            ConversationStatus status
    );

    /*
     * Busca todas las conversaciones de un contacto.
     */
    List<Conversation> findByContact(Contact contact);

    /*
     * Busca conversaciones asignadas a un vendedor.
     *
     * Será útil posteriormente para construir
     * "Mis conversaciones".
     */
    List<Conversation> findByAssignedUserIdAndStatus(
            UUID assignedUserId,
            ConversationStatus status
    );

    /*
     * Busca conversaciones humanas asignadas a un vendedor.
     */
    List<Conversation> findByAssignedUserIdAndStatusOrderByLastMessageAtDesc(
            UUID assignedUserId,
            ConversationStatus status
    );
}
