package com.neumatica.embudo.whatsap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Contact;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>{
	
	/*@Query("""
	        SELECT DISTINCT c
	        FROM Contact c
	        LEFT JOIN FETCH c.conversations conv
	        LEFT JOIN FETCH conv.messages
	    """)
	    List<Contact> findAllWithConversations();*/

	Optional<Contact> findByPhone(String phone);
}
