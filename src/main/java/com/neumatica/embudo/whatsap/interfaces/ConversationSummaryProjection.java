package com.neumatica.embudo.whatsap.interfaces;

import java.time.LocalDateTime;
import java.util.UUID;

import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.MessageType;
import com.neumatica.embudo.whatsap.enums.RegistrationStep;

public interface ConversationSummaryProjection {

    UUID getConversationId();

    UUID getContactId();

    String getContactName();

    String getPhone();

    RegistrationStep getRegistrationStep();

    String getLastMessage();

    MessageType getLastMessageType();

    LocalDateTime getLastMessageAt();

    ConversationStatus getStatus();

    UUID getAssignedUserId();
}