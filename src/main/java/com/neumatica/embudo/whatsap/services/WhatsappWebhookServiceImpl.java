package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.webhook.ContactDto;
import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.dto.webhook.ValueDto;
import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.mapper.ContactMapper;
import com.neumatica.embudo.whatsap.mapper.MessageMapper;
import com.neumatica.embudo.whatsap.repository.ContactRepository;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;
import com.neumatica.embudo.whatsap.repository.WhatsappWebhookService;

@Service
public class WhatsappWebhookServiceImpl implements  WhatsappWebhookService{
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private ConversationRepository conversationRepository;
	
	@Autowired
	private MessageRepository messageRepository;
	
	@Autowired
	private ContactMapper contactMapper;
	
	@Autowired
	private MessageMapper messageMapper;
	
	@Override
	public List<Contact> contacts() {
		return this.contactRepository.findAll();
	}

	@Override
	public void processWebhook(WhatsappWebHookDto webhook) {
		ValueDto value = webhook.getEntry()
                .getFirst()
                .getChanges()
                .getFirst()
                .getValue();

        ContactDto contactDTO = value.getContacts().getFirst();

        MessageDto messageDTO = value.getMessages().getFirst();

        Contact contact =this.getOrCreateContact(contactDTO);

        Conversation conversation = getOrCreateConversation(contact);

        saveMessage(conversation, messageDTO);

    }

    private Contact getOrCreateContact(ContactDto dto) {

        return contactRepository.findByPhone(dto.getWaId())
                .map(contact -> {

                    contact.setName(dto.getProfile().getName());

                    contact.setLastInteraction(LocalDateTime.now());

                    return contactRepository.save(contact);

                })
                .orElseGet(() -> {

                    Contact contact = contactMapper.toEntity(dto);

                    return contactRepository.save(contact);

                });

    }

    private Conversation getOrCreateConversation(Contact contact) {

        return conversationRepository
                .findFirstByContactAndStatus(
                        contact,
                        ConversationStatus.BOT
                )
                .orElseGet(() -> {

                    Conversation conversation =
                            Conversation.builder()
                                    .contact(contact)
                                    .status(ConversationStatus.BOT)
                                    .startedAt(LocalDateTime.now())
                                    .lastMessageAt(LocalDateTime.now())
                                    .build();

                    return conversationRepository.save(conversation);

                });

    }

    private void saveMessage(Conversation conversation,
                             MessageDto dto) {

        if (messageRepository.existsByWhatsappMessageId(dto.getId())) {
            return;
        }

        Message message =
                messageMapper.toEntity(dto, conversation);

        messageRepository.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());

        conversationRepository.save(conversation);

    }

}
