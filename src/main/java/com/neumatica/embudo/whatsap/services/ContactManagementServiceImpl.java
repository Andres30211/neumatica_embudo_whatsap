package com.neumatica.embudo.whatsap.services;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neumatica.embudo.whatsap.dto.contact.ContactResponse;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.repository.ContactManagementService;
import com.neumatica.embudo.whatsap.repository.ContactRepository;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactManagementServiceImpl implements ContactManagementService {

	@Autowired
    private ContactRepository contactRepository;
    
	@Autowired
    private MediaStorageServiceImpl mediaStorageServiceImpl;
	
	@Autowired
	private MessageRepository messageRepository;
	
	@Autowired
	private ConversationRepository conversationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(Pageable pageable) {

        return contactRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactResponse getContactById(UUID id) {

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Contacto no encontrado: " + id)
                );

        return toResponse(contact);
    }
    
    @Override
    @Transactional
    public void delete(UUID id) {

        /*
         * ============================================================
         * 1. BUSCAR EL CONTACTO
         * ============================================================
         */

        Contact contact = this.contactRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Contacto no encontrado."
                        )
                );


        /*
         * ============================================================
         * 2. OBTENER LAS CONVERSACIONES
         * ============================================================
         *
         * Creamos una copia de la colección para evitar problemas
         * si Hibernate modifica la colección durante la eliminación.
         */

        List<Conversation> conversations =
                contact.getConversations() == null
                        ? List.of()
                        : List.copyOf(
                                contact.getConversations()
                        );


        /*
         * ============================================================
         * 3. RECORRER LAS CONVERSACIONES
         * ============================================================
         */

        for (Conversation conversation : conversations) {

            if (conversation == null) {
                continue;
            }


            /*
             * ========================================================
             * 4. OBTENER LOS MENSAJES
             * ========================================================
             */

            List<Message> messages =
                    conversation.getMessages() == null
                            ? List.of()
                            : List.copyOf(
                                    conversation.getMessages()
                            );


            /*
             * ========================================================
             * 5. ELIMINAR ARCHIVOS MULTIMEDIA FÍSICOS
             * ========================================================
             *
             * La base de datos solamente guarda la referencia
             * (storagePath).
             *
             * Por eso debemos eliminar también el archivo físico.
             */

            for (Message message : messages) {

                if (message == null) {
                    continue;
                }

                String storagePath =
                        message.getStoragePath();


                if (storagePath != null
                        && !storagePath.isBlank()) {

                    try {

                        this.mediaStorageServiceImpl.delete(
                                storagePath
                        );

                    } catch (Exception e) {

                        /*
                         * Si el archivo físico ya no existe
                         * o Storage presenta algún problema,
                         * no detenemos la eliminación de la BD.
                         */

                        System.err.println(
                                "No fue posible eliminar el archivo multimedia: "
                                        + storagePath
                        );

                        System.err.println(
                                "Detalle: "
                                        + e.getMessage()
                        );
                    }
                }
            }


            /*
             * ========================================================
             * 6. ELIMINAR MENSAJES DE LA BASE DE DATOS
             * ========================================================
             *
             * Se eliminan primero los mensajes porque dependen
             * de la conversación.
             */

            if (!messages.isEmpty()) {

                this.messageRepository.deleteAll(
                        messages
                );
            }


            /*
             * ========================================================
             * 7. ELIMINAR LA CONVERSACIÓN
             * ========================================================
             */

            this.conversationRepository.delete(
                    conversation
            );
        }


        /*
         * ============================================================
         * 8. ELIMINAR EL CONTACTO
         * ============================================================
         *
         * En este punto:
         *
         * Contacto
         *    └── Conversaciones
         *          └── Mensajes
         *
         * ya fueron procesados.
         */

        this.contactRepository.delete(
                contact
        );


        /*
         * ============================================================
         * 9. FORZAR LA EJECUCIÓN DE LAS OPERACIONES
         * ============================================================
         *
         * Esto permite detectar inmediatamente cualquier problema
         * de integridad referencial antes de finalizar la transacción.
         */

        this.contactRepository.flush();
    }


    private ContactResponse toResponse(Contact contact) {

        String registrationStatus =
                contact.getRegistrationStep() != null
                        && contact.getRegistrationStep().name().equals("COMPLETED")
                        ? "COMPLETO"
                        : "INCOMPLETO";

        return ContactResponse.builder()
                .id(contact.getId())
                .name(contact.getName())
                .phone(contact.getPhone())
                .email(contact.getEmail())
                .company(contact.getCompany())
                .registrationStep(contact.getRegistrationStep())
                .registrationStatus(registrationStatus)
                .firstContact(contact.getFirstContact())
                .lastInteraction(contact.getLastInteraction())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
