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
     * Puede ser null para mensajes que todavía no tengan
     * un wamid disponible.
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
     * Dirección del mensaje.
     *
     * INBOUND:
     * Cliente -> CRM
     *
     * OUTBOUND:
     * CRM -> Cliente
     */
    @Enumerated(EnumType.STRING)
    private Direction direction;

    /*
     * Usuario del CRM que escribió el mensaje.
     *
     * IMPORTANTE:
     *
     * Solamente tendrá valor cuando el mensaje sea enviado
     * manualmente por un vendedor.
     *
     * Para mensajes del cliente será null.
     *
     * Para mensajes automáticos del BOT también puede ser null.
     */
    private UUID senderUserId;

    /*
     * Tipo de mensaje.
     */
    @Enumerated(EnumType.STRING)
    private MessageType type;

    /*
     * Texto del mensaje.
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String body;

    /*
     * ID del archivo multimedia proporcionado por WhatsApp.
     */
    @Column(nullable = true)
    private String mediaId;

    /*
     * Ruta donde nuestro sistema almacena el archivo.
     */
    @Column(nullable = true)
    private String storagePath;

    /*
     * Tipo MIME.
     */
    @Column(nullable = true)
    private String mimeType;

    /*
     * Hash SHA-256.
     */
    @Column(nullable = true)
    private String sha256;

    /*
     * Caption del archivo multimedia.
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String caption;

    /*
     * Nombre original del archivo.
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