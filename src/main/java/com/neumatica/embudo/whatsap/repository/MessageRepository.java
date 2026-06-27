package com.neumatica.embudo.whatsap.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID>{

	boolean existsByWhatsappMessageId(String whatsappMessageId);
}
