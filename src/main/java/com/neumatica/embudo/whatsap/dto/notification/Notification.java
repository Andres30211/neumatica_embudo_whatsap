package com.neumatica.embudo.whatsap.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record Notification(
        UUID id,
        String type,
        String title,
        String message,
        UUID referenceId,
        LocalDateTime createdAt,
        boolean read
) {
}
