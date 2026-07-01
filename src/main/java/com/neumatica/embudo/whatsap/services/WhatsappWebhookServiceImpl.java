package com.neumatica.embudo.whatsap.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.webhook.ContactDto;
import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.dto.webhook.ValueDto;
import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.RegistrationStep;
import com.neumatica.embudo.whatsap.mapper.ContactMapper;
import com.neumatica.embudo.whatsap.mapper.MessageMapper;
import com.neumatica.embudo.whatsap.repository.ContactRepository;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;
import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;
import com.neumatica.embudo.whatsap.repository.WhatsappWebhookService;

import jakarta.transaction.Transactional;

@Service
public class WhatsappWebhookServiceImpl implements  WhatsappWebhookService{
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private ConversationRepository conversationRepository;
	
	@Autowired
	private MessageRepository messageRepository;
	
	@Autowired
	private WhatsappResponseAutimatics whatsappResponseAutimatics;
	
	@Autowired
	private ContactMapper contactMapper;
	
	@Autowired
	private MessageMapper messageMapper;
	
	@Override
	public List<Contact> contacts() {
		
		List<Contact> contacts = this.contactRepository.findAll();

        for (Contact contact : contacts) {

            List<Conversation> conversations =
                    this.conversationRepository.findByContact(contact);

            for (Conversation conversation : conversations) {

                List<Message> messages =
                        this.messageRepository.findByConversation(conversation);

                conversation.setMessages(messages);
            }

            contact.setConversations(conversations);
        }

        return contacts;
    }
	
	@Transactional
	@Override
	public void processWebhook(WhatsappWebHookDto webhook) {
		
		ValueDto value = webhook.getEntry()
                .getFirst()
                .getChanges()
                .getFirst()
                .getValue();

        ContactDto contactDTO = value.getContacts().getFirst();

        MessageDto messageDTO = value.getMessages().getFirst();

        Contact contact = this.getOrCreateContact(contactDTO);
        System.out.println(contact.getName());

        Conversation conversation = getOrCreateConversation(contact);
        System.out.println(conversation.getLastMessageAt());
        
        saveMessage(conversation, messageDTO);
        
        
        /*switch (contact.getRegistrationStep()) {

	        case EMAIL -> processEmail(contact, messageDTO);
	
	        case COUNTRY -> processCountry(contact, messageDTO);
	
	        case CITY -> processCity(contact, messageDTO);
	
	        case COMPLETED -> {
	
	            this.whatsappResponseAutimatics.sendText(
	                    contact.getPhone(),
	                    "En un momento te atenderá un asesor."
	            );
	
	        }

        }*/
    }

    private Contact getOrCreateContact(ContactDto dto) {

        return this.contactRepository.findByPhone(dto.getWaId())
                .map(contact -> {

                    contact.setName(dto.getProfile().getName());

                    contact.setLastInteraction(LocalDateTime.now());

                    return this.contactRepository.save(contact);

                })
                .orElseGet(() -> {

                    Contact contact = contactMapper.toEntity(dto);
                    
                    contact.setRegistrationStep(RegistrationStep.EMAIL);

                    return this.contactRepository.save(contact);

                });

    }

    private Conversation getOrCreateConversation(Contact contact) {

        return this.conversationRepository
                .findFirstByContactAndStatus(
                        contact,
                        ConversationStatus.BOT
                )
                .orElseGet(() -> {

                    Conversation conversation =
                            Conversation.builder()
                                    .contact(contact)
                                    .status(ConversationStatus.BOT)
                                    .startedAt(LocalDateTime.now())
                                    .lastMessageAt(LocalDateTime.now())
                                    .build();

                    return this.conversationRepository.save(conversation);

                });

    }

    private void saveMessage(Conversation conversation,
                             MessageDto dto) {
    	
    	List<Message> messages = conversation.getMessages()
    			.stream()
    			.map(m -> new Message(m.getWhatsappMessageId(),
    					conversation,
    					m.getDirection(),
    					m.getType(),
    					m.getBody(),
    					m.getMediaId(),
    					m.getMimeType(),
    					m.getSha256(),
    					m.getCaption(),
    					m.getWhatsappTimestamp(),
    					m.getCreatedAt())
					).collect(Collectors.toList());
    	
    	conversation.setMessages(messages);

    	this.conversationRepository.save(conversation);
    	
        /*if (this.messageRepository.existsByWhatsappMessageId(dto.getId())) {
            return;
        }

        Message message = this.messageMapper.toEntity(dto, conversation);

        this.messageRepository.save(message);
        
        conversation.setLastMessageAt(
        	    Instant.ofEpochSecond(Long.parseLong(dto.getTimestamp()))
        	           .atZone(ZoneId.systemDefault())
        	           .toLocalDateTime()
        	);

        this.conversationRepository.save(conversation);*/

    }

    private void processEmail(Contact contact, MessageDto messageDTO){

		String email = messageDTO.getText().getBody();
		
		contact.setEmail(email);
		
		contact.setRegistrationStep(RegistrationStep.COUNTRY);
		
		contactRepository.save(contact);
		
		whatsappResponseAutimatics.sendText(
		contact.getPhone(),
		"Perfecto 😊\nAhora escribe tu país."
		);

    }
    
    private void processCity(Contact contact,
            MessageDto messageDTO){

		contact.setCity(
		messageDTO.getText().getBody()
		);
		
		contact.setRegistrationStep(
		RegistrationStep.COMPLETED
		);
		
		contactRepository.save(contact);
		
		whatsappResponseAutimatics.sendText(
		contact.getPhone(),
		"Gracias. \nAhora escribe tu ciudad"
		);
		
    }
    
    private void processCountry(Contact contact,
            MessageDto messageDTO){

		contact.setCountry(
		messageDTO.getText().getBody()
		);
		
		contact.setRegistrationStep(
		RegistrationStep.CITY
		);
		
		contactRepository.save(contact);
		
		whatsappResponseAutimatics.sendText(
		contact.getPhone(),
		"✅ Registro completado.\\nEn unos minutos un asesor te atenderá."
		);
		
	}

	@Override
	public void delete(UUID id) {
		this.contactRepository.deleteById(id);
		
	}
}
