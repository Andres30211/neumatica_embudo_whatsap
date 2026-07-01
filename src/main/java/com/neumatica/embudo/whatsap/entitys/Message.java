package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.Data;
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

    /**
     * wamid...
     */
    @Column(unique = true)
    private String whatsappMessageId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    /**
     * Texto del mensaje
     */
    @Column(columnDefinition = "TEXT")
    private String body;

    /**
     * Media de WhatsApp
     */
    private String mediaId;

    /**
     * image/jpeg
     */
    private String mimeType;

    /**
     * Verificación
     */
    private String sha256;

    /**
     * Pie de imagen
     */
    private String caption;

    /**
     * Timestamp enviado por Meta
     */
    private Long whatsappTimestamp;

    private LocalDateTime createdAt;

}
