package com.neumatica.embudo.whatsap.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.brevo.EmailRequestDto;
import com.neumatica.embudo.whatsap.dto.webhook.ChageDto;
import com.neumatica.embudo.whatsap.dto.webhook.ContactDto;
import com.neumatica.embudo.whatsap.dto.webhook.EntryDto;
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
	
	@Autowired
	private MessageProcessingService messageProcessingService;
	
	@Override
	//@Transactional(readOnly = true)
	public Page<Contact> contacts(int page) {

	    Pageable pageable = PageRequest.of(
	            page,
	            5,
	            Sort.by(Sort.Direction.DESC, "createdAt")
	    );

	    Page<Contact> contactPage =
	            this.contactRepository.findAll(pageable);

	    for (Contact contact : contactPage.getContent()) {

	        List<Conversation> conversations =
	                this.conversationRepository.findByContact(contact);

	        for (Conversation conversation : conversations) {

	            List<Message> messages =
	                    this.messageRepository.findByConversation(conversation);

	            conversation.setMessages(messages);
	        }

	        contact.setConversations(conversations);
	    }

	    return contactPage;
	}
	
	/**
	 * Procesa el webhook recibido desde WhatsApp.
	 *
	 * <p>Este método actúa únicamente como orquestador.
	 * No contiene lógica específica de contactos, conversaciones,
	 * mensajes o multimedia.
	 *
	 * @param webhook webhook recibido desde WhatsApp.
	 */
	@Transactional
	@Override
	public void processWebhook(WhatsappWebHookDto webhook) {

	    /*
	     * Validamos que el webhook tenga una estructura
	     * mínimamente procesable.
	     */
	    if (!hasEntries(webhook)) {

	        System.out.println(
	                "Webhook recibido sin entries."
	        );

	        return;
	    }

	    /*
	     * Un webhook puede contener uno o varios entries.
	     */
	    for (EntryDto entry : webhook.getEntry()) {

	        if (entry == null
	                || entry.getChanges() == null
	                || entry.getChanges().isEmpty()) {

	            continue;
	        }

	        /*
	         * Un entry puede contener uno o varios changes.
	         */
	        for (ChageDto change : entry.getChanges()) {

	            if (change == null
	                    || change.getValue() == null) {

	                continue;
	            }

	            /*
	             * Delegamos el procesamiento del value.
	             */
	            processValue(
	                    change.getValue()
	            );
	        }
	    }
	}
	
	/*
	 * Valida si el webhook contiene al menos un entry
	 * que pueda ser procesado.
	 *
	 * @param webhook webhook recibido desde WhatsApp.
	 * @return {@code true} si existen entries para procesar;
	 *         {@code false} en caso contrario.
	 */
	private boolean hasEntries(WhatsappWebHookDto webhook) {

	    /*
	     * El webhook no puede ser procesado si es null.
	     */
	    if (webhook == null) {
	        return false;
	    }

	    /*
	     * Validamos que exista la colección de entries
	     * y que contenga al menos un elemento.
	     */
	    return webhook.getEntry() != null
	            && !webhook.getEntry().isEmpty();
	}
	
	private void processValue(ValueDto value) {

	    /*
	     * Los statuses representan eventos relacionados
	     * con mensajes enviados: enviado, entregado,
	     * leído, etc.
	     *
	     * Por ahora los ignoramos.
	     */
	    if (value.getStatuses() != null &&
	        !value.getStatuses().isEmpty()) {

	        System.out.println(
	            "Webhook de estado recibido"
	        );

	        value.getStatuses().forEach(status ->
	            System.out.println(
	                status.getStatus()
	            )
	        );

	        return;
	    }

	    /*
	     * Si no existen mensajes, no tenemos nada
	     * que procesar.
	     */
	    if (value.getMessages() == null ||
	        value.getMessages().isEmpty()) {

	        System.out.println(
	            "Webhook recibido sin mensajes."
	        );

	        return;
	    }

	    /*
	     * Procesamos cada mensaje individualmente.
	     */
	    for (MessageDto messageDTO :
	            value.getMessages()) {

	        processMessage(
	            value,
	            messageDTO
	        );
	    }
	}
	
	private void processMessage(
	        ValueDto value,
	        MessageDto messageDTO) {

	    if (messageDTO == null) {
	        return;
	    }

	    /*
	     * Intentamos obtener la información del contacto
	     * asociada al mensaje.
	     */
	    ContactDto contactDTO =
	        findContactForMessage(
	            value,
	            messageDTO
	        );

	    /*
	     * El contacto puede ser parcial.
	     *
	     * No debemos descartar el mensaje simplemente
	     * porque no exista phone, waId o profile.
	     */
	    Contact contact =
	        this.getOrCreateContact(
	            contactDTO,
	            messageDTO
	        );

	    /*
	     * Obtenemos o creamos la conversación.
	     */
	    Conversation conversation =
	        this.getOrCreateConversation(
	            contact
	        );

	    /*
	     * Procesamos completamente el mensaje:
	     *
	     * TEXT
	     * IMAGE
	     * VIDEO
	     * AUDIO
	     * DOCUMENT
	     * STICKER
	     * ...
	     *
	     * Incluyendo descarga de multimedia.
	     */
	    messageProcessingService.process(
	        messageDTO,
	        conversation
	    );

	    /*
	     * A partir de aquí puedes mantener
	     * tu lógica de negocio actual.
	     *
	     * No estamos tocando las respuestas automáticas.
	     */
	    processBusinessFlow(
	        contact,
	        messageDTO
	    );
	}
	
	 /*
	  * Identifica el contacto asociado al mensaje recibido desde WhatsApp.
	  *
	  * <p>WhatsApp puede enviar diferentes identificadores dependiendo
	  * del tipo de evento. Por esta razón, intentamos realizar la
	  * asociación utilizando los identificadores disponibles.
	  *
	  * <p>Orden de búsqueda:
	  * <ol>
	  *     <li>Meta User ID ({@code fromUserId}).</li>
	  *     <li>WhatsApp ID / teléfono ({@code from}).</li>
	  *     <li>Si solamente existe un contacto en el webhook,
	  *         se utiliza como fallback.</li>
	  * </ol>
	  *
	  * @param value información general del webhook.
	  * @param messageDTO mensaje recibido.
	  * @return {@link ContactDto} asociado al mensaje, o {@code null}
	  *         si no es posible identificarlo.
	  */
	private ContactDto findContactForMessage(
	        ValueDto value,
	        MessageDto messageDTO) {

	    /*
	     * Validamos que el webhook contenga contactos.
	     */
	    if (value == null
	            || value.getContacts() == null
	            || value.getContacts().isEmpty()) {

	        return null;
	    }

	    /*
	     * Identificadores enviados por WhatsApp en el mensaje.
	     */
	    String fromUserId = messageDTO.getFromUserId();
	    String from = messageDTO.getFrom();

	    /*
	     * ---------------------------------------------------------
	     * 1. Buscar por Meta User ID
	     * ---------------------------------------------------------
	     *
	     * Es el identificador más preciso cuando está disponible.
	     */
	    if (fromUserId != null && !fromUserId.isBlank()) {

	        for (ContactDto contact : value.getContacts()) {

	            if (contact == null) {
	                continue;
	            }

	            String userId = contact.getUserId();

	            if (userId != null && userId.equals(fromUserId)) {
	                return contact;
	            }
	        }
	    }

	    /*
	     * ---------------------------------------------------------
	     * 2. Buscar por WhatsApp ID
	     * ---------------------------------------------------------
	     *
	     * En muchos mensajes el campo "from" corresponde
	     * al identificador de WhatsApp del remitente.
	     */
	    if (from != null && !from.isBlank()) {

	        for (ContactDto contact : value.getContacts()) {

	            if (contact == null) {
	                continue;
	            }

	            String waId = contact.getWaId();

	            if (waId != null && waId.equals(from)) {
	                return contact;
	            }
	        }
	    }

	    /*
	     * ---------------------------------------------------------
	     * 3. Fallback
	     * ---------------------------------------------------------
	     *
	     * Si Meta solamente proporciona un contacto y no fue posible
	     * realizar la asociación mediante IDs, utilizamos ese contacto.
	     *
	     * Esto permite continuar procesando mensajes parciales.
	     */
	    if (value.getContacts().size() == 1) {
	        return value.getContacts().getFirst();
	    }

	    /*
	     * No fue posible determinar qué contacto corresponde
	     * al mensaje.
	     */
	    return null;
	}
	
	private void processBusinessFlow(
	        Contact contact,
	        MessageDto messageDTO) {

	    if (contact == null) {
	        return;
	    }

	    switch (contact.getRegistrationStep()) {

	        case GREETING -> {
	
	            contact.setRegistrationStep(
	                    RegistrationStep.EMAILANDCOMPANY
	            );
	
	            this.contactRepository.save(contact);
	            
	            this.notificationService.sendNotification(contact);
	            this.notificationService.sendNewContact(contact);
	
	            this.whatsappResponseAutimatics.sendText(
	                    contact.getPhone(),
	                    " Bienvenido a Neumática Industrial S.A.S.\n"
	                    .concat("Especialistas en automatización, neumática y aire comprimido.\n")
	                    .concat("Para brindarte una atención más ágil, por favor envía en un solo mensaje:\n\n")
	                    .concat(". Correo electrónico (en minúscula)\n")
	                    .concat(". Nombre de la empresa (sin caracteres especiales)\n")
	            );
	        }
	
	        case EMAILANDCOMPANY -> {
	        	
	        	this.notificationService.sendNotification(contact);
	            this.notificationService.sendNewContact(contact);
	            
	        	processEmailAndCompany(contact, messageDTO);
	        }
	        
	        case COMPLETED -> {
	        	
	        	this.notificationService.sendNotification(contact);
	            this.notificationService.sendNewContact(contact);
	            
	        	this.whatsappResponseAutimatics.sendText(contact.getPhone(),	        
	        		"Hola ".concat(contact.getName()).concat("\nBienvenido nuevamente a nuestro canal de atención; revisaremos tus datos y en unos minutos un asesor se comunicará contigo..."));
	        }
	    }
	}
	
	private Contact getOrCreateContact(
	        ContactDto dto,
	        MessageDto messageDTO) {

	    /*
	     * Intentamos identificar al contacto
	     * mediante la información disponible.
	     */

	    if (dto != null &&
	        dto.getWaId() != null &&
	        !dto.getWaId().isBlank()) {

	        return contactRepository
	            .findByPhone(dto.getWaId())
	            .orElseGet(() ->
	                createContact(dto)
	            );
	    }

	    if (dto != null &&
	        dto.getUserId() != null &&
	        !dto.getUserId().isBlank()) {

	        return contactRepository
	            .findByMetaUserId(dto.getUserId())
	            .orElseGet(() ->
	                createContact(dto)
	            );
	    }

	    /*
	     * No tenemos identificadores suficientes.
	     *
	     * Aun así creamos un contacto parcial.
	     */
	    return createContact(dto);
	}
	
	/*
	 * Crea un nuevo contacto a partir de la información
	 * disponible en el webhook de WhatsApp.
	 *
	 * <p>Los datos del contacto pueden ser parciales, ya que
	 * WhatsApp no siempre proporciona todos los identificadores
	 * o información del perfil.
	 *
	 * @param contactDto información recibida desde WhatsApp.
	 * @return contacto persistido en la base de datos.
	 */
	private Contact createContact(ContactDto contactDto) {

	    String phone = contactDto.getWaId();
	    String metaUserId = contactDto.getUserId();

	    /*
	     * El perfil puede no venir en determinados eventos.
	     * Por eso nunca debemos hacer directamente:
	     *
	     * contactDto.getProfile().getName()
	     *
	     * porque podría producir un NullPointerException.
	     */
	    String name = null;

	    if (contactDto.getProfile() != null) {
	        name = contactDto.getProfile().getName();
	    }

	    LocalDateTime now = LocalDateTime.now();

	    Contact contact = Contact.builder()
	            .phone(phone)
	            .metaUserId(metaUserId)
	            .name(name)
	            .firstContact(now)
	            .lastInteraction(now)
	            .createdAt(now)
	            .build();

	    return contactRepository.save(contact);
	}
	
	/*@Transactional
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
        
        
        switch (contact.getRegistrationStep()) {

	        case GREETING -> {
	
	            contact.setRegistrationStep(
	                    RegistrationStep.EMAILANDCOMPANY
	            );
	
	            this.contactRepository.save(contact);
	            
	            this.notificationService.sendNotification(contact);
	            this.notificationService.sendNewContact(contact);
	
	            this.whatsappResponseAutimatics.sendText(
	                    contact.getPhone(),
	                    " Bienvenido a Neumática Industrial S.A.S.\n"
	                    .concat("Especialistas en automatización, neumática y aire comprimido.\n")
	                    .concat("Para brindarte una atención más ágil, por favor envía en un solo mensaje:\n\n")
	                    .concat(". Correo electrónico (en minúscula)\n")
	                    .concat(". Nombre de la empresa (sin caracteres especiales)\n")
	            );
	        }
	
	        case EMAILANDCOMPANY -> {
	        	
	        	this.notificationService.sendNotification(contact);
	            this.notificationService.sendNewContact(contact);
	            
	        	processEmailAndCompany(contact, messageDTO);
	        }
	        
	        case COMPLETED -> {
	        	
	        	this.notificationService.sendNotification(contact);
	            this.notificationService.sendNewContact(contact);
	            
	        	this.whatsappResponseAutimatics.sendText(contact.getPhone(),	        
	        		"Hola ".concat(contact.getName()).concat("\nBienvenido nuevamente a nuestro canal de atención; revisaremos tus datos y en unos minutos un asesor se comunicará contigo..."));
	        }
	    }
    }*/

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

        Message message = this.messageMapper.toEntity(dto);

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
    		    String company = matcher.group(2);

    		    contact.setEmail(email);
    		    contact.setCompany(company);
    		    
    		    contact.setRegistrationStep(RegistrationStep.COMPLETED);
    		    
    		    this.sendMessageByHour(contact);
    		    
    		    EmailRequestDto emailRequestDto = new EmailRequestDto(contact.getEmail(), contact.getName(), contact.getCompany());
    		    try {
					this.brevoEmailServices.sendEmail(emailRequestDto, 4L);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		    
    		    contactRepository.save(contact);
    			
    			this.notificationService.sendNewContact(contact);
    			
    			
    		}
    }
    
    public void sendMessageByHour(Contact contact) {

        LocalTime ahora = LocalTime.now(
            ZoneId.of("America/Bogota")
        );

        LocalTime horaInicio = LocalTime.of(7, 0);
        LocalTime horaFin = LocalTime.of(17, 0);

        if (!ahora.isBefore(horaInicio) && ahora.isBefore(horaFin)) {
        	this.whatsappResponseAutimatics.sendText(contact.getPhone(),
		    		contact.getName()
		    		.concat("  ¡Gracias!\n")
		    		.concat("Hemos recibido tu información correctamente.")
		    		.concat("En este momento estamos asignando un asesor especializado, quien se pondrá en contacto contigo lo antes posible.\n")
		    		.concat("Agradecemos la confianza depositada en Neumática Industrial. Estamos comprometidos con brindarte soluciones que impulsen la productividad y eficiencia de tu empresa."));
        	return;
        }

        this.whatsappResponseAutimatics.sendText(contact.getPhone(),
	    		contact.getName()
	    		.concat(" Gracias por comunicarte con Neumática Industrial.\n")
	    		.concat("En este momento nuestro equipo se encuentra fuera del horario de atención. Hemos recibido tu mensaje y uno de nuestros asesores te responderá a primera hora del siguiente día hábil.")
	    		.concat("Agradecemos tu confianza."));
    }
    
    @Async
    public void sendCampaing() {
    	
    	List<Contact> emails = this.contactRepository.findAllEmails();
    	
    	emails.forEach(contact ->{
    			try {
    				this.brevoEmailServices.sendEmail(new EmailRequestDto(contact.getEmail(), contact.getName(), contact.getCompany()), 4L);
				} catch (Exception e) {
					System.out.println("Error enviado a: ".concat(contact.getEmail()));
				}
    			
    		});
    }

	@Override
	public void delete(UUID id) {
		this.contactRepository.deleteById(id);
		
	}
}
