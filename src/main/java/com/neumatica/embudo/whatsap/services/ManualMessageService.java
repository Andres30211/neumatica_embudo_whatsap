package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.neumatica.embudo.whatsap.dto.media.StoredFile;
import com.neumatica.embudo.whatsap.dto.media.WhatsappSendResult;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;
import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;
import com.neumatica.embudo.whatsap.websocket.NotificationService;

import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class ManualMessageService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Bogota");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private WhatsappResponseAutimatics whatsappResponseAutimatics;

    @Autowired
    private UserClientService userClientService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private AudioConversionService audioConversionService;

    public ManualMessageService() {}

    // ============================================================
    // ENVIAR MENSAJE DE TEXTO
    // ============================================================

    @Transactional
    public Message sendMessage(
            UUID conversationId,
            UUID userId,
            String text,
            String accessToken
    ) {

        validateBasicParameters(
                conversationId,
                userId,
                accessToken
        );

        if (text == null || text.isBlank()) {

            throw new IllegalArgumentException(
                    "El mensaje no puede estar vacío."
            );
        }

        validateUser(
                userId,
                accessToken
        );

        Conversation conversation =
                getConversation(
                        conversationId
                );

        validateConversationForManualMessage(
                conversation,
                userId
        );

        Contact contact =
                getContact(conversation);

        validatePhone(contact);

        WhatsappSendResult sendResult =
                whatsappResponseAutimatics.sendText(
                        contact.getPhone(),
                        text
                );

        LocalDateTime now =
                LocalDateTime.now(
                        ZONE_ID
                );

        Message message =
                Message.builder()

                        .whatsappMessageId(
                                sendResult.getMessageId()
                        )

                        .conversation(
                                conversation
                        )

                        .direction(
                                Direction.OUTGOING
                        )

                        .senderUserId(
                                userId
                        )

                        .type(
                                MessageType.TEXT
                        )

                        .body(
                                text
                        )

                        .createdAt(
                                now
                        )

                        .whatsappTimestamp(
                                now.atZone(
                                        ZONE_ID
                                ).toEpochSecond()
                        )

                        .build();

        conversation.addMessage(
                message
        );

        conversation.setLastMessageAt(
                now
        );

        conversationRepository.save(
                conversation
        );

        notificationService
                .sendConversationMessageAfterCommit(
                        conversation.getId(),
                        message
                );

        notificationService
                .sendConversationSummaryAfterCommit(
                        conversation,
                        message
                );

        return message;
    }

    // ============================================================
    // ENVIAR ARCHIVO / MULTIMEDIA
    // ============================================================

    
	@Transactional
	public Message sendMedia(
	        UUID conversationId,
	        UUID userId,
	        MultipartFile file,
	        String caption,
	        String accessToken
	) {
	
	    validateBasicParameters(conversationId, userId, accessToken);
	
	    if (file == null || file.isEmpty()) {
	        throw new IllegalArgumentException(
	                "El archivo es obligatorio."
	        );
	    }
	
	    validateUser(userId, accessToken);
	
	    Conversation conversation = getConversation(conversationId);
	
	    validateConversationForManualMessage(conversation, userId);
	
	    Contact contact = getContact(conversation);
	
	    validatePhone(contact);
	
	    Path inputAudio = null;
	    Path convertedAudio = null;
	    StoredFile storedFile = null;
	
	    try {
	        MultipartFile fileToProcess = file;
	
	        String contentType = file.getContentType() == null
	                ? ""
	                : file.getContentType().toLowerCase(Locale.ROOT);
	
	        String originalName = file.getOriginalFilename() == null
	                ? ""
	                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
	
	        boolean isWebmAudio =
	                contentType.contains("webm")
	                || originalName.endsWith(".webm");
	
	        // 1. Convertir WebM a OGG/Opus cuando sea necesario.
	        if (isWebmAudio) {
	            inputAudio = Files.createTempFile(
	                    "incoming-audio-", ".webm"
	            );
	
	            file.transferTo(inputAudio);
	
	            convertedAudio =
	                    audioConversionService.convertWebmToOggOpus(
	                            inputAudio
	                    );
	
	            fileToProcess = new ConvertedAudioMultipartFile(
	                    convertedAudio,
	                    "audio.ogg"
	            );
	        }
	
	        // 2. Guardar el archivo que realmente vamos a enviar.
	        storedFile = fileStorageService.store(
	                fileToProcess,
	                conversationId
	        );
	
	        String mimeType = storedFile.getMimeType();
	
	        MessageType messageType;
	
	        if (mimeType != null
	                && mimeType.toLowerCase(Locale.ROOT)
	                        .startsWith("image/")) {
	
	            messageType = MessageType.IMAGE;
	
	        } else if (mimeType != null
	                && mimeType.toLowerCase(Locale.ROOT)
	                        .startsWith("audio/")) {
	
	            messageType = MessageType.AUDIO;
	
	        } else {
	            messageType = MessageType.DOCUMENT;
	        }
	
	        // 3. Subir el archivo a WhatsApp.
	        String mediaId;
	
	        if (messageType == MessageType.AUDIO) {
	            Path audioToUpload = convertedAudio;
	
	            if (audioToUpload != null) {
	                mediaId = whatsappResponseAutimatics.uploadAudio(
	                        audioToUpload,
	                        "audio.ogg",
	                        "audio/ogg"
	                );
	            } else {
	                mediaId = whatsappResponseAutimatics.uploadMedia(
	                        fileToProcess
	                );
	            }
	        } else {
	            mediaId = whatsappResponseAutimatics.uploadMedia(
	                    fileToProcess
	            );
	        }
	
	        // 4. Enviar según el tipo de archivo.
	        WhatsappSendResult sendResult;
	
	        if (messageType == MessageType.IMAGE) {
	
	            sendResult = whatsappResponseAutimatics.sendImage(
	                    contact.getPhone(),
	                    mediaId,
	                    caption
	            );
	
	        } else if (messageType == MessageType.AUDIO) {
	
	            sendResult = whatsappResponseAutimatics.sendAudio(
	                    contact.getPhone(),
	                    mediaId
	            );
	
	        } else {
	
	            sendResult = whatsappResponseAutimatics.sendDocument(
	                    contact.getPhone(),
	                    mediaId,
	                    caption,
	                    storedFile.getOriginalFileName()
	            );
	        }
	
	        // 5. Crear el registro del mensaje.
	        LocalDateTime now = LocalDateTime.now(ZONE_ID);
	
	        Message message = Message.builder()
	                .whatsappMessageId(sendResult.getMessageId())
	                .conversation(conversation)
	                .direction(Direction.OUTGOING)
	                .senderUserId(userId)
	                .type(messageType)
	                .body(null)
	                .mediaId(mediaId)
	                .storagePath(storedFile.getStoragePath())
	                .mimeType(storedFile.getMimeType())
	                .sha256(storedFile.getSha256())
	                .caption(caption)
	                .fileName(storedFile.getOriginalFileName())
	                .createdAt(now)
	                .whatsappTimestamp(
	                        now.atZone(ZONE_ID).toEpochSecond()
	                )
	                .build();
	
	        // 6. Actualizar la conversación.
	        conversation.addMessage(message);
	        conversation.setLastMessageAt(now);
	
	        conversationRepository.save(conversation);
	
	        // 7. Notificar por WebSocket.
	        notificationService.sendConversationMessageAfterCommit(
	                conversation.getId(),
	                message
	        );
	
	        notificationService.sendConversationSummaryAfterCommit(
	                conversation,
	                message
	        );
	
	        return message;
	
	    } catch (IOException e) {
	
	        deleteStoredFile(storedFile);
	
	        throw new RuntimeException(
	                "Error procesando o convirtiendo el archivo multimedia.",
	                e
	        );
	
	    } catch (RuntimeException e) {
	
	        deleteStoredFile(storedFile);
	
	        throw e;
	
	    } finally {
	
	        try {
	            if (inputAudio != null) {
	                Files.deleteIfExists(inputAudio);
	            }
	
	            if (convertedAudio != null) {
	                Files.deleteIfExists(convertedAudio);
	            }
	        } catch (IOException e) {
	            System.err.println(
	                    "No se pudieron eliminar los archivos temporales: "
	                            + e.getMessage()
	            );
	        }
	    }
	}
	

	private void deleteStoredFile(StoredFile storedFile) {
	    if (storedFile != null
	            && storedFile.getStoragePath() != null) {
	        try {
	            fileStorageService.delete(
	                    storedFile.getStoragePath()
	            );
	        } catch (RuntimeException e) {
	            System.err.println(
	                    "No se pudo eliminar el archivo almacenado: "
	                            + e.getMessage()
	            );
	        }
	    }
	}



    // ============================================================
    // VALIDACIONES
    // ============================================================

    private void validateBasicParameters(
            UUID conversationId,
            UUID userId,
            String accessToken
    ) {

        if (conversationId == null) {

            throw new IllegalArgumentException(
                    "El conversationId es obligatorio."
            );
        }

        if (userId == null) {

            throw new IllegalArgumentException(
                    "El userId es obligatorio."
            );
        }

        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "El accessToken es obligatorio."
            );
        }
    }

    private void validateUser(
            UUID userId,
            String accessToken
    ) {

        var user =
                userClientService.findById(
                        userId,
                        accessToken
                );

        if (user == null) {

            throw new RuntimeException(
                    "Usuario no encontrado."
            );
        }
    }

    private Conversation getConversation(
            UUID conversationId
    ) {

        return conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Conversación no encontrada."
                        )
                );
    }

    private void validateConversationForManualMessage(
            Conversation conversation,
            UUID userId
    ) {

        if (
                conversation.getStatus()
                        != ConversationStatus.HUMAN
        ) {

            throw new RuntimeException(
                    "La conversación no está en atención humana."
            );
        }

        if (
                !userId.equals(
                        conversation
                                .getAssignedUserId()
                )
        ) {

            throw new RuntimeException(
                    "La conversación no está asignada a este vendedor."
            );
        }
    }

    private Contact getContact(
            Conversation conversation
    ) {

        Contact contact =
                conversation.getContact();

        if (contact == null) {

            throw new RuntimeException(
                    "La conversación no tiene contacto."
            );
        }

        return contact;
    }

    private void validatePhone(
            Contact contact
    ) {

        if (
                contact.getPhone() == null
                        || contact.getPhone().isBlank()
        ) {

            throw new RuntimeException(
                    "El contacto no tiene número de WhatsApp."
            );
        }
    }
}