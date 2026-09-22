package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Contacto asociado a la conversación.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = true)
    private Contact contact;

    /*
     * Mensajes de la conversación.
     */
    @OneToMany(
        mappedBy = "conversation",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    /*
     * Estado actual de la conversación.
     *
     * BOT:
     * La conversación está siendo manejada por el bot.
     *
     * HUMAN:
     * La conversación está siendo manejada por un vendedor.
     *
     * CLOSED:
     * La conversación fue cerrada.
     */
    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    /*
     * UUID del vendedor asignado.
     *
     * IMPORTANTE:
     *
     * Este NO es una relación @ManyToOne porque User
     * pertenece a otro microservicio.
     *
     * Aquí solamente almacenamos el UUID del usuario.
     */
    private UUID assignedUserId;

    /*
     * Fecha y hora en la que el vendedor tomó
     * la conversación.
     */
    private LocalDateTime assignedAt;

    /*
     * Fecha de inicio de la conversación.
     */
    private LocalDateTime startedAt;

    /*
     * Fecha del último mensaje.
     */
    private LocalDateTime lastMessageAt;

    /*
     * Fecha en la que se cerró la conversación.
     */
    private LocalDateTime closedAt;

    /*
     * Control de concurrencia optimista.
     *
     * Evita problemas cuando dos operaciones intentan
     * modificar la misma conversación al mismo tiempo.
     */
    @Version
    private Long version;

    /*
     * Agrega un mensaje manteniendo la relación bidireccional.
     */
    public void addMessage(Message message) {

        if (message == null) {
            return;
        }

        messages.add(message);
        message.setConversation(this);
    }
}