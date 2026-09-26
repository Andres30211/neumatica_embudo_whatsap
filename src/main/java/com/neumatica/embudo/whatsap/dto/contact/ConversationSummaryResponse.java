package com.neumatica.embudo.whatsap.dto.contact;

import java.time.LocalDateTime;
import java.util.UUID;

import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.MessageType;
import com.neumatica.embudo.whatsap.enums.RegistrationStep;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryResponse {

    private UUID conversationId;

    private UUID contactId;

    private String contactName;

    private String phone;

    private RegistrationStep registrationStep;

    private String lastMessage;

    private MessageType lastMessageType;

    private LocalDateTime lastMessageAt;

    private ConversationStatus status;

    /*
     * Vendedor asignado actualmente
     * a la conversación.
     */
    private UUID assignedUserId;

    /*
     * Nombre del vendedor.
     *
     * No se persiste en Conversation.
     * Se obtiene desde el servicio de seguridad.
     */
    private String assignedUserName;
}