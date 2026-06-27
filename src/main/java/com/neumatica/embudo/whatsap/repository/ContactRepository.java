package com.neumatica.embudo.whatsap.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Contact;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>{

	Optional<Contact> findByPhone(String phone);
}
