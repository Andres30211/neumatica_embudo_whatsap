package com.neumatica.embudo.whatsap.enums;


/**
 * Estados posibles de una conversación.
 *
 * BOT:
 * La conversación está siendo manejada por la automatización.
 *
 * HUMAN:
 * La conversación está siendo manejada por un vendedor.
 *
 * CLOSED:
 * La conversación fue finalizada.
 */
public enum ConversationStatus {

    BOT,

    HUMAN,

    CLOSED
}
