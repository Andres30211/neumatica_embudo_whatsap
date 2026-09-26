package com.neumatica.embudo.whatsap.mapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;

/*
 * Convierte mensajes provenientes del webhook de WhatsApp
 * a entidades de dominio.
 */
@Component
public class MessageMapper {

    /*
     * Convierte un MessageDto en Message.
     */
	private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    public Message toEntity(
            MessageDto dto) {

        Message message =
                Message.builder()

                        .whatsappMessageId(
                                dto.getId()
                        )

                        .direction(
                                Direction.INCOMING
                        )

                        .createdAt(
                                LocalDateTime.now(
                                        ZONE_ID
                                )
                        )

                        .whatsappTimestamp(
                                parseTimestamp(
                                        dto.getTimestamp()
                                )
                        )

                        .build();

        /*
         * Determinamos el tipo de mensaje.
         */
        if (dto.getType() == null
                || dto.getType().isBlank()) {

            throw new IllegalArgumentException(
                    "El mensaje no contiene type."
            );
        }

        MessageType type =
                MessageType.valueOf(
                        dto.getType()
                                .toUpperCase()
                );

        message.setType(type);

        /*
         * Procesamos el contenido dependiendo
         * del tipo.
         */
        switch (type) {

            case TEXT ->
                    mapText(
                            message,
                            dto
                    );

            case IMAGE ->
                    mapImage(
                            message,
                            dto
                    );

            case VIDEO ->
                    mapVideo(
                            message,
                            dto
                    );

            case AUDIO ->
                    mapAudio(
                            message,
                            dto
                    );

            case DOCUMENT ->
                    mapDocument(
                            message,
                            dto
                    );

            case STICKER ->
                    mapSticker(
                            message,
                            dto
                    );

            default -> {
                /*
                 * Los tipos adicionales se podrán
                 * implementar posteriormente.
                 */
            }
        }

        return message;
    }

    private void mapText(
            Message message,
            MessageDto dto) {

        if (dto.getText() == null) {
            return;
        }

        message.setBody(
                dto.getText().getBody()
        );
    }

    private void mapImage(
            Message message,
            MessageDto dto) {

        if (dto.getImage() == null) {
            return;
        }

        message.setMediaId(
                dto.getImage().getId()
        );

        message.setMimeType(
                dto.getImage().getMimeType()
        );

        message.setSha256(
                dto.getImage().getSha256()
        );

        message.setCaption(
                dto.getImage().getCaption()
        );
    }

    private void mapVideo(
            Message message,
            MessageDto dto) {

        if (dto.getVideo() == null) {
            return;
        }

        message.setMediaId(
                dto.getVideo().getId()
        );

        message.setMimeType(
                dto.getVideo().getMimeType()
        );

        message.setSha256(
                dto.getVideo().getSha256()
        );

        message.setCaption(
                dto.getVideo().getCaption()
        );
    }

    private void mapAudio(
            Message message,
            MessageDto dto) {

        if (dto.getAudio() == null) {
            return;
        }

        message.setMediaId(
                dto.getAudio().getId()
        );

        message.setMimeType(
                dto.getAudio().getMimeType()
        );

        message.setSha256(
                dto.getAudio().getSha256()
        );
    }

    private void mapDocument(
            Message message,
            MessageDto dto) {

        if (dto.getDocument() == null) {
            return;
        }

        message.setMediaId(
                dto.getDocument().getId()
        );

        message.setMimeType(
                dto.getDocument().getMimeType()
        );

        message.setSha256(
                dto.getDocument().getSha256()
        );

        message.setCaption(
                dto.getDocument().getCaption()
        );

        message.setFileName(
                dto.getDocument().getFilename()
        );
    }

    private void mapSticker(
            Message message,
            MessageDto dto) {

        if (dto.getSticker() == null) {
            return;
        }

        message.setMediaId(
                dto.getSticker().getId()
        );

        message.setMimeType(
                dto.getSticker().getMimeType()
        );

        message.setSha256(
                dto.getSticker().getSha256()
        );
    }

    private Long parseTimestamp(
            String timestamp) {

        if (timestamp == null
                || timestamp.isBlank()) {

            return null;
        }

        try {

            return Long.parseLong(
                    timestamp
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }
}