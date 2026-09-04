package com.neumatica.embudo.whatsap.services;

import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.mapper.MessageMapper;
import com.neumatica.embudo.whatsap.repository.MediaStorageService;
import com.neumatica.embudo.whatsap.repository.MessageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
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

    /*
     * Procesa completamente un mensaje recibido.
     *
     * <p>El flujo es:
     *
     * <ol>
     *     <li>Validar el mensaje.</li>
     *     <li>Evitar duplicados.</li>
     *     <li>Convertir DTO a entidad.</li>
     *     <li>Detectar multimedia.</li>
     *     <li>Descargar y almacenar multimedia.</li>
     *     <li>Asociar el mensaje a la conversación.</li>
     *     <li>Persistir el mensaje.</li>
     * </ol>
     */
    @Transactional
    public Message process(
            MessageDto dto,
            Conversation conversation) {

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

        /*
         * Evitamos procesar nuevamente un mensaje
         * que ya fue almacenado.
         */
        if (dto.getId() != null
                && messageRepository
                        .existsByWhatsappMessageId(
                                dto.getId()
                        )) {

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

        /*
         * Convertimos el DTO recibido desde WhatsApp
         * en nuestra entidad.
         */
        Message message =
                messageMapper.toEntity(
                        dto
                );

        /*
         * Determinamos si existe multimedia.
         */
        if (hasMedia(message)) {

            log.info(
                    "Mensaje multimedia detectado. mediaId={}, type={}",
                    message.getMediaId(),
                    message.getType()
            );

            /*
             * Descargamos el archivo y obtenemos
             * la ruta lógica de almacenamiento.
             */
            String storagePath =
                    mediaStorageService.downloadAndStore(
                            message.getMediaId(),
                            message.getMimeType(),
                            message.getSha256()
                    );

            /*
             * Guardamos solamente la referencia
             * al archivo físico.
             */
            message.setStoragePath(
                    storagePath
            );
        }

        /*
         * Asociamos el mensaje a la conversación.
         *
         * addMessage() configura también:
         *
         * message.setConversation(this)
         */
        conversation.addMessage(
                message
        );

        /*
         * Persistimos el mensaje.
         */
        Message savedMessage =
                messageRepository.save(
                        message
                );

        log.info(
                "Mensaje guardado correctamente. id={}, whatsappMessageId={}",
                savedMessage.getId(),
                savedMessage.getWhatsappMessageId()
        );

        return savedMessage;
    }

    /**
     * Determina si el mensaje contiene multimedia.
     */
    private boolean hasMedia(
            Message message) {

        return message.getMediaId() != null
                && !message.getMediaId().isBlank();
    }
}