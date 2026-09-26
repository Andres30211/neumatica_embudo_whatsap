package com.neumatica.embudo.whatsap.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.interfaces.ConversationSummaryProjection;

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
    
    @Query(
    	    value = """
    	        SELECT
    	            x.conversation_id,
    	            x.contact_id,
    	            x.contact_name,
    	            x.phone,
    	            x.registration_step,
    	            x.last_message,
    	            x.last_message_type,
    	            x.last_message_at,
    	            x.status,
    	            x.assigned_user_id
    	        FROM (
    	            SELECT
    	                c.id AS conversation_id,
    	                ct.id AS contact_id,
    	                ct.name AS contact_name,
    	                ct.phone AS phone,
    	                ct.registration_step AS registration_step,

    	                m.body AS last_message,
    	                m.type AS last_message_type,
    	                m.created_at AS last_message_at,

    	                c.status AS status,

    	                /*
    	                 * Vendedor asignado.
    	                 */
    	                c.assigned_user_id AS assigned_user_id,

    	                ROW_NUMBER() OVER (
    	                    PARTITION BY c.id
    	                    ORDER BY
    	                        m.created_at DESC NULLS LAST,
    	                        m.id DESC
    	                ) AS row_number

    	            FROM conversation c

    	            INNER JOIN contact ct
    	                ON ct.id = c.contact_id

    	            LEFT JOIN message m
    	                ON m.conversation_id = c.id
    	        ) x

    	        WHERE x.row_number = 1

    	        ORDER BY
    	            x.last_message_at DESC NULLS LAST,
    	            x.conversation_id DESC
    	        """,

    	    countQuery = """
    	        SELECT COUNT(*)
    	        FROM conversation c

    	        INNER JOIN contact ct
    	            ON ct.id = c.contact_id
    	        """,

    	    nativeQuery = true
    	)
    	Page<ConversationSummaryProjection> findConversationSummaries(
    	        Pageable pageable
    	);
    
    long countByStatus(
            ConversationStatus status
    );

    long countByStartedAtGreaterThanEqualAndStartedAtLessThan(
            LocalDateTime from,
            LocalDateTime to
    );
}
