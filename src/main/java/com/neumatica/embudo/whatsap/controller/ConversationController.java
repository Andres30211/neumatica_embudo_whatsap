package com.neumatica.embudo.whatsap.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.dto.user.SendMessageRequest;
import com.neumatica.embudo.whatsap.dto.user.TakeConversationRequest;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.services.ConversationAttentionService;
import com.neumatica.embudo.whatsap.services.ManualMessageService;

/**
 * Endpoints relacionados con la atención humana
 * de conversaciones de WhatsApp.
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    /*
     * Servicio encargado de:
     * - Tomar conversaciones
     * - Cerrar conversaciones
     * - Devolver conversaciones al BOT
     */
    private final ConversationAttentionService conversationAttentionService;

    /*
     * Servicio encargado de enviar mensajes
     * manuales desde el CRM hacia WhatsApp.
     */
    private final ManualMessageService manualMessageService;

    /*
     * Repositorio para consultar las conversaciones.
     *
     * IMPORTANTE:
     * Los repositorios de Spring NO se utilizan de forma estática.
     * Debemos inyectar la instancia y utilizar:
     *
     * conversationRepository.findById(...)
     */
    private final ConversationRepository conversationRepository;

    /**
     * Constructor utilizado por Spring para inyectar
     * las dependencias necesarias.
     */
    public ConversationController(
            ConversationAttentionService conversationAttentionService,
            ManualMessageService manualMessageService,
            ConversationRepository conversationRepository
    ) {

        this.conversationAttentionService =
                conversationAttentionService;

        this.manualMessageService =
                manualMessageService;

        this.conversationRepository =
                conversationRepository;
    }

    /**
     * ============================================================
     * TOMAR CONVERSACIÓN
     * ============================================================
     *
     * Permite que un vendedor tome una conversación
     * que actualmente está siendo manejada por el BOT.
     *
     * POST:
     *
     * /api/conversations/{conversationId}/take
     *
     * Body:
     *
     * {
     *     "userId": "UUID-DEL-VENDEDOR"
     * }
     */
    @PostMapping("/{conversationId}/take")
    public ResponseEntity<Conversation> takeConversation(

            @PathVariable UUID conversationId,

            @RequestBody TakeConversationRequest request

    ) {

        Conversation conversation =
                conversationAttentionService.takeConversation(
                        conversationId,
                        request.getUserId()
                );

        return ResponseEntity.ok(conversation);
    }

    /**
     * ============================================================
     * CERRAR CONVERSACIÓN
     * ============================================================
     *
     * POST:
     *
     * /api/conversations/{conversationId}/close
     *
     * El estado de la conversación pasa a CLOSED.
     */
    @PostMapping("/{conversationId}/close")
    public ResponseEntity<Conversation> closeConversation(

            @PathVariable UUID conversationId

    ) {

        Conversation conversation =
                conversationAttentionService.closeConversation(
                        conversationId
                );

        return ResponseEntity.ok(conversation);
    }

    /**
     * ============================================================
     * DEVOLVER CONVERSACIÓN AL BOT
     * ============================================================
     *
     * POST:
     *
     * /api/conversations/{conversationId}/return-to-bot
     *
     * Permite que una conversación que estaba siendo
     * atendida por una persona vuelva a ser manejada
     * automáticamente por el BOT.
     */
    @PostMapping("/{conversationId}/return-to-bot")
    public ResponseEntity<Conversation> returnToBot(

            @PathVariable UUID conversationId

    ) {

        Conversation conversation =
                conversationAttentionService.returnToBot(
                        conversationId
                );

        return ResponseEntity.ok(conversation);
    }

    /**
     * ============================================================
     * ENVIAR MENSAJE MANUAL
     * ============================================================
     *
     * Envía un mensaje desde el CRM hacia WhatsApp.
     *
     * POST:
     *
     * /api/conversations/{conversationId}/messages
     *
     * Body:
     *
     * {
     *     "userId": "UUID-DEL-VENDEDOR",
     *     "message": "Hola, ¿cómo podemos ayudarte?"
     * }
     *
     * IMPORTANTE:
     *
     * NO debemos hacer:
     *
     * ManualMessageService.sendMessage(...)
     *
     * porque sendMessage NO es static.
     *
     * Debemos utilizar la instancia inyectada:
     *
     * manualMessageService.sendMessage(...)
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Message> sendMessage(

            @PathVariable UUID conversationId,

            @RequestBody SendMessageRequest request

    ) {

        Message message =
                manualMessageService.sendMessage(
                        conversationId,
                        request.getUserId(),
                        request.getMessage()
                );

        return ResponseEntity.ok(message);
    }

    /**
     * ============================================================
     * OBTENER CONVERSACIÓN
     * ============================================================
     *
     * GET:
     *
     * /api/conversations/{conversationId}
     *
     * Permite obtener una conversación junto con
     * la información que tenga asociada.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<Conversation> getConversation(

            @PathVariable UUID conversationId

    ) {

        /*
         * IMPORTANTE:
         *
         * No debemos hacer:
         *
         * ConversationRepository.findById(...)
         *
         * porque ConversationRepository tampoco es static.
         *
         * Utilizamos la instancia:
         *
         * conversationRepository.findById(...)
         */
        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversación no encontrada."
                                )
                        );

        return ResponseEntity.ok(conversation);
    }
}