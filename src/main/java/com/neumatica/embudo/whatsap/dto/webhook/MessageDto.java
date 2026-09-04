package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MessageDto {

    private String from;

    /*
     * Identificador del usuario proporcionado por Meta.
     * Puede no estar disponible en determinados eventos.
     */
    @JsonProperty("from_user_id")
    private String fromUserId;

    /*
     * ID del mensaje de WhatsApp (wamid).
     */
    private String id;

    /*
     * Timestamp enviado por Meta.
     */
    private String timestamp;

    /*
     * Tipo del mensaje:
     * text, image, video, audio, document, sticker, location, etc.
     */
    private String type;

    /*
     * Contenido textual.
     * Será null cuando el mensaje no sea de tipo text.
     */
    private TextDto text;

    /*
     * Contenido de imagen.
     * Será null cuando el mensaje no sea de tipo image.
     */
    private ImageDto image;

    /*
     * Contenido de video.
     */
    private VideoDto video;

    /*
     * Contenido de audio.
     */
    private AudioDto audio;

    /*
     * Contenido de documento.
     */
    private DocumentDto document;

    /*
     * Contenido de sticker.
     */
    private StickerDto sticker;

    /*
     * Contenido de ubicación.
     */
    private LocationDto location;

    /*
     * Contexto del mensaje, por ejemplo,
     * cuando es una respuesta a otro mensaje.
     */
    private ContextDto context;
}

