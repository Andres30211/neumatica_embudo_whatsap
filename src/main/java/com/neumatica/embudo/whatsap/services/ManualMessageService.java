package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

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

@Service
public class ManualMessageService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    private final ConversationRepository
            conversationRepository;

    private final MessageRepository
            messageRepository;

    private final WhatsappResponseAutimatics
            whatsappResponseAutimatics;

    private final UserClientService
            userClientService;

    private final NotificationService
            notificationService;

    private final FileStorageService
            fileStorageService;

    public ManualMessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WhatsappResponseAutimatics whatsappResponseAutimatics,
            UserClientService userClientService,
            NotificationService notificationService,
            FileStorageService fileStorageService
    ) {

        this.conversationRepository =
                conversationRepository;

        this.messageRepository =
                messageRepository;

        this.whatsappResponseAutimatics =
                whatsappResponseAutimatics;

        this.userClientService =
                userClientService;

        this.notificationService =
                notificationService;

        this.fileStorageService =
                fileStorageService;
    }

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

        validateBasicParameters(
                conversationId,
                userId,
                accessToken
        );

        if (
                file == null
                        || file.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "El archivo es obligatorio."
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

        /*
         * --------------------------------------------------------
         * 1. GUARDAMOS EL ARCHIVO EN NUESTRO SISTEMA
         * --------------------------------------------------------
         */

        StoredFile storedFile =
                fileStorageService.store(
                        file,
                        conversationId
                );

        try {

            /*
             * ----------------------------------------------------
             * 2. DETERMINAMOS EL TIPO
             * ----------------------------------------------------
             */

            String mimeType =
                    storedFile.getMimeType();

            MessageType messageType;

            if (
                    mimeType != null
                            && mimeType
                            .toLowerCase()
                            .startsWith("image/")
            ) {

                messageType =
                        MessageType.IMAGE;

            } else {

                /*
                 * PDF, Word, Excel, ZIP, etc.
                 * se manejarán como DOCUMENT.
                 */

                messageType =
                        MessageType.DOCUMENT;
            }

            /*
             * ----------------------------------------------------
             * 3. SUBIMOS EL ARCHIVO A WHATSAPP
             * ----------------------------------------------------
             */

            String mediaId =
                    whatsappResponseAutimatics
                            .uploadMedia(file);

            /*
             * ----------------------------------------------------
             * 4. ENVIAMOS EL MEDIA AL CONTACTO
             * ----------------------------------------------------
             */

            WhatsappSendResult sendResult;

            if (
                    messageType
                            == MessageType.IMAGE
            ) {

                sendResult =
                        whatsappResponseAutimatics
                                .sendImage(
                                        contact.getPhone(),
                                        mediaId,
                                        caption
                                );

            } else {

                sendResult =
                        whatsappResponseAutimatics
                                .sendDocument(
                                        contact.getPhone(),
                                        mediaId,
                                        caption,
                                        storedFile
                                                .getOriginalFileName()
                                );
            }

            /*
             * ----------------------------------------------------
             * 5. CREAMOS EL MENSAJE
             * ----------------------------------------------------
             */

            LocalDateTime now =
                    LocalDateTime.now(
                            ZONE_ID
                    );

            Message message =
                    Message.builder()

                            .whatsappMessageId(
                                    sendResult
                                            .getMessageId()
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
                                    messageType
                            )

                            .body(
                                    null
                            )

                            .mediaId(
                                    mediaId
                            )

                            .storagePath(
                                    storedFile
                                            .getStoragePath()
                            )

                            .mimeType(
                                    storedFile
                                            .getMimeType()
                            )

                            .sha256(
                                    storedFile
                                            .getSha256()
                            )

                            .caption(
                                    caption
                            )

                            .fileName(
                                    storedFile
                                            .getOriginalFileName()
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

            /*
             * ----------------------------------------------------
             * 6. AGREGAMOS EL MENSAJE A LA CONVERSACIÓN
             * ----------------------------------------------------
             */

            conversation.addMessage(
                    message
            );

            conversation.setLastMessageAt(
                    now
            );

            /*
             * ----------------------------------------------------
             * 7. GUARDAMOS
             * ----------------------------------------------------
             */

            conversationRepository.save(
                    conversation
            );

            /*
             * ----------------------------------------------------
             * 8. WEBSOCKET
             * ----------------------------------------------------
             */

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

        } catch (RuntimeException e) {

            /*
             * Si WhatsApp falla, eliminamos el archivo
             * que acabamos de guardar localmente.
             */

            fileStorageService.delete(
                    storedFile.getStoragePath()
            );

            throw e;
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