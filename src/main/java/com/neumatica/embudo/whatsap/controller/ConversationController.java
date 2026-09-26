package com.neumatica.embudo.whatsap.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.dto.user.SendMessageRequest;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.services.ConversationAttentionService;
import com.neumatica.embudo.whatsap.services.ManualMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationAttentionService conversationAttentionService;

    private final ManualMessageService manualMessageService;

    private final ConversationRepository conversationRepository;


    /**
     * ============================================================
     * TOMAR CONVERSACIÓN
     * ============================================================
     */
    @PostMapping("/{conversationId}/take")
    public ResponseEntity<Conversation> takeConversation(

            @PathVariable UUID conversationId,

            @AuthenticationPrincipal Jwt jwt

    ) {

        UUID userId =
                getUserIdFromJwt(jwt);

        String accessToken =
                jwt.getTokenValue();

        Conversation conversation =
                conversationAttentionService.takeConversation(
                        conversationId,
                        userId,
                        accessToken
                );

        return ResponseEntity.ok(conversation);
    }


    /**
     * ============================================================
     * ENVIAR MENSAJE MANUAL
     * ============================================================
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Message> sendMessage(

            @PathVariable UUID conversationId,

            @RequestBody SendMessageRequest request,

            @AuthenticationPrincipal Jwt jwt

    ) {

        UUID userId =
                getUserIdFromJwt(jwt);

        String accessToken =
                jwt.getTokenValue();

        Message message =
                manualMessageService.sendMessage(
                        conversationId,
                        userId,
                        request.getMessage(),
                        accessToken
                );

        return ResponseEntity.ok(message);
    }


    /**
     * ============================================================
     * OBTENER CONVERSACIÓN
     * ============================================================
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<Conversation> getConversation(

            @PathVariable UUID conversationId

    ) {

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


    /**
     * ============================================================
     * CERRAR CONVERSACIÓN
     * ============================================================
     */
    @PostMapping("/{conversationId}/close")
    public ResponseEntity<Conversation> closeConversation(

            @PathVariable UUID conversationId

    ) {

        return ResponseEntity.ok(
                conversationAttentionService
                        .closeConversation(
                                conversationId
                        )
        );
    }


    /**
     * ============================================================
     * DEVOLVER AL BOT
     * ============================================================
     */
    @PostMapping("/{conversationId}/return-to-bot")
    public ResponseEntity<Conversation> returnToBot(

            @PathVariable UUID conversationId

    ) {

        return ResponseEntity.ok(
                conversationAttentionService
                        .returnToBot(
                                conversationId
                        )
        );
    }


    /**
     * ============================================================
     * OBTENER UUID DEL USUARIO DESDE JWT
     * ============================================================
     */
    private UUID getUserIdFromJwt(Jwt jwt) {

        if (jwt == null) {

            throw new RuntimeException(
                    "JWT no disponible."
            );
        }

        String subject =
                jwt.getSubject();

        if (
                subject == null ||
                subject.isBlank()
        ) {

            throw new RuntimeException(
                    "El JWT no contiene el subject del usuario."
            );
        }

        try {

            return UUID.fromString(subject);

        } catch (IllegalArgumentException e) {

            throw new RuntimeException(
                    "El subject del JWT no contiene un UUID válido."
            );
        }
    }
}