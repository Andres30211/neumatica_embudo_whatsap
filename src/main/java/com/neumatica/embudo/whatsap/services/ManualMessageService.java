package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;
import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;

import jakarta.transaction.Transactional;

/**
 * Servicio responsable de enviar mensajes manuales
 * desde el CRM hacia WhatsApp.
 */
@Service
public class ManualMessageService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Bogota");

    private final ConversationRepository
            conversationRepository;

    private final MessageRepository
            messageRepository;

    private final WhatsappResponseAutimatics
            whatsappResponseAutimatics;

    private final UserClientService
            userClientService;

    public ManualMessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WhatsappResponseAutimatics whatsappResponseAutimatics,
            UserClientService userClientService
    ) {

        this.conversationRepository =
                conversationRepository;

        this.messageRepository =
                messageRepository;

        this.whatsappResponseAutimatics =
                whatsappResponseAutimatics;

        this.userClientService =
                userClientService;
    }

    /**
     * Envía un mensaje manual desde el CRM.
     */
    @Transactional
    public Message sendMessage(
            UUID conversationId,
            UUID userId,
            String text,
            String accessToken
    ) {

        /*
         * ============================================================
         * 1. VALIDAMOS LOS PARÁMETROS
         * ============================================================
         */

        if (conversationId == null) {
            throw new IllegalArgumentException(
                    "El conversationId es obligatorio."
            );
        }

        if (userId == null) {
            throw new IllegalArgumentException(
                    "El userId es obligatorio."
            );
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "El mensaje no puede estar vacío."
            );
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "El accessToken es obligatorio."
            );
        }


        /*
         * ============================================================
         * 2. VERIFICAMOS QUE EL VENDEDOR EXISTA
         * ============================================================
         *
         * En este punto enviamos también el JWT al Security Service.
         *
         * El Security Service será el encargado de validar
         * que el token sea válido y consultar al usuario.
         */

        var user = userClientService.findById(
                userId,
                accessToken
        );

        if (user == null) {
            throw new RuntimeException(
                    "Usuario no encontrado."
            );
        }


        /*
         * ============================================================
         * 3. BUSCAMOS LA CONVERSACIÓN
         * ============================================================
         */

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversación no encontrada."
                                )
                        );


        /*
         * ============================================================
         * 4. VERIFICAMOS QUE ESTÉ EN ATENCIÓN HUMANA
         * ============================================================
         *
         * El vendedor no puede enviar mensajes manuales si
         * la conversación todavía está siendo atendida por el BOT
         * o si ya fue cerrada.
         */

        if (conversation.getStatus()
                != ConversationStatus.HUMAN) {

            throw new RuntimeException(
                    "La conversación no está en atención humana."
            );
        }


        /*
         * ============================================================
         * 5. VERIFICAMOS QUE EL VENDEDOR SEA EL ASIGNADO
         * ============================================================
         *
         * Esto evita que un vendedor pueda escribir en una
         * conversación que pertenece a otro vendedor.
         */

        if (!userId.equals(
                conversation.getAssignedUserId()
        )) {

            throw new RuntimeException(
                    "La conversación no está asignada a este vendedor."
            );
        }


        /*
         * ============================================================
         * 6. OBTENEMOS EL CONTACTO
         * ============================================================
         */

        Contact contact =
                conversation.getContact();

        if (contact == null) {
            throw new RuntimeException(
                    "La conversación no tiene contacto."
            );
        }


        /*
         * ============================================================
         * 7. VALIDAMOS EL TELÉFONO DE WHATSAPP
         * ============================================================
         */

        if (contact.getPhone() == null
                || contact.getPhone().isBlank()) {

            throw new RuntimeException(
                    "El contacto no tiene número de WhatsApp."
            );
        }


        /*
         * ============================================================
         * 8. ENVIAMOS EL MENSAJE A WHATSAPP
         * ============================================================
         *
         * Utilizamos el servicio que ya tienes actualmente.
         *
         * IMPORTANTE:
         *
         * Tu método sendText() actualmente no devuelve el wamid
         * generado por Meta, por eso por ahora guardamos null
         * en whatsappMessageId.
         */

        whatsappResponseAutimatics.sendText(
                contact.getPhone(),
                text
        );


        /*
         * ============================================================
         * 9. CREAMOS EL MENSAJE PARA EL HISTORIAL
         * ============================================================
         */

        LocalDateTime now =
                LocalDateTime.now(ZONE_ID);

        Message message =
                Message.builder()

                        /*
                         * Por ahora no tenemos el ID que devuelve
                         * Meta/WhatsApp.
                         */
                        .whatsappMessageId(null)

                        /*
                         * Conversación a la que pertenece.
                         */
                        .conversation(conversation)

                        /*
                         * El mensaje sale desde nuestro CRM
                         * hacia el cliente.
                         */
                        .direction(Direction.OUTGOING)

                        /*
                         * Vendedor que envió el mensaje.
                         */
                        .senderUserId(userId)

                        /*
                         * Tipo de mensaje.
                         */
                        .type(MessageType.TEXT)

                        /*
                         * Texto enviado.
                         */
                        .body(text)

                        /*
                         * Fecha de creación.
                         */
                        .createdAt(now)

                        /*
                         * Timestamp en formato Unix.
                         */
                        .whatsappTimestamp(
                                now.atZone(ZONE_ID)
                                        .toEpochSecond()
                        )

                        .build();


        /*
         * ============================================================
         * 10. AGREGAMOS EL MENSAJE A LA CONVERSACIÓN
         * ============================================================
         */

        conversation.addMessage(message);


        /*
         * ============================================================
         * 11. ACTUALIZAMOS EL ÚLTIMO MENSAJE
         * ============================================================
         */

        conversation.setLastMessageAt(now);


        /*
         * ============================================================
         * 12. GUARDAMOS LA CONVERSACIÓN
         * ============================================================
         *
         * Si Conversation tiene CascadeType.ALL sobre messages,
         * el nuevo Message también será persistido.
         */

        conversationRepository.save(
                conversation
        );


        /*
         * ============================================================
         * 13. DEVOLVEMOS EL MENSAJE
         * ============================================================
         */

        return message;
    }
}