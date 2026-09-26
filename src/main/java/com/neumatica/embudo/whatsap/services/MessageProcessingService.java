package com.neumatica.embudo.whatsap.services;

import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.mapper.MessageMapper;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MediaStorageService;
import com.neumatica.embudo.whatsap.repository.MessageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Servicio encargado de procesar los mensajes recibidos
 * desde WhatsApp.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessingService {


    private final MessageMapper messageMapper;

    private final MediaStorageService mediaStorageService;

    private final MessageRepository messageRepository;

    private final ConversationRepository conversationRepository;


    /**
     * Procesa completamente un mensaje recibido
     * desde WhatsApp.
     */
    @Transactional
    public Message process(
            MessageDto dto,
            Conversation conversation
    ) {


        // =====================================================
        // 1. VALIDACIONES
        // =====================================================

        if (dto == null) {

            throw new IllegalArgumentException(
                    "El MessageDto no puede ser null."
            );
        }


        if (conversation == null) {

            throw new IllegalArgumentException(
                    "La conversación no puede ser null."
            );
        }


        // =====================================================
        // 2. EVITAR DUPLICADOS
        // =====================================================

        if (
                dto.getId() != null
                &&
                messageRepository
                        .existsByWhatsappMessageId(
                                dto.getId()
                        )
        ) {

            log.info(
                    "Mensaje ya procesado. whatsappMessageId={}",
                    dto.getId()
            );


            return messageRepository
                    .findByWhatsappMessageId(
                            dto.getId()
                    )
                    .orElse(null);
        }


        // =====================================================
        // 3. CONVERTIR DTO → ENTITY
        // =====================================================

        Message message =
                messageMapper.toEntity(
                        dto
                );


        // =====================================================
        // 4. MULTIMEDIA
        // =====================================================

        if (
                hasMedia(
                        message
                )
        ) {

            log.info(
                    "Mensaje multimedia detectado. mediaId={}, type={}",
                    message.getMediaId(),
                    message.getType()
            );


            String storagePath =
                    mediaStorageService
                            .downloadAndStore(
                                    message.getMediaId(),
                                    message.getMimeType(),
                                    message.getSha256()
                            );


            message.setStoragePath(
                    storagePath
            );
        }


        // =====================================================
        // 5. ASOCIAR MENSAJE A LA CONVERSACIÓN
        // =====================================================

        conversation.addMessage(
                message
        );


        // =====================================================
        // 6. GUARDAR MENSAJE
        // =====================================================

        Message savedMessage =
                messageRepository.save(
                        message
                );


        // =====================================================
        // 7. ACTUALIZAR ACTIVIDAD DE LA CONVERSACIÓN
        // =====================================================

        /*
         * La fecha utilizada para ordenar las conversaciones
         * debe provenir del mensaje realmente almacenado.
         */
        conversation.setLastMessageAt(
                savedMessage.getCreatedAt()
        );


        // =====================================================
        // 8. PERSISTIR CONVERSACIÓN
        // =====================================================

        conversationRepository.save(
                conversation
        );


        // =====================================================
        // 9. LOG
        // =====================================================

        log.info(
                "Mensaje guardado correctamente. " +
                "id={}, whatsappMessageId={}, conversationId={}, lastMessageAt={}",
                savedMessage.getId(),
                savedMessage.getWhatsappMessageId(),
                conversation.getId(),
                conversation.getLastMessageAt()
        );


        return savedMessage;
    }


    /**
     * Determina si el mensaje contiene multimedia.
     */
    private boolean hasMedia(
            Message message
    ) {

        return message.getMediaId() != null
                &&
                !message.getMediaId().isBlank();
    }

}