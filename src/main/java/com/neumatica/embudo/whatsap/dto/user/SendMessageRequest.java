package com.neumatica.embudo.whatsap.dto.user;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request utilizado para enviar un mensaje manual
 * desde el CRM hacia WhatsApp.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /*
     * UUID del vendedor que está enviando.
     *
     * Temporalmente viene desde Angular.
     *
     * Posteriormente lo obtendremos desde JWT.
     */
    private UUID userId;

    /*
     * Texto que desea enviar el vendedor.
     */
    private String message;
}
