package com.neumatica.embudo.whatsap.repository;

import java.util.List;
import java.util.UUID;

import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;
import com.neumatica.embudo.whatsap.entitys.Contact;

public interface WhatsappWebhookService {
	
	void delete(UUID id);
	
	List<Contact> contacts();

	void processWebhook(WhatsappWebHookDto webhook);
}
