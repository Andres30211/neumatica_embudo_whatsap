package com.neumatica.embudo.whatsap.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.brevo.EmailRequestDto;
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
import com.neumatica.embudo.whatsap.websocket.NotificationService;

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
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private BrevoEmailServices brevoEmailServices;
	
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
		
		// Ignorar estados de entrega, leído, enviado, etc.
		if (!value.getStatuses().isEmpty()) {

		    System.out.println("Webhook de estado recibido");

		    value.getStatuses().forEach(status ->
		        System.out.println(status.getStatus())
		    );

		    return;
		}
		
		// No hay mensajes
	    if (value.getMessages() == null || value.getMessages().isEmpty()) {
	        System.out.println("Webhook recibido sin mensajes. Se ignora.");
	        return;
	    }
	    
	    MessageDto messageDTO = value.getMessages().getFirst();

	    // No hay contactos
	    if (value.getContacts() == null || value.getContacts().isEmpty()) {
	        System.out.println("Webhook recibido sin contactos. Se ignora.");
	        return;
	    }

        ContactDto contactDTO = value.getContacts().getFirst();

        Contact contact = this.getOrCreateContact(contactDTO);

        Conversation conversation = getOrCreateConversation(contact);
        
        saveMessage(conversation, messageDTO);
        
        this.notificationService.sendNewContact(contact);
        
        switch (contact.getRegistrationStep()) {

	        case GREETING -> {
	
	            contact.setRegistrationStep(
	                    RegistrationStep.EMAILANDCOMPANY
	            );
	
	            this.contactRepository.save(contact);
	
	            this.whatsappResponseAutimatics.sendText(
	                    contact.getPhone(),
	                    "Hola " + contact.getName() + " 👋\n"
	                    + "Bienvenido a Neumatica Industrial.\n\n"
	                    + "Para comenzar necesitamos:\n\n"
	                    + "1. Email\n"
	                    + "2. Nombre de la empresa"
	            );
	        }
	
	        case EMAILANDCOMPANY -> processEmailAndCompany(contact, messageDTO);
	        
	        case COMPLETED -> this.whatsappResponseAutimatics.sendText(contact.getPhone(),
	        		"Hola ".concat(contact.getName()).concat("\nBienvenido nuevamente a nuestro canal de atención; revisaremos tus datos y en unos minutos un asesor se comunicará contigo..."));
	
	    }
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
                    
                    contact.setRegistrationStep(RegistrationStep.GREETING);

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
    	
        if (this.messageRepository.existsByWhatsappMessageId(dto.getId())) {
            return;
        }

        Message message = this.messageMapper.toEntity(dto, conversation);

        conversation.addMessage(message);
        
        conversation.setLastMessageAt(
        	    Instant.ofEpochSecond(Long.parseLong(dto.getTimestamp()))
        	           .atZone(ZoneId.systemDefault())
        	           .toLocalDateTime()
        	);

        this.conversationRepository.save(conversation);

    }

    public void processEmailAndCompany(Contact contact, MessageDto messageDTO){
    	
    	Pattern pattern = Pattern.compile(
    		    "^\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\s*\\r?\\n\\s*(.+?)\\s*$",
    		    Pattern.MULTILINE
    		);

    		Matcher matcher = pattern.matcher(messageDTO.getText().getBody());

    		if (matcher.find()) {
    		    String email = matcher.group(1);
    		    String empresa = matcher.group(2);

    		    contact.setEmail(email);
    		    contact.setCompany(empresa);
    		    
    		    contact.setRegistrationStep(RegistrationStep.COMPLETED);
    		    
    		    this.whatsappResponseAutimatics.sendText(contact.getPhone(), "Perfecto ".concat(contact.getName()).concat("\nEn unos minútos un asesor se comunicara con tigo..."));
    		    
    		    EmailRequestDto emailRequestDto = new EmailRequestDto(email/*, empresa, "Saludo inicial...", "<h1>Hola desde neumatica industrial...</h1>"*/);
    		    try {
					this.brevoEmailServices.sendEmail(emailRequestDto);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		    
    		    contactRepository.save(contact);
    			
    			this.notificationService.sendNewContact(contact);
    			
    			
    		}

    }

	@Override
	public void delete(UUID id) {
		this.contactRepository.deleteById(id);
		
	}
}
