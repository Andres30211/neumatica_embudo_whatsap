package com.neumatica.embudo.whatsap.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.interfaces.DailyMessageActivityProjection;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID>{
	
	List<Message> findByConversation(Conversation conversation);

	boolean existsByWhatsappMessageId(String whatsappMessageId);
	
	Optional<Message> findByWhatsappMessageId(String id);
	
	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime from,
            LocalDateTime to
    );

    long countByDirectionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Direction direction,
            LocalDateTime from,
            LocalDateTime to
    );

    long countByDirectionAndSenderUserIdIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Direction direction,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT COUNT(DISTINCT m.conversation.contact.id)
        FROM Message m
        WHERE m.direction = :direction
          AND m.createdAt >= :from
          AND m.createdAt < :to
          AND m.conversation.contact IS NOT NULL
    """)
    long countDistinctContactsByDirectionAndPeriod(
            @Param("direction") Direction direction,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
    
    @Query(
    	    value = """
    	        SELECT
    	            CAST(m.created_at AS DATE) AS date,
    	            m.direction AS direction,
    	            COUNT(*) AS total
    	        FROM message m
    	        WHERE m.created_at >= :from
    	          AND m.created_at < :to
    	        GROUP BY
    	            CAST(m.created_at AS DATE),
    	            m.direction
    	        ORDER BY
    	            CAST(m.created_at AS DATE)
    	        """,
    	    nativeQuery = true
    	)
    	List<DailyMessageActivityProjection> findDailyMessageActivity(
    	        @Param("from") LocalDateTime from,
    	        @Param("to") LocalDateTime to
    	);
}
