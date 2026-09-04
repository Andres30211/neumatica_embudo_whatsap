package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * ID del mensaje proporcionado por WhatsApp/Meta.
     *
     * Ejemplo:
     * wamid.HBgM...
     *
     * Puede ser null en determinados eventos,
     * por lo que no se establece nullable = false.
     */
    @Column(unique = true)
    private String whatsappMessageId;

    /*
     * Conversación a la que pertenece el mensaje.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    /*
     * Dirección del mensaje:
     * INBOUND  -> Contacto hacia nuestro sistema
     * OUTBOUND -> Nuestro sistema hacia el contacto
     */
    @Enumerated(EnumType.STRING)
    private Direction direction;

    /*
     * Tipo de mensaje:
     * TEXT
     * IMAGE
     * VIDEO
     * AUDIO
     * DOCUMENT
     * STICKER
     * LOCATION
     * etc.
     */
    @Enumerated(EnumType.STRING)
    private MessageType type;

    /*
     * Texto del mensaje.
     *
     * Puede ser null.
     *
     * Ejemplos:
     *
     * Mensaje de texto:
     * body = "Hola, necesito una cotización"
     *
     * Imagen sin texto:
     * body = null
     *
     * Imagen con caption:
     * body = null
     * caption = "Mira esta referencia"
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String body;

    /*
     * ID del archivo multimedia proporcionado por WhatsApp/Meta.
     *
     * Ejemplo:
     * 123456789012345
     *
     * Puede ser null cuando el mensaje no contiene multimedia.
     */
    @Column(nullable = true)
    private String mediaId;

    /*
     * Ubicación del archivo multimedia almacenado
     * por nuestra aplicación.
     *
     * Ejemplo:
     * /uploads/whatsapp/abc123.jpg
     *
     * Puede ser null si el mensaje no contiene multimedia
     * o si todavía no ha sido almacenado.
     */
    @Column(nullable = true)
    private String storagePath;

    /*
     * Tipo MIME del archivo.
     *
     * Ejemplos:
     * image/jpeg
     * image/png
     * video/mp4
     * audio/ogg
     * application/pdf
     */
    @Column(nullable = true)
    private String mimeType;

    /*
     * Hash SHA-256 proporcionado por WhatsApp.
     */
    @Column(nullable = true)
    private String sha256;

    /*
     * Texto que acompaña a una imagen, video o documento.
     *
     * Ejemplo:
     * "Mira esta referencia"
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String caption;

    /*
     * Nombre original del archivo.
     *
     * Especialmente útil para documentos.
     */
    @Column(nullable = true)
    private String fileName;

    /*
     * Timestamp enviado por Meta.
     */
    @Column(nullable = true)
    private Long whatsappTimestamp;

    /*
     * Fecha en la que nuestro sistema registró el mensaje.
     */
    private LocalDateTime createdAt;
}
