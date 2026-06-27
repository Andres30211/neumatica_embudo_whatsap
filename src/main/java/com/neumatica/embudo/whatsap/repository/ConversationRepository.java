package com.neumatica.embudo.whatsap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID>{
	
	List<Conversation> findByContact(Contact contact);

	Optional<Conversation> findFirstByContactAndStatus(
            Contact contact,
            ConversationStatus status);
}
