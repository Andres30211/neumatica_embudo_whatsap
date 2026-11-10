package com.neumatica.embudo.whatsap.services;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neumatica.embudo.whatsap.dto.contact.ContactResponse;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.repository.ContactManagementService;
import com.neumatica.embudo.whatsap.repository.ContactRepository;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactManagementServiceImpl implements ContactManagementService {

	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private MediaStorageServiceImpl mediaStorageServiceImpl;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private ConversationRepository conversationRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<ContactResponse> getContacts(Pageable pageable) {

		return contactRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public ContactResponse getContactById(UUID id) {

		Contact contact = contactRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Contacto no encontrado: " + id));

		return toResponse(contact);
	}

	@Override
	@Transactional
	public void delete(UUID id) {

		// 1. Buscar contacto
		Contact contact = contactRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Contacto no encontrado."));

		// 2. Recorrer conversaciones y mensajes
		List<Conversation> conversations = contact.getConversations() == null ? List.of()
				: List.copyOf(contact.getConversations());

		for (Conversation conversation : conversations) {

			if (conversation == null) {
				continue;
			}

			List<Message> messages = conversation.getMessages() == null ? List.of()
					: List.copyOf(conversation.getMessages());

			// 3. Eliminar archivos multimedia
			for (Message message : messages) {

				if (message == null) {
					continue;
				}

				String storagePath = message.getStoragePath();

				if (storagePath != null && !storagePath.isBlank()) {
					try {
						mediaStorageServiceImpl.delete(storagePath);
					} catch (Exception e) {
						System.err.println("Error eliminando archivo: " + storagePath);
						System.err.println(e.getMessage());
					}
				}
			}
		}

		// 4. Eliminar contacto
		// Hibernate eliminará conversaciones y mensajes
		// mediante CascadeType.ALL.
		contactRepository.delete(contact);

		// 5. Ejecutar operaciones SQL
		contactRepository.flush();
	}

	private ContactResponse toResponse(Contact contact) {

		String registrationStatus = contact.getRegistrationStep() != null
				&& contact.getRegistrationStep().name().equals("COMPLETED") ? "COMPLETO" : "INCOMPLETO";

		return ContactResponse.builder().id(contact.getId()).name(contact.getName()).phone(contact.getPhone())
				.email(contact.getEmail()).company(contact.getCompany()).registrationStep(contact.getRegistrationStep())
				.registrationStatus(registrationStatus).firstContact(contact.getFirstContact())
				.lastInteraction(contact.getLastInteraction()).createdAt(contact.getCreatedAt()).build();
	}
}
