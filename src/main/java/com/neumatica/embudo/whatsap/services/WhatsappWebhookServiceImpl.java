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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.brevo.EmailRequestDto;
import com.neumatica.embudo.whatsap.dto.contact.ConversationSummaryResponse;
import com.neumatica.embudo.whatsap.dto.user.UserResponseDto;
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
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;
import com.neumatica.embudo.whatsap.enums.RegistrationStep;
import com.neumatica.embudo.whatsap.interfaces.ConversationSummaryProjection;
import com.neumatica.embudo.whatsap.mapper.ContactMapper;
import com.neumatica.embudo.whatsap.mapper.MessageMapper;
import com.neumatica.embudo.whatsap.repository.ContactRepository;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;
import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;
import com.neumatica.embudo.whatsap.repository.WhatsappWebhookService;
import com.neumatica.embudo.whatsap.websocket.NotificationService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	
	@Autowired
	private UserClientService userClientService;

    WhatsappWebhookServiceImpl(MediaStorageServiceImpl mediaStorageServiceImpl) {
        this.mediaStorageServiceImpl = mediaStorageServiceImpl;
    }
	
    @Override
    //@Transactional(readOnly = true)
    public Page<ConversationSummaryResponse> contacts(
            int page,
            String accessToken
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        5
                );

        Page<ConversationSummaryProjection> conversationPage =
                this.conversationRepository
                        .findConversationSummaries(
                                pageable
                        );

        return conversationPage.map(conversation -> {

            UUID assignedUserId =
                    conversation.getAssignedUserId();

            String assignedUserName = null;

            /*
             * Si existe un vendedor asignado,
             * consultamos su información en el
             * microservicio de seguridad.
             */
            if (assignedUserId != null) {

                try {

                    UserResponseDto user =
                            userClientService.findById(
                                    assignedUserId,
                                    accessToken
                            );

                    if (user != null) {

                        assignedUserName =
                                user.getName();
                    }

                } catch (Exception exception) {

                    /*
                     * Si temporalmente el servicio de usuarios
                     * no responde, no dejamos caer toda la lista
                     * de conversaciones.
                     */
                    log.warn(
                            "No fue posible obtener el vendedor asignado. userId={}",
                            assignedUserId,
                            exception
                    );
                }
            }

            return ConversationSummaryResponse
                    .builder()

                    .conversationId(
                            conversation.getConversationId()
                    )

                    .contactId(
                            conversation.getContactId()
                    )

                    .contactName(
                            conversation.getContactName()
                    )

                    .phone(
                            conversation.getPhone()
                    )

                    .registrationStep(
                            conversation.getRegistrationStep()
                    )

                    .lastMessage(
                            conversation.getLastMessage()
                    )

                    .lastMessageType(
                            conversation.getLastMessageType()
                    )

                    .lastMessageAt(
                            conversation.getLastMessageAt()
                    )

                    .status(
                            conversation.getStatus()
                    )

                    .assignedUserId(
                            assignedUserId
                    )

                    .assignedUserName(
                            assignedUserName
                    )

                    .build();
        });
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
     * El mensaje entrante ya fue procesado y persistido por
     * MessageProcessingService. Lo publicamos para TODOS los estados
     * (BOT y HUMAN).
     */
    Message realtimeMessage =
            this.messageRepository
                    .findByWhatsappMessageId(
                            messageDTO.getId()
                    )
                    .orElse(null);

    if (realtimeMessage != null) {

        this.notificationService
                .sendConversationMessageAfterCommit(
                        conversation.getId(),
                        realtimeMessage
                );

        this.notificationService
                .sendConversationSummaryAfterCommit(
                        conversation,
                        realtimeMessage
                );
    }

    this.notificationService.sendNotification(contact);

    /*
     * Si la conversación está en atención humana, no ejecutamos el bot.
     */
    if (conversation.getStatus() == ConversationStatus.HUMAN) {
        return;
    }

    /*
     * Si continúa en BOT, ejecutamos la automatización.
     */
    processBusinessFlow(
            contact,
            conversation,
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
	        Conversation conversation,
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

	        addNoPhoneObservation(contact, messageDTO, conversation);

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
	            	    conversation,
	            	    "Bienvenido a Neumática Industrial S.A.S.\n\n"
	            	        + "Especialistas en automatización, neumática y aire comprimido.\n\n"
	            	        + "Para brindarte una atención más ágil, por favor indícanos:\n\n"
	            	        + "📧 Tu correo electrónico\n"
	            	        + "🏢 Nombre de la empresa\n\n"
	            	        + "Puedes enviarnos los datos juntos."
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
                    conversation,
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
							conversation,

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
	        Contact contact,
	        MessageDto messageDTO,
	        Conversation conversation) {

	    /*
	     * Esta función solamente debe ejecutarse cuando el contacto
	     * realmente no tiene un número de teléfono.
	     */
	    if (contact == null
	            || contact.getPhone() == null
	            || contact.getPhone().isBlank()) {

	        /*
	         * Necesitamos el ID del mensaje únicamente para localizar
	         * el mensaje que ya fue guardado.
	         *
	         * La existencia del mensaje NO determina si el contacto
	         * tiene teléfono.
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
	         * Si el mensaje original es de texto, agregamos
	         * la observación al contenido existente.
	         */
	        if (message.getBody() != null
	                && !message.getBody().isBlank()) {

	            message.setBody(
	                    message.getBody()
	                            + "\n\n"
	                            + observation
	            );

	        /*
	         * Si es multimedia y tiene caption, agregamos
	         * la observación al caption.
	         */
	        } else if (message.getCaption() != null
	                && !message.getCaption().isBlank()) {

	            message.setCaption(
	                    message.getCaption()
	                            + "\n\n"
	                            + observation
	            );

	        /*
	         * Si es multimedia pero no tiene caption,
	         * utilizamos el caption para almacenar la observación.
	         */
	        } else {

	            message.setCaption(observation);
	        }

	        /*
	         * Persistimos nuevamente el mensaje.
	         */
	        this.messageRepository.save(message);

	        /*
	         * Actualizamos tiempo real solamente si existe
	         * una conversación.
	         */
	        if (conversation != null) {

	            this.notificationService.sendConversationMessageAfterCommit(
	                    conversation.getId(),
	                    message
	            );

	            this.notificationService.sendConversationSummaryAfterCommit(
	                    conversation,
	                    message
	            );
	        }
	    }
	}


	
	private Contact getOrCreateContact(
	        ContactDto dto,
	        MessageDto messageDTO) {

	    /*
	     * ============================================================
	     * 1. OBTENER EL TELÉFONO / WHATSAPP ID
	     * ============================================================
	     *
	     * WhatsApp puede enviar el identificador en:
	     *
	     *     contacts[].wa_id
	     *
	     * pero en nuestros webhooks hemos encontrado casos donde
	     * ese valor viene null y el número real está en:
	     *
	     *     messages[].from
	     *
	     * Por eso utilizamos from como respaldo.
	     */

	    String phone = null;

	    if (dto != null
	            && dto.getWaId() != null
	            && !dto.getWaId().isBlank()) {

	        phone = dto.getWaId();

	    } else if (messageDTO != null
	            && messageDTO.getFrom() != null
	            && !messageDTO.getFrom().isBlank()) {

	        phone = messageDTO.getFrom();
	    }


	    /*
	     * ============================================================
	     * 2. OBTENER META USER ID
	     * ============================================================
	     */

	    String metaUserId = null;

	    if (dto != null
	            && dto.getUserId() != null
	            && !dto.getUserId().isBlank()) {

	        metaUserId = dto.getUserId();

	    } else if (messageDTO != null
	            && messageDTO.getFromUserId() != null
	            && !messageDTO.getFromUserId().isBlank()) {

	        metaUserId = messageDTO.getFromUserId();
	    }


	    /*
	     * ============================================================
	     * 3. BUSCAR POR TELÉFONO
	     * ============================================================
	     *
	     * El teléfono / WhatsApp ID es el identificador principal
	     * que utilizaremos para evitar contactos duplicados.
	     */

	    if (phone != null && !phone.isBlank()) {

	        var existingContact =
	                this.contactRepository.findByPhone(phone);

	        if (existingContact.isPresent()) {

	            Contact contact = existingContact.get();

	            /*
	             * Actualizamos información disponible del contacto.
	             */

	            if (dto != null
	                    && dto.getProfile() != null
	                    && dto.getProfile().getName() != null
	                    && !dto.getProfile().getName().isBlank()) {

	                contact.setName(
	                        dto.getProfile().getName()
	                );
	            }

	            /*
	             * Si anteriormente no tenía Meta User ID,
	             * aprovechamos el que acaba de llegar.
	             */

	            if ((contact.getMetaUserId() == null
	                    || contact.getMetaUserId().isBlank())
	                    && metaUserId != null
	                    && !metaUserId.isBlank()) {

	                contact.setMetaUserId(
	                        metaUserId
	                );
	            }

	            /*
	             * Actualizamos la última interacción.
	             */

	            contact.setLastInteraction(
	                    LocalDateTime.now(
	                            ZoneId.of("America/Bogota")
	                    )
	            );

	            return this.contactRepository.save(
	                    contact
	            );
	        }
	    }


	    /*
	     * ============================================================
	     * 4. BUSCAR POR META USER ID
	     * ============================================================
	     */

	    if (metaUserId != null
	            && !metaUserId.isBlank()) {

	        var existingContact =
	                this.contactRepository.findByMetaUserId(
	                        metaUserId
	                );

	        if (existingContact.isPresent()) {

	            Contact contact = existingContact.get();

	            /*
	             * Si el contacto anteriormente no tenía teléfono,
	             * ahora podemos completarlo utilizando messageDTO.from.
	             */

	            if ((contact.getPhone() == null
	                    || contact.getPhone().isBlank())
	                    && phone != null
	                    && !phone.isBlank()) {

	                contact.setPhone(
	                        phone
	                );
	            }

	            /*
	             * Actualizamos el nombre si WhatsApp lo proporciona.
	             */

	            if (dto != null
	                    && dto.getProfile() != null
	                    && dto.getProfile().getName() != null
	                    && !dto.getProfile().getName().isBlank()) {

	                contact.setName(
	                        dto.getProfile().getName()
	                );
	            }

	            /*
	             * Actualizamos la última interacción.
	             */

	            contact.setLastInteraction(
	                    LocalDateTime.now(
	                            ZoneId.of("America/Bogota")
	                    )
	            );

	            return this.contactRepository.save(
	                    contact
	            );
	        }
	    }


	    /*
	     * ============================================================
	     * 5. NO EXISTE → CREAR CONTACTO
	     * ============================================================
	     *
	     * En este punto ya sabemos que no encontramos un contacto
	     * existente por teléfono ni por Meta User ID.
	     */

	    return createContact(
	            dto,
	            phone,
	            metaUserId
	    );
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
	private Contact createContact(
	        ContactDto contactDto,
	        String phone,
	        String metaUserId) {

	    /*
	     * ============================================================
	     * VALIDACIÓN
	     * ============================================================
	     */

	    if (contactDto == null) {

	        throw new IllegalArgumentException(
	                "No es posible crear un contacto sin ContactDto."
	        );
	    }


	    /*
	     * ============================================================
	     * OBTENER NOMBRE
	     * ============================================================
	     */

	    String name = null;

	    if (contactDto.getProfile() != null
	            && contactDto.getProfile().getName() != null
	            && !contactDto.getProfile().getName().isBlank()) {

	        name = contactDto.getProfile().getName();
	    }


	    /*
	     * ============================================================
	     * FECHA ACTUAL
	     * ============================================================
	     */

	    LocalDateTime now =
	            LocalDateTime.now(
	                    ZoneId.of("America/Bogota")
	            );


	    /*
	     * ============================================================
	     * CREAR CONTACTO
	     * ============================================================
	     */

	    Contact contact =
	            Contact.builder()
	                    .phone(phone)
	                    .metaUserId(metaUserId)
	                    .name(name)
	                    .registrationStep(
	                            RegistrationStep.GREETING
	                    )
	                    .firstContact(now)
	                    .lastInteraction(now)
	                    .createdAt(now)
	                    .build();


	    /*
	     * ============================================================
	     * GUARDAR
	     * ============================================================
	     */

	    return this.contactRepository.save(
	            contact
	    );
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

    public void processEmailAndCompany(
            Contact contact,
            Conversation conversation,
            MessageDto messageDTO
    ) {

        if (contact == null || messageDTO == null) {
            return;
        }

        /*
         * Esta etapa espera:
         *
         * correo electrónico
         * nombre de la empresa
         *
         * Se aceptan ambos órdenes:
         *
         * correo + empresa
         * empresa + correo
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
                "^\\s*(?:"
                        + "(?<email>[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})"
                        + "[\\s,;:\\-]+"
                        + "(?<company>.+?)"
                        + "|"
                        + "(?<companyFirst>.+?)"
                        + "[\\s,;:\\-]+"
                        + "(?<emailSecond>[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})"
                        + ")\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher =
                pattern.matcher(
                        messageDTO.getText().getBody()
                );

        if (!matcher.matches()) {
            return;
        }

        String email;
        String company;

        if (matcher.group("email") != null) {

            email =
                    matcher.group("email").trim();

            company =
                    matcher.group("company").trim();

        } else {

            email =
                    matcher.group("emailSecond").trim();

            company =
                    matcher.group("companyFirst").trim();
        }

        contact.setEmail(email);

        contact.setCompany(company);

        contact.setRegistrationStep(
                RegistrationStep.COMPLETED
        );

        /*
         * Guardamos primero la información
         * capturada del contacto.
         */
        contactRepository.save(contact);

        sendAutomaticMessage(
                contact,
                conversation,
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
                        email,
                        contact.getName(),
                        company
                );

        try {

            this.brevoEmailServices.sendEmail(
                    emailRequestDto,
                    4L
            );

        } catch (Exception e) {

            e.printStackTrace();
        }

        this.notificationService.sendNewContact(
                contact
        );
    }
    
    
    private void sendAutomaticMessage(
            Contact contact,
            Conversation conversation,
            String message) {

        if (contact == null || conversation == null
                || message == null || message.isBlank()) {
            return;
        }

        if (conversation.getStatus() != ConversationStatus.BOT) {
            return;
        }

        if (contact.getPhone() == null || contact.getPhone().isBlank()) {
            return;
        }

        LocalTime currentTime =
                LocalTime.now(ZoneId.of("America/Bogota"));

        LocalTime startTime = LocalTime.of(7, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        boolean withinBusinessHours =
                !currentTime.isBefore(startTime)
                        && currentTime.isBefore(endTime);

        String messageToSend = message;

        if (!withinBusinessHours) {
            messageToSend =
                    "Gracias por comunicarte con Neumática Industrial.\n"
                    + "En este momento nuestro equipo se encuentra fuera del horario de atención. "
                    + "Hemos recibido tu mensaje y uno de nuestros asesores te responderá a primera hora del siguiente día hábil.\n"
                    + "Agradecemos tu confianza.";
        }

        this.whatsappResponseAutimatics.sendText(
                contact.getPhone(),
                messageToSend
        );

        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("America/Bogota"));

        Message automaticMessage =
                Message.builder()
                        .whatsappMessageId(null)
                        .conversation(conversation)
                        .direction(Direction.OUTGOING)
                        .senderUserId(null)
                        .type(MessageType.TEXT)
                        .body(messageToSend)
                        .createdAt(now)
                        .whatsappTimestamp(
                                now.atZone(ZoneId.of("America/Bogota"))
                                        .toEpochSecond()
                        )
                        .build();

        conversation.addMessage(automaticMessage);
        conversation.setLastMessageAt(now);
        this.conversationRepository.save(conversation);

        this.notificationService.sendConversationMessageAfterCommit(
                conversation.getId(),
                automaticMessage
        );

        this.notificationService.sendConversationSummaryAfterCommit(
                conversation,
                automaticMessage
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
         * ============================================================
         * 1. BUSCAR EL CONTACTO
         * ============================================================
         */

        Contact contact = this.contactRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Contacto no encontrado."
                        )
                );


        /*
         * ============================================================
         * 2. OBTENER LAS CONVERSACIONES
         * ============================================================
         *
         * Creamos una copia de la colección para evitar problemas
         * si Hibernate modifica la colección durante la eliminación.
         */

        List<Conversation> conversations =
                contact.getConversations() == null
                        ? List.of()
                        : List.copyOf(
                                contact.getConversations()
                        );


        /*
         * ============================================================
         * 3. RECORRER LAS CONVERSACIONES
         * ============================================================
         */

        for (Conversation conversation : conversations) {

            if (conversation == null) {
                continue;
            }


            /*
             * ========================================================
             * 4. OBTENER LOS MENSAJES
             * ========================================================
             */

            List<Message> messages =
                    conversation.getMessages() == null
                            ? List.of()
                            : List.copyOf(
                                    conversation.getMessages()
                            );


            /*
             * ========================================================
             * 5. ELIMINAR ARCHIVOS MULTIMEDIA FÍSICOS
             * ========================================================
             *
             * La base de datos solamente guarda la referencia
             * (storagePath).
             *
             * Por eso debemos eliminar también el archivo físico.
             */

            for (Message message : messages) {

                if (message == null) {
                    continue;
                }

                String storagePath =
                        message.getStoragePath();


                if (storagePath != null
                        && !storagePath.isBlank()) {

                    try {

                        this.mediaStorageServiceImpl.delete(
                                storagePath
                        );

                    } catch (Exception e) {

                        /*
                         * Si el archivo físico ya no existe
                         * o Storage presenta algún problema,
                         * no detenemos la eliminación de la BD.
                         */

                        System.err.println(
                                "No fue posible eliminar el archivo multimedia: "
                                        + storagePath
                        );

                        System.err.println(
                                "Detalle: "
                                        + e.getMessage()
                        );
                    }
                }
            }


            /*
             * ========================================================
             * 6. ELIMINAR MENSAJES DE LA BASE DE DATOS
             * ========================================================
             *
             * Se eliminan primero los mensajes porque dependen
             * de la conversación.
             */

            if (!messages.isEmpty()) {

                this.messageRepository.deleteAll(
                        messages
                );
            }


            /*
             * ========================================================
             * 7. ELIMINAR LA CONVERSACIÓN
             * ========================================================
             */

            this.conversationRepository.delete(
                    conversation
            );
        }


        /*
         * ============================================================
         * 8. ELIMINAR EL CONTACTO
         * ============================================================
         *
         * En este punto:
         *
         * Contacto
         *    └── Conversaciones
         *          └── Mensajes
         *
         * ya fueron procesados.
         */

        this.contactRepository.delete(
                contact
        );


        /*
         * ============================================================
         * 9. FORZAR LA EJECUCIÓN DE LAS OPERACIONES
         * ============================================================
         *
         * Esto permite detectar inmediatamente cualquier problema
         * de integridad referencial antes de finalizar la transacción.
         */

        this.contactRepository.flush();
    }

}
