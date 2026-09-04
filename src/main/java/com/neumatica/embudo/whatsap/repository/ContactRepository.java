package com.neumatica.embudo.whatsap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Contact;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>{
	
	Page<Contact> findAllByOrderByCreatedAtDesc(Pageable pageable);
	
	@Query("""
			select c from Contact c
			""")
	List<Contact> findAllEmails();

	Optional<Contact> findByPhone(String phone);
	
	Optional<Contact> findByMetaUserId(String metaUserId);
}
