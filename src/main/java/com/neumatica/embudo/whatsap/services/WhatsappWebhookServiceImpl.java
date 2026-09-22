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

    private final MediaStorageServiceImpl mediaStorageServiceImpl;
	
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

    WhatsappWebhookServiceImpl(MediaStorageServiceImpl mediaStorageServiceImpl) {
        this.mediaStorageServiceImpl = mediaStorageServiceImpl;
    }
	
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
	
	/*
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
	            this.getOrCreateConversation(contact);

	    messageProcessingService.process(
	            messageDTO,
	            conversation
	    );

	    /*
	     * Si la conversación está siendo atendida
	     * por un vendedor, NO ejecutamos el bot.
	     */
	    if (conversation.getStatus()
	            == ConversationStatus.HUMAN) {

	        /*
	         * Aquí posteriormente enviaremos una notificación
	         * específica al vendedor mediante WebSocket.
	         */

	        return;
	    }

	    /*
	     * Si todavía está en BOT,
	     * ejecutamos la automatización.
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

	    /*
	     * Un contacto puede llegar desde Meta sin número telefónico.
	     *
	     * En ese caso:
	     * - Conservamos el contacto.
	     * - Conservamos el mensaje.
	     * - No intentamos enviar una respuesta a WhatsApp.
	     *
	     * El resto del flujo podrá continuar cuando posteriormente
	     * tengamos disponible un número telefónico.
	     */
	    boolean hasPhone =
	            contact.getPhone() != null
	                    && !contact.getPhone().isBlank();

	    if (!hasPhone) {

	        addNoPhoneObservation(messageDTO);

	        return;
	    }

	    /*
	     * Protección para contactos antiguos que puedan tener
	     * registrationStep = NULL.
	     */
	    if (contact.getRegistrationStep() == null) {

	        contact.setRegistrationStep(
	                RegistrationStep.GREETING
	        );

	        this.contactRepository.save(contact);
	    }

	    switch (contact.getRegistrationStep()) {

	        case GREETING -> {

	            contact.setRegistrationStep(
	                    RegistrationStep.EMAILANDCOMPANY
	            );

	            this.contactRepository.save(contact);

	            this.notificationService.sendNotification(
	                    contact
	            );

	            this.notificationService.sendNewContact(
	                    contact
	            );

	            /*
	             * El mensaje pertenece al primer contacto.
	             *
	             * El método sendAutomaticMessage() se encargará
	             * de determinar si estamos dentro o fuera del
	             * horario de atención.
	             */
	            sendAutomaticMessage(
	                    contact,

	                    "Bienvenido a Neumática Industrial S.A.S.\n"
	                    .concat(
	                            "Especialistas en automatización, neumática y aire comprimido.\n"
	                    )
	                    .concat(
	                            "Para brindarte una atención más ágil, por favor envía en un solo mensaje:\n\n"
	                    )
	                    .concat(
	                            ". Correo electrónico (en minúscula)\n"
	                    )
	                    .concat(
	                            ". Nombre de la empresa (sin caracteres especiales)\n"
	                    )
	            );
	        }

	        case EMAILANDCOMPANY -> {

	            this.notificationService.sendNotification(
	                    contact
	            );

	            this.notificationService.sendNewContact(
	                    contact
	            );

	            processEmailAndCompany(
	                    contact,
	                    messageDTO
	            );
	        }

	        case COMPLETED -> {

	            this.notificationService.sendNotification(
	                    contact
	            );

	            this.notificationService.sendNewContact(
	                    contact
	            );

	            sendAutomaticMessage(
	                    contact,

	                    "Hola "
	                    .concat(contact.getName())
	                    .concat(
	                            "\nBienvenido nuevamente a nuestro canal de atención; revisaremos tus datos y en unos minutos un asesor se comunicará contigo..."
	                    )
	            );
	        }
	    }
	}
	
	
	private void addNoPhoneObservation(
	        MessageDto messageDTO) {

	    /*
	     * Validamos que tengamos el ID del mensaje recibido
	     * para poder localizarlo en la base de datos.
	     */
	    if (messageDTO == null
	            || messageDTO.getId() == null
	            || messageDTO.getId().isBlank()) {

	        return;
	    }

	    /*
	     * Buscamos el mensaje que acabamos de guardar.
	     */
	    Message message =
	            this.messageRepository
	                    .findByWhatsappMessageId(
	                            messageDTO.getId()
	                    )
	                    .orElse(null);

	    if (message == null) {
	        return;
	    }

	    /*
	     * Mensaje interno que queremos dejar registrado
	     * para que posteriormente pueda ser visualizado
	     * desde el CRM.
	     */
	    String observation =
	            "[SISTEMA] Este contacto no tiene un número de teléfono "
	            + "disponible. Actualmente no puede ser atendido mediante "
	            + "respuesta automática de WhatsApp. El contacto y su mensaje "
	            + "han sido conservados para futuras implementaciones de atención.";

	    /*
	     * Si el mensaje original es de texto, agregamos la observación
	     * al contenido existente.
	     */
	    if (message.getBody() != null
	            && !message.getBody().isBlank()) {

	        message.setBody(
	                message.getBody()
	                        + "\n\n"
	                        + observation
	        );

	    /*
	     * Si es multimedia y tiene caption, agregamos la observación
	     * al caption.
	     */
	    } else if (message.getCaption() != null
	            && !message.getCaption().isBlank()) {

	        message.setCaption(
	                message.getCaption()
	                        + "\n\n"
	                        + observation
	        );

	    /*
	     * Si es multimedia pero no tiene caption, utilizamos
	     * el caption para almacenar la observación.
	     */
	    } else {

	        message.setCaption(
	                observation
	        );
	    }

	    /*
	     * Persistimos nuevamente el mensaje.
	     */
	    this.messageRepository.save(message);
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

	    LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Bogota"));

	    Contact contact = Contact.builder()
	            .phone(phone)
	            .metaUserId(metaUserId)
	            .name(name)
	            .registrationStep(RegistrationStep.GREETING)
	            .firstContact(now)
	            .lastInteraction(now)
	            .createdAt(now)
	            .build();

	    return contactRepository.save(contact);
	}
	

    private Contact getOrCreateContact(ContactDto dto) {

        return this.contactRepository.findByPhone(dto.getWaId())
                .map(contact -> {

                    contact.setName(dto.getProfile().getName());

                    contact.setLastInteraction(LocalDateTime.now(ZoneId.of("America/Bogota")));

                    return this.contactRepository.save(contact);

                })
                .orElseGet(() -> {

                    Contact contact = contactMapper.toEntity(dto);
                    
                    contact.setRegistrationStep(RegistrationStep.GREETING);

                    return this.contactRepository.save(contact);

                });

    }

    private Conversation getOrCreateConversation(
            Contact contact
    ) {

        /*
         * Primero buscamos una conversación HUMAN.
         *
         * Esto es importante porque significa que
         * actualmente existe un vendedor atendiendo.
         */
        var humanConversation =
                conversationRepository
                        .findFirstByContactAndStatus(
                                contact,
                                ConversationStatus.HUMAN
                        );

        if (humanConversation.isPresent()) {

            return humanConversation.get();
        }

        /*
         * Si no hay conversación humana,
         * buscamos la conversación del BOT.
         */
        var botConversation =
                conversationRepository
                        .findFirstByContactAndStatus(
                                contact,
                                ConversationStatus.BOT
                        );

        if (botConversation.isPresent()) {

            return botConversation.get();
        }

        /*
         * Si no existe ninguna conversación activa,
         * creamos una nueva conversación BOT.
         */
        LocalDateTime now =
                LocalDateTime.now(
                        ZoneId.of("America/Bogota")
                );

        Conversation conversation =
                Conversation.builder()
                        .contact(contact)
                        .status(
                                ConversationStatus.BOT
                        )
                        .startedAt(now)
                        .lastMessageAt(now)
                        .build();

        return conversationRepository.save(
                conversation
        );
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
        	           .atZone(ZoneId.of("America/Bogota"))
        	           .toLocalDateTime()
        	);

        this.conversationRepository.save(conversation);

    }

    public void processEmailAndCompany(
            Contact contact,
            MessageDto messageDTO) {

        if (contact == null || messageDTO == null) {
            return;
        }

        /*
         * Esta etapa del flujo espera que el contacto envíe:
         *
         * correo electrónico
         * nombre de la empresa
         *
         * Por lo tanto, solamente debemos procesar mensajes de tipo TEXT.
         */
        if (!"text".equalsIgnoreCase(messageDTO.getType())) {
            return;
        }

        if (messageDTO.getText() == null
                || messageDTO.getText().getBody() == null
                || messageDTO.getText().getBody().isBlank()) {
            return;
        }

        Pattern pattern = Pattern.compile(
                "^\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\s*\\r?\\n\\s*(.+?)\\s*$",
                Pattern.MULTILINE
        );

        Matcher matcher =
                pattern.matcher(
                        messageDTO.getText().getBody()
                );

        if (matcher.find()) {

            String email = matcher.group(1);
            String company = matcher.group(2);

            contact.setEmail(email);
            contact.setCompany(company);
            contact.setRegistrationStep(
                    RegistrationStep.COMPLETED
            );

            sendAutomaticMessage(
                    contact,
                    contact.getName()
                            .concat("  ¡Gracias!\n")
                            .concat(
                                    "Hemos recibido tu información correctamente."
                            )
                            .concat(
                                    "En este momento estamos asignando un asesor especializado, "
                                    + "quien se pondrá en contacto contigo lo antes posible.\n"
                            )
                            .concat(
                                    "Agradecemos la confianza depositada en Neumática Industrial. "
                                    + "Estamos comprometidos con brindarte soluciones que impulsen "
                                    + "la productividad y eficiencia de tu empresa."
                            )
            );

            EmailRequestDto emailRequestDto =
                    new EmailRequestDto(
                            contact.getEmail(),
                            contact.getName(),
                            contact.getCompany()
                    );

            try {

                this.brevoEmailServices.sendEmail(
                        emailRequestDto,
                        4L
                );

            } catch (Exception e) {

                e.printStackTrace();
            }

            contactRepository.save(contact);

            this.notificationService.sendNewContact(contact);
        }
    }
    
    
    private void sendAutomaticMessage(
            Contact contact,
            String message) {

        if (contact == null) {
            return;
        }

        if (message == null || message.isBlank()) {
            return;
        }

        /*
         * Zona horaria oficial de la empresa.
         *
         * Esto evita depender de la zona horaria configurada
         * en el servidor donde esté desplegado Spring Boot.
         */
        LocalTime currentTime =
                LocalTime.now(
                        ZoneId.of("America/Bogota")
                );

        /*
         * Horario de atención:
         *
         * Lunes a viernes:
         * 07:00 incluido
         * 17:00 excluido
         *
         * Actualmente solamente estamos controlando la hora.
         */
        LocalTime startTime =
                LocalTime.of(7, 0);

        LocalTime endTime =
                LocalTime.of(17, 0);

        boolean withinBusinessHours =
                !currentTime.isBefore(startTime)
                        && currentTime.isBefore(endTime);

        /*
         * Dentro del horario:
         *
         * Enviamos directamente el mensaje correspondiente
         * al flujo actual.
         */
        if (withinBusinessHours) {

            this.whatsappResponseAutimatics.sendText(
                    contact.getPhone(),
                    message
            );

            return;
        }

        /*
         * Fuera del horario:
         *
         * No enviamos el mensaje específico del flujo.
         * En su lugar enviamos el mensaje general indicando
         * que la empresa está fuera de horario.
         */
        String outOfHoursMessage =
                "Gracias por comunicarte con Neumática Industrial.\n"
                .concat(
                        "En este momento nuestro equipo se encuentra fuera del horario de atención. "
                )
                .concat(
                        "Hemos recibido tu mensaje y uno de nuestros asesores te responderá a primera hora del siguiente día hábil.\n"
                )
                .concat(
                        "Agradecemos tu confianza."
                );

        this.whatsappResponseAutimatics.sendText(
                contact.getPhone(),
                outOfHoursMessage
        );
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
    @Transactional
    public void delete(UUID id) {

        /*
         * Buscamos el contacto junto con sus conversaciones y mensajes
         * para poder identificar los archivos multimedia asociados.
         */
        Contact contact =
                this.contactRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Contacto no encontrado."
                                )
                        );

        /*
         * Recorremos todas las conversaciones del contacto.
         */
        if (contact.getConversations() != null) {

            contact.getConversations()
                    .forEach(conversation -> {

                        /*
                         * Recorremos todos los mensajes de la conversación.
                         */
                        if (conversation.getMessages() != null) {

                            conversation.getMessages()
                                    .forEach(message -> {

                                        /*
                                         * Si el mensaje tiene un archivo
                                         * multimedia almacenado, lo eliminamos
                                         * físicamente del servidor.
                                         */
                                        if (message.getStoragePath() != null
                                                && !message.getStoragePath().isBlank()) {

                                            this.mediaStorageServiceImpl
                                                    .delete(
                                                            message.getStoragePath()
                                                    );
                                        }
                                    });
                        }
                    });
        }

        /*
         * Finalmente eliminamos el contacto.
         *
         * Debido al cascade configurado en Contact -> Conversation
         * y Conversation -> Message, las conversaciones y mensajes
         * asociados también serán eliminados de la base de datos.
         */
        this.contactRepository.delete(contact);
    }

}
